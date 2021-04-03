package spyke.monitor.pcap4j.manage;

import java.io.Serializable;

public class PacketId implements Serializable {
    private String ip;
    private String hour;
    public PacketId(String ip, String hour){
        this.ip=ip;
        this.hour=hour;
    }
    public String getIp(){return ip;}
    public String getHour(){return hour;}
    @Override
    public boolean equals(Object obj){
        if (this == obj){
            return true;
        }
        PacketId packetId = (PacketId) obj;
        if (ip != null ?
                !ip.equals(packetId.getIp())
                :packetId.getIp() != null
                && hour != null ?
                !ip.equals(packetId.getHour())
                :packetId.getIp() != null){
            return false;
        }
        else {
            return true;
        }
    }
}
