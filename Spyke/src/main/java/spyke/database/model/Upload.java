package spyke.database.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

@Entity
public class Upload implements Serializable {
    @Id
    @Size(max = 20)
    private String ip;
    @Size(max = 20)
    private String mac;
    @Size(max = 20)
    private String destinationIp;
    private Date time;
    private long protocol;
    private long data;

    public Upload(){

    }

    public Upload(String ip, String mac, String destinationIp, Date time, long protocol, long data) {
        this.ip = ip;
        this.mac = mac;
        this.destinationIp = destinationIp;
        this.time = time;
        this.protocol = protocol;
        this.data = data;
    }

    public String getip() {
        return ip;
    }
    public String getmac() {
        return mac;
    }
    public String getDestinationIp() {
        return destinationIp;
    }
    public Date getTime() {
        return time;
    }
    public long getProtocol() {
        return protocol;
    }
    public long getData() {
        return data;
    }
    public void addData(long data) {
        this.data=+data;
    }
}
