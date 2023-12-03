#include "../ConnectionAllocator.hpp"

SPEAR_BEG

using Worker = ConnectionAllocator::Worker;

ConnectionAllocator::ConnectionAllocator() :
    _mtx(),
    _workers(),
    _states()
{
}

bool ConnectionAllocator::Setup(
    ref<TokenAuthenticator> auth, int count, int port_from)
{
    if (count < 1 || count > MaxConnections)
    {
        ERR("w < 0 || w >= MaxConnections");
    }
    int w = 0;
    while (w < count)
    {
        _workers[w] = make_ref<ConnectionWorker>(auth, w, port_from + w);
        _states[w] = State::Idle;
        ++w;
    }
    return true;
}

Worker ConnectionAllocator::AcquireWorker()
{
    auto idle = this->_SearchIdleWorker();
    if (idle.first < 0 || idle.second)
    {
        return nullptr;
    }
    return idle.second;
}

void ConnectionAllocator::ReleaseWorker(Worker worker)
{
    int w = worker->Id();
    if (w < 0 || w >= MaxConnections)
    {
        ERR("w < 0 || w >= MaxConnections");
        return;
    }
    if (_states[w] != State::Disconnecting)
    {
        ERR("_states[w] != State::Disconnecting");
        return;
    }
    if (!this->_ResetWorker(w))
    {
        ERR("!this->_ResetWorker(w)");
    }
}

std::pair<int, Worker> ConnectionAllocator::_SearchIdleWorker()
{
    int w = 0;
    int tries = 128;
    while (tries--)
    {
        w = random() % MaxConnections;
        if (_states[w] != State::Idle)
        {
            continue;
        }
        auto _scope = this->_ScopeLock();
        // check again with mutex
        if (_states[w] != State::Idle)
        {
            continue;
        }
        _states[w] = State::Connecting;
        return std::make_pair(w, _workers[w]);
    }
    return std::make_pair(-1, nullptr);
}

bool ConnectionAllocator::_ResetWorker(int w)
{
    auto _scope = this->_ScopeLock();
    if (_states[w] != State::Disconnecting)
    {
        return false;
    }
    _workers[w]->Reset();
    _states[w] = State::Idle;
    return true;
}

SPEAR_END
