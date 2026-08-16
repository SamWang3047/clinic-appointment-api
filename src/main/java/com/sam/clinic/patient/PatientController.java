package com.sam.clinic.patient;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

	private final PatientService patientService;

	public PatientController(PatientService patientService) {
		this.patientService = patientService;
	}

	@PostMapping("/register")
	ResponseEntity<PatientResponse> register(@Valid @RequestBody RegisterPatientRequest request) {
		PatientResponse patient = patientService.register(request);
		return ResponseEntity.created(URI.create("/api/v1/me/profile")).body(patient);
	}

	@PostMapping
	@PreAuthorize("hasRole('RECEPTIONIST')")
	ResponseEntity<PatientResponse> createProfile(@Valid @RequestBody CreatePatientProfileRequest request) {
		PatientResponse patient = patientService.createProfile(request);
		return ResponseEntity.created(URI.create("/api/v1/patients/" + patient.id())).body(patient);
	}
}
