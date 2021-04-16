package spyke.database.model;

import com.google.common.base.Objects;
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

    public Device() {

    }

    public Device(
            final String ip,
            final String mac,
            final String name,
            final Status status,
            final long quota,
            final long bandwidth,
            final long period,
            final BUnit quotaBUnit,
            final BUnit bandwidthBUnit,
            final TUnit periodUnit
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
        return this.ip;
    }

    public String getMac() {
        return this.mac;
    }

    public String getName() {
        return this.name;
    }

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public long getQuota() {
        return this.quota;
    }

    public void setQuota(final long quota) {
        this.quota = quota;
    }

    public long getBandwidth() {
        return this.bandwidth;
    }

    public void setBandwidth(final long bandwidth) {
        this.bandwidth = bandwidth;
    }

    public BUnit getQuotaBUnit() {
        return this.quotaBUnit;
    }

    public void setQuotaBUnit(final BUnit quotaBUnit) {
        this.quotaBUnit = quotaBUnit;
    }

    public BUnit getBandwidthBUnit() {
        return this.bandwidthBUnit;
    }

    public void setBandwidthBUnit(final BUnit bandwidthBUnit) {
        this.bandwidthBUnit = bandwidthBUnit;
    }

    public long getPeriod() {
        return this.period;
    }

    public void setPeriod(final long period) {
        this.period = period;
    }

    public TUnit getPeriodUnit() {
        return this.periodUnit;
    }

    public void setPeriodUnit(final TUnit periodUnit) {
        this.periodUnit = periodUnit;
    }

    @Override
    public int hashCode() {
        if (this.ip == null || this.mac == null) {
            return -1;
        }
        return Objects.hashCode(this.ip, this.mac);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        final Device device = (Device) other;
        return Objects.equal(this.mac, device.getMac())
                && Objects.equal(this.ip, device.getIp());
    }

    @Override
    public String toString() {
        return "Device [ip="
                + this.ip
                + ", mac="
                + this.mac
                + ", name="
                + this.name
                + ", status="
                + this.status
                + ", quota="
                + this.quota
                + " "
                + this.quotaBUnit
                + ", bandwidth="
                + this.bandwidth
                + " "
                + this.bandwidthBUnit
                + "]\n";
    }
}
