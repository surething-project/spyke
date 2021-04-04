package spyke.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import spyke.CommandLine;
import spyke.database.model.Device;
import spyke.database.model.types.BUnit;
import spyke.database.model.types.Status;
import spyke.database.model.types.TUnit;
import spyke.database.repository.PeriodRepository;
import spyke.engine.config.ScheduleConfig;
import spyke.engine.manage.DeviceManager;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
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

    @BeforeEach
    public void setup() {
        this.deviceSender = this.applicationContext.getBean(DeviceManager.class);
        this.taskExecutor.execute(this.deviceSender);
        this.device = new Device(
                "192.168.8.24",
                "f0:18:98:05:64:90",
                "Shengs-MBP",
                Status.NEW,
                0,
                0,
                0,
                BUnit.kb,
                BUnit.kb,
                TUnit.m
        );
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
    public void twoDeviceTest() {
        final Device device2 = new Device(
                "192.168.8.71",
                "b0:4e:26:57:b8:33",
                "HS110",
                Status.NEW,
                0,
                0,
                0,
                BUnit.kb,
                BUnit.kb,
                TUnit.m
        );

        final int periodFifteenMinutes = 15;
        this.device.setPeriod(periodFifteenMinutes);
        this.scheduleConfig.setScheduler(this.device);
        assertThat(this.scheduleConfig.isDone(this.device))
                .as("Scheduled config of device1 is not done")
                .isFalse();

        final int periodTenMinutes = 10;
        device2.setPeriod(periodTenMinutes);
        this.scheduleConfig.setScheduler(device2);
        assertThat(this.scheduleConfig.isDone(device2))
                .as("Scheduled config of device2 is not done")
                .isFalse();

        final Date date = new Date();
        final Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        final int now = calendar.get(Calendar.MINUTE);

        final long deviceOneDelay = this.scheduleConfig.getScheduleByDevice(this.device).getDelay(TimeUnit.MINUTES);
        assertThat((14 - (now % periodFifteenMinutes)) <= deviceOneDelay && deviceOneDelay < (15 - (now % periodFifteenMinutes)))
                .as("(14 - (now % 15)) time left to each 15 minutes")
                .isTrue();

        final long deviceTwoDelay = this.scheduleConfig.getScheduleByDevice(device2).getDelay(TimeUnit.MINUTES);
        assertThat((9 - (now % periodTenMinutes)) <= deviceTwoDelay && deviceTwoDelay < (10 - (now % periodTenMinutes)))
                .as("(9 - (now % 10)) time left to each 10 minutes")
                .isTrue();
    }

    @Test
    public void taskCreate() {

        assertThat(this.deviceSender)
                .as("Device manager should not be null")
                .isNotNull();
    }

    @Test
    public void taskAdd() {

        this.scheduleConfig.setScheduler(this.device);
        assertThat(this.scheduleConfig.exists(this.device))
                .as("Scheduled config exists")
                .isTrue();
    }

    @Test
    public void taskRunning() {

        final Date date = new Date();
        this.device.setPeriod(5);
        this.scheduleConfig.setScheduler(this.device);
        assertThat(this.scheduleConfig.isDone(this.device))
                .as("Scheduled config should not be created.")
                .isFalse();

        final Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        final int now = calendar.get(Calendar.MINUTE);
        final long deviceDelay = this.scheduleConfig.getScheduleByDevice(this.device).getDelay(TimeUnit.MINUTES);
        assertThat((4 - (now % 5)) <= deviceDelay && deviceDelay < (5 - (now % 5)))
                .as("(4 - (now % 5)) time left to each 5 minutes")
                .isTrue();
    }

    @Test
    public void taskCancel() {
        this.scheduleConfig.setScheduler(this.device);
        assertThat(this.scheduleConfig.isCancelled(this.device))
                .as("Scheduled config is not cancelled")
                .isFalse();

        this.scheduleConfig.cancelScheduler(this.device);
        assertThat(this.scheduleConfig.isCancelled(this.device))
                .as("Scheduled config is cancelled")
                .isTrue();
    }
}
