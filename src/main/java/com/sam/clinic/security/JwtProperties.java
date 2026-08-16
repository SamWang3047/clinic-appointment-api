package com.sam.clinic.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("clinic.security.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl) {

	public JwtProperties {
		issuer = issuer == null || issuer.isBlank() ? "clinic-appointment-api" : issuer;
		accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(30) : accessTokenTtl;
		if (accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
			throw new IllegalArgumentException("JWT access token TTL must be positive");
		}
	}
}
