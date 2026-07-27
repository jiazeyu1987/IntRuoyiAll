param(
    [string]$WorktreeRoot = 'D:\IntRuoyiWorktree\20260727-codex-test-node-chain-build',
    [int]$Slot = 7
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$frontendPort = 8081 + $Slot
$backendPort = 48081 + $Slot
$runtimeDir = Join-Path $WorktreeRoot '.runtime\node-chain-isolated'
$backendScript = Join-Path $WorktreeRoot 'scripts\runtime\start-branch-backend.ps1'
$frontendScript = Join-Path $WorktreeRoot 'scripts\runtime\start-branch-frontend.ps1'
$runnerScript = Join-Path $WorktreeRoot 'IntRuoyiFronted\scripts\codex-test-runner.mjs'
$frontendRoot = Join-Path $WorktreeRoot 'IntRuoyiFronted'

function Assert-PortAvailable {
    param([int]$Port)

    $listeners = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    if ($listeners.Count -gt 0) {
        $processIds = ($listeners | Select-Object -ExpandProperty OwningProcess -Unique) -join ','
        throw "Port $Port is already listening; owning process id(s): $processIds"
    }
}

function Wait-HttpReady {
    param(
        [string]$Url,
        [string]$Name,
        [int]$TimeoutSeconds = 180
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ([DateTime]::UtcNow -lt $deadline)

    throw "$Name did not become ready: $Url"
}

function Write-Utf8Text {
    param(
        [string]$Path,
        [string]$Value
    )

    [System.IO.File]::WriteAllText($Path, $Value, [System.Text.UTF8Encoding]::new($false))
}

Assert-PortAvailable -Port $frontendPort
Assert-PortAvailable -Port $backendPort

New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null

$tokenBytes = New-Object byte[] 32
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $random.GetBytes($tokenBytes)
} finally {
    $random.Dispose()
}
$runnerToken = [Convert]::ToHexString($tokenBytes).ToLowerInvariant()
Write-Utf8Text -Path (Join-Path $runtimeDir 'runner-token.txt') -Value $runnerToken

$environmentNames = @(
    'CODEX_TEST_RUNNER_TOKEN',
    'CODEX_TEST_RUNNER_ON_DEMAND_ENABLED',
    'CODEX_TEST_API_BASE',
    'CODEX_TEST_FRONTEND_BASE_URL',
    'CODEX_TEST_TENANT_ID',
    'CODEX_TEST_WORKDIR',
    'CODEX_CLI_COMMAND',
    'CODEX_TEST_RUNNER_NAME',
    'CODEX_TEST_MAX_PARALLELISM',
    'CODEX_TEST_CLAIM_CAPACITY',
    'CODEX_TEST_POLL_INTERVAL_MS',
    'CODEX_TEST_HEARTBEAT_INTERVAL_MS',
    'CODEX_TEST_API_TIMEOUT_MS',
    'YUDAO_CODEX_TEST_ARTIFACT_TEMP_DIR'
)
$oldEnvironment = @{}
foreach ($name in $environmentNames) {
    $oldEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    $env:CODEX_TEST_RUNNER_TOKEN = $runnerToken
    $env:CODEX_TEST_RUNNER_ON_DEMAND_ENABLED = 'false'
    $env:CODEX_TEST_API_BASE = "http://127.0.0.1:$backendPort/admin-api"
    $env:CODEX_TEST_FRONTEND_BASE_URL = "http://127.0.0.1:$frontendPort"
    $env:YUDAO_CODEX_TEST_ARTIFACT_TEMP_DIR = Join-Path $runtimeDir 'artifacts'

    $backendProcess = Start-Process `
        -FilePath 'powershell.exe' `
        -ArgumentList @(
            '-NoProfile',
            '-ExecutionPolicy',
            'Bypass',
            '-File',
            $backendScript,
            '-Slot',
            [string]$Slot
        ) `
        -WorkingDirectory $WorktreeRoot `
        -RedirectStandardOutput (Join-Path $runtimeDir 'backend.stdout.log') `
        -RedirectStandardError (Join-Path $runtimeDir 'backend.stderr.log') `
        -WindowStyle Hidden `
        -PassThru
    Write-Utf8Text -Path (Join-Path $runtimeDir 'backend-wrapper.pid') -Value ([string]$backendProcess.Id)

    $frontendProcess = Start-Process `
        -FilePath 'powershell.exe' `
        -ArgumentList @(
            '-NoProfile',
            '-ExecutionPolicy',
            'Bypass',
            '-File',
            $frontendScript,
            '-Slot',
            [string]$Slot
        ) `
        -WorkingDirectory $WorktreeRoot `
        -RedirectStandardOutput (Join-Path $runtimeDir 'frontend.stdout.log') `
        -RedirectStandardError (Join-Path $runtimeDir 'frontend.stderr.log') `
        -WindowStyle Hidden `
        -PassThru
    Write-Utf8Text -Path (Join-Path $runtimeDir 'frontend-wrapper.pid') -Value ([string]$frontendProcess.Id)

    Wait-HttpReady -Url "http://127.0.0.1:$backendPort/actuator/health" -Name 'Backend'
    Wait-HttpReady -Url "http://127.0.0.1:$frontendPort/" -Name 'Frontend'

    $env:CODEX_TEST_TENANT_ID = '1'
    $env:CODEX_TEST_WORKDIR = $WorktreeRoot
    $env:CODEX_CLI_COMMAND = 'codex.cmd'
    $env:CODEX_TEST_RUNNER_NAME = "node-chain-slot-$Slot-runner"
    $env:CODEX_TEST_MAX_PARALLELISM = '2'
    $env:CODEX_TEST_CLAIM_CAPACITY = '2'
    $env:CODEX_TEST_POLL_INTERVAL_MS = '200'
    $env:CODEX_TEST_HEARTBEAT_INTERVAL_MS = '3000'
    $env:CODEX_TEST_API_TIMEOUT_MS = '30000'

    $runnerProcess = Start-Process `
        -FilePath 'node.exe' `
        -ArgumentList @($runnerScript, '--loop') `
        -WorkingDirectory $frontendRoot `
        -RedirectStandardOutput (Join-Path $runtimeDir 'runner.stdout.log') `
        -RedirectStandardError (Join-Path $runtimeDir 'runner.stderr.log') `
        -WindowStyle Hidden `
        -PassThru
    Write-Utf8Text -Path (Join-Path $runtimeDir 'runner.pid') -Value ([string]$runnerProcess.Id)

    Start-Sleep -Seconds 8
    if ($runnerProcess.HasExited) {
        $stderrTail = if (Test-Path -LiteralPath (Join-Path $runtimeDir 'runner.stderr.log')) {
            (Get-Content -LiteralPath (Join-Path $runtimeDir 'runner.stderr.log') -Encoding utf8 -Tail 20) -join "`n"
        } else {
            ''
        }
        throw "Isolated Runner exited during startup: $stderrTail"
    }

    [pscustomobject]@{
        status = 'started'
        slot = $Slot
        frontendUrl = "http://127.0.0.1:$frontendPort"
        backendUrl = "http://127.0.0.1:$backendPort"
        backendWrapperPid = $backendProcess.Id
        frontendWrapperPid = $frontendProcess.Id
        runnerPid = $runnerProcess.Id
        runnerName = $env:CODEX_TEST_RUNNER_NAME
        runtimeDir = $runtimeDir
    } | ConvertTo-Json -Compress
} finally {
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $oldEnvironment[$name], 'Process')
    }
}
