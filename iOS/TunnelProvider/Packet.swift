//
//  Packet.swift
//  TunnelProvider
//
//  Created by dexterdreeeam on 2024/1/3.
//

import Foundation

class Packet {
    
    var buf = [UInt8](repeating: 0, count: 2048)
    var len = 0
    
    func toString() -> String {
        return String(
            decoding: Array(self.buf.prefix(self.len)), as: UTF8.self)
    }
}
