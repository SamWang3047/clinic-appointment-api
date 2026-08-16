package com.sam.clinic.doctor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

	List<Doctor> findByActiveTrueOrderByFullNameAscIdAsc();

	Optional<Doctor> findByIdAndActiveTrue(UUID id);
}
