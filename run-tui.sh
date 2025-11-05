#!/bin/bash
# Script para ejecutar la TUI de CauchoChain

cd "$(dirname "$0")"

echo "🚀 Compilando CauchoChain..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Compilación exitosa!"
    echo "🎨 Iniciando TUI..."
    echo ""
    mvn exec:java -Dexec.mainClass="BlockchainTUIDemo"
else
    echo "❌ Error en la compilación"
    exit 1
fi

