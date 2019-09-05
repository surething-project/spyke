# SPYKE  
Security ProxY with Knowledge-based intrusion prEvention, a network intermediary that stands between IoT devices and the Internet, that provides visibility to which communications are taking place between devices and remote servers; and it also has the ability block and limit connection.
The evaluation results are presented in the folder experiments.  
The System is integrated into a Raspberry pi 3 B+.  

## OS and version  
Raspbian GNU/Linux 9.6 (stretch)

## dependencies  
+hostapd  
+dnsmasq  
+oracle-java8-jdk  
+maven  
+libpcap-dev (pcap4j)  
+sqlite3  
+crontab  (already installed)
+iptables  (already installed)

## setup  
Before installing the system we need to install and configure the dependencies mentioned above.  
Note, you should not modify neither delete files under folder script. At least you know what you are doing where those files.

### configure dnsmasq
Dnsmasq is a daemon that provides DNS server and DHCP server.  
After installing it we need to configure the DHCP daemon configuration file in order to define the ip address.  
Moreover, we need to configure the dnsmasq configuration file to define characteristics of DHCP server.  

#### /etc/dhcpd.conf  
(Add following lines at end of the file)  
interface wlan0   
static ip_address=192.168.8.1/24  
denyinterfaces eth0  
denyinterfaces wlan0  
#### /etc/dnsmasq.conf  
(Replace file with following settings, you can backup original one)  
interface=wlan0  
listen-address=192.168.8.1  
dhcp-range=192.168.8.2,192.168.8.255,255.255.255.0,infinite  
dhcp-option=3,192.168.8.1  # router  
dhcp-option=6,192.168.1.254,8.8.8.8  # dns server  
dhcp-script=/<path>/spyke/script/device  

### configure hostapd  
Hostapd is a daemon that provide host access point.  
After installing it we need to configure the hostapd configuration file in order to define characteristics of Wi-Fi.  
Then configure hostapd using the configuration file just mentioned.  

#### /etc/hostapd/hostapd.conf  
interface=wlan0  
driver=nl80211  
ssid=spyke  
hw_mode=g  
channel=7  
beacon_int=500  
wpa=2  
wpa_passphrase=spyke2018  
wpa_key_mgmt=WPA-PSK  
rsn_pairwise=CCMP  
#### /etc/default/hostapd
(Replace #DAEMON_CONF="")  
DAEMON_CONF="/etc/hostapd/hostapd.conf"  

### Packet forward
To be able to forward packets, it is needed to set ip forward to 1 in the sysctl configuration file.  

#### /etc/sysctl.conf  
(Replace #net.ipv4.ip_forward=1)  
net.ipv4.ip_forward=1    

## spyke commands
After installing the dependencies and configuring the system based on the setup above, you can run the following command to install and run the system.  
$ sudo ./spyke -i

For more options just run:  
$ sudo ./spyke  
or  
$ sudo ./spyke -h  
