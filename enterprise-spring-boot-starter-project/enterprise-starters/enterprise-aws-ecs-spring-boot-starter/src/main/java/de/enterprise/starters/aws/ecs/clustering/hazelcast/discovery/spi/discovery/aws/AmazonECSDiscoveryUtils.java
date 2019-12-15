package de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Optional;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.model.Container;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.Task;
import com.amazonaws.util.EC2MetadataUtils;

/**
 *
 * @author Malte Geßner
 *
 */
public class AmazonECSDiscoveryUtils {

	private final AmazonECS amazonECS;

	public AmazonECSDiscoveryUtils(AmazonECS amazonECS) {
		this.amazonECS = amazonECS;
	}

	/**
	 * Discovers the name of the ECS Cluster associated with the Docker container this process is running in.
	 *
	 * @return the ECS Cluster name
	 * @throws ClusterNameDiscoveryException
	 *             Cluster name discovery failed
	 */
	public String discoverClusterName() throws ClusterNameDiscoveryException {
		try {
			return AmazonECSAgentIntrospectionUtils.getMetadata().getCluster();
		} catch (Exception e) {
			throw new ClusterNameDiscoveryException("Cluster name discovery failed", e);
		}
	}

	/**
	 * Discovers the name of the ECS Service associated with the Docker container this process is running in, if any.
	 *
	 * @param clusterName
	 *            the name of the ECS cluster this process is running in; see {@link #discoverClusterName()}
	 * @return the ECS service name, if any
	 * @throws ServiceNameDiscoveryException
	 *             Service name discovery failed
	 */
	public Optional<String> discoverServiceName(String clusterName) throws ServiceNameDiscoveryException {
		try {
			String[] groupParts = getTask(clusterName, getAgentTask(getShortContainerId()).getArn())
					.getGroup().split(":");
			if (groupParts.length == 2 && "service".equals(groupParts[0])) {
				return Optional.of(groupParts[1]);
			}
		} catch (Exception e) {
			throw new ServiceNameDiscoveryException(e);
		}

		// The task group indicates that the task is not part of a service
		return Optional.empty();
	}

	/**
	 * Discovers the public Hazelcast address of this process assuming this process is running in an ECS container where the container port
	 * the Hazelcast instance is bound to has been dynamically mapped to a host port on the ECS container instance by ECS. The address
	 * returned (if any) is intended to be passed to {@link com.hazelcast.config.NetworkConfig#setPublicAddress(java.lang.String)}.
	 *
	 * @param containerPort
	 *            the port Hazelcast is listening on inside the container this process is running in; see
	 *            {@link com.hazelcast.config.NetworkConfig#getPort()}
	 * @return the public Hazelcast address
	 * @throws PublicHazelcastAddressDiscoveryException
	 *             Public Hazelcast address discovery failed
	 */
	public String discoverPublicHazelcastAddress(int containerPort)
			throws PublicHazelcastAddressDiscoveryException {

		try {
			return getContainer(discoverClusterName(), getShortContainerId()).getNetworkBindings().stream()
					.filter(networkBinding -> networkBinding.getContainerPort() == containerPort)
					.map(networkBinding -> EC2MetadataUtils.getPrivateIpAddress() + ":" + networkBinding.getHostPort())
					.findFirst()
					.orElseThrow(PublicHazelcastAddressNotFoundException::new);
		} catch (Exception e) {
			throw new PublicHazelcastAddressDiscoveryException("Public address discovery failed", e);
		}
	}

	private Container getContainer(
			String clusterName, String shortContainerId) throws AmazonECSDiscoveryException {

		try {
			AmazonECSAgentIntrospectionUtils.Task agentTask = getAgentTask(shortContainerId);
			String containerName = getAgentContainer(agentTask, shortContainerId).getName();
			return getTask(clusterName, agentTask.getArn()).getContainers().stream()
					.filter(container -> containerName.equals(container.getName()))
					.findFirst()
					.orElseThrow(() -> new AmazonECSDiscoveryException(String.format("Container not found for " +
							"cluster name: %s, short container ID: %s", clusterName, shortContainerId)));
		} catch (AmazonECSDiscoveryException e) {
			throw new AmazonECSDiscoveryException(String.format("Container not found for cluster name: %s, " +
					"short container ID: %s", clusterName, shortContainerId), e);
		}
	}

	private AmazonECSAgentIntrospectionUtils.Container getAgentContainer(
			AmazonECSAgentIntrospectionUtils.Task agentTask, String shortContainerId)
			throws AmazonECSDiscoveryException {

		return agentTask.getContainers().stream()
				.filter(c -> c.getDockerId().startsWith(shortContainerId))
				.findFirst()
				.orElseThrow(() -> new AmazonECSDiscoveryException(
						"Container not found for short container ID: " + shortContainerId));
	}

	private Task getTask(String clusterName, String taskArn) {
		DescribeTasksRequest request = new DescribeTasksRequest()
				.withCluster(clusterName)
				.withTasks(Collections.singletonList(taskArn));

		// We provided a single task ARN, so we expect a single Task
		return this.amazonECS.describeTasks(request).getTasks().get(0);
	}

	private AmazonECSAgentIntrospectionUtils.Task getAgentTask(
			String shortContainerId) throws AmazonECSDiscoveryException {

		AmazonECSAgentIntrospectionUtils.Task task = AmazonECSAgentIntrospectionUtils.getTask(shortContainerId);
		if (task != null) {
			return task;
		}
		throw new AmazonECSDiscoveryException("Task not found for short container ID: " + shortContainerId);
	}

	private String getShortContainerId() throws UnknownHostException {
		return InetAddress.getLocalHost().getHostName();
	}

}
