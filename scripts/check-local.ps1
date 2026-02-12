$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot

try {
    Write-Host "Running unit tests..."
    & .\gradlew.bat :app:testDebugUnitTest
    if ($LASTEXITCODE -ne 0) {
        throw "Unit tests failed."
    }

    Write-Host "Running lint..."
    & .\gradlew.bat :app:lintDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Lint failed."
    }

    Write-Host "Local quality checks passed."
} finally {
    Pop-Location
}
