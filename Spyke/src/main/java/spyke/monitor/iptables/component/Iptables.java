package spyke.monitor.iptables.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import spyke.database.model.Device;
import spyke.monitor.iptables.model.Rule;
import spyke.monitor.iptables.model.types.Filter;

import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component("iptables")
public class Iptables{

    private Logger logger = LoggerFactory.getLogger(Iptables.class);
    // each device should have a list of rules
    private Map<Device, Map<Integer, Rule>> deviceRules = new ConcurrentHashMap<>();
    private List<Rule> blacklist = new ArrayList<>();

    public List<String> getBlacklist(){
        List<String> list = blacklist.stream()
                .map(e -> e.getDestination())
                .collect(Collectors.toList());
        return list;
    }

    public boolean restoreRules() {
        // FIXME update to FS
        if (execute("sudo /sbin/iptables-restore < " + "iptables.ipv4.conf")){
            logger.info("Iptables is restored with spyke default rules.");
            return true;
        }
        return false;
    }

    public boolean block(String block){ // block can be ip or domain
        if( block!=null && !block.equals("")) {
            try {
                InetAddress address = InetAddress.getByName(block);
                Rule rule = new Rule();
                rule.setDestination(address.getHostAddress());
                rule.setFilter(Filter.DROP);
                //logger.info(address.getHostAddress());
                //logger.info(address.getCanonicalHostName());
                if (!blacklist.contains(rule) && execute("sudo /sbin/iptables -t mangle -A POSTROUTING " + rule.toString())) {
                    blacklist.add(rule);
                    logger.info(block + " blocked!");
                    return true;
                }
            } catch (UnknownHostException e) {
                //e.printStackTrace();
                logger.info(e.getMessage());
            }
        }
        return false;
    }

    public boolean unblock(String ip){ // ip or domain one is blocked
        if( ip!=null && !ip.equals("")  ) {
            if (blacklist.size() != 0) {
                Rule rule = new Rule();
                rule.setDestination(ip);
                rule.setFilter(Filter.DROP);
                if (blacklist.contains(rule) && execute("sudo /sbin/iptables -t mangle -D POSTROUTING " + rule.toString())) {
                    blacklist.remove(rule);
                    logger.info(ip + " unblocked!");
                    return true;
                }
            }
        }
        return false;
    }

    /*
        renew rules by device
     */
    public void renRule(Device device){
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
        for(Rule rule: deviceRules.get(device).values()){
            i++;
            if(execute("sudo /sbin/iptables -R " + device.getIp() + " " + (i) + " " + rule.toString())){
                logger.info(rule + " renewed!");
            }
        }
    }

    /*
        delete rule by ip
     */
    public void delRule(Device device){
        //sudo iptables -D 192.168.8.95 -s 192.168.8.95/32 -j ACCEPT  # delete first appeared
        for(Rule rule: deviceRules.get(device).values()){
            if (execute("sudo /sbin/iptables -D " + device.getIp() + " " + rule.toString())) {
                logger.info(rule + " deleted!");
            }
        }
        deviceRules.put(device, new ConcurrentHashMap<Integer, Rule>());
    }

