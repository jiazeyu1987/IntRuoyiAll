param(
    [string]$WorktreeRoot = 'D:\IntRuoyiWorktree\20260727-codex-test-node-chain-build',
    [int]$Slot = 7
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$frontendPort = 8081 + $Slot
$backendPort = 48081 + $Slot
$runtimeDir = Join-Path $WorktreeRoot '.runtime\node-chain-isolated'

function Stop-OwnedProcess {
    param(
        [int]$ProcessId,
        [string]$ExpectedCommandFragment
    )

    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$ProcessId" -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        return
    }
    if (-not $process.CommandLine -or -not $process.CommandLine.Contains($ExpectedCommandFragment)) {
        throw "Process $ProcessId is not owned by this runtime: $($process.CommandLine)"
    }
    Stop-Process -Id $ProcessId -Force
}

foreach ($port in @($frontendPort, $backendPort)) {
    $listeners = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
    foreach ($listener in $listeners) {
        Stop-OwnedProcess -ProcessId $listener.OwningProcess -ExpectedCommandFragment $WorktreeRoot
    }
}

$runnerPidPath = Join-Path $runtimeDir 'runner.pid'
if (Test-Path -LiteralPath $runnerPidPath) {
    $runnerPid = (Get-Content -LiteralPath $runnerPidPath -Encoding utf8).Trim()
    if ($runnerPid -match '^\d+$') {
        Stop-OwnedProcess -ProcessId ([int]$runnerPid) -ExpectedCommandFragment $WorktreeRoot
    }
}

foreach ($pidFile in @('frontend-wrapper.pid', 'backend-wrapper.pid')) {
    $path = Join-Path $runtimeDir $pidFile
    if (-not (Test-Path -LiteralPath $path)) {
        continue
    }
    $wrapperPid = (Get-Content -LiteralPath $path -Encoding utf8).Trim()
    if ($wrapperPid -match '^\d+$') {
        Stop-OwnedProcess -ProcessId ([int]$wrapperPid) -ExpectedCommandFragment $WorktreeRoot
    }
}

Start-Sleep -Seconds 2
$remaining = @(Get-NetTCPConnection -State Listen -LocalPort $frontendPort, $backendPort -ErrorAction SilentlyContinue)
if ($remaining.Count -gt 0) {
    throw "Runtime ports were not released: $($remaining.LocalPort -join ',')"
}

[pscustomobject]@{
    status = 'stopped'
    slot = $Slot
    frontendPort = $frontendPort
    backendPort = $backendPort
} | ConvertTo-Json -Compress
