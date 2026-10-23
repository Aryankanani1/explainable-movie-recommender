package edu.fau.recsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MovieRecommenderApplication {

	public static void main(String[] args) {
		SpringApplication.run(MovieRecommenderApplication.class, args);
	}

}
