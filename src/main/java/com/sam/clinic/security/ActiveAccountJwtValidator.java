package com.sam.clinic.security;

import com.sam.clinic.account.UserAccountRepository;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

final class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_ACCOUNT = new OAuth2Error(
			"invalid_token", "The token account is unavailable.", null);

	private final UserAccountRepository accountRepository;

	ActiveAccountJwtValidator(UserAccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		try {
			UUID accountId = UUID.fromString(token.getSubject());
			return accountRepository.findById(accountId)
					.filter(account -> account.isActive() && account.getRole().name().equals(token.getClaimAsString("role")))
					.map(account -> OAuth2TokenValidatorResult.success())
					.orElseGet(() -> OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT));
		}
		catch (IllegalArgumentException exception) {
			return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
		}
	}
}
