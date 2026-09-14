param(
  [Parameter(Mandatory = $true)]
  [string]$NewJar,

  [int]$OldPid = 14800
)

$ErrorActionPreference = 'Stop'

$repoRoot = 'E:\IntRuoyi'
$backendRoot = Join-Path $repoRoot 'IntRuoyiBackend'
$logDir = Join-Path $repoRoot 'output\runtime\int_main\logs'
$java = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe'
$oldJarNeedle = 'E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260804-dcc-nas-uncontrolled-import.jar'

if (-not (Test-Path $NewJar)) {
  throw "Missing new runtime jar: $NewJar"
}

$oldProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$OldPid"
if ($null -eq $oldProcess -or $oldProcess.CommandLine -notlike "*$oldJarNeedle*") {
  throw "48081 old PID ownership changed; refusing to stop PID $OldPid"
}

Stop-Process -Id $OldPid
try {
  Wait-Process -Id $OldPid -Timeout 30
} catch {
  throw "Old backend PID $OldPid did not stop within timeout"
}

[Environment]::SetEnvironmentVariable('CODEX_TEST_RUNNER_TOKEN', $null, 'Process')

$stdout = Join-Path $logDir 'backend-runtime-control-20260804-dcc-upload-approval-chain-projection.out.log'
$stderr = Join-Path $logDir 'backend-runtime-control-20260804-dcc-upload-approval-chain-projection.err.log'
$args = @(
  '-jar', $NewJar,
  '--server.port=48081',
  '--spring.profiles.active=local',
  "--logging.file.name=$logDir\yudao-server.log",
  "--yudao.runtime-control.repo-root=$backendRoot",
  "--yudao.runtime-control.state-dir=$backendRoot\runtime\runtime-control",
  "--yudao.runtime-control.storage-guard.log-dir=$logDir"
)

$process = Start-Process `
  -FilePath $java `
  -ArgumentList $args `
  -WorkingDirectory $backendRoot `
  -WindowStyle Hidden `
  -PassThru `
  -RedirectStandardOutput $stdout `
  -RedirectStandardError $stderr

$deadline = (Get-Date).AddSeconds(180)
$health = $null
while ((Get-Date) -lt $deadline) {
  Start-Sleep -Seconds 3
  try {
    $health = Invoke-RestMethod -Uri 'http://127.0.0.1:48081/actuator/health' -TimeoutSec 5
    if ($health.status -eq 'UP') {
      break
    }
  } catch {
    if ($process.HasExited) {
      throw "New backend PID $($process.Id) exited before health UP"
    }
  }
}

if ($null -eq $health -or $health.status -ne 'UP') {
  throw "Backend health did not become UP for PID $($process.Id)"
}

$cmd = Get-CimInstance Win32_Process -Filter "ProcessId=$($process.Id)" | Select-Object ProcessId,ExecutablePath,CommandLine
[PSCustomObject]@{
  oldPid = $OldPid
  newPid = $process.Id
  health = $health.status
  newJar = $NewJar
  stdout = $stdout
  stderr = $stderr
  commandLine = $cmd.CommandLine
} | ConvertTo-Json -Depth 4
