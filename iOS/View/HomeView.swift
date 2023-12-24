//
//  ContentView.swift
//  Spear
//
//  Created by dexterdreeeam on 2023/12/20.
//

import SwiftUI

struct HomeView: View {
    
    @Environment(Home.self) var home
    @State private var proxyToken = ""
    
    var body: some View {
        
        VStack {
            HStack(alignment: .bottom) {
                Text(home.appTitle)
                    .font(.largeTitle)
                Text(home.appVersion)
                    .font(.footnote)
                    .foregroundStyle(.secondary)

                Spacer()

                Button(home.appWeb) {
                    print("App Web clicked")
                }
            }
            .padding(.horizontal)

            
            TextEditor(text: $proxyToken)
                .font(.system(size: 16, weight: .regular))
                .shadow(color: Color("shadow"), radius: 2)
                .frame(height: 36)
                .padding(.horizontal)
                .padding(.bottom, 5.0)
                .onAppear {
                    proxyToken = home.proxyToken
                }
                
            Button {
                home.onActionButton()
            } label: {
                Text(home.status.actionString())
                    .font(.system(size: 18))
            }
            .frame(maxWidth: .infinity, minHeight: 36)
            .foregroundColor(Color("buttonForeground"))
            .background(Color("buttonBackground"))
            .cornerRadius(6)
            .shadow(color: Color("shadow"), radius: 2)
            .padding(.horizontal)

            Spacer()
        }
    }
}

#Preview {
    let home = Home()
    return HomeView()
        .environment(home)
}
