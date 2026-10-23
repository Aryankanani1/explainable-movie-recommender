package edu.fau.recsys.web;

import java.time.Instant;

import edu.fau.recsys.grading.GradeView;

record GradeDto(int movieId, String title, Integer year, int score, Instant updatedAt) {

	static GradeDto from(GradeView grade) {
		return new GradeDto(grade.movieId(), grade.title(), grade.year(), grade.score(), grade.updatedAt());
	}
}
