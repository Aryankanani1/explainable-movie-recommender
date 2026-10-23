package edu.fau.recsys.web;

import java.util.ArrayList;
import java.util.List;

import edu.fau.recsys.catalog.CatalogNotReadyException;
import edu.fau.recsys.grading.UnknownMovieException;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every API error as an RFC 9457 problem detail. Extending {@link ResponseEntityExceptionHandler} covers Spring MVC's
 * own exceptions (Boot's default handler then steps aside); the overrides turn validation failures into a
 * {@code detail} a person can read, e.g. "score must be a whole number from 1 to 10", which the UI shows as is.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler
	ProblemDetail unknownMovie(UnknownMovieException ex) {
		return problem(HttpStatus.NOT_FOUND, "Movie not found", ex.getMessage());
	}

	@ExceptionHandler
	ProblemDetail unknownMethod(UnknownMethodException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Unknown recommendation method", ex.getMessage());
	}

	@ExceptionHandler
	ProblemDetail catalogNotReady(CatalogNotReadyException ex) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "Catalog loading", ex.getMessage());
	}

	/** A request body that fails Bean Validation, e.g. {@code {"score": 11}}. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<String> errors = new ArrayList<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.add(error.getField() + " " + error.getDefaultMessage());
		}
		return withErrors(ex.getBody(), errors, ex, headers, status, request);
	}

	/** A request parameter that fails a constraint, e.g. {@code ?k=0}. */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<String> errors = new ArrayList<>();
		for (ParameterValidationResult result : ex.getParameterValidationResults()) {
			for (MessageSourceResolvable error : result.getResolvableErrors()) {
				errors.add(result.getMethodParameter().getParameterName() + " " + error.getDefaultMessage());
			}
		}
		return withErrors(ex.getBody(), errors, ex, headers, status, request);
	}

	/** Not JSON, or a value of the wrong type, e.g. {@code {"score": 8.5}} or {@code {"score": "nine"}}. */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail body = createProblemDetail(ex, status, "Malformed request body, or a value of the wrong type.",
				null, null, request);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	private ResponseEntity<Object> withErrors(ProblemDetail body, List<String> errors, Exception ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		errors.sort(null);
		body.setDetail(String.join("; ", errors));
		body.setProperty("errors", errors);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
