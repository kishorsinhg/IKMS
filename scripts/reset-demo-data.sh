#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR/backend"

mvn -q \
  -Dspring-boot.run.profiles=demo \
  -Dspring-boot.run.arguments="--ikms.demo.mode=reset --ikms.demo.exit-after-run=true" \
  spring-boot:run
