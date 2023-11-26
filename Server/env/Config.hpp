#pragma once

#include "common.hpp"

SPEAR_BEG

struct Config
{
    string tun;
    string address;
    string dns;

    Config() :
        tun(),
        address(),
        dns()
    {}

    Config(int argc, char** argv) :
        tun(),
        address(),
        dns()
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
                dns = cstr;
                break;
            default:
                break;
            }
            ++i;
        }
    }
};

SPEAR_END
