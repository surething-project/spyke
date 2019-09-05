package spyke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import spyke.monitor.manage.DeviceManager;

@SpringBootApplication
@EnableScheduling
public class Main {

    private Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Main.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.run(args);
    }
}


