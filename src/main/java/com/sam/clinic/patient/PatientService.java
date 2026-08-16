package com.sam.clinic.patient;

import com.sam.clinic.account.EmailAvailabilityService;
import com.sam.clinic.account.UserAccount;
import com.sam.clinic.account.UserAccountRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

	private final PatientRepository patientRepository;
	private final UserAccountRepository accountRepository;
	private final EmailAvailabilityService emailAvailabilityService;
	private final PasswordEncoder passwordEncoder;

	public PatientService(
			PatientRepository patientRepository,
			UserAccountRepository accountRepository,
			EmailAvailabilityService emailAvailabilityService,
			PasswordEncoder passwordEncoder) {
		this.patientRepository = patientRepository;
		this.accountRepository = accountRepository;
		this.emailAvailabilityService = emailAvailabilityService;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public PatientResponse register(RegisterPatientRequest request) {
		String email = emailAvailabilityService.requireAvailable(request.email());
		Patient patient = patientRepository.save(new Patient(request.fullName(), email, request.phone()));
		String passwordHash = passwordEncoder.encode(request.password());
		accountRepository.save(UserAccount.forPatient(email, passwordHash, patient));
		return PatientResponse.from(patient, true);
	}

	@Transactional
	@PreAuthorize("hasRole('RECEPTIONIST')")
	public PatientResponse createProfile(CreatePatientProfileRequest request) {
		String email = emailAvailabilityService.requireAvailable(request.email());
		Patient patient = patientRepository.save(new Patient(request.fullName(), email, request.phone()));
		return PatientResponse.from(patient, false);
	}
}
