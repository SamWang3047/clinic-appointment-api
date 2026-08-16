package com.sam.clinic.availability;

import com.sam.clinic.appointment.Appointment;
import com.sam.clinic.appointment.AppointmentRepository;
import com.sam.clinic.appointment.AppointmentStatus;
import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.doctor.DoctorRepository;
import com.sam.clinic.shared.error.BusinessRuleException;
import com.sam.clinic.shared.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailableSlotService {

	private static final Set<Integer> ALLOWED_DURATIONS = Set.of(15, 30, 45, 60);
	private static final Set<AppointmentStatus> RESERVING_STATUSES = Set.of(
			AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

	private final DoctorRepository doctorRepository;
	private final DoctorWeeklyIntervalRepository intervalRepository;
	private final AppointmentRepository appointmentRepository;
	private final Clock clock;
	private final ZoneId clinicZone;

	public AvailableSlotService(
			DoctorRepository doctorRepository,
			DoctorWeeklyIntervalRepository intervalRepository,
			AppointmentRepository appointmentRepository,
			Clock clock,
			ZoneId clinicZone) {
		this.doctorRepository = doctorRepository;
		this.intervalRepository = intervalRepository;
		this.appointmentRepository = appointmentRepository;
		this.clock = clock;
		this.clinicZone = clinicZone;
	}

	@Transactional(readOnly = true)
	public List<AvailableSlotResponse> list(UUID doctorId, LocalDate date, int durationMinutes) {
		if (!ALLOWED_DURATIONS.contains(durationMinutes)) {
			throw new BusinessRuleException("durationMinutes must be one of 15, 30, 45 or 60.");
		}
		Doctor doctor = doctorRepository.findById(doctorId)
				.orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
		if (!doctor.isActive()) {
			return List.of();
		}

		Instant dayStart = date.atStartOfDay(clinicZone).toInstant();
		Instant dayEnd = date.plusDays(1).atStartOfDay(clinicZone).toInstant();
		List<Appointment> blockers = appointmentRepository.findDoctorAppointmentsOverlapping(
				doctorId, RESERVING_STATUSES, dayStart, dayEnd);

		return intervalRepository.findByDoctorIdAndDayOfWeek(doctorId, date.getDayOfWeek()).stream()
				.flatMap(interval -> candidates(date, interval, durationMinutes))
				.filter(slot -> slot.startAt().isAfter(clock.instant()))
				.filter(slot -> blockers.stream().noneMatch(appointment -> overlaps(slot, appointment)))
				.sorted(java.util.Comparator.comparing(AvailableSlotResponse::startAt))
				.toList();
	}

	private java.util.stream.Stream<AvailableSlotResponse> candidates(
			LocalDate date, DoctorWeeklyInterval interval, int durationMinutes) {
		LocalDateTime candidate = roundUpToQuarterHour(date.atTime(interval.getStartTime()));
		LocalDateTime intervalEnd = date.atTime(interval.getEndTime());
		java.util.stream.Stream.Builder<AvailableSlotResponse> slots = java.util.stream.Stream.builder();
		while (!candidate.plusMinutes(durationMinutes).isAfter(intervalEnd)) {
			Instant startAt = candidate.atZone(clinicZone).toInstant();
			slots.add(new AvailableSlotResponse(
					startAt,
					startAt.plus(Duration.ofMinutes(durationMinutes)),
					durationMinutes));
			candidate = candidate.plusMinutes(15);
		}
		return slots.build();
	}

	private static LocalDateTime roundUpToQuarterHour(LocalDateTime value) {
		LocalDateTime minute = value.withSecond(0).withNano(0);
		int remainder = minute.getMinute() % 15;
		return remainder == 0 ? minute : minute.plusMinutes(15 - remainder);
	}

	private static boolean overlaps(AvailableSlotResponse slot, Appointment appointment) {
		return appointment.getStartAt().isBefore(slot.endAt())
				&& appointment.getEndAt().isAfter(slot.startAt());
	}
}
