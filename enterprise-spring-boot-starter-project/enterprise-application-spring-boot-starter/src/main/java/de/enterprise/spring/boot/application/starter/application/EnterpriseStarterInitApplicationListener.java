package de.enterprise.spring.boot.application.starter.application;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import de.enterprise.spring.boot.common.exception.TechnicalException;

public class EnterpriseStarterInitApplicationListener implements SmartApplicationListener {

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		Properties defaultProperties = new Properties();
		try {
			ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
			Resource[] defaultPropertiesResources = patternResolver
					.getResources("classpath:/META-INF/logging-enterprise-starter.properties");

			for (Resource defaultPropertiesResource : defaultPropertiesResources) {
				defaultProperties.load(defaultPropertiesResource.getInputStream());
			}
		} catch (IOException e) {
			throw new TechnicalException("no default properties found!", e);
		}
		SpringApplication springApplication = ((ApplicationStartingEvent) event).getSpringApplication();

		Map<String, Object> defaultPropertyMap = new HashMap<>();
		for (Object key : Collections.list(defaultProperties.propertyNames())) {
			defaultPropertyMap.put((String) key, defaultProperties.get(key));
		}
		defaultPropertyMap.put("spring.profiles.default", "dev,dev-local," + System.getenv().get("COMPUTERNAME"));

		springApplication.setDefaultProperties(defaultPropertyMap);
		springApplication.setBannerMode(Mode.LOG);

	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return ApplicationStartingEvent.class.isAssignableFrom(eventType);
	}
}
