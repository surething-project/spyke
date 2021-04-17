package spyke.engine.iptables.component;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import spyke.database.model.Device;
import spyke.engine.iptables.model.CompoundRules;
import spyke.engine.iptables.model.Rule;
import spyke.engine.iptables.model.types.Filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("iptables")
public class Iptables {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(Iptables.class);

    /**
     * The map with key as {@code device} and value as map of rule number with corresponding {@code rule}. FIXME
     * refactor map to an object
     */
    private final Map<Device, CompoundRules> deviceRules = new ConcurrentHashMap<>();

    /**
     * The list of blacklist.
     */
    private final Collection<Rule> blacklist = new HashSet<>();

    /**
     * Gets the list of blacked devices.
     *
     * @return list of blacked devices.
     */
    public List<String> getBlacklist() {
        return this.blacklist.stream()
                .flatMap(rule -> rule.getDestination().isPresent() ? Stream.of(rule.getDestination().get()) : Stream.empty())
                .collect(Collectors.toList());
    }

    /**
     * Restores the default iptables rules for spyke.
     *
     * @return {@code true} if succeed, {@code false} otherwise.
     */
    public boolean restoreDefaultRules() {

        final File iptablesConf = new File("script/config/iptables.ipv4.conf");
        if (isAdmin() && iptablesConf.exists() &&
                execute("sudo /sbin/iptables-restore < " + iptablesConf.getAbsolutePath())) {
            logger.info("Iptables is restored with spyke default rules.");
            return true;
        }
        logger.error("Iptables failed to restore with spyke default rules.");
        return false;
    }

    /**
     * Blocks device by given ip or domain.
     *
     * @param domain The given ip or domain to block.
     * @return {@code true} if succeed, {@code false} otherwise.
     */
    public boolean block(final String domain) {
        if (domain != null && !domain.equals("")) {
            try {
                final InetAddress address = InetAddress.getByName(domain);
                final Rule rule = Rule.builder().destination(address.getHostAddress()).filter(Filter.DROP).build();
                if (!this.blacklist.contains(rule) && execute("sudo /sbin/iptables -t mangle -A POSTROUTING " + rule.toString())) {
                    this.blacklist.add(rule);
                    logger.info(domain + " blocked!");
                    return true;
                }
            } catch (final UnknownHostException e) {
                logger.error(e.getMessage());
            }
        }
        logger.debug("Blocking domain {} failed", domain);
        return false;
    }

    /**
     * Unblocks device by given ip or domain.
     *
     * @param domain The given ip or domain to block.
     * @return {@code true} if succeed, {@code false} otherwise.
     */
    public boolean unblock(final String domain) {
        if (domain != null && !domain.equals("")) {
            if (this.blacklist.size() != 0) {
                final Rule rule = Rule.builder().destination(domain).filter(Filter.DROP).build();
                if (this.blacklist.contains(rule) && execute("sudo /sbin/iptables -t mangle -D POSTROUTING " + rule.toString())) {
                    this.blacklist.remove(rule);
                    logger.info(domain + " unblocked!");
                    return true;
                }
            }
        }
        logger.debug("Unblocking domain {} failed", domain);
        return false;
    }

    /**
     * Renews rules for given device.
     * <p/>
     * Example
     *
     * @param device The device.
     */
    public void renewRules(final Device device) {

        int i = 0;
        for (final Rule rule : this.deviceRules.get(device).getRules()) {
            i++;
            if (execute("sudo /sbin/iptables -R " + device.getIp() + " " + (i) + " " + rule.toString())) {
                logger.warn("Added rule: {}", rule);
            }
        }
    }

    /**
     * Deletes rules for given device.
     *
     * @param device The device.
     */
    public void deleteRules(final Device device) {
        // sudo iptables -D 192.168.8.95 -s 192.168.8.95/32 -j ACCEPT  # delete first appeared
        final CompoundRules compoundRules = this.deviceRules.get(device);
        for (final Rule rule : compoundRules.getRules()) {
            if (execute("sudo /sbin/iptables -D " + device.getIp() + " " + rule.toString())) {
                logger.warn("Deleted rule: {}", rule);
            }
        }
        this.deviceRules.put(device, CompoundRules.builder().defaultDrop(device.getIp()).build());
    }

