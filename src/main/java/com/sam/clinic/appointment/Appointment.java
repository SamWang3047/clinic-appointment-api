package com.sam.clinic.appointment;

import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.patient.Patient;
import com.sam.clinic.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment extends AuditableEntity {

	private static final Set<Long> ALLOWED_DURATIONS = Set.of(15L, 30L, 45L, 60L);

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "doctor_id", nullable = false)
	private Doctor doctor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private Patient patient;

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(name = "end_at", nullable = false)
	private Instant endAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AppointmentStatus status = AppointmentStatus.PENDING;

	@Column(nullable = false, length = 500)
	private String reason;

	@Column(name = "cancellation_reason", length = 500)
	private String cancellationReason;

	@Version
	@Column(nullable = false)
	private long version;

	protected Appointment() {
		// Required by JPA.
	}

	public Appointment(Doctor doctor, Patient patient, Instant startAt, Instant endAt, String reason) {
		this.doctor = Objects.requireNonNull(doctor, "doctor must not be null");
		this.patient = Objects.requireNonNull(patient, "patient must not be null");
		this.startAt = Objects.requireNonNull(startAt, "startAt must not be null");
		this.endAt = Objects.requireNonNull(endAt, "endAt must not be null");
		this.reason = requireText(reason, "reason");

		if (!endAt.isAfter(startAt)) {
			throw new IllegalArgumentException("endAt must be after startAt");
		}
		long durationMinutes = Duration.between(startAt, endAt).toMinutes();
		if (!ALLOWED_DURATIONS.contains(durationMinutes)
				|| !endAt.equals(startAt.plus(Duration.ofMinutes(durationMinutes)))) {
			throw new IllegalArgumentException("duration must be 15, 30, 45 or 60 minutes");
		}
		ZonedDateTime utcStart = startAt.atZone(ZoneOffset.UTC);
		if (utcStart.getMinute() % 15 != 0 || utcStart.getSecond() != 0 || utcStart.getNano() != 0) {
			throw new IllegalArgumentException("startAt must be on a 15-minute boundary");
		}
	}

	public void confirm(Instant now) {
		requirePending("confirmed");
		requireBeforeStart(now, "confirmed");
		status = AppointmentStatus.CONFIRMED;
	}

	public void decline(Instant now) {
		requirePending("declined");
		requireBeforeStart(now, "declined");
		status = AppointmentStatus.DECLINED;
	}

	public void cancel(Instant now, String cancellationReason) {
		if (!status.reservesTime()) {
			throw new InvalidAppointmentStateException("Only pending or confirmed appointments can be cancelled");
		}
		requireBeforeStart(now, "cancelled");
		status = AppointmentStatus.CANCELLED;
		this.cancellationReason = cancellationReason == null ? null : requireText(cancellationReason, "cancellationReason");
	}

	public void complete(Instant now) {
		if (status != AppointmentStatus.CONFIRMED) {
			throw new InvalidAppointmentStateException("Only confirmed appointments can be completed");
		}
		if (Objects.requireNonNull(now, "now must not be null").isBefore(startAt)) {
			throw new InvalidAppointmentStateException("An appointment cannot be completed before it starts");
		}
		status = AppointmentStatus.COMPLETED;
	}

	public UUID getId() {
		return id;
	}

	public Doctor getDoctor() {
		return doctor;
	}

	public Patient getPatient() {
		return patient;
	}

	public Instant getStartAt() {
		return startAt;
	}

	public Instant getEndAt() {
		return endAt;
	}

	public AppointmentStatus getStatus() {
		return status;
	}

	public String getReason() {
		return reason;
	}

	public String getCancellationReason() {
		return cancellationReason;
	}

	public long getVersion() {
		return version;
	}

	public int getDurationMinutes() {
		return Math.toIntExact(Duration.between(startAt, endAt).toMinutes());
	}

	public boolean reservesTime() {
		return status.reservesTime();
	}

	private void requirePending(String targetState) {
		if (status != AppointmentStatus.PENDING) {
			throw new InvalidAppointmentStateException("Only pending appointments can be " + targetState);
		}
	}

	private void requireBeforeStart(Instant now, String targetState) {
		if (!Objects.requireNonNull(now, "now must not be null").isBefore(startAt)) {
			throw new InvalidAppointmentStateException(
					"An appointment cannot be " + targetState + " after it starts");
		}
	}

	private static String requireText(String value, String fieldName) {
		String text = Objects.requireNonNull(value, fieldName + " must not be null").trim();
		if (text.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		if (text.length() > 500) {
			throw new IllegalArgumentException(fieldName + " must not exceed 500 characters");
		}
		return text;
	}
}
