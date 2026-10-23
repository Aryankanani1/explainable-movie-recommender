package edu.fau.recsys.infra;

import java.util.Locale;
import java.util.StringJoiner;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * Times every {@link LogLatency} method and logs one INFO line, e.g.
 * {@code RecommendationService.recommend(method=improved, k=10, minRatings=0, lambda=1.0) grades=6 fallback=NONE in 7.4 ms}.
 * A failing call is logged too, with the exception type, and then rethrown.
 */
@Aspect
@Component
class LatencyAspect {

	private static final Logger log = LoggerFactory.getLogger(LatencyAspect.class);

	@Around("@annotation(edu.fau.recsys.infra.LogLatency)")
	Object logLatency(ProceedingJoinPoint call) throws Throwable {
		long start = System.nanoTime();
		String outcome = "";
		try {
			Object result = call.proceed();
			if (result instanceof LatencySummary summary) {
				outcome = " " + summary.latencySummary();
			}
			return result;
		}
		catch (Throwable ex) {
			outcome = " failed with " + ex.getClass().getSimpleName();
			throw ex;
		}
		finally {
			double elapsedMs = (System.nanoTime() - start) / 1e6;
			log.info("{}{} in {} ms", describe(call), outcome, String.format(Locale.ROOT, "%.1f", elapsedMs));
		}
	}

	private static String describe(ProceedingJoinPoint call) {
		MethodSignature signature = (MethodSignature) call.getSignature();
		String[] names = signature.getParameterNames();
		Object[] values = call.getArgs();
		StringJoiner args = new StringJoiner(", ", "(", ")");
		for (int n = 0; n < values.length; n++) {
			args.add((names != null ? names[n] : "arg" + n) + "=" + values[n]);
		}
		return signature.getDeclaringType().getSimpleName() + "." + signature.getName() + args;
	}
}
