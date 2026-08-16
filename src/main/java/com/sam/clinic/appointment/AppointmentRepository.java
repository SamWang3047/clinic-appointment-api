package com.sam.clinic.appointment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

	@EntityGraph(attributePaths = { "doctor", "patient" })
	@Query("select appointment from Appointment appointment where appointment.id = :appointmentId")
	Optional<Appointment> findDetailedById(@Param("appointmentId") UUID appointmentId);

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

	@Query("""
			select count(appointment) > 0
			from Appointment appointment
			where appointment.doctor.id = :doctorId
			  and appointment.status in :statuses
			  and appointment.startAt < :rangeEnd
			  and appointment.endAt > :rangeStart
			""")
	boolean existsDoctorOverlap(
			@Param("doctorId") UUID doctorId,
			@Param("statuses") Collection<AppointmentStatus> statuses,
			@Param("rangeStart") Instant rangeStart,
			@Param("rangeEnd") Instant rangeEnd);

	@Query("""
			select count(appointment) > 0
			from Appointment appointment
			where appointment.patient.id = :patientId
			  and appointment.status in :statuses
			  and appointment.startAt < :rangeEnd
			  and appointment.endAt > :rangeStart
			""")
	boolean existsPatientOverlap(
			@Param("patientId") UUID patientId,
			@Param("statuses") Collection<AppointmentStatus> statuses,
			@Param("rangeStart") Instant rangeStart,
			@Param("rangeEnd") Instant rangeEnd);

	@EntityGraph(attributePaths = { "doctor", "patient" })
	@Query("""
			select appointment
			from Appointment appointment
			where (:patientId is null or appointment.patient.id = :patientId)
			  and (:doctorId is null or appointment.doctor.id = :doctorId)
			  and appointment.startAt >= :fromAt
			  and appointment.startAt < :toAt
			  and appointment.status in :statuses
			order by appointment.startAt asc, appointment.id asc
			""")
	Page<Appointment> search(
			@Param("patientId") UUID patientId,
			@Param("doctorId") UUID doctorId,
			@Param("fromAt") Instant fromAt,
			@Param("toAt") Instant toAt,
			@Param("statuses") Collection<AppointmentStatus> statuses,
			Pageable pageable);
}
