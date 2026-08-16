package com.sam.clinic.shared.error;

public class ResourceNotFoundException extends ApiException {

	public ResourceNotFoundException(String message) {
		super(ApiErrorCode.RESOURCE_NOT_FOUND, message);
	}
}
