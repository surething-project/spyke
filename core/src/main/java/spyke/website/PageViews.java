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
import spyke.engine.iptables.component.Iptables;
import spyke.engine.iptables.IptablesManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PageViews {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(PageViews.class);

    @Autowired
    private Iptables iptables;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private IptablesManager iptablesManager;

    @GetMapping("/devices")
    public String devices(final Model model) {
        final List<Device> devices = this.deviceRepository.findAll();
        model.addAttribute("devices", devices);
        return "devices";
    }

    @GetMapping("/device/{mac}")
    public String device(@PathVariable("mac") final String mac, final Model model) {
        final Device device = this.deviceRepository.findById(mac).get();
        model.addAttribute("device", device);
        return "device";
    }

    @GetMapping("/iplist/{mac}")
    public String iplist(@PathVariable("mac") final String mac, final Model model) {
        final Device device = this.deviceRepository.findById(mac).get();
        final Map<String, String> ipname = this.iptablesManager.getList(device);
        model.addAttribute("device", device);
        model.addAttribute("iplist", ipname);

        final Map<String, String> blocklist = new HashMap<>();
        for (final String ip : this.iptables.getBlacklist()) {
            try {
                final InetAddress address = InetAddress.getByName(ip);
                blocklist.put(address.getHostAddress(), address.getCanonicalHostName());
            } catch (final UnknownHostException e) {
                logger.error("Unknown Host Exception: {}", e.getMessage());
            }
        }
        model.addAttribute("blocklist", blocklist);
        return "iplist";
    }
}
