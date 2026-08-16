package com.sam.clinic.appointment;

import com.sam.clinic.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/me/appointments")
public class MyAppointmentController {

	private final AppointmentService appointmentService;

	public MyAppointmentController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	@GetMapping
	AppointmentPageResponse listMine(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(name = "status", required = false) List<AppointmentStatus> statuses,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return appointmentService.listMine(from, to, statuses, page, size);
	}

	@PostMapping
	ResponseEntity<AppointmentResponse> create(
			@Valid @RequestBody CreateSelfAppointmentRequest request,
			HttpServletRequest httpRequest) {
		AppointmentResponse appointment = appointmentService.createForCurrentPatient(
				request, CorrelationIdFilter.from(httpRequest));
		return ResponseEntity
				.created(URI.create("/api/v1/appointments/" + appointment.id()))
				.body(appointment);
	}
}
