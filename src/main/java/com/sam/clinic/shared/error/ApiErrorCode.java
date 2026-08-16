package com.sam.clinic.shared.error;

import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;

public enum ApiErrorCode {

	MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
	BUSINESS_RULE_VIOLATION(HttpStatus.BAD_REQUEST, "Business rule violation"),
	AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "Authentication required"),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied"),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
	DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "Duplicate resource"),
	APPOINTMENT_CONFLICT(HttpStatus.CONFLICT, "Appointment conflict"),
	INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "Invalid state transition"),
	CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "Concurrent modification"),
	IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "Idempotency key reused"),
	IDEMPOTENCY_REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "Idempotent request in progress"),
	RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

	private static final String TYPE_PREFIX = "urn:problem:clinic:";

	private final HttpStatus status;
	private final String title;

	ApiErrorCode(HttpStatus status, String title) {
		this.status = status;
		this.title = title;
	}

	public HttpStatus status() {
		return status;
	}

	public String title() {
		return title;
	}

	public URI type() {
		return URI.create(TYPE_PREFIX + name().toLowerCase(Locale.ROOT).replace('_', '-'));
	}
}
