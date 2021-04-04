package spyke.engine.manage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spyke.database.model.Device;
import spyke.database.repository.DeviceRepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IptablesLog implements Runnable {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(IptablesLog.class);
    /**
     * A map with key as device and value as a concurrent map of key ip and value hostname of server that device is uploading data.
     */
    private final Map<Device, ConcurrentHashMap<String, String>> outgoingDomainByDevice = new ConcurrentHashMap<>();
    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * Gets the list of outgoing ip and corresponding host of given device.
     *
     * @param device The device.
     * @return The map with ip and corresponding host.
     */
    public Map<String, String> getList(final Device device) {
        return this.outgoingDomainByDevice.get(device);
    }

    @Override
    public void run() {

        final String dirname = "iptables_log";
        final String iptableLog = "iptables.log";
        final String iptableLogBackup = "iptables_backup.log";

        createDirectory(dirname);

        final File file = new File(System.getProperty("user.dir") +
                File.separator + dirname +
                File.separator + iptableLog
        );

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                logger.warn("Failed creating iptables.log: {}", e.getMessage());
            }
        }

        final File backup = new File(System.getProperty("user.dir") +
                File.separator + dirname +
                File.separator + iptableLogBackup
        );

        if (backup.exists() && !backup.delete()) {
            logger.warn("File already existed and was not able to delete.");
            return;
        }
        try {
            // copy is pretty faster, nevertheless it may lose some lines
            Files.copy(file.toPath(), backup.toPath());
            logger.warn("copied file successfully");

            // empty file
            file.delete();
            file.createNewFile();

            logger.warn("empty file successfully");
            final List<String> lines = Files.readAllLines(backup.toPath(), Charset.defaultCharset());
            for (final String line : lines) {
                writeToFile(line);
                storeOutgoingDomain(line);
            }
        } catch (final IOException e) {
            logger.error("Failed updating logs for iptables: {}", e.getMessage());
        }
    }

    private void createDirectory(final String dirname) {
        final String PATH = System.getProperty("user.dir");
        final String directoryName = PATH.concat(File.separator + dirname);
        final String logsPath = directoryName.concat(File.separator + "log");

        final File directory = new File(logsPath);
        if (!directory.exists()) {
            directory.mkdirs();
            logger.info("Directory created: {}", directory.getAbsolutePath());
        }
    }

    /**
     * Store log on file.
     *
     * @param line The line.
     */
    private void writeToFile(final String line) {
        final long ONE_MINUTE_IN_MILLIS = 60000;
        final Calendar cal = Calendar.getInstance();
        final Date now = new Date(cal.getTimeInMillis() - ONE_MINUTE_IN_MILLIS);
        cal.setTime(now);
        final int month = cal.get(Calendar.MONTH) + 1;
        final int day = cal.get(Calendar.DAY_OF_MONTH);
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        final String date = month + "-" + day + "-" + hour;
        final File file = new File(System.getProperty("user.dir") +
                File.separator + "iptables-log" +
                File.separator + "log" +
                File.separator + date + ".txt"
        );
        try (final PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
            writer.println(line);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Save the domain within {@code ConcurrentHashMap}, where key is ip and value hostname. Note a ip might have
     * several hostnames.
     *
     * @param line The line.
     */
    private void storeOutgoingDomain(final String line) {
        /*
            example:
            Apr  3 22:44:59 spyke kernel: [14815.689123] [spyke - log]IN=wlan0 OUT=eth0
            MAC=b8:27:eb:fe:d5:33:3c:bd:3e:a9:3d:c3:08:00 SRC=192.168.8.69 DST=183.84.5.204 LEN=104
            TOS=0x00 PREC=0x00 TTL=63 ID=58999 DF PROTO=TCP SPT=46928 DPT=80 WINDOW=29200 RES=0x00 ACK PSH URGP=0
         */
        final String dst_ip = line.split("DST=")[1].split(" ")[0].trim();
        final String src_ip = line.split("SRC=")[1].split(" ")[0].trim();
        // logger.warn("source: " + src_ip + " des: " +dst_ip);
        final Optional<Device> devices = this.deviceRepository.findByIp(src_ip);
        if (devices.isPresent()) {
            final Device device = devices.get();
            if (this.outgoingDomainByDevice.containsKey(device)) {
                if (this.outgoingDomainByDevice.get(device).size() != 0 && this.outgoingDomainByDevice.get(device).containsKey(dst_ip)) {
                    return;
                }
                try {
                    final InetAddress addr = InetAddress.getByName(dst_ip);
                    final String host = addr.getHostName();
                    this.outgoingDomainByDevice.get(device).put(dst_ip, host);
                    logger.warn("device: {} updated with dest ip: {}", device.getName(), dst_ip);
                } catch (final UnknownHostException e) {
                    e.printStackTrace();
                }
            } else {
                final ConcurrentHashMap<String, String> name_ip = new ConcurrentHashMap<String, String>();
                try {
                    final InetAddress addr = InetAddress.getByName(dst_ip);
                    final String host = addr.getHostName();
                    name_ip.put(dst_ip, host);
                    this.outgoingDomainByDevice.put(device, name_ip);
                    logger.warn("device: {} added with dest ip: {}", device.getName(), dst_ip);
                } catch (final UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
