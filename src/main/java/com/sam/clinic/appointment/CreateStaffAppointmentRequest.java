package com.sam.clinic.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateStaffAppointmentRequest(
		@NotNull UUID patientId,
		@NotNull UUID doctorId,
		@NotNull Instant startAt,
		@NotNull Integer durationMinutes,
		@NotBlank @Size(max = 500) String reason) {
}
