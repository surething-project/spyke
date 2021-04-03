package spyke.monitor.iptables.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import spyke.database.model.Device;
import spyke.monitor.iptables.model.Rule;
import spyke.monitor.iptables.model.types.Filter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component("iptables")
public class Iptables {

    /**
     * The logger
     */
    private final Logger logger = LoggerFactory.getLogger(Iptables.class);
    // each device should have a list of rules

    /**
     * The map with key as {@code device} and value as map of rule number with corresponding {@code rule}.
     * FIXME refactor map to an object
     */
    private final Map<Device, Map<Integer, Rule>> deviceRules = new ConcurrentHashMap<>();

    /**
     * The list of blacklist.
     */
    private final List<Rule> blacklist = new ArrayList<>();

    /**
     * Gets the list of blacklist.
     *
     * @return
     */
    public List<String> getBlacklist() {
        return this.blacklist.stream()
                .map(Rule::getDestination)
                .collect(Collectors.toList());
    }

    /**
     * Restores the default iptables rules for spyke.
     * 
     * @return {@code true} if succeed, {@code false} otherwise.
     */
    public boolean restoreDefaultRules() {
        // FIXME update to FS
        if (execute("sudo /sbin/iptables-restore < " + "iptables.ipv4.conf")) {
            this.logger.info("Iptables is restored with spyke default rules.");
            return true;
        }
        return false;
    }

    /**
     * Blocks device by given ip or domain.
     *
     * @param domain The given ip or domain to block.
     * @return {@code true} if succeed, {@code false} otherwise.
     */
    public boolean block(final String domain) { // block can be ip or domain
        if (domain != null && !domain.equals("")) {
            try {
                final InetAddress address = InetAddress.getByName(domain);
                final Rule rule = new Rule();
                rule.setDestination(address.getHostAddress());
                rule.setFilter(Filter.DROP);
                //logger.info(address.getHostAddress());
                //logger.info(address.getCanonicalHostName());
                if (!this.blacklist.contains(rule) && execute("sudo /sbin/iptables -t mangle -A POSTROUTING " + rule.toString())) {
                    this.blacklist.add(rule);
                    this.logger.info(domain + " blocked!");
                    return true;
                }
            } catch (final UnknownHostException e) {
                //e.printStackTrace();
                this.logger.info(e.getMessage());
            }
        }
        return false;
    }

