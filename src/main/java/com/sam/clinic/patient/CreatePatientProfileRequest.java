package com.sam.clinic.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePatientProfileRequest(
		@NotBlank @Size(max = 100) String fullName,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 32) String phone) {

	public CreatePatientProfileRequest {
		fullName = trim(fullName);
		email = trim(email);
		phone = trim(phone);
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}
}
