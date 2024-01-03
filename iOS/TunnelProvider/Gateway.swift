//
//  Gateway.swift
//  TunnelProvider
//
//  Created by dexterdreeeam on 2024/1/3.
//

import Foundation
import NetworkExtension

protocol IGatewayProtocol {
    
    func start(flow: NEPacketTunnelFlow) -> Void
    
    func setup() -> Void
    
    func stop() -> Void
    
    func clean() -> Void
}

class IGateway: IGatewayProtocol {
        
    internal var flow: NEPacketTunnelFlow?
    internal var subGateways: [IGateway]
    private var thread: Thread?
    private var running: Bool?
    
    init() {
        self.flow = nil
        self.subGateways = []
        self.thread = nil
        self.running = nil
    }
    
    final func start(flow: NEPacketTunnelFlow) -> Void {
        guard self.thread?.isExecuting == false else {
            fatalError("self.thread?.isExecuting == false")
        }
        
        self.flow = flow
        self.running = nil
        self.setup()
        self.thread = Thread {
            self.loop()
        }
        self.thread?.start()
        self.subGateways.forEach { g in
            g.start(flow: flow)
        }
    }
    
    internal func setup() -> Void {}
    
    final func stop() -> Void {
        self.running = nil
        self.subGateways.forEach { g in
            g.stop()
        }
        self.subGateways.removeAll()
        if self.thread?.isFinished != true {
            self.thread?.cancel()
        }
    }
    
    internal func clean() -> Void {}
    
    internal func firstIterate() -> Bool {
        return true
    }
    
    internal func iterate() -> Bool {
        return false
    }
    
    private func loop() {
        self.running = true
        if (self.firstIterate()) {
            while (true) {
                if (!self.iterate()) {
                    break
                }
            }
        }
        self.clean()
        self.running = false
    }
}

class Gateway: IGateway {
    
    private var port: IPortProtocol
    
    init(endpoint: String) {
        self.port = TestPort(endpoint: endpoint)
        super.init()
    }
    
    override func setup() {
        self.subGateways.removeAll()
        self.subGateways.append(GatewaySend(port: self.port))
        self.subGateways.append(GatewayReceive(port: self.port))
    }
    
    override func firstIterate() -> Bool {
        return self.port.connect()
    }
    
    override func iterate() -> Bool {
        return false
    }
}

class GatewaySend: IGateway {
    
    private var port: IPortProtocol
    
    init(port: IPortProtocol) {
        self.port = port
        super.init()
    }
    
    override func setup() {
        // todo
    }
    
    override func iterate() -> Bool {
        // todo
        return false
    }
}

class GatewayReceive: IGateway {
    
    private var port: IPortProtocol
    
    init(port: IPortProtocol) {
        self.port = port
        super.init()
    }
    
    override func setup() {
        self.port.onReceive = { packet in
            return true
        }
        
        // todo
    }
}
