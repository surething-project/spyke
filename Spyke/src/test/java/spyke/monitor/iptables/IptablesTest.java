package spyke.linux;

import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import spyke.monitor.iptables.util.OperatingSystem;

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
        String task = "sudo /usr/bin/which iptables";
        processBuilder = new ProcessBuilder("/bin/sh", "-c", task);

        String PATH = System.getProperty("user.dir");
        String directoryName = PATH.concat(File.separator+"iptables-log");
        String logsPath = directoryName.concat(File.separator+"log");
        File directory = new File(logsPath);
        if (! directory.exists()){
            directory.mkdirs();
        }
        File file = new File(directoryName + File.separator + "iptables.log");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void erase() {
        String PATH = System.getProperty("user.dir");
        String directoryName = PATH.concat(File.separator+"iptables-log");
        File logsPath = new File(directoryName.concat(File.separator+"log"));
        File directory = new File(directoryName);
        if (logsPath.exists()){
            String[]entries = logsPath.list();
            for(String s: entries){
                File currentFile = new File(logsPath.getPath(),s);
                currentFile.delete();
            }
        }
        if (directory.exists()){
            String[]entries = directory.list();
            for(String s: entries){
                File currentFile = new File(directory.getPath(),s);
                currentFile.delete();
            }
        }
        directory.delete();
    }

    @Test
    public void getIptables(){
        if(OperatingSystem.isLinux()){
            StringBuilder input = new StringBuilder();
            StringBuilder error = new StringBuilder();
            try{
                String line;
                Process process = processBuilder.start();
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((line = inputReader.readLine()) != null){
                    input.append(line);
                }
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = errorReader.readLine()) != null)
                {
                    error.append(line);
                }
                process.waitFor();
                inputReader.close();
                errorReader.close();
            } catch (Exception ignored) {
                System.out.println("Exception:");
                System.out.println(ignored.getMessage());
            }
            Assert.assertNotEquals("", input.toString());
            Assert.assertEquals("", error.toString());
        }
    }

}
