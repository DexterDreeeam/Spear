//
//  Home.swift
//  Spear
//
//  Created by dexterdreeeam on 2023/12/24.
//

import Foundation

@Observable
class Home {
    
    var appTitle = "Spear"
    var appVersion = "2023.12.20d"
    var appWeb = "About"
    var proxyToken = "your proxy token"
    var status: ConnectionStatus = ConnectionStatus.Loading
    
    init() {
        status = ConnectionStatus.Disconnect
    }
    
    func onActionButton() {
        if status == ConnectionStatus.Disconnect {
            connect()
        }
        else if status == ConnectionStatus.Connected {
            disconnect()
        }
    }
    
    func connect() {
        status = ConnectionStatus.Connected
    }
    
    func disconnect() {
        status = ConnectionStatus.Disconnect
    }
}
