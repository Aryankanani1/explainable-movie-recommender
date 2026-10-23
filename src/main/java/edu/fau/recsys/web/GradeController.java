package edu.fau.recsys.web;

import java.util.List;

import edu.fau.recsys.grading.CurrentUserProvider;
import edu.fau.recsys.grading.GradeService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** "My grades". A score outside 1–10 → 400; an unknown movie → 404 (both as ProblemDetail). */
@RestController
@RequestMapping("/api/me/grades")
class GradeController {

	private final GradeService gradeService;

	private final CurrentUserProvider currentUserProvider;

	GradeController(GradeService gradeService, CurrentUserProvider currentUserProvider) {
		this.gradeService = gradeService;
		this.currentUserProvider = currentUserProvider;
	}

	@GetMapping
	List<GradeDto> list() {
		return gradeService.list(me()).stream().map(GradeDto::from).toList();
	}

	@PutMapping("/{movieId}")
	GradeDto put(@PathVariable int movieId, @Valid @RequestBody GradeRequest request) {
		return GradeDto.from(gradeService.upsert(me(), movieId, request.score()));
	}

	@DeleteMapping("/{movieId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable int movieId) {
		gradeService.delete(me(), movieId);
	}

	private long me() {
		return currentUserProvider.currentUser().id();
	}
}
