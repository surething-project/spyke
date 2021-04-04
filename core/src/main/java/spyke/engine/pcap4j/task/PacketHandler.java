package spyke.engine.pcap4j.task;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.NifSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import spyke.database.model.Device;

import java.io.IOException;

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
public class PacketHandler implements Runnable {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(PacketHandler.class);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private TaskExecutor taskExecutor;

    @Override
    public void run() {
        try {
            // TODO shouldn't be scanner
            //PcapNetworkInterface dev = getNetworkDevice();
            final PcapNetworkInterface dev = Pcaps.getDevByName("wlan0");
            logger.info("nif created: " + dev);
            if (dev == null) {
                logger.error("No interface chosen.");
                return;
            }
            final int snapLen = 65536;
            final PcapNetworkInterface.PromiscuousMode mode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS;
            final int timeout = 10;
            final PcapHandle handle = dev.openLive(snapLen, mode, timeout);
            final PacketListener listener = new PacketListener() {
                @Override
                public void gotPacket(final Packet packet) {
                    // Override the default gotPacket() function and process packet
                    //logger.info("Packet received: " + packet);
                    final IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
                    final PacketReceiver packetReceiver = PacketHandler.this.applicationContext.getBean(PacketReceiver.class);
                    packetReceiver.packet = packet;
                    PacketHandler.this.taskExecutor.execute(packetReceiver);
                }
            };
            // Tell the handle to loop using the listener we created
            try {
                final int maxPackets = -1;   // -1 = infinity
                handle.loop(maxPackets, listener);
            } catch (final InterruptedException e) {
                logger.error("Listening packet interrupted: " + e.getMessage());
            }
            logger.info("Listening packet exited...");
            handle.close();
        } catch (final PcapNativeException | NotOpenException | NullPointerException e) {
            logger.error("Listening packet exception: " + e.getMessage());
        }
    }

    /**
     * TODO this is outdated, since new approach is added
     *
     * @param device The device.
     * @param data   The data.
     */
    private synchronized void isOverLimited(final Device device, final int data) {
        /*
        if (device.getStatus().equals("ALLOWED") &&
                (device.getRule().getMedian() + device.getRule().getDeviation() <= pcap4j.getUsage(
                        device.getIp(),
                        data
                ))) {
            final Table tableIn = new Table();
            tableIn.setSource(device.getIp());
            tableIn.setFilter(Filter.ACCEPT);
            final Table tableOut = new Table();
            tableOut.setDestination(device.getIp());
            tableOut.setFilter(Filter.ACCEPT);
            iptables.deleteTable(tableIn);
            iptables.deleteTable(tableOut);
            database.changeDevice(
                    device.getIp(),
                    device.getMac(),
                    device.getName(),
                    "EXCEEDED",
                    device.getRule().getMedian(),
                    device.getRule().getDeviation()
            );
        }
         */
    }

    private void checkPayload(final IpV4Packet ipV4Packet) {
        // Only UDP & TCP
    }

    private PcapNetworkInterface getNetworkDevice() {
        PcapNetworkInterface device = null;
        try {
            device = new NifSelector().selectNetworkInterface();
        } catch (final IOException e) {
            logger.error("Interface exception: " + e.getMessage());
        }
        return device;
    }
}
