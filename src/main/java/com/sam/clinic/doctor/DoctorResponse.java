package com.sam.clinic.doctor;

import java.util.UUID;

public record DoctorResponse(UUID id, String fullName, String specialty, boolean active) {

	public static DoctorResponse from(Doctor doctor) {
		return new DoctorResponse(
				doctor.getId(),
				doctor.getFullName(),
				doctor.getSpecialty(),
				doctor.isActive());
	}
}
