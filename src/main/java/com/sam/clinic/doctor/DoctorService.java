package com.sam.clinic.doctor;

import com.sam.clinic.account.EmailAvailabilityService;
import com.sam.clinic.account.UserAccount;
import com.sam.clinic.account.UserAccountRepository;
import com.sam.clinic.shared.error.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorService {

	private final DoctorRepository doctorRepository;
	private final UserAccountRepository accountRepository;
	private final EmailAvailabilityService emailAvailabilityService;
	private final PasswordEncoder passwordEncoder;

	public DoctorService(
			DoctorRepository doctorRepository,
			UserAccountRepository accountRepository,
			EmailAvailabilityService emailAvailabilityService,
			PasswordEncoder passwordEncoder) {
		this.doctorRepository = doctorRepository;
		this.accountRepository = accountRepository;
		this.emailAvailabilityService = emailAvailabilityService;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<DoctorResponse> listActive() {
		return doctorRepository.findByActiveTrueOrderByFullNameAscIdAsc().stream()
				.map(DoctorResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public DoctorResponse getPublic(UUID doctorId) {
		return doctorRepository.findByIdAndActiveTrue(doctorId)
				.map(DoctorResponse::from)
				.orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public DoctorResponse create(CreateDoctorRequest request) {
		String email = emailAvailabilityService.requireAvailable(request.loginEmail());
		Doctor doctor = doctorRepository.save(new Doctor(request.fullName(), request.specialty()));
		String passwordHash = passwordEncoder.encode(request.initialPassword());
		accountRepository.save(UserAccount.forDoctor(email, passwordHash, doctor));
		return DoctorResponse.from(doctor);
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public DoctorResponse deactivate(UUID doctorId) {
		Doctor doctor = doctorRepository.findById(doctorId)
				.orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
		doctor.deactivate();
		return DoctorResponse.from(doctor);
	}
}
