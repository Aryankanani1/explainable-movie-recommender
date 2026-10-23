package edu.fau.recsys.infra;

/** A result that can describe itself in a latency log line, e.g. {@code grades=6 fallback=NONE}. */
public interface LatencySummary {

	String latencySummary();

}
