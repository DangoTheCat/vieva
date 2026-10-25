#!/usr/bin/env bash
# NOTE: Ensure executable permission before running: chmod +x scripts/docker-smoke.sh
set -euo pipefail

# ==============================================================================
# Vieva Docker Smoke Test (Bash)
# Non-destructive smoke check for Docker Compose configuration and service health.
# ==============================================================================

echo "==> [1/3] Validating Docker Compose configuration..."
docker compose config --quiet
echo "    Docker Compose configuration is valid."

echo "==> [2/3] Checking container status..."
docker compose ps

echo "==> [3/3] Verifying Backend readiness..."
docker compose exec -T backend wget -qO- http://localhost:8080/api/v1/health/readiness
echo ""
echo "    Backend readiness check passed."

# Optional TTS health check if tts service is active
if docker compose ps --services --filter "status=running" | grep -q "^tts-service$"; then
    echo "==> Checking optional TTS service health..."
    docker compose exec -T tts-service curl -sf http://localhost:8001/health
    echo ""
    echo "    TTS service check finished."
fi

echo "==> All Docker smoke checks completed successfully!"
