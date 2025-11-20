package com.snowsuccess

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView

/**
 * Диалог выбора цвета с RGB слайдерами
 */
class ColorPickerDialog(
    context: Context,
    private val initialColor: Int = Color.GRAY,
    private val onColorSelected: (Int) -> Unit
) : Dialog(context) {

    private var currentRed = Color.red(initialColor)
    private var currentGreen = Color.green(initialColor)
    private var currentBlue = Color.blue(initialColor)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_color_picker)
        
        setupColorPicker()
    }

    private fun setupColorPicker() {
        val colorPreview = findViewById<View>(R.id.colorPreview)
        val hexValue = findViewById<TextView>(R.id.hexValue)
        
        val redSeekBar = findViewById<SeekBar>(R.id.redSeekBar)
        val greenSeekBar = findViewById<SeekBar>(R.id.greenSeekBar)
        val blueSeekBar = findViewById<SeekBar>(R.id.blueSeekBar)
        
        val redValue = findViewById<TextView>(R.id.redValue)
        val greenValue = findViewById<TextView>(R.id.greenValue)
        val blueValue = findViewById<TextView>(R.id.blueValue)
        
        // Устанавливаем начальные значения
        redSeekBar.max = 255
        greenSeekBar.max = 255
        blueSeekBar.max = 255
        
        redSeekBar.progress = currentRed
        greenSeekBar.progress = currentGreen
        blueSeekBar.progress = currentBlue
        
        redValue.text = currentRed.toString()
        greenValue.text = currentGreen.toString()
        blueValue.text = currentBlue.toString()
        
        updateColorPreview(colorPreview, hexValue)
        
        // Слайдер Red
        redSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentRed = progress
                redValue.text = progress.toString()
                updateColorPreview(colorPreview, hexValue)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Слайдер Green
        greenSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentGreen = progress
                greenValue.text = progress.toString()
                updateColorPreview(colorPreview, hexValue)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Слайдер Blue
        blueSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBlue = progress
                blueValue.text = progress.toString()
                updateColorPreview(colorPreview, hexValue)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Кнопки
        findViewById<View>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }
        
        findViewById<View>(R.id.btnOk).setOnClickListener {
            val selectedColor = Color.rgb(currentRed, currentGreen, currentBlue)
            onColorSelected(selectedColor)
            dismiss()
        }
    }

    private fun updateColorPreview(colorPreview: View, hexValue: TextView) {
        val color = Color.rgb(currentRed, currentGreen, currentBlue)
        colorPreview.setBackgroundColor(color)
        hexValue.text = String.format("#%02X%02X%02X", currentRed, currentGreen, currentBlue)
    }
}

