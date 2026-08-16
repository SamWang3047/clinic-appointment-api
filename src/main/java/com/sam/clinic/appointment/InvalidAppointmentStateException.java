package com.sam.clinic.appointment;

import com.sam.clinic.shared.error.ApiErrorCode;
import com.sam.clinic.shared.error.ConflictException;

public class InvalidAppointmentStateException extends ConflictException {

	public InvalidAppointmentStateException(String message) {
		super(ApiErrorCode.INVALID_STATE_TRANSITION, message);
	}
}
