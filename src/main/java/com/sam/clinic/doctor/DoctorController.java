package com.sam.clinic.doctor;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

	private final DoctorService doctorService;

	public DoctorController(DoctorService doctorService) {
		this.doctorService = doctorService;
	}

	@GetMapping
	List<DoctorResponse> listActive() {
		return doctorService.listActive();
	}

	@GetMapping("/{doctorId}")
	DoctorResponse getPublic(@PathVariable UUID doctorId) {
		return doctorService.getPublic(doctorId);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	ResponseEntity<DoctorResponse> create(@Valid @RequestBody CreateDoctorRequest request) {
		DoctorResponse doctor = doctorService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/doctors/" + doctor.id())).body(doctor);
	}

	@PostMapping("/{doctorId}/deactivate")
	@PreAuthorize("hasRole('ADMIN')")
	DoctorResponse deactivate(@PathVariable UUID doctorId) {
		return doctorService.deactivate(doctorId);
	}
}
