package com.sam.clinic.security;

import com.sam.clinic.shared.error.ApiErrorCode;
import com.sam.clinic.shared.error.ProblemDetailsFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityProblemWriter {

	private final ProblemDetailsFactory problemDetailsFactory;
	private final ObjectMapper objectMapper;

	public SecurityProblemWriter(ProblemDetailsFactory problemDetailsFactory, ObjectMapper objectMapper) {
		this.problemDetailsFactory = problemDetailsFactory;
		this.objectMapper = objectMapper;
	}

	public void write(
			HttpServletRequest request,
			HttpServletResponse response,
			ApiErrorCode errorCode,
			String detail) throws IOException {
		ProblemDetail problem = problemDetailsFactory.create(errorCode, detail, request);
		response.setStatus(errorCode.status().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
