package de.enterprise.spring.boot.application.starter.properties.springboot.env;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;
import org.springframework.security.rsa.crypto.RsaAlgorithm;
import org.springframework.security.rsa.crypto.RsaSecretEncryptor;
import org.springframework.util.StringUtils;

import de.enterprise.spring.boot.application.starter.properties.EncryptablePropertiesPropertySource;
import de.enterprise.spring.boot.common.exception.TechnicalException;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Malte Geßner
 *
 */
@Slf4j
public class EncryptedPropertySourceLoader implements PropertySourceLoader, PriorityOrdered {

	@Override
	public String[] getFileExtensions() {
		return new String[] { "properties", "xml" };
	}

	@Override
	public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
		// load the properties
		final Properties properties = PropertiesLoaderUtils.loadProperties(resource);
		EncryptablePropertiesPropertySource encryptablePropertiesPropertySource = new EncryptablePropertiesPropertySource(name, properties,
				configureTextEncryptor(properties));

		return Arrays.asList(encryptablePropertiesPropertySource);
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}

	public static TextEncryptor configureTextEncryptor(Properties properties) {
		TextEncryptor textEncryptor = null;
		if (!properties.isEmpty()) {
			// create the encryptable properties property source
			String keystorePath = properties.getProperty(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PATH,
					System.getenv(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PATH_ENV));
			String keystorePassword = properties.getProperty(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD,
					System.getenv(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD_ENV));
			String keystorePasswordPath = properties.getProperty(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD_PATH,
					System.getenv(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD_PATH_ENV));
			String keyAlias = properties.getProperty(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEY_ALIAS,
					System.getenv(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEY_ALIAS_ENV));
			String keyPassword = properties.getProperty(EncryptionConstants.SPRING_ENCRYPTED_PROPERTY_KEY_PASSWORD,
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
		}

		return textEncryptor;
	}
}