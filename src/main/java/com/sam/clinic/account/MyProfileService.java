package com.sam.clinic.account;

import com.sam.clinic.doctor.DoctorResponse;
import com.sam.clinic.patient.PatientResponse;
import com.sam.clinic.shared.error.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyProfileService {

	private final UserAccountRepository accountRepository;

	public MyProfileService(UserAccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Transactional(readOnly = true)
	public Object findForAccount(UUID accountId) {
		UserAccount account = accountRepository.findWithProfilesById(accountId)
				.filter(UserAccount::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
		return switch (account.getRole()) {
			case PATIENT -> PatientResponse.from(account.getPatient(), true);
			case DOCTOR -> DoctorResponse.from(account.getDoctor());
			case RECEPTIONIST, ADMIN -> throw new ResourceNotFoundException("Profile not found");
		};
	}
}
