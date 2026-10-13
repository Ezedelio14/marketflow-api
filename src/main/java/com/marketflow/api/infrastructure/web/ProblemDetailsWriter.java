package com.marketflow.api.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;

/**
 * Builds RFC 7807 Problem Details consistently for both the MVC exception handler and servlet filters
 * (which run outside of {@code @RestControllerAdvice}).
 */
@Component
@RequiredArgsConstructor
public class ProblemDetailsWriter {

    public static final String PROBLEM_BASE_URI = "https://api.marketflow.dev/problems/";

    private final ObjectMapper objectMapper;

    public static ProblemDetail build(HttpStatus status, String errorCode, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_BASE_URI + errorCode.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(status.getReasonPhrase());
        if (instance != null) {
            problem.setInstance(URI.create(instance));
        }
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now());
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                      String errorCode, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), build(status, errorCode, detail, request.getRequestURI()));
    }
}
