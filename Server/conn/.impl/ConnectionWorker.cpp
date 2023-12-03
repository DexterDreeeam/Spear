
#include "../ConnectionWorker.hpp"

SPEAR_BEG

void ConnectionWorker::Entry(ref<Self> self, int sk_service, FnExit exit)
{
    sockaddr_in addrin;
    sockaddr& addr = *(sockaddr*)&addrin;
    uint addrsz = sizeof(addrin);
    int sk_client = accept(sk_service, &addr, &addrsz);
    if (sk_client < 0)
    {
        ERR("sk_client < 0");
        return;
    }
    self->_sk_client = sk_client;
    self->_loop = std::thread(
        [=]()
        {
            self->_Loop(exit);
        });
    self->_loop.detach();
}

void ConnectionWorker::_Loop(FnExit exit)
{
    if (this->_HandShake())
    {
        while (true)
        {
            sleep_ms(20);
        }
    }
    exit();
}

bool ConnectionWorker::_HandShake()
{
    timeval tv = {};
    tv.tv_sec = CLIENT_CONNECTING_SEC;
    tv.tv_usec = 0;

    fd_set fdSet;
    FD_ZERO(&fdSet);
    FD_SET(_sk_client, &fdSet);

    if (select(_sk_client + 1, &fdSet, 0, 0, &tv) <= 0)
    {
        ERR("elect(...) <= 0");
        return false;
    }

    int r_len = recv(_sk_client, &_token, sizeof(_token), 0);
    if (r_len != sizeof(_token))
    {
        ERR("r_len != sizeof(_token)");
        return false;
    }

    if (!_auth->IsTokenValid(_token))
    {
        ERR("!_auth->IsTokenValid(_token)");
        return false;
    }

    auto r_token = _auth->Reply(_token, _port);
    int s_len = send(_sk_client, &r_token, sizeof(r_token), 0);
    if (s_len != sizeof(r_token))
    {
        ERR("s_len != sizeof(r_token)");
        return false;
    }

    return true;
}

SPEAR_END
