package de.enterprise.spring.boot.application.starter.exception.testapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@SpringBootApplication(scanBasePackages = "de.enterprise.spring.boot.application.starter.exception")
public class ExceptionHandlingTestApplication {
	public static void main(String[] args) {
		SpringApplication.run(ExceptionHandlingTestApplication.class, args);
	}

	@Order(98)
	@Configuration
	public class SecurityConfig extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// Note:
			// Use this to enable the tomcat basic authentication (tomcat popup rather than spring login page)
			// Note that the CSRf token is disabled for all requests (change it as you wish...)
			http.csrf().disable().requestMatchers().antMatchers("/**").and().authorizeRequests().anyRequest().authenticated().and()
					.httpBasic();
		}
	}
}
