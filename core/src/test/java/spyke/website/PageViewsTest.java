package spyke.website;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PageViewsTest {

    @LocalServerPort
    private int port;

    private URL base;

    @Autowired
    private TestRestTemplate template;

    @Before
    public void setUp() throws Exception {
        this.base = new URL("http://localhost:" + port + "/");
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
    public void getDevices() throws Exception {
        ResponseEntity<String> response = template.getForEntity(base.toString()+"devices",
                String.class);
        assertEquals(response.getStatusCodeValue(), 200);
    }
}