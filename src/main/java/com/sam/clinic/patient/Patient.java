package com.sam.clinic.patient;

import com.sam.clinic.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class Patient extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, length = 32)
	private String phone;

	protected Patient() {
		// Required by JPA.
	}

	public Patient(String fullName, String email, String phone) {
		this.fullName = requireText(fullName, "fullName");
		this.email = requireText(email, "email").toLowerCase(Locale.ROOT);
		this.phone = requireText(phone, "phone");
	}

	public UUID getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	private static String requireText(String value, String fieldName) {
		String text = Objects.requireNonNull(value, fieldName + " must not be null").trim();
		if (text.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return text;
	}
}
