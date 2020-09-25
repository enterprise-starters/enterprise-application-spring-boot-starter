package de.enterprise.spring.boot.application.starter.properties.springboot.env;

import org.springframework.core.env.PropertySource;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@SuppressWarnings("rawtypes")
public class EncryptablePropertySource extends PropertySource {

	private static final String ENCRYPTED_VALUE_PREFIX = "ENC(";
	private static final String ENCRYPTED_VALUE_SUFFIX = ")";

	private TextEncryptor textEncryptor;
	private PropertySource<?> originPropertySource;

	@SuppressWarnings("unchecked")
	public EncryptablePropertySource(String name, PropertySource originPropertySource, TextEncryptor textEncryptor) {
		super(name, originPropertySource.getSource());
		this.textEncryptor = textEncryptor;
		this.originPropertySource = originPropertySource;
	}

	@Override
	public Object getProperty(String name) {
		return this.decode(this.originPropertySource.getProperty(name));
	}

	/*
	 * Internal method for decoding (decrypting) a value if needed.
	 */
	private synchronized Object decode(final Object encodedValue) {

		if (encodedValue instanceof String) {
			String encodedValueAsString = (String) encodedValue;
			if (!this.isEncryptedValue(encodedValueAsString)) {
				return encodedValue;
			}
			if (this.textEncryptor != null) {
				return this.textEncryptor.decrypt(getInnerEncryptedValue(encodedValueAsString));
			}
		}

		return encodedValue;
	}

	private static String getInnerEncryptedValue(final String value) {
		String trimmed = value.trim();
		return trimmed.substring(ENCRYPTED_VALUE_PREFIX.length(), trimmed.length() - ENCRYPTED_VALUE_SUFFIX.length());
	}

	private boolean isEncryptedValue(final String value) {
		if (value == null) {
			return false;
		}
		final String trimmedValue = value.trim();
		return trimmedValue.startsWith(ENCRYPTED_VALUE_PREFIX) && trimmedValue.endsWith(ENCRYPTED_VALUE_SUFFIX);
	}

	void setTextEncrypter(TextEncryptor textEncryptor) {
		this.textEncryptor = textEncryptor;
	}
}
