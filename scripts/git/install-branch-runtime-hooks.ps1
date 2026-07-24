Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (& git rev-parse --show-toplevel).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repoRoot)) {
    throw 'Current directory is not inside a Git repository.'
}

$repoRoot = [System.IO.Path]::GetFullPath($repoRoot)
$hooksDir = Join-Path $repoRoot '.githooks'
if (-not (Test-Path $hooksDir)) {
    throw "Missing hooks directory: $hooksDir"
}

& git -C $repoRoot config core.hooksPath .githooks
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to set core.hooksPath.'
}

$configured = (& git -C $repoRoot config --get core.hooksPath).Trim()
if ($configured -ne '.githooks') {
    throw "Unexpected core.hooksPath: $configured"
}

Write-Output "Installed branch runtime Git hooks for $repoRoot."
