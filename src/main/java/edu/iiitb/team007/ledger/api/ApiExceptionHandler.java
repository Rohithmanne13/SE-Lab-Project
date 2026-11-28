package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.domain.FieldProblem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception into an RFC 9457 problem document (ADR-0004, FR-0.3, UI-3). Unexpected faults become
 * 500 INTERNAL_ERROR with a generic detail; their stack trace goes only to the log, under the request ID
 * (SEC-6, SEC-7). The unit of work has already rolled back, so nothing was persisted.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ProblemWriter problems;

    public ApiExceptionHandler(ProblemWriter problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ApiException.class)
    public void api(ApiException e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(request, response, e);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void unreadable(HttpMessageNotReadableException e, HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        if (causedBy(e, PayloadTooLargeException.class)) {
            problems.write(request, response, PayloadSizeFilter.tooLarge());
        } else {
            problems.write(request, response, new ApiException(ErrorCode.VALIDATION_ERROR,
                    "The request body could not be read.", List.of(new FieldProblem("body", "could not be read"))));
        }
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public void notFound(HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.writeHttpStatus(request, response, HttpStatus.NOT_FOUND,
                "The API has no resource at this path; see docs/api/openapi.yaml for the endpoints.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void methodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {
        if (e.getSupportedHttpMethods() != null) {
            response.setHeader("Allow", String.join(", ", e.getSupportedHttpMethods().stream()
                    .map(Object::toString).toList()));
        }
        problems.writeHttpStatus(request, response, HttpStatus.METHOD_NOT_ALLOWED,
                "The method " + request.getMethod() + " is not allowed on this path.");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void notAcceptable(HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.writeHttpStatus(request, response, HttpStatus.NOT_ACCEPTABLE,
                "The API answers only with JSON; accept application/json.");
    }

    @ExceptionHandler(Exception.class)
    public void unexpected(Exception e, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        LOG.error("Unexpected error, request_id={}", request.getAttribute(RequestAttributes.REQUEST_ID), e);
        problems.write(request, response, new ApiException(ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred and nothing was changed; report the request_id if it persists."));
    }

    private static boolean causedBy(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (type.isInstance(c)) {
                return true;
            }
        }
        return false;
    }
}
