package com.sam.clinic.shared.error;

public class InvalidCredentialsException extends ApiException {

	public InvalidCredentialsException() {
		super(ApiErrorCode.INVALID_CREDENTIALS, "The email or password is invalid.");
	}
}
