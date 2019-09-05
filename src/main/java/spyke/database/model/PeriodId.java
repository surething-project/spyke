package spyke.database.model;

import javax.persistence.*;
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

    public PeriodId(Date startTime, Date endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PeriodId)) return false;
        PeriodId periodId = (PeriodId) obj;
        if (startTime != null ?
                !startTime.equals(periodId.getStartTime())
                :periodId.getStartTime() != null
                && endTime != null ?
                !endTime.equals(periodId.getEndTime())
                :periodId.getEndTime() != null
                && device != null ?
                !device.equals(periodId.getDevice())
                :periodId.getDevice() != null
        ){
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    public int hashCode() {
        if(this.startTime == null || this.endTime == null){
            return -1;
        }
        return (this.startTime.toString()+this.endTime.toString()+device.getMac()).hashCode();
    }

    @Override
    public String toString() {
        return "PeriodId [start_time="
                + startTime
                + ", end_time="
                + endTime
                + ", device_mac="
                + device.getMac()
                + "]\n";
    }

}
