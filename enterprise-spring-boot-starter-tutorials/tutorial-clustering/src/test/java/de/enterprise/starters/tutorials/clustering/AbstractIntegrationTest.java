package de.enterprise.starters.tutorials.clustering;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.enterprise.spring.boot.application.starter.application.AbstractEnterpriseApplication;

/**
 *
 * @author Jonas Keßler
 */
@ActiveProfiles(AbstractEnterpriseApplication.INTEGRATION_TEST_PROFILE)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TutorialClusteringApplication.class)
public class AbstractIntegrationTest {

}
