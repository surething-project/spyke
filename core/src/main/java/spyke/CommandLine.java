package spyke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import spyke.engine.manage.DeviceManager;
import spyke.engine.manage.IptablesLog;
import spyke.engine.pcap4j.task.PacketHandler;

@Component
public class CommandLine implements CommandLineRunner {

    /**
     * The automatic dependency injection for environment where profile and properties are used.
     */
    @Autowired
    private Environment env;
    /**
     * The automatic dependency injection for execution tasks.
     */
    @Autowired
    private TaskExecutor taskExecutor;
    /**
     * The automatic dependency injection pcap4j tasks.
     */
    @Autowired
    private TaskExecutor pcap4jExecutor;
    /**
     * The automatic dependency injection for scheduler tasks.
     */
    @Autowired
    private TaskScheduler taskScheduler;
    /**
     * The automatic dependency injection for application context.
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * The logger.
     */
    private Logger logger = LoggerFactory.getLogger(CommandLine.class);

    @Override
    public void run(final String... args) throws Exception {

        manageExecutionTasks();
        manageSchedulerTasks();
    }

    /**
     * Manage iptables rules at bootstrap. It includes adding default rules and devices rules.
     *
     */
    private void manageExecutionTasks() {

        final DeviceManager deviceSender = this.applicationContext.getBean(DeviceManager.class);
        this.taskExecutor.execute(deviceSender);

        final PacketHandler packetHandler = this.applicationContext.getBean(PacketHandler.class);
        this.pcap4jExecutor.execute(packetHandler);
        /* STORE DATA TO DATABASE, NOTE: more data we are receiving, less interval we should have
        TODO this may not be working
        //CronTrigger cronTrigger = new CronTrigger("* * * * * ?");   // every second right now
        //CronTrigger cronTrigger = new CronTrigger("0 * * * * ?");   // every minute right now
        CronTrigger cronTrigger5Min = new CronTrigger("0 5 * * * ?");   // perhaps every hour at 5 mins
        PacketSender packetSender = applicationContext.getBean(PacketSender.class);
        taskScheduler.schedule(
                packetSender, cronTrigger5Min
        );
         */
    }

    private void manageSchedulerTasks() {
        final IptablesLog iptablesLog = this.applicationContext.getBean(IptablesLog.class);

        //CronTrigger cronTrigger = new CronTrigger("0 0 * * * ?");   // every hour
        final CronTrigger cronTrigger = new CronTrigger("0 * * * * ?");   // every minute
        this.taskScheduler.schedule(iptablesLog, cronTrigger);
    }
}
