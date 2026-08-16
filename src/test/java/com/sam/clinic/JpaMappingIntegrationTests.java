package com.sam.clinic;

import static org.assertj.core.api.Assertions.assertThat;

import com.sam.clinic.appointment.Appointment;
import com.sam.clinic.appointment.AppointmentRepository;
import com.sam.clinic.appointment.AppointmentStatus;
import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.doctor.DoctorRepository;
import com.sam.clinic.patient.Patient;
import com.sam.clinic.patient.PatientRepository;
import com.sam.clinic.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class JpaMappingIntegrationTests {

	private final DoctorRepository doctorRepository;
	private final PatientRepository patientRepository;
	private final AppointmentRepository appointmentRepository;
	private final EntityManager entityManager;

	@Autowired
	JpaMappingIntegrationTests(
			DoctorRepository doctorRepository,
			PatientRepository patientRepository,
			AppointmentRepository appointmentRepository,
			EntityManager entityManager) {
		this.doctorRepository = doctorRepository;
		this.patientRepository = patientRepository;
		this.appointmentRepository = appointmentRepository;
		this.entityManager = entityManager;
	}

	@Test
	void persistsAndReloadsTheCoreDomainModel() {
		Doctor doctor = doctorRepository.save(new Doctor("Dr Alice Chen", "General Practice"));
		Patient patient = patientRepository.save(
				new Patient("Sam Patient", "SAM.PATIENT@example.com", "+61 400 000 000"));
		Instant startAt = Instant.parse("2030-01-07T00:00:00Z");
		Instant endAt = startAt.plusSeconds(30 * 60);
		Appointment appointment = appointmentRepository.saveAndFlush(
				new Appointment(doctor, patient, startAt, endAt, "Initial consultation"));
		UUID appointmentId = appointment.getId();

		entityManager.clear();

		Appointment reloaded = appointmentRepository.findById(appointmentId).orElseThrow();

		assertThat(reloaded.getDoctor().getFullName()).isEqualTo("Dr Alice Chen");
		assertThat(reloaded.getPatient().getEmail()).isEqualTo("sam.patient@example.com");
		assertThat(reloaded.getStartAt()).isEqualTo(startAt);
		assertThat(reloaded.getEndAt()).isEqualTo(endAt);
		assertThat(reloaded.getStatus()).isEqualTo(AppointmentStatus.PENDING);
		assertThat(reloaded.getReason()).isEqualTo("Initial consultation");
		assertThat(reloaded.getCancellationReason()).isNull();
		assertThat(reloaded.getCreatedAt()).isNotNull();
		assertThat(reloaded.getUpdatedAt()).isNotNull();
	}
}
