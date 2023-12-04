#pragma once

#include "../env/_headers.hpp"
#include "../auth/_headers.hpp"
#include "ConnectionAllocator.hpp"

SPEAR_BEG

class ConnectionManager
{
    Config                   _config;
    int                      _tunnel;
    int                      _sk_service;
    ref<ConnectionAllocator> _allocator;
    ref<TokenAuthenticator>  _auth;
    bool                     _loop_running;
    std::thread              _loop;

    ConnectionManager(const Config& config) :
        _config(config),
        _tunnel(-1),
        _sk_service(-1),
        _allocator(nullptr),
        _auth(nullptr),
        _loop_running(false),
        _loop()
    {}

    ConnectionManager(const ConnectionManager&) = delete;

public:
    ~ConnectionManager()
    {
        this->WaitComplete();
    }

public:
    static void Setup(const Config& config);

    static ref<ConnectionManager> Ins(
        ref<ConnectionManager> ins = nullptr)
    {
        static ref<ConnectionManager> _ins = nullptr;
        return _ins ? _ins : _ins = ins;
    }

    void Run()
    {
        if (_loop_running)
        {
            return;
        }
        _loop_running = true;
        _loop = std::thread(
            [this]()
            {
                try
                {
                    this->_Loop();
                }
                catch (...)
                {
                    ERR("this->_Loop() exception thrown");
                }
                this->_loop_running = false;
            }
        );
        // _loop.detach();
    }

    void WaitComplete()
    {
        if (_loop_running && _loop.joinable())
        {
            _loop.join();
        }
    }

private:
    bool _Init(const Config& config);
    bool _InitTunnel();
    void _UninitTunnel();
    bool _InitService();
    void _UninitService();
    void _Loop();
};

SPEAR_END
