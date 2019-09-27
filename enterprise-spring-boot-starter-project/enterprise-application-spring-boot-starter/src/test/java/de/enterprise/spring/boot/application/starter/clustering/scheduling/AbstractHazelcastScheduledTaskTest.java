package de.enterprise.spring.boot.application.starter.clustering.scheduling;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import de.enterprise.spring.boot.application.starter.clustering.scheduling.task.AbstractHazelcastScheduledTask;

@ExtendWith(MockitoExtension.class)
public class AbstractHazelcastScheduledTaskTest {

	@Mock
	HazelcastInstance hazelcastInstance;

	@InjectMocks
	AbstractHazelcastScheduledTask scheduledTask = new AbstractHazelcastScheduledTask() {
	};

	@SuppressWarnings("unchecked")
	@Test
	public void getMap() {

		when(this.hazelcastInstance.getMap(Mockito.anyString())).thenReturn(Mockito.mock(IMap.class));

		this.scheduledTask.setTaskProperty("testProperty", 15);
		this.scheduledTask.getTaskProperty("testProperty");

		verify(this.hazelcastInstance, times(2)).getMap(AbstractHazelcastScheduledTaskTest.class.getName());

	}

}
