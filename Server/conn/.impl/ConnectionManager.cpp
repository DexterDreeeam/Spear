#include "../ConnectionManager.hpp"

SPEAR_BEG

void ConnectionManager::Setup(const Config& config)
{
    RET(ConnectionManager::Ins());

    auto* p = new ConnectionManager(config);
    auto ins = ref<ConnectionManager>(p);
    RET(!ins || !ins->_Init(config));

    ConnectionManager::Ins(ins);
}

bool ConnectionManager::_Init(const Config& config)
{
    RET(_config.Empty(), false);

    RET(!_InitTunnel(), false);
    ef ef_tunnel = [this]() { this->_UninitTunnel(); };

    RET(!_InitService(), false);
    ef ef_service = [this]() { this->_UninitService(); };

    this->_auth = make_ref<TokenAuthenticator>();
    this->_allocator = make_ref<ConnectionAllocator>(_config);
    RET(!this->_allocator->Setup(
            _auth,
            config.max_connection,
            config.transport_port_from),
        false);

    ef_tunnel.disable();
    ef_service.disable();
    return true;
}

bool ConnectionManager::_InitTunnel()
{
    int tunnel = build_tunnel(_config.tun);
    RET(tunnel <= 0, false);
    _tunnel = tunnel;
    return true;
}

void ConnectionManager::_UninitTunnel()
{
    if (_tunnel)
    {
        destroy_tunnel(_tunnel);
        _tunnel = -1;
    }
}

bool ConnectionManager::_InitService()
{
    _sk_service = socket(AF_INET, SOCK_STREAM, 0);
    RET(_sk_service <= 0, false);

    sockaddr_in addrin;
    auto* addr = (sockaddr*)&addrin;
    uint addrsz = sizeof(addrin);
    bzero(&addrin, addrsz);
    addrin.sin_family = AF_INET;
    addrin.sin_addr.s_addr = htonl(INADDR_ANY);
    addrin.sin_port = htons(atoi(_config.port.c_str()));

    RET(bind(_sk_service, addr, addrsz) != 0, false);
    RET(listen(_sk_service, 5) != 0, false);
    return true;
}

void ConnectionManager::_UninitService()
{
    if (_sk_service)
    {
        close(_sk_service);
        _sk_service = -1;
    }
}

void ConnectionManager::_Loop()
{
    RET(_incomming_running)
    _incomming_running = true;
    _incomming = std::thread(
        [this]()
        {
            try
            {
                this->_LoopIncomming();
            }
            catch (...)
            {
                ERR("this->_LoopIncomming() exception thrown");
            }
            _incomming_running = false;
        });

    this->_LoopWorker();

    if (_incomming.joinable())
    {
        _incomming.join();
    }
}

void ConnectionManager::_LoopIncomming()
{
    RET(_tunnel <= 0);
    while (true)
    {
        Buffer buf(BUFFER_MAX_SIZE);
        int len = read(_tunnel, buf.Ptr(), buf.Cap());
        if (len <= 0)
        {
            // ERR("ConnectionManager::_LoopIncomming no data loop.");
            sleep_ms(100);
            continue;
        }
        buf.Set(len);
        this->_DispatchPacket(buf);
    }
}

void ConnectionManager::_LoopWorker()
{
    int sk = _sk_service;
    while (true)
    {
        auto worker = _allocator->AcquireWorker();
        if (!worker)
        {
            ERR("cannot acquire worker");
            sleep_ms(1000);
            continue;
        }
        worker->Entry(
            sk,
            [this](Buffer buf)
            {
                this->_Arrive(buf);
            },
            [=]()
            {
                _allocator->ReleaseWorker(worker);
            });
    }
}

void ConnectionManager::_Arrive(Buffer buf)
{
    RET(_tunnel <= 0);
    // LOG("+++ %d", buf.Len());
    // auto s =
    //     "s: " + this->_FormatAddr(this->_SourceAddr(buf)) + ", " +
    //     "d: " + this->_FormatAddr(this->_DestinationAddr(buf));
    // LOG("+++ %s", s.c_str());
    write(_tunnel, buf.Ptr(), buf.Len());
}

void ConnectionManager::_DispatchPacket(Buffer buf)
{
    // LOG("--- %d", len);
    auto src = this->_FormatAddr(this->_SourceAddr(buf));
    auto dst = this->_FormatAddr(this->_DestinationAddr(buf));
    // auto s = "s: " + src + ", " + "d: " + dst;
    // LOG("--- %s", s.c_str());
    int w = this->_ParseWorkerId(dst);
    if (w < 0)
    {
        return;
    }
    auto worker = _allocator->IndexWorker(w);
    if (!worker)
    {
        return;
    }
    worker->Leave(buf);
}

u32 ConnectionManager::_SourceAddr(Buffer buf)
{
    int ofst = 12;
    RET(buf.Len() < ofst + 4, 0);
    return *(u32*)((char*)buf.Ptr() + ofst);
}

u32 ConnectionManager::_DestinationAddr(Buffer buf)
{
    int ofst = 16;
    RET(buf.Len() < ofst + 4, 0);
    return *(u32*)((char*)buf.Ptr() + ofst);
}

std::string ConnectionManager::_FormatAddr(u32 addr)
{
    int p1 = (addr >> 0) & 0xff;
    int p2 = (addr >> 8) & 0xff;
    int p3 = (addr >> 16) & 0xff;
    int p4 = (addr >> 24) & 0xff;
    return
        std::to_string(p1) + '.' +
        std::to_string(p2) + '.' +
        std::to_string(p3) + '.' +
        std::to_string(p4);
}

int ConnectionManager::_ParseWorkerId(const std::string& addr)
{
    auto ad = addr + '.';
    int ips[4] = {};
    size_t ofst = 0;
    for (int i = 0; i < 4; ++i)
    {
        size_t pos = ad.find('.', ofst);
        if (pos == std::string::npos)
        {
            return -1;
        }
        ips[i] = atoi(ad.substr(ofst, pos - ofst).c_str());
        ofst = pos + 1;
    }
    return (ips[2] << 8) + ips[3];
}

SPEAR_END
