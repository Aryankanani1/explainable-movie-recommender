package edu.fau.recsys.infra;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Logs how long the annotated method took, with its arguments and, when the result is a {@link LatencySummary}, a
 * short summary of it. See {@link LatencyAspect}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogLatency {

}
