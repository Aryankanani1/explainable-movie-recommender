package edu.fau.recsys.catalog;

/** A recommendation was requested before the catalog snapshot finished building. */
public class CatalogNotReadyException extends RuntimeException {

	CatalogNotReadyException() {
		super("The catalog is still loading; try again in a few seconds.");
	}
}
