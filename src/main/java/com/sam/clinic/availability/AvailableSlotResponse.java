package com.sam.clinic.availability;

import java.time.Instant;

public record AvailableSlotResponse(Instant startAt, Instant endAt, int durationMinutes) {
}
