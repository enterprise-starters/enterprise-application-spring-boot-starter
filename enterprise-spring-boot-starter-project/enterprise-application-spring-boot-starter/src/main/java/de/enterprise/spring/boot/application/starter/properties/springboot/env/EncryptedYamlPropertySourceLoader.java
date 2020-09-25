package de.enterprise.spring.boot.application.starter.properties.springboot.env;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

public class EncryptedYamlPropertySourceLoader extends YamlPropertySourceLoader implements PriorityOrdered {

	@Override
	public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
		List<PropertySource<?>> propertySources = super.load(name, resource);

		List<PropertySource<?>> encryptablePropertySources = new ArrayList<>();
		propertySources.forEach(propertySource -> encryptablePropertySources
				.add(new EncryptablePropertySource(propertySource.getName(), propertySource, null)));

		return encryptablePropertySources;
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}
}
