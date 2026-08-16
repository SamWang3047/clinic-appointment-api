package com.sam.clinic.account;

import com.sam.clinic.patient.PatientRepository;
import com.sam.clinic.shared.error.ApiErrorCode;
import com.sam.clinic.shared.error.ConflictException;
import com.sam.clinic.shared.validation.EmailNormalizer;
import org.springframework.stereotype.Service;

@Service
public class EmailAvailabilityService {

	private final UserAccountRepository accountRepository;
	private final PatientRepository patientRepository;

	public EmailAvailabilityService(
			UserAccountRepository accountRepository,
			PatientRepository patientRepository) {
		this.accountRepository = accountRepository;
		this.patientRepository = patientRepository;
	}

	public String requireAvailable(String candidate) {
		String email = EmailNormalizer.normalize(candidate);
		if (accountRepository.existsByEmail(email) || patientRepository.existsByEmail(email)) {
			throw new ConflictException(
					ApiErrorCode.DUPLICATE_RESOURCE,
					"The request conflicts with an existing resource.");
		}
		return email;
	}
}
