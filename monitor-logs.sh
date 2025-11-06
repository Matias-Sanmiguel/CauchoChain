#!/bin/bash
# Script simplificado para monitorear logs de Redis
# Uso: ./monitor-logs.sh

set -e

REDIS_CONTAINER="redis-cauchochain"
LOG_KEY="blockchain:logs"
POLL_INTERVAL="${1:-1}"
SHOW_LAST="${2:-20}"

if ! docker ps --format '{{.Names}}' | grep -q "^${REDIS_CONTAINER}$"; then
    echo "❌ Contenedor Redis no está corriendo"
    echo "   Ejecuta: ./start-all.sh"
    exit 1
fi

echo ""
echo "=== Monitor de Logs Redis (CauchoChain) ==="
echo "📋 Key: $LOG_KEY"
echo "⏱️  Poll: ${POLL_INTERVAL}s"
echo "📊 Mostrando últimos: $SHOW_LAST"
echo "🛑 Presiona Ctrl+C para salir"
echo ""

redis_cmd() {
    docker exec -i "$REDIS_CONTAINER" redis-cli --raw "$@"
}

print_last_n() {
    local n=$1
    local count=$(redis_cmd LLEN "$LOG_KEY" 2>/dev/null || echo 0)
    if [ "$count" -eq 0 ]; then
        echo "(sin logs aún)"
        return
    fi
    redis_cmd LRANGE "$LOG_KEY" -"$n" -1 | nl -v $((count - n + 1)) -w4 -s'. '
}

trap 'echo ""; echo "Saliendo..."; exit 0' INT TERM

# Mostrar últimos logs iniciales
echo "Últimos $SHOW_LAST logs:"
print_last_n "$SHOW_LAST"
echo ""

# Obtener cursor inicial
last_len=$(redis_cmd LLEN "$LOG_KEY" 2>/dev/null || echo 0)
cursor=$last_len

echo "Monitoreando..."
echo ""

while true; do
    sleep "$POLL_INTERVAL"
    curr_len=$(redis_cmd LLEN "$LOG_KEY" 2>/dev/null || echo 0)

    if [ "$curr_len" -gt "$cursor" ]; then
        start=$cursor
        end=$((curr_len - 1))
        new_logs=$(redis_cmd LRANGE "$LOG_KEY" "$start" "$end" 2>/dev/null || true)

        if [ -n "$new_logs" ]; then
            echo "$(date '+%H:%M:%S') - Nuevos logs:"
            idx=$start
            while IFS= read -r line; do
                printf "  %4d. %s\n" $((idx + 1)) "$line"
                idx=$((idx + 1))
            done <<< "$new_logs"
            echo ""
        fi
        cursor=$curr_len
    fi
done

