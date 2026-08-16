package com.sam.clinic.appointment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sam.clinic.patient.Patient;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatientSummary(UUID id, String fullName, String email, String phone) {

	static PatientSummary from(Patient patient, boolean includeContact) {
		return new PatientSummary(
				patient.getId(),
				patient.getFullName(),
				includeContact ? patient.getEmail() : null,
				includeContact ? patient.getPhone() : null);
	}
}
