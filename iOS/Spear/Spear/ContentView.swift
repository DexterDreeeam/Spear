//
//  ContentView.swift
//  Spear
//
//  Created by dexterdreeeam on 2023/12/20.
//

import SwiftUI

struct ContentView: View {
    
    var AppTitle = "Spear"
    var AppVersion = "2023.12.20d"
    var AppWeb = "About"
    
    var ProxyToken = "your proxy token"
    
    var body: some View {
        VStack {
            HStack(alignment: .bottom) {
                Text(AppTitle)
                    .font(.system(size: 32, weight: .medium, design: .default))
                Text(AppVersion)
                    .font(.system(size: 12, weight: .light))
                
                Spacer()
                
                Button(AppWeb) {
                }
            }
            .padding(.leading)
            .padding(.trailing)
            
            
            TextEditor(text: .constant(ProxyToken))
                .font(.system(size: 16, weight: .regular))
                .frame(height: 36)
                .shadow(color: Color("shadow"), radius: 2)
                .padding(.leading)
                .padding(.trailing)
                
            
            HStack {
                Button {
                    print("clicked")
                } label: {
                    Text("Connect")
                        .font(.system(size: 18))
                        .frame(minHeight: 36)
                        .foregroundColor(Color("buttonForeground"))
                        .background(Color("buttonBackground"))
                        .cornerRadius(6)
                        .shadow(color: Color("shadow"), radius: 2)
                }
                
                Button {
                    print("clicked")
                } label: {
                    Text("Global")
                        .font(.system(size: 18))
                        .frame(minHeight: 36)
                        .foregroundColor(Color("buttonForeground"))
                        .background(Color("buttonBackground"))
                        .cornerRadius(6)
                        .shadow(color: Color("shadow"), radius: 2)
                }
            }
            
            
            VStack {
                
            }
            
            
            Spacer()
        }
    }
}

#Preview {
    ContentView()
}
