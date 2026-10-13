package com.marketflow.api.infrastructure.security;

import com.marketflow.api.infrastructure.web.ProblemDetailsWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401/403 answers from the security filter chain rendered as RFC 7807 Problem Details. */
@Component
@RequiredArgsConstructor
public class ProblemDetailsSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ProblemDetailsWriter writer;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        writer.write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "A valid Bearer token is required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException {
        writer.write(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action");
    }
}
