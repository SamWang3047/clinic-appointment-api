package com.sam.clinic.security;

import com.sam.clinic.shared.error.ApiErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityProblemWriter problemWriter;

	public ProblemAuthenticationEntryPoint(SecurityProblemWriter problemWriter) {
		this.problemWriter = problemWriter;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authenticationException) throws IOException, ServletException {
		problemWriter.write(
				request,
				response,
				ApiErrorCode.AUTHENTICATION_REQUIRED,
				"A valid Bearer access token is required.");
	}
}
