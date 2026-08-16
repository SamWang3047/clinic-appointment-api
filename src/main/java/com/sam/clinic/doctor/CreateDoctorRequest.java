package com.sam.clinic.doctor;

import com.sam.clinic.shared.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDoctorRequest(
		@NotBlank @Size(max = 100) String fullName,
		@NotBlank @Size(max = 80) String specialty,
		@NotBlank @Email @Size(max = 255) String loginEmail,
		@ValidPassword String initialPassword) {

	public CreateDoctorRequest {
		fullName = trim(fullName);
		specialty = trim(specialty);
		loginEmail = trim(loginEmail);
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}
}
