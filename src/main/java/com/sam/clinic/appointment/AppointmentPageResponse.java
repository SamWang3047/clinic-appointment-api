package com.sam.clinic.appointment;

import java.util.List;

public record AppointmentPageResponse(
		List<AppointmentResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
