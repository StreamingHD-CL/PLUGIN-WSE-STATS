#!/usr/bin/env bash
set -euo pipefail

# Directorio base del proyecto (raíz)
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

mkdir -p "$BASE_DIR/build"

# Compilación
javac -classpath "$BASE_DIR/lib/*" -d "$BASE_DIR/build" $(find "$BASE_DIR/src" -name '*.java')

# Empaquetado JAR
jar cf "$BASE_DIR/MetricaSHD.jar" -C "$BASE_DIR/build" .

echo "JAR generado en $BASE_DIR/MetricaSHD.jar" 