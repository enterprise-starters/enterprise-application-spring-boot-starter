package de.enterprise.spring.boot.application.starter.properties.testapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "de.enterprise.spring.boot.application.starter.exception")
public class PropertyTestApplication {
	public static void main(String[] args) {
		SpringApplication.run(PropertyTestApplication.class, args);
	}
}
