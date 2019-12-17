package de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.model.Container;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.NetworkBinding;
import com.amazonaws.services.ecs.model.Task;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;

/**
 *
 * @author Malte Geßner
 *
 */
public class AmazonECSDiscoveryStrategy extends AbstractDiscoveryStrategy {

	private final AmazonECS ecsClient;
	private final AmazonEC2 ec2Client;
	private final int containerPort;

	private final AmazonECSDiscoveryUtils amazonECSDiscoveryUtils;

	private String clusterName;
	private String serviceName;

	@SuppressWarnings("rawtypes")
	public AmazonECSDiscoveryStrategy(ILogger logger,
			Map<String, Comparable> properties,
			AmazonECS ecsClient,
			AmazonEC2 ec2Client,
			int containerPort) {
		super(logger, properties);

		this.ecsClient = ecsClient;
		this.ec2Client = ec2Client;
		this.containerPort = containerPort;

		this.amazonECSDiscoveryUtils = new AmazonECSDiscoveryUtils(ecsClient);
	}

	@Override
	public void start() {
		try {
			this.clusterName = this.amazonECSDiscoveryUtils.discoverClusterName();
			this.serviceName = this.amazonECSDiscoveryUtils.discoverServiceName(this.clusterName).orElse(null);
		} catch (AmazonECSDiscoveryException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Iterable<DiscoveryNode> discoverNodes() {
		final Map<String, String> ipAddressByContainerInstanceArn = new HashMap<>();

		List<DiscoveryNode> nodes = getTasks().flatMap(ecsTask -> {
			String ipAddress = ipAddressByContainerInstanceArn.computeIfAbsent(
					ecsTask.getContainerInstanceArn(), k -> getIpAddress(getContainerInstance(ecsTask)));

			return getHazelcastNetworkBindings(ecsTask)
					.map(networkBinding -> getDiscoveryNode(ipAddress, networkBinding.getHostPort()))
					.filter(Objects::nonNull);
		}).filter(Objects::nonNull)
				.peek(node -> getLogger().fine("Discovered node: " + node.getPrivateAddress().toString()))
				.collect(Collectors.toList());

		if (nodes.isEmpty()) {
			getLogger().info("No nodes discovered");
		}

		return Collections.unmodifiableList(nodes);
	}

	private DiscoveryNode getDiscoveryNode(String ipAddress, Integer port) {
		if (ipAddress == null || port == null) {
			return null;
		}

		try {
			return new SimpleDiscoveryNode(new Address(ipAddress, port));
		} catch (UnknownHostException e) {
			getLogger().warning(
					"Failed to resolve node address; IP address: " + ipAddress + ", port: " + port, e);
		}

		return null;
	}

	private Stream<NetworkBinding> getHazelcastNetworkBindings(Task task) {
		return task.getContainers().stream()
				.peek(container -> getLogger().fine(
						"Found ECS container for ECS task [" + task.getTaskArn() + "]: " + container.getContainerArn()))
				.map(this::getHazelcastNetworkBinding)
				.filter(Objects::nonNull)
				.peek(networkBinding -> getLogger().fine(
						"Found Hazelcast network binding for ECS task [" + task.getTaskArn() + "]: " + networkBinding));
	}

	private NetworkBinding getHazelcastNetworkBinding(Container container) {
		return container.getNetworkBindings().stream()
				.peek(networkBinding -> getLogger().fine(
						"Found network binding for ECS container ["
								+ container.getContainerArn() + "]: " + networkBinding))
				.filter(networkBinding -> networkBinding.getContainerPort() == this.containerPort)
				.peek(networkBinding -> getLogger().fine(
						"Identified Hazelcast network binding for ECS container ["
								+ container.getContainerArn() + "]: " + networkBinding))
				.findFirst().orElse(null);
	}

	private String getIpAddress(ContainerInstance containerInstance) {
		if (containerInstance == null) {
			return null;
		}

		Instance ec2Instance = getEc2Instance(containerInstance);
		if (ec2Instance == null) {
			getLogger().warning("EC2 instance not found for ECS container instance: "
					+ containerInstance.getContainerInstanceArn());
			return null;
		}

		String ipAddress = ec2Instance.getPrivateIpAddress();
		getLogger().fine("Private IP address of ECS container instance ["
				+ containerInstance.getContainerInstanceArn() + "]: " + ipAddress);
		return ipAddress;
	}

	private Stream<Task> getTasks() {
		List<String> taskArns = getTaskArns();

		if (taskArns.size() > 0) {
			DescribeTasksRequest describeTasksRequest = new DescribeTasksRequest()
					.withCluster(this.clusterName)
					.withTasks(taskArns);

			List<Task> tasks = null;
			try {
				tasks = this.ecsClient.describeTasks(describeTasksRequest).getTasks();
			} catch (Exception e) {
				getLogger().severe("Failed to retrieve ECS task details", e);
			}

			if (tasks != null && tasks.size() > 0) {
				tasks.forEach(task -> getLogger().fine("ECS task details: " + task));
				return tasks.stream().filter(Objects::nonNull);
			}
			getLogger().warning("No ECS task details found");
		}

		return Stream.empty();
	}

	private List<String> getTaskArns() {
		ListTasksRequest listTasksRequest = new ListTasksRequest()
				.withCluster(this.clusterName);

		if (this.serviceName != null) {
			listTasksRequest.setServiceName(this.serviceName);
		}

		List<String> taskArns = null;
		try {
			taskArns = this.ecsClient.listTasks(listTasksRequest).getTaskArns();
		} catch (Exception e) {
			getLogger().severe("Failed to get list of ECS tasks", e);
		}

		if (taskArns != null && taskArns.size() > 0) {
			taskArns.forEach(taskArn -> getLogger().fine("Found ECS task: " + taskArn));
			return taskArns;
		}

		getLogger().warning("No ECS tasks found");
		return Collections.emptyList();
	}

	private ContainerInstance getContainerInstance(Task task) {
		if (task == null) {
			return null;
		}

		DescribeContainerInstancesRequest request = new DescribeContainerInstancesRequest()
				.withCluster(this.clusterName)
				.withContainerInstances(Collections.singletonList(task.getContainerInstanceArn()));

		List<ContainerInstance> containerInstances = null;

		try {
			containerInstances = this.ecsClient.describeContainerInstances(request).getContainerInstances();
		} catch (Exception e) {
			getLogger().severe(
					"Failed to get ECS container instances for ECS task [" + task.getTaskArn() + "]", e);
		}

		if (containerInstances != null && containerInstances.size() > 0) {
			// We provided a single container instance ARN, so we expect a single ContainerInstance
			ContainerInstance containerInstance = containerInstances.get(0);
			getLogger().fine("Found ECS container instance: " + containerInstance);
			return containerInstance;
		}

		getLogger().warning("No ECS container instances found for ECS task [" + task.getTaskArn() + "]");
		return null;
	}

	private Instance getEc2Instance(ContainerInstance containerInstance) {
		if (containerInstance == null) {
			return null;
		}

		DescribeInstancesRequest request = new DescribeInstancesRequest()
				.withInstanceIds(Collections.singletonList(containerInstance.getEc2InstanceId()));

		List<Reservation> reservations = null;
		try {
			reservations = this.ec2Client.describeInstances(request).getReservations();
		} catch (Exception e) {
			getLogger().severe("Failed to get EC2 instance for ECS container instance ["
					+ containerInstance.getContainerInstanceArn() + "]", e);
		}

		if (reservations != null && reservations.size() > 0) {
			// We provided single EC2 instance ID, so we expect a single Reservation and a single Instance
			List<Instance> instances = reservations.get(0).getInstances();
			if (instances.size() > 0) {
				return instances.get(0);
			}
		}

		return null;
	}

}
