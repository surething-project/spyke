
package spyke.database;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import spyke.database.model.Device;
import spyke.database.model.Period;
import spyke.database.model.PeriodId;
import spyke.database.repository.DeviceRepository;
import spyke.database.repository.PeriodRepository;
import spyke.database.variable.BUnit;
import spyke.database.variable.Status;
import spyke.database.variable.TUnit;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class DatabaseTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @Before
    public void setup() {
        assertNotEquals(deviceRepository, null);
        assertNotEquals(periodRepository, null);
        Date now = new Date();
        Calendar calendar = GregorianCalendar.getInstance();    // creates a new calendar instance
        calendar.setTime(now);  // assigns calendar to given date
        calendar.add(Calendar.HOUR_OF_DAY, 1);  //adds one hour
        Date after = calendar.getTime();

        long diff = after.getTime() - now.getTime();
        long diffHours = diff / (60 * 60 * 1000);
        // test diff time
        assertEquals("Date difference are"+diffHours, diffHours, 1L);

        Device device = new Device("192.168.8.24",
                "f0:18:98:05:64:90",
                "Shengs-MBP",
                Status.NEW,
                0,
                0,
                0,
                BUnit.kb,
                BUnit.kb,
                TUnit.m);
        deviceRepository.save(device);
        PeriodId periodId = new PeriodId(now, after);
        periodId.setDevice(device);
        Period period = new Period(periodId, 0, 0);
        periodRepository.save(period);
    }

    @After
    public void exit(){
        deviceRepository.flush();
        periodRepository.flush();
    }

    @Test
    public void databaseTest() throws IllegalStateException {
        // test repositories not null
        assertNotEquals(null, deviceRepository);
        assertNotEquals(null, periodRepository);

        // test exists
        List<Device> devices=deviceRepository.findAll();
        assertEquals(1, devices.size());
        List<Period> periods=periodRepository.findAll();
        assertEquals(1, periods.size());

    }

    @Test
    public void findAllByMacTest() {
        // test foreign key
        List<Period> periodsByDevice=periodRepository.findAllByMac("f0:18:98:05:64:90");
        if(periodsByDevice.size()==1){
            assertEquals("PeriodId mac is correct",periodsByDevice.get(0).getId().getDevice().getMac(),"f0:18:98:05:64:90");
        }
    }

}
