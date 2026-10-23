package edu.fau.recsys.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

/**
 * Session-based login for the single-page UI. Browsing (search, movie detail, genres, the lecture example, the
 * starter set, static files) is public; {@code /api/me/**} needs a signed-in user. Login, logout and failures answer
 * with JSON status codes instead of redirects, and CSRF uses Spring Security's single-page-app setup: an
 * {@code XSRF-TOKEN} cookie the JS reads and sends back as the {@code X-XSRF-TOKEN} header.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests((auth) -> auth
			.requestMatchers("/api/me/**").authenticated()
			.requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
			.requestMatchers(HttpMethod.GET, "/", "/index.html", "/login.html", "/css/**", "/js/**", "/favicon.ico",
					"/api/movies/**", "/api/genres", "/api/demo/**", "/api/onboarding/**")
			.permitAll()
			.anyRequest().authenticated())
			// spa(): a JS-readable XSRF-TOKEN cookie, written on every response, accepted back as X-XSRF-TOKEN.
			.csrf((csrf) -> csrf.spa())
			.formLogin((form) -> form.loginPage("/login.html")
				.loginProcessingUrl("/login")
				.successHandler((request, response, authentication) -> json(response, HttpStatus.OK,
						"{\"username\":\"" + escape(authentication.getName()) + "\"}"))
				.failureHandler((request, response, exception) -> problem(response, HttpStatus.UNAUTHORIZED,
						"Sign-in failed", "Wrong username or password.")))
			.logout((logout) -> logout.logoutUrl("/logout")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			.exceptionHandling((errors) -> errors
				.authenticationEntryPoint((request, response, exception) -> problem(response, HttpStatus.UNAUTHORIZED,
						"Sign in required", "Sign in to keep and use your own grades."))
				.accessDeniedHandler((request, response, exception) -> problem(response, HttpStatus.FORBIDDEN,
						"Forbidden", "Missing or invalid CSRF token, or not allowed.")));
		return http.build();
	}

	/** BCrypt by default, with the {@code {bcrypt}} prefix so the algorithm can change later. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	private static void problem(HttpServletResponse response, HttpStatus status, String title, String detail)
			throws IOException {
		write(response, status, MediaType.APPLICATION_PROBLEM_JSON_VALUE, "{\"status\":" + status.value()
				+ ",\"title\":\"" + escape(title) + "\",\"detail\":\"" + escape(detail) + "\"}");
	}

	private static void json(HttpServletResponse response, HttpStatus status, String body) throws IOException {
		write(response, status, MediaType.APPLICATION_JSON_VALUE, body);
	}

	/** JSON is UTF-8 by definition, so no charset parameter, the same as Spring MVC's own problem details. */
	private static void write(HttpServletResponse response, HttpStatus status, String contentType, String body)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(contentType);
		response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
	}

	private static String escape(String text) {
		return text.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
