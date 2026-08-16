package com.sam.clinic.shared.config;

import com.sam.clinic.account.AccountRole;
import com.sam.clinic.account.UserAccount;
import com.sam.clinic.account.UserAccountRepository;
import com.sam.clinic.availability.DoctorWeeklyInterval;
import com.sam.clinic.availability.DoctorWeeklyIntervalRepository;
import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.doctor.DoctorRepository;
import com.sam.clinic.patient.Patient;
import com.sam.clinic.patient.PatientRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoDataSeeder {

	private static final String PATIENT_EMAIL = "patient@demo.local";
	private static final String DOCTOR_EMAIL = "doctor@demo.local";
	private static final String RECEPTIONIST_EMAIL = "receptionist@demo.local";
	private static final String ADMIN_EMAIL = "admin@demo.local";

	private final UserAccountRepository accountRepository;
	private final PatientRepository patientRepository;
	private final DoctorRepository doctorRepository;
	private final DoctorWeeklyIntervalRepository intervalRepository;
	private final PasswordEncoder passwordEncoder;
	private final String demoPassword;

	public DemoDataSeeder(
			UserAccountRepository accountRepository,
			PatientRepository patientRepository,
			DoctorRepository doctorRepository,
			DoctorWeeklyIntervalRepository intervalRepository,
			PasswordEncoder passwordEncoder,
			@Value("${clinic.demo.password:}") String demoPassword) {
		this.accountRepository = accountRepository;
		this.patientRepository = patientRepository;
		this.doctorRepository = doctorRepository;
		this.intervalRepository = intervalRepository;
		this.passwordEncoder = passwordEncoder;
		this.demoPassword = demoPassword;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void seed() {
		if (demoPassword == null || demoPassword.length() < 10) {
			throw new IllegalStateException("DEMO_PASSWORD must contain at least 10 characters");
		}
		ensurePatientAccount();
		Doctor doctor = ensureDoctorAccount();
		createStaffAccountIfMissing(RECEPTIONIST_EMAIL, AccountRole.RECEPTIONIST);
		createStaffAccountIfMissing(ADMIN_EMAIL, AccountRole.ADMIN);
		createWorkingWeekIfMissing(doctor);
	}

	private void ensurePatientAccount() {
		accountRepository.findByEmail(PATIENT_EMAIL).ifPresentOrElse(account -> {
			if (account.getRole() != AccountRole.PATIENT || account.getPatient() == null) {
				throw new IllegalStateException("Demo patient email belongs to an incompatible account");
			}
		}, () -> {
			Patient patient = patientRepository.findByEmail(PATIENT_EMAIL)
					.orElseGet(() -> patientRepository.save(
							new Patient("Sam Demo Patient", PATIENT_EMAIL, "0400000000")));
			accountRepository.save(UserAccount.forPatient(PATIENT_EMAIL, encodedPassword(), patient));
		});
	}

	private Doctor ensureDoctorAccount() {
		UserAccount existing = accountRepository.findByEmail(DOCTOR_EMAIL).orElse(null);
		if (existing != null) {
			if (existing.getRole() != AccountRole.DOCTOR || existing.getDoctor() == null) {
				throw new IllegalStateException("Demo doctor email belongs to an incompatible account");
			}
			return existing.getDoctor();
		}
		Doctor doctor = doctorRepository.save(
				new Doctor("Dr Ada Lovelace", "General Practice"));
		accountRepository.save(UserAccount.forDoctor(DOCTOR_EMAIL, encodedPassword(), doctor));
		return doctor;
	}

	private void createStaffAccountIfMissing(String email, AccountRole role) {
		if (!accountRepository.existsByEmail(email)) {
			accountRepository.save(UserAccount.forStaff(email, encodedPassword(), role));
		}
	}

	private void createWorkingWeekIfMissing(Doctor doctor) {
		if (!intervalRepository.findByDoctorId(doctor.getId()).isEmpty()) {
			return;
		}
		List<DoctorWeeklyInterval> intervals = List.of(
				workingDay(doctor, DayOfWeek.MONDAY),
				workingDay(doctor, DayOfWeek.TUESDAY),
				workingDay(doctor, DayOfWeek.WEDNESDAY),
				workingDay(doctor, DayOfWeek.THURSDAY),
				workingDay(doctor, DayOfWeek.FRIDAY));
		intervalRepository.saveAll(intervals);
	}

	private DoctorWeeklyInterval workingDay(Doctor doctor, DayOfWeek dayOfWeek) {
		return new DoctorWeeklyInterval(doctor, dayOfWeek, LocalTime.of(9, 0), LocalTime.of(17, 0));
	}

	private String encodedPassword() {
		return passwordEncoder.encode(demoPassword);
	}
}
