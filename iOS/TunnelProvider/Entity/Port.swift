//
//  Port.swift
//  TunnelProvider
//
//  Created by dexterdreeeam on 2024/1/3.
//

import Foundation
import NetworkExtension

protocol IPortProtocol {
    
    func connect() -> Bool
    
    func stop() -> Bool
    
    func send(packet: Packet) -> Bool
    
    var onReceive: ((Packet) -> Bool)? { get set }
}

class IPort: IPortProtocol {
    
    internal var connected: Bool? = false
    
    func connect() -> Bool {
        fatalError("Not Implement")
    }
    
    func stop() -> Bool {
        fatalError("Not Implement")
    }
    
    func send(packet: Packet) -> Bool {
        fatalError("Not Implement")
    }
    
    var onReceive: ((Packet) -> Bool)? {
        get {
            fatalError("Not Implement")
        }
        set {
            fatalError("Not Implement")
        }
    }
}

class TestPort: IPort {
    
    private var endpoint: String
    private var thread: Thread?
    
    init(endpoint: String) {
        self.endpoint = endpoint
    }
    
    override func connect() -> Bool {
        guard self.connected == false else {
            return false
        }
        self.connected = nil
        do {
            // todo udp
        
            self.thread = Thread {
                self.loop()
            }
            self.thread!.start()
        } catch {
            // error
            connected = false
            return false
        }
        connected = true
        return true
    }
    
    override func stop() -> Bool {
        guard self.connected == true else {
            return false
        }
        self.connected = nil
        self.thread?.cancel()
        self.thread = nil
        self.connected = false
        return true
    }
    
    override func send(packet: Packet) -> Bool {
        guard self.connected == true else {
            return false
        }
        // todo
        return true
    }
    
    private func loop() {
        
    }
}
