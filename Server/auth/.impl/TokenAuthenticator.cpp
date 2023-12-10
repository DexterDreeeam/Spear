#include "../TokenAuthenticator.hpp"

SPEAR_BEG

bool TokenAuthenticator::IsTokenValid(const Token& t)
{
    size_t i = 0;
    const char* password = "password";
    while (i < sizeof(t.t) && i < strlen(password))
    {
        if (t.t[i] != password[i])
        {
            return false;
        }
        ++i;
    }
    return true;
}

TokenResponse TokenAuthenticator::Reply(const Token& t, int server_port)
{
    return TokenResponse();
}

SPEAR_END
