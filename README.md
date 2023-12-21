<img src="https://raw.githubusercontent.com/DexterDreeeam/Spear/main/Android/icon.png" width="20%" height="20%">

# Spear
- VPN Proxy
- Simplest
- Fast
- Total Free
- Open Source

# Platforms
### _Server_
- Ubuntu

### _Client_
- Android
- IOS - Developing
- Harmony - Wait for OS Function Release

# Client Proxy Token
Client side Proxy Token to Connect your server.  
Authentication system is under development, so pleases note there is no security for auth yet.  
Your Proxy Token should be `{IP}:22333`.  
e.g. if your server ip is `10.87.225.96`, your token is `10.87.225.96:22333`.


# Guide - Setup Ubuntu Server
- Setup Network Firewall `22333-22589` Ports inbound TCP & UDP open
- Download `_deploy.sh` and `spear_server` in [Latest Release](https://github.com/DexterDreeeam/Spear/releases/latest)
- Make sure they are in same folder on your Ubuntu Server
- Run `_deploy.sh` using Root Privilege
  
Done! If you need customize configuration, refer to [Config Ubuntu Server](https://github.com/DexterDreeeam/Spear/tree/main?tab=readme-ov-file#advance---config-ubuntu-server)  
If you need to **Re-Deploy** server, please kill old process mannually firstly.  
`ps aux | spear` to get spear process and `kill -9 {Spear-Server-Process-Id}`

# Guide - Install Android Client
- Download `spear.apk` in [Latest Release](https://github.com/DexterDreeeam/Spear/releases/latest)
- Install `spear.apk` in your Android Device
- Add Android Widget to quickly toggle VPN

https://github.com/DexterDreeeam/Spear/assets/43837899/cb5dda8c-2558-4ccf-831e-5237a6c2bc61



# Guide - Install iOS Client
- TBD

# Advance - Config Ubuntu Server
In `_deploy.sh` there are several server configurable parameters.  
`-p 22333 -d 8.8.8.8 -f 22334 -m 256` in `_deploy.sh`  

- `-p 22333` is server communication port with all clients
- `-d 8.8.8.8` is VPN DNS server ip
- `-f 22334` is packet transport port range start
- `-m 256` is maximum support client number

Clients will connect server port range is `22334` to `22334 + 256 - 1 = 22589`, so Server's ports in this scope need to open. 


