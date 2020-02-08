package spyke.pcap4j.task;

import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.namednumber.IpNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import spyke.database.model.Download;
import spyke.database.model.Upload;
import spyke.database.repository.DeviceRepository;
import spyke.pcap4j.manage.PacketManager;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Queue;
import java.util.GregorianCalendar;

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
public class PacketReceiver implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PacketReceiver.class);
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private PacketManager packetManager;
    private HashMap<String, Queue<String>> devices = new HashMap<String, Queue<String>>();
    public Packet packet;

    @Override
    public void run() {
        try {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            String srcAddr = ipV4Packet.getHeader().getSrcAddr().getHostAddress();
            String dstAddr = ipV4Packet.getHeader().getDstAddr().getHostAddress();
            // note: we are ignoring packets that have src ip and dst ip different from our devices.
            if(!srcAddr.equals(InetAddress.getLocalHost())||
                    !dstAddr.equals(InetAddress.getLocalHost())){
                //logger.info("Packet is not from/to spyke("+InetAddress.getLocalHost().getHostAddress()+")");
                IpNumber protocol = ipV4Packet.getHeader().getProtocol();
                int data = ipV4Packet.getHeader().getTotalLengthAsInt();

                //int srcPort=packet.get(TcpPacket.class).getHeader().getSrcPort().valueAsInt();
                //int dstPort=packet.get(TcpPacket.class).getHeader().getDstPort().valueAsInt();
                String srcMac=packet.get(EthernetPacket.class).getHeader().getSrcAddr().toString();
                String dstMac=packet.get(EthernetPacket.class).getHeader().getDstAddr().toString();

                Date now = new Date();
                Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
                calendar.setTime(now);   // assigns calendar to given date
                String hour= String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
                if (deviceRepository.findById(srcAddr).get() != null) {
                    Upload upload = new Upload(srcAddr, srcMac, dstAddr, now, 1, data);
                    packetManager.addUploadPacket(srcAddr, hour, upload);
                } else if (deviceRepository.findById(dstAddr).get() != null) {
                    Download download = new Download(srcAddr, dstMac, dstAddr, now, 1, data);
                    packetManager.addDownloadPacket(srcAddr, hour, download);
                } else {
                    logger.warn("Packet with src: ["+srcAddr+"] and dst: ["+dstAddr+"] are unknown devices");
                }
            } else {
                logger.warn("Packet is from/to spyke("+InetAddress.getLocalHost().getHostAddress()+")");
            }
        } catch (Exception e) {
            logger.error("Read packet error: "+e.getMessage());
        }
    }
}
