package com.sam.clinic.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sam.clinic.account.AccountRole;
import com.sam.clinic.account.JwtTokenService;
import com.sam.clinic.account.TokenResponse;
import com.sam.clinic.account.UserAccount;
import com.sam.clinic.account.UserAccountRepository;
import com.sam.clinic.doctor.DoctorRepository;
import com.sam.clinic.patient.PatientRepository;
import com.sam.clinic.support.IntegrationTest;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class AuthenticationIntegrationTests {

	private static final String PATIENT_EMAIL = "sam.patient@example.com";
	private static final String PASSWORD = "securePass123";

	private final MockMvc mockMvc;
	private final UserAccountRepository accountRepository;
	private final PatientRepository patientRepository;
	private final DoctorRepository doctorRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService tokenService;

	@Autowired
	AuthenticationIntegrationTests(
			MockMvc mockMvc,
			UserAccountRepository accountRepository,
			PatientRepository patientRepository,
			DoctorRepository doctorRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenService tokenService) {
		this.mockMvc = mockMvc;
		this.accountRepository = accountRepository;
		this.patientRepository = patientRepository;
		this.doctorRepository = doctorRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	@Test
	void registrationAtomicallyCreatesNormalisedPatientAccountWithoutSecrets() throws Exception {
		MvcResult result = register("  Sam.Patient@Example.COM  ")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/me/profile"))
				.andExpect(jsonPath("$.email").value(PATIENT_EMAIL))
				.andExpect(jsonPath("$.hasLoginAccount").value(true))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andReturn();

		UserAccount account = accountRepository.findByEmail(PATIENT_EMAIL).orElseThrow();
		assertThat(account.getRole()).isEqualTo(AccountRole.PATIENT);
		assertThat(account.getPatient().getEmail()).isEqualTo(PATIENT_EMAIL);
		assertThat(result.getResponse().getContentAsString()).doesNotContain(PASSWORD);
	}

	@Test
	void duplicateRegistrationIsCaseInsensitiveAndLeavesOneAccountAndProfile() throws Exception {
		register(PATIENT_EMAIL).andExpect(status().isCreated());

		register("  " + PATIENT_EMAIL.toUpperCase(Locale.ROOT) + "  ")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"))
				.andExpect(jsonPath("$.detail").value("The request conflicts with an existing resource."));

		assertThat(accountRepository.count()).isOne();
		assertThat(patientRepository.count()).isOne();
	}

	@Test
	void publicRegistrationRejectsRoleInjectionAndWeakPasswords() throws Exception {
		mockMvc.perform(post("/api/v1/patients/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Sam Wang",
						  "email": "sam@example.com",
						  "phone": "0400000000",
						  "password": "securePass123",
						  "role": "ADMIN"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));

		mockMvc.perform(post("/api/v1/patients/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Sam Wang",
						  "email": "sam@example.com",
						  "phone": "0400000000",
						  "password": "lettersOnly"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.violations[0].field").value("password"));

		assertThat(accountRepository.count()).isZero();
		assertThat(patientRepository.count()).isZero();
	}

	@Test
	void validLoginReturnsThirtyMinuteJwtAndOwnProfile() throws Exception {
		register(PATIENT_EMAIL).andExpect(status().isCreated());
		String token = login(PATIENT_EMAIL, PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(1800))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andReturn()
				.getResponse()
				.getContentAsString();
		token = extractJsonString(token, "accessToken");

		mockMvc.perform(get("/api/v1/me/profile").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(PATIENT_EMAIL))
				.andExpect(jsonPath("$.hasLoginAccount").value(true));
	}

	@Test
	void invalidLoginDoesNotRevealWhetherTheAccountExists() throws Exception {
		register(PATIENT_EMAIL).andExpect(status().isCreated());

		login("unknown@example.com", PASSWORD)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.detail").value("The email or password is invalid."));
		login(PATIENT_EMAIL, "incorrect123")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.detail").value("The email or password is invalid."));
	}

	@Test
	void missingAndDisabledAccountTokensAreRejectedWithSafeProblems() throws Exception {
		mockMvc.perform(get("/api/v1/me/profile"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));

		register(PATIENT_EMAIL).andExpect(status().isCreated());
		UserAccount account = accountRepository.findByEmail(PATIENT_EMAIL).orElseThrow();
		TokenResponse token = tokenService.issue(account);
		account.disable();
		accountRepository.flush();

		mockMvc.perform(get("/api/v1/me/profile")
				.header("Authorization", "Bearer " + token.accessToken()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void roleRulesDenyReceptionistButAllowAdminToCreateDoctor() throws Exception {
		String receptionistToken = createStaffToken("reception@example.com", AccountRole.RECEPTIONIST);
		mockMvc.perform(post("/api/v1/doctors")
				.header("Authorization", "Bearer " + receptionistToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(doctorJson("blocked.doctor@example.com")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

		String adminToken = createStaffToken("admin@example.com", AccountRole.ADMIN);
		mockMvc.perform(post("/api/v1/doctors")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(doctorJson("doctor@example.com")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fullName").value("Dr Ada Lovelace"))
				.andExpect(jsonPath("$.loginEmail").doesNotExist())
				.andExpect(jsonPath("$.initialPassword").doesNotExist());

		assertThat(doctorRepository.count()).isOne();
		mockMvc.perform(get("/api/v1/doctors"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].fullName").value("Dr Ada Lovelace"));
	}

	@Test
	void receptionistCreatesPatientProfileWithoutLoginAccount() throws Exception {
		String receptionistToken = createStaffToken("reception@example.com", AccountRole.RECEPTIONIST);

		mockMvc.perform(post("/api/v1/patients")
				.header("Authorization", "Bearer " + receptionistToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "New Patient",
						  "email": "new.patient@example.com",
						  "phone": "0411111111"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.hasLoginAccount").value(false))
				.andExpect(jsonPath("$.password").doesNotExist());

		assertThat(patientRepository.count()).isOne();
		assertThat(accountRepository.count()).isOne();
	}

	@Test
	void adminCreatesStaffAndDisablingItInvalidatesAnIssuedToken() throws Exception {
		String adminToken = createStaffToken("admin@example.com", AccountRole.ADMIN);
		MvcResult created = mockMvc.perform(post("/api/v1/admin/accounts")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "reception@example.com",
						  "initialPassword": "securePass123",
						  "role": "RECEPTIONIST"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("RECEPTIONIST"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.initialPassword").doesNotExist())
				.andReturn();
		String accountId = extractJsonString(created.getResponse().getContentAsString(), "id");
		String receptionistToken = extractJsonString(
				login("reception@example.com", PASSWORD).andReturn().getResponse().getContentAsString(),
				"accessToken");

		mockMvc.perform(post("/api/v1/admin/accounts/{accountId}/disable", accountId)
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));

		mockMvc.perform(post("/api/v1/patients")
				.header("Authorization", "Bearer " + receptionistToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Blocked Request",
						  "email": "blocked@example.com",
						  "phone": "0422222222"
						}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
	}

	private org.springframework.test.web.servlet.ResultActions register(String email) throws Exception {
		return mockMvc.perform(post("/api/v1/patients/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Sam Wang",
						  "email": "%s",
						  "phone": "0400000000",
						  "password": "%s"
						}
						""".formatted(email, PASSWORD)));
	}

	private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "%s",
						  "password": "%s"
						}
						""".formatted(email, password)));
	}

	private String createStaffToken(String email, AccountRole role) {
		UserAccount account = accountRepository.save(
				UserAccount.forStaff(email, passwordEncoder.encode(PASSWORD), role));
		return tokenService.issue(account).accessToken();
	}

	private static String doctorJson(String email) {
		return """
				{
				  "fullName": "Dr Ada Lovelace",
				  "specialty": "General Practice",
				  "loginEmail": "%s",
				  "initialPassword": "%s"
				}
				""".formatted(email, PASSWORD);
	}

	private static String extractJsonString(String json, String field) {
		String marker = "\"" + field + "\":\"";
		int start = json.indexOf(marker) + marker.length();
		int end = json.indexOf('"', start);
		return json.substring(start, end);
	}
}
