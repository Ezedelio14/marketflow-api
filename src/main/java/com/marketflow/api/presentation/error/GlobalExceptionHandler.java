package com.marketflow.api.presentation.error;

import com.marketflow.api.application.payment.InvalidWebhookSignatureException;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import com.marketflow.api.domain.common.exception.ConflictException;
import com.marketflow.api.domain.common.exception.DomainException;
import com.marketflow.api.domain.common.exception.ForbiddenOperationException;
import com.marketflow.api.domain.common.exception.InsufficientStockException;
import com.marketflow.api.domain.common.exception.InvalidStateTransitionException;
import com.marketflow.api.domain.common.exception.PaymentProviderException;
import com.marketflow.api.domain.common.exception.ResourceNotFoundException;
import com.marketflow.api.infrastructure.web.ProblemDetailsWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * Maps every exception to an RFC 7807 {@link ProblemDetail} ({@code application/problem+json}) enriched with
 * {@code errorCode}, {@code timestamp} and {@code correlationId}. Framework exceptions (malformed JSON, missing
 * parameters, 405, 415 ...) are handled by the parent {@link ResponseEntityExceptionHandler} and enriched in
 * {@link #handleExceptionInternal}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, e, request);
    }

    @ExceptionHandler({BusinessRuleException.class, InsufficientStockException.class})
    public ProblemDetail handleBusinessRule(DomainException e, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, e, request);
    }

    @ExceptionHandler({InvalidStateTransitionException.class, ConflictException.class})
    public ProblemDetail handleConflict(DomainException e, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, e, request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbidden(ForbiddenOperationException e, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, e, request);
    }

    @ExceptionHandler(PaymentProviderException.class)
    public ProblemDetail handlePaymentProvider(PaymentProviderException e, HttpServletRequest request) {
        log.error("Payment provider failure: {}", e.getMessage(), e.getCause());
        return ProblemDetailsWriter.build(HttpStatus.BAD_GATEWAY, e.getErrorCode(),
                "The payment provider is unavailable. Your order was not charged; please retry.", request.getRequestURI());
    }

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    public ProblemDetail handleInvalidSignature(InvalidWebhookSignatureException e, HttpServletRequest request) {
        log.warn("Rejected webhook: {}", e.getMessage());
        return problem(HttpStatus.BAD_REQUEST, e, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException e, HttpServletRequest request) {
        return ProblemDetailsWriter.build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return ProblemDetailsWriter.build(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You do not have permission to perform this action", request.getRequestURI());
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ProblemDetail handleConcurrency(RuntimeException e, HttpServletRequest request) {
        return ProblemDetailsWriter.build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The resource was modified concurrently, please retry", request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return ProblemDetailsWriter.build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "The request conflicts with existing data", request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetailsWriter.build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request parameters are invalid", request.getRequestURI());
        problem.setProperty("errors", e.getConstraintViolations().stream()
                .map(v -> Map.of("field", v.getPropertyPath().toString(), "message", v.getMessage()))
                .toList());
        return problem;
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleBadSort(PropertyReferenceException e, HttpServletRequest request) {
        return ProblemDetailsWriter.build(HttpStatus.BAD_REQUEST, "INVALID_SORT_PROPERTY", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return ProblemDetailsWriter.build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request.getRequestURI());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        ProblemDetail problem = ProblemDetailsWriter.build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "One or more fields are invalid", path(request));
        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.<String, Object>of("field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception ex, Object body,
                                                             @NonNull HttpHeaders headers,
                                                             @NonNull HttpStatusCode statusCode,
                                                             @NonNull WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String detail = body instanceof ProblemDetail pd && pd.getDetail() != null ? pd.getDetail() : ex.getMessage();
        ProblemDetail problem = ProblemDetailsWriter.build(status, status.name(), detail, path(request));
        return ResponseEntity.status(status).headers(headers).body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, DomainException e, HttpServletRequest request) {
        return ProblemDetailsWriter.build(status, e.getErrorCode(), e.getMessage(), request.getRequestURI());
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null;
    }
}
