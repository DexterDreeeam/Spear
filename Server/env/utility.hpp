#pragma once

SPEAR_BEG

class escape_function
{
    class internal_release_base
    {
    public:
        internal_release_base()
        {}

        virtual ~internal_release_base()
        {}

        virtual void disable() = 0;
    };

    template<typename Fn_Ty>
    class internal_release : public internal_release_base
    {
    public:
        internal_release(Fn_Ty fn) :
            _fn(fn),
            _is_active(true)
        {}

        virtual ~internal_release() override
        {
            if (_is_active)
            {
                _fn();
            }
        }

        virtual void disable() override
        {
            _is_active = false;
        }

    private:
        Fn_Ty _fn;
        bool  _is_active;
    };

public:
    escape_function() :
        _release(nullptr)
    {}

    template<typename Fn_Ty>
    escape_function(Fn_Ty fn) :
        _release(new internal_release<Fn_Ty>(fn))
    {}

    ~escape_function()
    {
        if (_release)
        {
            delete _release;
        }
    }

    template<typename Fn_Ty>
    escape_function& operator =(Fn_Ty fn)
    {
        if (_release)
        {
            _release->disable();
            delete _release;
        }
        _release = new internal_release<Fn_Ty>(fn);
        return *this;
    }

    void disable()
    {
        if (_release)
        {
            _release->disable();
        }
    }

private:
    internal_release_base* _release;
};

using ef = escape_function;

SPEAR_END
