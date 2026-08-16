package com.sam.clinic.doctor;

import com.sam.clinic.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "doctors")
public class Doctor extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(nullable = false, length = 80)
	private String specialty;

	@Column(nullable = false)
	private boolean active = true;

	protected Doctor() {
		// Required by JPA.
	}

	public Doctor(String fullName, String specialty) {
		this.fullName = requireText(fullName, "fullName");
		this.specialty = requireText(specialty, "specialty");
	}

	public void deactivate() {
		active = false;
	}

	public UUID getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getSpecialty() {
		return specialty;
	}

	public boolean isActive() {
		return active;
	}

	private static String requireText(String value, String fieldName) {
		String text = Objects.requireNonNull(value, fieldName + " must not be null").trim();
		if (text.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return text;
	}
}
