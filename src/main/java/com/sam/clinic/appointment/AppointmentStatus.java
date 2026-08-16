package com.sam.clinic.appointment;

public enum AppointmentStatus {
	PENDING,
	CONFIRMED,
	DECLINED,
	CANCELLED,
	COMPLETED;

	public boolean reservesTime() {
		return this == PENDING || this == CONFIRMED;
	}
}
