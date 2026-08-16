package com.sam.clinic.appointment;

import com.sam.clinic.doctor.Doctor;
import java.util.UUID;

public record DoctorSummary(UUID id, String fullName, String specialty) {

	static DoctorSummary from(Doctor doctor) {
		return new DoctorSummary(doctor.getId(), doctor.getFullName(), doctor.getSpecialty());
	}
}
