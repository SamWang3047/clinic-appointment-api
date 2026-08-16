package com.sam.clinic.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.sam.clinic.account.JwtTokenService;
import com.sam.clinic.account.UserAccount;
import com.sam.clinic.account.UserAccountRepository;
import com.sam.clinic.availability.DoctorWeeklyInterval;
import com.sam.clinic.availability.DoctorWeeklyIntervalRepository;
import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.doctor.DoctorRepository;
import com.sam.clinic.patient.Patient;
import com.sam.clinic.patient.PatientRepository;
import com.sam.clinic.shared.config.TimeConfiguration;
import com.sam.clinic.support.IntegrationTest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AppointmentConcurrencyIntegrationTests {

	private static final String PASSWORD = "securePass123";

	private final MockMvc mockMvc;
	private final DoctorRepository doctorRepository;
	private final PatientRepository patientRepository;
	private final UserAccountRepository accountRepository;
	private final DoctorWeeklyIntervalRepository intervalRepository;
	private final AppointmentRepository appointmentRepository;
	private final AppointmentAuditEventRepository auditRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService tokenService;

	@Autowired
	AppointmentConcurrencyIntegrationTests(
			MockMvc mockMvc,
			DoctorRepository doctorRepository,
			PatientRepository patientRepository,
			UserAccountRepository accountRepository,
			DoctorWeeklyIntervalRepository intervalRepository,
			AppointmentRepository appointmentRepository,
			AppointmentAuditEventRepository auditRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenService tokenService) {
		this.mockMvc = mockMvc;
		this.doctorRepository = doctorRepository;
		this.patientRepository = patientRepository;
		this.accountRepository = accountRepository;
		this.intervalRepository = intervalRepository;
		this.appointmentRepository = appointmentRepository;
		this.auditRepository = auditRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	@Test
	void concurrentRequestsForSameDoctorAndTimeProduceOneBookingAndOneConflict() throws Exception {
		LocalDate date = LocalDate.now(TimeConfiguration.CLINIC_ZONE)
				.plusWeeks(1)
				.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
		Instant startAt = date.atTime(10, 0).atZone(TimeConfiguration.CLINIC_ZONE).toInstant();
		Doctor doctor = doctorRepository.save(new Doctor("Dr Concurrent", "General Practice"));
		intervalRepository.save(new DoctorWeeklyInterval(
				doctor, date.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(17, 0)));
		UserAccount firstPatient = patientAccount("First", "first.concurrent@example.com");
		UserAccount secondPatient = patientAccount("Second", "second.concurrent@example.com");

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Future<Integer> first = executor.submit(booking(ready, start, firstPatient, doctor, startAt));
			Future<Integer> second = executor.submit(booking(ready, start, secondPatient, doctor, startAt));
			ready.await();
			start.countDown();

			assertThat(List.of(first.get(), second.get()))
					.containsExactlyInAnyOrder(201, 409);
		}

		assertThat(appointmentRepository.count()).isOne();
		assertThat(auditRepository.count()).isOne();
	}

	private Callable<Integer> booking(
			CountDownLatch ready,
			CountDownLatch start,
			UserAccount patient,
			Doctor doctor,
			Instant startAt) {
		String token = tokenService.issue(patient).accessToken();
		return () -> {
			ready.countDown();
			start.await();
			return mockMvc.perform(post("/api/v1/me/appointments")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "doctorId": "%s",
							  "startAt": "%s",
							  "durationMinutes": 30,
							  "reason": "Concurrent request"
							}
							""".formatted(doctor.getId(), startAt)))
					.andReturn()
					.getResponse()
					.getStatus();
		};
	}

	private UserAccount patientAccount(String name, String email) {
		Patient patient = patientRepository.save(new Patient(name, email, "0400000000"));
		return accountRepository.saveAndFlush(UserAccount.forPatient(
				email, passwordEncoder.encode(PASSWORD), patient));
	}
}
