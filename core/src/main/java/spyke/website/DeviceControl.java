package spyke.website;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spyke.database.model.Device;
import spyke.database.model.Period;
import spyke.database.model.PeriodId;
import spyke.database.model.types.BUnit;
import spyke.database.model.types.Status;
import spyke.database.model.types.TUnit;
import spyke.database.repository.DeviceRepository;
import spyke.database.repository.PeriodRepository;
import spyke.engine.config.ScheduleConfig;
import spyke.engine.iptables.component.Iptables;
import spyke.engine.manage.IptablesLog;
import spyke.engine.util.OperatingSystem;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;

@RestController
@RequestMapping("/device")
public class DeviceControl {

    private static final Logger logger = LoggerFactory.getLogger(DeviceControl.class);
    @Autowired
    private Iptables iptables;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private PeriodRepository periodRepository;
    @Autowired
    private ScheduleConfig scheduleConfig;
    @Autowired
    private IptablesLog iptablesLog;

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                     G E T                                           *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @RequestMapping(path = "/device", method = POST)
    public ResponseEntity<Device> getDeviceById(@RequestParam("mac") final String mac) {
        final Device device = this.deviceRepository.findById(mac).get();
        return new ResponseEntity<Device>(device, HttpStatus.OK);
    }

    @RequestMapping(path = "/devices", method = POST)
    public ResponseEntity<List<Device>> getDevices() {
        final List<Device> devices = this.deviceRepository.findAll();
        return new ResponseEntity<List<Device>>(devices, HttpStatus.OK);
    }

