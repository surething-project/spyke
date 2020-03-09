package spyke.pcap4j.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import spyke.database.model.Device;
import spyke.database.repository.DeviceRepository;
import spyke.database.variable.BUnit;
import spyke.database.variable.Status;
import spyke.database.variable.TUnit;

import java.util.Scanner;

/*
1. singleton(default*):
Scopes a single bean definition to a single object instance per Spring IoC container.

2. prototype:
Scopes a single bean definition to any number of object instances.

3. request:
Scopes a single bean definition to the lifecycle of a single HTTP request; that is each and every HTTP request will have its own instance of a bean created off the back of a single bean definition. Only valid in the context of a web-aware Spring ApplicationContext.

4. session:
Scopes a single bean definition to the lifecycle of a HTTP Session. Only valid in the context of a web-aware Spring ApplicationContext.

5. global session:
Scopes a single bean definition to the lifecycle of a global HTTP Session. Typically only valid when used in a portlet context. Only valid in the context of a web-aware Spring ApplicationContext.
 */
@Component
@Scope("prototype")
public class DeviceReceiver implements Runnable{
    @Autowired
    private DeviceRepository deviceRepository;
    private static final Logger logger = LoggerFactory.getLogger(DeviceReceiver.class);

    @Override
    public void run() {
        logger.info("Type device: (e.g. [IP:MAC:NAME])");
	    Device device = new Device(
	            "IP",
                "MAC",
                "NAME",
                Status.NEW,
                100,
                100,
                5,
                BUnit.mb,
                BUnit.mb,
                TUnit.m
        );
        deviceRepository.save(device);
        logger.info("Device added: " + device);
        Scanner scanner = new Scanner(System.in);
        while(scanner.hasNext()) {
            String data = scanner.next();
            try{
                String[] info = data.split(":");
                device = new Device(
                        info[0],
                        info[1],
                        info[2],
                        Status.NEW,
                        100,
                        100,
                        5,
                        BUnit.mb,
                        BUnit.mb,
                        TUnit.m
                        );
                deviceRepository.save(device);
                logger.info("Device added: " + device);
            } catch (Exception e) {
                logger.error("Scanner error: " + e.getMessage());
            }
        }
    }
}
