package com.sam.clinic.account;

import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MyProfileController {

	private final MyProfileService profileService;

	public MyProfileController(MyProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping("/profile")
	Object getMyProfile(JwtAuthenticationToken authentication) {
		return profileService.findForAccount(UUID.fromString(authentication.getName()));
	}
}
