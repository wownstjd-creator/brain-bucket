$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    Push-Location frontend
    try { & npm.cmd ci; if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }; & npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' } } finally { Pop-Location }
    $staticTarget = Join-Path $projectRoot 'backend/src/main/resources/static'
    $staticTarget = [IO.Path]::GetFullPath($staticTarget)
    $expectedTarget = [IO.Path]::GetFullPath((Join-Path $projectRoot 'backend/src/main/resources/static'))
    if ($staticTarget -ne $expectedTarget -or !$staticTarget.StartsWith([IO.Path]::GetFullPath($projectRoot).TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Unexpected static output path' }
    if (Test-Path -LiteralPath $staticTarget) { Remove-Item -LiteralPath $staticTarget -Recurse -Force }
    New-Item -ItemType Directory -Path $staticTarget -Force | Out-Null
    Copy-Item 'frontend/dist/*' $staticTarget -Recurse -Force
    Push-Location backend
    try { & .\mvnw.cmd clean verify; if ($LASTEXITCODE -ne 0) { throw 'Backend build/test failed' } } finally { Pop-Location }
    $runtimeTarget = Join-Path $projectRoot 'runtime'
    New-Item -ItemType Directory -Path $runtimeTarget -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'backend/target/brain-bucket-backend-0.0.1-SNAPSHOT.jar') -Destination (Join-Path $runtimeTarget 'brain-bucket.jar') -Force
    Write-Host 'Build complete. The packaged launcher now uses the new runtime/brain-bucket.jar.'
} finally { Pop-Location }
