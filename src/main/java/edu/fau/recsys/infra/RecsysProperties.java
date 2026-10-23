package edu.fau.recsys.infra;

import java.nio.file.Path;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.validation.annotation.Validated;

/**
 * Application settings under {@code recsys.*}. Dev defaults live in {@code application.yaml}.
 *
 * @param dataDir folder with the MovieLens CSVs ({@code recsys.data-dir})
 * @param importSettings {@code recsys.import.*}; {@code import} is a Java keyword, hence the rename
 */
@Validated
@ConfigurationProperties("recsys")
public record RecsysProperties(@NotNull Path dataDir, @Name("import") @DefaultValue Import importSettings) {

	/** @param enabled import MovieLens on startup when the catalog is empty */
	public record Import(@DefaultValue("true") boolean enabled) {
	}
}
