//
//  ConnectionInfo.swift
//  TunnelProvider
//
//  Created by dexterdreeeam on 2024/1/8.
//

import Foundation

struct ConnectionInfo: Decodable {
    
    let VpnTunAddr: String
    let VpnDns: String
    let TransportPort: String
    let Encryption: String
    let EncryptionToken: String
}