    /**
     * Adds rules for given device.
     *
     * @param device The device.
     */
    public void addRules(final Device device) {
        /*
        example:
            String ip, String quota, String hashlimit
            sudo iptables -A FORWARD -s 192.168.8.95/32 -j ACCEPT
            sudo iptables -A FORWARD -s 192.168.8.95/32 -m quota --quota 1024
                -m hashlimit --hashlimit-name 192.168.8.95 --hashlimit-upto 512kb/s --hashlimit-mode srcip --hashlimit-srcmask 32 -j ACCEPT
                -m connlimit --connlimit-upto 20 --connlimit-mask 32    # this is for limiting number of connections
        note:
            Quota is only considered as bytes without unit defined
            Hashlimit can be used with <b, kb, mb, gb> unit and s as time unit
        */
        if (this.deviceRules.containsKey(device)) {
            if (!this.deviceRules.get(device).getRules().isEmpty()) {
                replaceRule(device, getCompoundRules(device));
            } else {
                insertToList(device, getCompoundRules(device));
            }
        } else if (!this.deviceRules.containsKey(device) && newDevice(device)) {
            // -I FORWARD 1 = insert at the first
            if (execute("sudo /sbin/iptables -I FORWARD -j " + device.getIp())) {
                logger.info("Chain [{}] created!", device.getIp());
            }
            insertToList(device, getCompoundRules(device));
        } else {
            logger.error("Add rules failed for device: {}", device);
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
                logger.error("Read iptables failed: " + line);
            }
            process.waitFor();
            inputReader.close();
            errorReader.close();
        } catch (final Exception e) {
            logger.error("Process get bytes from iptables failed: " + e.getMessage());
        }
        return convertUploadLinesToBytes(lines, device.getIp());
    }

    /**
     * Adds rules of given device to iptables.
     *
     * @param device The device.
     * @param rules  The map with key as rules index and value with corresponding rule.
     */
    private void insertToList(final Device device, final CompoundRules rules) {
        boolean success = true;
        for (final Rule rule : rules.getRules()) {
            if (!execute("sudo /sbin/iptables -A " + device.getIp() + " " + rule)) {
                logger.error("Failed appending Rule {}", rule);
                success = false;
            }
        }
        if (success) {
            this.deviceRules.put(device, rules);
        }
    }

    /**
     * Replace rules of given device with new given rules.
     *
     * @param device The device.
     * @param rules  The map with key as rules index and value with corresponding rule.
     */
    private void replaceRule(final Device device, final CompoundRules rules) {

        final boolean success;

        final int deviceRulesSize = this.deviceRules.get(device).getRules().size();
        final int rulesSize = rules.getRules().size();
        if (deviceRulesSize == rulesSize) {
            success = repRules(device, rules);
        } else {
            if (deviceRulesSize > rulesSize) {
                // If device has one more rule, it means that optional (4) outgoing bandwidth was being used, and new rule doesn't
                // note we are not considering incoming bandwidth yet
                final Optional<Rule> fourth = this.deviceRules.get(device).getFourth();
                if (fourth.isPresent() && !execute("sudo /sbin/iptables -D " + device.getIp() + " " + fourth.get())) {
                    logger.error("Failed inserting Rule {}", fourth.get());
                    return;
                }
                success = repRules(device, rules);
            } else {
                success = repRules(device, rules);
                // If device has one less rule, it means that optional (4) outgoing bandwidth was not being used, and new rule does
                // note we are not considering incoming bandwidth yet, so we need to append the last rule
                final Rule eighth = this.deviceRules.get(device).getEighth();
                if (!execute("sudo /sbin/iptables -I " + device.getIp() + " " + eighth)) {
                    logger.error("Failed inserting Rule {}", eighth);
                    return;
                }
            }
        }
        if (success) {
            this.deviceRules.put(device, rules);
        }
    }

    private boolean repRules(final Device device, final CompoundRules rules) {
        int i = 0;
        boolean success = true;
        for (final Rule rule : rules.getRules()) {
            i++;
            if (!execute("sudo /sbin/iptables -R " + device.getIp() + " " + i + " " + rule)) {
                logger.error("Failed replacing Rule {}", rule);
                success = false;
            }
        }
        return success;
    }

    private CompoundRules getCompoundRules(final Device device) {

        final CompoundRules rules = this.deviceRules.getOrDefault(
                device,
                CompoundRules.builder().defaultDrop(device.getIp()).build()
        );

        final CompoundRules.Builder compoundRules = CompoundRules.builder().from(rules);

        if (device.getBandwidth() != 0) {
            final String hashLimit = device.getBandwidth() + String.valueOf(device.getBandwidthBUnit()) + "/s";
            final Rule bandwidthRule = Rule.builder()
                    .source(device.getIp())
                    .filter(Filter.DROP)
                    .hashlimit(hashLimit)
                    .hashlimitName("s" + device.getIp()) /* s for source and d for destination */
                    .build();
            // 4 - outgoing bandwidth
            compoundRules.fifth(bandwidthRule);
        }

        if (device.getQuota() != 0) {
            final Rule acceptLimitedRule = Rule.builder()
                    .source(device.getIp())
                    .filter(Filter.ACCEPT)
                    .quota(convertToUnit(device))
                    .build();
            // 5 - outgoing quota
            compoundRules.sixth(acceptLimitedRule);
        } else {
            final Rule acceptSourceRule = Rule.builder().source(device.getIp()).filter(Filter.ACCEPT).build();
            // 6 - outgoing unlimited
            compoundRules.sixth(acceptSourceRule);
        }

        // TODO: incoming is unlimited
        final Rule acceptSourceRule = Rule.builder().destination(device.getIp()).filter(Filter.ACCEPT).build();
        // incoming unlimited
        compoundRules.third(acceptSourceRule);
        return compoundRules.build();
    }

    private boolean newDevice(final Device device) {
        if (execute("sudo /sbin/iptables -N " + device.getIp())) {
            this.deviceRules.put(device, CompoundRules.builder().defaultDrop(device.getIp()).build());
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
                    bytes[0] = convertToBytes(uploadLine[0].split("ACCEPT")[0].replaceAll(
                            "(^\\s+|\\s+$)",
                            ""
                    ).split("\\s+")[1]);
                } else if (uploadLine[0].contains("DROP")) {
                    // there are two upload drop, one for the bandwidth another for normal. In this case, the normal will replace the bw drop.
                    bytes[1] = convertToBytes(uploadLine[0].split("DROP")[0].replaceAll("(^\\s+|\\s+$)", "").split(
                            "\\s+")[1]);
                }
            }
        }
        return bytes;
    }

    private long convertToBytes(String bytes_value) {
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

    /**
     * Gets the value of bytes converted.
     *
     * @param device The expected device.
     * @return The bytes for quota.
     */
    private String convertToUnit(final Device device) {
        switch (device.getQuotaBUnit()) {
            case kb:
                return String.valueOf(device.getQuota() * 1024);
            case mb:
                return String.valueOf(device.getQuota() * 1024 * 1024);
            case gb:
                return String.valueOf(device.getQuota() * 1024 * 1024 * 1024);
            default:
                return String.valueOf(device.getQuota());
        }
    }

    /**
     * Execute task on the terminal.
     *
     * @param task The expected task command.
     * @return {@code true} if succeed, {@code false} otherwise.
     */
    private boolean execute(final String task) {
        try {
            synchronized (this) {
                final Process p = Runtime.getRuntime().exec(task);
                p.waitFor();
            }
            logger.info("Task success: {} ", task);
        } catch (final Exception e) {
            logger.error("Task fail: {}", task);
            return false;
        }
        return true;
    }

    /**
     * Check whether the current user has admin privilege.
     *
     * @return {@code true} if the current user has admin privilege, {@code false} otherwise.
     */
    public boolean isAdmin() {
        try {
            final Process p;
            synchronized (this) {
                p = Runtime.getRuntime().exec("id -u");
                p.waitFor();
            }
            try (final BufferedReader buffer = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                if (buffer.readLine().equals("0")) {
                    logger.warn("The user has admin privilege");
                    return true;
                }
            }
        } catch (final Exception e) {
            logger.error("Checking is admin failed: {}", e.getMessage());
        }
        logger.warn("The user has NOT admin privilege");
        return false;
    }

    /**
     * Saves the system iptables rules to given file.
     *
     * @return {@code true} if succeed, {@code false} otherwise.
     */
    public boolean saveSystemRules(final File saveIptablesFile) {

        if (isAdmin() && execute("/sbin/iptables-save > " + saveIptablesFile.getAbsolutePath())) {
            logger.info("System Iptables rules are saved on {}.", saveIptablesFile.getAbsolutePath());
            return true;
        }

        logger.error("System Iptables rules failed to save on {}.", saveIptablesFile.getAbsolutePath());
        return false;
    }

    /**
     * Saves the system iptables rules to given file.
     *
     * @return {@code true} if succeed, {@code false} otherwise.
     */
    public boolean restoreSystemRules(final File saveIptablesFile) {

        if (isAdmin() && execute("/sbin/iptables-restore < " + saveIptablesFile.getAbsolutePath())) {
            logger.info("System Iptables rules are restored from {}.", saveIptablesFile.getAbsolutePath());
            saveIptablesFile.delete();
            return true;
        }

        logger.error("System Iptables rules failed to restore from {}.", saveIptablesFile.getAbsolutePath());
        return false;
    }
}

