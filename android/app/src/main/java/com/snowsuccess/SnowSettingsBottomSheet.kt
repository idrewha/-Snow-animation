package com.snowsuccess

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom Sheet с настройками снега
 */
class SnowSettingsBottomSheet : BottomSheetDialogFragment() {

    private var onSettingsChanged: ((SnowSettings) -> Unit)? = null
    private var currentSettings = SnowSettings()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            inflater.inflate(R.layout.bottom_sheet_snow_settings_simple, container, false)
        } catch (e: Exception) {
            android.util.Log.e("SnowSettingsBottomSheet", "Failed to inflate layout", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupModeControls(view)
        setupSliders(view)
        setupColorPickers(view)
        setupCustomColorButton(view)
        setupCloseButton(view)
        
        // Обновляем preview текущего цвета
        updateCurrentColorPreview(view)
    }

    private fun setupModeControls(view: View) {
        val radioLight = view.findViewById<RadioButton>(R.id.radioLight)
        val radioHeavy = view.findViewById<RadioButton>(R.id.radioHeavy)

        radioLight.isChecked = currentSettings.isLightMode

        radioLight.setOnClickListener {
            currentSettings = currentSettings.copy(
                layers = 50,
                speed = 1.2f,
                depth = 0.5f,
                width = 0.3f,
                isLightMode = true
            )
            updateSlidersFromSettings(view)
            notifySettingsChanged()
        }

        radioHeavy.setOnClickListener {
            currentSettings = currentSettings.copy(
                layers = 200,
                speed = 1.5f,
                depth = 0.1f,
                width = 0.8f,
                isLightMode = false
            )
            updateSlidersFromSettings(view)
            notifySettingsChanged()
        }
    }

    private fun setupSliders(view: View) {
        val layersSeekBar = view.findViewById<SeekBar>(R.id.layersSeekBar)
        val layersValue = view.findViewById<TextView>(R.id.layersValue)
        val speedSeekBar = view.findViewById<SeekBar>(R.id.speedSeekBar)
        val speedValue = view.findViewById<TextView>(R.id.speedValue)
        val opacitySeekBar = view.findViewById<SeekBar>(R.id.opacitySeekBar)
        val opacityValue = view.findViewById<TextView>(R.id.opacityValue)

        // Слайдер слоёв (0-200)
        layersSeekBar.max = 200
        layersSeekBar.progress = currentSettings.layers
        layersValue.text = currentSettings.layers.toString()

        layersSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                layersValue.text = progress.toString()
                currentSettings = currentSettings.copy(layers = progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Применяем ТОЛЬКО когда пользователь отпустил слайдер!
                notifySettingsChanged()
            }
        })

        // Слайдер скорости (0.0 - 5.0)
        speedSeekBar.max = 50
        speedSeekBar.progress = (currentSettings.speed * 10).toInt()
        speedValue.text = String.format("%.1f", currentSettings.speed)

        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = progress / 10f
                speedValue.text = String.format("%.1f", speed)
                currentSettings = currentSettings.copy(speed = speed)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Применяем ТОЛЬКО когда пользователь отпустил слайдер!
                notifySettingsChanged()
            }
        })

        // Слайдер прозрачности (0.0 - 1.0)
        opacitySeekBar.max = 100
        opacitySeekBar.progress = (currentSettings.opacity * 100).toInt()
        opacityValue.text = String.format("%.2f", currentSettings.opacity)

        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val opacity = progress / 100f
                opacityValue.text = String.format("%.2f", opacity)
                currentSettings = currentSettings.copy(opacity = opacity)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Применяем ТОЛЬКО когда пользователь отпустил слайдер!
                notifySettingsChanged()
            }
        })
    }

    private fun setupColorPickers(view: View) {
        // Preset цвета
        view.findViewById<View>(R.id.colorWhite).setOnClickListener {
            setFlakeColor(Color.WHITE)
            updateCurrentColorPreview(view)
        }
        view.findViewById<View>(R.id.colorGray).setOnClickListener {
            setFlakeColor(Color.rgb(128, 128, 128))
            updateCurrentColorPreview(view)
        }
        view.findViewById<View>(R.id.colorBlue).setOnClickListener {
            setFlakeColor(Color.rgb(135, 206, 235)) // Sky blue
            updateCurrentColorPreview(view)
        }
        view.findViewById<View>(R.id.colorPink).setOnClickListener {
            setFlakeColor(Color.rgb(255, 182, 193)) // Light pink
            updateCurrentColorPreview(view)
        }
        view.findViewById<View>(R.id.colorYellow).setOnClickListener {
            setFlakeColor(Color.rgb(255, 215, 0)) // Gold
            updateCurrentColorPreview(view)
        }
    }

    private fun setupCustomColorButton(view: View) {
        view.findViewById<View>(R.id.btnCustomColor).setOnClickListener {
            showColorPickerDialog()
        }
    }

    private fun showColorPickerDialog() {
        val dialog = ColorPickerDialog(
            requireContext(),
            currentSettings.flakeColor
        ) { selectedColor ->
            setFlakeColor(selectedColor)
            view?.let { updateCurrentColorPreview(it) }
        }
        dialog.show()
    }

    private fun updateCurrentColorPreview(view: View) {
        val preview = view.findViewById<View>(R.id.currentColorPreview)
        if (preview != null) {
            // Используем backgroundTint для совместимости
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                preview.backgroundTintList = android.content.res.ColorStateList.valueOf(currentSettings.flakeColor)
            } else {
                preview.setBackgroundColor(currentSettings.flakeColor)
            }
        }
    }

    private fun setupCloseButton(view: View) {
        view.findViewById<android.widget.Button>(R.id.btnClose).setOnClickListener {
            dismiss()
        }
    }

    private fun setFlakeColor(color: Int) {
        currentSettings = currentSettings.copy(flakeColor = color)
        notifySettingsChanged()
    }

    private fun updateSlidersFromSettings(view: View) {
        val layersSeekBar = view.findViewById<SeekBar>(R.id.layersSeekBar)
        val layersValue = view.findViewById<TextView>(R.id.layersValue)
        val speedSeekBar = view.findViewById<SeekBar>(R.id.speedSeekBar)
        val speedValue = view.findViewById<TextView>(R.id.speedValue)

        layersSeekBar.progress = currentSettings.layers
        layersValue.text = currentSettings.layers.toString()

        speedSeekBar.progress = (currentSettings.speed * 10).toInt()
        speedValue.text = String.format("%.1f", currentSettings.speed)
    }

    private fun notifySettingsChanged() {
        onSettingsChanged?.invoke(currentSettings)
    }

    fun setCurrentSettings(settings: SnowSettings) {
        this.currentSettings = settings
    }

    fun setOnSettingsChangedListener(listener: (SnowSettings) -> Unit) {
        this.onSettingsChanged = listener
    }

    companion object {
        fun newInstance(currentSettings: SnowSettings): SnowSettingsBottomSheet {
            return SnowSettingsBottomSheet().apply {
                setCurrentSettings(currentSettings)
            }
        }
    }
}

/**
 * Data class для настроек снега
 */
data class SnowSettings(
    val layers: Int = 30,  // Базовое значение для всех платформ
    val speed: Float = 0.5f,
    val depth: Float = 0.5f,
    val width: Float = 0.3f,
    val opacity: Float = 1.0f,
    val flakeColor: Int = Color.rgb(1, 66, 152),  // #014298
    val isLightMode: Boolean = true
)

