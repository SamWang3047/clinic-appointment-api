package com.sam.clinic.shared.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sam.clinic.shared.error.ApiErrorHandlingTests.ErrorProbeController;
import com.sam.clinic.shared.web.CorrelationIdFilter;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
		controllers = ErrorProbeController.class,
		excludeAutoConfiguration = {
				SecurityAutoConfiguration.class,
				OAuth2ResourceServerAutoConfiguration.class
		})
@Import({
		GlobalExceptionHandler.class,
		ProblemDetailsFactory.class,
		CorrelationIdFilter.class,
		ErrorProbeController.class
})
class ApiErrorHandlingTests {

	private static final String CORRELATION_ID = "test-correlation-123";

	private final MockMvc mockMvc;

	@Autowired
	ApiErrorHandlingTests(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void returnsStableProblemDetailsAndPreservesSafeCorrelationId() throws Exception {
		mockMvc.perform(get("/test/errors/not-found")
				.header(CorrelationIdFilter.HEADER_NAME, CORRELATION_ID))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(header().string(CorrelationIdFilter.HEADER_NAME, CORRELATION_ID))
				.andExpect(jsonPath("$.type").value("urn:problem:clinic:resource-not-found"))
				.andExpect(jsonPath("$.title").value("Resource not found"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("Appointment not found"))
				.andExpect(jsonPath("$.instance").value("/test/errors/not-found"))
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
				.andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
	}

	@Test
	void reportsValidationViolationsWithoutEchoingRejectedValues() throws Exception {
		mockMvc.perform(post("/test/errors/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.violations[0].field").value("name"))
				.andExpect(jsonPath("$.violations[0].message").value("must not be blank"))
				.andExpect(jsonPath("$.violations[0].rejectedValue").doesNotExist());
	}

	@Test
	void reportsMethodParameterViolationsAsBadRequests() throws Exception {
		mockMvc.perform(get("/test/errors/method-validation").queryParam("size", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.violations[0].field").value("size"))
				.andExpect(jsonPath("$.violations[0].message").value("must be greater than 0"));
	}

	@Test
	void mapsUnknownRoutesToSafeNotFoundProblems() throws Exception {
		mockMvc.perform(get("/test/no-such-route"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("The requested resource was not found."));
	}

	@Test
	void replacesUnsafeCorrelationIdBeforeUsingItInHeadersOrProblems() throws Exception {
		MvcResult result = mockMvc.perform(get("/test/errors/not-found")
				.header(CorrelationIdFilter.HEADER_NAME, "unsafe header value"))
				.andExpect(status().isNotFound())
				.andReturn();

		String generatedId = result.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);
		assertThat(generatedId)
				.isNotEqualTo("unsafe header value")
				.matches("[0-9a-f-]{36}");
		assertThat(result.getResponse().getContentAsString()).contains(generatedId);
	}

	@Test
	void hidesUnexpectedExceptionDetails() throws Exception {
		Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		try {
			mockMvc.perform(get("/test/errors/unexpected"))
					.andExpect(status().isInternalServerError())
					.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
					.andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
					.andExpect(content().string(org.hamcrest.Matchers.not(
							org.hamcrest.Matchers.containsString("database-password"))));
		}
		finally {
			logger.detachAppender(appender);
			appender.stop();
		}

		assertThat(appender.list)
				.extracting(ILoggingEvent::getFormattedMessage)
				.noneMatch(message -> message.contains("database-password"));
		assertThat(appender.list)
				.extracting(ILoggingEvent::getThrowableProxy)
				.containsOnlyNulls();
	}

	@RestController
	@RequestMapping("/test/errors")
	public static class ErrorProbeController {

		@GetMapping("/not-found")
		String notFound() {
			throw new ResourceNotFoundException("Appointment not found");
		}

		@PostMapping("/validation")
		void validate(@Valid @RequestBody ProbeRequest request) {
		}

		@GetMapping("/method-validation")
		String methodValidation(@RequestParam @Positive int size) {
			return Integer.toString(size);
		}

		@GetMapping("/unexpected")
		String unexpected() {
			throw new IllegalStateException("database-password must stay private");
		}
	}

	record ProbeRequest(@NotBlank String name) {
	}
}
