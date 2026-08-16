package com.sam.clinic.appointment;

import com.sam.clinic.shared.error.ApiErrorCode;
import com.sam.clinic.shared.error.ConflictException;

public class AppointmentConflictException extends ConflictException {

	public AppointmentConflictException(String message) {
		super(ApiErrorCode.APPOINTMENT_CONFLICT, message);
	}
}
