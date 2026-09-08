param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('prod', 'backup')]
    [string]$TargetEnvironment,
    [Parameter(Mandatory = $true)]
    [string]$ServerHost,
    [string]$ServerUser = 'root',
    [Parameter(Mandatory = $true)]
    [string]$RemoteAppDir
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
. (Join-Path $PSScriptRoot 'resolve-trusted-time-ssh-result.ps1')

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $Name"
    }
}

function ConvertTo-ProcessArgumentString {
    param([string[]]$Arguments = @())

    $escaped = New-Object System.Collections.Generic.List[string]
    foreach ($argument in @($Arguments)) {
        $value = [string]$argument
        if ($value.Length -gt 0 -and $value -notmatch '[\s"]') {
            $escaped.Add($value)
            continue
        }
        $builder = New-Object System.Text.StringBuilder
        [void]$builder.Append('"')
        $backslashes = 0
        foreach ($character in $value.ToCharArray()) {
            if ($character -eq '\') {
                $backslashes++
                continue
            }
            if ($character -eq '"') {
                [void]$builder.Append('\' * (($backslashes * 2) + 1))
                [void]$builder.Append('"')
                $backslashes = 0
                continue
            }
            if ($backslashes -gt 0) {
                [void]$builder.Append('\' * $backslashes)
                $backslashes = 0
            }
            [void]$builder.Append($character)
        }
        if ($backslashes -gt 0) {
            [void]$builder.Append('\' * ($backslashes * 2))
        }
        [void]$builder.Append('"')
        $escaped.Add($builder.ToString())
    }
    return $escaped -join ' '
}

function Invoke-Remote([string]$Command) {
    $sshCommand = Get-Command 'ssh' -ErrorAction Stop
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $sshCommand.Source
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    if ($startInfo.PSObject.Properties.Name -contains 'StandardOutputEncoding') {
        $startInfo.StandardOutputEncoding = [System.Text.UTF8Encoding]::new($false)
        $startInfo.StandardErrorEncoding = [System.Text.UTF8Encoding]::new($false)
    }
    $arguments = @(
        '-n',
        '-o', 'BatchMode=yes',
        '-o', 'ConnectTimeout=5',
        '-o', 'StrictHostKeyChecking=yes',
        "$ServerUser@$ServerHost",
        $Command
    )
    if ($startInfo.PSObject.Properties.Name -contains 'ArgumentList') {
        foreach ($argument in $arguments) {
            [void]$startInfo.ArgumentList.Add($argument)
        }
    } else {
        $startInfo.Arguments = ConvertTo-ProcessArgumentString -Arguments $arguments
    }

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    if (-not $process.Start()) {
        throw 'SSH process failed to start'
    }
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    return Resolve-TrustedTimeSshResult `
        -ExitCode $process.ExitCode `
        -StdOut $stdoutTask.GetAwaiter().GetResult() `
        -StdErr $stderrTask.GetAwaiter().GetResult()
}

if ([string]::IsNullOrWhiteSpace($ServerHost)) {
    throw 'ServerHost is required'
}
if ([string]::IsNullOrWhiteSpace($RemoteAppDir)) {
    throw 'RemoteAppDir is required'
}

Require-Command 'ssh'
$chronycTracking = Invoke-Remote 'set -e; export LC_ALL=C; command -v chronyc >/dev/null; chronyc tracking'
$chronycSources = Invoke-Remote 'set -e; export LC_ALL=C; command -v chronyc >/dev/null; chronyc sources -n'
$timedatectlStatus = Invoke-Remote 'set -e; export LC_ALL=C; command -v timedatectl >/dev/null; timedatectl status'
$serverTimeUtc = Invoke-Remote 'set -e; date -u +%Y-%m-%dT%H:%M:%S.%NZ'
$databaseCommand = "set -e; docker exec intruoyi-mysql sh -c 'test -n `"`$MYSQL_ROOT_PASSWORD`"; export MYSQL_PWD=`"`$MYSQL_ROOT_PASSWORD`"; exec mysql -N -B -uroot -e `"SELECT UTC_TIMESTAMP(6);`"'"
$databaseTimeRaw = Invoke-Remote $databaseCommand
$databaseTime = [DateTime]::ParseExact(
    $databaseTimeRaw,
    'yyyy-MM-dd HH:mm:ss.ffffff',
    [Globalization.CultureInfo]::InvariantCulture,
    [Globalization.DateTimeStyles]::AssumeUniversal -bor [Globalization.DateTimeStyles]::AdjustToUniversal
)
$databaseTimeUtc = $databaseTime.ToString('yyyy-MM-ddTHH:mm:ss.ffffffZ')
$checkedAtUtc = [DateTimeOffset]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ss.fffffffZ')

[ordered]@{
    targetEnvironment = $TargetEnvironment
    serverHost = $ServerHost
    chronycTracking = $chronycTracking
    chronycSources = $chronycSources
    timedatectlStatus = $timedatectlStatus
    serverTimeUtc = $serverTimeUtc
    databaseTimeUtc = $databaseTimeUtc
    checkedAtUtc = $checkedAtUtc
} | ConvertTo-Json -Depth 4 -Compress
