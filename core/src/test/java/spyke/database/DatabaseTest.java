package spyke.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import spyke.database.model.Device;
import spyke.database.model.Period;
import spyke.database.model.PeriodId;
import spyke.database.model.types.BUnit;
import spyke.database.model.types.Status;
import spyke.database.model.types.TUnit;
import spyke.database.repository.DeviceRepository;
import spyke.database.repository.PeriodRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DatabaseTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @BeforeEach
    public void setup() {

        assertThat(this.deviceRepository)
                .as("The device repository should not be null")
                .isNotNull();
        assertThat(this.periodRepository)
                .as("The period repository should not be null")
                .isNotNull();

        final Date now = new Date();
        final Calendar calendar = GregorianCalendar.getInstance();    // creates a new calendar instance
        calendar.setTime(now);  // assigns calendar to given date
        calendar.add(Calendar.HOUR_OF_DAY, 1);  //adds one hour
        final Date after = calendar.getTime();

        final long diff = after.getTime() - now.getTime();
        final long diffHours = diff / (60 * 60 * 1000);
        assertThat(diffHours)
                .as("Date difference should be 1")
                .isEqualTo(1L);

        final Device device = new Device(
                "192.168.8.24",
                "f0:18:98:05:64:90",
                "Shengs-MBP",
                Status.NEW,
                0,
                0,
                0,
                BUnit.kb,
                BUnit.kb,
                TUnit.m
        );
        this.deviceRepository.save(device);
        final PeriodId periodId = new PeriodId(now, after);
        periodId.setDevice(device);
        final Period period = new Period(periodId, 0, 0);
        this.periodRepository.save(period);
    }

    @AfterEach
    public void exit() {
        this.deviceRepository.flush();
        this.periodRepository.flush();
    }

    @Test
    public void databaseTest() throws IllegalStateException {

        assertThat(this.deviceRepository)
                .as("The device repository should not be null")
                .isNotNull();
        assertThat(this.periodRepository)
                .as("The period repository should not be null")
                .isNotNull();

        final List<Device> devices = this.deviceRepository.findAll();
        assertThat(devices.size())
                .as("The list of device should contain only one")
                .isEqualTo(1);
        final List<Period> periods = this.periodRepository.findAll();
        assertThat(periods.size())
                .as("The list of period should contain only one")
                .isEqualTo(1);

    }

    @Test
    public void findAllByMacTest() {

        final String deviceMac = "f0:18:98:05:64:90";
        final List<Period> periodsByDevice = this.periodRepository.findAllByMac(deviceMac);
        assertThat(periodsByDevice.size())
                .as("There should be one period per device")
                .isEqualTo(1);
        assertThat(periodsByDevice.get(0).getId().getDevice().getMac())
                .as("PeriodId mac is correct")
                .isEqualTo(deviceMac);
    }

}
