package com.sam.clinic.availability;

import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "doctor_weekly_intervals")
public class DoctorWeeklyInterval extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "doctor_id", nullable = false)
	private Doctor doctor;

	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", nullable = false, length = 9)
	private DayOfWeek dayOfWeek;

	@Column(name = "start_time", nullable = false)
	@JdbcTypeCode(SqlTypes.LOCAL_TIME)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	@JdbcTypeCode(SqlTypes.LOCAL_TIME)
	private LocalTime endTime;

	protected DoctorWeeklyInterval() {
		// Required by JPA.
	}

	public DoctorWeeklyInterval(Doctor doctor, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
		this.doctor = Objects.requireNonNull(doctor, "doctor must not be null");
		this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
		this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
		this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
		if (!endTime.isAfter(startTime)) {
			throw new IllegalArgumentException("endTime must be after startTime");
		}
	}

	public UUID getId() {
		return id;
	}

	public Doctor getDoctor() {
		return doctor;
	}

	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}
}
