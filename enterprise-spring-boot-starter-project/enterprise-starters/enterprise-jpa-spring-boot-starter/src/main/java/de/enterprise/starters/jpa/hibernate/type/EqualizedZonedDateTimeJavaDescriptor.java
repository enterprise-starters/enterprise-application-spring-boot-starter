package de.enterprise.starters.jpa.hibernate.type;

import java.time.ZonedDateTime;

import org.hibernate.type.descriptor.java.ZonedDateTimeJavaDescriptor;

/**
 * Java type descriptor for the ZonedDateTime type.
 *
 * @author Malte Geßner
 */
public class EqualizedZonedDateTimeJavaDescriptor extends ZonedDateTimeJavaDescriptor {

	private static final long serialVersionUID = 201802211500L;

	/**
	 * Singleton access.
	 */
	public static final EqualizedZonedDateTimeJavaDescriptor INSTANCE = new EqualizedZonedDateTimeJavaDescriptor();

	@Override
	public boolean areEqual(ZonedDateTime one, ZonedDateTime another) {
		return one.isEqual(another);
	}
}
