package spyke.website;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import spyke.database.model.Device;
import spyke.database.model.Period;
import spyke.database.model.PeriodId;
import spyke.database.repository.DeviceRepository;
import spyke.database.repository.PeriodRepository;
import spyke.database.model.types.BUnit;
import spyke.database.model.types.Status;
import spyke.database.model.types.TUnit;
import spyke.monitor.iptables.component.Iptables;
import spyke.monitor.iptables.util.OperatingSystem;
import spyke.monitor.config.ScheduleConfig;
import spyke.monitor.manage.IptablesLog;

import java.util.*;

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

    @RequestMapping(path = "/device", method = RequestMethod.POST)
    public ResponseEntity<Device> getDeviceById(@RequestParam("mac") final String mac) {
        Device device = null;
        if(deviceRepository.findById(mac).isPresent()){
            device = deviceRepository.findById(mac).get();
        }
        return new ResponseEntity<Device>(device, HttpStatus.OK);
    }
    @RequestMapping(path = "/devices", method = RequestMethod.POST)
    public ResponseEntity<List<Device>> getDevices() {
        List<Device> devices = deviceRepository.findAll();
        return new ResponseEntity<List<Device>>(devices, HttpStatus.OK);
    }
    @RequestMapping(path = "/control/status", method = RequestMethod.POST)
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
        if(quota.length() > 10 || bandwidth.length() > 10 || period.length() > 10)
            return new ResponseEntity<Boolean>(false, HttpStatus.OK);
        logger.info("Starting change status!");
        if (deviceRepository.findById(mac).isPresent()) {
            Device device = deviceRepository.findById(mac).get();
            if (status.equals("ALLOWED")) {
                Device newDevice = modifyDeviceIfDifferent(device, quota, quotaUnit, bandwidth, bandwidthUnit, period, periodUnit);
                if (OperatingSystem.isLinux())
                    iptables.addRule(newDevice);
                changed = true;
                logger.warn(device + " -> allowed");
            } else if (status.equals("BLOCKED")) {
                if (device.getStatus() == Status.ALLOWED) {
                    scheduleConfig.cancelScheduler(device);
                    if (OperatingSystem.isLinux())
                        iptables.delRule(device);
                }
                changed = true;
                logger.warn(device + " -> blocked");
            } else {
                logger.error("Invalid status...");
                return new ResponseEntity<Boolean>(changed, HttpStatus.OK);
            }
            device.setStatus(Status.valueOf(status));
            deviceRepository.saveAndFlush(device);
        } else
            logger.error("MAC (" + mac + ") not found");
        return new ResponseEntity<Boolean>(changed, HttpStatus.OK);
    }

    @RequestMapping(path = "/control/period/{mac}/limited", method = RequestMethod.GET)
    public ResponseEntity<List<Period>> getPeriodsByMacWithLimit(@PathVariable("mac") String mac) {
        List<Period> periods = new ArrayList<Period>();
        if(deviceRepository.findById(mac).isPresent()) {
            Device device = deviceRepository.findById(mac).get();
            long time = device.getPeriod() * 1000 * 24; // 24 is the how many; Perhaps it will return last 23 periods
            if(time == 0) {
                time = 1000 * 24 * 60 * 60; // 1 hour by default
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
                        time = 1000 * 24 * 60 * 60; // 1 hour by default
                }
            }
            periods = periodRepository.findAllByTimeStartAfter(mac, new Date(System.currentTimeMillis() - time));

            long[] currentBytes = iptables.extractBytes(device);    // [0] = sent & [1] = dropped
            // currentBytes[0] != null && currentBytes[1] != null, by default a long is 0
            PeriodId currentPeriodId;
            if(periods.size()!=0)
                currentPeriodId = new PeriodId(periods.get(periods.size() - 1).getId().getEndTime(), new Date());  // start_time & end_time
            else {
                Date end_time_plus_one_minute = new Date(System.currentTimeMillis() - (time / 24) + 60000); // 60 * 1000
                currentPeriodId = new PeriodId(end_time_plus_one_minute, new Date());
            }
            Period currentPeriod = new Period(currentPeriodId, currentBytes[0], currentBytes[1]);  // PeriodId, passed, dropped
            periods.add(currentPeriod);
        }
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    @RequestMapping(path = "/control/device/period/{mac}", method = RequestMethod.GET)
    public ResponseEntity<String> getDevicePeriod(@PathVariable("mac") final String mac) {
        String period="not found";
        if(deviceRepository.findById(mac).isPresent()) {
            Device device = deviceRepository.findById(mac).get();
            period=device.getPeriod()+String.valueOf(device.getPeriodUnit());
            return new ResponseEntity<String>(period, HttpStatus.OK);
        }
        return new ResponseEntity<String>(period, HttpStatus.NOT_FOUND);
    }

    // add device
    @RequestMapping(path = "/control/device/add", method = RequestMethod.POST)
    public ResponseEntity<Boolean> addDevice(@RequestParam("ip") final String ip,
                                            @RequestParam("mac") final String mac,
                                            @RequestParam("name") final String name) {
        Device device = new Device(ip, mac, name, Status.NEW, 0, 0, 0, BUnit.kb, BUnit.kb, TUnit.m);
        deviceRepository.saveAndFlush(device);
        return new ResponseEntity<Boolean>(true, HttpStatus.OK);
    }

    // block ip or mac
    @RequestMapping(path = "/control/ip/block", method = RequestMethod.POST)
    public ResponseEntity<Boolean> getBlock(@RequestParam("block") final String block) {
        // TODO input validation
        return new ResponseEntity<Boolean>(iptables.block(block), HttpStatus.OK);
    }

    // unblock ip or mac
    @RequestMapping(path = "/control/ip/unblock", method = RequestMethod.POST)
    public ResponseEntity<Boolean> getUnblock(@RequestParam("unblockip") final String unblockip) {
        // TODO input validation
        return new ResponseEntity<Boolean>(iptables.unblock(unblockip), HttpStatus.OK);
    }

    // get list of blocked ip or mac
    @RequestMapping(path = "/control/ip/blacklist", method = RequestMethod.GET)
    public ResponseEntity<List<String>> getIpBlackList() {
        return new ResponseEntity<List<String>>(iptables.getBlacklist(), HttpStatus.OK);
    }

    // get list of outgoing ip
    @RequestMapping(path = "/control/ip/list/{mac}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> getIpList(@PathVariable("mac") String mac) {
        if(deviceRepository.findById(mac).isPresent()){
            Device device = deviceRepository.findById(mac).get();
            return new ResponseEntity<Map<String, String>>(iptablesLog.getList(device), HttpStatus.OK);
        }
        return new ResponseEntity<Map<String, String>>(new HashMap<String, String>(), HttpStatus.OK);
    }

    // test: renew device's rules
    @RequestMapping(path = "/control/rule/renew/{mac}", method = RequestMethod.GET)
    public ResponseEntity<Boolean> renewRule(@PathVariable("mac") String mac) {
        if(deviceRepository.findById(mac).isPresent()){
            Device device = deviceRepository.findById(mac).get();
            iptables.renRule(device);
        }
        return new ResponseEntity<Boolean>(true, HttpStatus.OK);
    }

    // test: get all periods
    @RequestMapping(path = "/control/period/list", method = RequestMethod.GET)
    public ResponseEntity<List<Period>> getPeriods() {
        List<Period> periods = periodRepository.findAll();
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    // test: get device's all periods
    @RequestMapping(path = "/control/period/{mac}/list", method = RequestMethod.GET)
    public ResponseEntity<List<Period>> getPeriodsByMac(@PathVariable("mac") String mac) {
        List<Period> periods = periodRepository.findAllByMac(mac);
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    // test: get device's periods from 'time' hours ago before  note: not working with param 0 since period is not saved yet
    @RequestMapping(path = "/control/period/{mac}/before/{time}", method = RequestMethod.GET)
    public ResponseEntity<List<Period>> getPeriodByMacAndStartBefore(@PathVariable("mac") String mac, @PathVariable("time") String paramtime) {
        List<Period> periods = periodRepository.findAllByTimeStartBefore(mac, getDate(paramtime));
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    // test: get device's periods from 'time' hours ago after   note: not working with param 0 since period is not saved yet
    @RequestMapping(path = "/control/period/{mac}/after/{time}", method = RequestMethod.GET)
    public ResponseEntity<List<Period>> getPeriodByMacAndStartAfter(@PathVariable("mac") String mac, @PathVariable("time") String paramtime) {
        List<Period> periods = periodRepository.findAllByTimeStartAfter(mac, getDate(paramtime));
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    // test: get device's period from 'time' hours ago  note: not working with param 0 since period is not saved yet
    @RequestMapping(path = "/control/period/{mac}/time/{time}", method = RequestMethod.GET)
    public ResponseEntity<List<Period>> getPeriodByMacAndTime(@PathVariable("mac") String mac, @PathVariable("time") String paramtime) {
        List<Period> periods = periodRepository.findAllByTime(mac, getDate(paramtime));
        return new ResponseEntity<List<Period>>(periods, HttpStatus.OK);
    }

    // test: is device's schedule cancelled?
    @RequestMapping(path = "/control/schedule/{mac}/cancelled", method = RequestMethod.GET)
    public ResponseEntity<Boolean> getSchedulerIsCancelled(@PathVariable("mac") String mac) {
        Device device = null;
        if(deviceRepository.findById(mac).isPresent()){
            device = deviceRepository.findById(mac).get();
        }
        return new ResponseEntity<Boolean>(scheduleConfig.isCancelled(device), HttpStatus.OK);
    }

    // test: does device's schedule exist?
    @RequestMapping(path = "/control/schedule/{mac}/exists", method = RequestMethod.GET)
    public ResponseEntity<Boolean> getSchedulerIsCancel(@PathVariable("mac") String mac) {
        Device device = null;
        if(deviceRepository.findById(mac).isPresent()){
            device = deviceRepository.findById(mac).get();
        }
        return new ResponseEntity<Boolean>(scheduleConfig.exists(device), HttpStatus.OK);
    }

    // test: get device passed and dropped values
    @RequestMapping(path = "/control/schedule/{mac}/bytes", method = RequestMethod.GET)
    public ResponseEntity<List<Long>> getBytesTest(@PathVariable("mac") String mac) {
        Device device = null;
        if(deviceRepository.findById(mac).isPresent()){
            device = deviceRepository.findById(mac).get();
        }
        List<Long> bytes = new ArrayList<Long>();
        for(long b:iptables.extractBytes(device)){
            bytes.add(b);
        }
        return new ResponseEntity<List<Long>>(bytes, HttpStatus.OK);
    }


    // perhaps, this function should not be here
    private Device modifyDeviceIfDifferent(Device device, String quota, String quotaUnit, String bandwidth, String bandwidthUnit, String period, String periodUnit){
        // update quota
        if((quota!=null && quotaUnit!=null && !quota.equals("") && !quotaUnit.equals("") &&
                (device.getQuota() != Long.parseLong(quota, 10) || !device.getQuotaBUnit().equals(quotaUnit)))
        ) {
            // SECURITY PROBLEM: input validation
            device.setQuota(Long.parseLong(quota, 10));
            device.setQuotaBUnit(BUnit.valueOf(quotaUnit));
        }
        // update bandwidth
        if((bandwidth!=null && bandwidthUnit!=null && !bandwidth.equals("") && !bandwidthUnit.equals("") &&
                (device.getBandwidth()!=Long.parseLong(bandwidth, 10) || !device.getBandwidthBUnit().equals(bandwidthUnit)))){
            // SECURITY PROBLEM: input validation
            device.setBandwidth(Long.parseLong(bandwidth, 10));
            device.setBandwidthBUnit(BUnit.valueOf(bandwidthUnit));
        }
        // update period
        if(period!=null && periodUnit!=null && !period.equals("") && !periodUnit.equals("") &&
                (device.getPeriod()!=Long.parseLong(period, 10) || !device.getPeriodUnit().equals(periodUnit))
        ){
            // remove existing periods of a Device
            if(periodRepository.findAllByMac(device.getMac()).size()!=0)
                periodRepository.deleteByAllMac(device.getMac());

            // SECURITY PROBLEM: input validation
            device.setPeriod(Long.parseLong(period, 10));
            device.setPeriodUnit(TUnit.valueOf(periodUnit));
            scheduleConfig.setScheduler(device);
        }
        return device;
    }

    private Date getDate(String time){
        long HOUR_IN_MS = 1000 * 60 * 60;
        long hour = Long.valueOf(time);
        return new Date(System.currentTimeMillis() - (hour * HOUR_IN_MS));
    }

}


