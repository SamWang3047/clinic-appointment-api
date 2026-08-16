package com.sam.clinic.availability;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WeeklyAvailabilityRequest(@NotNull List<@Valid WeeklyTimeRange> intervals) {
}
