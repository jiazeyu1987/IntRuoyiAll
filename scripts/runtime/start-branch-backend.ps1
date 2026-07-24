param(
    [int]$Slot = 0,
    [switch]$Build,
    [string[]]$ExtraArgs = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\branch-runtime-profile.ps1"

$repoRoot = Get-CurrentRepoRoot
$branch = Get-GitValue -RepoRoot $repoRoot -Arguments @('branch', '--show-current')
$profile = Resolve-BranchRuntimeProfile -RepoRoot $repoRoot -Branch $branch
$ports = Get-BranchRuntimePorts -Profile $profile -Slot $Slot
$backendRoot = Join-Path $repoRoot 'IntRuoyiBackend'
$jarPath = Join-Path $backendRoot 'yudao-server\target\yudao-server-exec.jar'

$listeners = @(Get-NetTCPConnection -LocalPort $ports.BackendPort -State Listen -ErrorAction SilentlyContinue)
if ($listeners.Count -gt 0) {
    $pids = ($listeners | Select-Object -ExpandProperty OwningProcess -Unique) -join ', '
    throw "Backend port $($ports.BackendPort) is already listening; owning process id(s): $pids. Stop the owned process explicitly before restarting."
}

if ($Build) {
    Push-Location $backendRoot
    try {
        & mvn.cmd -pl yudao-server -am -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw 'Backend package command failed.'
        }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $jarPath)) {
    throw "Missing backend executable jar: $jarPath. Run with -Build or package yudao-server first."
}

$javaArgs = @(
    '-jar',
    $jarPath,
    "--server.port=$($ports.BackendPort)",
    '--spring.profiles.active=local'
) + $ExtraArgs

Write-Host "Starting $($profile.Name) backend on $($ports.BackendPort)."
& java @javaArgs
exit $LASTEXITCODE
