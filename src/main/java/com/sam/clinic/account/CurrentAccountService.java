package com.sam.clinic.account;

import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentAccountService {

	private final UserAccountRepository accountRepository;

	public CurrentAccountService(UserAccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Transactional(readOnly = true)
	public UserAccount requireCurrent() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AuthenticationCredentialsNotFoundException("Authentication required");
		}
		try {
			UUID accountId = UUID.fromString(authentication.getName());
			return accountRepository.findWithProfilesById(accountId)
					.filter(UserAccount::isActive)
					.orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authentication required"));
		}
		catch (IllegalArgumentException exception) {
			throw new AuthenticationCredentialsNotFoundException("Authentication required", exception);
		}
	}
}
