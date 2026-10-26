param([Parameter(Mandatory = $true)][string]$Target)
$ErrorActionPreference = 'Stop'
$sourceRoot = [IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent)).TrimEnd([char[]]@([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar))
$targetRoot = [IO.Path]::GetFullPath($Target).TrimEnd([char[]]@([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar))
if ($sourceRoot -eq $targetRoot) { throw 'Extract the final ZIP to a separate folder, then target your existing repository.' }
if (!(Test-Path -LiteralPath (Join-Path $targetRoot '.git')) -or
    !(Test-Path -LiteralPath (Join-Path $targetRoot 'backend/pom.xml')) -or
    !(Test-Path -LiteralPath (Join-Path $targetRoot 'frontend/package.json'))) {
    throw 'Target must be your existing brain-bucket Git repository root (containing .git, backend and frontend).'
}
# Stop both the app and frontend dev server before running. No Git or database commands are run here.
$manifest = Get-Content -LiteralPath (Join-Path $sourceRoot 'SHA256SUMS.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$obsolete = @(Get-Content -LiteralPath (Join-Path $sourceRoot 'scripts/obsolete-files.json') -Raw -Encoding UTF8 | ConvertFrom-Json)
$files = @($manifest.PSObject.Properties.Name) + @('SHA256SUMS.json')
function SafePath([string]$root, [string]$relative) {
    if ([IO.Path]::IsPathRooted($relative) -or $relative -match '(^|[\\/])\.\.([\\/]|$)' -or
        $relative -match '(^|[\\/])(\.git|\.env|uploads|node_modules|target|backups)([\\/]|$)') {
        throw "Unsafe release path: $relative"
    }
    $full = [IO.Path]::GetFullPath((Join-Path $root $relative))
    if (!$full.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Path escapes repository: $relative"
    }
    return $full
}
# Verify the complete download before changing anything in the target.
foreach ($entry in $manifest.PSObject.Properties) {
    $file = SafePath $sourceRoot $entry.Name
    if (!(Test-Path -LiteralPath $file -PathType Leaf) -or
        (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne $entry.Value) {
        throw "Release verification failed: $($entry.Name). Extract the ZIP again. Target was not changed."
    }
}
foreach ($relative in @($files) + @($obsolete)) {
    $null = SafePath $targetRoot $relative
}
$backupRoot = $targetRoot + '.before-final-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,6)
New-Item -ItemType Directory -Path $backupRoot | Out-Null
# Back up every file that will be overwritten or removed, outside the Git repository.
foreach ($relative in (@($files) + @($obsolete) | Select-Object -Unique)) {
    $existing = SafePath $targetRoot $relative
    if (Test-Path -LiteralPath $existing -PathType Leaf) {
        $destination = SafePath $backupRoot $relative
        New-Item -ItemType Directory -Force -Path (Split-Path $destination -Parent) | Out-Null
        Copy-Item -LiteralPath $existing -Destination $destination -Force
    }
}
foreach ($relative in $files) {
    $destination = SafePath $targetRoot $relative
    New-Item -ItemType Directory -Force -Path (Split-Path $destination -Parent) | Out-Null
    Copy-Item -LiteralPath (SafePath $sourceRoot $relative) -Destination $destination -Force
}
foreach ($relative in $obsolete) {
    $file = SafePath $targetRoot $relative
    if (Test-Path -LiteralPath $file -PathType Leaf) { Remove-Item -LiteralPath $file -Force }
}
# Only prune empty obsolete directories; unknown local files are preserved.
foreach ($relative in @('frontend/backend','frontend/scripts','frontend/docs')) {
    $directory = Join-Path $targetRoot $relative
    if (Test-Path -LiteralPath $directory -PathType Container) {
        Get-ChildItem -LiteralPath $directory -Directory -Recurse |
            Sort-Object { $_.FullName.Length } -Descending | ForEach-Object {
                if (@(Get-ChildItem -LiteralPath $_.FullName -Force).Count -eq 0) { Remove-Item -LiteralPath $_.FullName -Force }
            }
        if (@(Get-ChildItem -LiteralPath $directory -Force).Count -eq 0) { Remove-Item -LiteralPath $directory -Force }
        else { Write-Warning "Local files remain in $relative. They were preserved; the final project does not use them." }
    }
}
Write-Host "Applied successfully to: $targetRoot"
Write-Host "Replaced/deleted file backup: $backupRoot"
Write-Host '.git, .env, uploaded files and MySQL data were not changed. This is a source backup, not a database backup.'
Write-Host 'Next: run 2-start.cmd in the EXISTING repository, then review GitHub Desktop changes and commit/push.'
