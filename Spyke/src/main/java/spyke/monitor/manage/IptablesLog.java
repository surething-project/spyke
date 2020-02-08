package spyke.monitor.manage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spyke.database.model.Device;
import spyke.database.repository.DeviceRepository;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IptablesLog implements Runnable {

    @Autowired
    private DeviceRepository deviceRepository;

    private static final Logger logger = LoggerFactory.getLogger(IptablesLog.class);

    // ConcurrentHashMap<String, String> => IP address and Host
    private Map<Device, ConcurrentHashMap<String, String>> outgoingIpDevice = new ConcurrentHashMap<>();

    public Map<String, String> getList(Device device){
        return outgoingIpDevice.get(device);
    }



    private void createFile(String dirname, String filename){
        String PATH = System.getProperty("user.dir");
        String directoryName = PATH.concat(File.separator+dirname);
        String logsPath = directoryName.concat(File.separator+"log");

        File directory = new File(logsPath);
        if (! directory.exists()){
            logger.info("Directory created:" + directory.getAbsolutePath());
            directory.mkdirs();
        }
        File file = new File(directoryName + File.separator + filename);
        try {
            file.createNewFile();
        } catch (IOException e) {
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

    @Override
    public void run() {
        createFile("iptables-log", "iptables.log");

        File file = new File(System.getProperty("user.dir") +
                File.separator + "iptables-log" +
                File.separator + "iptables.log"
        );
        File backup = new File(System.getProperty("user.dir") +
                File.separator + "iptables-log" +
                File.separator + "iptables_backup.log"
        );
        if(backup.exists()
                && !backup.delete()
        ){
            logger.warn("File already existed and was not able to delete.");
            return;
        }
        try {
            Files.copy(file.toPath(), backup.toPath());
            // remove file content, copy is pretty faster, nevertheless it may lose some lines
            PrintWriter writer = new PrintWriter(file);
            writer.print("");
            writer.close();

            List<String> list = Files.readAllLines(backup.toPath(), Charset.defaultCharset());

            for(String line : list) {
                // store to log
                write2File(line);
                // store the outgoing ip address
                storeOutgoingIp(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void write2File(String line){
        // 1 minute
        long ONE_MINUTE_IN_MILLIS = 60000;
        Calendar cal = Calendar.getInstance();
        Date now = new Date(cal.getTimeInMillis() - ONE_MINUTE_IN_MILLIS);
        cal.setTime(now);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String date = month + "-" + day + "-" + hour;
        try {
            File file = new File(System.getProperty("user.dir") +
                    File.separator + "iptables-log" +
                    File.separator + "log" +
                    File.separator + date + ".txt"
            );
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            writer.println(line);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void storeOutgoingIp(String line){
        /*
            example:
            Apr  3 22:44:59 spyke kernel: [14815.689123] [spyke - log]IN=wlan0 OUT=eth0
            MAC=b8:27:eb:fe:d5:33:3c:bd:3e:a9:3d:c3:08:00 SRC=192.168.8.69 DST=183.84.5.204 LEN=104
            TOS=0x00 PREC=0x00 TTL=63 ID=58999 DF PROTO=TCP SPT=46928 DPT=80 WINDOW=29200 RES=0x00 ACK PSH URGP=0
         */
        String dst_ip = line.split("DST=")[1].split(" ")[0].trim();
        String src_ip = line.split("SRC=")[1].split(" ")[0].trim();
        // logger.warn("source: " + src_ip + " des: " +dst_ip);
        List<Device> devices = deviceRepository.findByIp(src_ip);
        if (devices.size() != 0) {
            Device device = devices.get(0);
            if (outgoingIpDevice.containsKey(device)) {
                if (outgoingIpDevice.get(device).size() != 0 && outgoingIpDevice.get(device).containsKey(dst_ip))
                    return;
                try {
                    InetAddress addr = InetAddress.getByName(dst_ip);
                    String host = addr.getHostName();
                    outgoingIpDevice.get(device).put(dst_ip, host);
                    // logger.warn("device: "+device.getName() + " updated and dest ip: " +dst_ip);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            } else {
                ConcurrentHashMap<String, String> name_ip = new ConcurrentHashMap<String, String>();
                try {
                    InetAddress addr = InetAddress.getByName(dst_ip);
                    String host = addr.getHostName();
                    name_ip.put(dst_ip, host);
                    outgoingIpDevice.put(device, name_ip);
                    // logger.warn("device: "+device.getName() + " added and dest ip: " +dst_ip);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
