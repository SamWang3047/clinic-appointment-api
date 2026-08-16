package com.sam.clinic.appointment;

import com.sam.clinic.account.AccountRole;
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
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "appointment_audit_events")
public class AppointmentAuditEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "appointment_id", nullable = false)
	private Appointment appointment;

	@Column(name = "actor_account_id", nullable = false)
	private UUID actorAccountId;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_role", nullable = false, length = 20)
	private AccountRole actorRole;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AppointmentAction action;

	@Enumerated(EnumType.STRING)
	@Column(name = "previous_status", length = 20)
	private AppointmentStatus previousStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "new_status", length = 20)
	private AppointmentStatus newStatus;

	@Column(name = "previous_start_at")
	private Instant previousStartAt;

	@Column(name = "previous_end_at")
	private Instant previousEndAt;

	@Column(name = "new_start_at")
	private Instant newStartAt;

	@Column(name = "new_end_at")
	private Instant newEndAt;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	@Column(name = "correlation_id", nullable = false, updatable = false, length = 64)
	private String correlationId;

	protected AppointmentAuditEvent() {
		// Required by JPA.
	}

	private AppointmentAuditEvent(
			Appointment appointment,
			UUID actorAccountId,
			AccountRole actorRole,
			AppointmentAction action,
			AppointmentStatus previousStatus,
			AppointmentStatus newStatus,
			Instant previousStartAt,
			Instant previousEndAt,
			Instant newStartAt,
			Instant newEndAt,
			Instant occurredAt,
			String correlationId) {
		this.appointment = Objects.requireNonNull(appointment, "appointment must not be null");
		this.actorAccountId = Objects.requireNonNull(actorAccountId, "actorAccountId must not be null");
		this.actorRole = Objects.requireNonNull(actorRole, "actorRole must not be null");
		this.action = Objects.requireNonNull(action, "action must not be null");
		this.previousStatus = previousStatus;
		this.newStatus = newStatus;
		this.previousStartAt = previousStartAt;
		this.previousEndAt = previousEndAt;
		this.newStartAt = newStartAt;
		this.newEndAt = newEndAt;
		this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		this.correlationId = requireText(correlationId, "correlationId");
	}

	public static AppointmentAuditEvent created(
			Appointment appointment,
			UUID actorAccountId,
			AccountRole actorRole,
			Instant occurredAt,
			String correlationId) {
		return new AppointmentAuditEvent(
				appointment,
				actorAccountId,
				actorRole,
				AppointmentAction.CREATED,
				null,
				appointment.getStatus(),
				null,
				null,
				appointment.getStartAt(),
				appointment.getEndAt(),
				occurredAt,
				correlationId);
	}

	public static AppointmentAuditEvent statusChanged(
			Appointment appointment,
			UUID actorAccountId,
			AccountRole actorRole,
			AppointmentAction action,
			AppointmentStatus previousStatus,
			Instant occurredAt,
			String correlationId) {
		return new AppointmentAuditEvent(
				appointment,
				actorAccountId,
				actorRole,
				action,
				previousStatus,
				appointment.getStatus(),
				appointment.getStartAt(),
				appointment.getEndAt(),
				appointment.getStartAt(),
				appointment.getEndAt(),
				occurredAt,
				correlationId);
	}

	public UUID getId() {
		return id;
	}

	public Appointment getAppointment() {
		return appointment;
	}

	public UUID getActorAccountId() {
		return actorAccountId;
	}

	public AccountRole getActorRole() {
		return actorRole;
	}

	public AppointmentAction getAction() {
		return action;
	}

	public AppointmentStatus getPreviousStatus() {
		return previousStatus;
	}

	public AppointmentStatus getNewStatus() {
		return newStatus;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	private static String requireText(String value, String fieldName) {
		String text = Objects.requireNonNull(value, fieldName + " must not be null").trim();
		if (text.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		if (text.length() > 64) {
			throw new IllegalArgumentException(fieldName + " must not exceed 64 characters");
		}
		return text;
	}
}
