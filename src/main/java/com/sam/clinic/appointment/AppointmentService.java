package com.sam.clinic.appointment;

import com.sam.clinic.account.AccountRole;
import com.sam.clinic.account.CurrentAccountService;
import com.sam.clinic.account.UserAccount;
import com.sam.clinic.availability.DoctorWeeklyInterval;
import com.sam.clinic.availability.DoctorWeeklyIntervalRepository;
import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.doctor.DoctorRepository;
import com.sam.clinic.patient.Patient;
import com.sam.clinic.patient.PatientRepository;
import com.sam.clinic.shared.error.BusinessRuleException;
import com.sam.clinic.shared.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

	private static final Set<Integer> ALLOWED_DURATIONS = Set.of(15, 30, 45, 60);
	private static final Set<AppointmentStatus> RESERVING_STATUSES = Set.of(
			AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
	private static final Instant EARLIEST_SEARCH = Instant.parse("0001-01-01T00:00:00Z");
	private static final Instant LATEST_SEARCH = Instant.parse("9999-12-31T23:59:59Z");

	private final AppointmentRepository appointmentRepository;
	private final AppointmentAuditEventRepository auditRepository;
	private final DoctorRepository doctorRepository;
	private final PatientRepository patientRepository;
	private final DoctorWeeklyIntervalRepository intervalRepository;
	private final CurrentAccountService currentAccountService;
	private final Clock clock;
	private final ZoneId clinicZone;

	public AppointmentService(
			AppointmentRepository appointmentRepository,
			AppointmentAuditEventRepository auditRepository,
			DoctorRepository doctorRepository,
			PatientRepository patientRepository,
			DoctorWeeklyIntervalRepository intervalRepository,
			CurrentAccountService currentAccountService,
			Clock clock,
			ZoneId clinicZone) {
		this.appointmentRepository = appointmentRepository;
		this.auditRepository = auditRepository;
		this.doctorRepository = doctorRepository;
		this.patientRepository = patientRepository;
		this.intervalRepository = intervalRepository;
		this.currentAccountService = currentAccountService;
		this.clock = clock;
		this.clinicZone = clinicZone;
	}

	@Transactional
	@PreAuthorize("hasRole('PATIENT')")
	public AppointmentResponse createForCurrentPatient(
			CreateSelfAppointmentRequest request, String correlationId) {
		UserAccount actor = currentAccountService.requireCurrent();
		return create(
				actor,
				actor.getPatient().getId(),
				request.doctorId(),
				request.startAt(),
				request.durationMinutes(),
				request.reason(),
				correlationId);
	}

	@Transactional
	@PreAuthorize("hasRole('RECEPTIONIST')")
	public AppointmentResponse createForPatient(
			CreateStaffAppointmentRequest request, String correlationId) {
		UserAccount actor = currentAccountService.requireCurrent();
		return create(
				actor,
				request.patientId(),
				request.doctorId(),
				request.startAt(),
				request.durationMinutes(),
				request.reason(),
				correlationId);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
	public AppointmentPageResponse listMine(
			Instant fromAt,
			Instant toAt,
			Collection<AppointmentStatus> statuses,
			int page,
			int size) {
		UserAccount viewer = currentAccountService.requireCurrent();
		UUID patientId = viewer.getRole() == AccountRole.PATIENT ? viewer.getPatient().getId() : null;
		UUID doctorId = viewer.getRole() == AccountRole.DOCTOR ? viewer.getDoctor().getId() : null;
		return search(viewer, patientId, doctorId, fromAt, toAt, statuses, page, size);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
	public AppointmentPageResponse searchClinic(
			UUID patientId,
			UUID doctorId,
			Instant fromAt,
			Instant toAt,
			Collection<AppointmentStatus> statuses,
			int page,
			int size) {
		return search(
				currentAccountService.requireCurrent(),
				patientId,
				doctorId,
				fromAt,
				toAt,
				statuses,
				page,
				size);
	}

	@Transactional(readOnly = true)
	public AppointmentResponse get(UUID appointmentId) {
		UserAccount viewer = currentAccountService.requireCurrent();
		Appointment appointment = requireDetailedAppointment(appointmentId);
		if (!canRead(appointment, viewer)) {
			throw new ResourceNotFoundException("Appointment not found");
		}
		return AppointmentResponse.from(appointment, viewer);
	}

	@Transactional
	@PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
	public AppointmentResponse confirm(UUID appointmentId, String correlationId) {
		UserAccount actor = currentAccountService.requireCurrent();
		Appointment appointment = requireDetailedAppointment(appointmentId);
		requireCanConfirm(appointment, actor);
		AppointmentStatus previousStatus = appointment.getStatus();
		Instant now = clock.instant();
		appointment.confirm(now);
		appointmentRepository.flush();
		auditRepository.save(AppointmentAuditEvent.statusChanged(
				appointment,
				actor.getId(),
				actor.getRole(),
				AppointmentAction.CONFIRMED,
				previousStatus,
				now,
				correlationId));
		return AppointmentResponse.from(appointment, actor);
	}

	@Transactional
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'RECEPTIONIST', 'ADMIN')")
	public AppointmentResponse cancel(
			UUID appointmentId, CancelAppointmentRequest request, String correlationId) {
		UserAccount actor = currentAccountService.requireCurrent();
		Appointment appointment = requireDetailedAppointment(appointmentId);
		String reason = request == null ? null : request.reason();
		requireCanCancel(appointment, actor, reason);

		Instant now = clock.instant();
		if (actor.getRole() == AccountRole.PATIENT
				&& appointment.getStartAt().isBefore(now.plus(Duration.ofHours(2)))) {
			throw new InvalidAppointmentStateException(
					"Patients must cancel at least two hours before the appointment starts");
		}
		AppointmentStatus previousStatus = appointment.getStatus();
		appointment.cancel(now, reason);
		appointmentRepository.flush();
		auditRepository.save(AppointmentAuditEvent.statusChanged(
				appointment,
				actor.getId(),
				actor.getRole(),
				AppointmentAction.CANCELLED,
				previousStatus,
				now,
				correlationId));
		return AppointmentResponse.from(appointment, actor);
	}

	private AppointmentResponse create(
			UserAccount actor,
			UUID patientId,
			UUID doctorId,
			Instant startAt,
			int durationMinutes,
			String reason,
			String correlationId) {
		validateRequestedTime(startAt, durationMinutes);
		Instant endAt = startAt.plus(Duration.ofMinutes(durationMinutes));

		// Fixed lock order is part of the concurrency contract: Doctor, then Patient.
		Doctor doctor = doctorRepository.findByIdForUpdate(doctorId)
				.orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
		Patient patient = patientRepository.findByIdForUpdate(patientId)
				.orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
		if (!doctor.isActive()) {
			throw new BusinessRuleException("Inactive doctors cannot receive appointments.");
		}
		requireWithinWeeklyAvailability(doctorId, startAt, endAt);
		requireNoOverlap(doctorId, patientId, startAt, endAt);

		Appointment appointment = appointmentRepository.saveAndFlush(
				new Appointment(doctor, patient, startAt, endAt, reason));
		Instant occurredAt = clock.instant();
		auditRepository.save(AppointmentAuditEvent.created(
				appointment,
				actor.getId(),
				actor.getRole(),
				occurredAt,
				correlationId));
		return AppointmentResponse.from(appointment, actor);
	}

	private AppointmentPageResponse search(
			UserAccount viewer,
			UUID patientId,
			UUID doctorId,
			Instant fromAt,
			Instant toAt,
			Collection<AppointmentStatus> statuses,
			int page,
			int size) {
		Instant effectiveFrom = fromAt == null ? EARLIEST_SEARCH : fromAt;
		Instant effectiveTo = toAt == null ? LATEST_SEARCH : toAt;
		if (!effectiveTo.isAfter(effectiveFrom)) {
			throw new BusinessRuleException("to must be after from.");
		}
		Collection<AppointmentStatus> effectiveStatuses = statuses == null || statuses.isEmpty()
				? EnumSet.allOf(AppointmentStatus.class)
				: EnumSet.copyOf(statuses);
		Page<Appointment> result = appointmentRepository.search(
				patientId,
				doctorId,
				effectiveFrom,
				effectiveTo,
				effectiveStatuses,
				PageRequest.of(page, size));
		List<AppointmentResponse> items = result.getContent().stream()
				.map(appointment -> AppointmentResponse.from(appointment, viewer))
				.toList();
		return new AppointmentPageResponse(
				items,
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages());
	}

	private void validateRequestedTime(Instant startAt, int durationMinutes) {
		if (!ALLOWED_DURATIONS.contains(durationMinutes)) {
			throw new BusinessRuleException("durationMinutes must be one of 15, 30, 45 or 60.");
		}
		if (!startAt.isAfter(clock.instant())) {
			throw new BusinessRuleException("startAt must be in the future.");
		}
		ZonedDateTime utcStart = startAt.atZone(java.time.ZoneOffset.UTC);
		if (utcStart.getMinute() % 15 != 0 || utcStart.getSecond() != 0 || utcStart.getNano() != 0) {
			throw new BusinessRuleException("startAt must be on a 15-minute boundary.");
		}
	}

	private void requireWithinWeeklyAvailability(UUID doctorId, Instant startAt, Instant endAt) {
		ZonedDateTime localStart = startAt.atZone(clinicZone);
		ZonedDateTime localEnd = endAt.atZone(clinicZone);
		boolean withinOneInterval = localStart.toLocalDate().equals(localEnd.toLocalDate())
				&& intervalRepository.findByDoctorIdAndDayOfWeek(doctorId, localStart.getDayOfWeek()).stream()
						.anyMatch(interval -> contains(interval, localStart, localEnd));
		if (!withinOneInterval) {
			throw new AppointmentConflictException("The requested time is outside the doctor's availability.");
		}
	}

	private void requireNoOverlap(UUID doctorId, UUID patientId, Instant startAt, Instant endAt) {
		if (appointmentRepository.existsDoctorOverlap(
				doctorId, RESERVING_STATUSES, startAt, endAt)) {
			throw new AppointmentConflictException("The doctor is no longer available at the requested time.");
		}
		if (appointmentRepository.existsPatientOverlap(
				patientId, RESERVING_STATUSES, startAt, endAt)) {
			throw new AppointmentConflictException("The patient already has an overlapping appointment.");
		}
	}

	private Appointment requireDetailedAppointment(UUID appointmentId) {
		return appointmentRepository.findDetailedById(appointmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
	}

	private static boolean contains(
			DoctorWeeklyInterval interval, ZonedDateTime localStart, ZonedDateTime localEnd) {
		return !localStart.toLocalTime().isBefore(interval.getStartTime())
				&& !localEnd.toLocalTime().isAfter(interval.getEndTime());
	}

	private static boolean canRead(Appointment appointment, UserAccount viewer) {
		return switch (viewer.getRole()) {
			case PATIENT -> samePatient(appointment, viewer);
			case DOCTOR -> sameDoctor(appointment, viewer);
			case RECEPTIONIST, ADMIN -> true;
		};
	}

	private static void requireCanConfirm(Appointment appointment, UserAccount actor) {
		if (actor.getRole() == AccountRole.RECEPTIONIST
				|| actor.getRole() == AccountRole.DOCTOR && sameDoctor(appointment, actor)) {
			return;
		}
		throw new AccessDeniedException("Account cannot confirm this appointment");
	}

	private static void requireCanCancel(
			Appointment appointment, UserAccount actor, String cancellationReason) {
		boolean patientOwner = actor.getRole() == AccountRole.PATIENT && samePatient(appointment, actor);
		boolean assignedDoctor = actor.getRole() == AccountRole.DOCTOR && sameDoctor(appointment, actor);
		boolean authorisedStaff = assignedDoctor
				|| actor.getRole() == AccountRole.RECEPTIONIST
				|| actor.getRole() == AccountRole.ADMIN;
		if (!patientOwner && !authorisedStaff) {
			throw new AccessDeniedException("Account cannot cancel this appointment");
		}
		if (authorisedStaff && (cancellationReason == null || cancellationReason.isBlank())) {
			throw new BusinessRuleException("Staff must provide a cancellation reason.");
		}
	}

	private static boolean samePatient(Appointment appointment, UserAccount account) {
		return account.getPatient() != null
				&& account.getPatient().getId().equals(appointment.getPatient().getId());
	}

	private static boolean sameDoctor(Appointment appointment, UserAccount account) {
		return account.getDoctor() != null
				&& account.getDoctor().getId().equals(appointment.getDoctor().getId());
	}
}
