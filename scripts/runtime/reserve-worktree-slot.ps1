param(
    [Parameter(Mandatory = $true)][ValidatePattern('^[A-Za-z0-9][A-Za-z0-9._-]*$')][string]$Name,
    [Parameter(Mandatory = $true)][string]$Path,
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$Branch,
    [Parameter(Mandatory = $true)]
    [ValidateSet('int_main_d', 'int_main', 'int_batch', 'int_shedule', 'int_qms')]
    [string]$Profile,
    [string]$RegistryPath = 'D:\IntRuoyiWorktree\.ports\worktree-ports.json',
    [switch]$AsJson
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\branch-runtime-profile.ps1"

$worktreeRoot = Normalize-BranchRuntimePath -Path 'D:\IntRuoyiWorktree'
$normalizedPath = Normalize-BranchRuntimePath -Path $Path
if (-not $normalizedPath.StartsWith("$worktreeRoot\", [StringComparison]::OrdinalIgnoreCase)) {
    throw "Worktree path '$normalizedPath' must be a child of '$worktreeRoot'."
}
if ([System.IO.Path]::GetFileName($normalizedPath) -ne $Name) {
    throw "Worktree name '$Name' must match target directory name '$([System.IO.Path]::GetFileName($normalizedPath))'."
}

$registryFullPath = [System.IO.Path]::GetFullPath($RegistryPath)
$registryDirectory = Split-Path -Parent $registryFullPath
if ([string]::IsNullOrWhiteSpace($registryDirectory)) {
    throw "Worktree port registry path must have a parent directory: $registryFullPath"
}
[void][System.IO.Directory]::CreateDirectory($registryDirectory)

$hash = [System.Security.Cryptography.SHA256]::Create()
try {
    $registryHash = [System.BitConverter]::ToString(
        $hash.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($registryFullPath.ToUpperInvariant()))
    ).Replace('-', '').Substring(0, 24)
} finally {
    $hash.Dispose()
}

$mutex = New-Object System.Threading.Mutex($false, "IntRuoyiWorktreePorts-$registryHash")
$lockAcquired = $false
$tempPath = $null
$backupPath = $null
try {
    try {
        $lockAcquired = $mutex.WaitOne([TimeSpan]::FromSeconds(30))
    } catch [System.Threading.AbandonedMutexException] {
        $lockAcquired = $true
    }
    if (-not $lockAcquired) {
        throw "Timed out waiting for the worktree port registry lock: $registryFullPath"
    }

    $profiles = Get-BranchRuntimeProfiles
    $profileMatches = @($profiles | Where-Object { $_.Name -eq $Profile })
    if ($profileMatches.Count -ne 1) {
        throw "Runtime profile '$Profile' is not defined."
    }

    $entries = @(Read-BranchRuntimePortRegistryEntries -RegistryPath $registryFullPath)
    Assert-BranchRuntimePortRegistryEntries -Entries $entries -Profiles $profiles
    $activeEntries = @($entries | Where-Object {
            Test-BranchRuntimeRegistryEntryActive -Entry $_
        })

    $samePathEntries = @($activeEntries | Where-Object {
            (Normalize-BranchRuntimePath -Path ([string]$_.path)) -eq $normalizedPath
        })
    if ($samePathEntries.Count -gt 0) {
        throw "Worktree path '$normalizedPath' already has an active runtime slot reservation."
    }
    $sameBranchEntries = @($activeEntries | Where-Object { [string]$_.branch -eq $Branch })
    if ($sameBranchEntries.Count -gt 0) {
        throw "Branch '$Branch' already has an active runtime slot reservation."
    }

    $usedSlots = @{}
    foreach ($entry in $activeEntries | Where-Object { [string]$_.profile -eq $Profile }) {
        $usedSlots[[string][int]$entry.slot] = $true
    }

    $slot = $null
    for ($candidate = $script:MinimumWorktreeSlot; $candidate -le $script:MaximumWorktreeSlot; $candidate++) {
        if (-not $usedSlots.ContainsKey([string]$candidate)) {
            $slot = $candidate
            break
        }
    }
    if ($null -eq $slot) {
        throw "No available runtime slot for profile '$Profile' in range $($script:MinimumWorktreeSlot)..$($script:MaximumWorktreeSlot)."
    }

    $ports = Get-BranchRuntimePorts -Profile $profileMatches[0] -Slot $slot
    $now = [DateTimeOffset]::Now.ToString('o')
    $allocation = [pscustomobject][ordered]@{
        name = $Name
        path = $normalizedPath
        branch = $Branch
        profile = $Profile
        slot = $slot
        frontendPort = $ports.FrontendPort
        backendPort = $ports.BackendPort
        active = $true
        createdAt = $now
        updatedAt = $now
    }

    $document = [pscustomobject][ordered]@{
        version = 1
        contractVersion = $script:PortContractVersion
        updatedAt = $now
        worktrees = @($entries) + $allocation
    }
    Assert-BranchRuntimePortRegistryEntries -Entries @($document.worktrees) -Profiles $profiles

    $tempPath = Join-Path $registryDirectory ".$([System.IO.Path]::GetFileName($registryFullPath)).$PID.$([Guid]::NewGuid().ToString('N')).tmp"
    $json = $document | ConvertTo-Json -Depth 8
    [System.IO.File]::WriteAllText($tempPath, "$json`r`n", [System.Text.UTF8Encoding]::new($false))
    if (Test-Path -LiteralPath $registryFullPath) {
        $backupPath = Join-Path $registryDirectory ".$([System.IO.Path]::GetFileName($registryFullPath)).$PID.$([Guid]::NewGuid().ToString('N')).bak"
        [System.IO.File]::Replace($tempPath, $registryFullPath, $backupPath)
        [System.IO.File]::Delete($backupPath)
        $backupPath = $null
    } else {
        [System.IO.File]::Move($tempPath, $registryFullPath)
    }
    $tempPath = $null

    if ($AsJson) {
        $allocation | ConvertTo-Json -Depth 4
    } else {
        $allocation
    }
} finally {
    if ($null -ne $tempPath -and (Test-Path -LiteralPath $tempPath)) {
        Remove-Item -LiteralPath $tempPath -Force
    }
    if ($null -ne $backupPath -and (Test-Path -LiteralPath $backupPath)) {
        Remove-Item -LiteralPath $backupPath -Force
    }
    if ($lockAcquired) {
        $mutex.ReleaseMutex()
    }
    $mutex.Dispose()
}
