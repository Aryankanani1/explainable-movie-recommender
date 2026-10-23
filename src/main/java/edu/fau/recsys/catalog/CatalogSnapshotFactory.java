package edu.fau.recsys.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.fau.recsys.engine.CatalogSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

/**
 * Adapts database rows into the engine's {@link CatalogSnapshot}, once, after startup (and so after the import).
 * Uses four flat queries instead of loading the entity graph. Genre order is genre ID order.
 */
@Component
class CatalogSnapshotFactory {

	private static final Logger log = LoggerFactory.getLogger(CatalogSnapshotFactory.class);

	private final JdbcTemplate jdbcTemplate;

	private final CatalogSnapshotHolder holder;

	CatalogSnapshotFactory(JdbcTemplate jdbcTemplate, CatalogSnapshotHolder holder) {
		this.jdbcTemplate = jdbcTemplate;
		this.holder = holder;
	}

	@EventListener(ApplicationReadyEvent.class)
	void buildSnapshot() {
		long start = System.nanoTime();
		CatalogSnapshot snapshot = load();
		holder.set(snapshot);
		log.info("Catalog snapshot built: {} movies, {} genres in {} ms", snapshot.size(), snapshot.genreNames().size(),
				(System.nanoTime() - start) / 1_000_000);
	}

	CatalogSnapshot load() {
		CatalogSnapshot.Builder builder = CatalogSnapshot.builder()
			.genres(jdbcTemplate.queryForList("SELECT name FROM genre ORDER BY id", String.class));

		Map<Integer, List<String>> genresByMovie = new HashMap<>();
		jdbcTemplate.query("""
				SELECT mg.movie_id, g.name
				  FROM movie_genre mg JOIN genre g ON g.id = mg.genre_id
				 ORDER BY mg.movie_id, g.id
				""", (RowCallbackHandler) rs -> genresByMovie
			.computeIfAbsent(rs.getInt("movie_id"), id -> new ArrayList<>())
			.add(rs.getString("name")));

		Map<Integer, Map<String, Integer>> tagsByMovie = new HashMap<>();
		jdbcTemplate.query("SELECT movie_id, tag, count FROM movie_tag ORDER BY movie_id, tag",
				(RowCallbackHandler) rs -> tagsByMovie.computeIfAbsent(rs.getInt("movie_id"), id -> new TreeMap<>())
					.put(rs.getString("tag"), rs.getInt("count")));

		jdbcTemplate.query("SELECT id, display_title, year, rating_count FROM movie ORDER BY id",
				(RowCallbackHandler) rs -> {
					int id = rs.getInt("id");
					builder.addMovie(id, rs.getString("display_title"), rs.getObject("year", Integer.class),
							genresByMovie.getOrDefault(id, List.of()), tagsByMovie.getOrDefault(id, Map.of()),
							rs.getInt("rating_count"));
				});
		return builder.build();
	}
}
