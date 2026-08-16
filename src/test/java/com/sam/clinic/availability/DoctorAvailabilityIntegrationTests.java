package com.sam.clinic.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sam.clinic.account.AccountRole;
import com.sam.clinic.account.JwtTokenService;
import com.sam.clinic.account.UserAccount;
import com.sam.clinic.account.UserAccountRepository;
import com.sam.clinic.appointment.Appointment;
import com.sam.clinic.appointment.AppointmentRepository;
import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.doctor.DoctorRepository;
import com.sam.clinic.patient.Patient;
import com.sam.clinic.patient.PatientRepository;
import com.sam.clinic.shared.config.TimeConfiguration;
import com.sam.clinic.support.IntegrationTest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class DoctorAvailabilityIntegrationTests {

	private static final String PASSWORD = "securePass123";

	private final MockMvc mockMvc;
	private final DoctorRepository doctorRepository;
	private final PatientRepository patientRepository;
	private final UserAccountRepository accountRepository;
	private final DoctorWeeklyIntervalRepository intervalRepository;
	private final AppointmentRepository appointmentRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService tokenService;

	@Autowired
	DoctorAvailabilityIntegrationTests(
			MockMvc mockMvc,
			DoctorRepository doctorRepository,
			PatientRepository patientRepository,
			UserAccountRepository accountRepository,
			DoctorWeeklyIntervalRepository intervalRepository,
			AppointmentRepository appointmentRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenService tokenService) {
		this.mockMvc = mockMvc;
		this.doctorRepository = doctorRepository;
		this.patientRepository = patientRepository;
		this.accountRepository = accountRepository;
		this.intervalRepository = intervalRepository;
		this.appointmentRepository = appointmentRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	@Test
	void adminReplacesWeeklyHoursAndPublicCanListQuarterHourSlots() throws Exception {
		Doctor doctor = doctorRepository.save(new Doctor("Dr Ada Lovelace", "General Practice"));
		String adminToken = staffToken("admin@example.com", AccountRole.ADMIN);

		mockMvc.perform(put("/api/v1/doctors/{doctorId}/availability/weekly", doctor.getId())
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(weeklyHoursJson()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.doctorId").value(doctor.getId().toString()))
				.andExpect(jsonPath("$.timeZone").value("Australia/Melbourne"))
				.andExpect(jsonPath("$.intervals[0].startTime").value("09:00"));

		mockMvc.perform(get("/api/v1/doctors/{doctorId}/available-slots", doctor.getId())
				.param("date", futureMonday().toString())
				.param("durationMinutes", "30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(11))
				.andExpect(jsonPath("$[0].durationMinutes").value(30));
	}

	@Test
	void overlappingWeeklyIntervalsAreRejectedWithoutReplacingExistingHours() throws Exception {
		Doctor doctor = doctorRepository.save(new Doctor("Dr Ada Lovelace", "General Practice"));
		String adminToken = staffToken("admin@example.com", AccountRole.ADMIN);
		intervalRepository.save(new DoctorWeeklyInterval(
				doctor, futureMonday().getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(9, 0)));

		mockMvc.perform(put("/api/v1/doctors/{doctorId}/availability/weekly", doctor.getId())
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "intervals": [
						    {"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00"},
						    {"dayOfWeek":"MONDAY","startTime":"11:00","endTime":"13:00"}
						  ]
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

		assertThat(intervalRepository.findByDoctorId(doctor.getId())).hasSize(1);
	}

	@Test
	void doctorCannotManageAnotherDoctorsHours() throws Exception {
		Doctor assignedDoctor = doctorRepository.save(new Doctor("Dr Grace Hopper", "General Practice"));
		Doctor otherDoctor = doctorRepository.save(new Doctor("Dr Alan Turing", "General Practice"));
		UserAccount doctorAccount = accountRepository.save(UserAccount.forDoctor(
				"grace@example.com", passwordEncoder.encode(PASSWORD), assignedDoctor));

		mockMvc.perform(put("/api/v1/doctors/{doctorId}/availability/weekly", otherDoctor.getId())
				.header("Authorization", "Bearer " + tokenService.issue(doctorAccount).accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content(weeklyHoursJson()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

	@Test
	void pendingAppointmentsRemoveEveryOverlappingCandidateSlot() throws Exception {
		Doctor doctor = doctorRepository.save(new Doctor("Dr Ada Lovelace", "General Practice"));
		Patient patient = patientRepository.save(
				new Patient("Sam Patient", "sam@example.com", "0400000000"));
		LocalDate monday = futureMonday();
		intervalRepository.save(new DoctorWeeklyInterval(
				doctor, monday.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(12, 0)));
		var blockedStart = monday.atTime(10, 0).atZone(TimeConfiguration.CLINIC_ZONE).toInstant();
		appointmentRepository.save(new Appointment(
				doctor, patient, blockedStart, blockedStart.plusSeconds(30 * 60), "Consultation"));

		mockMvc.perform(get("/api/v1/doctors/{doctorId}/available-slots", doctor.getId())
				.param("date", monday.toString())
				.param("durationMinutes", "30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(8));
	}

	private String staffToken(String email, AccountRole role) {
		UserAccount account = accountRepository.save(
				UserAccount.forStaff(email, passwordEncoder.encode(PASSWORD), role));
		return tokenService.issue(account).accessToken();
	}

	private static LocalDate futureMonday() {
		return LocalDate.now(TimeConfiguration.CLINIC_ZONE)
				.plusWeeks(1)
				.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
	}

	private static String weeklyHoursJson() {
		return """
				{
				  "intervals": [
				    {"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00"}
				  ]
				}
				""";
	}
}
