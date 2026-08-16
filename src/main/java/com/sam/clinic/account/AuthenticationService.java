package com.sam.clinic.account;

import com.sam.clinic.shared.error.InvalidCredentialsException;
import com.sam.clinic.shared.validation.EmailNormalizer;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

	private final UserAccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService tokenService;
	private final String dummyPasswordHash;

	public AuthenticationService(
			UserAccountRepository accountRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenService tokenService) {
		this.accountRepository = accountRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		String email = EmailNormalizer.normalize(request.email());
		UserAccount account = accountRepository.findByEmail(email).orElse(null);
		boolean passwordMatches = account == null
				? passwordEncoder.matches(request.password(), dummyPasswordHash)
				: passwordEncoder.matches(request.password(), account.passwordHash());

		if (account == null || !passwordMatches || !account.isActive()) {
			throw new InvalidCredentialsException();
		}
		return tokenService.issue(account);
	}
}
