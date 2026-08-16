package com.sam.clinic.shared.error;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

	public ConflictException(ApiErrorCode errorCode, String message) {
		super(requireConflictCode(errorCode), message);
	}

	private static ApiErrorCode requireConflictCode(ApiErrorCode errorCode) {
		if (errorCode == null || errorCode.status() != HttpStatus.CONFLICT) {
			throw new IllegalArgumentException("A conflict exception requires an HTTP 409 error code");
		}
		return errorCode;
	}
}
