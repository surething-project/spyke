package spyke.monitor.manage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spyke.database.model.Device;
import spyke.database.repository.DeviceRepository;
import spyke.database.variable.Status;
import spyke.iptables.component.Iptables;
import spyke.iptables.util.OperatingSystem;
import spyke.monitor.config.ScheduleConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class DeviceManager implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DeviceManager.class);
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private ScheduleConfig scheduleConfig;
    @Autowired
    private Iptables iptables;
    @Override
    public void run() {
        // restore iptables rules
        if (iptables.restoreRules()) {
            // FIXME log
        }
        // add iptables
        List<Device> devices = deviceRepository.findAll();
        for (Device device : devices) {
            if (device.getStatus() == Status.ALLOWED) {
                // add period
                scheduleConfig.setScheduler(device);
                // add rule if OS is linux
                if (OperatingSystem.isLinux())
                    iptables.addRule(device);
            }
        }
    }
}
