package com.samarama.bicycle;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.annotation.PostConstruct;

@SpringBootApplication(scanBasePackages = "com.samarama.bicycle")
@EnableJpaRepositories(basePackages = "com.samarama.bicycle.api.repository")
public class BicycleApplication {

	@Autowired
	private Flyway flyway;

	@PostConstruct
	public void migrateFlyway() {
		flyway.migrate();
	}

	public static void main(String[] args) {
		SpringApplication.run(BicycleApplication.class, args);
	}
}