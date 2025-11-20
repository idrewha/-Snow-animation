# 📖 Руководство по использованию SnowView

## 🎯 Как использовать в своём проекте

### Вариант 1: Drop-in компонент (рекомендуется)

Скопируй **всего 2 файла** в свой проект:

1. **`SnowView.kt`** → `app/src/main/java/[твой_пакет]/SnowView.kt`
2. **`attrs.xml`** → `app/src/main/res/values/attrs.xml`

Измени в `SnowView.kt`:
```kotlin
package com.snowsuccess  // ← Замени на свой пакет
```

Готово! Теперь можешь использовать `SnowView` в любом layout.

---

## 📝 Примеры использования

### 1. Снег как фон всей активности

```xml
<!-- activity_main.xml -->
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Снег на заднем плане -->
    <com.yourpackage.SnowView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:snowLayers="50"
        app:snowSpeed="1.2" />

    <!-- Твой контент поверх снега -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:orientation="vertical">
        
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="С Новым Годом! ❄️"
            android:textSize="32sp"
            android:textColor="#FFFFFF"
            android:textStyle="bold" />
    </LinearLayout>
</FrameLayout>
```

---

### 2. Добавить программно в существующую Activity

```kotlin
class MyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my)
        
        // Создаём SnowView
        val snowView = SnowView(this)
        snowView.layers = 50
        snowView.speed = 1.2f
        
        // Добавляем фоном (index 0 = самый задний слой)
        val rootLayout = findViewById<ViewGroup>(android.R.id.content)
        rootLayout.addView(snowView, 0)
    }
}
```

---

### 3. Снег в Fragment

```kotlin
class MyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_my, container, false)
        
        // Добавляем снег
        val snowView = SnowView(requireContext())
        snowView.layers = 50
        (rootView as ViewGroup).addView(snowView, 0)
        
        return rootView
    }
}
```

---

### 4. Снег в RecyclerView header

```kotlin
class MyAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = FrameLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    600
                )
                
                // Добавляем снег в header
                val snowView = SnowView(parent.context)
                snowView.layers = 30
                snowView.speed = 1.0f
                addView(snowView)
                
                // Текст поверх снега
                val textView = TextView(parent.context).apply {
                    text = "Зимняя коллекция"
                    textSize = 24f
                    gravity = Gravity.CENTER
                }
                addView(textView)
            }
            HeaderViewHolder(view)
        } else {
            // ...
        }
    }
}
```

---

### 5. Динамическое управление (плавные переходы)

```kotlin
class SnowControlActivity : AppCompatActivity() {
    private lateinit var snowView: SnowView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snow_control)
        
        snowView = findViewById(R.id.snowView)
        
        // Кнопки управления
        findViewById<Button>(R.id.btnLight).setOnClickListener {
            animateToLightMode()
        }
        
        findViewById<Button>(R.id.btnHeavy).setOnClickListener {
            animateToHeavyMode()
        }
        
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopSnow()
        }
    }
    
    private fun animateToLightMode() {
        // Плавный переход к лёгкому режиму
        ValueAnimator.ofInt(snowView.layers, 50).apply {
            duration = 1000
            addUpdateListener { snowView.layers = it.animatedValue as Int }
            start()
        }
        
        ValueAnimator.ofFloat(snowView.speed, 1.2f).apply {
            duration = 1000
            addUpdateListener { snowView.speed = it.animatedValue as Float }
            start()
        }
    }
    
    private fun animateToHeavyMode() {
        // Плавный переход к сильному режиму
        ValueAnimator.ofInt(snowView.layers, 200).apply {
            duration = 2000
            addUpdateListener { snowView.layers = it.animatedValue as Int }
            start()
        }
        
        ValueAnimator.ofFloat(snowView.speed, 1.5f).apply {
            duration = 2000
            addUpdateListener { snowView.speed = it.animatedValue as Float }
            start()
        }
    }
    
    private fun stopSnow() {
        // Плавная остановка снега
        ValueAnimator.ofInt(snowView.layers, 0).apply {
            duration = 1500
            addUpdateListener { snowView.layers = it.animatedValue as Int }
            start()
        }
    }
}
```

---

### 6. Снег с изменением цвета по времени суток

```kotlin
class DynamicSnowActivity : AppCompatActivity() {
    private lateinit var snowView: SnowView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dynamic_snow)
        
        snowView = findViewById(R.id.snowView)
        
        // Меняем цвет снега в зависимости от времени
        adjustSnowColorByTime()
    }
    
    private fun adjustSnowColorByTime() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        snowView.flakeColor = when (hour) {
            in 6..11 -> Color.rgb(200, 220, 255)   // Утро - голубоватый
            in 12..17 -> Color.rgb(255, 255, 255)  // День - белый
            in 18..22 -> Color.rgb(180, 180, 220)  // Вечер - сиреневый
            else -> Color.rgb(150, 150, 200)       // Ночь - тёмно-синий
        }
    }
}
```

