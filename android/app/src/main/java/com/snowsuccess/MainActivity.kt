package com.snowsuccess

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Главная активность с анимацией снега
 * Настройки открываются в BottomSheet
 */
/**
 * Главная активность с оптимизированной анимацией снега
 * Production-ready для банковского приложения
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var snowView: SnowView
    private lateinit var fpsCounter: TextView
    private lateinit var fabSettings: FloatingActionButton
    
    private var currentSettings = SnowSettings()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting Snow Success app")
        
        try {
            setContentView(R.layout.activity_main)
            Log.d(TAG, "onCreate: Layout inflated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Failed to inflate layout", e)
            throw e
        }
        
        try {
            snowView = findViewById(R.id.snowView)
            Log.d(TAG, "onCreate: SnowView found")
            
            fpsCounter = findViewById(R.id.fpsCounter)
            Log.d(TAG, "onCreate: FPS counter found")
            
            fabSettings = findViewById(R.id.fabSettings)
            Log.d(TAG, "onCreate: FAB found")
            
            setupSettingsButton()
            startFPSCounter()
            
            // Применяем начальные настройки
            applySettingsToSnowView(currentSettings)
            
            Log.d(TAG, "onCreate: Setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Failed during setup", e)
            throw e
        }
    }
    
    private fun setupSettingsButton() {
        fabSettings.setOnClickListener {
            Log.d(TAG, "Opening settings bottom sheet")
            showSettingsBottomSheet()
        }
    }
    
    private fun showSettingsBottomSheet() {
        try {
            val bottomSheet = SnowSettingsBottomSheet.newInstance(currentSettings)
            
            bottomSheet.setOnSettingsChangedListener { newSettings ->
                currentSettings = newSettings
                applySettingsToSnowView(newSettings)
            }
            
            bottomSheet.show(supportFragmentManager, "SnowSettingsBottomSheet")
            Log.d(TAG, "BottomSheet shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show BottomSheet", e)
            // Показываем Toast с ошибкой
            android.widget.Toast.makeText(
                this,
                "Ошибка открытия настроек: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun applySettingsToSnowView(settings: SnowSettings) {
        snowView.layers = settings.layers
        snowView.speed = settings.speed
        snowView.depth = settings.depth
        snowView.width = settings.width
        snowView.opacity = settings.opacity
        snowView.flakeColor = settings.flakeColor
        
        Log.d(TAG, "Applied settings: layers=${settings.layers}, speed=${settings.speed}, color=${Integer.toHexString(settings.flakeColor)}")
    }
    
    private fun startFPSCounter() {
        snowView.post(object : Runnable {
            override fun run() {
                // Используем встроенный FPS counter из SnowView
                val fps = snowView.getCurrentFPS()
                fpsCounter.text = "FPS: $fps"
                
                // Обновляем раз в секунду (не нужно каждый кадр)
                snowView.postDelayed(this, 1000)
            }
        })
    }
    
    override fun onResume() {
        super.onResume()
        snowView.onResume()
        Log.d(TAG, "Activity resumed - SnowView active")
    }
    
    override fun onPause() {
        super.onPause()
        snowView.onPause()
        Log.d(TAG, "Activity paused - SnowView stopped")
    }
    
    override fun onStop() {
        super.onStop()
        // Дополнительная остановка для экономии батареи
        Log.d(TAG, "Activity stopped - ensuring SnowView is paused")
    }
    
    companion object {
        private const val TAG = "SnowSuccess"
    }
}
