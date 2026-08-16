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
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment extends AuditableEntity {

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
	private AppointmentStatus status = AppointmentStatus.BOOKED;

	@Column(nullable = false, length = 500)
	private String reason;

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
	}

	public void cancel() {
		if (status != AppointmentStatus.BOOKED) {
			throw new IllegalStateException("Only booked appointments can be cancelled");
		}
		status = AppointmentStatus.CANCELLED;
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

	public long getVersion() {
		return version;
	}

	private static String requireText(String value, String fieldName) {
		String text = Objects.requireNonNull(value, fieldName + " must not be null").trim();
		if (text.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return text;
	}
}
