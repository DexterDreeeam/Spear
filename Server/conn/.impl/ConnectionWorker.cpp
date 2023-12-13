
#include "../ConnectionWorker.hpp"

SPEAR_BEG

void ConnectionWorker::Entry(
    int sk_service, FnArrive arrive, FnExit exit)
{
    sockaddr_in addrin;
    sockaddr& addr = *(sockaddr*)&addrin;
    uint addrsz = sizeof(addrin);
    _sk_msg = accept(sk_service, &addr, &addrsz);
    RET(_sk_msg < 0);

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
    RET(_sk_transport <= 0);
    RET(_sk_addr_len <= 0);
    int s_rst = sendto(
        _sk_transport, buf.Ptr(), buf.Len(), 0,
        (sockaddr*)&_sk_addr, _sk_addr_len);
    RET(s_rst < 0);
}

void ConnectionWorker::_Loop(FnArrive arrive, FnExit exit)
{
    RET(!this->_SetupInformation());
    RET(!this->_HandShake());
    RET(!this->_SetupTransport());

    LOG("Client %d Enter Transport.", _port);

    std::atomic_bool isConnecting = true;
    std::thread t = std::thread(
        [=, &isConnecting]()
        {
            while (isConnecting)
            {
                Buffer buf(BUFFER_MAX_SIZE);
                int r_len = recvfrom(
                    _sk_transport, buf.Ptr(), buf.Cap(), 0,
                    (sockaddr*)&_sk_addr, &_sk_addr_len);
                if (r_len <= 0)
                {
                    if (errno == EAGAIN || errno == EWOULDBLOCK)
                    {
                        // timeout
                        continue;
                    }
                    else
                    {
                        break;
                    }
                }
                buf.Set(r_len);
                arrive(buf);
            }
        });

    while (true)
    {
        char msg[128];
        int r_len = recv(_sk_msg, msg, sizeof(msg), 0);
        if (r_len <= 0)
        {
            // connection closed
            isConnecting = false;
            t.join();
            break;
        }
    }

    LOG("Client %d Exit Transport.", _port);
    this->_RetireTransport();
    exit();
}

bool ConnectionWorker::_SetupInformation()
{
    _information["VpnTunAddr"]    = this->_ClientSourceIp();
    _information["VpnDns"]        = _config.dns;
    _information["TransportPort"] = std::to_string(_port);
    RET(!_information.IsValid(), false);
    return true;
}

bool ConnectionWorker::_SetupTransport()
{
    ef ef_transport = [this]() { this->_RetireTransport(); };

    timeval tv = {};
    tv.tv_sec = 5;
    tv.tv_usec = 0;
    _sk_transport = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    LOG("SET _sk_transport: %d", _sk_transport);
    RET(_sk_transport <= 0, false);
    RET(setsockopt(_sk_transport, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv)) < 0, false);

    sockaddr_in addrin;
    auto* addr = (sockaddr*)&addrin;
    uint addrsz = sizeof(addrin);
    bzero(&addrin, addrsz);
    addrin.sin_family = AF_INET;
    addrin.sin_addr.s_addr = htonl(INADDR_ANY);
    addrin.sin_port = htons(_port);
    RET(bind(_sk_transport, addr, addrsz) != 0, false);

    ef_transport.disable();
    return true;
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
    RET(r_len <= 0, false)
    RET(r_len != sizeof(_token), false);
    RET(!_auth->IsTokenValid(_token), false);

    // auto r_token = _auth->Reply(_token, _port);
    // int s_len = send(_sk_msg, &r_token, sizeof(r_token), 0);
    // RET(s_len != sizeof(r_token), false);

    auto infoStr = _information.ToString();
    LOG("Client-%d ShakeHand Info %s", _id, infoStr.c_str());
    int i_len = send(_sk_msg, infoStr.c_str(), infoStr.size(), 0);
    RET(i_len != (int)infoStr.size(), false);

    return true;
}

void ConnectionWorker::_RetireTransport()
{
    if (_sk_transport > 0)
    {
        close(_sk_transport);
        _sk_transport = -1;
    }
    if (_sk_msg > 0)
    {
        close(_sk_msg);
        _sk_msg = -1;
    }
}

std::string ConnectionWorker::_ClientSourceIp()
{
    auto addr = _config.address;
    int ips[4] = {};
    size_t ofst = 0;
    for (int i = 0; i < 2; ++i)
    {
        size_t pos = addr.find('.', ofst);
        if (pos == std::string::npos)
        {
            break;
        }
        ips[i] = atoi(addr.substr(ofst, pos - ofst).c_str());
        ofst = pos + 1;
    }
    ips[2] = (_id >> 8) & 0xff;
    ips[3] = (_id >> 0) & 0xff;

    std::string rst = "";
    rst += std::to_string(ips[0]) + '.';
    rst += std::to_string(ips[1]) + '.';
    rst += std::to_string(ips[2]) + '.';
    rst += std::to_string(ips[3]);
    return rst;
}

SPEAR_END
