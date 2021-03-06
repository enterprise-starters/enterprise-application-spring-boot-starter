package de.enterprise.spring.boot.application.starter.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.enterprise.spring.boot.application.starter.properties.testapp.PropertyTestApplication;

@SpringBootTest(classes = PropertyTestApplication.class)
@ActiveProfiles("yamltest")
public class YamlPropertyDecryptionTest {

	@Value("${test.password}")
	private String password;

	@Test
	public void property() {
		assertThat(this.password).isEqualTo("test123");
	}

}
