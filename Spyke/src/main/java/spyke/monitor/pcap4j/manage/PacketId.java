package spyke.monitor.pcap4j.manage;

import java.io.Serializable;

public class PacketId implements Serializable {
    private final String ip;
    private final String hour;

    public PacketId(final String ip, final String hour) {
        this.ip = ip;
        this.hour = hour;
    }

    public String getIp() {
        return this.ip;
    }

    public String getHour() {
        return this.hour;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        final PacketId packetId = (PacketId) obj;
        return this.ip != null ? this.ip.equals(packetId.getIp()) : packetId.getIp() != null
                && this.hour != null ? this.ip.equals(packetId.getHour()) : packetId.getIp() == null;
    }
}
