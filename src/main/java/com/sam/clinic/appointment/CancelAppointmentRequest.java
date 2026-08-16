package com.sam.clinic.appointment;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(
		@Size(max = 500)
		@Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
		String reason) {
}
