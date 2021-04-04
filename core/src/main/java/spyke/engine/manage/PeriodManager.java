package spyke.engine.manage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Service;
import spyke.database.model.Device;
import spyke.database.model.Period;
import spyke.database.model.PeriodId;
import spyke.database.repository.PeriodRepository;
import spyke.database.model.types.TUnit;
import spyke.engine.iptables.component.Iptables;
import spyke.engine.util.OperatingSystem;
import spyke.engine.config.ScheduleConfig;

import java.util.Date;

@Service
@Scope("prototype")
public class PeriodManager implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PeriodManager.class);
    @Autowired
    private PeriodRepository periodRepository;
    @Autowired
    private Iptables iptables;
    @Autowired
    private ScheduleConfig scheduleConfig;
    private Period period;
    private Device device;
    private CronTrigger cronTrigger;
    public CronTrigger setDevice(Device device){
        //set device
        this.device = device;

        if(device.getPeriod() != 0)
            cronTrigger = scheduleConfig.createCronTrigger(device.getPeriod(), device.getPeriodUnit());
        else
            cronTrigger = scheduleConfig.createCronTrigger(1, TUnit.h);

        // if device already has the task/period and not cancelled -> cancel it first
        if(scheduleConfig.exists(device) && !scheduleConfig.isCancelled(device)){
            scheduleConfig.cancelScheduler(device);
            logger.error(device.getIp() + " already exists and it is cancelled");
        }
        this.setPeriod();
        return cronTrigger;
    }
    public void setPeriod(){
        // set periodId and related device
        Date now = new Date();
        SimpleTriggerContext triggerContext = new SimpleTriggerContext();
        triggerContext.update(null, null, now);
        PeriodId periodId = new PeriodId(now, cronTrigger.nextExecutionTime(triggerContext));
        periodId.setDevice(this.device);
        // set period
        this.period = new Period(periodId, 0, 0);
        //logger.info(this.period.toString());
    }
    @Override
    public void run() {
        if (OperatingSystem.isLinux()) {
            // get passed and dropped byte and change period
            long[] bytes = iptables.extractBytes(period.getId().getDevice());
            if (bytes.length != 2) {
                logger.error("bytes error!");
            }
            this.period.setPassedBytes(bytes[0]);
            this.period.setDroppedBytes(bytes[1]);
            // renew rules
            iptables.renewRules(device);
        }
        periodRepository.saveAndFlush(this.period);
        logger.info(this.period + " saved");
        // renew period
        setPeriod();
    }
}
