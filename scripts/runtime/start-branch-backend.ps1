param(
    [Nullable[int]]$Slot = $null,
    [switch]$Build,
    [string[]]$ExtraArgs = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\branch-runtime-profile.ps1"

$repoRoot = Get-CurrentRepoRoot
$branch = Get-GitValue -RepoRoot $repoRoot -Arguments @('branch', '--show-current')
$context = Resolve-BranchRuntimeContext -RepoRoot $repoRoot -Branch $branch -RequestedSlot $Slot
$profile = $context.Profile
$ports = $context.Ports
$backendRoot = Join-Path $repoRoot 'IntRuoyiBackend'
$sourceJarPath = Join-Path $backendRoot 'yudao-server\target\yudao-server-exec.jar'
$runtimeDir = Join-Path $repoRoot "output\runtime\$($profile.Name)"
$runtimeLogDir = Join-Path $runtimeDir 'logs'

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

if (-not (Test-Path $sourceJarPath)) {
    throw "Missing backend executable jar: $sourceJarPath. Run with -Build or package yudao-server first."
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$runtimeJarPath = Join-Path $runtimeDir "backend-$timestamp.jar"
$runtimeLogPath = Join-Path $runtimeLogDir 'yudao-server.log'
New-Item -ItemType Directory -Force -Path $runtimeLogDir | Out-Null
Copy-Item -LiteralPath $sourceJarPath -Destination $runtimeJarPath -Force

$javaArgs = @(
    '-jar',
    $runtimeJarPath,
    "--server.port=$($ports.BackendPort)",
    '--spring.profiles.active=local',
    "--logging.file.name=$runtimeLogPath",
    "--yudao.runtime-control.storage-guard.log-dir=$runtimeLogDir"
) + $ExtraArgs

Write-Host "Starting $($profile.Name) backend on $($ports.BackendPort)."
& java @javaArgs
exit $LASTEXITCODE
