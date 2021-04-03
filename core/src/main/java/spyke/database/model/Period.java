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

    public Period() {

    }

    public Period(final PeriodId id, final long passedBytes, final long droppedBytes) {
        this.id = id;
        this.passedBytes = passedBytes;
        this.droppedBytes = droppedBytes;
    }

    public PeriodId getId() {
        return this.id;
    }

    public long getPassedBytes() {
        return this.passedBytes;
    }

    public void setPassedBytes(final long passedBytes) {
        this.passedBytes = passedBytes;
    }

    public long getDroppedBytes() {
        return this.droppedBytes;
    }

    public void setDroppedBytes(final long droppedBytes) {
        this.droppedBytes = droppedBytes;
    }

    @Override
    public String toString() {
        return "Period ["
                + "PeriodId="
                + this.id
                + ", passed_bytes="
                + this.passedBytes
                + ", dropped_bytes="
                + this.droppedBytes
                + "]\n";
    }

}
