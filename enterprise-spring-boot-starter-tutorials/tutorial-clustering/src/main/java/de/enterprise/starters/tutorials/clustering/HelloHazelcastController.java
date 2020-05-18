package de.enterprise.starters.tutorials.clustering;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;

/**
 *
 * @author Jonas Keßler
 */
@RestController
public class HelloHazelcastController {

	@Autowired
	private HazelcastInstance hazelcastInstance;

	@GetMapping("/counter")
	public String countUp() {
		IAtomicLong atomicLong = this.hazelcastInstance.getAtomicLong("test");
		long addAndGet = atomicLong.addAndGet(1L);
		return Long.toString(addAndGet);
	}

}
