param(
    [Parameter(Mandatory = $true)]
    [string]$OldJar,
    [Parameter(Mandatory = $true)]
    [string]$NewJar
)

$ErrorActionPreference = 'Stop'

$expectedRepoRoot = 'E:\IntRuoyi\IntRuoyiBackend'
$runtimeRoot = [System.IO.Path]::GetFullPath('E:\IntRuoyi\output\runtime\int_main')
$oldJarPath = [System.IO.Path]::GetFullPath($OldJar)
$newJarPath = [System.IO.Path]::GetFullPath($NewJar)
$logRoot = Join-Path $runtimeRoot 'logs'
$stdoutLog = Join-Path $logRoot 'backend-acm04-pqc-source-context-20260805.stdout.log'
$stderrLog = Join-Path $logRoot 'backend-acm04-pqc-source-context-20260805.stderr.log'

foreach ($path in @($oldJarPath, $newJarPath)) {
    if (-not $path.StartsWith($runtimeRoot + [System.IO.Path]::DirectorySeparatorChar,
            [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Runtime jar escaped the int_main runtime root: $path"
    }
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Runtime jar is missing: $path"
    }
}

$listener = netstat -ano -p tcp |
    Select-String -Pattern '^\s*TCP\s+\S+:48081\s+\S+\s+LISTENING\s+(\d+)\s*$' |
    Select-Object -First 1
if ($null -eq $listener) {
    throw 'Port 48081 has no listener before the controlled restart.'
}

$oldPid = [int]$listener.Matches[0].Groups[1].Value
$oldProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $oldPid"
if ($null -eq $oldProcess -or $oldProcess.Name -ne 'java.exe') {
    throw "Port 48081 PID $oldPid is not the expected Java backend."
}
if (-not $oldProcess.CommandLine.Contains($oldJarPath)) {
    throw "Port 48081 PID $oldPid does not own the expected old runtime jar."
}
if (-not $oldProcess.CommandLine.Contains('--server.port=48081')) {
    throw "Port 48081 PID $oldPid does not declare the expected server port."
}
if (-not $oldProcess.CommandLine.Contains("--yudao.runtime-control.repo-root=$expectedRepoRoot")) {
    throw "Port 48081 PID $oldPid does not belong to the E:\IntRuoyi backend."
}

$commandPattern = '(?s)^"(?<exe>[^"]+)"\s+-jar\s+"?' +
    [regex]::Escape($oldJarPath) +
    '"?\s+(?<tail>.*)$'
$commandMatch = [regex]::Match($oldProcess.CommandLine, $commandPattern)
if (-not $commandMatch.Success) {
    throw 'Unable to parse the verified backend command line.'
}
$javaExecutable = $commandMatch.Groups['exe'].Value
$argumentTail = $commandMatch.Groups['tail'].Value

Stop-Process -Id $oldPid
Wait-Process -Id $oldPid -Timeout 30 -ErrorAction SilentlyContinue
if (Get-Process -Id $oldPid -ErrorAction SilentlyContinue) {
    throw "The verified old backend PID $oldPid did not stop."
}

$releaseDeadline = (Get-Date).AddSeconds(30)
do {
    $remainingListener = netstat -ano -p tcp |
        Select-String -Pattern '^\s*TCP\s+\S+:48081\s+\S+\s+LISTENING\s+\d+\s*$' |
        Select-Object -First 1
    if ($null -eq $remainingListener) {
        break
    }
    Start-Sleep -Milliseconds 500
} while ((Get-Date) -lt $releaseDeadline)
if ($null -ne $remainingListener) {
    throw 'Port 48081 did not release after the verified old backend stopped.'
}

Remove-Item Env:\CODEX_TEST_RUNNER_TOKEN -ErrorAction SilentlyContinue
$newProcess = Start-Process `
    -FilePath $javaExecutable `
    -ArgumentList "-jar `"$newJarPath`" $argumentTail" `
    -WorkingDirectory $expectedRepoRoot `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -WindowStyle Hidden `
    -PassThru

$health = $null
$healthDeadline = (Get-Date).AddMinutes(5)
do {
    $newProcess.Refresh()
    if ($newProcess.HasExited) {
        throw "The new backend exited early with code $($newProcess.ExitCode)."
    }
    try {
        $health = Invoke-RestMethod -Uri 'http://127.0.0.1:48081/actuator/health' -TimeoutSec 3
    } catch {
        $health = $null
    }
    if ($null -ne $health -and $health.status -eq 'UP') {
        break
    }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $healthDeadline)
if ($null -eq $health -or $health.status -ne 'UP') {
    throw 'The new backend did not reach health UP within five minutes.'
}

$listenerAfter = netstat -ano -p tcp |
    Select-String -Pattern '^\s*TCP\s+\S+:48081\s+\S+\s+LISTENING\s+(\d+)\s*$' |
    Select-Object -First 1
if ($null -eq $listenerAfter) {
    throw 'Port 48081 has no listener after health reached UP.'
}
$newListenerPid = [int]$listenerAfter.Matches[0].Groups[1].Value
if ($newListenerPid -ne $newProcess.Id) {
    throw "Port 48081 listener PID $newListenerPid does not match started PID $($newProcess.Id)."
}

$runtimeJar = Get-Item -LiteralPath $newJarPath
$runtimeProcess = Get-Process -Id $newProcess.Id
if ($runtimeJar.LastWriteTime -gt $runtimeProcess.StartTime) {
    throw 'The runtime jar was modified after the backend process started.'
}

$runtimeHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $newJarPath).Hash
Write-Output "OLD_BACKEND_PID=$oldPid"
Write-Output "NEW_BACKEND_PID=$($newProcess.Id)"
Write-Output "RUNTIME_JAR=$newJarPath"
Write-Output "RUNTIME_JAR_SHA256=$runtimeHash"
Write-Output 'RUNTIME_JAR_IMMUTABLE=PASS'
Write-Output "BACKEND_HEALTH=$($health.status)"
