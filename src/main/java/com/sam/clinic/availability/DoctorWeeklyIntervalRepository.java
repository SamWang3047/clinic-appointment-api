package com.sam.clinic.availability;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorWeeklyIntervalRepository extends JpaRepository<DoctorWeeklyInterval, UUID> {

	List<DoctorWeeklyInterval> findByDoctorId(UUID doctorId);

	List<DoctorWeeklyInterval> findByDoctorIdAndDayOfWeek(UUID doctorId, DayOfWeek dayOfWeek);

	@Modifying(flushAutomatically = true)
	@Query("delete from DoctorWeeklyInterval weeklyInterval where weeklyInterval.doctor.id = :doctorId")
	void deleteAllByDoctorId(@Param("doctorId") UUID doctorId);
}
