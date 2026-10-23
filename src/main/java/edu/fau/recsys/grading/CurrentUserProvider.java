package edu.fau.recsys.grading;

/**
 * Resolves "me". Until ticket 08 every request is the {@code demo} user; ticket 08 swaps in a Spring Security-backed
 * implementation without touching the controllers.
 */
public interface CurrentUserProvider {

	CurrentUser currentUser();

}
