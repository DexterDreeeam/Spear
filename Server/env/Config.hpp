#pragma once

#include "common.hpp"

SPEAR_BEG

static const std::string default_dns = "8.8.8.8";
static const int max_conn_default = 256;
static const int port_from_default = 22334;


struct Config
{
    std::string tun;             // -t
    std::string address;         // -a
    std::string port;            // -p
    std::string dns;             // -d
    int transport_port_from;     // -f
    int max_connection;          // -m

    Config() :
        tun(),
        address(),
        port(),
        dns(default_dns),
        transport_port_from(port_from_default),
        max_connection(max_conn_default)
    {}

    Config(int argc, char** argv) :
        tun(),
        address(),
        port(),
        dns(default_dns),
        transport_port_from(port_from_default),
        max_connection(max_conn_default)
    {
        int i = 0;
        while (++i < argc)
        {
            std::string p = argv[i];
            std::string s = i + 1 < argc ? argv[i + 1] : "";
            if (p == "-t")
            {
                tun = s;
                ++i;
            }
            else if (p == "-a")
            {
                address = s;
                ++i;
            }
            else if (p == "-p")
            {
                port = s;
                ++i;
            }
            else if (p == "-d")
            {
                dns = s;
                ++i;
            }
            else if (p == "-f")
            {
                transport_port_from = atoi(s.c_str());
                ++i;
            }
            else if (p == "-m")
            {
                max_connection = atoi(s.c_str());
                ++i;
            }
        }
    }

    bool Empty() const
    {
        return !(
            tun.length() &&
            address.length() &&
            port.length()
        );
    }
};

SPEAR_END
