#!/bin/bash

# Скрипт для быстрого запуска локального сервера

echo "🌨️  Snow Animation - WebGL версия"
echo "=================================="
echo ""
echo "Запускаю локальный сервер..."
echo ""

# Проверка доступности Python
if command -v python3 &> /dev/null; then
    echo "✅ Используется Python3"
    echo "🌐 Открой браузер: http://localhost:8000"
    echo ""
    echo "Для остановки нажми Ctrl+C"
    echo ""
    python3 -m http.server 8000
elif command -v python &> /dev/null; then
    echo "✅ Используется Python"
    echo "🌐 Открой браузер: http://localhost:8000"
    echo ""
    echo "Для остановки нажми Ctrl+C"
    echo ""
    python -m SimpleHTTPServer 8000
else
    echo "❌ Python не найден!"
    echo ""
    echo "Альтернативные способы запуска:"
    echo "1. npx http-server -p 8000"
    echo "2. Открой index.html напрямую в браузере (может не работать из-за CORS)"
    exit 1
fi






