package de.enterprise.starters.tutorials.basics;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.enterprise.spring.boot.application.starter.application.AbstractApplication;
import de.enterprise.starters.tutorials.basics.TutorialBasicsApplication;

/**
 *
 * @author Jonas Keßler
 */
@ActiveProfiles(AbstractApplication.INTEGRATION_TEST_PROFILE)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TutorialBasicsApplication.class)
public class AbstractIntegrationTest {

}
