//
//  ConnectionKeeper.swift
//  TunnelProvider
//
//  Created by dexterdreeeam on 2024/1/8.
//

import Foundation

class ConnectionKeeper {
    
    private var sk: Int32 = 0
    private var thread: Thread? = nil
    
    var vpnTunAddr: String
    var vpnDns: String
    var vpnTransportPort: String
    
    init() {
        self.vpnTunAddr = ""
        self.vpnDns = ""
        self.vpnTransportPort = ""
    }
    
    deinit {
        if (self.sk != 0) {
            close(self.sk)
        }
    }
    
    func tryConnect() -> Bool {
        var serverInfo: UnsafeMutablePointer<addrinfo>?
        var hints = addrinfo()
        hints.ai_family = AF_INET
        hints.ai_socktype = SOCK_STREAM
        
        guard getaddrinfo("123.123.123.123", "12345", &hints, &serverInfo) == 0 else {
            perror("Error in getaddrinfo")
            return false
        }
        
        self.sk = socket(
            (serverInfo?.pointee.ai_family)!,
            (serverInfo?.pointee.ai_socktype)!,
            (serverInfo?.pointee.ai_protocol)!)
        
        guard connect(
            self.sk,
            (serverInfo?.pointee.ai_addr)!,
            (serverInfo?.pointee.ai_addrlen)!) == 0 else {
            perror("Error in connect")
            return false
        }
        
        guard shakeHands() == true else {
            perror("Error in shakeHands")
            return false
        }
        
        self.loop()
        return true
    }
    
    private func shakeHands() -> Bool {
        
        let pkt = receivePacket()
        guard pkt != nil else {
            return false
        }
        
        if let jsonData = pkt!.toString().data(using: .utf8) {
            do {
                let info = try JSONDecoder().decode(
                    ConnectionInfo.self, from: jsonData)
                
                self.vpnTunAddr = info.VpnTunAddr
                self.vpnDns = info.VpnDns
                self.vpnTransportPort = info.TransportPort
            } catch {
                print("Error decoding JSON: \(error)")
            }
        }
        return true
    }
    
    private func loop() {
        DispatchQueue.global().async {
            while true {
                var p = self.receivePacket()
                if (p == nil) {
                    break
                }
            }
        }
    }
    
    private func sendString(msg: String) {
        if (self.sk != 0) {
            send(self.sk, msg, msg.utf8.count, 0)
        }
    }
    
    private func receivePacket() -> Packet? {
        var p = Packet()
        p.len = recv(self.sk, &p.buf, p.buf.count, 0)
        return if (p.len > 0) {
            p
        } else {
            nil
        }
    }
}
