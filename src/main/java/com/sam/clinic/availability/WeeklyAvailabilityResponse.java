package com.sam.clinic.availability;

import java.util.List;
import java.util.UUID;

public record WeeklyAvailabilityResponse(UUID doctorId, String timeZone, List<WeeklyTimeRange> intervals) {
}
