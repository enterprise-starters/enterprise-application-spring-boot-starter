package de.enterprise.starters.tutorials.clustering;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Jonas Keßler
 */
@Service
@Slf4j
public class CronDemoService {

	@Scheduled(cron = "0/5 * * * * ?")
	public void doSomething() {
		log.info("Hello");
	}
}
