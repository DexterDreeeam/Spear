//
//  ConnectionService.swift
//  TunnelProvider
//
//  Created by dexterdreeeam on 2024/1/8.
//

import Foundation
import NetworkExtension

class ConnectionService {
    
    private var endpoint: String
    private var gateway: IGateway?
    
    init(endpoint: String) {
        self.endpoint = endpoint
        self.gateway = nil
    }
    
    func initialize(flow: NEPacketTunnelFlow) -> Bool {
        
        do {
            self.gateway = Gateway(endpoint: self.endpoint)
            self.gateway?.start(flow: flow)
            return true
        } catch {
            return false
        }
    }
    
    func close() {
        self.gateway?.stop()
        self.gateway = nil
    }
}
