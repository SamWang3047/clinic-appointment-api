package com.sam.clinic.account;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	Optional<UserAccount> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByPatientId(UUID patientId);

	@EntityGraph(attributePaths = { "patient", "doctor" })
	Optional<UserAccount> findWithProfilesById(UUID id);
}
