$ErrorActionPreference = 'Stop'

$oldPid = 38500
$oldJar = 'E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260807-082313.jar'
$newJar = 'E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260807-100911-route-workstation-fix.jar'
$runtimeDir = 'E:\IntRuoyi\output\runtime\int_main'
$repoRoot = 'E:\IntRuoyi\IntRuoyiBackend'
$outLog = Join-Path $runtimeDir 'backend-runtime-control-20260807-100911-route-workstation-fix.out.log'
$errLog = Join-Path $runtimeDir 'backend-runtime-control-20260807-100911-route-workstation-fix.err.log'

$oldProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$oldPid"
if ($null -eq $oldProcess) {
    throw "Expected int_main backend PID $oldPid is no longer running"
}
if (-not $oldProcess.CommandLine.Contains($oldJar) -or
    -not $oldProcess.CommandLine.Contains('--server.port=48081')) {
    throw 'Current 48081 backend ownership changed; refusing restart'
}

$javaExe = $oldProcess.ExecutablePath
$argumentTail = $oldProcess.CommandLine -replace '^"[^"]+"\s+-jar\s+[^\s]+\s*', ''
$argumentLine = "-jar `"$newJar`" $argumentTail"

Stop-Process -Id $oldPid
Wait-Process -Id $oldPid -ErrorAction SilentlyContinue
$newProcess = Start-Process -FilePath $javaExe -ArgumentList $argumentLine `
    -WorkingDirectory $repoRoot -WindowStyle Hidden `
    -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru

$deadline = (Get-Date).AddMinutes(3)
$health = $null
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if ($newProcess.HasExited) {
        throw "Patched backend exited with code $($newProcess.ExitCode); see $errLog"
    }
    try {
        $health = Invoke-RestMethod -Uri 'http://127.0.0.1:48081/actuator/health' -TimeoutSec 3
        if ($health.status -eq 'UP') {
            break
        }
    } catch {
        $health = $null
    }
}
if ($null -eq $health -or $health.status -ne 'UP') {
    throw 'Patched backend did not become healthy on 48081 within 3 minutes'
}

$listener = Get-NetTCPConnection -LocalPort 48081 -State Listen | Select-Object -First 1
$runtimeProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
if (-not $runtimeProcess.CommandLine.Contains($newJar)) {
    throw '48081 listener does not belong to patched runtime jar'
}

[pscustomobject]@{
    OldPid = $oldPid
    NewPid = $listener.OwningProcess
    RuntimeJar = $newJar
    Health = $health.status
} | Format-List
