package com.samarama.bicycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.samarama.bicycle")
@EnableJpaRepositories(basePackages = "com.samarama.bicycle.api.repository")
public class BicycleApplication {

	public static void main(String[] args) {
		SpringApplication.run(BicycleApplication.class, args);
	}

}
