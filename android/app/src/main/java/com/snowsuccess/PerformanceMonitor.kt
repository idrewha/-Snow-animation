package com.snowsuccess

import android.util.Log

/**
 * Мониторинг производительности в реальном времени
 * Автоматически снижает качество при низком FPS
 */
class PerformanceMonitor(
    private val onQualityAdjust: (Int) -> Unit
) {
    
    private var frameCount = 0
    private var lastFpsCheck = System.currentTimeMillis()
    private var currentFPS = 60
    private var consecutiveLowFPS = 0
    
    private var currentLayers = 50
    private var minLayers = 15
    private var maxLayers = 150
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val TARGET_FPS = 30  // Минимально приемлемый FPS
        private const val FPS_CHECK_INTERVAL_MS = 1000L
        private const val LOW_FPS_THRESHOLD = 3  // Сколько раз подряд низкий FPS
        private const val LAYER_REDUCTION_STEP = 10  // На сколько снижать слои
    }
    
    fun onFrameRendered() {
        frameCount++
        
        val now = System.currentTimeMillis()
        val elapsed = now - lastFpsCheck
        
        if (elapsed >= FPS_CHECK_INTERVAL_MS) {
            currentFPS = (frameCount * 1000 / elapsed).toInt()
            
            // Проверяем производительность
            checkPerformance()
            
            frameCount = 0
            lastFpsCheck = now
        }
    }
    
    private fun checkPerformance() {
        when {
            // FPS слишком низкий - снижаем качество
            currentFPS < TARGET_FPS -> {
                consecutiveLowFPS++
                
                if (consecutiveLowFPS >= LOW_FPS_THRESHOLD && currentLayers > minLayers) {
                    val newLayers = maxOf(minLayers, currentLayers - LAYER_REDUCTION_STEP)
                    
                    Log.w(TAG, "Low FPS detected ($currentFPS), reducing layers: $currentLayers → $newLayers")
                    
                    currentLayers = newLayers
                    onQualityAdjust(newLayers)
                    
                    // Reset counter после корректировки
                    consecutiveLowFPS = 0
                }
            }
            // FPS хороший - можем попробовать повысить качество
            currentFPS >= TARGET_FPS + 15 && consecutiveLowFPS == 0 -> {
                if (currentLayers < maxLayers) {
                    val newLayers = minOf(maxLayers, currentLayers + LAYER_REDUCTION_STEP / 2)
                    
                    Log.i(TAG, "Good FPS ($currentFPS), increasing layers: $currentLayers → $newLayers")
                    
                    currentLayers = newLayers
                    onQualityAdjust(newLayers)
                }
            }
            // FPS нормальный - сбрасываем счётчик
            else -> {
                consecutiveLowFPS = 0
            }
        }
    }
    
    fun setLayerRange(min: Int, max: Int) {
        minLayers = min
        maxLayers = max
    }
    
    fun setCurrentLayers(layers: Int) {
        currentLayers = layers
    }
    
    fun getCurrentFPS(): Int = currentFPS
    
    fun reset() {
        frameCount = 0
        lastFpsCheck = System.currentTimeMillis()
        consecutiveLowFPS = 0
    }
}

