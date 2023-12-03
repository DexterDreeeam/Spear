#include "../TokenAuthenticator.hpp"

SPEAR_BEG

bool TokenAuthenticator::IsTokenValid(const Token& t)
{
    return true;
}

TokenResponse TokenAuthenticator::Reply(const Token& t, int server_port)
{
    return TokenResponse();
}

SPEAR_END
