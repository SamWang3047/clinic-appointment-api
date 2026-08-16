package com.sam.clinic.appointment;

import com.sam.clinic.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

	private final AppointmentService appointmentService;

	public AppointmentController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	@GetMapping
	AppointmentPageResponse searchClinic(
			@RequestParam(required = false) UUID patientId,
			@RequestParam(required = false) UUID doctorId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(name = "status", required = false) List<AppointmentStatus> statuses,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return appointmentService.searchClinic(patientId, doctorId, from, to, statuses, page, size);
	}

	@PostMapping
	ResponseEntity<AppointmentResponse> createForPatient(
			@Valid @RequestBody CreateStaffAppointmentRequest request,
			HttpServletRequest httpRequest) {
		AppointmentResponse appointment = appointmentService.createForPatient(
				request, CorrelationIdFilter.from(httpRequest));
		return ResponseEntity
				.created(URI.create("/api/v1/appointments/" + appointment.id()))
				.body(appointment);
	}

	@GetMapping("/{appointmentId}")
	AppointmentResponse get(@PathVariable UUID appointmentId) {
		return appointmentService.get(appointmentId);
	}

	@PostMapping("/{appointmentId}/confirm")
	AppointmentResponse confirm(@PathVariable UUID appointmentId, HttpServletRequest httpRequest) {
		return appointmentService.confirm(appointmentId, CorrelationIdFilter.from(httpRequest));
	}

	@PostMapping("/{appointmentId}/cancel")
	AppointmentResponse cancel(
			@PathVariable UUID appointmentId,
			@Valid @RequestBody(required = false) CancelAppointmentRequest request,
			HttpServletRequest httpRequest) {
		return appointmentService.cancel(
				appointmentId, request, CorrelationIdFilter.from(httpRequest));
	}
}
