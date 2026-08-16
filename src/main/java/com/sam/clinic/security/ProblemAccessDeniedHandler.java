package com.sam.clinic.security;

import com.sam.clinic.shared.error.ApiErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityProblemWriter problemWriter;

	public ProblemAccessDeniedHandler(SecurityProblemWriter problemWriter) {
		this.problemWriter = problemWriter;
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		problemWriter.write(
				request,
				response,
				ApiErrorCode.ACCESS_DENIED,
				"The authenticated account cannot perform this operation.");
	}
}
