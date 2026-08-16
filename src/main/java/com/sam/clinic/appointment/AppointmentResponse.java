package com.sam.clinic.appointment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sam.clinic.account.AccountRole;
import com.sam.clinic.account.UserAccount;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppointmentResponse(
		UUID id,
		DoctorSummary doctor,
		PatientSummary patient,
		Instant startAt,
		Instant endAt,
		int durationMinutes,
		AppointmentStatus status,
		String reason,
		String cancellationReason,
		Instant createdAt,
		Instant updatedAt) {

	static AppointmentResponse from(Appointment appointment, UserAccount viewer) {
		boolean patientOwner = viewer.getRole() == AccountRole.PATIENT
				&& viewer.getPatient() != null
				&& viewer.getPatient().getId().equals(appointment.getPatient().getId());
		boolean assignedDoctor = viewer.getRole() == AccountRole.DOCTOR
				&& viewer.getDoctor() != null
				&& viewer.getDoctor().getId().equals(appointment.getDoctor().getId());
		boolean receptionist = viewer.getRole() == AccountRole.RECEPTIONIST;
		boolean maySeeSensitive = patientOwner || assignedDoctor || receptionist;

		return new AppointmentResponse(
				appointment.getId(),
				DoctorSummary.from(appointment.getDoctor()),
				PatientSummary.from(appointment.getPatient(), maySeeSensitive),
				appointment.getStartAt(),
				appointment.getEndAt(),
				appointment.getDurationMinutes(),
				appointment.getStatus(),
				maySeeSensitive ? appointment.getReason() : null,
				maySeeSensitive ? appointment.getCancellationReason() : null,
				appointment.getCreatedAt(),
				appointment.getUpdatedAt());
	}
}
