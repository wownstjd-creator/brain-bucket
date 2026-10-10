$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    Push-Location frontend
    try { & npm.cmd ci; if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }; & npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' } } finally { Pop-Location }
    $staticTarget = Join-Path $projectRoot 'backend/src/main/resources/static'
    if (Test-Path $staticTarget) { Remove-Item $staticTarget -Recurse -Force }
    New-Item -ItemType Directory -Path $staticTarget -Force | Out-Null
    Copy-Item 'frontend/dist/*' $staticTarget -Recurse -Force
    Push-Location backend
    try { & .\mvnw.cmd clean verify; if ($LASTEXITCODE -ne 0) { throw 'Backend build/test failed' } } finally { Pop-Location }
} finally { Pop-Location }
