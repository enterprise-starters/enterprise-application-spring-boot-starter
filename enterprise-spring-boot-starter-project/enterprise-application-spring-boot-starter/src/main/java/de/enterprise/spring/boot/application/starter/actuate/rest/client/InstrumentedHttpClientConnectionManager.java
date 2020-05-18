package de.enterprise.spring.boot.application.starter.actuate.rest.client;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * A {@link HttpClientConnectionManager} which monitors the number of open connections.
 *
 * @author Malte Geßner
 */
public class InstrumentedHttpClientConnectionManager extends PoolingHttpClientConnectionManager {

	private static final String TAG_NAME_HTTPCLIENT_NAME = "httpClientName";

	protected static Registry<ConnectionSocketFactory> getDefaultRegistry() {
		return RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
	}

	public InstrumentedHttpClientConnectionManager(MeterRegistry meterRegistry, String metricName) {
		this(meterRegistry, getDefaultRegistry(), metricName);
	}

	public InstrumentedHttpClientConnectionManager(MeterRegistry meterRegistry, Registry<ConnectionSocketFactory> socketFactoryRegistry,
			String metricName) {
		this(meterRegistry, socketFactoryRegistry, -1, TimeUnit.MILLISECONDS, metricName);
	}

	public InstrumentedHttpClientConnectionManager(MeterRegistry meterRegistry, Registry<ConnectionSocketFactory> socketFactoryRegistry,
			long connTTL, TimeUnit connTTLTimeUnit, String metricName) {
		this(meterRegistry, socketFactoryRegistry, null, null, SystemDefaultDnsResolver.INSTANCE, connTTL, connTTLTimeUnit, metricName);
	}

	public InstrumentedHttpClientConnectionManager(MeterRegistry meterRegistry, Registry<ConnectionSocketFactory> socketFactoryRegistry,
			HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory, SchemePortResolver schemePortResolver,
			DnsResolver dnsResolver, long connTTL, TimeUnit connTTLTimeUnit, String metricName) {
		super(socketFactoryRegistry, connFactory, schemePortResolver, dnsResolver, connTTL, connTTLTimeUnit);
		Tags tags = Tags.of(TAG_NAME_HTTPCLIENT_NAME, StringUtils.defaultIfBlank(metricName, "default"));

		meterRegistry.gauge("httpclientconnection.available-connections", tags, Integer.valueOf(0),
				x -> this.getTotalStats().getAvailable());
		meterRegistry.gauge("httpclientconnection.leased-connections", tags, Integer.valueOf(0),
				x -> this.getTotalStats().getLeased());
		meterRegistry.gauge("httpclientconnection.max-connections", tags, Integer.valueOf(0),
				x -> this.getTotalStats().getMax());
		meterRegistry.gauge("httpclientconnection.pending-connections", tags, Integer.valueOf(0),
				x -> this.getTotalStats().getPending());
	}
}
