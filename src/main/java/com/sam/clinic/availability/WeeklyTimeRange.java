package com.sam.clinic.availability;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record WeeklyTimeRange(
		@NotNull DayOfWeek dayOfWeek,
		@NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
		@NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime) {

	public static WeeklyTimeRange from(DoctorWeeklyInterval interval) {
		return new WeeklyTimeRange(interval.getDayOfWeek(), interval.getStartTime(), interval.getEndTime());
	}
}
