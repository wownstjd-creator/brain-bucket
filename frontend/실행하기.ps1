$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$nodeCommand = Get-Command node -ErrorAction SilentlyContinue
if (-not $nodeCommand) { throw 'Node.js is required. Install Node.js and try again.' }
$npmCli = Join-Path (Split-Path $nodeCommand.Source) 'node_modules\npm\bin\npm-cli.js'
if (-not (Test-Path -LiteralPath $npmCli)) { throw 'npm was not found. Reinstall Node.js or run npm install manually.' }
if (-not (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'node_modules\vite\bin\vite.js'))) {
    & $nodeCommand.Source $npmCli install
    if ($LASTEXITCODE -ne 0) { throw 'Dependency installation failed.' }
}
& $nodeCommand.Source $npmCli run dev
if ($LASTEXITCODE -ne 0) { throw 'Development server stopped with an error.' }
