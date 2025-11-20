//
//  SnowSettingsView.swift
//  snow-success
//
//  Settings sheet for snow animation parameters
//

import SwiftUI

struct SnowSettingsView: View {
    @Binding var settings: SnowSettings
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Режим снега")) {
                    Button(action: {
                        settings.layers = 30
                        settings.speed = 0.5
                        settings.depth = 0.5
                        settings.width = 0.3
                    }) {
                        HStack {
                            Image(systemName: settings.layers <= 40 ? "checkmark.circle.fill" : "circle")
                                .foregroundColor(settings.layers <= 40 ? .blue : .gray)
                            Text("Лёгкий (30 слоёв)")
                        }
                    }
                    .foregroundColor(.primary)
                    
                    Button(action: {
                        settings.layers = 150
                        settings.speed = 1.5
                        settings.depth = 0.1
                        settings.width = 0.8
                    }) {
                        HStack {
                            Image(systemName: settings.layers > 40 ? "checkmark.circle.fill" : "circle")
                                .foregroundColor(settings.layers > 40 ? .blue : .gray)
                            Text("Сильный (150 слоёв)")
                        }
                    }
                    .foregroundColor(.primary)
                }
                
                Section(header: Text("Количество слоёв: \(settings.layers)")) {
                    Slider(value: Binding(
                        get: { Double(settings.layers) },
                        set: { settings.layers = Int($0) }
                    ), in: 0...200, step: 1)
                }
                
                Section(header: Text("Скорость падения: \(String(format: "%.1f", settings.speed))")) {
                    Slider(value: Binding(
                        get: { Double(settings.speed) },
                        set: { settings.speed = Float($0) }
                    ), in: 0...5, step: 0.1)
                }
                
                Section(header: Text("Прозрачность: \(String(format: "%.1f", settings.opacity))")) {
                    Slider(value: Binding(
                        get: { Double(settings.opacity) },
                        set: { settings.opacity = Float($0) }
                    ), in: 0...1, step: 0.1)
                }
                
                Section(header: Text("Цвет снежинок")) {
                    ColorPicker("Выбрать цвет", selection: $settings.flakeColor)
                    
                    HStack(spacing: 10) {
                        ForEach([
                            ("Белый", Color.white),
                            ("Серый", Color.gray),
                            ("Голубой", Color(red: 135/255, green: 206/255, blue: 235/255)),
                            ("Синий", Color(red: 1/255, green: 66/255, blue: 152/255))
                        ], id: \.0) { name, color in
                            Button(action: {
                                settings.flakeColor = color
                            }) {
                                Circle()
                                    .fill(color)
                                    .frame(width: 40, height: 40)
                                    .overlay(
                                        Circle()
                                            .stroke(Color.gray, lineWidth: 2)
                                    )
                            }
                        }
                    }
                    .padding(.vertical, 8)
                }
            }
            .navigationTitle("Настройки снега")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Готово") {
                        dismiss()
                    }
                }
            }
        }
    }
}

#Preview {
    SnowSettingsView(settings: .constant(SnowSettings()))
}

