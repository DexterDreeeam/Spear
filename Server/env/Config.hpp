#pragma once

#include "common.hpp"

SPEAR_BEG

static const std::string default_dns = "8.8.8.8";
static const int max_conn_default = 256;
static const int port_from_default = 22334;


struct Config
{
    std::string tun;
    std::string address;
    std::string port;
    std::string dns;

    int max_connection;
    int transport_port_from;

    Config() :
        tun(),
        address(),
        port(),
        dns(default_dns),
        max_connection(max_conn_default),
        transport_port_from(port_from_default)
    {}

    Config(int argc, char** argv) :
        tun(),
        address(),
        port(),
        dns(default_dns),
        max_connection(max_conn_default),
        transport_port_from(port_from_default)
    {
        int i = 0;
        while (++i < argc)
        {
            std::string a = argv[i];
            if (a == "" || a == "*" || a == "/")
            {
                continue;
            }
            switch (i)
            {
            case 1:
                tun = a;
                break;
            case 2:
                address = a;
                break;
            case 3:
                port = a;
                break;
            case 4:
                dns = a;
                break;
            case 5:
                max_connection = atoi(a.c_str());
                break;
            case 6:
                transport_port_from = atoi(a.c_str());
                break;
            default:
                break;
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
