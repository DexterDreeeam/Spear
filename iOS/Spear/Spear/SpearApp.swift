//
//  SpearApp.swift
//  Spear
//
//  Created by dexterdreeeam on 2023/12/20.
//

import SwiftUI

@main
struct SpearApp: App {
    
    @State private var home = Home()
    
    var body: some Scene {
        WindowGroup {
            HomeView()
                .environment(home)
        }
    }
}
