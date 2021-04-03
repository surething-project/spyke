package spyke.database.model;

import spyke.database.model.types.BUnit;
import spyke.database.model.types.Status;
import spyke.database.model.types.TUnit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "device")
public class Device implements Serializable {

    @Size(max = 20)
    private String ip;
    @Id
    @NotNull
    @Size(max = 20)
    private String mac;
    @NotNull
    private String name;
    @Enumerated(EnumType.STRING)
    private Status status;
    private long quota;
    @Enumerated(EnumType.STRING)
    @Column(name = "quota_unit")
    private BUnit quotaBUnit;
    private long bandwidth;
    @Enumerated(EnumType.STRING)
    @Column(name = "bandwidth_unit")
    private BUnit bandwidthBUnit;
    private long period;
    @Enumerated(EnumType.STRING)
    @Column(name = "period_unit")
    private TUnit periodUnit;

    public Device(){

    }

    public Device(
            String ip,
            String mac,
            String name,
            Status status,
            long quota,
            long bandwidth,
            long period,
            BUnit quotaBUnit,
            BUnit bandwidthBUnit,
            TUnit periodUnit
    ) {
        this.ip = ip;
        this.mac = mac;
        this.name = name;
        this.status = status;
        this.quota = quota;
        this.bandwidth = bandwidth;
        this.period = period;
        this.quotaBUnit = quotaBUnit;
        this.bandwidthBUnit = bandwidthBUnit;
        this.periodUnit = periodUnit;
    }

    public String getIp() {
        return ip;
    }
    public String getMac() {
        return mac;
    }
    public String getName() {
        return name;
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public long getQuota() {
        return quota;
    }
    public void setQuota(long quota) {
        this.quota = quota;
    }
    public long getBandwidth() {
        return bandwidth;
    }
    public void setBandwidth(long bandwidth) {
        this.bandwidth = bandwidth;
    }
    public BUnit getQuotaBUnit() {
        return quotaBUnit;
    }
    public void setQuotaBUnit(BUnit quotaBUnit) {
        this.quotaBUnit = quotaBUnit;
    }
    public BUnit getBandwidthBUnit() {
        return bandwidthBUnit;
    }
    public void setBandwidthBUnit(BUnit bandwidthBUnit) {
        this.bandwidthBUnit = bandwidthBUnit;
    }

    public long getPeriod(){
        return period;
    }
    public void setPeriod(long period){
        this.period=period;
    }
    public TUnit getPeriodUnit() {
        return periodUnit;
    }
    public void setPeriodUnit(TUnit periodUnit) {
        this.periodUnit = periodUnit;
    }

    @Override
    public boolean equals(Object obj){
        if (this == obj){
            return true;
        }
        Device device = (Device) obj;
        if (mac != null ?
                !mac.equals(device.getMac())
                :device.getMac() != null
            && ip != null ?
                !ip.equals(device.getIp())
                :device.getIp() != null){
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    public int hashCode(){
        if(this.ip == null || this.mac == null){
            return -1;
        }
        return (this.ip+this.mac).hashCode();
    }

    @Override
    public String toString() {
        return "Device [ip="
                + ip
                + ", mac="
                + mac
                + ", name="
                + name
                + ", status="
                + status
                + ", quota="
                + quota
                + " "
                + quotaBUnit
                + ", bandwidth="
                + bandwidth
                + " "
                + bandwidthBUnit
                + "]\n";
    }
}
