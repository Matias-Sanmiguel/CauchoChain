#!/usr/bin/env bash
# language: bash
# File: `monitor_redis_logs.sh`
# Uso: ./monitor_redis_logs.sh [LOG_KEY] [POLL_INTERVAL] [SHOW_LAST] [REDIS_CONTAINER]
# Ejemplo: ./monitor_redis_logs.sh blockchain:logs 1 20
set -uuo pipefail

LOG_KEY="${1:-blockchain:logs}"
POLL_INTERVAL="${2:-1}"   # segundos entre polls
SHOW_LAST="${3:-20}"
USER_CONTAINER="${4:-}"

print_header() {
  echo
  echo "=== Monitor Redis Logs ==="
  echo "Key: $LOG_KEY   Poll: ${POLL_INTERVAL}s   Mostrar últimos: $SHOW_LAST"
  echo "Presiona Ctrl+C para salir"
  echo
}

detect_container() {
  if [ -n "$USER_CONTAINER" ]; then
    echo "$USER_CONTAINER"
    return
  fi
  # intentar detectar por imagen o por nombre
  local c
  c=$(docker ps --format '{{.Names}}' --filter ancestor=redis | head -n1 2>/dev/null || true)
  if [ -n "$c" ]; then
    echo "$c"
    return
  fi
  c=$(docker ps --format '{{.Names}}' --filter name=redis | head -n1 2>/dev/null || true)
  echo "$c"
}

# Ejecuta comando redis-cli ya sea dentro de contenedor o local
run_redis_cmd() {
  local cmd="$1"; shift
  if [ -n "$REDIS_CONTAINER" ]; then
    docker exec -i "$REDIS_CONTAINER" redis-cli --raw "$cmd" "$@"
  else
    redis-cli --raw "$cmd" "$@"
  fi
}

# imprime los últimos N logs
print_last_n() {
  local n="$1"
  echo "=== Últimos $n logs ==="
  # usar índices negativos para eficiencia si redis-cli soporta
  if [ "$n" -gt 0 ]; then
    run_redis_cmd LRANGE "$LOG_KEY" -"$n" -1 | nl -v1 -w3 -s'. '
  else
    echo "(n debe ser > 0)"
  fi
  echo
}

# manejo de Ctrl+C
trap 'echo; echo "Saliendo..."; exit 0' INT TERM

# detectar si hay redis-cli o contenedor
REDIS_CONTAINER="$(detect_container)"
if [ -z "$REDIS_CONTAINER" ] && ! command -v redis-cli >/dev/null 2>&1; then
  echo "No se encontró contenedor Redis ni \`redis-cli\` local. Iniciar Redis o pasar el nombre del contenedor como 4º argumento."
  exit 2
fi

print_header
print_last_n "$SHOW_LAST"

# obtener longitud inicial
len_str="$(run_redis_cmd LLEN "$LOG_KEY" 2>/dev/null || echo 0)"
last_len=0
# asegurar que last_len sea entero
if [[ "$len_str" =~ ^[0-9]+$ ]]; then
  last_len=$len_str
else
  last_len=0
fi

# si la lista es más larga que SHOW_LAST, ajustamos el cursor para no reimprimir todo
if [ "$last_len" -gt "$SHOW_LAST" ]; then
  cursor=$(( last_len - SHOW_LAST ))
else
  cursor=0
fi

# contador de entradas mostradas (índice humano)
total_shown=$cursor

echo "Iniciando monitor (cursor en $cursor, total en lista: $last_len)."

while true; do
  sleep "$POLL_INTERVAL"
  len_str="$(run_redis_cmd LLEN "$LOG_KEY" 2>/dev/null || echo 0)"
  if ! [[ "$len_str" =~ ^[0-9]+$ ]]; then
    echo "Error leyendo longitud de lista. Reintentando..."
    continue
  fi
  curr_len=$len_str

  if [ "$curr_len" -lt "$cursor" ]; then
    # la lista fue recortada en el servidor
    echo
    echo "--- Lista recortada en el servidor (antes $cursor, ahora $curr_len). Reimprimiendo últimos $SHOW_LAST ---"
    print_last_n "$SHOW_LAST"
    if [ "$curr_len" -gt "$SHOW_LAST" ]; then
      cursor=$(( curr_len - SHOW_LAST ))
    else
      cursor=0
    fi
    total_shown=$cursor
    continue
  fi

  if [ "$curr_len" -gt "$cursor" ]; then
    # hay elementos nuevos: fetch desde cursor hasta curr_len-1
    start=$cursor
    end=$(( curr_len - 1 ))
    new_lines="$(run_redis_cmd LRANGE "$LOG_KEY" "$start" "$end" 2>/dev/null || true)"
    if [ -n "$new_lines" ]; then
      # imprimir cada línea con numeración continua
      echo
      echo "+++ Nuevos logs (mostrando $(( end - start + 1 ))) +++"
      # usar while read para conservar saltos y caracteres
      idx=$start
      while IFS= read -r line; do
        printf "%4d. %s\n" $(( idx + 1 )) "$line"
        idx=$(( idx + 1 ))
      done <<< "$new_lines"
      total_shown=$idx
    fi
    cursor=$curr_len
  fi
done
