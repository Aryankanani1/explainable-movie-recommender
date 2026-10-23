package edu.fau.recsys.catalog;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

@Embeddable
record MovieTagId(Integer movieId, String tag) implements Serializable {
}
