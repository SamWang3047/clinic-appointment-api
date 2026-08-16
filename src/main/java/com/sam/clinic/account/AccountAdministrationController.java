package com.sam.clinic.account;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AccountAdministrationController {

	private final AccountAdministrationService administrationService;

	public AccountAdministrationController(AccountAdministrationService administrationService) {
		this.administrationService = administrationService;
	}

	@PostMapping
	ResponseEntity<AccountResponse> createStaff(@Valid @RequestBody CreateStaffAccountRequest request) {
		return ResponseEntity.status(201).body(administrationService.createStaff(request));
	}

	@PostMapping("/{accountId}/disable")
	AccountResponse disable(@PathVariable UUID accountId) {
		return administrationService.disable(accountId);
	}
}
