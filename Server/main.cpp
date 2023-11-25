#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include <fcntl.h>

#include <net/if.h>
#include <linux/if_tun.h>

static int get_interface()
{
    int interface = open("/dev/net/tun", O_RDWR | O_NONBLOCK);
    ifreq ifr;
    memset(&ifr, 0, sizeof(ifr));
    ifr.ifr_flags = IFF_TUN | IFF_NO_PI;
    strncpy(ifr.ifr_name, "Spear-Tunnel", sizeof(ifr.ifr_name));
    if (ioctl(interface, TUNSETIFF, &ifr)) {
        perror("Cannot get TUN interface\r\n");
        exit(1);
    }
    return interface;
}

static int get_tunnel()
{
    // We use an IPv6 socket to cover both IPv4 and IPv6.
    int tunnel = socket(AF_INET6, SOCK_DGRAM, 0);
    int flag = 1;
    setsockopt(tunnel, SOL_SOCKET, SO_REUSEADDR, &flag, sizeof(flag));
    flag = 0;
    setsockopt(tunnel, IPPROTO_IPV6, IPV6_V6ONLY, &flag, sizeof(flag));
    // Accept packets received on any local address.
    sockaddr_in6 addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin6_family = AF_INET6;
    addr.sin6_port = htons(22333);
    while (bind(tunnel, (sockaddr *)&addr, sizeof(addr))) {
        if (errno != EADDRINUSE) {
            return -1;
        }
        usleep(100000);
    }

    char packet[1024];
    socklen_t addrlen = sizeof(addr);

    printf("Waiting for being connected...\r\n");
    int n = recvfrom(
        tunnel, packet, sizeof(packet), 0, (sockaddr *)&addr, &addrlen);
    if (n <= 0) {
        return -1;
    }

    printf("New connect arrive.\r\n");
    // Connect to the client as we only handle one client at a time.
    connect(tunnel, (sockaddr *)&addr, addrlen);
    return tunnel;
}

//-----------------------------------------------------------------------------
int main(int argc, char** argv)
{
    int interface = get_interface();
    int tunnel = get_tunnel();
    if (tunnel != -1) {
        printf("Here comes a new tunnel\r\n");
        fcntl(tunnel, F_SETFL, O_NONBLOCK);
        char packet[32767];
        bool idle_last = true;
        while (true) {
            bool idle = true;
            int length = read(interface, packet, sizeof(packet));
            if (length > 0) {
                send(tunnel, packet, length, MSG_NOSIGNAL);
                idle = false;
            }

            length = recv(tunnel, packet, sizeof(packet), 0);
            if (length == 0) {
                break;
            }
            if (length > 0) {
                if (packet[0] != 0) {
                    write(interface, packet, length);
                }
                idle = false;
            }

            if (idle) {
                usleep(100000);
            }
            if (idle_last != idle) {
                idle_last = idle;
                printf("The tunnel is %s now.\r\n", idle ? "idle" : "busy");
            }
        }
        printf("The tunnel is broken\r\n");
        close(tunnel);
    }
    perror("Cannot create tunnels");
    exit(1);
}
