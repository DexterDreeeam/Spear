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
    int sk = socket(AF_INET, SOCK_STREAM, 0);
    RET(sk <= 0, false);
    _sk_service = sk;

    sockaddr_in addrin;
    auto* addr = (sockaddr*)&addrin;
    uint addrsz = sizeof(addrin);
    bzero(&addrin, addrsz);
    addrin.sin_family = AF_INET;
    addrin.sin_addr.s_addr = htonl(INADDR_ANY);
    addrin.sin_port = htons(atoi(_config.port.c_str()));

    RET(bind(sk, addr, addrsz) != 0, false);
    RET(listen(sk, 5) != 0, false);
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
            sleep_ms(100);
            continue;
        }
        buf.Set(len);
        // LOG("--- %d", len);
        _test->Leave(buf);
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
        _test = worker;
    }
}

void ConnectionManager::_Arrive(Buffer buf)
{
    RET(_tunnel <= 0);
    // LOG("+++ %d", buf.Len());
    write(_tunnel, buf.Ptr(), buf.Len());
}

SPEAR_END
