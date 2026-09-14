param(
    [int]$BackendPid,
    [string]$BackendJar,
    [string]$BackendRoot,
    [string]$TokenFile,
    [string]$RuntimeDir = 'E:\IntRuoyi\.runtime\codex-test-runner'
)

$ErrorActionPreference = 'Stop'

if (-not $BackendPid) {
    throw 'BackendPid is required'
}
if (-not (Test-Path -LiteralPath $BackendJar)) {
    throw "Backend jar missing: $BackendJar"
}
if (-not (Test-Path -LiteralPath $BackendRoot)) {
    throw "Backend root missing: $BackendRoot"
}
if (-not (Test-Path -LiteralPath $TokenFile)) {
    throw "Runner token file missing: $TokenFile"
}

$processInfo = Get-CimInstance Win32_Process -Filter "ProcessId=$BackendPid"
if (-not $processInfo) {
    throw "Backend PID not found: $BackendPid"
}
if (-not $processInfo.CommandLine.Contains($BackendJar) -or -not $processInfo.CommandLine.Contains('--server.port=48081')) {
    throw "PID $BackendPid is not the expected 48081 backend"
}

$token = (Get-Content -Raw -Encoding utf8 -LiteralPath $TokenFile).Trim()
if ([string]::IsNullOrWhiteSpace($token)) {
    throw 'Runner token file is empty'
}

if (-not (Test-Path -LiteralPath $RuntimeDir)) {
    New-Item -ItemType Directory -Force -Path $RuntimeDir | Out-Null
}
$stdoutLog = Join-Path $RuntimeDir 'backend-token-restart.stdout.log'
$stderrLog = Join-Path $RuntimeDir 'backend-token-restart.stderr.log'

Stop-Process -Id $BackendPid -Force
try {
    Wait-Process -Id $BackendPid -Timeout 20
} catch {
    Start-Sleep -Seconds 2
}

Start-Sleep -Seconds 2
if (Get-NetTCPConnection -LocalPort 48081 -State Listen -ErrorAction SilentlyContinue) {
    throw 'Port 48081 is still occupied after stopping backend'
}

$java = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) {
    $java = (Get-Command java.exe).Source
}

$oldRunnerTokenEnv = $env:YUDAO_CODEX_TEST_RUNNER_TOKEN
$oldSpringApplicationJson = $env:SPRING_APPLICATION_JSON
$springApplicationJson = @{
    yudao = @{
        'codex-test' = @{
            runner = @{
                token = $token
            }
        }
    }
} | ConvertTo-Json -Compress -Depth 8

try {
    $env:YUDAO_CODEX_TEST_RUNNER_TOKEN = $token
    $env:SPRING_APPLICATION_JSON = $springApplicationJson
    $newProcess = Start-Process `
        -FilePath $java `
        -ArgumentList @('-jar', $BackendJar, '--spring.profiles.active=local', '--server.port=48081') `
        -WorkingDirectory $BackendRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -PassThru
} finally {
    if ($null -eq $oldRunnerTokenEnv) {
        Remove-Item Env:\YUDAO_CODEX_TEST_RUNNER_TOKEN -ErrorAction SilentlyContinue
    } else {
        $env:YUDAO_CODEX_TEST_RUNNER_TOKEN = $oldRunnerTokenEnv
    }
    if ($null -eq $oldSpringApplicationJson) {
        Remove-Item Env:\SPRING_APPLICATION_JSON -ErrorAction SilentlyContinue
    } else {
        $env:SPRING_APPLICATION_JSON = $oldSpringApplicationJson
    }
}

$healthy = $false
for ($i = 1; $i -le 60; $i++) {
    Start-Sleep -Seconds 2
    try {
        $health = Invoke-RestMethod -Uri 'http://127.0.0.1:48081/actuator/health' -TimeoutSec 3
        if ($health.status -eq 'UP') {
            $healthy = $true
            break
        }
    } catch {
    }
    if ($newProcess.HasExited) {
        break
    }
}

if (-not $healthy) {
    $tail = if (Test-Path -LiteralPath $stderrLog) {
        (Get-Content -Tail 80 -Encoding utf8 -LiteralPath $stderrLog) -join "`n"
    } else {
        ''
    }
    throw "Backend failed to become healthy. PID=$($newProcess.Id). stderr tail: $tail"
}

[pscustomobject]@{
    status = 'backend_restarted'
    pid = $newProcess.Id
    health = 'UP'
    stderr = $stderrLog
} | ConvertTo-Json -Compress
