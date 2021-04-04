#!/bin/bash

rfkill unblock wifi

subnet="192.168.8.1/24"
# below helps us to find correctly two interfaces: eth and wlan
# shellcheck disable=SC2006
eth=`ip link | sed -e 's/.*\:\(.*\)\:/\1/' | grep -e ' ' | tr -s ' ' | cut -d ' ' -f 2 | grep -e '\be'`
# shellcheck disable=SC2006
wlan=`ip link | sed -e 's/.*\:\(.*\)\:/\1/' | grep -e ' ' | tr -s ' ' | cut -d ' ' -f 2 | grep -e '\bw'`

echo $eth >> interfaces.txt
echo $wlan >> interfaces.txt

# SETUP: /etc/network/interfaces
echo  "auto lo $eth $wlan
iface lo inet loopback
iface $eth inet dhcp
iface $wlan inet static
    address 192.168.8.1
    netmask 255.255.255.0
    broadcast 192.168.8.255" | tee --append /etc/network/interfaces

# SETUP: /etc/hostapd/hostapd.conf
echo "interface=$wlan
driver=nl80211
ssid=spyke
hw_mode=g
channel=7
beacon_int=500
wpa=2
wpa_passphrase=spyke2018
wpa_key_mgmt=WPA-PSK
rsn_pairwise=CCMP
# https://www.raspberrypi.org/forums/viewtopic.php?t=63045
ctrl_interface=/var/run/hostapd
ctrl_interface_group=0" > /etc/hostapd/hostapd.conf

echo "DAEMON_CONF=\"/etc/hostapd/hostapd.conf\"" | tee --append /etc/default/hostapd

# SETUP: /etc/dnsmasq.conf
echo "interface=$wlan
listen-address=192.168.8.1
dhcp-range=192.168.8.2,192.168.8.254,255.255.255.0,infinite
dhcp-option=3,192.168.8.1 # router
dhcp-option=6,127.0.0.1,1.1.1.1 # dns server
dhcp-script=/etc/spyke/docker/spyke/device" > /etc/dnsmasq.conf

# SETUP: forward ipv4
echo "net.ipv4.ip_forward=1" | tee --append /etc/sysctl.conf

# SETUP: /etc/dhcpcd.conf
echo "interface $wlan" | tee --append /etc/dhcpcd.conf
echo "static ip_address=$subnet" | tee --append /etc/dhcpcd.conf
echo "denyinterfaces $eth" | tee --append /etc/dhcpcd.conf
echo "denyinterfaces $wlan" | tee --append /etc/dhcpcd.conf

# required to make hostapd working
ip link set dev ${wlan} down
service hostapd start >> /etc/spyke/script_output.txt  # hostapd_cli all_sta -> check wifi connected devices
service dnsmasq start >> /etc/spyke/script_output.txt  # cat /var/lib/misc/dnsmasq.leases -> see connect devices

mv /etc/spyke/docker/spyke/iptables.ipv4.conf /etc/iptables.ipv4.conf

# restart interface
ip link set ${wlan} up
ip addr flush dev ${wlan}
ip addr add 192.168.8.1/24 dev ${wlan}

# tail -f /dev/null
#mvn spring-boot:run -Dagentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000
mvn spring-boot:run
