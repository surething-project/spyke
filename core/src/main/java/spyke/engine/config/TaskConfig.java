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
        executor.setCorePoolSize(10);   // core pool size 10
        executor.setQueueCapacity(200); // queue size 100
        executor.setMaxPoolSize(50);    // max pool size 50
        executor.setThreadNamePrefix("Spyke Task Executor");   // name for task executor
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
        scheduler.setPoolSize(100);
        scheduler.setThreadNamePrefix("Spyke Task Scheduler");   // name for task scheduler
        scheduler.initialize();
        return scheduler;
    }
}
