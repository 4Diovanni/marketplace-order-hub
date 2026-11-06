package com.giovanni.orderhub.exception.handler;

import com.giovanni.orderhub.dto.error.FieldViolation;
import com.giovanni.orderhub.enums.ErrorCode;
import com.giovanni.orderhub.exception.ApiException;
import com.giovanni.orderhub.exception.integration.InvalidPayloadException;
import com.giovanni.orderhub.exception.integration.RateLimitExceededException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns any error into an RFC 7807 response ({@link ProblemDetail}) with the same shape:
 *
 * <ul>
 *   <li>{@code code}: stable {@link ErrorCode} name, so the front end can decide what to do;</li>
 *   <li>{@code message}: text ready to show to the seller;</li>
 *   <li>{@code detail}: technical detail, for support and logs.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String CODE = "code";
    static final String MESSAGE = "message";
    static final String ERRORS = "errors";
    static final String RETRY_AFTER_SECONDS = "retryAfterSeconds";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        return respond(problemFor(ex));
    }

    @ExceptionHandler(InvalidPayloadException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPayload(InvalidPayloadException ex) {
        ProblemDetail body = problemFor(ex);
        body.setProperty(ERRORS, ex.getViolations());
        return respond(body);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException ex) {
        log.warn(ex.getMessage());
        ProblemDetail body = problemFor(ex);
        body.setProperty(RETRY_AFTER_SECONDS, ex.getRetryAfterSeconds());
        return ResponseEntity.status(body.getStatus())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(body);
    }

    /** Last defense for constraint violations that got past the service checks (e.g. a race between two requests). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco: {}", ex.getMostSpecificCause().getMessage());
        return respond(problem(ErrorCode.DATA_CONFLICT, "A operação conflita com dados já existentes."));
    }

    /** Any unexpected error: logs everything but returns only the standard message (no stack trace in the response). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return respond(problem(ErrorCode.INTERNAL_ERROR, "Erro inesperado no servidor."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();

        ProblemDetail body = problem(ErrorCode.VALIDATION_ERROR, "Um ou mais campos são inválidos.");
        body.setProperty(ERRORS, errors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Errors handled by Spring itself (malformed JSON, unknown route, wrong HTTP method...).
     * Keeps Spring's status and detail and adds the default {@code code} and {@code message}.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, status, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem && !hasCode(problem)) {
            ErrorCode code = fallbackCodeFor(status);
            problem.setTitle(code.getTitle());
            problem.setProperty(CODE, code.name());
            problem.setProperty(MESSAGE, code.formatMessage());
        }
        return response;
    }

    private static ErrorCode fallbackCodeFor(HttpStatusCode status) {
        if (status.value() == HttpStatus.BAD_REQUEST.value()) {
            return ErrorCode.MALFORMED_REQUEST;
        }
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return ErrorCode.RESOURCE_NOT_FOUND;
        }
        if (status.is5xxServerError()) {
            return ErrorCode.INTERNAL_ERROR;
        }
        return ErrorCode.REQUEST_NOT_SUPPORTED;
    }

    private static boolean hasCode(ProblemDetail problem) {
        return problem.getProperties() != null && problem.getProperties().containsKey(CODE);
    }

    private static ProblemDetail problemFor(ApiException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(ex.getErrorCode().getStatus(), ex.getMessage());
        body.setTitle(ex.getErrorCode().getTitle());
        body.setProperty(CODE, ex.getErrorCode().name());
        body.setProperty(MESSAGE, ex.getSellerMessage());
        return body;
    }

    private static ProblemDetail problem(ErrorCode code, String technicalDetail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(code.getStatus(), technicalDetail);
        body.setTitle(code.getTitle());
        body.setProperty(CODE, code.name());
        body.setProperty(MESSAGE, code.formatMessage());
        return body;
    }

    private static ResponseEntity<ProblemDetail> respond(ProblemDetail body) {
        return ResponseEntity.status(body.getStatus()).body(body);
    }
}
