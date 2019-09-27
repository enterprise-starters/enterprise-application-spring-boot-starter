package de.enterprise.spring.boot.application.starter.properties;

import java.util.Properties;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 *
 * @author Malte Geßner
 *
 */
public final class EncryptablePropertiesPropertySource extends PropertiesPropertySource {

	public EncryptablePropertiesPropertySource(final String name, final EncryptableProperties props) {
		super(name, props);
	}

	public EncryptablePropertiesPropertySource(final String name, final Properties props, final TextEncryptor textEncryptor) {
		super(name, processProperties(props, textEncryptor));
	}

	private static Properties processProperties(final Properties props, final TextEncryptor textEncryptor) {
		if (props == null) {
			return null;
		}
		if (props instanceof EncryptableProperties) {
			throw new IllegalArgumentException("Properties object already is an " + EncryptableProperties.class.getName()
					+ " object. No decrypter should be specified.");
		}
		final EncryptableProperties encryptableProperties = new EncryptableProperties(textEncryptor);
		encryptableProperties.putAll(props);
		return encryptableProperties;
	}
}
