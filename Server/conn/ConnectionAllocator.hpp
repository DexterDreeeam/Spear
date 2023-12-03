#pragma once

#include "../auth/_headers.hpp"
#include "../env/_headers.hpp"
#include "ConnectionWorker.hpp"

SPEAR_BEG

class ConnectionAllocator
{
public:
    enum State
    {
        Idle, Connecting, Connected, Disconnecting,
    };

    using Worker = ref<ConnectionWorker>;

private:
    static const int   MaxConnections = 256;
    std::mutex         _mtx;
    Worker             _workers[MaxConnections];
    State              _states[MaxConnections];

public:
    ConnectionAllocator();
    ~ConnectionAllocator() = default;

    bool Setup(ref<TokenAuthenticator> auth, int count, int port_from);
    Worker AcquireWorker();
    void ReleaseWorker(Worker worker);

private:
    auto _ScopeLock() { return std::lock_guard<std::mutex>(_mtx); }
    std::pair<int, Worker> _SearchIdleWorker();
    bool _ResetWorker(int w);
};

SPEAR_END
