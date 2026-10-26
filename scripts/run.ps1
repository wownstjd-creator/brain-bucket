param([switch]$Packaged)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java.exe' }
try {
    $versionInfo = New-Object System.Diagnostics.ProcessStartInfo
    $versionInfo.FileName = $java
    $versionInfo.Arguments = '-version'
    $versionInfo.UseShellExecute = $false
    $versionInfo.CreateNoWindow = $true
    $versionInfo.RedirectStandardError = $true
    $versionInfo.RedirectStandardOutput = $true
    $versionProcess = [System.Diagnostics.Process]::Start($versionInfo)
    $javaVersion = $versionProcess.StandardError.ReadToEnd() + $versionProcess.StandardOutput.ReadToEnd()
    $versionProcess.WaitForExit()
    $javaExitCode = $versionProcess.ExitCode
    $versionProcess.Dispose()
} catch {
    throw 'Install JDK 21 or newer and set JAVA_HOME to its installation folder, then reopen PowerShell.'
}
if ($javaExitCode -ne 0 -or $javaVersion -notmatch 'version "(\d+)' -or [int]$Matches[1] -lt 21) {
    throw 'JDK 21 or newer is required. Check JAVA_HOME and java -version, then reopen PowerShell.'
}
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
if ($env:DB_PASSWORD -eq 'replace_with_your_own_password') {
    throw 'Replace DB_PASSWORD in .env with your MySQL password before starting.'
}
Write-Host 'Starting BrainBucket. MySQL must be running and the brain_bucket database must exist.'
if (!$Packaged) { Write-Host 'On the first run Maven downloads dependencies; an internet connection is required.' }
Push-Location (Join-Path $projectRoot 'backend')
try {
    if ($Packaged) {
        $jar = Join-Path $projectRoot 'runtime/brain-bucket.jar'
        if (!(Test-Path -LiteralPath $jar)) { throw 'runtime/brain-bucket.jar is missing. Extract the entire delivery ZIP first.' }
        & $java -jar $jar
    } else {
        & .\mvnw.cmd spring-boot:run
    }
    if ($LASTEXITCODE -ne 0) { throw 'Server startup failed. See the error above.' }
} finally { Pop-Location }
