#!/bin/bash
# Script para ejecutar la GUI (TUI) de CauchoChain

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "🚀 ================================"
echo "🚀 Iniciando CauchoChain GUI"
echo "🚀 ================================"
echo ""

echo "🔧 Compilando CauchoChain..."
cd "$SCRIPT_DIR"
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Error en la compilación"
    exit 1
fi

echo "✅ Compilación exitosa!"
echo ""
echo "🎨 Iniciando GUI (TUI)..."
echo ""

mvn exec:java -Dexec.mainClass="BlockchainTUIDemo"

