# ==============================================================================
# Vieva Docker Smoke Test (PowerShell)
# Non-destructive smoke check for Docker Compose configuration and service health.
# ==============================================================================

$ErrorActionPreference = "Stop"

Write-Host "==> [1/3] Validating Docker Compose configuration..." -ForegroundColor Cyan
docker compose config --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker Compose configuration validation failed."
}
Write-Host "    Docker Compose configuration is valid." -ForegroundColor Green

Write-Host "==> [2/3] Checking container status..." -ForegroundColor Cyan
docker compose ps
if ($LASTEXITCODE -ne 0) { throw "Could not inspect Compose services." }

Write-Host "==> [3/3] Verifying Backend readiness..." -ForegroundColor Cyan
docker compose exec -T backend wget -qO- http://localhost:8080/api/v1/health/readiness
if ($LASTEXITCODE -ne 0) { throw "Backend readiness check failed." }
Write-Host ""
Write-Host "    Backend readiness check passed." -ForegroundColor Green

# Optional TTS health check if tts service is active
$runningServices = docker compose ps --services --filter "status=running"
if ($LASTEXITCODE -ne 0) { throw "Could not list running Compose services." }
if ($runningServices -split "`r?`n" -contains "tts-service") {
    Write-Host "==> Checking optional TTS service health..." -ForegroundColor Cyan
    docker compose exec -T tts-service curl -sf http://localhost:8001/health
    if ($LASTEXITCODE -ne 0) { throw "TTS health check failed." }
    Write-Host ""
    Write-Host "    TTS service check finished." -ForegroundColor Green
}

Write-Host "==> All Docker smoke checks completed successfully!" -ForegroundColor Green
