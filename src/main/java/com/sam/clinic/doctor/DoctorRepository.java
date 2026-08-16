package com.sam.clinic.doctor;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

	List<Doctor> findByActiveTrueOrderByFullNameAscIdAsc();

	Optional<Doctor> findByIdAndActiveTrue(UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select doctor from Doctor doctor where doctor.id = :doctorId")
	Optional<Doctor> findByIdForUpdate(@Param("doctorId") UUID doctorId);
}
