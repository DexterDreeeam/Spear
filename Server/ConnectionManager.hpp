#pragma once

#include "env/headers_env.hpp"

SPEAR_BEG

class ConnectionManager
{
    Config _config;

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
        shared_ptr<ConnectionManager> ins(p);
        if (!ins)
        {
            return;
        }
        ins->_config = config;
        ConnectionManager::Ins(ins);
    }

    static shared_ptr<ConnectionManager> Ins(
        shared_ptr<ConnectionManager> ins = nullptr)
    {
        static  shared_ptr<ConnectionManager> _ins = nullptr;
        return _ins ? _ins : _ins = ins;
    }
};

SPEAR_END
