package com.sam.clinic.shared.error;

import java.util.Objects;

public abstract class ApiException extends RuntimeException {

	private final ApiErrorCode errorCode;

	protected ApiException(ApiErrorCode errorCode, String message) {
		super(message);
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
	}

	public ApiErrorCode getErrorCode() {
		return errorCode;
	}
}
