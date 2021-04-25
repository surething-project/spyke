package spyke.engine.iptables;

import com.google.common.base.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import spyke.engine.iptables.component.Iptables;
import spyke.engine.util.OperatingSystem;

import java.io.File;
import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test class that ensures that iptables rules work as expected.
 * <ol>
 *     <li>docker build -t spyke-test .</li>
 *     <li>docker run -d -it --privileged --name spyke-test spyke-test</li>
 *     <li>docker exec -it spyke-test /bin/bash</li>
 *     <li>mvn test -pl :core -Dtest=IptablesRulesTest</li>
 * </ol>
 *
 * @author Sheng Wang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IptablesRulesTest {

    /**
     * The injected iptables.
     */
    @Autowired
    private Iptables iptables;

    /**
     * The path for iptables rules that used to backup the current rules.
     */
    private final static File saveIptablesFile = new File("script/config/saved_iptables.conf");

    @BeforeEach
    public void setup() {
        assumeTrue(OperatingSystem.isLinux());
        assumeTrue(this.iptables.isAdmin());
        assumeTrue(this.iptables.saveSystemRules(saveIptablesFile));
    }

    @AfterEach
    public void erase() {
        assumeTrue(this.iptables.restoreSystemRules(saveIptablesFile));
    }

    /**
     * Restore iptables and check if rules are applied.
     * <p>
     *     Note: if the current iptables is the same as the default iptables. The test will fail.
     * </p>
     */
    @Test
    public void restoreDefaultRules() {

        final Optional<String> filter = Optional.of("-A ");
        final List<String> iptables = this.iptables.executeWithResult("sudo /sbin/iptables-save", filter);
        final List<String> currentIptables = this.iptables.executeWithResult("sudo /sbin/iptables-save", filter);

        assertThat(iptables.stream().collect(toImmutableList()))
                .as("The current iptables should be different comparing to default iptables.")
                .isEqualTo(currentIptables.stream().collect(toImmutableList()));

        assertThat(this.iptables.restoreDefaultRules())
                .as("Restore default rules should succeed.")
                .isTrue();

        final List<String> defaultIptables = this.iptables.executeWithResult("sudo /sbin/iptables-save", filter);

        assertThat(currentIptables.stream().collect(toImmutableList()))
                .as("The current iptables should be different comparing to default iptables.")
                .isNotEqualTo(defaultIptables.stream().collect(toImmutableList()));
    }

    /**
     * Ensure rules are created correctly when new device is added.
     */
    @Test
    public void rulesAreCorrectlyCreated() {

    }
}
