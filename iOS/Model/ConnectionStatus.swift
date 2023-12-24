//
//  ConnectionStatus.swift
//  Spear
//
//  Created by dexterdreeeam on 2023/12/24.
//

import Foundation

enum ConnectionStatus: String {
    case Loading = "Loading"
    case Disconnect = "Disconnect"
    case Connecting = "Connecting"
    case Connected = "Connected"
    case Disconnecting = "Disconnecting"
    
    func actionString() -> String {
        switch self {
        case .Loading:
            return "Loading"
        case .Disconnect:
            return "Connect"
        case .Connecting:
            return "Connecting"
        case .Connected:
            return "Disconnect"
        case .Disconnecting:
            return "Disconnecting"
        }
    }
}
