package edu.fau.recsys.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Body of {@code PUT /api/me/grades/{movieId}}: a whole grade from 1 to 10, as in the lecture. */
record GradeRequest(@NotNull(message = "is required") @Min(value = 1, message = GradeRequest.RANGE) @Max(value = 10,
		message = GradeRequest.RANGE) Integer score) {

	static final String RANGE = "must be a whole number from 1 to 10";
}
