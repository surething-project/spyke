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
import spyke.monitor.manage.DeviceManager;
import spyke.monitor.manage.IptablesLog;
import spyke.pcap4j.task.PacketHandler;
import spyke.pcap4j.task.PacketSender;

@Component
public class CommandLine implements CommandLineRunner {
    @Autowired
    private Environment env;
    @Autowired
    private TaskExecutor taskExecutor;
    @Autowired
    private TaskScheduler taskScheduler;
    @Autowired
    private ApplicationContext applicationContext;

    private Logger logger = LoggerFactory.getLogger(CommandLine.class);

    @Override
    public void run(String... args) throws Exception {

        // ADD RULES ON IPTABLES AT BOOTSTRAP
        DeviceManager deviceSender = applicationContext.getBean(DeviceManager.class);
        taskExecutor.execute(deviceSender);

        IptablesLog iptablesLog = applicationContext.getBean(IptablesLog.class);

        //CronTrigger cronTrigger = new CronTrigger("0 0 * * * ?");   // every hour
        CronTrigger cronTrigger = new CronTrigger("0 * * * * ?");   // every minute
        taskScheduler.schedule(
                iptablesLog, cronTrigger
        );

        // Execute PCAP4j
        PacketHandler packetHandler = applicationContext.getBean(PacketHandler.class);
        taskExecutor.execute(packetHandler);

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
}
