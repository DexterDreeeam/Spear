//
//  PacketTunnelProvider.swift
//  TunnelProvider
//
//  Created by dexterdreeeam on 2023/12/20.
//

import Foundation
import NetworkExtension

class TunnelProvider: NEPacketTunnelProvider {

    override func startTunnel(options: [String : NSObject]?, completionHandler: @escaping (Error?) -> Void) {
        print("TunnelProvider startTunnel")
        
        
        setTunnelNetworkSettings(buildSettings()) { [self] err in
            if err != nil {
                completionHandler(err)
                return
            }
            completionHandler(nil)
            loop()
        }
    }
    
    override func stopTunnel(with reason: NEProviderStopReason, completionHandler: @escaping () -> Void) {
        print("TunnelProvider stopTunnel")
        completionHandler()
    }
    
    override func handleAppMessage(_ messageData: Data, completionHandler: ((Data?) -> Void)?) {
        print("TunnelProvider handleAppMessage")
        if let handler = completionHandler {
            handler(messageData)
        }
    }
    
    override func sleep(completionHandler: @escaping () -> Void) {
        print("TunnelProvider sleep")
        completionHandler()
    }
    
    override func wake() {
        print("TunnelProvider wake")
    }
    
    private func buildSettings() -> NEPacketTunnelNetworkSettings? {
        let ipv4Settings = NEIPv4Settings(addresses: ["10.1.0.10"], subnetMasks: ["255.255.255.255"])
        ipv4Settings.includedRoutes = [NEIPv4Route.default()]

        let dnsSettings = NEDNSSettings(servers: ["8.8.8.8"])
        dnsSettings.matchDomains = [""]

        let settings = NEPacketTunnelNetworkSettings(tunnelRemoteAddress: "20.255.49.236")
        settings.mtu = 1400
        settings.ipv4Settings = ipv4Settings
        settings.dnsSettings = dnsSettings

        return settings
    }
    
    private func loop() {
        packetFlow.readPackets { [self] packets, protocols in
            // todo
        }
        
        packetFlow.writePackets([], withProtocols: [NSNumber(value: AF_INET)])
    }
}
