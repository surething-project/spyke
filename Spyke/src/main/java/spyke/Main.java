package spyke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Main {

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Main process, where the Spring application starts.
     *
     * @param args        The command line arguments.
     */
    public static void main(final String[] args) {
        final SpringApplication application = new SpringApplication(Main.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.run(args);
    }
}


