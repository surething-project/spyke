package spyke.monitor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import spyke.CommandLine;
import spyke.database.model.Device;
import spyke.database.repository.PeriodRepository;
import spyke.database.model.types.Status;
import spyke.database.model.types.TUnit;
import spyke.database.model.types.BUnit;
import spyke.monitor.config.ScheduleConfig;
import spyke.monitor.manage.DeviceManager;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class TaskExecutorTest {

    @Autowired
    private CommandLine commandLine;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private ScheduleConfig scheduleConfig;

    private DeviceManager deviceSender;

    private Device device;

    @Before
    public void setup() {
        deviceSender = applicationContext.getBean(DeviceManager.class);
        taskExecutor.execute(deviceSender);
        device = new Device("192.168.8.24",
                "f0:18:98:05:64:90",
                "Shengs-MBP",
                Status.NEW,
                0,
                0,
                0,
                BUnit.kb,
                BUnit.kb,
                TUnit.m);
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
    public void twoDeviceTest(){
        Device device2 = new Device("192.168.8.71",
                "b0:4e:26:57:b8:33",
                "HS110",
                Status.NEW,
                0,
                0,
                0,
                BUnit.kb,
                BUnit.kb,
                TUnit.m);

        device.setPeriod(15);
        scheduleConfig.setScheduler(device);
        Assert.assertFalse(scheduleConfig.isDone(device));


        device2.setPeriod(10);
        scheduleConfig.setScheduler(device2);
        Assert.assertFalse(scheduleConfig.isDone(device2));

        Date date = new Date();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        int now = calendar.get(Calendar.MINUTE);

        System.out.println("Time: "+scheduleConfig.getScheduleByDevice(device).getDelay(TimeUnit.MINUTES)+" and "+(4-(now%5)));
        // (4-(now%10)) time left to each 5 minutes
        Assert.assertTrue((14-(now%15))<= scheduleConfig.getScheduleByDevice(device).getDelay(TimeUnit.MINUTES) &&
                scheduleConfig.getScheduleByDevice(device).getDelay(TimeUnit.MINUTES)<(15-(now%15))
        );

        System.out.println("Time: "+scheduleConfig.getScheduleByDevice(device).getDelay(TimeUnit.MINUTES)+" and "+(9-(now%10)));
        // (9-(now%10)) time left to each 10 minutes
        Assert.assertTrue((9-(now%10))<= scheduleConfig.getScheduleByDevice(device2).getDelay(TimeUnit.MINUTES) &&
                scheduleConfig.getScheduleByDevice(device2).getDelay(TimeUnit.MINUTES)<(10-(now%10))
        );
    }

    @Test
    public void taskCreate() {
        Assert.assertNotEquals(null, deviceSender);
    }

    @Test
    public void taskAdd(){
        scheduleConfig.setScheduler(device);
        Assert.assertTrue(scheduleConfig.exists(device));

    }

    @Test
    public void taskRunning(){
        Date date = new Date();
        device.setPeriod(5);
        scheduleConfig.setScheduler(device);
        Assert.assertFalse(scheduleConfig.isDone(device));
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        int now = calendar.get(Calendar.MINUTE);
        // (9-(now%10)) time left to each 10 minutes
        Assert.assertTrue((4-(now%5))<= scheduleConfig.getScheduleByDevice(device).getDelay(TimeUnit.MINUTES) &&
                scheduleConfig.getScheduleByDevice(device).getDelay(TimeUnit.MINUTES)<(5-(now%5))
        );
    }

    @Test
    public void taskCancel() {
        scheduleConfig.setScheduler(device);
        Assert.assertFalse(scheduleConfig.isCancelled(device));

        scheduleConfig.cancelScheduler(device);
        Assert.assertTrue(scheduleConfig.isCancelled(device));
    }
}
