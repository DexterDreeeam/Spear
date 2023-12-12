#include "../ConnectionAllocator.hpp"

SPEAR_BEG

using Worker = ConnectionAllocator::Worker;

ConnectionAllocator::ConnectionAllocator(const Config& config) :
    _config(config),
    _mtx(),
    _cnt(0),
    _workers()
{
}

bool ConnectionAllocator::Setup(
    ref<TokenAuthenticator> auth, int count, int port_from)
{
    RET(count < 1 || count > MaxConnections, false);

    int w = 0;
    do
    {
        _workers[w] = make_ref<ConnectionWorker>(
            _config, auth, w, port_from + w);
    } while (++w < count);
    _cnt = count;
    return true;
}

Worker ConnectionAllocator::AcquireWorker()
{
    auto worker = this->_SearchIdleWorker();
    RET(!worker, nullptr);
    return worker;
}

void ConnectionAllocator::ReleaseWorker(Worker worker)
{
    int w = worker->Id();
    RET(w < 0 || w >= MaxConnections);
    worker->Reset();
    worker->Idle();
}

Worker ConnectionAllocator::IndexWorker(int w)
{
    RET(w < 0 || w >= MaxConnections, nullptr);
    return _workers[w];
}

Worker ConnectionAllocator::_SearchIdleWorker()
{
    int w = 0;
    int tries = _cnt;
    while (tries--)
    {
        w = random() % _cnt;
        // auto _scope = this->_ScopeLock();
        auto worker = _workers[w];
        if (worker->Occupy())
        {
            worker->Escape(
                make_ef([=]()
                {
                    // Final Escape
                    worker->Idle();
                }));
            return worker;
        }
    }
    return nullptr;
}

bool ConnectionAllocator::_ResetWorker(int w)
{
    // auto _scope = this->_ScopeLock();
    // RET(_states[w] != State::Disconnecting, false);
    _workers[w]->Reset();
    return true;
}

SPEAR_END
