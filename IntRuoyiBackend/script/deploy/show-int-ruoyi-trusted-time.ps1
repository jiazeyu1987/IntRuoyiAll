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
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $Name"
    }
}

function Escape-SingleQuotedBash([string]$Value) {
    return $Value.Replace("'", "'`"'`"'")
}

function Invoke-Remote([string]$Command) {
    $lines = & ssh `
        -o BatchMode=yes `
        -o ConnectTimeout=5 `
        -o StrictHostKeyChecking=yes `
        "$ServerUser@$ServerHost" `
        $Command 2>&1
    $exitCode = $LASTEXITCODE
    $text = ($lines | ForEach-Object { [string]$_ }) -join "`n"
    if ($exitCode -ne 0) {
        throw "SSH command failed with exit code ${exitCode}: $text"
    }
    if ([string]::IsNullOrWhiteSpace($text)) {
        throw "SSH command returned empty output: $Command"
    }
    return $text.Trim()
}

if ([string]::IsNullOrWhiteSpace($ServerHost)) {
    throw 'ServerHost is required'
}
if ([string]::IsNullOrWhiteSpace($RemoteAppDir)) {
    throw 'RemoteAppDir is required'
}

Require-Command 'ssh'
$escapedRemoteAppDir = Escape-SingleQuotedBash $RemoteAppDir
$chronycTracking = Invoke-Remote 'set -e; export LC_ALL=C; command -v chronyc >/dev/null; chronyc tracking'
$chronycSources = Invoke-Remote 'set -e; export LC_ALL=C; command -v chronyc >/dev/null; chronyc sources -n'
$timedatectlStatus = Invoke-Remote 'set -e; export LC_ALL=C; command -v timedatectl >/dev/null; timedatectl status'
$serverTimeUtc = Invoke-Remote 'set -e; date -u +%Y-%m-%dT%H:%M:%S.%NZ'
$databaseCommand = "set -e; cd '$escapedRemoteAppDir'; test -f .env; set -a; . ./.env; set +a; test -n `"`$MYSQL_ROOT_PASSWORD`"; docker exec -e MYSQL_PWD=`"`$MYSQL_ROOT_PASSWORD`" intruoyi-mysql mysql -N -B -uroot -e `"SELECT DATE_FORMAT(UTC_TIMESTAMP(6), '%Y-%m-%dT%H:%i:%s.%fZ');`""
$databaseTimeUtc = Invoke-Remote $databaseCommand
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