    /**
     * Unblocks device by given ip or domain.
     *
     * @param domain The given ip or domain to block.
     * @return {@code true} if succeed, {@code false} otherwise.
     */
    public boolean unblock(final String domain) { // ip or domain one is blocked
        if (domain != null && !domain.equals("")) {
            if (this.blacklist.size() != 0) {
                final Rule rule = new Rule();
                rule.setDestination(domain);
                rule.setFilter(Filter.DROP);
                if (this.blacklist.contains(rule) && execute("sudo /sbin/iptables -t mangle -D POSTROUTING " + rule.toString())) {
                    this.blacklist.remove(rule);
                    this.logger.info(domain + " unblocked!");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Renews rules for given device.
     *
     * @param device The device.
     */
    public void renRule(final Device device) {
        /*
        example: -A for append and -I for insert
            sudo iptables -N 192.168.8.95   # create chain
            sudo iptables -A FORWARD -j 192.168.8.95
            sudo iptables -A 192.168.8.95 -s 192.168.8.95/32 -m quota --quota 1024 -m limit --limit 1/s -j ACCEPT	# 1 packet has 1500bytes usually
            sudo iptables -A 192.168.8.95 -s 192.168.8.95/32 -j DROP
         */
        /*
            1st = forward in accept
            2nd = forward out accept
            3rd = forward in drop
            4th = forward out drop
         */
        int i = 0;
        for (final Rule rule : this.deviceRules.get(device).values()) {
            i++;
            if (execute("sudo /sbin/iptables -R " + device.getIp() + " " + (i) + " " + rule.toString())) {
                this.logger.info(rule + " renewed!");
            }
        }
    }

    /**
     * Deletes rules for given device.
     *
     * @param device The device.
     */
    public void delRule(final Device device) {
        // sudo iptables -D 192.168.8.95 -s 192.168.8.95/32 -j ACCEPT  # delete first appeared
        for (final Rule rule : this.deviceRules.get(device).values()) {
            if (execute("sudo /sbin/iptables -D " + device.getIp() + " " + rule.toString())) {
                this.logger.info(rule + " deleted!");
            }
        }
        this.deviceRules.put(device, new ConcurrentHashMap<Integer, Rule>());
    }

    /**
     * Adds rules for given device.
     *
     * @param device The device.
     */
    public void addRule(final Device device) {
        /*
        example:
            String ip, String quota, String hashlimit
            sudo iptables -A FORWARD -s 192.168.8.95/32 -j ACCEPT
            sudo iptables -A FORWARD -s 192.168.8.95/32 -m quota --quota 1024
                -m hashlimit --hashlimit-name 192.168.8.95 --hashlimit-upto 512kb/s --hashlimit-mode srcip --hashlimit-srcmask 32 -j ACCEPT
                -m connlimit --connlimit-upto 20 --connlimit-mask 32    # this is for limiting number of connections
        */
        /*
        note:
            Quota is only considered as bytes without unit defined
            Hashlimit can be used with <b, kb, mb, gb> unit and s as time unit
        */
        if (this.deviceRules.containsKey(device)) {
            if (!this.deviceRules.get(device).isEmpty()) {
                final Map<Integer, Rule> rules = getRules(device);
                replaceRule(device, rules);
            } else {
                final Map<Integer, Rule> rules = getRules(device);
                insertToList(device, rules);
            }
        } else if (!this.deviceRules.containsKey(device) && newDevice(device)) {
            // -I FORWARD 1 = insert at the first
            if (execute("sudo /sbin/iptables -I FORWARD -j " + device.getIp())) {
                this.logger.info("Chain [" + device.getIp() + "] created!");
            }
            /* INPUT & OUTPUT ACCEPT is no longer necessary
            Rule input = getRule(device.getIp(), Filter.ACCEPT, true);
            Rule output = getRule(device.getIp(), Filter.ACCEPT, false);

            if(execute("sudo /sbin/iptables -A INPUT " + input.toString())){
                deviceRules.get(device).add(input);
            }
            if(execute("sudo /sbin/iptables -A OUTPUT " + output.toString())){
                deviceRules.get(device).add(output);
            }
             */
            final Map<Integer, Rule> rules = getRules(device);
            insertToList(device, rules);
        }
    }

    /**
     * Gets uploaded bytes from given device.
     *
     * @param device The device.
     * @return The tuple with accepted and dropped bytes.
     */
    public long[] extractBytes(final Device device) {
        final List<String> lines = new ArrayList<String>();
        try {
            final String exec = "sudo /sbin/iptables -nvL " + device.getIp() + " | grep \"" + device.getIp() + "\"";
            final ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", exec);
            final Process process = processBuilder.start();
            String line;
            final BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = inputReader.readLine()) != null) {
                lines.add(line);
            }

            final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                this.logger.error("Read iptables failed: " + line);
            }
            process.waitFor();
            inputReader.close();
            errorReader.close();
        } catch (final Exception e) {
            this.logger.error("Process get bytes from iptables failed: " + e.getMessage());
        }
        return convertUploadLinesToBytes(lines, device.getIp());
    }

    private void insertToList(final Device device, final Map<Integer, Rule> rules) {
        boolean success = true;
        for (final Map.Entry<Integer, Rule> entry : rules.entrySet()) {
            if (!execute("sudo /sbin/iptables -A " + device.getIp() + " " + entry.getValue().toString())) {
                success = false;
            }
        }
        if (success) {
            this.deviceRules.put(device, rules);
        }
    }

    private void replaceRule(final Device device, final Map<Integer, Rule> rules) {
        /*
            1-incoming
            2-outgoing with bw
            3-outgoing with qt
            4-outgoing unlimited
            5-outgoing drop
            6-incoming drop

            this may be 4 or 5 rules
         */

        boolean success = true;

        if (this.deviceRules.get(device).size() == rules.size()) {
            success = repRules(device, rules);
        } else {
            if (this.deviceRules.get(device).size() > rules.size()) {
                if (!execute("sudo /sbin/iptables -D " + device.getIp() + " " + this.deviceRules.get(device).get(2).toString())) {
                    return;
                }
                success = repRules(device, rules);
            } else {
                success = repRules(device, rules);
                if (!execute("sudo /sbin/iptables -I " + device.getIp() + " " + this.deviceRules.get(device).get(5).toString())) {
                    return;
                }
            }
        }
        if (success) {
            this.deviceRules.put(device, rules);
        }
    }

    private boolean repRules(final Device device, final Map<Integer, Rule> rules) {
        int i = 0;
        boolean success = true;
        for (final Rule entry : rules.values()) {
            i++;
            if (!execute("sudo /sbin/iptables -R " + device.getIp() + " " + i + " " + entry.toString())) {
                success = false;
            }
        }
        return success;
    }

    private Map<Integer, Rule> getRules(final Device device) {
        /*
            1-incoming
            2-outgoing with bw
            3-outgoing with qt
            4-outgoing unlimited
            5-outgoing drop
            6-incoming drop
         */

        final Map<Integer, Rule> rules = new ConcurrentHashMap<>();
        rules.put(5, getRule(device.getIp(), Filter.DROP, true));
        rules.put(6, getRule(device.getIp(), Filter.DROP, false));

        String quota = null;
        String hashlimit = null;
        if (device.getBandwidth() != 0) {
            hashlimit = device.getBandwidth() + String.valueOf(device.getBandwidthBUnit()) + "/s";
            rules.put(2, getHashlimitRule(device.getIp(), Filter.DROP, true, hashlimit));
        }
        if (device.getQuota() != 0) {
            switch (device.getQuotaBUnit()) {
                case kb:
                    quota = String.valueOf(device.getQuota() * 1024);
                    break;
                case mb:
                    quota = String.valueOf(device.getQuota() * 1024 * 1024);
                    break;
                case gb:
                    quota = String.valueOf(device.getQuota() * 1024 * 1024 * 1024);
                    break;
                default:
                    quota = String.valueOf(device.getQuota());
            }
            rules.put(3, getQuotaRule(device.getIp(), Filter.ACCEPT, true, quota));
        }
        if (quota == null) {
            rules.put(4, getRule(device.getIp(), Filter.ACCEPT, true));
        }

        // for download, it is not limited yet
        rules.put(1, getRule(device.getIp(), Filter.ACCEPT, false));
        return rules;
    }

    private boolean newDevice(final Device device) {
        if (execute("sudo /sbin/iptables -N " + device.getIp())) {
            this.deviceRules.put(device, new ConcurrentHashMap<Integer, Rule>());
            return true;
        }
        return false;
    }

    private long[] convertUploadLinesToBytes(final List<String> lines, final String deviceIp) {
        // bytes[0] = 0 && bytes[1] = 0
        final long[] bytes = new long[2];
        for (int i = 1; i < lines.size(); i++) {
            /* first line is skipped:
              Chain 192.168.8.70 (1 references)
                   59  9845 ACCEPT     all  --  *      *       0.0.0.0/0            192.168.8.70
                   20 85343 DROP       all  --  *      *       192.168.8.70         0.0.0.0/0           bandwidth: 10 mb/s
                  113 10204 ACCEPT     all  --  *      *       192.168.8.70         0.0.0.0/0            quota: 10240 bytes limit: up to 1kb/s
                 1320 85343 DROP       all  --  *      *       192.168.8.70         0.0.0.0/0
                    0     0 DROP       all  --  *      *       0.0.0.0/0            192.168.8.70
             */
            final String[] uploadLine = lines.get(i).split("0.0.0.0/0");
            if (!uploadLine[1].contains(deviceIp)) {
                if (uploadLine[0].contains("ACCEPT")) {
                    // the 2nd ACCEPT is read
                    bytes[0] = convertExtractBytes(uploadLine[0].split("ACCEPT")[0].replaceAll(
                            "(^\\s+|\\s+$)",
                            ""
                    ).split("\\s+")[1]);
                } else if (uploadLine[0].contains("DROP")) {
                    // there are two upload drop, one for the bandwidth another for normal. In this case, the normal will replace the bw drop.
                    bytes[1] = convertExtractBytes(uploadLine[0].split("DROP")[0].replaceAll("(^\\s+|\\s+$)", "").split(
                            "\\s+")[1]);
                }
            }
        }
        return bytes;
    }

    private long convertExtractBytes(String bytes_value) {
        final long bytes;
        switch (bytes_value.charAt(bytes_value.length() - 1)) {
            case 'K':
                bytes_value = extractLastChar(bytes_value);
                bytes = Long.parseLong(
                        //2nd value before ACCEPT
                        bytes_value,
                        10
                ) * 1024;
                break;
            case 'M':
                bytes_value = extractLastChar(bytes_value);
                bytes = Long.parseLong(
                        //2nd value before ACCEPT
                        bytes_value,
                        10
                ) * 1024 * 1024;
                break;
            case 'G':
                bytes_value = extractLastChar(bytes_value);
                bytes = Long.parseLong(
                        //2nd value before ACCEPT
                        bytes_value,
                        10
                ) * 1024 * 1024 * 1024;
                break;
            default:
                bytes = Long.parseLong(
                        //2nd value before ACCEPT
                        bytes_value,
                        10
                );
        }
        return bytes;
    }

    private String extractLastChar(final String str) {
        return str.substring(0, str.length() - 1);
    }

    /*
        get rules by parameters
     */
    private Rule getRule(final String ip, final Filter filter, final boolean source) {
        final Rule rule = new Rule();
        if (source) {
            rule.setSource(ip);
        } else {
            rule.setDestination(ip);
        }
        /* ConnLimit limit the established connection, not new connection.
        if(filter.equals(Filter.ACCEPT))
            rule.setLimit("10"); // 10 packet per minute with 5 limit-burst as default
        */
        rule.setFilter(filter);
        return rule;
    }

    private Rule getQuotaRule(final String ip, final Filter filter, final boolean source, final String quota) {
        final Rule rule = getRule(ip, filter, source);
        if (quota != null)
            rule.setQuota(quota);
        return rule;
    }

    private Rule getHashlimitRule(final String ip, final Filter filter, final boolean source, final String hashlimit) {
        final Rule rule = getRule(ip, filter, source);
        if (hashlimit != null) {
            String hashlimit_name;
            if (source)
                hashlimit_name = "s";
            else
                hashlimit_name = "d";
            hashlimit_name += ip;
            rule.setHashlimitName(hashlimit_name);
            rule.setHashlimit(hashlimit);
        }
        return rule;
    }

    /*
        command task
     */
    private synchronized boolean execute(final String task) {
        try {
            final Process p = Runtime.getRuntime().exec(task);
            p.waitFor();
            this.logger.info("Task " + task + " success");
        } catch (final Exception e) {
            this.logger.error("Task " + task + " fail");
            return false;
        }
        return true;
    }
}

