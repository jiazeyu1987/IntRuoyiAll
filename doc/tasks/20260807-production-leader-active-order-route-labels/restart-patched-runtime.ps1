$ErrorActionPreference = 'Stop'

Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class NativeArgvActiveOrder {
    [DllImport("shell32.dll", SetLastError = true)]
    private static extern IntPtr CommandLineToArgvW(
        [MarshalAs(UnmanagedType.LPWStr)] string commandLine,
        out int argc);

    [DllImport("kernel32.dll")]
    private static extern IntPtr LocalFree(IntPtr memory);

    public static string[] Parse(string commandLine) {
        int argc;
        IntPtr argv = CommandLineToArgvW(commandLine, out argc);
        if (argv == IntPtr.Zero) {
            throw new System.ComponentModel.Win32Exception();
        }
        try {
            string[] result = new string[argc];
            for (int index = 0; index < argc; index++) {
                IntPtr value = Marshal.ReadIntPtr(argv, index * IntPtr.Size);
                result[index] = Marshal.PtrToStringUni(value);
            }
            return result;
        } finally {
            LocalFree(argv);
        }
    }
}
'@

$expectedJar = 'E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260807-121743-route-workstation-formal-binding-fix.jar'
$newJar = 'E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260807-active-order-route-labels.jar'
$runtimeDir = 'E:\IntRuoyi\output\runtime\int_main'
$repoRoot = 'E:\IntRuoyi\IntRuoyiBackend'
$outLog = Join-Path $runtimeDir 'backend-runtime-control-20260807-active-order-route-labels.out.log'
$errLog = Join-Path $runtimeDir 'backend-runtime-control-20260807-active-order-route-labels.err.log'
if (-not (Test-Path -LiteralPath $newJar)) {
    throw 'Patched runtime jar is missing'
}

$listener = Get-NetTCPConnection -LocalPort 48081 -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1
$oldPid = $null
if ($null -ne $listener) {
    $oldPid = $listener.OwningProcess
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$oldPid"
    $argv = [NativeArgvActiveOrder]::Parse($process.CommandLine)
    if ($argv.Length -lt 3 -or $argv[1] -ne '-jar' -or $argv[2] -ne $expectedJar) {
        throw 'Current 48081 process is not the validated base runtime'
    }
    $httpClients = @(Get-NetTCPConnection -LocalPort 48081 -State Established -ErrorAction SilentlyContinue)
    if ($httpClients.Count -ne 0) {
        throw "48081 has $($httpClients.Count) active HTTP client connections"
    }
    Stop-Process -Id $oldPid
    if (Get-Process -Id $oldPid -ErrorAction SilentlyContinue) {
        Wait-Process -Id $oldPid -Timeout 30
    }
    $releaseDeadline = (Get-Date).AddSeconds(30)
    while ((Get-NetTCPConnection -LocalPort 48081 -State Listen -ErrorAction SilentlyContinue) `
            -and (Get-Date) -lt $releaseDeadline) {
        Start-Sleep -Milliseconds 250
    }
    if (Get-NetTCPConnection -LocalPort 48081 -State Listen -ErrorAction SilentlyContinue) {
        throw '48081 did not release'
    }
}

$env:CODEX_TEST_RUNNER_TOKEN = $null
$javaExecutable = (Get-Command java).Source
$javaArguments = @(
    '-jar',
    $newJar,
    '--server.port=48081',
    '--spring.profiles.active=local',
    '--spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.2:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true',
    '--spring.datasource.dynamic.datasource.slave.url=jdbc:mysql://127.0.0.2:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&nullCatalogMeansCurrent=true',
    '--spring.data.redis.host=127.0.0.2',
    '--spring.data.redis.port=26379',
    "--logging.file.name=$runtimeDir\logs\yudao-server.log",
    "--yudao.runtime-control.repo-root=$repoRoot",
    "--yudao.runtime-control.state-dir=$repoRoot\runtime\runtime-control",
    "--yudao.runtime-control.storage-guard.log-dir=$runtimeDir\logs"
)
$newProcess = Start-Process -FilePath $javaExecutable -ArgumentList $javaArguments `
    -WorkingDirectory $repoRoot -WindowStyle Hidden `
    -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru
Write-Output "OLD_PID=$oldPid"
Write-Output "NEW_PID=$($newProcess.Id)"
Write-Output "NEW_JAR=$newJar"

$healthDeadline = (Get-Date).AddSeconds(180)
$healthy = $false
do {
    Start-Sleep -Seconds 2
    try {
        $health = Invoke-RestMethod -Uri 'http://127.0.0.1:48081/actuator/health' -TimeoutSec 5
        if ($health.status -eq 'UP') {
            $healthy = $true
        }
    } catch {
        # Startup connection failures remain expected until the deadline.
    }
} while (-not $healthy -and (Get-Date) -lt $healthDeadline)

if (-not $healthy) {
    throw 'Patched backend did not become healthy within 180 seconds'
}
$newListener = Get-NetTCPConnection -LocalPort 48081 -State Listen | Select-Object -First 1
if ($newListener.OwningProcess -ne $newProcess.Id) {
    throw '48081 listener does not belong to patched backend'
}
Write-Output 'HEALTH=UP'
Write-Output "LISTENER_PID=$($newListener.OwningProcess)"
