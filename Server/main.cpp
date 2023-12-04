
#include "env/_headers.hpp"
#include "conn/_headers.hpp"

int main(int argc, char** argv)
{
    // int test = spear::build_tunnel("tun0");
    // LOG("%d", test);
    auto config = spear::Config(argc, argv);
    spear::ConnectionManager::Setup(config);
    if (!CM)
    {
        ERR("Cannot create ConnectionManager.");
        exit(1);
    }

    CM->Run();
    CM->WaitComplete();

    /*
    int sk = spear::create_socket();
    if (sk <= 0)
    {
        ERR("Cannot create socket");
        exit(1);
    }

    LOG("Here comes a new client");
    fcntl(sk, F_SETFL, O_NONBLOCK);
    char packet[32767];
    bool idle_last = true;
    while (true) {
        bool idle = true;
        int length = read(interface, packet, sizeof(packet));
        if (length > 0) {
            send(sk, packet, length, MSG_NOSIGNAL);
            LOG("Packet received");
            idle = false;
        }

        length = recv(sk, packet, sizeof(packet), 0);
        if (length == 0) {
            break;
        }
        if (length > 0) {
            write(interface, packet, length);
            LOG("Packet sent");
            idle = false;
        }

        if (idle) {
            usleep(100000);
        }
        if (idle_last != idle) {
            idle_last = idle;
            LOG("The sk is %s now", idle ? "idle" : "busy");
        }
    }
    ERR("The sk is broken");
    close(sk);*/
}
