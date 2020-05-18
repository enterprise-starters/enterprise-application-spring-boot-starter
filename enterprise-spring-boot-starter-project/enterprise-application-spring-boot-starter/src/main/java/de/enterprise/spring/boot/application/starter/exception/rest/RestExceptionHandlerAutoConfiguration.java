package de.enterprise.spring.boot.application.starter.exception.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to auto configure the rest global exception handling.
 *
 * @author Malte Geßner
 *
 */
@Configuration
@ComponentScan
@ConditionalOnWebApplication
public class RestExceptionHandlerAutoConfiguration {

}
