package spyke.database.model;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
public class Period {

    @EmbeddedId
    private PeriodId id;
    @Column(name = "passed_bytes")
    private long passedBytes;
    @Column(name = "dropped_bytes")
    private long droppedBytes;

    public Period(){

    }

    public Period(PeriodId id, long passedBytes, long droppedBytes) {
        this.id = id;
        this.passedBytes = passedBytes;
        this.droppedBytes = droppedBytes;
    }

    public PeriodId getId() {
        return id;
    }
    public long getPassedBytes() {
        return passedBytes;
    }
    public void setPassedBytes(long passedBytes) {
        this.passedBytes = passedBytes;
    }
    public long getDroppedBytes() {
        return droppedBytes;
    }
    public void setDroppedBytes(long droppedBytes) {
        this.droppedBytes = droppedBytes;
    }

    @Override
    public String toString() {
        return "Period ["
                + "PeriodId="
                + id
                + ", passed_bytes="
                + passedBytes
                + ", dropped_bytes="
                + droppedBytes
                + "]\n";
    }

}
