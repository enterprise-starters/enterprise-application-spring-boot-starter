package de.enterprise.starters.jpa.hibernate.type;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.annotations.TypeDef;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ZonedDateTimeComparator;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.LiteralType;
import org.hibernate.type.VersionType;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

/**
 * @author Malte Geßner
 */
@TypeDef(typeClass = EqualizedZonedDateTimeType.class, defaultForType = ZonedDateTime.class)
public class EqualizedZonedDateTimeType extends AbstractSingleColumnStandardBasicType<ZonedDateTime>
		implements VersionType<ZonedDateTime>, LiteralType<ZonedDateTime> {

	private static final long serialVersionUID = 201802211500L;

	/**
	 * 
	 */
	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S VV", Locale.ENGLISH);

	public EqualizedZonedDateTimeType() {
		super(TimestampTypeDescriptor.INSTANCE, EqualizedZonedDateTimeJavaDescriptor.INSTANCE);
	}

	@Override
	public String objectToSQLString(ZonedDateTime value, Dialect dialect) throws Exception {
		return "{ts '" + FORMATTER.format(value) + "'}";
	}

	@Override
	public ZonedDateTime seed(SharedSessionContractImplementor session) {
		return ZonedDateTime.now();
	}

	@Override
	public ZonedDateTime next(ZonedDateTime current, SharedSessionContractImplementor session) {
		return ZonedDateTime.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<ZonedDateTime> getComparator() {
		return ZonedDateTimeComparator.INSTANCE;
	}

	@Override
	public String getName() {
		return ZonedDateTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
