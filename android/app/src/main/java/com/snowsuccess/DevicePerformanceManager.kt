package com.snowsuccess

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Менеджер производительности устройства
 * Определяет возможности устройства и рекомендует настройки
 */
object DevicePerformanceManager {
    
    private const val TAG = "DevicePerformance"
    
    enum class PerformanceTier {
        LOW,      // Старые/слабые устройства (< 2GB RAM, старый GPU)
        MEDIUM,   // Средний сегмент (2-4GB RAM)
        HIGH      // Флагманы (> 4GB RAM, современный GPU)
    }
    
    data class PerformanceProfile(
        val tier: PerformanceTier,
        val maxLayers: Int,
        val defaultLayers: Int,
        val useAdaptiveFPS: Boolean,
        val renderContinuously: Boolean
    )
    
    /**
     * Определяет производительность устройства
     */
    fun detectPerformanceTier(context: Context): PerformanceTier {
        val ramGB = getTotalRAM(context)
        val isOldAndroid = Build.VERSION.SDK_INT < Build.VERSION_CODES.M // < Android 6.0
        val cpuCores = Runtime.getRuntime().availableProcessors()
        
        Log.d(TAG, "Device info: RAM=${ramGB}GB, API=${Build.VERSION.SDK_INT}, Cores=$cpuCores")
        
        return when {
            // Низкая производительность
            ramGB < 2.0 || isOldAndroid || cpuCores < 4 -> {
                Log.i(TAG, "Detected LOW performance tier")
                PerformanceTier.LOW
            }
            // Высокая производительность
            ramGB >= 4.0 && cpuCores >= 8 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                Log.i(TAG, "Detected HIGH performance tier")
                PerformanceTier.HIGH
            }
            // Средняя производительность
            else -> {
                Log.i(TAG, "Detected MEDIUM performance tier")
                PerformanceTier.MEDIUM
            }
        }
    }
    
    /**
     * Получает профиль производительности для устройства
     */
    fun getPerformanceProfile(context: Context): PerformanceProfile {
        val tier = detectPerformanceTier(context)
        
        return when (tier) {
            PerformanceTier.LOW -> PerformanceProfile(
                tier = tier,
                maxLayers = 50,
                defaultLayers = 25,
                useAdaptiveFPS = true,
                renderContinuously = false  // WHEN_DIRTY для экономии батареи
            )
            PerformanceTier.MEDIUM -> PerformanceProfile(
                tier = tier,
                maxLayers = 100,
                defaultLayers = 50,
                useAdaptiveFPS = true,
                renderContinuously = false  // WHEN_DIRTY для стабильности
            )
            PerformanceTier.HIGH -> PerformanceProfile(
                tier = tier,
                maxLayers = 150,
                defaultLayers = 75,
                useAdaptiveFPS = false,
                renderContinuously = true  // Можно CONTINUOUSLY
            )
        }
    }
    
    /**
     * Проверяет режим энергосбережения
     */
    fun isLowPowerMode(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            return powerManager.isPowerSaveMode
        }
        return false
    }
    
    /**
     * Получает общий объём RAM в GB
     */
    private fun getTotalRAM(context: Context): Double {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
    }
    
    /**
     * Проверяет доступность OpenGL ES 3.0
     */
    fun supportsOpenGLES3(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        return configurationInfo.reqGlEsVersion >= 0x30000
    }
}

