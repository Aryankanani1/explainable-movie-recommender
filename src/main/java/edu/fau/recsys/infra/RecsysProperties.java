package edu.fau.recsys.infra;

import java.nio.file.Path;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.validation.annotation.Validated;

/**
 * Application settings under {@code recsys.*}. Dev defaults live in {@code application.yaml}.
 *
 * @param dataDir folder with the MovieLens CSVs ({@code recsys.data-dir})
 * @param importSettings {@code recsys.import.*}; {@code import} is a Java keyword, hence the rename
 * @param recommend {@code recsys.recommend.*}
 */
@Validated
@ConfigurationProperties("recsys")
public record RecsysProperties(@NotNull Path dataDir, @Name("import") @DefaultValue Import importSettings,
		@Valid @DefaultValue Recommend recommend) {

	/** @param enabled import MovieLens on startup when the catalog is empty */
	public record Import(@DefaultValue("true") boolean enabled) {
	}

	/**
	 * @param defaultK list length when {@code ?k=} is absent
	 * @param tagWeight β, the weight of the tag block relative to the genre block in the improved method
	 */
	public record Recommend(@DefaultValue("10") @Min(1) @Max(100) int defaultK,
			@DefaultValue("0.5") @PositiveOrZero double tagWeight) {
	}
}