    /*
        add rule by device
     */
    public void addRule(Device device){
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
        if(deviceRules.containsKey(device)) {
            if(!deviceRules.get(device).isEmpty()){
                Map<Integer, Rule> rules = getRules(device);
                replaceRule(device, rules);
            } else {
                Map<Integer, Rule> rules = getRules(device);
                insertToList(device, rules);
            }
        } else if(!deviceRules.containsKey(device) && newDevice(device)){
            // -I FORWARD 1 = insert at the first
            if(execute("sudo /sbin/iptables -I FORWARD -j " + device.getIp())){
                logger.info("Chain [" + device.getIp() + "] created!");
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
            Map<Integer, Rule> rules = getRules(device);
            insertToList(device, rules);
        }
    }
    private void insertToList(Device device, Map<Integer, Rule> rules) {
        boolean success = true;
        for (Map.Entry<Integer,Rule> entry : rules.entrySet()){
            if(!execute("sudo /sbin/iptables -A " + device.getIp() + " " + entry.getValue().toString())){
                success = false;
            }
        }
        if (success){
            deviceRules.put(device, rules);
        }
    }
    private void replaceRule(Device device, Map<Integer, Rule> rules){
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

        if(deviceRules.get(device).size() == rules.size()){
            success = repRules(device, rules);
        } else {
            if(deviceRules.get(device).size() > rules.size()){
                if(!execute("sudo /sbin/iptables -D " + device.getIp() + " " + deviceRules.get(device).get(2).toString())){
                    return;
                }
                success = repRules(device, rules);
            } else {
                success = repRules(device, rules);
                if(!execute("sudo /sbin/iptables -I " + device.getIp() + " " + deviceRules.get(device).get(5).toString())){
                    return;
                }
            }
        }
        if (success) {
            deviceRules.put(device, rules);
        }
    }

    private boolean repRules(Device device, Map<Integer, Rule> rules) {
        int i = 0;
        boolean success = true;
        for (Rule entry : rules.values()){
            i++;
            if(!execute("sudo /sbin/iptables -R " + device.getIp() + " " + i + " " + entry.toString())){
                success = false;
            }
        }
        return success;
    }


    private Map<Integer, Rule> getRules(Device device){
        /*
            1-incoming
            2-outgoing with bw
            3-outgoing with qt
            4-outgoing unlimited
            5-outgoing drop
            6-incoming drop
         */

        Map<Integer, Rule> rules = new ConcurrentHashMap<>();
        rules.put(5, getRule(device.getIp(), Filter.DROP, true));
        rules.put(6, getRule(device.getIp(), Filter.DROP, false));

        String quota = null;
        String hashlimit = null;
        if (device.getBandwidth() != 0) {
            hashlimit = device.getBandwidth() + String.valueOf(device.getBandwidthBUnit()) + "/s";
            rules.put(2, getHashlimitRule(device.getIp(), Filter.DROP, true, hashlimit));
        }
        if(device.getQuota()!=0) {
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
        if(quota == null){
            rules.put(4, getRule(device.getIp(), Filter.ACCEPT,true));
        }

        // for download, it is not limited yet
        rules.put(1, getRule(device.getIp(), Filter.ACCEPT,false));
        return rules;
    }

    private boolean newDevice(Device device){
        if(execute("sudo /sbin/iptables -N " + device.getIp())){
            deviceRules.put(device, new ConcurrentHashMap<Integer, Rule>());
            return true;
        }
        return false;
    }

    /*
        get bytes from rules
     */
    public long[] extractBytes(Device device){
        List<String> lines=new ArrayList<String>();
        try{
            String exec="sudo /sbin/iptables -nvL "+device.getIp()+" | grep \""+device.getIp()+"\"";
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", exec);
            Process process = processBuilder.start();
            String line;
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = inputReader.readLine()) != null){
                lines.add(line);
            }

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null)
            {
                logger.error("Read iptables failed: "+line);
            }
            process.waitFor();
            inputReader.close();
            errorReader.close();
        } catch (Exception e){
            logger.error("Process get bytes from iptables failed: "+e.getMessage());
        }
        return convertUploadLinesToBytes(lines, device.getIp());
    }

    private long[] convertUploadLinesToBytes(List<String> lines, String deviceIp){
        // bytes[0] = 0 && bytes[1] = 0
        long[] bytes = new long[2];
        for(int i=1; i<lines.size();i++){
            /* first line is skipped:
              Chain 192.168.8.70 (1 references)
                   59  9845 ACCEPT     all  --  *      *       0.0.0.0/0            192.168.8.70
                   20 85343 DROP       all  --  *      *       192.168.8.70         0.0.0.0/0           bandwidth: 10 mb/s
                  113 10204 ACCEPT     all  --  *      *       192.168.8.70         0.0.0.0/0            quota: 10240 bytes limit: up to 1kb/s
                 1320 85343 DROP       all  --  *      *       192.168.8.70         0.0.0.0/0
                    0     0 DROP       all  --  *      *       0.0.0.0/0            192.168.8.70
             */
            String[] uploadLine = lines.get(i).split("0.0.0.0/0");
            if(!uploadLine[1].contains(deviceIp)){
                if(uploadLine[0].contains("ACCEPT")){
                    // the 2nd ACCEPT is read
                    bytes[0] = convertExtractBytes(uploadLine[0].split("ACCEPT")[0].replaceAll("(^\\s+|\\s+$)", "").split("\\s+")[1]);
                }
                else if(uploadLine[0].contains("DROP")){
                    // there are two upload drop, one for the bandwidth another for normal. In this case, the normal will replace the bw drop.
                    bytes[1] = convertExtractBytes(uploadLine[0].split("DROP")[0].replaceAll("(^\\s+|\\s+$)", "").split("\\s+")[1]);
                }
            }
        }
        return bytes;
    }

    private long convertExtractBytes(String bytes_value) {
        long bytes;
        switch (bytes_value.charAt(bytes_value.length()-1)){
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

    private String extractLastChar(String str) {
        return str.substring(0, str.length() - 1);
    }

    /*
        get rules by parameters
     */
    private Rule getRule(String ip, Filter filter, boolean source){
        Rule rule = new Rule();
        if(source){
            rule.setSource(ip);
        }
        else{
            rule.setDestination(ip);
        }
        /* ConnLimit limit the established connection, not new connection.
        if(filter.equals(Filter.ACCEPT))
            rule.setLimit("10"); // 10 packet per minute with 5 limit-burst as default
        */
        rule.setFilter(filter);
        return rule;
    }

    private Rule getQuotaRule(String ip, Filter filter, boolean source, String quota){
        Rule rule = getRule(ip, filter, source);
        if(quota != null)
            rule.setQuota(quota);
        return rule;
    }

    private Rule getHashlimitRule(String ip, Filter filter, boolean source, String hashlimit){
        Rule rule = getRule(ip, filter, source);
        if(hashlimit != null){
            String hashlimit_name;
            if(source)
                hashlimit_name="s";
            else
                hashlimit_name="d";
            hashlimit_name+=ip;
            rule.setHashlimitName(hashlimit_name);
            rule.setHashlimit(hashlimit);
        }
        return rule;
    }

    /*
        command task
     */
    private synchronized boolean execute(String task){
        try{
            Process p = Runtime.getRuntime().exec(task);
            p.waitFor();
            logger.info("Task "+task+" success");
        } catch (Exception e){
            logger.error("Task "+task+" fail");
            return false;
        }
        return true;
    }
}

