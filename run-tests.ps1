# ==============================================================================
# Chay test case du an AIVES (vieva) bang Docker (PowerShell)
# ==============================================================================
param (
    [string]$TestClass = ""
)

if ($TestClass -eq "") {
    Write-Host "[AIVES] Running full unit & slice test suite via Docker..." -ForegroundColor Cyan
    docker compose run --rm test
} else {
    Write-Host "[AIVES] Running tests for: $TestClass..." -ForegroundColor Cyan
    docker compose run --rm test mvn test -Dtest="$TestClass"
}
