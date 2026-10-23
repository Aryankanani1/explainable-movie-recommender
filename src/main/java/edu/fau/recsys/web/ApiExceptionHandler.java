package edu.fau.recsys.web;

import edu.fau.recsys.catalog.CatalogNotReadyException;
import edu.fau.recsys.grading.UnknownMovieException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the app's own exceptions to RFC 9457 problem details. Spring MVC's exceptions (validation, bad JSON) are
 * already rendered as problem details by Boot ({@code spring.mvc.problemdetails.enabled}).
 */
@RestControllerAdvice
class ApiExceptionHandler {

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

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
