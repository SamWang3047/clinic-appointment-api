package com.sam.clinic.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sam.clinic.account.AccountRole;
import com.sam.clinic.account.UserAccount;
import com.sam.clinic.account.UserAccountRepository;
import com.sam.clinic.availability.DoctorWeeklyIntervalRepository;
import com.sam.clinic.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@ActiveProfiles("demo")
@TestPropertySource(properties = "clinic.demo.password=DemoPass123")
@Transactional
class DemoDataSeederIntegrationTests {

	private final UserAccountRepository accountRepository;
	private final DoctorWeeklyIntervalRepository intervalRepository;
	private final DemoDataSeeder demoDataSeeder;

	@Autowired
	DemoDataSeederIntegrationTests(
			UserAccountRepository accountRepository,
			DoctorWeeklyIntervalRepository intervalRepository,
			DemoDataSeeder demoDataSeeder) {
		this.accountRepository = accountRepository;
		this.intervalRepository = intervalRepository;
		this.demoDataSeeder = demoDataSeeder;
	}

	@Test
	void demoProfileSeedsFourRolesAndAWeekdaySchedule() {
		demoDataSeeder.seed();

		assertThat(accountRepository.findAll())
				.extracting(UserAccount::getRole)
				.containsExactlyInAnyOrder(
						AccountRole.PATIENT,
						AccountRole.DOCTOR,
						AccountRole.RECEPTIONIST,
						AccountRole.ADMIN);
		UserAccount doctor = accountRepository.findByEmail("doctor@demo.local").orElseThrow();
		assertThat(intervalRepository.findByDoctorId(doctor.getDoctor().getId())).hasSize(5);
	}
}
