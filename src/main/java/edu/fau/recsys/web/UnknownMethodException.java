package edu.fau.recsys.web;

/** {@code ?method=} named a recommender the API does not offer (yet). */
class UnknownMethodException extends RuntimeException {

	UnknownMethodException(String method) {
		super("Unknown method '" + method + "'. Supported: " + RecommendationService.SUPPORTED_METHODS);
	}
}
