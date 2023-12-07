
#include "../ConnectionWorker.hpp"

SPEAR_BEG

void ConnectionWorker::Entry(
    int sk_service, FnArrive arrive, FnExit exit)
{
    sockaddr_in addrin;
    sockaddr& addr = *(sockaddr*)&addrin;
    uint addrsz = sizeof(addrin);
    int sk_client = accept(sk_service, &addr, &addrsz);
    RET(sk_client < 0);

    _sk_msg = sk_client;
    _loop = std::thread(
        [=]()
        {
            this->_Loop(arrive, exit);
        });
    _loop.detach();
}

// Leave:
//   - From Server to Client
// Arrive:
//   - From Client to Server
void ConnectionWorker::Leave(Buffer buf)
{
    RET(_sk_transport);
    RET(_sk_addr_len > 0);
    int s_rst = sendto(
        _sk_transport, buf.Pos(), buf.Len(), 0,
        (sockaddr*)&_sk_addr, _sk_addr_len);
    if (s_rst < 0)
    {
        LOG("Cannoot do sendto client.");
    }
}

void ConnectionWorker::_Loop(FnArrive arrive, FnExit exit)
{
    if (this->_HandShake() && this->_SetupTransport())
    {
        LOG("Client %d Enter Transport.", _port);
        while (_sk_transport > 0)
        {
            Buffer buf(BUFFER_MAX_SIZE);
            int r_len = recvfrom(
                _sk_transport, buf.Pos(), buf.Cap(), 0,
                (sockaddr*)&_sk_addr, &_sk_addr_len);
            if (r_len <= 0)
            {
                break;
            }
            buf.Set(r_len);
            arrive(buf);
        }
        LOG("Client %d Exit Transport.", _port);
    }
    this->_RetireTransport();
    exit();
}

bool ConnectionWorker::_HandShake()
{
    RET(_sk_msg <= 0, false);

    timeval tv = {};
    tv.tv_sec = CLIENT_CONNECTING_SEC;
    tv.tv_usec = 0;

    fd_set fdSet;
    FD_ZERO(&fdSet);
    FD_SET(_sk_msg, &fdSet);
    RET(select(_sk_msg + 1, &fdSet, 0, 0, &tv) <= 0, false);

    int r_len = recv(_sk_msg, &_token, sizeof(_token), 0);
    RET(r_len != sizeof(_token), false);
    RET(!_auth->IsTokenValid(_token), false);

    auto r_token = _auth->Reply(_token, _port);
    int s_len = send(_sk_msg, &r_token, sizeof(r_token), 0);
    RET(s_len != sizeof(r_token), false);
    return true;
}

bool ConnectionWorker::_SetupTransport()
{
    ef ef_transport = [this]() { this->_RetireTransport(); };

    int sk = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    RET(sk <= 0, false);
    _sk_transport = sk;

    sockaddr_in addrin;
    auto* addr = (sockaddr*)&addrin;
    uint addrsz = sizeof(addrin);
    bzero(&addrin, addrsz);
    addrin.sin_family = AF_INET;
    addrin.sin_addr.s_addr = htonl(INADDR_ANY);
    addrin.sin_port = htons(_port);
    RET(bind(sk, addr, addrsz) != 0, false);

    ef_transport.disable();
    return true;
}

void ConnectionWorker::_RetireTransport()
{
    if (_sk_transport > 0)
    {
        close(_sk_transport);
        _sk_transport = -1;
    }
}

SPEAR_END
