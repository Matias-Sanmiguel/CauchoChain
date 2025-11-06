#!/bin/bash
# Script para prender Redis en Docker y la TUI de CauchoChain

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REDIS_CONTAINER="redis-cauchochain"
REDIS_IMAGE="redis-cauchochain:latest"
REDIS_PORT=6379

echo "🚀 ================================"
echo "🚀 Iniciando CauchoChain + Redis"
echo "🚀 ================================"
echo ""

# Verificar si Docker está corriendo
if ! command -v docker &> /dev/null; then
    echo "❌ Docker no está instalado o no está en el PATH"
    exit 1
fi

echo "🐳 Compilando imagen Docker de Redis..."
docker build -f "$SCRIPT_DIR/Dockerfile.redis" -t "$REDIS_IMAGE" "$SCRIPT_DIR"

if [ $? -ne 0 ]; then
    echo "❌ Error compilando imagen Docker de Redis"
    exit 1
fi

echo "✅ Imagen Docker compilada!"
echo ""
echo "📦 Verificando contenedor Redis..."

# Detener y remover contenedor anterior si existe
if docker ps -a --format '{{.Names}}' | grep -q "^${REDIS_CONTAINER}$"; then
    echo "   Deteniendo contenedor Redis anterior..."
    docker stop "$REDIS_CONTAINER" 2>/dev/null || true
    docker rm "$REDIS_CONTAINER" 2>/dev/null || true
fi

echo "   Iniciando Redis en Docker..."
docker run -d \
    --name "$REDIS_CONTAINER" \
    -p "$REDIS_PORT:6379" \
    "$REDIS_IMAGE"

echo "   ⏳ Esperando a que Redis esté listo..."
for i in {1..30}; do
    if docker exec "$REDIS_CONTAINER" redis-cli ping > /dev/null 2>&1; then
        echo "   ✅ Redis está listo!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "   ❌ Redis no respondió en tiempo"
        exit 1
    fi
    sleep 1
done

echo ""
echo "🔧 Compilando CauchoChain..."
cd "$SCRIPT_DIR"
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Error en la compilación"
    docker stop "$REDIS_CONTAINER" 2>/dev/null || true
    docker rm "$REDIS_CONTAINER" 2>/dev/null || true
    exit 1
fi

echo "✅ Compilación exitosa!"
echo ""
echo "📋 Redis disponible en: localhost:$REDIS_PORT"
echo "📋 Para ver logs en otra terminal:"
echo "   ./monitor-logs.sh"
echo ""
echo "🎨 Iniciando TUI..."
echo ""

mvn exec:java -Dexec.mainClass="BlockchainTUIDemo"

# Limpiar al salir
echo ""
echo "🛑 Deteniendo Redis..."
docker stop "$REDIS_CONTAINER" 2>/dev/null || true
docker rm "$REDIS_CONTAINER" 2>/dev/null || true
echo "✅ Limpieza completada"
