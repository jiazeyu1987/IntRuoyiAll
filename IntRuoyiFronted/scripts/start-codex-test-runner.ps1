param(
    [string]$ApiBase = 'http://127.0.0.1:48081/admin-api',
    [string]$FrontendBaseUrl = 'http://127.0.0.1:8081',
    [string]$TenantId = '1',
    [string]$RunnerName = 'local-codex-runner',
    [string]$TokenFile = '',
    [string]$NodeCommand = 'node.exe',
    [string]$CodexCommand = 'codex.cmd',
    [int]$PollIntervalMs = 5000,
    [int]$ApiTimeoutMs = 30000,
    [int]$StartupProbeSeconds = 8,
    [switch]$RestartExisting
)

$ErrorActionPreference = 'Stop'

$frontendRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $frontendRoot
$runnerScript = Join-Path $PSScriptRoot 'codex-test-runner.mjs'
$runtimeDir = Join-Path $workspaceRoot '.runtime\codex-test-runner'
$runnerWorkdir = Join-Path $env:TEMP 'IntRuoyi-codex-test-runner-workspace'
$stdoutLog = Join-Path $runtimeDir 'codex-runner.stdout.log'
$stderrLog = Join-Path $runtimeDir 'codex-runner.stderr.log'
$pidFile = Join-Path $runtimeDir 'codex-runner.pid'

function Assert-CommandExists {
    param([string]$CommandName)
    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "$CommandName is required to start Codex Runner"
    }
}

function Assert-HttpReachable {
    param(
        [string]$Url,
        [string]$Name
    )
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 400) {
            throw "$Name returned HTTP $($response.StatusCode): $Url"
        }
    } catch {
        throw "$Name is not reachable: $Url"
    }
}

function Get-RunnerProcesses {
    Get-CimInstance Win32_Process |
        Where-Object {
            $_.CommandLine -and
            $_.CommandLine.Contains('codex-test-runner.mjs') -and
            $_.CommandLine.Contains('--loop')
        }
}

if (-not (Test-Path -LiteralPath $runnerScript)) {
    throw "Missing runner script: $runnerScript"
}

Assert-CommandExists -CommandName $NodeCommand
Assert-CommandExists -CommandName $CodexCommand
Assert-HttpReachable -Url ($ApiBase -replace '/admin-api$', '/actuator/health') -Name 'Backend health'

if (-not (Test-Path -LiteralPath $runtimeDir)) {
    New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
}
if (-not (Test-Path -LiteralPath $runnerWorkdir)) {
    New-Item -ItemType Directory -Force -Path $runnerWorkdir | Out-Null
}

$runnerToken = $env:CODEX_TEST_RUNNER_TOKEN
if ([string]::IsNullOrWhiteSpace($runnerToken) -and -not [string]::IsNullOrWhiteSpace($TokenFile)) {
    if (-not (Test-Path -LiteralPath $TokenFile)) {
        throw "Missing runner token file: $TokenFile"
    }
    $runnerToken = (Get-Content -Raw -Encoding utf8 -LiteralPath $TokenFile).Trim()
}

$existing = @(Get-RunnerProcesses)
if ($existing.Count -gt 0) {
    if (-not $RestartExisting) {
        throw "Existing Codex Runner process found. Re-run with -RestartExisting after confirming it is stale. PID(s): $($existing.ProcessId -join ',')"
    }
    foreach ($process in $existing) {
        Stop-Process -Id $process.ProcessId -Force
    }
}

$oldEnv = @{
    CODEX_TEST_API_BASE = $env:CODEX_TEST_API_BASE
    CODEX_TEST_FRONTEND_BASE_URL = $env:CODEX_TEST_FRONTEND_BASE_URL
    CODEX_TEST_RUNNER_TOKEN = $env:CODEX_TEST_RUNNER_TOKEN
    CODEX_TEST_TENANT_ID = $env:CODEX_TEST_TENANT_ID
    CODEX_TEST_WORKDIR = $env:CODEX_TEST_WORKDIR
    CODEX_TEST_PROJECT_ROOT = $env:CODEX_TEST_PROJECT_ROOT
    CODEX_TEST_FRONTEND_ROOT = $env:CODEX_TEST_FRONTEND_ROOT
    CODEX_CLI_COMMAND = $env:CODEX_CLI_COMMAND
    CODEX_TEST_RUNNER_NAME = $env:CODEX_TEST_RUNNER_NAME
    CODEX_TEST_MAX_PARALLELISM = $env:CODEX_TEST_MAX_PARALLELISM
    CODEX_TEST_CLAIM_CAPACITY = $env:CODEX_TEST_CLAIM_CAPACITY
    CODEX_TEST_POLL_INTERVAL_MS = $env:CODEX_TEST_POLL_INTERVAL_MS
    CODEX_TEST_API_TIMEOUT_MS = $env:CODEX_TEST_API_TIMEOUT_MS
}

try {
    $env:CODEX_TEST_API_BASE = $ApiBase
    $env:CODEX_TEST_FRONTEND_BASE_URL = $FrontendBaseUrl
    if ([string]::IsNullOrWhiteSpace($runnerToken)) {
        Remove-Item -Path 'Env:\CODEX_TEST_RUNNER_TOKEN' -ErrorAction SilentlyContinue
    } else {
        $env:CODEX_TEST_RUNNER_TOKEN = $runnerToken
    }
    $env:CODEX_TEST_TENANT_ID = $TenantId
    $env:CODEX_TEST_WORKDIR = $runnerWorkdir
    $env:CODEX_TEST_PROJECT_ROOT = $workspaceRoot
    $env:CODEX_TEST_FRONTEND_ROOT = $frontendRoot
    $env:CODEX_CLI_COMMAND = $CodexCommand
    $env:CODEX_TEST_RUNNER_NAME = $RunnerName
    $env:CODEX_TEST_MAX_PARALLELISM = '1'
    $env:CODEX_TEST_CLAIM_CAPACITY = '1'
    $env:CODEX_TEST_POLL_INTERVAL_MS = [string]$PollIntervalMs
    $env:CODEX_TEST_API_TIMEOUT_MS = [string]$ApiTimeoutMs

    $process = Start-Process `
        -FilePath $NodeCommand `
        -ArgumentList @("`"$runnerScript`"", '--loop') `
        -WorkingDirectory $frontendRoot `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -WindowStyle Hidden `
        -PassThru
} finally {
    foreach ($entry in $oldEnv.GetEnumerator()) {
        if ($null -eq $entry.Value) {
            Remove-Item -Path "Env:\$($entry.Key)" -ErrorAction SilentlyContinue
        } else {
            Set-Item -Path "Env:\$($entry.Key)" -Value $entry.Value
        }
    }
}

Start-Sleep -Seconds $StartupProbeSeconds
if (-not $process -or $process.HasExited) {
    $stderrTail = if (Test-Path -LiteralPath $stderrLog) {
        (Get-Content -Tail 20 -Encoding utf8 -LiteralPath $stderrLog) -join "`n"
    } else {
        ''
    }
    throw "Codex Runner exited during startup probe. stderr tail: $stderrTail"
}

Set-Content -LiteralPath $pidFile -Value ([string]$process.Id) -Encoding utf8

[pscustomobject]@{
    status = 'started'
    pid = $process.Id
    stdout = $stdoutLog
    stderr = $stderrLog
    pidFile = $pidFile
} | ConvertTo-Json -Compress
