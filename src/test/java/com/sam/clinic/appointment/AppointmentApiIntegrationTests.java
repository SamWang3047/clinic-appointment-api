package com.sam.clinic.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sam.clinic.account.AccountRole;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class AppointmentApiIntegrationTests {

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
	AppointmentApiIntegrationTests(
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
	void patientBooksDoctorConfirmsAndPatientCancelsWithCompleteAuditTrail() throws Exception {
		LocalDate monday = futureMonday();
		Instant startAt = atClinicTime(monday, 10, 0);
		Doctor doctor = doctorWithHours("Dr Ada Lovelace", monday);
		UserAccount doctorAccount = doctorAccount("ada@example.com", doctor);
		UserAccount patientAccount = patientAccount("Sam Patient", "sam@example.com");

		MvcResult created = mockMvc.perform(post("/api/v1/me/appointments")
				.header("Authorization", bearer(patientAccount))
				.header("X-Correlation-ID", "demo-create-001")
				.contentType(MediaType.APPLICATION_JSON)
				.content(selfCreateJson(doctor.getId(), startAt, "Recurring headaches")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/appointments/")))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.reason").value("Recurring headaches"))
				.andExpect(jsonPath("$.patient.email").value("sam@example.com"))
				.andExpect(jsonPath("$.version").doesNotExist())
				.andReturn();
		UUID appointmentId = UUID.fromString(extractJsonString(
				created.getResponse().getContentAsString(), "id"));

		mockMvc.perform(get("/api/v1/me/appointments")
				.header("Authorization", bearer(patientAccount)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.items[0].id").value(appointmentId.toString()));
		mockMvc.perform(get("/api/v1/me/appointments")
				.header("Authorization", bearer(patientAccount))
				.param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/appointments/{appointmentId}/confirm", appointmentId)
				.header("Authorization", bearer(doctorAccount))
				.header("X-Correlation-ID", "demo-confirm-001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.patient.phone").value("0400000000"));

		mockMvc.perform(post("/api/v1/appointments/{appointmentId}/cancel", appointmentId)
				.header("Authorization", bearer(patientAccount))
				.header("X-Correlation-ID", "demo-cancel-001")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Symptoms resolved\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.cancellationReason").value("Symptoms resolved"));

		List<AppointmentAuditEvent> events = auditRepository
				.findByAppointmentIdOrderByOccurredAtAscIdAsc(appointmentId);
		assertThat(events)
				.extracting(AppointmentAuditEvent::getAction)
				.containsExactly(AppointmentAction.CREATED, AppointmentAction.CONFIRMED, AppointmentAction.CANCELLED);
		assertThat(events)
				.extracting(AppointmentAuditEvent::getCorrelationId)
				.containsExactly("demo-create-001", "demo-confirm-001", "demo-cancel-001");

		String availableSlots = mockMvc.perform(get(
				"/api/v1/doctors/{doctorId}/available-slots", doctor.getId())
				.param("date", monday.toString())
				.param("durationMinutes", "30"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(availableSlots).contains(startAt.toString());
	}

	@Test
	void doctorAndPatientOverlapsReturnStableConflicts() throws Exception {
		LocalDate monday = futureMonday();
		Instant startAt = atClinicTime(monday, 10, 0);
		Doctor firstDoctor = doctorWithHours("Dr First", monday);
		Doctor secondDoctor = doctorWithHours("Dr Second", monday);
		UserAccount firstPatient = patientAccount("First Patient", "first@example.com");
		UserAccount secondPatient = patientAccount("Second Patient", "second@example.com");

		createSelf(firstPatient, firstDoctor, startAt).andExpect(status().isCreated());

		createSelf(secondPatient, firstDoctor, startAt.plusSeconds(15 * 60))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("APPOINTMENT_CONFLICT"));
		createSelf(firstPatient, secondDoctor, startAt)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("APPOINTMENT_CONFLICT"));
		createSelf(secondPatient, secondDoctor, atClinicTime(monday, 18, 0))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("APPOINTMENT_CONFLICT"));

		assertThat(appointmentRepository.count()).isOne();
		assertThat(auditRepository.count()).isOne();
	}

	@Test
	void accessAndPrivacyRulesHideAppointmentsAndClinicalReasons() throws Exception {
		LocalDate monday = futureMonday();
		Instant startAt = atClinicTime(monday, 10, 0);
		Doctor assignedDoctor = doctorWithHours("Dr Assigned", monday);
		Doctor unrelatedDoctor = doctorWithHours("Dr Unrelated", monday);
		UserAccount assignedDoctorAccount = doctorAccount("assigned@example.com", assignedDoctor);
		UserAccount unrelatedDoctorAccount = doctorAccount("unrelated@example.com", unrelatedDoctor);
		UserAccount owner = patientAccount("Owner", "owner@example.com");
		UserAccount stranger = patientAccount("Stranger", "stranger@example.com");
		UserAccount admin = staffAccount("admin@example.com", AccountRole.ADMIN);

		MvcResult created = createSelf(owner, assignedDoctor, startAt)
				.andExpect(status().isCreated())
				.andReturn();
		UUID appointmentId = UUID.fromString(extractJsonString(
				created.getResponse().getContentAsString(), "id"));

		mockMvc.perform(get("/api/v1/appointments/{appointmentId}", appointmentId)
				.header("Authorization", bearer(stranger)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/appointments/{appointmentId}/confirm", appointmentId)
				.header("Authorization", bearer(unrelatedDoctorAccount)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/appointments/{appointmentId}/confirm", appointmentId)
				.header("Authorization", bearer(assignedDoctorAccount)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/appointments/{appointmentId}", appointmentId)
				.header("Authorization", bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reason").doesNotExist())
				.andExpect(jsonPath("$.cancellationReason").doesNotExist())
				.andExpect(jsonPath("$.patient.email").doesNotExist())
				.andExpect(jsonPath("$.patient.phone").doesNotExist());
	}

	@Test
	void receptionistCanBookAndStaffCancellationRequiresAReason() throws Exception {
		LocalDate monday = futureMonday();
		Instant startAt = atClinicTime(monday, 11, 0);
		Doctor doctor = doctorWithHours("Dr Ada Lovelace", monday);
		Patient patient = patientRepository.save(
				new Patient("Walk-in Patient", "walkin@example.com", "0411111111"));
		UserAccount receptionist = staffAccount("reception@example.com", AccountRole.RECEPTIONIST);

		MvcResult created = mockMvc.perform(post("/api/v1/appointments")
				.header("Authorization", bearer(receptionist))
				.contentType(MediaType.APPLICATION_JSON)
				.content(staffCreateJson(patient.getId(), doctor.getId(), startAt)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.patient.email").value("walkin@example.com"))
				.andReturn();
		UUID appointmentId = UUID.fromString(extractJsonString(
				created.getResponse().getContentAsString(), "id"));

		mockMvc.perform(post("/api/v1/appointments/{appointmentId}/cancel", appointmentId)
				.header("Authorization", bearer(receptionist)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

		mockMvc.perform(post("/api/v1/appointments/{appointmentId}/cancel", appointmentId)
				.header("Authorization", bearer(receptionist))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Doctor is unavailable\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	private org.springframework.test.web.servlet.ResultActions createSelf(
			UserAccount patient, Doctor doctor, Instant startAt) throws Exception {
		return mockMvc.perform(post("/api/v1/me/appointments")
				.header("Authorization", bearer(patient))
				.contentType(MediaType.APPLICATION_JSON)
				.content(selfCreateJson(doctor.getId(), startAt, "Consultation")));
	}

	private Doctor doctorWithHours(String name, LocalDate date) {
		Doctor doctor = doctorRepository.save(new Doctor(name, "General Practice"));
		intervalRepository.save(new DoctorWeeklyInterval(
				doctor, date.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(17, 0)));
		return doctor;
	}

	private UserAccount patientAccount(String name, String email) {
		Patient patient = patientRepository.save(new Patient(name, email, "0400000000"));
		return accountRepository.save(UserAccount.forPatient(
				email, passwordEncoder.encode(PASSWORD), patient));
	}

	private UserAccount doctorAccount(String email, Doctor doctor) {
		return accountRepository.save(UserAccount.forDoctor(
				email, passwordEncoder.encode(PASSWORD), doctor));
	}

	private UserAccount staffAccount(String email, AccountRole role) {
		return accountRepository.save(UserAccount.forStaff(
				email, passwordEncoder.encode(PASSWORD), role));
	}

	private String bearer(UserAccount account) {
		return "Bearer " + tokenService.issue(account).accessToken();
	}

	private static LocalDate futureMonday() {
		return LocalDate.now(TimeConfiguration.CLINIC_ZONE)
				.plusWeeks(1)
				.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
	}

	private static Instant atClinicTime(LocalDate date, int hour, int minute) {
		return date.atTime(hour, minute).atZone(TimeConfiguration.CLINIC_ZONE).toInstant();
	}

	private static String selfCreateJson(UUID doctorId, Instant startAt, String reason) {
		return """
				{
				  "doctorId": "%s",
				  "startAt": "%s",
				  "durationMinutes": 30,
				  "reason": "%s"
				}
				""".formatted(doctorId, startAt, reason);
	}

	private static String staffCreateJson(UUID patientId, UUID doctorId, Instant startAt) {
		return """
				{
				  "patientId": "%s",
				  "doctorId": "%s",
				  "startAt": "%s",
				  "durationMinutes": 30,
				  "reason": "Consultation"
				}
				""".formatted(patientId, doctorId, startAt);
	}

	private static String extractJsonString(String json, String field) {
		String marker = "\"" + field + "\":\"";
		int start = json.indexOf(marker) + marker.length();
		int end = json.indexOf('"', start);
		return json.substring(start, end);
	}
}
