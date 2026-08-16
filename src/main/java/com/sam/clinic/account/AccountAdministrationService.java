package com.sam.clinic.account;

import com.sam.clinic.shared.error.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAdministrationService {

	private final UserAccountRepository accountRepository;
	private final EmailAvailabilityService emailAvailabilityService;
	private final PasswordEncoder passwordEncoder;

	public AccountAdministrationService(
			UserAccountRepository accountRepository,
			EmailAvailabilityService emailAvailabilityService,
			PasswordEncoder passwordEncoder) {
		this.accountRepository = accountRepository;
		this.emailAvailabilityService = emailAvailabilityService;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public AccountResponse createStaff(CreateStaffAccountRequest request) {
		String email = emailAvailabilityService.requireAvailable(request.email());
		AccountRole role = AccountRole.valueOf(request.role());
		String passwordHash = passwordEncoder.encode(request.initialPassword());
		UserAccount account = accountRepository.save(UserAccount.forStaff(email, passwordHash, role));
		return AccountResponse.from(account);
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public AccountResponse disable(UUID accountId) {
		UserAccount account = accountRepository.findById(accountId)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found"));
		account.disable();
		return AccountResponse.from(account);
	}
}
