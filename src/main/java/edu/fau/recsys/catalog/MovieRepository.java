package edu.fau.recsys.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface MovieRepository extends JpaRepository<Movie, Integer> {

	/** Case-insensitive "contains" on the display title. Spring Data escapes {@code %} and {@code _} in the fragment. */
	Page<Movie> findByDisplayTitleContainingIgnoreCase(String fragment, Pageable pageable);
}
