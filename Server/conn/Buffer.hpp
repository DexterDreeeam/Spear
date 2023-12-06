#pragma once

#include "../env/_headers.hpp"

SPEAR_BEG

class Buffer
{
    class BufferMemory
    {
    public:
        char* ptr;
        int   cap;

        BufferMemory(int sz) :
            ptr(nullptr),
            cap(sz)
        {
            ptr = new char[sz];
        }

        ~BufferMemory()
        {
            if (ptr)
            {
                delete[] ptr;
            }
        }
    };

    ref<BufferMemory> _mem;
    int               _len;

public:
    Buffer(int sz) :
        _mem(nullptr),
        _len(0)
    {
        auto* bm = new BufferMemory(sz);
        if (bm)
        {
            _mem = ref<BufferMemory>(bm);
        }
    }

    ~Buffer() = default;

    template<typename T = void>
    T* Pos()
    {
        return _mem ? reinterpret_cast<T*>(_mem->ptr) : nullptr;
    }

    int Cap() const
    {
        return _mem ? _mem->cap : 0;
    }

    int Len() const
    {
        return _len;
    }

    void Set(int len)
    {
        _len = len;
    }
};

SPEAR_END
