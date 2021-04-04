package spyke.website;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PageViewsTest {

    @LocalServerPort
    private int port;

    private URL base;

    @Autowired
    private TestRestTemplate template;

    @BeforeEach
    public void setUp() throws Exception {
        this.base = new URL("http://localhost:" + this.port + "/");
    }

    @AfterEach
    public void erase() {
        final String PATH = System.getProperty("user.dir");
        final String directoryName = PATH.concat(File.separator + "iptables-log");
        final File logsPath = new File(directoryName.concat(File.separator + "log"));
        final File directory = new File(directoryName);
        if (logsPath.exists()) {
            final String[] entries = logsPath.list();
            for (final String s : entries) {
                final File currentFile = new File(logsPath.getPath(), s);
                currentFile.delete();
            }
        }
        if (directory.exists()) {
            final String[] entries = directory.list();
            for (final String s : entries) {
                final File currentFile = new File(directory.getPath(), s);
                currentFile.delete();
            }
        }
        directory.delete();
    }

    @Test
    public void getDevices() throws Exception {
        final ResponseEntity<String> response = this.template.getForEntity(
                this.base.toString() + "devices",
                String.class
        );
        assertThat(response.getStatusCodeValue())
                .as("The response code should be 200")
                .isEqualTo(200);
    }
}
