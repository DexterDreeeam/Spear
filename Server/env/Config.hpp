#pragma once

#include "common.hpp"

SPEAR_BEG

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
        dns(),
        max_connection(256),
        transport_port_from(22334)
    {}

    Config(int argc, char** argv) :
        tun(),
        address(),
        port(),
        dns(),
        max_connection(256),
        transport_port_from(22334)
    {
        int i = 1;
        while (i < argc)
        {
            const char* cstr = argv[i];
            switch (i)
            {
            case 1:
                tun = cstr;
                break;
            case 2:
                address = cstr;
                break;
            case 3:
                port = cstr;
                break;
            case 4:
                dns = cstr;
                break;
            default:
                break;
            }
            ++i;
        }
    }

    bool Empty() const
    {
        return
            tun.length() &&
            address.length() &&
            port.length();
    }
};

SPEAR_END
