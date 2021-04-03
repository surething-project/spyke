package spyke.database.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

@Entity
public class Download implements Serializable {
    @Id
    @Size(max = 20)
    private String ip;
    @Size(max = 20)
    private String mac;
    @Size(max = 20)
    private String sourceIp;
    private Date time;
    private long protocol;
    private long data;

    public Download() {

    }

    public Download(final String ip, final String mac, final String sourceIp, final Date time, final long protocol, final long data) {
        this.ip = ip;
        this.mac = mac;
        this.sourceIp = sourceIp;
        this.time = time;
        this.protocol = protocol;
        this.data = data;
    }

    public String getip() {
        return this.ip;
    }

    public String getmac() {
        return this.mac;
    }

    public String getSourceIp() {
        return this.sourceIp;
    }

    public Date getTime() {
        return this.time;
    }

    public long getProtocol() {
        return this.protocol;
    }

    public long getData() {
        return this.data;
    }

}
