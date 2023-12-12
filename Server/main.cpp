
#include "env/_headers.hpp"
#include "conn/_headers.hpp"

int main(int argc, char** argv)
{
    // int test = spear::build_tunnel("tun0");
    // LOG("%d", test);
    auto config = spear::Config(argc, argv);
    spear::ConnectionManager::Setup(config);
    if (!CM)
    {
        ERR("Cannot create ConnectionManager.");
        exit(1);
    }

    CM->Run();
    CM->WaitComplete();
}
