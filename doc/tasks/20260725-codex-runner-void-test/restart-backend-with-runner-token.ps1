$ErrorActionPreference = 'Stop'

$TaskDir = 'E:\IntRuoyi\doc\tasks\20260725-codex-runner-void-test'
$TokenPath = Join-Path $env:TEMP '20260725-codex-runner-void-test.runner-token.txt'
$BackendRoot = 'E:\IntRuoyi\IntRuoyiBackend'
$FrontendRoot = 'E:\IntRuoyi\IntRuoyiFronted'
$BackendJar = Join-Path $BackendRoot 'yudao-server\target\yudao-server-exec.jar'
$StdoutLog = Join-Path $TaskDir 'backend-token-restart.stdout.log'
$StderrLog = Join-Path $TaskDir 'backend-token-restart.stderr.log'

if (-not (Test-Path -LiteralPath $TokenPath)) {
  throw 'Runner token file missing'
}
if (-not (Test-Path -LiteralPath $BackendJar)) {
  throw "Backend jar missing: $BackendJar"
}

$token = [System.IO.File]::ReadAllText($TokenPath, [System.Text.Encoding]::ASCII).Trim()
if ([string]::IsNullOrWhiteSpace($token)) {
  throw 'Runner token file is empty'
}

$connections = @(Get-NetTCPConnection -LocalPort 48081 -State Listen -ErrorAction SilentlyContinue)
if ($connections.Count -gt 0) {
  foreach ($connection in $connections) {
    $pidValue = $connection.OwningProcess
    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId=$pidValue"
    if ($processInfo.CommandLine -notlike '*E:\IntRuoyi\IntRuoyiBackend*') {
      throw "Port 48081 listener is not this backend workspace: PID $pidValue"
    }
    Stop-Process -Id $pidValue -Force
    try {
      Wait-Process -Id $pidValue -Timeout 20
    } catch {
      Start-Sleep -Seconds 2
    }
  }
}

Start-Sleep -Seconds 2
if (Get-NetTCPConnection -LocalPort 48081 -State Listen -ErrorAction SilentlyContinue) {
  throw 'Port 48081 is still occupied after stopping backend'
}

if (Test-Path -LiteralPath $StdoutLog) {
  Remove-Item -LiteralPath $StdoutLog -Force
}
if (Test-Path -LiteralPath $StderrLog) {
  Remove-Item -LiteralPath $StderrLog -Force
}

$java = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) {
  $java = (Get-Command java.exe).Source
}

$env:YUDAO_CODEX_TEST_RUNNER_TOKEN = $token
$argsList = @(
  '-jar',
  $BackendJar,
  '--server.port=48081',
  '--spring.profiles.active=local',
  "--yudao.runtime-control.repo-root=$BackendRoot",
  "--yudao.runtime-control.frontend-root=$FrontendRoot"
)
$newProcess = Start-Process -FilePath $java `
  -ArgumentList $argsList `
  -WorkingDirectory $BackendRoot `
  -WindowStyle Hidden `
  -RedirectStandardOutput $StdoutLog `
  -RedirectStandardError $StderrLog `
  -PassThru
Remove-Item Env:\YUDAO_CODEX_TEST_RUNNER_TOKEN -ErrorAction SilentlyContinue

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
  $tail = ''
  if (Test-Path -LiteralPath $StderrLog) {
    $tail = (Get-Content -LiteralPath $StderrLog -Tail 80 -Encoding utf8) -join "`n"
  }
  throw "Backend failed to become healthy. PID=$($newProcess.Id). stderr tail: $tail"
}

Write-Output "BACKEND_RESTARTED_PID=$($newProcess.Id)"
Write-Output 'BACKEND_HEALTH=UP'
Write-Output "STDOUT_LOG=$StdoutLog"
Write-Output "STDERR_LOG=$StderrLog"
