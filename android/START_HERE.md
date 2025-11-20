# 🚀 START HERE - Android Quick Guide

## ⚡ Самый быстрый способ запустить

### Вариант 1: Автоматический скрипт (1 команда)

```bash
./build-and-run.sh
```

Готово! Приложение соберётся и запустится автоматически.

---

### Вариант 2: Android Studio (визуально)

1. Открой Android Studio
2. File → Open → Выбери папку `android/`
3. Нажми ▶️ Run

---

### Вариант 3: Вручную через терминал

```bash
# Собрать
./gradlew assembleDebug

# Установить
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Запустить
adb shell am start -n com.snowsuccess/.MainActivity
```

---

## 📖 Полная документация

- [README.md](README.md) - Полное описание
- [QUICKSTART.md](QUICKSTART.md) - Быстрый старт
- [USAGE_GUIDE.md](USAGE_GUIDE.md) - Примеры использования

---

## 🎯 Главное

**SnowView.kt** - это единственный файл, который тебе нужен!

Скопируй его в свой проект и используй:

```xml
<com.yourpackage.SnowView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:snowLayers="50" />
```

Или программно:

```kotlin
val snowView = SnowView(context)
snowView.layers = 50
rootLayout.addView(snowView, 0)
```

---

## ✅ Требования

- Android 5.0+ (API 21+)
- OpenGL ES 2.0+
- 1GB RAM минимум

---

## 💡 Нужна помощь?

1. Проверь [QUICKSTART.md](QUICKSTART.md)
2. Смотри [USAGE_GUIDE.md](USAGE_GUIDE.md)
3. Читай [../TESTING.md](../TESTING.md)

---

Удачи! ❄️

