#pragma once

#include "common.hpp"

SPEAR_BEG

inline int build_tunnel(const std::string& tunName)
{
    int tun = open("/dev/net/tun", O_RDWR | O_NONBLOCK);
    ifreq ifr = {};
    ifr.ifr_flags = IFF_TUN | IFF_NO_PI;
    strncpy(ifr.ifr_name, tunName.c_str(), sizeof(ifr.ifr_name));
    int ctl = ioctl(tun, TUNSETIFF, &ifr);
    LOG("tun-[%d]: ioctl(tun, TUNSETIFF, &ifr) >> %d", tun, ctl);
    RET(ctl, -1)
    return tun;
}

inline void destroy_tunnel(int tunnel)
{
    if (tunnel > 0)
    {
        // close(tunnel);
    }
}

SPEAR_END
