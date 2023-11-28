#pragma once

// STD
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

#include <string>
#include <iostream>
#include <memory>

// Linux
#include <net/if.h>
#include <linux/if_tun.h>

// Spear
using u8  = std::uint8_t;
using s8  = std::int8_t;
using u16 = std::uint16_t;
using s16 = std::int16_t;
using u32 = std::uint32_t;
using s32 = std::int32_t;
using u64 = std::uint64_t;
using s64 = std::int64_t;
template<typename T> using ref = std::shared_ptr<T>;

#define SPEAR_BEG  namespace spear {
#define SPEAR_END  }
#define CM spear::ConnectionManager::Ins()
