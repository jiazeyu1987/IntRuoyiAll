$ErrorActionPreference = 'Stop'

$repoRoot = 'E:\IntRuoyi'
$backendRoot = Join-Path $repoRoot 'IntRuoyiBackend'
$runtimeRoot = Join-Path $repoRoot 'output\runtime\int_main'
$logRoot = Join-Path $runtimeRoot 'logs'

New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
New-Item -ItemType Directory -Path $logRoot -Force | Out-Null

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$sourceJar = Join-Path $backendRoot 'yudao-server\target\yudao-server-exec.jar'
if (-not (Test-Path -LiteralPath $sourceJar)) {
    throw "Missing backend executable jar: $sourceJar"
}

$runtimeJar = Join-Path $runtimeRoot "backend-runtime-start-local-backend-$stamp.jar"
Copy-Item -LiteralPath $sourceJar -Destination $runtimeJar -Force

$stdoutLog = Join-Path $logRoot "backend-runtime-start-local-backend-$stamp.stdout.log"
$stderrLog = Join-Path $logRoot "backend-runtime-start-local-backend-$stamp.stderr.log"

if (Test-Path Env:\CODEX_TEST_RUNNER_TOKEN) {
    Remove-Item Env:\CODEX_TEST_RUNNER_TOKEN
}

$javaArgs = @(
    '-jar',
    $runtimeJar,
    '--server.port=48081',
    '--spring.profiles.active=local',
    "--logging.file.name=$logRoot\yudao-server.log",
    "--yudao.runtime-control.storage-guard.log-dir=$logRoot",
    '--yudao.codex-test.runner.token='
)

$process = Start-Process `
    -FilePath 'java' `
    -ArgumentList $javaArgs `
    -WorkingDirectory $backendRoot `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -WindowStyle Hidden `
    -PassThru

$health = $null
$deadline = (Get-Date).AddSeconds(180)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    if ($process.HasExited) {
        break
    }

    try {
        $health = Invoke-RestMethod -Uri 'http://127.0.0.1:48081/actuator/health' -TimeoutSec 5
        if ($health.status -eq 'UP') {
            break
        }
    } catch {
        $health = $null
    }
}

$commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId=$($process.Id)").CommandLine
$result = [ordered]@{
    pid = $process.Id
    exited = $process.HasExited
    exitCode = $(if ($process.HasExited) { $process.ExitCode } else { $null })
    runtimeJar = $runtimeJar
    stdoutLog = $stdoutLog
    stderrLog = $stderrLog
    commandLine = $commandLine
    health = $health
}

$result | ConvertTo-Json -Depth 5

if ($process.HasExited -or $null -eq $health -or $health.status -ne 'UP') {
    exit 1
}
