#include "../ConnectionManager.hpp"

SPEAR_BEG

void ConnectionManager::Setup(const Config& config)
{
    if (ConnectionManager::Ins())
    {
        ERR("ConnectionManager::Ins()");
        return;
    }

    auto* p = new ConnectionManager();
    auto ins = ref<ConnectionManager>(p);
    if (!ins || !ins->_Init(config))
    {
        ERR("!ins || !ins->_Init()");
        return;
    }

    ins->_config = config;
    ConnectionManager::Ins(ins);
}

bool ConnectionManager::_Init(const Config& config)
{
    if (_config.Empty())
    {
        return false;
    }

    if (!_InitTunnel())
    {
        return false;
    }
    escape_function ef_tunnel = [this]() { this->_UninitTunnel(); };

    if (!_InitService())
    {
        return false;
    }
    escape_function ef_service = [this]() { this->_UninitService(); };

    this->_auth = make_ref<TokenAuthenticator>();
    this->_allocator = make_ref<ConnectionAllocator>();
    if (!this->_allocator->Setup(
            _auth, config.max_connection, config.transport_port_from))
    {
        ERR("!ins->_allocator->Setup");
        return false;
    }

    ef_tunnel.disable();
    ef_service.disable();
    return true;
}

bool ConnectionManager::_InitTunnel()
{
    int tunnel = build_tunnel(_config.tun);
    if (tunnel <= 0)
    {
        return false;
    }
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
    if (sk <= 0)
    {
        ERR("socket");
        return false;
    }
    _sk_service = sk;

    sockaddr_in addrin;
    auto* addr = (sockaddr*)&addrin;
    uint addrsz = sizeof(addrin);
    bzero(&addrin, addrsz);
    addrin.sin_family = AF_INET;
    addrin.sin_addr.s_addr = htonl(INADDR_ANY);
    addrin.sin_port = htons(atoi(_config.port.c_str()));
    if (bind(sk, addr, addrsz) != 0)
    {
        ERR("bind");
    }
    if (listen(sk, 5) != 0)
    {
        ERR("listen");
    }
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
        ConnectionWorker::Entry(
            worker, sk, [=]()
            {
                _allocator->ReleaseWorker(worker);
            });
    }
}

SPEAR_END
