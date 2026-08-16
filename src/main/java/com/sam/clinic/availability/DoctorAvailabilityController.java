package com.sam.clinic.availability;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctors/{doctorId}")
public class DoctorAvailabilityController {

	private final WeeklyAvailabilityService weeklyAvailabilityService;
	private final AvailableSlotService availableSlotService;

	public DoctorAvailabilityController(
			WeeklyAvailabilityService weeklyAvailabilityService,
			AvailableSlotService availableSlotService) {
		this.weeklyAvailabilityService = weeklyAvailabilityService;
		this.availableSlotService = availableSlotService;
	}

	@GetMapping("/available-slots")
	List<AvailableSlotResponse> listAvailableSlots(
			@PathVariable UUID doctorId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam int durationMinutes) {
		return availableSlotService.list(doctorId, date, durationMinutes);
	}

	@GetMapping("/availability/weekly")
	WeeklyAvailabilityResponse getWeekly(@PathVariable UUID doctorId) {
		return weeklyAvailabilityService.get(doctorId);
	}

	@PutMapping("/availability/weekly")
	WeeklyAvailabilityResponse replaceWeekly(
			@PathVariable UUID doctorId,
			@Valid @RequestBody WeeklyAvailabilityRequest request) {
		return weeklyAvailabilityService.replace(doctorId, request);
	}
}
