package spyke.database.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

@Embeddable
public class PeriodId implements Serializable {
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_time")
    private Date startTime;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_time")
    private Date endTime;

    @ManyToOne
    private Device device;

    public PeriodId() {

    }

    public PeriodId(final Date startTime, final Date endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Date getStartTime() {
        return this.startTime;
    }

    public Date getEndTime() {
        return this.endTime;
    }

    public Device getDevice() {
        return this.device;
    }

    public void setDevice(final Device device) {
        this.device = device;
    }

    @Override
    public int hashCode() {
        if (this.startTime == null || this.endTime == null) {
            return -1;
        }
        return (this.startTime.toString() + this.endTime.toString() + this.device.getMac()).hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof PeriodId)) return false;
        final PeriodId periodId = (PeriodId) obj;
        return this.startTime != null ? this.startTime.equals(periodId.getStartTime()) : periodId.getStartTime() != null
                && this.endTime != null ? this.endTime.equals(periodId.getEndTime()) : periodId.getEndTime() != null
                && this.device != null ? this.device.equals(periodId.getDevice()) : periodId.getDevice() == null;
    }

    @Override
    public String toString() {
        return "PeriodId [start_time="
                + this.startTime
                + ", end_time="
                + this.endTime
                + ", device_mac="
                + this.device.getMac()
                + "]\n";
    }

}
