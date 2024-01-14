//
//  Utils.swift
//  TunnelProvider
//
//  Created by dexterdreeeam on 2024/1/8.
//

import Foundation

class Utils {
    
    static func toString(u8s: [UInt8]) -> String {
        return String(decoding: u8s, as: UTF8.self)
    }
}
