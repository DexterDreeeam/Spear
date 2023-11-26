#pragma once

#include "common.hpp"

SPEAR_BEG

static int create_socket()
{
    int sk = socket(AF_INET6, SOCK_DGRAM, 0);
    int flag = 1;
    setsockopt(sk, SOL_SOCKET, SO_REUSEADDR, &flag, sizeof(flag));
    flag = 0;
    setsockopt(sk, IPPROTO_IPV6, IPV6_V6ONLY, &flag, sizeof(flag));
    sockaddr_in6 addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin6_family = AF_INET6;
    addr.sin6_port = htons(22333);
    while (bind(sk, (sockaddr *)&addr, sizeof(addr))) {
        if (errno != EADDRINUSE) {
            return -1;
        }
        usleep(100000);
    }

    char packet[1024];
    socklen_t addrlen = sizeof(addr);

    printf("Waiting for being connected...\r\n");
    int n = recvfrom(sk, packet, sizeof(packet), 0, (sockaddr *)&addr, &addrlen);
    if (n <= 0) {
        printf("Exit...\r\n");
        return -1;
    }

    printf("New connect arrive.\r\n");
    connect(sk, (sockaddr *)&addr, addrlen);
    return sk;
}

SPEAR_END
