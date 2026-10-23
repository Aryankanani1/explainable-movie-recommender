package edu.fau.recsys.catalog;

import java.util.concurrent.atomic.AtomicReference;

import edu.fau.recsys.engine.CatalogSnapshot;

import org.springframework.stereotype.Component;

/** The current in-memory catalog. Set once at startup by {@link CatalogSnapshotFactory}; read on every request. */
@Component
public class CatalogSnapshotHolder {

	private final AtomicReference<CatalogSnapshot> snapshot = new AtomicReference<>();

	/** @throws CatalogNotReadyException before the snapshot has been built */
	public CatalogSnapshot get() {
		CatalogSnapshot current = snapshot.get();
		if (current == null) {
			throw new CatalogNotReadyException();
		}
		return current;
	}

	void set(CatalogSnapshot snapshot) {
		this.snapshot.set(snapshot);
	}
}
