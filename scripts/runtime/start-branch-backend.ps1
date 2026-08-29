param(
    [Nullable[int]]$Slot = $null,
    [switch]$Build,
    [string[]]$ExtraArgs = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\branch-runtime-profile.ps1"

$repoRoot = Get-CurrentRepoRoot
$branch = Get-GitValue -RepoRoot $repoRoot -Arguments @('branch', '--show-current')
$context = Resolve-BranchRuntimeContext -RepoRoot $repoRoot -Branch $branch -RequestedSlot $Slot
$profile = $context.Profile
$ports = $context.Ports
$backendRoot = Join-Path $repoRoot 'IntRuoyiBackend'
$jarPath = Join-Path $backendRoot 'yudao-server\target\yudao-server-exec.jar'

function Get-RequiredRuntimeEnvironmentValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    foreach ($scope in @('Process', 'User', 'Machine')) {
        $value = [Environment]::GetEnvironmentVariable($Name, $scope)
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            return $value
        }
    }

    throw "Missing required runtime environment variable: $Name. Configure the DCC download encryption runtime value before starting the backend."
}

$requiredDccDownloadEncryptionEnvironmentNames = @(
    'DCC_DOWNLOAD_ENCRYPTION_POLICY_VERSION',
    'DCC_DOWNLOAD_ENCRYPTION_KEY_ID',
    'DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY',
    'DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY'
)

$dccDownloadEncryptionEnvironment = @{}
foreach ($name in $requiredDccDownloadEncryptionEnvironmentNames) {
    $dccDownloadEncryptionEnvironment[$name] = Get-RequiredRuntimeEnvironmentValue -Name $name
}

$listeners = @(Get-NetTCPConnection -LocalPort $ports.BackendPort -State Listen -ErrorAction SilentlyContinue)
if ($listeners.Count -gt 0) {
    $pids = ($listeners | Select-Object -ExpandProperty OwningProcess -Unique) -join ', '
    throw "Backend port $($ports.BackendPort) is already listening; owning process id(s): $pids. Stop the owned process explicitly before restarting."
}

if ($Build) {
    Push-Location $backendRoot
    try {
        & mvn.cmd -pl yudao-server -am -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw 'Backend package command failed.'
        }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $jarPath)) {
    throw "Missing backend executable jar: $jarPath. Run with -Build or package yudao-server first."
}

$javaArgs = @(
    '-jar',
    $jarPath,
    "--server.port=$($ports.BackendPort)",
    '--spring.profiles.active=local'
) + $ExtraArgs

Write-Host "Starting $($profile.Name) backend on $($ports.BackendPort)."
$previousDccDownloadEncryptionEnvironment = @{}
foreach ($name in $requiredDccDownloadEncryptionEnvironmentNames) {
    $environmentPath = "Env:$name"
    $previousDccDownloadEncryptionEnvironment[$name] = (Get-Item -Path $environmentPath -ErrorAction SilentlyContinue).Value
    Set-Item -Path $environmentPath -Value $dccDownloadEncryptionEnvironment[$name]
}

try {
    & java @javaArgs
    exit $LASTEXITCODE
} finally {
    foreach ($name in $requiredDccDownloadEncryptionEnvironmentNames) {
        $environmentPath = "Env:$name"
        if ($null -eq $previousDccDownloadEncryptionEnvironment[$name]) {
            Remove-Item -Path $environmentPath -ErrorAction SilentlyContinue
        } else {
            Set-Item -Path $environmentPath -Value $previousDccDownloadEncryptionEnvironment[$name]
        }
    }
}
