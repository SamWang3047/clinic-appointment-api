package com.sam.clinic.availability;

import com.sam.clinic.account.AccountRole;
import com.sam.clinic.account.CurrentAccountService;
import com.sam.clinic.account.UserAccount;
import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.doctor.DoctorRepository;
import com.sam.clinic.shared.config.TimeConfiguration;
import com.sam.clinic.shared.error.BusinessRuleException;
import com.sam.clinic.shared.error.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyAvailabilityService {

	private static final Comparator<WeeklyTimeRange> RANGE_ORDER = Comparator
			.comparingInt((WeeklyTimeRange interval) -> interval.dayOfWeek().getValue())
			.thenComparing(WeeklyTimeRange::startTime)
			.thenComparing(WeeklyTimeRange::endTime);

	private final DoctorRepository doctorRepository;
	private final DoctorWeeklyIntervalRepository intervalRepository;
	private final CurrentAccountService currentAccountService;

	public WeeklyAvailabilityService(
			DoctorRepository doctorRepository,
			DoctorWeeklyIntervalRepository intervalRepository,
			CurrentAccountService currentAccountService) {
		this.doctorRepository = doctorRepository;
		this.intervalRepository = intervalRepository;
		this.currentAccountService = currentAccountService;
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
	public WeeklyAvailabilityResponse get(UUID doctorId) {
		Doctor doctor = requireDoctor(doctorId);
		requireCanManage(doctor, currentAccountService.requireCurrent());
		return response(doctorId, intervalRepository.findByDoctorId(doctorId));
	}

	@Transactional
	@PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
	public WeeklyAvailabilityResponse replace(UUID doctorId, WeeklyAvailabilityRequest request) {
		Doctor doctor = requireDoctor(doctorId);
		requireCanManage(doctor, currentAccountService.requireCurrent());
		List<WeeklyTimeRange> ranges = validateAndSort(request.intervals());

		intervalRepository.deleteAllByDoctorId(doctorId);
		List<DoctorWeeklyInterval> intervals = ranges.stream()
				.map(range -> new DoctorWeeklyInterval(
						doctor, range.dayOfWeek(), range.startTime(), range.endTime()))
				.toList();
		intervalRepository.saveAll(intervals);
		return response(doctorId, intervals);
	}

	private Doctor requireDoctor(UUID doctorId) {
		return doctorRepository.findById(doctorId)
				.orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
	}

	private static void requireCanManage(Doctor doctor, UserAccount account) {
		if (account.getRole() == AccountRole.RECEPTIONIST || account.getRole() == AccountRole.ADMIN) {
			return;
		}
		if (account.getRole() == AccountRole.DOCTOR
				&& account.getDoctor() != null
				&& account.getDoctor().getId().equals(doctor.getId())) {
			return;
		}
		throw new AccessDeniedException("Account cannot manage this doctor's availability");
	}

	private static List<WeeklyTimeRange> validateAndSort(List<WeeklyTimeRange> requested) {
		List<WeeklyTimeRange> ranges = new ArrayList<>(
				Objects.requireNonNull(requested, "intervals must not be null"));
		ranges.sort(RANGE_ORDER);
		WeeklyTimeRange previous = null;
		for (WeeklyTimeRange range : ranges) {
			if (!range.endTime().isAfter(range.startTime())) {
				throw new BusinessRuleException("Each availability end time must be after its start time.");
			}
			if (previous != null
					&& previous.dayOfWeek() == range.dayOfWeek()
					&& range.startTime().isBefore(previous.endTime())) {
				throw new BusinessRuleException("Weekly availability intervals must not overlap.");
			}
			previous = range;
		}
		return List.copyOf(ranges);
	}

	private static WeeklyAvailabilityResponse response(
			UUID doctorId, List<DoctorWeeklyInterval> intervals) {
		List<WeeklyTimeRange> ranges = intervals.stream()
				.map(WeeklyTimeRange::from)
				.sorted(RANGE_ORDER)
				.toList();
		return new WeeklyAvailabilityResponse(
				doctorId,
				TimeConfiguration.CLINIC_ZONE.getId(),
				ranges);
	}
}
