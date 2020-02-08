package spyke.monitor.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import spyke.database.model.Device;
import spyke.database.model.Period;
import spyke.database.model.PeriodId;
import spyke.database.variable.TUnit;
import spyke.monitor.manage.PeriodManager;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class ScheduleConfig {
    @Autowired
    private TaskScheduler taskScheduler;
    @Autowired
    private ApplicationContext applicationContext;
    private Map<Device, ScheduledFuture<?>> schedules = new ConcurrentHashMap<Device, ScheduledFuture<?>>();

    public CronTrigger createCronTrigger(long timeValue, TUnit tUnit){
        CronTrigger cronTrigger;
        if(timeValue==0)
            return new CronTrigger("0 0 * * * ?");  // every hour as default
        switch (tUnit){
            case m:
                cronTrigger = new CronTrigger("0 */"+timeValue+" * * * ?"); // [0 */minute * * * ?]
                break;
            case h:
                cronTrigger = new CronTrigger("0 0 */"+timeValue+" * * ?"); // [0 0 */hour * * ?]
                break;
            case d:
                cronTrigger = new CronTrigger("0 0 0 */"+timeValue+" * ?"); // [0 0 0 */day * * ?]
                break;
            default:
                cronTrigger = new CronTrigger("0 0 * * * ?");   // every hour as default
        }
        return cronTrigger;
    }

    public void setScheduler(Device device){
        PeriodManager periodSender = applicationContext.getBean(PeriodManager.class);
        CronTrigger cronTrigger = periodSender.setDevice(device);

        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(
                periodSender, cronTrigger
        );
        schedules.put(device, scheduledFuture);
    }

    public boolean isCancelled(Device device){
        return schedules.get(device).isCancelled();
    }

    public void cancelScheduler(Device device){
        schedules.get(device).cancel(false);
    }

    public boolean exists(Device device){
        return schedules.containsKey(device);
    }

    public boolean isDone(Device device){
        return schedules.get(device).isDone();
    }

    public ScheduledFuture<?> getScheduleByDevice(Device device){
        return schedules.get(device);
    }
}
