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

using namespace std;

// Linux
#include <net/if.h>
#include <linux/if_tun.h>

// Spear
#define SPEAR_BEG  namespace spear {
#define SPEAR_END  }

#define CM spear::ConnectionManager::Ins()
