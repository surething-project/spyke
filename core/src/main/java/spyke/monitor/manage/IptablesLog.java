package spyke.monitor.manage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spyke.database.model.Device;
import spyke.database.repository.DeviceRepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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

    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * A map with key as device and value as a concurrent map of key ip and value host.
     * FIXME the concurrent hashmap can be a tuple
     */
    private final Map<Device, ConcurrentHashMap<String, String>> outgoingIpDevice = new ConcurrentHashMap<>();

    /**
     * Gets the list of outgoing ip and corresponding host of given device.
     *
     * @param device The device.
     * @return The map with ip and corresponding host.
     */
    public Map<String, String> getList(final Device device) {
        return this.outgoingIpDevice.get(device);
    }

    @Override
    public void run() {
        createFile("iptables-log", "iptables.log");

        final File file = new File(System.getProperty("user.dir") +
                File.separator + "iptables-log" +
                File.separator + "iptables.log"
        );
        final File backup = new File(System.getProperty("user.dir") +
                File.separator + "iptables-log" +
                File.separator + "iptables_backup.log"
        );
        if (backup.exists()
                && !backup.delete()
        ) {
            logger.warn("File already existed and was not able to delete.");
            return;
        }
        try {
            Files.copy(file.toPath(), backup.toPath());
            // remove file content, copy is pretty faster, nevertheless it may lose some lines
            final PrintWriter writer = new PrintWriter(file);
            writer.print("");
            writer.close();

            final List<String> list = Files.readAllLines(backup.toPath(), Charset.defaultCharset());

            for (final String line : list) {
                // store to log
                write2File(line);
                // store the outgoing ip address
                storeOutgoingIp(line);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void createFile(final String dirname, final String filename) {
        final String PATH = System.getProperty("user.dir");
        final String directoryName = PATH.concat(File.separator + dirname);
        final String logsPath = directoryName.concat(File.separator + "log");

        final File directory = new File(logsPath);
        if (!directory.exists()) {
            logger.info("Directory created:" + directory.getAbsolutePath());
            directory.mkdirs();
        }
        final File file = new File(directoryName + File.separator + filename);
        try {
            file.createNewFile();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        /*
        try {
            logger.warn("file created: "+file.createNewFile());
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        */
    }

    private void write2File(final String line) {
        // 1 minute
        final long ONE_MINUTE_IN_MILLIS = 60000;
        final Calendar cal = Calendar.getInstance();
        final Date now = new Date(cal.getTimeInMillis() - ONE_MINUTE_IN_MILLIS);
        cal.setTime(now);
        final int month = cal.get(Calendar.MONTH) + 1;
        final int day = cal.get(Calendar.DAY_OF_MONTH);
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        final String date = month + "-" + day + "-" + hour;
        try {
            final File file = new File(System.getProperty("user.dir") +
                    File.separator + "iptables-log" +
                    File.separator + "log" +
                    File.separator + date + ".txt"
            );
            final PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            writer.println(line);
            writer.close();
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }


    private void storeOutgoingIp(final String line) {
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
            if (this.outgoingIpDevice.containsKey(device)) {
                if (this.outgoingIpDevice.get(device).size() != 0 && this.outgoingIpDevice.get(device).containsKey(dst_ip))
                    return;
                try {
                    final InetAddress addr = InetAddress.getByName(dst_ip);
                    final String host = addr.getHostName();
                    this.outgoingIpDevice.get(device).put(dst_ip, host);
                    // logger.warn("device: "+device.getName() + " updated and dest ip: " +dst_ip);
                } catch (final UnknownHostException e) {
                    e.printStackTrace();
                }
            } else {
                final ConcurrentHashMap<String, String> name_ip = new ConcurrentHashMap<String, String>();
                try {
                    final InetAddress addr = InetAddress.getByName(dst_ip);
                    final String host = addr.getHostName();
                    name_ip.put(dst_ip, host);
                    this.outgoingIpDevice.put(device, name_ip);
                    // logger.warn("device: "+device.getName() + " added and dest ip: " +dst_ip);
                } catch (final UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
