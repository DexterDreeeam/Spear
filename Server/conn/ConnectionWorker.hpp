#pragma once

#include "../auth/_headers.hpp"
#include "../env/_headers.hpp"
#include "Buffer.hpp"

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

    int                       _id;
    int                       _port;
    int                       _sk_msg;
    int                       _sk_transport;
    sockaddr_in               _sk_addr;
    socklen_t                 _sk_addr_len;
    Token                     _token;
    ref<TokenAuthenticator>   _auth;
    std::mutex                _mtx;
    std::thread               _loop;

public:
    static void Entry(ref<Self> self, int sk_service, FnArrive arrive, FnExit exit);

    ConnectionWorker(ref<TokenAuthenticator> auth, int id, int port) :
        _id(id),
        _port(port),
        _sk_msg(-1),
        _sk_transport(-1),
        _sk_addr(),
        _sk_addr_len(0),
        _token(),
        _auth(auth),
        _mtx(),
        _loop()
    {}

    ~ConnectionWorker()
    {
        this->Reset();
    }

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
    }

    // Leave:
    //   - From Server to Client
    // Arrive:
    //   - From Client to Server
    void Leave(Buffer buf);

private:
    auto _ScopeLock() { return std::lock_guard<std::mutex>(_mtx); }
    void _Loop(FnArrive arrive, FnExit exit);
    bool _HandShake();
    bool _SetupTransport();
    void _RetireTransport();
};

SPEAR_END
