
#include "env/headers_env.hpp"
#include "ConnectionManager.hpp"

int main(int argc, char** argv)
{
    auto config = spear::Config(argc, argv);
    spear::ConnectionManager::Init(config);
    if (!CM)
    {
        perror("Cannot create ConnectionManager.");
        exit(1);
    }

    int interface = spear::build_tunnel("tun0");
    if (interface <= 0)
    {
        perror("Cannot create interface");
        exit(1);
    }
    int sk = spear::create_socket();
    if (sk <= 0)
    {
        perror("Cannot create socket");
        exit(1);
    }

    printf("Here comes a new client\r\n");
    fcntl(sk, F_SETFL, O_NONBLOCK);
    char packet[32767];
    bool idle_last = true;
    while (true) {
        bool idle = true;
        int length = read(interface, packet, sizeof(packet));
        if (length > 0) {
            send(sk, packet, length, MSG_NOSIGNAL);
            printf("Packet received.\r\n");
            idle = false;
        }

        length = recv(sk, packet, sizeof(packet), 0);
        if (length == 0) {
            break;
        }
        if (length > 0) {
            write(interface, packet, length);
            printf("Packet sent.\r\n");
            idle = false;
        }

        if (idle) {
            usleep(100000);
        }
        if (idle_last != idle) {
            idle_last = idle;
            printf("The sk is %s now.\r\n", idle ? "idle" : "busy");
        }
    }
    printf("The sk is broken\r\n");
    close(sk);
}
