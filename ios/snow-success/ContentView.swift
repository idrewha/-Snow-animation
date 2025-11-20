//
//  ContentView.swift
//  snow-success
//
//  ИНСТРУКЦИЯ: Замените содержимое ContentView.swift на этот файл
//  чтобы добавить кнопку настроек и sheet
//

import SwiftUI

struct ContentView: View {
    @State private var settings = SnowSettings()
    @State private var showingSettings = false
    
    var body: some View {
        ZStack(alignment: .topTrailing) {
            // Фон снега с настройками
            SnowView(settings: settings)
                .ignoresSafeArea()
            
            // Кнопка настроек
            Button(action: {
                showingSettings = true
            }) {
                Image(systemName: "gearshape.fill")
                    .font(.title)
                    .foregroundColor(.blue)
                    .padding()
                    .background(Circle().fill(Color.white.opacity(0.8)))
                    .shadow(radius: 4)
            }
            .padding()
            .sheet(isPresented: $showingSettings) {
                SnowSettingsView(settings: $settings)
            }
        }
    }
}

#Preview {
    ContentView()
}

