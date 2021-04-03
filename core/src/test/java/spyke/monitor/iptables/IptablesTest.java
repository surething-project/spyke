package spyke.monitor.iptables;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import spyke.monitor.util.OperatingSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class IptablesTest {

    private ProcessBuilder processBuilder;

    @Before
    public void setup() {
        final String task = "sudo /usr/bin/which iptables";
        this.processBuilder = new ProcessBuilder("/bin/sh", "-c", task);

        final String PATH = System.getProperty("user.dir");
        final String directoryName = PATH.concat(File.separator + "iptables-log");
        final String logsPath = directoryName.concat(File.separator + "log");
        final File directory = new File(logsPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        final File file = new File(directoryName + File.separator + "iptables.log");
        try {
            file.createNewFile();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @After
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
    public void getIptables() {
        if (OperatingSystem.isLinux()) {
            final StringBuilder input = new StringBuilder();
            final StringBuilder error = new StringBuilder();
            try {
                String line;
                final Process process = this.processBuilder.start();
                final BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((line = inputReader.readLine()) != null) {
                    input.append(line);
                }
                final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = errorReader.readLine()) != null) {
                    error.append(line);
                }
                process.waitFor();
                inputReader.close();
                errorReader.close();
            } catch (final Exception ignored) {
                System.out.println("Exception:");
                System.out.println(ignored.getMessage());
            }
            Assert.assertNotEquals("", input.toString());
            Assert.assertEquals("", error.toString());
        }
    }

}
