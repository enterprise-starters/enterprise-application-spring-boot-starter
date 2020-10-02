package de.enterprise.spring.boot.application.starter.properties.springboot.env;

import java.io.IOException;
import java.nio.file.Files;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;
import org.springframework.security.rsa.crypto.RsaAlgorithm;
import org.springframework.security.rsa.crypto.RsaSecretEncryptor;
import org.springframework.util.StringUtils;

import de.enterprise.spring.boot.common.exception.TechnicalException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EncryptedPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		TextEncryptor textEnrypter = configureTextEncryptor(environment);

		environment.getPropertySources().forEach(propertySource -> {
			if (propertySource instanceof EncryptablePropertySource) {
				((EncryptablePropertySource) propertySource).setTextEncrypter(textEnrypter);
			}
		});

	}

	public static TextEncryptor configureTextEncryptor(ConfigurableEnvironment environment) {
		TextEncryptor textEncryptor = null;

		// create the encryptable properties property source
		String keystorePath = environment.getProperty(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PATH,
				System.getenv(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PATH_ENV));
		String keystorePassword = environment.getProperty(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD,
				System.getenv(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD_ENV));
		String keystorePasswordPath = environment.getProperty(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD_PATH,
				System.getenv(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD_PATH_ENV));
		String keyAlias = environment.getProperty(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEY_ALIAS,
				System.getenv(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEY_ALIAS_ENV));
		String keyPassword = environment.getProperty(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEY_PASSWORD,
				System.getenv(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEY_PASSWORD_ENV));

		// log.trace(
		// "values found for textEncrypter. keystorePath={}, keystorePassword={}, keystorePassordPath={}, keyAlias={}, keyPassword={}",
		// keystorePath, keystorePassword, keystorePasswordPath, keyAlias, keyPassword);

		if (!StringUtils.isEmpty(keystorePasswordPath)) {
			Resource keystorePasswordLocation = new FileSystemResource(keystorePasswordPath);
			byte[] keystorePasswordBytes;
			try {
				keystorePasswordBytes = Files.readAllBytes(keystorePasswordLocation.getFile().toPath());
			} catch (IOException e) {
				throw new TechnicalException("can't read keystore password from file.", e);
			}
			keystorePassword = new String(keystorePasswordBytes);
		}

		if (StringUtils.isEmpty(keyPassword)) {
			// Key password protecting the key (defaults to the same as the keystore password).
			keyPassword = keystorePassword;
			log.trace("no keypassword configured, keystorePassword is used.");
		}

		if (!StringUtils.isEmpty(keystorePath) && !StringUtils.isEmpty(keystorePassword) && !StringUtils.isEmpty(keyAlias)) {
			Resource keyStoreLocation = new FileSystemResource(keystorePath);

			if (keyStoreLocation.exists()) {
				log.info("init rsa decrypter for properties with ENC() embracing.");
				textEncryptor = new RsaSecretEncryptor(new KeyStoreKeyFactory(keyStoreLocation, keystorePassword.toCharArray())
						.getKeyPair(keyAlias, keyPassword.toCharArray()), RsaAlgorithm.DEFAULT, "Salt4Ves", true);
			}
		}

		return textEncryptor;
	}

}
