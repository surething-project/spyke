package spyke.engine.iptables;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import spyke.engine.util.OperatingSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IptablesFilesTest {

    private ProcessBuilder processBuilder;

    @BeforeEach
    public void setup() throws IOException {
        final String task = "/usr/bin/which iptables";
        this.processBuilder = new ProcessBuilder("/bin/sh", "-c", task);

        final String PATH = System.getProperty("user.dir");
        final String directoryName = PATH.concat(File.separator + "iptables_log");
        final String logsPath = directoryName.concat(File.separator + "log");
        final File directory = new File(logsPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        final File file = new File(directoryName + File.separator + "iptables.log");
        file.createNewFile();
    }

    @AfterEach
    public void erase() {
        final String PATH = System.getProperty("user.dir");
        final String directoryName = PATH.concat(File.separator + "iptables_log");
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
    public void getIptables() throws IOException, InterruptedException {

        assumeTrue(OperatingSystem.isLinux());

        final StringBuilder input = new StringBuilder();
        final StringBuilder error = new StringBuilder();
        final Process process = this.processBuilder.start();

        try (final BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = inputReader.readLine()) != null) {
                input.append(line);
            }
        }

        try (final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                error.append(line);
            }
        }

        assertThat(input)
                .as("List of input should not be empty")
                .isNotEmpty();
        assertThat(error)
                .as("List of error should be empty")
                .isEmpty();
        process.waitFor();
    }
}
