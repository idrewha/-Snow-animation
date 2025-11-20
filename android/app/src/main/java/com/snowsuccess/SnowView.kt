package com.snowsuccess

import android.content.Context
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

/**
 * ❄️ SnowView - Drop-in снежная анимация для Android
 * 
 * Портирование WebGL/Metal анимации на Android OpenGL ES 2.0
 * Без внешних зависимостей - только Android SDK
 * 
 * Использование в XML:
 * ```xml
 * <com.snowsuccess.SnowView
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:snowLayers="50"
 *     app:snowSpeed="1.2" />
 * ```
 * 
 * Programmatic API:
 * ```kotlin
 * val snowView = SnowView(context)
 * snowView.layers = 50
 * snowView.speed = 1.2f
 * rootLayout.addView(snowView, 0) // Добавить как фон
 * ```
 * 
 * @author Портировано с Metal/WebGL версии
 */
class SnowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer: SnowRenderer
    private val performanceProfile = DevicePerformanceManager.getPerformanceProfile(context)
    private val performanceMonitor: PerformanceMonitor
    
    private var isActive = true
    private var autoPauseEnabled = true
    
    // Публичный API для настройки анимации
    var layers: Int = performanceProfile.defaultLayers
        set(value) {
            val coercedValue = value.coerceIn(0, performanceProfile.maxLayers)
            field = coercedValue
            renderer.layers = coercedValue
            performanceMonitor.setCurrentLayers(coercedValue)
            
            // Триггерим перерисовку для WHEN_DIRTY режима
            requestRender()
        }

    var speed: Float = 0.5f  // Новый дефолт по запросу пользователя
        set(value) {
            field = value.coerceAtLeast(0f)
            renderer.speed = field
            requestRender()
        }

    var depth: Float = 0.5f
        set(value) {
            field = value
            renderer.depth = field
            requestRender()
        }

    var width: Float = 0.3f
        set(value) {
            field = value
            renderer.width = field
            requestRender()
        }

    var opacity: Float = 1.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            renderer.opacity = field
            requestRender()
        }

    var flakeColor: Int = Color.rgb(1, 66, 152) // #014298 - новый дефолт
        set(value) {
            field = value
            renderer.setFlakeColor(value)
            requestRender()
        }

    init {
        android.util.Log.d(TAG, "Initializing SnowView with profile: $performanceProfile")
        
        // Инициализируем performance monitor
        performanceMonitor = PerformanceMonitor { adjustedLayers ->
            // Callback от монитора производительности
            post {
                layers = adjustedLayers
                android.util.Log.i(TAG, "Auto-adjusted layers to $adjustedLayers")
            }
        }
        performanceMonitor.setLayerRange(15, performanceProfile.maxLayers)
        
        // Читаем XML attributes (если используется в XML)
        attrs?.let {
            try {
                val typedArray = context.obtainStyledAttributes(it, R.styleable.SnowView, 0, 0)
                try {
                    val requestedLayers = typedArray.getInt(R.styleable.SnowView_snowLayers, performanceProfile.defaultLayers)
                    layers = requestedLayers.coerceAtMost(performanceProfile.maxLayers)
                    speed = typedArray.getFloat(R.styleable.SnowView_snowSpeed, 0.5f)  // Новый дефолт
                    depth = typedArray.getFloat(R.styleable.SnowView_snowDepth, 0.5f)
                    width = typedArray.getFloat(R.styleable.SnowView_snowWidth, 0.3f)
                    opacity = typedArray.getFloat(R.styleable.SnowView_snowOpacity, 1.0f)
                    flakeColor = typedArray.getColor(R.styleable.SnowView_snowColor, Color.rgb(1, 66, 152))  // #014298
                } finally {
                    typedArray.recycle()
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to read XML attributes, using defaults", e)
            }
        }

        // Настройка OpenGL ES 2.0 (совместимость с Android 4.1+)
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)

        // Прозрачный фон для использования поверх другого контента
        holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
        setZOrderOnTop(false)

        // Создаём рендерер
        renderer = SnowRenderer(performanceMonitor)
        renderer.layers = layers
        renderer.speed = speed
        renderer.depth = depth
        renderer.width = width
        renderer.opacity = opacity
        renderer.setFlakeColor(flakeColor)

        setRenderer(renderer)
        
        // КРИТИЧЕСКАЯ ОПТИМИЗАЦИЯ: выбираем режим рендеринга
        renderMode = if (performanceProfile.renderContinuously && !DevicePerformanceManager.isLowPowerMode(context)) {
            android.util.Log.d(TAG, "Using CONTINUOUSLY render mode (high-end device)")
            RENDERMODE_CONTINUOUSLY
        } else {
            android.util.Log.d(TAG, "Using WHEN_DIRTY render mode (battery save)")
            RENDERMODE_WHEN_DIRTY
        }
        
        // Для WHEN_DIRTY запускаем принудительную перерисовку
        if (renderMode == RENDERMODE_WHEN_DIRTY) {
            startManualRenderLoop()
        }
    }

    // Preset режимы (как в Web/iOS версиях) с учетом возможностей устройства
    fun setLightMode() {
        layers = minOf(50, performanceProfile.maxLayers)
        depth = 0.5f
        width = 0.3f
        speed = 1.2f
    }

    fun setHeavyMode() {
        layers = minOf(150, performanceProfile.maxLayers) // Не 200, а адаптивно!
        depth = 0.1f
        width = 0.8f
        speed = 1.5f
    }
    
    /**
     * Lifecycle management - КРИТИЧНО для производительности!
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        android.util.Log.d(TAG, "onAttachedToWindow: resuming render")
        resumeRender()
    }
    
    override fun onDetachedFromWindow() {
        android.util.Log.d(TAG, "onDetachedFromWindow: pausing render")
        pauseRender()
        super.onDetachedFromWindow()
    }
    
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        
        // ИСПРАВЛЕНИЕ: НЕ останавливаем анимацию для диалогов/BottomSheet
        // Останавливаем только при полном сворачивании приложения (onPause)
        android.util.Log.d(TAG, "Window focus changed: $hasWindowFocus (auto-pause disabled for dialogs)")
    }
    
    override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        
        // ИСПРАВЛЕНИЕ: проверяем только если это изменение видимости самого SnowView
        // а не дочерних элементов (например BottomSheet)
        if (changedView == this && autoPauseEnabled) {
            if (visibility == android.view.View.VISIBLE) {
                resumeRender()
            } else {
                pauseRender()
            }
        }
    }
    
    private fun pauseRender() {
        if (isActive) {
            isActive = false
            renderer.isPaused = true
            android.util.Log.d(TAG, "Render paused - saving battery")
        }
    }
    
    private fun resumeRender() {
        if (!isActive) {
            isActive = true
            renderer.isPaused = false
            performanceMonitor.reset()
            
            // ИСПРАВЛЕНИЕ: Перезапускаем render loop если используем WHEN_DIRTY
            if (renderMode == RENDERMODE_WHEN_DIRTY) {
                startManualRenderLoop()
            } else {
                requestRender()
            }
            
            android.util.Log.d(TAG, "Render resumed")
        }
    }
    
    /**
     * Для режима WHEN_DIRTY запускаем контролируемую перерисовку
     */
    private fun startManualRenderLoop() {
        val fps = if (performanceProfile.tier == DevicePerformanceManager.PerformanceTier.LOW) 30 else 60
        val frameDelay = 1000L / fps
        
        val renderRunnable = object : Runnable {
            override fun run() {
                if (isActive && renderMode == RENDERMODE_WHEN_DIRTY) {
                    requestRender()
                    postDelayed(this, frameDelay)
                }
            }
        }
        
        post(renderRunnable)
        android.util.Log.d(TAG, "Manual render loop started at $fps FPS")
    }
    
    /**
     * Получить текущий FPS (для UI)
     */
    fun getCurrentFPS(): Int = performanceMonitor.getCurrentFPS()
    
    /**
     * Включить/выключить автопаузу
     */
    fun setAutoPauseEnabled(enabled: Boolean) {
        autoPauseEnabled = enabled
    }
    
    companion object {
        private const val TAG = "SnowView"
    }

    /**
     * OpenGL ES 2.0 Renderer
     * Портирует шейдеры из WebGL версии
     * ОПТИМИЗИРОВАН для production использования
     */
    private class SnowRenderer(
        private val performanceMonitor: PerformanceMonitor
    ) : GLSurfaceView.Renderer {
        
        private var program = 0
        private var positionBuffer: FloatBuffer
        private val startTime = System.currentTimeMillis()
        
        // Uniform locations
        private var positionLoc = 0
        private var resolutionTimeLoc = 0
        private var mouseLoc = 0
        private var layersLoc = 0
        private var depthLoc = 0
        private var widthLoc = 0
        private var speedLoc = 0
        private var flakeColorLoc = 0
        private var opacityLoc = 0
        
        // Параметры анимации
        var layers = 50
        var speed = 0.5f  // Новый дефолт
        var depth = 0.5f
        var width = 0.3f
        var opacity = 1.0f
        private val flakeColorRGB = floatArrayOf(1f/255f, 66f/255f, 152f/255f)  // #014298
        
        private var viewWidth = 1
        private var viewHeight = 1
        
        // Управление паузой
        var isPaused = false

        init {
            // Fullscreen quad (как в Metal и WebGL версиях)
            val positions = floatArrayOf(
                -1f, -1f,
                 1f, -1f,
                -1f,  1f,
                 1f,  1f
            )
            positionBuffer = ByteBuffer.allocateDirect(positions.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(positions)
            positionBuffer.position(0)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0f, 0f, 0f, 0f) // Прозрачный фон
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            
            initShaders()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            viewWidth = max(width, 1)
            viewHeight = max(height, 1)
            GLES20.glViewport(0, 0, viewWidth, viewHeight)
        }

        override fun onDrawFrame(gl: GL10?) {
            // Если на паузе - не рендерим (экономия GPU/батареи)
            if (isPaused) {
                return
            }
            
            val currentTime = (System.currentTimeMillis() - startTime) / 1000f
            
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)
            
            // Обновляем uniforms (аналог Web версии)
            GLES20.glUniform4f(resolutionTimeLoc, viewWidth.toFloat(), viewHeight.toFloat(), currentTime, 0f)
            GLES20.glUniform4f(mouseLoc, 0f, 0f, 0f, 0f)
            GLES20.glUniform1i(layersLoc, layers)
            GLES20.glUniform1f(depthLoc, depth)
            GLES20.glUniform1f(widthLoc, width)
            GLES20.glUniform1f(speedLoc, speed)
            GLES20.glUniform3fv(flakeColorLoc, 1, flakeColorRGB, 0)
            GLES20.glUniform1f(opacityLoc, opacity)
            
            // Рендерим fullscreen quad
            GLES20.glEnableVertexAttribArray(positionLoc)
            GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_FLOAT, false, 0, positionBuffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(positionLoc)
            
            // Мониторинг производительности
            performanceMonitor.onFrameRendered()
        }

        private fun initShaders() {
            val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
            val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
            
            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            
            // Проверка линковки
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val error = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                throw RuntimeException("Error linking program: $error")
            }
            
            // Получаем locations для uniforms и attributes
            positionLoc = GLES20.glGetAttribLocation(program, "a_position")
            resolutionTimeLoc = GLES20.glGetUniformLocation(program, "u_resolutionTime")
            mouseLoc = GLES20.glGetUniformLocation(program, "u_mouse")
            layersLoc = GLES20.glGetUniformLocation(program, "u_layers")
            depthLoc = GLES20.glGetUniformLocation(program, "u_depth")
            widthLoc = GLES20.glGetUniformLocation(program, "u_width")
            speedLoc = GLES20.glGetUniformLocation(program, "u_speed")
            flakeColorLoc = GLES20.glGetUniformLocation(program, "u_flakeColor")
            opacityLoc = GLES20.glGetUniformLocation(program, "u_opacity")
        }

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val error = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Error compiling shader: $error")
            }
            
            return shader
        }

        fun setFlakeColor(color: Int) {
            flakeColorRGB[0] = Color.red(color) / 255f
            flakeColorRGB[1] = Color.green(color) / 255f
            flakeColorRGB[2] = Color.blue(color) / 255f
        }

        companion object {
            // ===== ШЕЙДЕРЫ ВСТРОЕНЫ В КОД =====
            // Портированы из WebGL standalone версии
            
            private const val VERTEX_SHADER = """
                attribute vec2 a_position;
                varying vec2 v_uv;
                
                void main() {
                    v_uv = a_position * 0.5 + 0.5;
                    gl_Position = vec4(a_position, 0.0, 1.0);
                }
            """
            
            private const val FRAGMENT_SHADER = """
                precision highp float;
                
                uniform vec4 u_resolutionTime;
                uniform vec4 u_mouse;
                uniform int u_layers;
                uniform float u_depth;
                uniform float u_width;
                uniform float u_speed;
                uniform vec3 u_flakeColor;
                uniform float u_opacity;
                
                varying vec2 v_uv;
                
                float mod1(float value) {
                    return value - floor(value);
                }
                
                vec2 mod1(vec2 value) {
                    return value - floor(value);
                }
                
                void main() {
                    vec2 resolution = max(u_resolutionTime.xy, vec2(1.0));
                    float iTime = u_resolutionTime.z;
                    
                    mat3 p = mat3(
                        vec3(13.323122, 23.5112, 21.71123),
                        vec3(21.1212, 28.7312, 11.9312),
                        vec3(21.8112, 14.7212, 61.3934)
                    );
                    
                    vec2 fragCoord = v_uv * resolution;
                    vec2 uv = vec2(1.0, resolution.y / resolution.x) * fragCoord / resolution;
                    
                    vec3 acc = vec3(0.0);
                    float dof = 5.0 * sin(iTime * 0.1);
                    
                    for (int i = 0; i < 250; ++i) {
                        if (i >= u_layers) break;
                        
                        float fi = float(i);
                        vec2 q = uv * (1.0 + fi * u_depth);
                        q += vec2(
                            q.y * (u_width * mod1(fi * 7.238917) - u_width * 0.5),
                            u_speed * iTime / (1.0 + fi * u_depth * 0.03)
                        );
                        
                        vec3 n = vec3(floor(q), 31.189 + fi);
                        vec3 m = floor(n) * 0.00001 + fract(n);
                        vec3 mp = (vec3(31415.9) + m) / fract(p * m);
                        vec3 r = fract(mp);
                        
                        vec2 s = abs(mod1(q) - 0.5 + 0.9 * r.xy - 0.45);
                        s += 0.01 * abs(2.0 * fract(10.0 * q.yx) - 1.0);
                        float d = 0.6 * max(s.x - s.y, s.x + s.y) + max(s.x, s.y) - 0.01;
                        float edge = 0.005 + 0.05 * min(0.5 * abs(fi - 5.0 - dof), 1.0);
                        
                        float contribution = smoothstep(edge, -edge, d) * (r.x / (1.0 + 0.02 * fi * u_depth));
                        acc += vec3(contribution);
                    }
                    
                    float intensity = clamp(acc.x, 0.0, 1.0);
                    vec3 color = mix(vec3(1.0), u_flakeColor, intensity);
                    
                    gl_FragColor = vec4(color, u_opacity);
                }
            """
        }
    }
}

