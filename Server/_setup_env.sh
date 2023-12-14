#!/bin/bash

if [[ "$EUID" -ne 0 ]]; then
    echo "Run with root privileges."
    exit 1
fi

if ip link show tunSpear > /dev/null 2>&1; then
    echo "tunSpear exists. Deleting..."
    sudo ip link delete tunSpear
fi

## setup network
echo 1 > /proc/sys/net/ipv4/ip_forward
iptables -t nat -A POSTROUTING -s 10.0.0.0/8 -o eth0 -j MASQUERADE
ip tuntap add dev tunSpear mode tun
ifconfig tunSpear 10.233.0.1 up
route add -net 10.233.0.0 netmask 255.255.0.0 dev tunSpear
exit 0