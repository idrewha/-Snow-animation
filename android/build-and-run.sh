#!/bin/bash

# ❄️ Snow Success Android - Build and Run Script
# Автоматическая сборка и установка на устройство

set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "❄️  Snow Success - Android Builder"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Проверка adb
if ! command -v adb &> /dev/null; then
    echo "⚠️  adb не найден. Установи Android SDK Platform Tools."
    echo "   https://developer.android.com/studio/releases/platform-tools"
    exit 1
fi

# Проверка подключенных устройств
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
if [ "$DEVICES" -eq 0 ]; then
    echo "⚠️  Нет подключенных устройств или эмуляторов."
    echo ""
    echo "Варианты:"
    echo "  1. Подключи Android устройство через USB"
    echo "  2. Запусти эмулятор из Android Studio"
    echo "  3. Используй: emulator -avd <имя_эмулятора>"
    exit 1
fi

echo "✅ Найдено устройств: $DEVICES"
echo ""

# Даём права на выполнение gradlew
chmod +x gradlew

# Очистка предыдущей сборки
echo "🧹 Очистка предыдущей сборки..."
./gradlew clean

# Сборка APK
echo ""
echo "🔨 Сборка APK..."
./gradlew assembleDebug

# Проверка успешности сборки
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "❌ Ошибка: APK не найден в $APK_PATH"
    exit 1
fi

echo ""
echo "✅ APK успешно собран: $APK_PATH"
echo ""

# Размер APK
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "📦 Размер APK: $APK_SIZE"
echo ""

# Установка на устройство
echo "📲 Установка на устройство..."
adb install -r "$APK_PATH"

echo ""
echo "🚀 Запуск приложения..."
adb shell am start -n com.snowsuccess/.MainActivity

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✨ Готово! Приложение запущено"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Полезные команды:"
echo "  adb logcat | grep SnowView     # Логи приложения"
echo "  adb shell dumpsys gfxinfo      # Статистика GPU"
echo "  adb uninstall com.snowsuccess  # Удалить приложение"
echo ""