---

### 7. Снег в диалоговом окне

```kotlin
class SnowDialog(context: Context) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // Снег на фоне
            val snowView = SnowView(context)
            snowView.layers = 30
            addView(snowView)
            
            // Контент диалога
            val contentView = layoutInflater.inflate(R.layout.dialog_content, this, false)
            addView(contentView)
        }
        
        setContentView(layout)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}

// Использование
val dialog = SnowDialog(this)
dialog.show()
```

---

### 8. Снег с реакцией на прокрутку

```kotlin
class ScrollSnowActivity : AppCompatActivity() {
    private lateinit var snowView: SnowView
    private lateinit var scrollView: ScrollView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_snow)
        
        snowView = findViewById(R.id.snowView)
        scrollView = findViewById(R.id.scrollView)
        
        // Увеличиваем интенсивность снега при прокрутке
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val scrollPercent = scrollY.toFloat() / scrollView.maxScrollAmount
            val layers = (50 + scrollPercent * 150).toInt().coerceIn(50, 200)
            snowView.layers = layers
        }
    }
}
```

---

## 🎨 Кастомизация через XML

Все параметры можно настроить через XML атрибуты:

```xml
<com.yourpackage.SnowView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    
    app:snowLayers="100"        <!-- Количество слоёв (0-250) -->
    app:snowSpeed="2.0"         <!-- Скорость падения -->
    app:snowDepth="0.3"         <!-- Эффект глубины -->
    app:snowWidth="0.5"         <!-- Ширина разброса -->
    app:snowOpacity="0.8"       <!-- Прозрачность (0.0-1.0) -->
    app:snowColor="#87CEEB" />  <!-- Цвет снежинок (голубой) -->
```

---

## ⚡ Советы по производительности

### 1. Адаптивное качество для слабых устройств

```kotlin
fun setupAdaptiveSnow(snowView: SnowView) {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    
    // Определяем мощность устройства
    val isLowEnd = memoryInfo.totalMem < 2_000_000_000L // < 2GB RAM
    
    snowView.layers = if (isLowEnd) 25 else 50
}
```

### 2. Остановка анимации когда Activity не видна

```kotlin
class MyActivity : AppCompatActivity() {
    private lateinit var snowView: SnowView
    
    override fun onResume() {
        super.onResume()
        snowView.onResume()  // Запустить анимацию
    }
    
    override fun onPause() {
        super.onPause()
        snowView.onPause()   // Остановить для экономии батареи
    }
}
```

### 3. Динамическое снижение качества при низком FPS

```kotlin
class SmartSnowActivity : AppCompatActivity() {
    private lateinit var snowView: SnowView
    private var frameCount = 0
    private var lastFpsCheck = System.currentTimeMillis()
    
    private fun monitorFPS() {
        snowView.post(object : Runnable {
            override fun run() {
                frameCount++
                val now = System.currentTimeMillis()
                
                if (now - lastFpsCheck >= 1000) {
                    val fps = (frameCount * 1000 / (now - lastFpsCheck)).toInt()
                    
                    // Если FPS падает ниже 30, уменьшаем качество
                    if (fps < 30 && snowView.layers > 25) {
                        snowView.layers = maxOf(25, snowView.layers - 10)
                    }
                    
                    frameCount = 0
                    lastFpsCheck = now
                }
                
                snowView.postDelayed(this, 16)
            }
        })
    }
}
```

---

## 🔧 Требования

- **Min SDK:** 21 (Android 5.0+)
- **OpenGL ES:** 2.0+
- **Зависимости:** androidx.core, androidx.appcompat

---

## 💡 Часто задаваемые вопросы

**Q: Можно ли использовать в Jetpack Compose?**

A: Да! Используй `AndroidView`:

```kotlin
@Composable
fun SnowBackground() {
    AndroidView(
        factory = { context ->
            SnowView(context).apply {
                layers = 50
                speed = 1.2f
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

**Q: Как изменить направление падения снега?**

A: Сейчас снег падает только вниз (как в iOS/Web версиях). Для изменения направления нужно модифицировать шейдер.

**Q: Работает ли на эмуляторе?**

A: Да, но производительность может быть ниже. Лучше тестировать на реальном устройстве.

**Q: Можно ли использовать несколько SnowView одновременно?**

A: Да, но это нагружает GPU. Лучше использовать один SnowView на весь экран.

---

Удачи с интеграцией! ❄️

