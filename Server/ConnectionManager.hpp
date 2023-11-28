#pragma once

#include "env/headers_env.hpp"
#include "TokenAuthenticator.hpp"

SPEAR_BEG

class ConnectionManager
{
    Config              _config;
    TokenAuthenticator  _auth;

    ConnectionManager() :
        _config()
    {}

    ConnectionManager(const ConnectionManager&) = delete;

public:
    ~ConnectionManager()
    {}

public:
    static void Init(const Config& config)
    {
        if (ConnectionManager::Ins())
        {
            return;
        }
        auto* p = new ConnectionManager();
        ref<ConnectionManager> ins(p);
        if (!ins)
        {
            return;
        }
        ins->_config = config;
        ConnectionManager::Ins(ins);
    }

    static ref<ConnectionManager> Ins(
        ref<ConnectionManager> ins = nullptr)
    {
        static ref<ConnectionManager> _ins = nullptr;
        return _ins ? _ins : _ins = ins;
    }

};

SPEAR_END
