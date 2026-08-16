package com.sam.clinic.appointment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

	@Query("""
			select appointment
			from Appointment appointment
			where appointment.doctor.id = :doctorId
			  and appointment.status in :statuses
			  and appointment.startAt < :rangeEnd
			  and appointment.endAt > :rangeStart
			order by appointment.startAt asc, appointment.id asc
			""")
	List<Appointment> findDoctorAppointmentsOverlapping(
			@Param("doctorId") UUID doctorId,
			@Param("statuses") Collection<AppointmentStatus> statuses,
			@Param("rangeStart") Instant rangeStart,
			@Param("rangeEnd") Instant rangeEnd);
}
