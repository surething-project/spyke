package spyke.engine.pcap4j.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import spyke.database.model.Device;
import spyke.database.model.Upload;
import spyke.database.repository.DeviceRepository;
import spyke.engine.pcap4j.manage.PacketManager;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Component
@Scope("prototype")
public class PacketSender implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PacketSender.class);
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private TaskExecutor taskExecutor;
    @Autowired
    private PacketManager packetManager;
    @Autowired
    private DeviceRepository deviceRepository;
    @Override
    public void run() {
        logger.info("DEBUG: Schedule working");
        Date now = new Date();
        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(now);   // assigns calendar to given date
        String lastHour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY) - 1);
        List<Device> devices = deviceRepository.findAll();
        for (Device device:devices){
            List<Upload> u=packetManager.retrieveUploadList(device.getIp(), lastHour);
            if(u!=null) {
                StoreUpload storeUpload = applicationContext.getBean(StoreUpload.class);
                storeUpload.uploadList=u;
                taskExecutor.execute(storeUpload);
            }
        }
    }
}
