param(
    [Nullable[int]]$Slot = $null,
    [switch]$Build,
    [switch]$NoDebug,
    [int]$DebugPort = 0,
    [string[]]$ExtraArgs = @(),
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9_-]*$')]
    [string]$SpringProfile = 'local'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\branch-runtime-profile.ps1"

function Get-BackendJarSha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    $stream = [System.IO.File]::OpenRead($Path)
    try {
        $sha256 = [System.Security.Cryptography.SHA256]::Create()
        try { return [System.BitConverter]::ToString($sha256.ComputeHash($stream)).Replace('-', '') }
        finally { $sha256.Dispose() }
    } finally {
        $stream.Dispose()
    }
}

$repoRoot = Get-CurrentRepoRoot
$branch = Get-GitValue -RepoRoot $repoRoot -Arguments @('branch', '--show-current')
$context = Resolve-BranchRuntimeContext -RepoRoot $repoRoot -Branch $branch -RequestedSlot $Slot
$profile = $context.Profile
$ports = $context.Ports
$backendRoot = Join-Path $repoRoot 'IntRuoyiBackend'
if ($DebugPort -eq 0) { $DebugPort = 5005 + ($ports.BackendPort - 48081) }
if (-not $NoDebug) {
    if ($DebugPort -lt 1024 -or $DebugPort -gt 65535) { throw 'Invalid debug port.' }
    if (Get-NetTCPConnection -LocalPort $DebugPort -State Listen -ErrorAction SilentlyContinue) {
        throw "Debug port $DebugPort is already occupied."
    }
}
$jarPath = Join-Path $backendRoot 'yudao-server\target\yudao-server-exec.jar'

$listeners = @(Get-NetTCPConnection -LocalPort $ports.BackendPort -State Listen -ErrorAction SilentlyContinue)
if ($listeners.Count -gt 0) {
    $pids = ($listeners | Select-Object -ExpandProperty OwningProcess -Unique) -join ', '
    throw "Backend port $($ports.BackendPort) is already listening; owning process id(s): $pids. Stop the owned process explicitly before restarting."
}

$packageAction = if ($Build) { 'build' } else { 'verify' }
& python (Join-Path $PSScriptRoot 'backend-package.py') $packageAction --root $backendRoot
if ($LASTEXITCODE -ne 0) {
    throw 'Backend package provenance verification failed. Rebuild using backend-package.py build before startup.'
}

# Spring Boot keeps ZIP offsets cached while classes are loaded lazily. Never run
# the Maven output directly: another package build can replace that file in use.
$runtimeDirectory = Join-Path $repoRoot ("output\runtime\{0}\backend-{1}" -f $profile.Name, [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $runtimeDirectory | Out-Null
$runtimeJarPath = Join-Path $runtimeDirectory 'yudao-server-exec.jar'
$sourceHash = Get-BackendJarSha256 -Path $jarPath
Copy-Item -LiteralPath $jarPath -Destination $runtimeJarPath
if ((Get-BackendJarSha256 -Path $runtimeJarPath) -ne $sourceHash -or
    (Get-BackendJarSha256 -Path $jarPath) -ne $sourceHash) {
    throw 'Backend package changed while preparing the runtime copy. Start again after packaging completes.'
}
& python (Join-Path $PSScriptRoot 'backend-package.py') verify --root $backendRoot
if ($LASTEXITCODE -ne 0) { throw 'Backend source or package changed during runtime preparation.' }
Copy-Item -LiteralPath ([System.IO.Path]::ChangeExtension($jarPath, '.provenance.json')) -Destination (Join-Path $runtimeDirectory 'provenance.json')
(Get-Item -LiteralPath $runtimeJarPath).IsReadOnly = $true

$javaArgs = @(
    '-jar',
    $runtimeJarPath,
    "--server.port=$($ports.BackendPort)",
    "--spring.profiles.active=$SpringProfile"
) + $ExtraArgs
if (-not $NoDebug) {
    $javaArgs = @("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$DebugPort") + $javaArgs
    Write-Host "Local Debug / HotSwap enabled on 127.0.0.1:$DebugPort."
}

Write-Host "Starting $($profile.Name) backend on $($ports.BackendPort)."
& java @javaArgs
exit $LASTEXITCODE
