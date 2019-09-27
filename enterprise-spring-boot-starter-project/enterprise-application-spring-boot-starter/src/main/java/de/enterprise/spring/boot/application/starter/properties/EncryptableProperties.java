package de.enterprise.spring.boot.application.starter.properties;

import java.util.Hashtable;
import java.util.Properties;

import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * 
 * @author Malte Geßner
 *
 */
public final class EncryptableProperties extends Properties {

	private static final long serialVersionUID = 6479795856725500639L;
	private static final String ENCRYPTED_VALUE_PREFIX = "ENC(";
	private static final String ENCRYPTED_VALUE_SUFFIX = ")";

	private TextEncryptor textEncryptor;

	/**
	 * <p>
	 * Creates an <code>EncryptableProperties</code> instance which will use the passed { cipher} object to decrypt encrypted values.
	 * </p>
	 * 
	 * @param textEncryptor
	 *            the { TextEncryptor} to be used do decrypt values. It can not be null.
	 */
	public EncryptableProperties(final TextEncryptor textEncryptor) {
		this(null, textEncryptor);
	}

	/**
	 * <p>
	 * Creates an <code>EncryptableProperties</code> instance which will use the passed { StringEncryptor} object to decrypt encrypted
	 * values, and the passed defaults as default values (may contain encrypted values).
	 * </p>
	 * 
	 * @param defaults
	 *            default values for properties (may be encrypted).
	 * @param textEncryptor
	 *            the { TextEncryptor} to be used do decrypt values. It can not be null.
	 */
	public EncryptableProperties(final Properties defaults, final TextEncryptor textEncryptor) {
		super(defaults);
		this.textEncryptor = textEncryptor;
	}

	/**
	 * <p>
	 * Obtains the property value for the specified key (see {@link Properties#getProperty(String)}), decrypting it if needed.
	 * </p>
	 * 
	 * @param key
	 *            the property key
	 * @return the (decrypted) value
	 */
	@Override
	public String getProperty(final String key) {
		return this.decode(super.getProperty(key));
	}

	/**
	 * <p>
	 * Obtains the property value for the specified key (see {@link Properties#getProperty(String)}), decrypting it if needed.
	 * </p>
	 * <p>
	 * If no value is found for the specified key, the default value will be returned (decrypted if needed).
	 * </p>
	 * 
	 * @param key
	 *            the property key
	 * @param defaultValue
	 *            the default value to return
	 * @return the (decrypted) value
	 */
	@Override
	public String getProperty(final String key, final String defaultValue) {
		return this.decode(super.getProperty(key, defaultValue));
	}

	/**
	 * <p>
	 * Obtains the property value for the specified key (see {@link Hashtable#get(Object)}), decrypting it if needed.
	 * </p>
	 * 
	 * @param key
	 *            the property key
	 * @return the (decrypted) value
	 * @since 1.9.0
	 */
	@Override
	public synchronized Object get(final Object key) {
		final Object value = super.get(key);
		final String valueStr = value instanceof String ? (String) value : null;
		return this.decode(valueStr);
	}

	/*
	 * Internal method for decoding (decrypting) a value if needed.
	 */
	private synchronized String decode(final String encodedValue) {

		if (!this.isEncryptedValue(encodedValue)) {
			return encodedValue;
		}
		if (this.textEncryptor != null) {
			return this.textEncryptor.decrypt(getInnerEncryptedValue(encodedValue));
		}

		return encodedValue;
	}

	private static String getInnerEncryptedValue(final String value) {
		String trimmed = value.trim();
		return trimmed.substring(ENCRYPTED_VALUE_PREFIX.length(), (trimmed.length() - ENCRYPTED_VALUE_SUFFIX.length()));
	}

	private boolean isEncryptedValue(final String value) {
		if (value == null) {
			return false;
		}
		final String trimmedValue = value.trim();
		return trimmedValue.startsWith(ENCRYPTED_VALUE_PREFIX) && trimmedValue.endsWith(ENCRYPTED_VALUE_SUFFIX);
	}
}
