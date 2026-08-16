package com.sam.clinic.account;

import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.patient.Patient;
import com.sam.clinic.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccount extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "login_email", nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountRole role;

	@Column(nullable = false)
	private boolean active = true;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "patient_id", unique = true)
	private Patient patient;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "doctor_id", unique = true)
	private Doctor doctor;

	protected UserAccount() {
		// Required by JPA.
	}

	private UserAccount(
			String email,
			String passwordHash,
			AccountRole role,
			Patient patient,
			Doctor doctor) {
		this.email = normalizeEmail(email);
		this.passwordHash = requireText(passwordHash, "passwordHash");
		this.role = Objects.requireNonNull(role, "role must not be null");
		this.patient = patient;
		this.doctor = doctor;
		validateProfileLink();
	}

	public static UserAccount forPatient(String email, String passwordHash, Patient patient) {
		return new UserAccount(email, passwordHash, AccountRole.PATIENT,
				Objects.requireNonNull(patient, "patient must not be null"), null);
	}

	public static UserAccount forDoctor(String email, String passwordHash, Doctor doctor) {
		return new UserAccount(email, passwordHash, AccountRole.DOCTOR,
				null, Objects.requireNonNull(doctor, "doctor must not be null"));
	}

	public static UserAccount forStaff(String email, String passwordHash, AccountRole role) {
		if (role != AccountRole.RECEPTIONIST && role != AccountRole.ADMIN) {
			throw new IllegalArgumentException("Staff role must be RECEPTIONIST or ADMIN");
		}
		return new UserAccount(email, passwordHash, role, null, null);
	}

	public void disable() {
		active = false;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public AccountRole getRole() {
		return role;
	}

	public boolean isActive() {
		return active;
	}

	public Patient getPatient() {
		return patient;
	}

	public Doctor getDoctor() {
		return doctor;
	}

	String passwordHash() {
		return passwordHash;
	}

	private void validateProfileLink() {
		boolean valid = switch (role) {
			case PATIENT -> patient != null && doctor == null;
			case DOCTOR -> doctor != null && patient == null;
			case RECEPTIONIST, ADMIN -> patient == null && doctor == null;
		};
		if (!valid) {
			throw new IllegalArgumentException("Account profile link does not match its role");
		}
	}

	private static String normalizeEmail(String value) {
		return requireText(value, "email").toLowerCase(Locale.ROOT);
	}

	private static String requireText(String value, String fieldName) {
		String text = Objects.requireNonNull(value, fieldName + " must not be null").trim();
		if (text.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return text;
	}
}
