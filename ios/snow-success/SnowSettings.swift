//
//  SnowSettings.swift
//  snow-success
//
//  Settings model for snow animation
//

import SwiftUI

struct SnowSettings {
    var layers: Int = 30
    var speed: Float = 0.5
    var depth: Float = 0.5
    var width: Float = 0.3
    var opacity: Float = 1.0
    var flakeColor: Color = Color(red: 1/255, green: 66/255, blue: 152/255) // #014298
}

