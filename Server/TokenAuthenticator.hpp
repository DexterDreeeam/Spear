#pragma once

#include "env/headers_env.hpp"

SPEAR_BEG

class TokenAuthenticator
{
public:
    bool IsTokenValid(const std::string& t);
};

SPEAR_END
