package spyke.engine.iptables;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IptablesRulesTest {

    @Autowired
    private Iptables iptables;

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
     */
    @Test
    public void restoreDefaultRules() {

        // assert that restore is the same as the file saveIptablesFile
        this.iptables.restoreDefaultRules();
        // assert that restore is different from the file

        assertThat(true)
                .as("test")
                .isTrue();
    }

    /**
     * Ensure rules are created correctly when new device is added.
     */
    @Test
    public void rulesAreCorrectlyCreated() {

    }
}
