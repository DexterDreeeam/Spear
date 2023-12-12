#pragma once

#include "../auth/_headers.hpp"
#include "../env/_headers.hpp"
#include "Buffer.hpp"
#include "ConnectionInformation.hpp"

SPEAR_BEG

// # Explain
//
//
//             Arrive              Outgoing
//           --------->          ----------->
//   Client              Server                Tunnel
//           <---------          <-----------
//              Leave              Incomming
//

class ConnectionWorker
{
    using Self = ConnectionWorker;
    using FnExit = function<void()>;
    using FnArrive = function<void(Buffer)>;

    static const int CLIENT_CONNECTING_SEC = 5;

    const Config&             _config;
    int                       _id;
    int                       _port;
    int                       _sk_msg;
    int                       _sk_transport;
    sockaddr_in               _sk_addr;
    socklen_t                 _sk_addr_len;
    Token                     _token;
    ConnectionInformation     _information;
    ref<TokenAuthenticator>   _auth;
    std::mutex                _mtx;
    std::thread               _loop;
    std::atomic<bool>         _occupied;
    ref<ef>                   _escape;

public:
    static void Entry(ref<Self> self, int sk_service, FnArrive arrive, FnExit exit);

    ConnectionWorker(
        const Config& config,
        ref<TokenAuthenticator> auth,
        int id,
        int port) :
        _config(config),
        _id(id),
        _port(port),
        _sk_msg(-1),
        _sk_transport(-1),
        _sk_addr(),
        _sk_addr_len(0),
        _token(),
        _information(),
        _auth(auth),
        _mtx(),
        _loop(),
        _occupied(false),
        _escape(nullptr)
    {}

    ~ConnectionWorker()
    {
        this->Reset();
    }

    void Escape(ref<ef> escape) { _escape = escape; }

    bool Occupy()
    {
        bool expected = false;
        return _occupied.compare_exchange_strong(
            expected,
            true,
            std::memory_order_release,
            std::memory_order_relaxed);
    }

    void Idle() { _occupied = false; }

    void Entry(int sk_service, FnArrive arrive, FnExit exit);

    int Id() const { return _id; }

    int Port() const { return _port; }

    void Reset()
    {
        this->_RetireTransport();
        if (_sk_msg > 0)
        {
            close(_sk_msg);
            _sk_msg = -1;
        }
        _escape = nullptr;
    }

    // Leave:
    //   - From Server to Client
    // Arrive:
    //   - From Client to Server
    void Leave(Buffer buf);

private:
    auto _ScopeLock() { return std::lock_guard<std::mutex>(_mtx); }
    void _Loop(FnArrive arrive, FnExit exit);
    bool _SetupInformation();
    bool _SetupTransport();
    bool _HandShake();
    void _RetireTransport();
    std::string _ClientSourceIp();
};

SPEAR_END
