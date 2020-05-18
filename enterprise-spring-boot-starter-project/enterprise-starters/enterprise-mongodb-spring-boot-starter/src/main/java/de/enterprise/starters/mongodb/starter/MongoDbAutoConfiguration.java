package de.enterprise.starters.mongodb.starter;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.util.ClassUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 *
 * @author Malte Geßner
 *
 */
@Configuration
public class MongoDbAutoConfiguration {

	@Bean
	public ValidatingMongoEventListener validatingMongoEventListener(LocalValidatorFactoryBean validator) {
		return new ValidatingMongoEventListener(validator);
	}

	/**
	 * Eigene Converter für LocalTime erstellt/registriert. Die mitgelieferten verwenden das aktuelle Datum. Dadurch kommt es zu Problemen
	 * mit der Sommer/Winterzeit. LocalTime sollte unabhängig vom Datum gespeichert und verwendet werden.
	 *
	 * @return new additional mongoConverters
	 */
	@Bean
	public MongoCustomConversions mongoCustomConversions() {
		List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(LocalDateToDateConverter.INSTANCE);
		converters.add(LocalDateTimeToDateConverter.INSTANCE);
		converters.add(LocalTimeToStringConverter.INSTANCE);
		converters.add(StringToLocalTimeConverter.INSTANCE);
		converters.add(OffsetDateTimeToDateConverter.INSTANCE);
		converters.add(DateToOffsetDateTimeConverter.INSTANCE);
		converters.add(ZonedDateTimeToDateConverter.INSTANCE);
		converters.add(DateToZonedDateTimeConverter.INSTANCE);

		if (isJavaXMailOnClasspath()) {
			converters.add(URIToStringConverter.INSTANCE);
			converters.add(StringToURIConverter.INSTANCE);
			converters.add(InternetAddressToStringConverter.INSTANCE);
			converters.add(StringToInternetAddressConverter.INSTANCE);
		}

		return new MongoCustomConversions(converters);
	}

	private static ZoneId getUtcZoneId() {
		return TimeZone.getTimeZone("UTC").toZoneId();
	}

	@WritingConverter
	public enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(LocalDateTime source) {
			return Date.from(source.atZone(getUtcZoneId()).toInstant());
		}
	}

	@WritingConverter
	public enum LocalDateToDateConverter implements Converter<LocalDate, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(LocalDate source) {
			return Date.from(source.atStartOfDay(getUtcZoneId()).toInstant());
		}
	}

	@WritingConverter
	public enum LocalDateToStringConverter implements Converter<LocalDate, String> {

		INSTANCE;

		@Override
		public String convert(LocalDate source) {
			return source == null ? null : source.format(DateTimeFormatter.ISO_LOCAL_DATE);
		}
	}

	@ReadingConverter
	public enum StringToLocalDateConverter implements Converter<String, LocalDate> {

		INSTANCE;

		@Override
		public LocalDate convert(String source) {
			return source == null ? null : LocalDate.parse(source, DateTimeFormatter.ISO_LOCAL_DATE);
		}
	}

	@WritingConverter
	public enum LocalTimeToStringConverter implements Converter<LocalTime, String> {

		INSTANCE;

		@Override
		public String convert(LocalTime source) {
			return source == null ? null : source.format(DateTimeFormatter.ISO_LOCAL_TIME);
		}
	}

	@ReadingConverter
	public enum StringToLocalTimeConverter implements Converter<String, LocalTime> {

		INSTANCE;

		@Override
		public LocalTime convert(String source) {
			return source == null ? null : LocalTime.parse(source, DateTimeFormatter.ISO_LOCAL_TIME);
		}
	}

	@WritingConverter
	public enum OffsetDateTimeToDateConverter implements Converter<OffsetDateTime, Date> {

		INSTANCE;

		@Override
		public Date convert(OffsetDateTime source) {
			return source == null ? null : Date.from(source.atZoneSimilarLocal(getUtcZoneId()).toInstant());
		}
	}

	@ReadingConverter
	public enum DateToOffsetDateTimeConverter implements Converter<Date, OffsetDateTime> {

		INSTANCE;

		@Override
		public OffsetDateTime convert(Date source) {
			return source == null ? null : OffsetDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
		}
	}

	@WritingConverter
	public enum ZonedDateTimeToDateConverter implements Converter<ZonedDateTime, Date> {

		INSTANCE;

		@Override
		public Date convert(ZonedDateTime source) {
			return source == null ? null : Date.from(source.toInstant());
		}
	}

	@ReadingConverter
	public enum DateToZonedDateTimeConverter implements Converter<Date, ZonedDateTime> {

		INSTANCE;

		@Override
		public ZonedDateTime convert(Date source) {
			return source == null ? null : ZonedDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
		}
	}

	@ReadingConverter
	public enum StringToURIConverter implements Converter<String, URI> {
		INSTANCE;

		@Override
		public URI convert(String source) {
			return URI.create(source);
		}
	}

	@WritingConverter
	public enum URIToStringConverter implements Converter<URI, String> {
		INSTANCE;

		@Override
		public String convert(URI source) {
			return source.toString();
		}
	}

	@ReadingConverter
	public enum StringToInternetAddressConverter implements Converter<String, InternetAddress> {
		INSTANCE;

		@Override
		public InternetAddress convert(String source) {
			return createInternetAddress(source);
		}

		private InternetAddress createInternetAddress(String address) {
			try {
				return InternetAddress.parse(address)[0];
			} catch (AddressException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	@WritingConverter
	public enum InternetAddressToStringConverter implements Converter<InternetAddress, String> {
		INSTANCE;

		@Override
		public String convert(InternetAddress source) {
			return source.toString();
		}
	}

	private boolean isJavaXMailOnClasspath() {
		return ClassUtils.isPresent("javax.mail.internet.InternetAddress", MongoDbAutoConfiguration.class.getClassLoader());
	}
}
