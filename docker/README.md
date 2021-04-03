
## Docker environment

## ❗❗❗Important Notes❗❗❗

Be warned that the docker will change *iptables* configuration of you machine and also changes the *network interfaces* of the machine.

The easy way is to restart the machine if you want to use default **iptables rules** and **network interfaces configurations**.

## 🔧 Set up

1. **Install components.**

* Docker
* Docker-compose

## 🚀 Quick start

1. **Start Project.**

### To start, execute this:
```shell
docker-compose up -d --no-deps --build
```

### For no cache, execute this:
```shell
docker-compose build --no-cache
docker-compose up -d --force-recreate
```

### Go inside docker
```shell
docker exec -it spyke /bin/bash
```

## 💻 Run it

1. **Client**

* HTTPS page       - localhost:80
* HTTPs page       - localhost:443

1. **Server**

* DNS             - localhost:53
* DHCP            - localhost:67

## 🧹 Clean up

1. **Quick remove**
```shell
docker stop $(docker ps -a -q) && \
docker rm $(docker ps -a -q) && \
docker rmi $(docker images -q -f dangling=true)
```

1. **Check docker and images.**
```shell
docker ps -a
docekr images -a
```

1. **Kill docker and rm image.**
```shell
docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)
```

1. **Remove dangling.**
```shell
docker rmi $(docker images -q -f dangling=true)
```

1. **Remove all images.**
```shell
docker image prune -a
```

## 💡 Other information

1. **Client and Server dockerfile**
The docker environment contains containers for [server](https://github.com/surething-project/spyke/blob/master/core/docker/server) and for [client](https://github.com/surething-project/spyke/blob/master/core/docker/server) which are not being used.
This would be the future work for integration test purpose.
