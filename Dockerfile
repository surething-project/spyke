FROM debian:stretch

RUN echo 'deb-src  http://deb.debian.org/debian  stretch main' >> /etc/apt/sources.list

RUN apt-get update -y

# seperating this to seperate error
RUN apt-get install -y openjdk-8-jdk

RUN apt-get install -y maven
RUN apt-get install -y iptables
RUN apt-get install -y hostapd
RUN apt-get install -y dnsmasq
RUN apt-get install -y libpcap-dev
RUN apt-get install -y net-tools
RUN apt-get install -y sqlite3
RUN apt-get install -y rfkill
RUN apt-get install -y vim

# for iptables update
RUN apt-get -y install sudo

WORKDIR /etc/spyke

COPY . .

RUN mvn package -DskipTests=true

#ENTRYPOINT ["tail", "-f", "/dev/null"]
ENTRYPOINT ["/bin/sh", "-c", "/etc/spyke/docker/spyke/startup.sh"]
