#include "../ConnectionAllocator.hpp"

SPEAR_BEG

using Worker = ConnectionAllocator::Worker;

ConnectionAllocator::ConnectionAllocator() :
    _mtx(),
    _cnt(0),
    _workers(),
    _states()
{
}

bool ConnectionAllocator::Setup(
    ref<TokenAuthenticator> auth, int count, int port_from)
{
    RET(count < 1 || count > MaxConnections, false);

    int w = 0;
    while (w < count)
    {
        _workers[w] = make_ref<ConnectionWorker>(auth, w, port_from + w);
        _states[w] = State::Idle;
        ++w;
    }
    _cnt = count;
    return true;
}

Worker ConnectionAllocator::AcquireWorker()
{
    auto idle = this->_SearchIdleWorker();
    RET(idle.first < 0 || !idle.second, nullptr);
    return idle.second;
}

void ConnectionAllocator::ReleaseWorker(Worker worker)
{
    int w = worker->Id();
    RET(w < 0 || w >= MaxConnections);
    RET(_states[w] != State::Disconnecting);
    RET(!this->_ResetWorker(w));
}

std::pair<int, Worker> ConnectionAllocator::_SearchIdleWorker()
{
    int w = 0;
    int tries = _cnt;
    while (tries--)
    {
        w = random() % _cnt;
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
    RET(_states[w] != State::Disconnecting, false);
    _workers[w]->Reset();
    _states[w] = State::Idle;
    return true;
}

SPEAR_END
