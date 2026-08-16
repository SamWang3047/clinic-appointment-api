package com.sam.clinic.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TimeConfigurationTests {

	private final TimeConfiguration configuration = new TimeConfiguration();

	@Test
	void suppliesUtcBusinessClockAndMelbourneClinicZone() {
		Clock clock = configuration.clock();
		ZoneId clinicZone = configuration.clinicZoneId();

		assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
		assertThat(clinicZone).isEqualTo(ZoneId.of("Australia/Melbourne"));
	}
}
