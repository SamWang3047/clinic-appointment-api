package com.sam.clinic.account;

import java.util.UUID;

public record AccountResponse(UUID id, String email, AccountRole role, boolean active) {

	public static AccountResponse from(UserAccount account) {
		return new AccountResponse(
				account.getId(),
				account.getEmail(),
				account.getRole(),
				account.isActive());
	}
}
