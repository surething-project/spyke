package spyke.website;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import spyke.database.model.Device;
import spyke.database.repository.DeviceRepository;
import spyke.monitor.iptables.component.Iptables;
import spyke.monitor.manage.IptablesLog;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PageViews {

    private static final Logger logger = LoggerFactory.getLogger(PageViews.class);

    @Autowired
    private Iptables iptables;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private IptablesLog iptablesLog;

    @GetMapping("/devices")
    public String devices(Model model) {
        List<Device> devices = deviceRepository.findAll();
        model.addAttribute("devices", devices);
        return "devices";
    }

    @GetMapping("/device/{mac}")
    public String device(@PathVariable("mac") String mac, Model model) {
        Device device = deviceRepository.findById(mac).get();
        model.addAttribute("device", device);
        return "device";
    }

    @GetMapping("/iplist/{mac}")
    public String iplist(@PathVariable("mac") String mac, Model model) {
        Device device = deviceRepository.findById(mac).get();
        Map<String, String > ipname = iptablesLog.getList(device);
        model.addAttribute("device", device);
        model.addAttribute("iplist", ipname);
        Map<String, String > blocklist = new HashMap<>();
        for(String ip : iptables.getBlacklist()) {
            try {
                InetAddress address = InetAddress.getByName(ip);
                blocklist.put(address.getHostAddress(), address.getCanonicalHostName());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        model.addAttribute("blocklist", blocklist);
        return "iplist";
    }
}
