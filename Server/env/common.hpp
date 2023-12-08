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
#include <functional>
#include <chrono>
#include <thread>
#include <atomic>

#include <mutex>
#include <set>
#include <map>
#include <vector>

// Linux
#include <net/if.h>
#include <linux/if_tun.h>

// Spear Def
#define SPEAR_BEG  namespace spear {
#define SPEAR_END  }
#define CM spear::ConnectionManager::Ins()

// Spear
SPEAR_BEG

using u8  = std::uint8_t;
using s8  = std::int8_t;
using u16 = std::uint16_t;
using s16 = std::int16_t;
using u32 = std::uint32_t;
using s32 = std::int32_t;
using u64 = std::uint64_t;
using s64 = std::int64_t;
template<typename T> using ref = std::shared_ptr<T>;
template<typename T> using function = std::function<T>;

template<typename T, typename... Ts>
inline auto make_ref(Ts&&... args) -> ref<T>
{
    return std::make_shared<T>(args...);
}

inline void sleep_ms(int ms)
{
    std::this_thread::sleep_for(std::chrono::milliseconds(ms));
}

#define ERR(x, ...) \
    printf("[ERROR] %s: " x " Failed.\r\n", __FUNCTION__, ##__VA_ARGS__)

#define LOG(x, ...) \
    printf("%s: " x "\r\n", __FUNCTION__, ##__VA_ARGS__)

#define RET(x, ...) \
    if (x) { \
        ERR(#x); \
        return __VA_ARGS__; \
    }

// Configuration
static const int BUFFER_MAX_SIZE = 1 << 16;

SPEAR_END
