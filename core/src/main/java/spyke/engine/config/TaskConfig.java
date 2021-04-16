package spyke.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableAsync
@EnableScheduling
public class TaskConfig {

    /**
     * A task executor (object) that is instantiated, assembled, and otherwise managed by a Spring IoC container.
     *
     * @return The task executor.
     */
    @Bean(name = "taskExecutor")
    public TaskExecutor threadPoolTaskExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);    // core pool size 8
        executor.setQueueCapacity(50);  // queue size 50
        executor.setMaxPoolSize(16);    // max pool size 16
        executor.setThreadNamePrefix("Spyke Task Executor");   // name for task executor
        executor.initialize();
        return executor;
    }

    /**
     * A task executor (object) that is instantiated, assembled, and otherwise managed by a Spring IoC container.
     *
     * @return The task executor for pcap4j.
     */
    @Bean(name = "pcap4jExecutor")
    public TaskExecutor threadPoolPcap4jExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);    // core pool size 8
        executor.setQueueCapacity(100); // queue size 200
        executor.setMaxPoolSize(32);    // max pool size 32
        executor.setThreadNamePrefix("Spyke Pcap4j Executor");   // name for task executor
        executor.initialize();
        return executor;
    }

    /**
     * A task scheduler (object) that is instantiated, assembled, and otherwise managed by a Spring IoC container.
     *
     * @return The task scheduler.
     */
    @Bean(name = "taskScheduler")
    public TaskScheduler threadPoolTaskScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // Recommended size based on the devices
        scheduler.setPoolSize(30);
        scheduler.setThreadNamePrefix("Spyke Task Scheduler");   // name for task scheduler
        scheduler.initialize();
        return scheduler;
    }
}
