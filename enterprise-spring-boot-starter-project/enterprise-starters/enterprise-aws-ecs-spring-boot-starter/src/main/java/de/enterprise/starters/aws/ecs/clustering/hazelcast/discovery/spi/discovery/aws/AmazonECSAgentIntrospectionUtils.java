package de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkClientException;
import com.amazonaws.internal.EC2CredentialsUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

/**
 * Utility class for retrieving Amazon ECS Agent Introspection data.<br>
 *
 * More information about Amazon ECS Agent Introspection
 *
 * @author Malte Geßner (malte.gessner@acando.de)
 * @see <a href="http://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-agent-introspection.html">Amazon EC2 Container Service
 *      Developer Guide: Amazon ECS Container Agent Introspection</a>
 */
public final class AmazonECSAgentIntrospectionUtils {

	/**
	 * System property for overriding the Amazon EC2 Instance Metadata Service endpoint.
	 */
	public static final String ECS_AGENT_INTROSPECTION_API_OVERRIDE_SYSTEM_PROPERTY = "com.commercehub.amazonaws.util.ecsAgentIntrospectionAPIEndpointOverride";

	/** Default endpoint for the Amazon ECS Agent Introspection API. */
	private static final String ECS_AGENT_INTROSPECTION_API_URL = "http://172.17.0.1:51678";
	private static final String ECS_METADATA_ROOT = "/v1/metadata";
	private static final String ECS_TASKS_ROOT = "/v1/tasks";

	private static final int DEFAULT_QUERY_RETRIES = 3;
	private static final int MINIMUM_RETRY_WAIT_TIME_MILLISECONDS = 250;

	private static final ObjectMapper mapper = new ObjectMapper();

	private AmazonECSAgentIntrospectionUtils() {
	}

	static {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
	}

	private static final Log log = LogFactory.getLog(AmazonECSAgentIntrospectionUtils.class);