    @RequestMapping(path = "/control/period/{mac}/limited", method = GET)
    public ResponseEntity<List<Period>> getPeriodsByMacWithLimit(@PathVariable("mac") final String mac) {
        List<Period> periods = new ArrayList<Period>();
        if (this.deviceRepository.findById(mac).isPresent()) {
            final Device device = this.deviceRepository.findById(mac).get();
            long time = device.getPeriod() * 1000 * 24; // 24 is the how many; Perhaps it will return last 23 periods
            if (time == 0) {
                time = 24 * 60 * 60 * 1000; // 1 day by default
            } else {
                switch (device.getPeriodUnit()) {
                    case m:
                        time *= 60;
                        break;
                    case h:
                        time *= 60 * 60;
                        break;
                    case d:
                        time *= 24 * 60 * 60;
                        break;
                    default:
                        time = 24 * 60 * 60 * 1000; // 1 day by default
                }
            }
            periods = this.periodRepository.findAllByTimeStartAfter(mac, new Date(System.currentTimeMillis() - time));

            final long[] currentBytes = this.iptables.extractBytes(device);    // [0] = sent & [1] = dropped
            // currentBytes[0] != null && currentBytes[1] != null, by default a long is 0
            final PeriodId currentPeriodId;
            if (periods.size() != 0)
                currentPeriodId = new PeriodId(
                        periods.get(periods.size() - 1).getId().getEndTime(),
                        new Date()
                );  // start_time & end_time
            else {
                final Date end_time_plus_one_minute = new Date(System.currentTimeMillis() - (time / 24) + 60000); // 60 * 1000
                currentPeriodId = new PeriodId(end_time_plus_one_minute, new Date());
            }
            final Period currentPeriod = new Period(
                    currentPeriodId,
                    currentBytes[0],
                    currentBytes[1]
            );  // PeriodId, passed, dropped
            periods.add(currentPeriod);
        }
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    @RequestMapping(path = "/control/device/period/{mac}", method = GET)
    public ResponseEntity<String> getDevicePeriod(@PathVariable("mac") final String mac) {
        String period = "not found";
        if (this.deviceRepository.findById(mac).isPresent()) {
            final Device device = this.deviceRepository.findById(mac).get();
            period = device.getPeriod() + String.valueOf(device.getPeriodUnit());
            return new ResponseEntity<String>(period, HttpStatus.OK);
        }
        return new ResponseEntity<String>(period, HttpStatus.NOT_FOUND);
    }

    // get list of blocked ip or mac
    @RequestMapping(path = "/control/ip/blacklist", method = GET)
    public ResponseEntity<List<String>> getIpBlackList() {
        return new ResponseEntity<List<String>>(this.iptables.getBlacklist(), HttpStatus.OK);
    }

    // get list of outgoing ip
    @RequestMapping(path = "/control/ip/list/{mac}", method = GET)
    public ResponseEntity<Map<String, String>> getIpList(@PathVariable("mac") final String mac) {
        if (this.deviceRepository.findById(mac).isPresent()) {
            final Device device = this.deviceRepository.findById(mac).get();
            return new ResponseEntity<Map<String, String>>(this.iptablesLog.getList(device), HttpStatus.OK);
        }
        return new ResponseEntity<Map<String, String>>(new HashMap<String, String>(), HttpStatus.OK);
    }

    // test: get all periods
    @RequestMapping(path = "/control/period/list", method = GET)
    public ResponseEntity<List<Period>> getPeriods() {
        final List<Period> periods = this.periodRepository.findAll();
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    // test: get device's all periods
    @RequestMapping(path = "/control/period/{mac}/list", method = GET)
    public ResponseEntity<List<Period>> getPeriodsByMac(@PathVariable("mac") final String mac) {
        final List<Period> periods = this.periodRepository.findAllByMac(mac);
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    // test: get device's periods from 'time' hours ago before  note: not working with param 0 since period is not saved yet
    @RequestMapping(path = "/control/period/{mac}/before/{time}", method = GET)
    public ResponseEntity<List<Period>> getPeriodByMacAndStartBefore(@PathVariable("mac") final String mac,
                                                                     @PathVariable("time") final String paramtime) {
        final List<Period> periods = this.periodRepository.findAllByTimeStartBefore(mac, getDateFromGivenHourAge(paramtime));
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    // test: get device's periods from 'time' hours ago after   note: not working with param 0 since period is not saved yet
    @RequestMapping(path = "/control/period/{mac}/after/{time}", method = GET)
    public ResponseEntity<List<Period>> getPeriodByMacAndStartAfter(@PathVariable("mac") final String mac,
                                                                    @PathVariable("time") final String paramtime) {
        final List<Period> periods = this.periodRepository.findAllByTimeStartAfter(mac, getDateFromGivenHourAge(paramtime));
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    // test: get device's period from 'time' hours ago  note: not working with param 0 since period is not saved yet
    @RequestMapping(path = "/control/period/{mac}/time/{time}", method = GET)
    public ResponseEntity<List<Period>> getPeriodByMacAndTime(@PathVariable("mac") final String mac,
                                                              @PathVariable("time") final String paramtime) {
        final List<Period> periods = this.periodRepository.findAllByTime(mac, getDateFromGivenHourAge(paramtime));
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    // test: is device's schedule cancelled?
    @RequestMapping(path = "/control/schedule/{mac}/cancelled", method = GET)
    public ResponseEntity<Boolean> getSchedulerIsCancelled(@PathVariable("mac") final String mac) {
        Device device = null;
        if (this.deviceRepository.findById(mac).isPresent()) {
            device = this.deviceRepository.findById(mac).get();
        }
        return new ResponseEntity<Boolean>(this.scheduleConfig.isCancelled(device), HttpStatus.OK);
    }

    // test: does device's schedule exist?
    @RequestMapping(path = "/control/schedule/{mac}/exists", method = GET)
    public ResponseEntity<Boolean> getSchedulerIsCancel(@PathVariable("mac") final String mac) {
        Device device = null;
        if (this.deviceRepository.findById(mac).isPresent()) {
            device = this.deviceRepository.findById(mac).get();
        }
        return new ResponseEntity<Boolean>(this.scheduleConfig.exists(device), HttpStatus.OK);
    }

    // test: get device passed and dropped values
    @RequestMapping(path = "/control/schedule/{mac}/bytes", method = GET)
    public ResponseEntity<List<Long>> getBytesTest(@PathVariable("mac") final String mac) {
        Device device = null;
        if (this.deviceRepository.findById(mac).isPresent()) {
            device = this.deviceRepository.findById(mac).get();
        }
        final List<Long> bytes = new ArrayList<Long>();
        for (final long b : this.iptables.extractBytes(device)) {
            bytes.add(b);
        }
        return new ResponseEntity<List<Long>>(bytes, HttpStatus.OK);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                    P O S T                                          *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // add device
    @RequestMapping(path = "/control/device/add", method = POST)
    public ResponseEntity<Boolean> addDevice(@RequestParam("ip") final String ip,
                                             @RequestParam("mac") final String mac,
                                             @RequestParam("name") final String name) {
        final Device device = new Device(ip, mac, name, Status.NEW, 0, 0, 0, BUnit.kb, BUnit.kb, TUnit.m);
        this.deviceRepository.saveAndFlush(device);
        return new ResponseEntity<Boolean>(true, HttpStatus.OK);
    }

    // block ip or mac
    @RequestMapping(path = "/control/ip/block", method = POST)
    public ResponseEntity<Boolean> getBlock(@RequestParam("block") final String block) {
        // TODO input validation
        return new ResponseEntity<Boolean>(this.iptables.block(block), HttpStatus.OK);
    }

    // unblock ip or mac
    @RequestMapping(path = "/control/ip/unblock", method = POST)
    public ResponseEntity<Boolean> getUnblock(@RequestParam("unblockip") final String unblockip) {
        // TODO input validation
        return new ResponseEntity<Boolean>(this.iptables.unblock(unblockip), HttpStatus.OK);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                     P U T                                           *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @RequestMapping(path = "/control/status", method = POST)
    public ResponseEntity<Boolean> changeStatus(@RequestParam("ip") final String ip,
                                                @RequestParam("mac") final String mac,
                                                @RequestParam("name") final String name,
                                                @RequestParam("quota") final String quota,
                                                @RequestParam("bandwidth") final String bandwidth,
                                                @RequestParam("quotaUnit") final String quotaUnit,
                                                @RequestParam("bandwidthUnit") final String bandwidthUnit,
                                                @RequestParam("period") final String period,
                                                @RequestParam("periodUnit") final String periodUnit,
                                                @RequestParam("status") final String status) {
        boolean changed = false;
        if (quota.length() > 10 || bandwidth.length() > 10 || period.length() > 10) {
            return new ResponseEntity<Boolean>(false, HttpStatus.OK);
        }
        if (this.deviceRepository.findById(mac).isPresent()) {
            final Device device = this.deviceRepository.findById(mac).get();
            if (status.equals("ALLOWED")) {
                if (OperatingSystem.isLinux()) {
                    device.setStatus(Status.valueOf(status));
                    allowDevice(device, quota, quotaUnit, bandwidth, bandwidthUnit, period, periodUnit);
                    return new ResponseEntity<Boolean>(true, HttpStatus.OK);
                } else {
                    logger.error("Invalid OS...");
                }
            } else if (status.equals("BLOCKED")) {
                if (device.getStatus() == Status.ALLOWED) {
                    if (OperatingSystem.isLinux()) {
                        this.scheduleConfig.cancelScheduler(device);
                        this.iptables.deleteRules(device);
                        device.setStatus(Status.valueOf(status));
                        this.deviceRepository.saveAndFlush(device);
                        logger.warn("{} -> blocked", device);
                        return new ResponseEntity<Boolean>(true, HttpStatus.OK);
                    } else {
                        logger.error("Invalid OS...");
                    }
                }
            }
            logger.error("Invalid status...");
        } else {
            logger.error("MAC ({}) not found", mac);
        }
        return new ResponseEntity<Boolean>(false, HttpStatus.OK);
    }

    // test: renew device's rules
    @RequestMapping(path = "/control/rule/renew/{mac}", method = GET)
    public ResponseEntity<Boolean> renewRule(@PathVariable("mac") final String mac) {
        if (this.deviceRepository.findById(mac).isPresent()) {
            final Device device = this.deviceRepository.findById(mac).get();
            this.iptables.renewRules(device);
        }
        return new ResponseEntity<Boolean>(true, HttpStatus.OK);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                   D E L E T E                                       *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                   O T H E R S                                       *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private void allowDevice(final Device device,
                             final String quota,
                             final String quotaUnit,
                             final String bandwidth,
                             final String bandwidthUnit,
                             final String period,
                             final String periodUnit) {
        // update quota
        if (quota != null && quotaUnit != null && !quota.equals("") && !quotaUnit.equals("")) {
            device.setQuota(Long.parseLong(quota, 10));
            device.setQuotaBUnit(BUnit.valueOf(quotaUnit));
        }
        // update bandwidth
        if ((bandwidth != null && bandwidthUnit != null && !bandwidth.equals("") && !bandwidthUnit.equals(""))) {
            device.setBandwidth(Long.parseLong(bandwidth, 10));
            device.setBandwidthBUnit(BUnit.valueOf(bandwidthUnit));
        }
        // update period
        if (period != null && periodUnit != null && !period.equals("") && !periodUnit.equals("")) {

            // remove existing periods of a Device
            if (this.periodRepository.findAllByMac(device.getMac()).size() != 0)
                this.periodRepository.deleteByAllMac(device.getMac());

            device.setPeriod(Long.parseLong(period, 10));
            device.setPeriodUnit(TUnit.valueOf(periodUnit));
            this.scheduleConfig.setScheduler(device);
        }
        this.iptables.addRules(device);
        this.deviceRepository.saveAndFlush(device);
        logger.warn("{} -> allowed", device);
    }

    /**
     * Gets {@link Date} of given hours ago.
     *
     * @param hour The past hour.
     * @return The {@link Date} of given {@code hour} ago.
     */
    private Date getDateFromGivenHourAge(final String hour) {
        final long HOUR_IN_MS = 60 * 60 * 1000;
        return new Date(System.currentTimeMillis() - (Long.parseLong(hour) * HOUR_IN_MS));
    }

}


