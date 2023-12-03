#pragma once

#include "../auth/_headers.hpp"
#include "../env/_headers.hpp"

SPEAR_BEG

class ConnectionWorker
{
    using Self = ConnectionWorker;
    using FnExit = function<void()>;

    static const int CLIENT_CONNECTING_SEC = 5;

    int                     _id;
    int                     _port;
    int                     _sk_client;
    Token                   _token;
    ref<TokenAuthenticator> _auth;
    std::mutex              _mtx;
    std::thread             _loop;

public:
    static void Entry(ref<Self> self, int sk_service, FnExit exit);

    ConnectionWorker(ref<TokenAuthenticator> auth, int id, int port) :
        _id(id),
        _port(port),
        _sk_client(-1),
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
        if (_sk_client > 0)
        {
            close(_sk_client);
            _sk_client = -1;
        }
    }

private:
    auto _ScopeLock() { return std::lock_guard<std::mutex>(_mtx); }
    void _Loop(FnExit exit);
    bool _HandShake();
};

SPEAR_END
