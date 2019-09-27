package de.enterprise.spring.boot.application.starter.properties.springboot.env;

/**
 *
 * @author Malte Geßner
 *
 */
public final class EncryptionConstants {

	private EncryptionConstants() {

	}

	/**
	 * Keystore type.
	 */
	public static final String KEYSTORE_TYPE = "JCEKS";
	/**
	 * rsa algorithm.
	 */
	public static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";

	/**
	 * key password property name.
	 */
	public static final String SPRING_ENCRYPTED_PROPERTY_KEY_PASSWORD = "spring.encrypted.property.key.password";
	/**
	 * key password environment name.
	 */
	public static final String SPRING_ENCRYPTED_PROPERTY_KEY_PASSWORD_ENV = SPRING_ENCRYPTED_PROPERTY_KEY_PASSWORD.replace('.', '_')
			.toUpperCase();
	/**
	 * key alias property name.
	 */
	public static final String SPRING_ENCRYPTED_PROPERTY_KEY_ALIAS = "spring.encrypted.property.key.alias";
	/**
	 * key alias environment name.
	 */
	public static final String SPRING_ENCRYPTED_PROPERTY_KEY_ALIAS_ENV = SPRING_ENCRYPTED_PROPERTY_KEY_ALIAS.replace('.', '_')
			.toUpperCase();
	/**
	 * keystore password property name.
	 */
	public static final String SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD = "spring.encrypted.property.keystore.password";
	/**
	 * keystore password environment name.
	 */
	public static final String SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD_ENV = SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD
			.replace('.', '_').toUpperCase();
	/**
	 * keystore password path,property name.
	 */
	public static final String SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD_PATH = "spring.encrypted.property.keystore.password.path";
	/**
	 * keystore password path environment name.
	 */
	public static final String SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD_PATH_ENV = SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PASSWORD_PATH
			.replace('.', '_').toUpperCase();
	/**
	 * keystore path property name.
	 */
	public static final String SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PATH = "spring.encrypted.property.keystore.path";
	/**
	 * keystore path environment name.
	 */
	public static final String SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PATH_ENV = SPRING_ENCRYPTED_PROPERTY_KEYSTORE_PATH.replace('.', '_')
			.toUpperCase();

}
