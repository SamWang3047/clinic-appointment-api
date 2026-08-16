package com.sam.clinic.shared.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

	public static final ZoneId CLINIC_ZONE = ZoneId.of("Australia/Melbourne");

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	ZoneId clinicZoneId() {
		return CLINIC_ZONE;
	}
}
