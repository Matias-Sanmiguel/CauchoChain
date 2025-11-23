#!/bin/bash
# Script para ejecutar la demo de CauchoChain P2P

if [ -z "$1" ]; then
  PORT=5000
else
  PORT=$1
fi

echo "Iniciando CauchoChain en puerto $PORT..."
java -cp bin:lib/lanterna-3.1.1.jar BlockchainTUIDemo $PORT
