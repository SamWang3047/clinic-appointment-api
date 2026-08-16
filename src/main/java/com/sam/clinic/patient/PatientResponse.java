package com.sam.clinic.patient;

import java.util.UUID;

public record PatientResponse(
		UUID id,
		String fullName,
		String email,
		String phone,
		boolean hasLoginAccount) {

	public static PatientResponse from(Patient patient, boolean hasLoginAccount) {
		return new PatientResponse(
				patient.getId(),
				patient.getFullName(),
				patient.getEmail(),
				patient.getPhone(),
				hasLoginAccount);
	}
}
