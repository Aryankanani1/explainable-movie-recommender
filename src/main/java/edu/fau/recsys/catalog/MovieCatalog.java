package edu.fau.recsys.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Database reads of the catalog for other packages. Entities stay inside these transactions. */
@Service
public class MovieCatalog {

	/** Most-rated first; the ID makes the order total, so pages never overlap or skip rows. */
	private static final Sort ORDER = Sort.by(Sort.Order.desc("ratingCount"), Sort.Order.asc("id"));

	private final MovieRepository movieRepository;

	MovieCatalog(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	@Transactional(readOnly = true)
	public Page<MovieSummary> search(String query, int page, int size) {
		return movieRepository.findByDisplayTitleContainingIgnoreCase(query.strip(), PageRequest.of(page, size, ORDER))
			.map(MovieSummary::from);
	}

	@Transactional(readOnly = true)
	public boolean exists(int movieId) {
		return movieRepository.existsById(movieId);
	}
}
