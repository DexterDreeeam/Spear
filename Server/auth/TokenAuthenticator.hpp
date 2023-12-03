#pragma once

#include "../env/_headers.hpp"
#include "Token.hpp"
#include "TokenResponse.hpp"

SPEAR_BEG

class TokenAuthenticator
{
public:
    bool IsTokenValid(const Token& t);
    TokenResponse Reply(const Token& t, int server_port);
};

SPEAR_END
