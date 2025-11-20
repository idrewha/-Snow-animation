# 🔧 Диагностика проблем запуска

## Шаг 1: Проверь логи в Android Studio

### В Android Studio:

1. **Открой Logcat** (внизу экрана)
2. **Выбери своё устройство** в dropdown
3. **Найди ошибки** - они будут красного цвета

### Что искать:

#### Ошибка 1: "Unable to start activity"
```
FATAL EXCEPTION: main
android.content.ActivityNotFoundException
```
**Причина:** Неправильный AndroidManifest.xml  
**Решение:** Смотри ниже исправление

#### Ошибка 2: "Resources not found"
```
android.content.res.Resources$NotFoundException
```
**Причина:** Отсутствуют ресурсы (иконки, строки и т.д.)  
**Решение:** Rebuild проект

#### Ошибка 3: "OpenGL ES not supported"
```
java.lang.RuntimeException: createContext failed
```
**Причина:** Эмулятор не поддерживает OpenGL ES  
**Решение:** Измени Graphics в настройках эмулятора

---

## Шаг 2: Проверь что Build успешен

В терминале Android Studio (или внешнем терминале):

```bash
cd /Users/andrey.alekseev6/Documents/Cursor/snow-success-cursor/android
./gradlew clean
./gradlew assembleDebug --stacktrace
```

Если видишь `BUILD SUCCESSFUL` - значит код компилируется.

---

## Шаг 3: Проверь что эмулятор поддерживает OpenGL

1. **Device Manager** → твой эмулятор → **⚙️ (Edit)**
2. **Show Advanced Settings**
3. **Graphics:** должно быть **"Hardware - GLES 3.0"** или **"Automatic"**
4. Если стоит "Software" - измени на Hardware
5. **Wipe Data** (очистить данные эмулятора)
6. Запусти заново

---

## Быстрое исправление: Упрощённая версия

Если ничего не помогает, давай создадим минимальную версию без зависимостей от ресурсов:

### 1. Временно убери XML attributes из layout

Открой `app/src/main/res/layout/activity_main.xml` и замени SnowView на:

```xml
<com.snowsuccess.SnowView
    android:id="@+id/snowView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

(Убери все `app:snow*` атрибуты)

### 2. Rebuild проект

Build → Clean Project → Rebuild Project

### 3. Запусти

Нажми ▶️ Run

---

## Получить детальные логи

В терминале:

```bash
# Смотреть все логи приложения
adb logcat | grep -i "snowsuccess"

# Смотреть только ошибки
adb logcat *:E | grep -i "snow"

# Очистить логи и смотреть заново
adb logcat -c && adb logcat
```

---

## Альтернатива: Запуск через командную строку

Попробуй собрать и установить вручную:

```bash
cd /Users/andrey.alekseev6/Documents/Cursor/snow-success-cursor/android

# Очистка
./gradlew clean

# Сборка
./gradlew assembleDebug

# Если сборка успешна, APK будет здесь:
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Установка
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Запуск
adb shell am start -n com.snowsuccess/.MainActivity

# Смотрим логи
adb logcat | grep -E "(SnowView|MainActivity|AndroidRuntime)"
```

---

## Скопируй и пришли мне вывод:

```bash
# Вариант 1: Через командную строку
cd /Users/andrey.alekseev6/Documents/Cursor/snow-success-cursor/android
./gradlew assembleDebug 2>&1 | tail -50

# Вариант 2: Логи из Android Studio
# После запуска скопируй красные строки из Logcat
```

---

## Частые проблемы и решения

### "Installed on device, but won't start"

**Попробуй:**
```bash
adb shell pm clear com.snowsuccess
adb uninstall com.snowsuccess
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### "App keeps crashing immediately"

Смотри Logcat и найди строку с `FATAL EXCEPTION`, скопируй всё что после неё.

### Эмулятор вообще не запускается

1. Убей все процессы:
   ```bash
   killall qemu-system-x86_64
   ```

2. Запусти эмулятор из командной строки:
   ```bash
   ~/Library/Android/sdk/emulator/emulator -avd Pixel_6_API_34
   ```

---

## Нужна помощь?

Скопируй и пришли:
1. ❌ Ошибки из Logcat (красный текст)
2. 🔨 Вывод `./gradlew assembleDebug`
3. 📱 Какое устройство/эмулятор используешь

Я помогу найти проблему! 🔍

