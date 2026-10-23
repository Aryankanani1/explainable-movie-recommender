package edu.fau.recsys.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Body of {@code PUT /api/me/grades/{movieId}}: a whole grade from 1 to 10, as in the lecture. */
record GradeRequest(@NotNull @Min(1) @Max(10) Integer score) {
}
