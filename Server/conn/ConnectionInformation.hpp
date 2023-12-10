#pragma once

#include "../env/_headers.hpp"
#include "ConnectionWorker.hpp"

SPEAR_BEG

class ConnectionInformation
{
private:
    std::map<std::string, std::string> _pairs;

    // [Minimal]
    // "VpnTunAddr" : "10.10.0.2"
    // "VpnDns": "8.8.8.8"
    // "TransportPort": "22334"
    //
    // [Extension]
    // "Encryption" : "RSA"
    // "EncryptionToken": "XxxxYyyy"
    //
    std::vector<std::string> BasicValues = {
        "VpnTunAddr",
        "VpnDns",
        "TransportPort",
    };
    std::vector<std::string> ExtensionValues = {
        "Encryption",
        "EncryptionToken",
    };


public:
    ConnectionInformation() = default;
    ~ConnectionInformation() = default;

    bool IsValid() const
    {
        for (auto& k : BasicValues)
        {
            if (!_pairs.count(k) || _pairs.at(k).size() < 1)
            {
                return false;
            }
        }
        return true;
    }

    std::string ToString() const
    {
        std::string str = "";
        for (auto& p : _pairs) {
            if (p.first.size() > 0)
            {
                str += '\"' + p.first + '\"';
                str += ':';
                str += '\"' + p.second + '\"';
                str += ',';
            }
        }
        if (str.size() > 0)
        {
            // remove tailing comma
            str.pop_back();
        }
        return '{' + str + '}';
    }

    std::string& operator [](const std::string& key)
    {
        if (_pairs.count(key) == 0)
        {
            _pairs[key] = "";
        }
        return _pairs[key];
    }
};

SPEAR_END
