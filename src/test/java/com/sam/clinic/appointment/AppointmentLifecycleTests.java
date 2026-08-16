package com.sam.clinic.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.patient.Patient;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AppointmentLifecycleTests {

	private static final Instant START_AT = Instant.parse("2030-01-07T01:00:00Z");
	private static final Instant BEFORE_START = START_AT.minusSeconds(60);

	@Test
	void newAppointmentIsPendingAndCanBeConfirmedThenCompleted() {
		Appointment appointment = appointment();

		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
		assertThat(appointment.reservesTime()).isTrue();

		appointment.confirm(BEFORE_START);
		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

		appointment.complete(START_AT);
		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
		assertThat(appointment.reservesTime()).isFalse();
	}

	@Test
	void pendingAppointmentCanBeDeclinedOrCancelled() {
		Appointment declined = appointment();
		declined.decline(BEFORE_START);
		assertThat(declined.getStatus()).isEqualTo(AppointmentStatus.DECLINED);

		Appointment cancelled = appointment();
		cancelled.cancel(BEFORE_START, "Patient is unavailable");
		assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
		assertThat(cancelled.getCancellationReason()).isEqualTo("Patient is unavailable");
	}

	@Test
	void terminalAndStartedAppointmentsRejectInvalidTransitions() {
		Appointment appointment = appointment();
		appointment.confirm(BEFORE_START);

		assertThatThrownBy(() -> appointment.confirm(BEFORE_START))
				.isInstanceOf(InvalidAppointmentStateException.class)
				.hasMessage("Only pending appointments can be confirmed");
		assertThatThrownBy(() -> appointment.cancel(START_AT, "Too late"))
				.isInstanceOf(InvalidAppointmentStateException.class)
				.hasMessage("An appointment cannot be cancelled after it starts");
	}

	@Test
	void constructorEnforcesSupportedDurationAndQuarterHourBoundary() {
		assertThatThrownBy(() -> new Appointment(
				doctor(), patient(), START_AT, START_AT.plusSeconds(20 * 60), "Consultation"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("duration must be");
		assertThatThrownBy(() -> new Appointment(
				doctor(), patient(), START_AT.plusSeconds(60), START_AT.plusSeconds(31 * 60), "Consultation"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("15-minute boundary");
	}

	private static Appointment appointment() {
		return new Appointment(doctor(), patient(), START_AT, START_AT.plusSeconds(30 * 60), "Consultation");
	}

	private static Doctor doctor() {
		return new Doctor("Dr Ada Lovelace", "General Practice");
	}

	private static Patient patient() {
		return new Patient("Sam Patient", "sam.patient@example.com", "0400000000");
	}
}
