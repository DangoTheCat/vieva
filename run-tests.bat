@echo off
REM ==============================================================================
REM Chay test case du an AIVES (vieva) bang Docker
REM Khong can cai dat Java 17 hay Maven tren may host
REM ==============================================================================

if "%~1"=="" (
    echo [AIVES] Running full unit & slice test suite via Docker...
    docker compose run --rm test
) else (
    echo [AIVES] Running tests for: %1...
    docker compose run --rm test mvn test -Dtest="%~1"
)
