package spyke.monitor.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import spyke.database.model.Device;
import spyke.database.model.types.TUnit;
import spyke.monitor.manage.PeriodManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class ScheduleConfig {

    @Autowired
    private TaskScheduler taskScheduler;
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * A {@code Map} with key {@code device} and corresponding scheduled task.
     */
    private final Map<Device, ScheduledFuture<?>> schedules = new ConcurrentHashMap<Device, ScheduledFuture<?>>();

    /**
     * Creates a cron job with given time value and unit.
     * <p />
     * If time is 0, the job default to every hour.
     *
     * @param timeValue The time value.
     * @param tUnit     The time unit.
     * @return The cron job.
     */
    public CronTrigger createCronTrigger(final long timeValue, final TUnit tUnit) {
        final CronTrigger cronTrigger;
        if (timeValue == 0)
            return new CronTrigger("0 0 * * * ?");  // every hour as default
        switch (tUnit) {
            case m:
                cronTrigger = new CronTrigger("0 */" + timeValue + " * * * ?"); // [0 */minute * * * ?]
                break;
            case h:
                cronTrigger = new CronTrigger("0 0 */" + timeValue + " * * ?"); // [0 0 */hour * * ?]
                break;
            case d:
                cronTrigger = new CronTrigger("0 0 0 */" + timeValue + " * ?"); // [0 0 0 */day * * ?]
                break;
            default:
                cronTrigger = new CronTrigger("0 0 * * * ?");   // every hour as default
        }
        return cronTrigger;
    }

    public void setScheduler(final Device device) {
        final PeriodManager periodSender = this.applicationContext.getBean(PeriodManager.class);
        final CronTrigger cronTrigger = periodSender.setDevice(device);

        final ScheduledFuture<?> scheduledFuture = this.taskScheduler.schedule(
                periodSender, cronTrigger
        );
        this.schedules.put(device, scheduledFuture);
    }

    public boolean isCancelled(final Device device) {
        return this.schedules.get(device).isCancelled();
    }

    public void cancelScheduler(final Device device) {
        this.schedules.get(device).cancel(false);
    }

    public boolean exists(final Device device) {
        return this.schedules.containsKey(device);
    }

    public boolean isDone(final Device device) {
        return this.schedules.get(device).isDone();
    }

    public ScheduledFuture<?> getScheduleByDevice(final Device device) {
        return this.schedules.get(device);
    }
}
