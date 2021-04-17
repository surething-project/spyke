package spyke.engine.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spyke.database.model.Device;
import spyke.database.model.types.Status;
import spyke.database.repository.DeviceRepository;
import spyke.engine.config.ScheduleConfig;
import spyke.engine.iptables.component.Iptables;
import spyke.engine.util.OperatingSystem;

import java.util.List;

@Service
public class DeviceManager implements Runnable {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(DeviceManager.class);

    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private ScheduleConfig scheduleConfig;
    @Autowired
    private Iptables iptables;

    @Override
    public void run() {

        if (this.iptables.restoreDefaultRules()) {
            logger.warn("Iptables default rules added successfully");
            final List<Device> devices = this.deviceRepository.findAll();
            for (final Device device : devices) {
                logger.debug("Device with ip {} is {}", device.getIp(), device.getStatus());
                if (device.getStatus() == Status.ALLOWED) {
                    logger.warn("Device with ip {} is allowed", device.getIp());
                    this.scheduleConfig.setScheduler(device);
                    if (OperatingSystem.isLinux()) {
                        this.iptables.addRules(device);
                        logger.warn("Iptables rules added successfully for {}", device.getIp());
                    }
                }
            }
        } else {
            logger.error("Adding Iptables default rules failed");
        }
    }
}