	public static Metadata getMetadata() {
		String json = getData(ECS_METADATA_ROOT);
		if (null == json) {
			return null;
		}

		try {
			return mapper.readValue(json, Metadata.class);
		} catch (Exception e) {
			log.warn("Unable to parse ECS Agent Metadata (" + json + "): " + e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Get information about an ECS Task running on the local container instance identified by the ID of the Docker container running for
	 * that Task. Both long- and short-form Docker container IDs are supported.
	 *
	 * @param dockerId
	 *            the long- or short-form ID of the Docker container running for the Task to be retrieved
	 * @return information about the ECS Task identified by the provided dockerId
	 */
	public static Task getTask(String dockerId) {
		String json = getData(ECS_TASKS_ROOT + "?dockerid=" + dockerId);
		if (null == json) {
			return null;
		}

		try {
			return mapper.readValue(json, Task.class);
		} catch (Exception e) {
			log.warn("Unable to parse ECS Agent Task (" + json + "): " + e.getMessage(), e);
			return null;
		}
	}

	public static String getData(String path) {
		return getData(path, DEFAULT_QUERY_RETRIES);
	}

	public static String getData(String path, int tries) {
		List<String> items = getItems(path, tries, true);
		if (null != items && items.size() > 0) {
			return items.get(0);
		}
		return null;
	}

	private static List<String> getItems(String path, int tries, boolean slurp) {
		if (tries == 0) {
			throw new SdkClientException(
					"Unable to contact ECS Agent Introspection API.");
		}

		List<String> items;
		try {
			String hostAddress = getHostAddressForECSAgentIntrospectionAPI();
			String response = EC2CredentialsUtils.getInstance().readResource(new URI(hostAddress + path));
			if (slurp) {
				items = Collections.singletonList(response);
			} else {
				items = Arrays.asList(response.split("\n"));
			}
			return items;
		} catch (AmazonClientException ace) {
			log.warn("Unable to retrieve the requested metadata.");
			return null;
		} catch (Exception e) {
			// Retry on any other exceptions
			int pause = (int) (Math.pow(2, DEFAULT_QUERY_RETRIES - tries) * MINIMUM_RETRY_WAIT_TIME_MILLISECONDS);
			try {
				Thread.sleep(pause < MINIMUM_RETRY_WAIT_TIME_MILLISECONDS ? MINIMUM_RETRY_WAIT_TIME_MILLISECONDS
						: pause);
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
			return getItems(path, tries - 1, slurp);
		}
	}

	public static String getHostAddressForECSAgentIntrospectionAPI() {
		String host = System.getProperty(ECS_AGENT_INTROSPECTION_API_OVERRIDE_SYSTEM_PROPERTY);
		return host != null ? host : ECS_AGENT_INTROSPECTION_API_URL;
	}

	/**
	 *
	 * @author Malte Geßner (malte.gessner@acando.de)
	 *
	 */
	public static class Metadata {

		private final String cluster;
		private final String containerInstanceArn;
		private final String version;

		@JsonCreator
		public Metadata(
				@JsonProperty("Cluster") String cluster,
				@JsonProperty("ContainerInstanceArn") String containerInstanceArn,
				@JsonProperty("Version") String version) {

			this.cluster = cluster;
			this.containerInstanceArn = containerInstanceArn;
			this.version = version;
		}

		public String getCluster() {
			return this.cluster;
		}

		public String getContainerInstanceArn() {
			return this.containerInstanceArn;
		}

		public String getVersion() {
			return this.version;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			if (getCluster() != null) {
				sb.append("Cluster: ").append(getCluster()).append(",");
			}
			if (getContainerInstanceArn() != null) {
				sb.append("ContainerInstanceArn: ").append(getContainerInstanceArn()).append(",");
			}
			if (getVersion() != null) {
				sb.append("Version: ").append(getVersion());
			}
			sb.append("}");
			return sb.toString();
		}

	}

	/**
	 *
	 * @author Malte Geßner (malte.gessner@acando.de)
	 *
	 */
	public static class Task {

		private final String arn;
		private final String desiredStatus;
		private final String knownStatus;
		private final String family;
		private final String version;
		private final List<Container> containers;

		@JsonCreator
		public Task(
				@JsonProperty("Arn") String arn,
				@JsonProperty("DesiredStatus") String desiredStatus,
				@JsonProperty("KnownStatus") String knownStatus,
				@JsonProperty("Family") String family,
				@JsonProperty("Version") String version,
				@JsonProperty("Containers") List<Container> containers) {

			this.arn = arn;
			this.desiredStatus = desiredStatus;
			this.knownStatus = knownStatus;
			this.family = family;
			this.version = version;

			if (containers != null) {
				this.containers = new ArrayList<>(containers);
			} else {
				this.containers = Collections.emptyList();
			}
		}

		public String getArn() {
			return this.arn;
		}

		public String getDesiredStatus() {
			return this.desiredStatus;
		}

		public String getKnownStatus() {
			return this.knownStatus;
		}

		public String getFamily() {
			return this.family;
		}

		public String getVersion() {
			return this.version;
		}

		public List<Container> getContainers() {
			return Collections.unmodifiableList(this.containers);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			if (getArn() != null) {
				sb.append("Arn: ").append(getArn()).append(",");
			}
			if (getDesiredStatus() != null) {
				sb.append("DesiredStatus: ").append(getDesiredStatus()).append(",");
			}
			if (getKnownStatus() != null) {
				sb.append("KnownStatus: ").append(getKnownStatus()).append(",");
			}
			if (getFamily() != null) {
				sb.append("Family: ").append(getFamily()).append(",");
			}
			if (getVersion() != null) {
				sb.append("Version: ").append(getVersion()).append(",");
			}
			if (getContainers() != null) {
				sb.append("Containers: ").append(getContainers());
			}
			sb.append("}");
			return sb.toString();
		}

	}

	/**
	 *
	 * @author Malte Geßner (malte.gessner@acando.de)
	 *
	 */
	public static class Container {

		private final String dockerId;
		private final String dockerName;
		private final String name;

		@JsonCreator
		public Container(
				@JsonProperty("DockerId") String dockerId,
				@JsonProperty("DockerName") String dockerName,
				@JsonProperty("Name") String name) {

			this.dockerId = dockerId;
			this.dockerName = dockerName;
			this.name = name;
		}

		public String getDockerId() {
			return this.dockerId;
		}

		public String getDockerName() {
			return this.dockerName;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			if (getDockerId() != null) {
				sb.append("DockerId: ").append(getDockerId()).append(",");
			}
			if (getDockerName() != null) {
				sb.append("DockerName: ").append(getDockerName()).append(",");
			}
			if (getName() != null) {
				sb.append("Name: ").append(getName());
			}
			sb.append("}");
			return sb.toString();
		}

	}

}
