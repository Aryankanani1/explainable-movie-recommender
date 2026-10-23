package edu.fau.recsys.grading;

import edu.fau.recsys.infra.RecsysProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * With {@code recsys.demo-user.enabled=true} (dev only), makes sure a {@code demo} / {@code demo} account exists so
 * anyone can try the app without registering. Off by default, so a deployment never ships a known password.
 */
@Component
class DemoUserSeeder implements ApplicationRunner {

	static final String DEMO = "demo";

	private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

	private final RecsysProperties properties;

	private final AccountService accounts;

	DemoUserSeeder(RecsysProperties properties, AccountService accounts) {
		this.properties = properties;
		this.accounts = accounts;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (properties.demoUser().enabled()) {
			accounts.ensureAccount(DEMO, DEMO);
			log.info("Demo account enabled: sign in as demo / demo");
		}
	}
}
