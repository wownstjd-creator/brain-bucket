$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
# Read only a fixed list of environment settings; never execute .env content.
$envFile = Join-Path $projectRoot '.env'
$allowed = @('DB_USERNAME', 'DB_PASSWORD', 'DATABASE_URL', 'UPLOAD_DIR', 'PORT', 'COOKIE_SECURE')
if (Test-Path $envFile) {
    foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
        if ($line -match '^\s*([A-Z_]+)=(.*)$') {
            $key = $Matches[1]; $value = $Matches[2]
            if ($allowed -contains $key) {
                if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                    $value = $value.Substring(1, $value.Length - 2)
                }
                [Environment]::SetEnvironmentVariable($key, $value, 'Process')
            }
        }
    }
}
if (!$env:DB_USERNAME -or !$env:DB_PASSWORD) {
    throw 'Set DB_USERNAME and DB_PASSWORD in the root .env file (see .env.example), or set environment variables.'
}
Push-Location (Join-Path $projectRoot 'backend')
try {
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'Server startup failed. See the error above.' }
} finally { Pop-Location }
