param(
    [int]$PollIntervalMs = 5000
)

$ErrorActionPreference = 'Stop'

$taskDir = 'E:\IntRuoyi\doc\tasks\20260725-codex-runner-void-test'
$workspaceRoot = 'E:\IntRuoyi'
$frontendRoot = Join-Path $workspaceRoot 'IntRuoyiFronted'
$runnerScript = Join-Path $frontendRoot 'scripts\codex-test-runner.mjs'
$tokenFile = Join-Path $env:TEMP '20260725-codex-runner-void-test.runner-token.txt'
$stdoutLog = Join-Path $taskDir 'codex-runner-loop.stdout.log'
$stderrLog = Join-Path $taskDir 'codex-runner-loop.stderr.log'
$pidFile = Join-Path $taskDir 'codex-runner-loop.pid'

if (-not (Test-Path -LiteralPath $runnerScript)) {
    throw "Missing runner script: $runnerScript"
}
if (-not (Test-Path -LiteralPath $tokenFile)) {
    throw "Missing runner token file: $tokenFile"
}
if (-not (Get-Command 'node.exe' -ErrorAction SilentlyContinue)) {
    throw 'node.exe is required to start Codex Runner'
}
if (-not (Get-Command 'codex.cmd' -ErrorAction SilentlyContinue)) {
    throw 'codex.cmd is required to start Codex Runner'
}

$existing = Get-CimInstance Win32_Process |
    Where-Object {
        $_.CommandLine -like '*codex-test-runner.mjs*' -and
        $_.CommandLine -like '*--loop*'
    } |
    Select-Object -First 1

if ($existing) {
    Set-Content -LiteralPath $pidFile -Value ([string]$existing.ProcessId) -Encoding utf8
    [pscustomobject]@{
        status = 'already_running'
        pid = $existing.ProcessId
        stdout = $stdoutLog
        stderr = $stderrLog
    } | ConvertTo-Json -Compress
    exit 0
}

$token = (Get-Content -Raw -Encoding utf8 -LiteralPath $tokenFile).Trim()
if ([string]::IsNullOrWhiteSpace($token)) {
    throw 'Runner token file is empty'
}

$oldEnv = @{
    CODEX_TEST_API_BASE = $env:CODEX_TEST_API_BASE
    CODEX_TEST_FRONTEND_BASE_URL = $env:CODEX_TEST_FRONTEND_BASE_URL
    CODEX_TEST_RUNNER_TOKEN = $env:CODEX_TEST_RUNNER_TOKEN
    CODEX_TEST_TENANT_ID = $env:CODEX_TEST_TENANT_ID
    CODEX_TEST_WORKDIR = $env:CODEX_TEST_WORKDIR
    CODEX_CLI_COMMAND = $env:CODEX_CLI_COMMAND
    CODEX_TEST_RUNNER_NAME = $env:CODEX_TEST_RUNNER_NAME
    CODEX_TEST_MAX_PARALLELISM = $env:CODEX_TEST_MAX_PARALLELISM
    CODEX_TEST_CLAIM_CAPACITY = $env:CODEX_TEST_CLAIM_CAPACITY
    CODEX_TEST_POLL_INTERVAL_MS = $env:CODEX_TEST_POLL_INTERVAL_MS
}

try {
    $env:CODEX_TEST_API_BASE = 'http://127.0.0.1:48081/admin-api'
    $env:CODEX_TEST_FRONTEND_BASE_URL = 'http://127.0.0.1:8081'
    $env:CODEX_TEST_RUNNER_TOKEN = $token
    $env:CODEX_TEST_TENANT_ID = '1'
    $env:CODEX_TEST_WORKDIR = $workspaceRoot
    $env:CODEX_CLI_COMMAND = 'codex.cmd'
    $env:CODEX_TEST_RUNNER_NAME = 'local-codex-runner-20260725'
    $env:CODEX_TEST_MAX_PARALLELISM = '1'
    $env:CODEX_TEST_CLAIM_CAPACITY = '1'
    $env:CODEX_TEST_POLL_INTERVAL_MS = [string]$PollIntervalMs

    $process = Start-Process `
        -FilePath 'node.exe' `
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

if (-not $process -or $process.HasExited) {
    throw 'Failed to start Codex Runner process'
}

Set-Content -LiteralPath $pidFile -Value ([string]$process.Id) -Encoding utf8

[pscustomobject]@{
    status = 'started'
    pid = $process.Id
    stdout = $stdoutLog
    stderr = $stderrLog
} | ConvertTo-Json -Compress
