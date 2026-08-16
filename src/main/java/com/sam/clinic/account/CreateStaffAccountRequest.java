package com.sam.clinic.account;

import com.sam.clinic.shared.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStaffAccountRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@ValidPassword String initialPassword,
		@NotBlank @Pattern(regexp = "RECEPTIONIST|ADMIN", message = "must be RECEPTIONIST or ADMIN") String role) {

	public CreateStaffAccountRequest {
		email = email == null ? null : email.trim();
		role = role == null ? null : role.trim();
	}
}
