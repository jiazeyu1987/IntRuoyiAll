param(
    [Parameter(Mandatory = $true)][string]$TargetPath,
    [Parameter(Mandatory = $true)][int]$ExpectedSlot,
    [Parameter(Mandatory = $true)][int]$ExpectedFrontendPort,
    [Parameter(Mandatory = $true)][int]$ExpectedBackendPort,
    [Parameter(Mandatory = $true)][string]$CleanupTask
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$registryPath = [System.IO.Path]::GetFullPath('D:\IntRuoyiWorktree\.ports\worktree-ports.json')
$normalizedTarget = [System.IO.Path]::GetFullPath($TargetPath)
$worktreeRoot = [System.IO.Path]::GetFullPath('D:\IntRuoyiWorktree')
if (-not $normalizedTarget.StartsWith("$worktreeRoot\", [StringComparison]::OrdinalIgnoreCase)) {
    throw "Target path escapes the worktree root: $normalizedTarget"
}
if (Test-Path -LiteralPath $normalizedTarget) {
    throw "Target directory still exists: $normalizedTarget"
}

. 'E:\IntRuoyi\scripts\runtime\branch-runtime-profile.ps1'

$hash = [System.Security.Cryptography.SHA256]::Create()
try {
    $registryHash = [System.BitConverter]::ToString(
        $hash.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($registryPath.ToUpperInvariant()))
    ).Replace('-', '').Substring(0, 24)
} finally {
    $hash.Dispose()
}

$mutex = [System.Threading.Mutex]::new($false, "IntRuoyiWorktreePorts-$registryHash")
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
        throw "Timed out waiting for the registry lock: $registryPath"
    }

    $document = [System.IO.File]::ReadAllText($registryPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
    $profiles = Get-BranchRuntimeProfiles
    Assert-BranchRuntimePortRegistryEntries -Entries @($document.worktrees) -Profiles $profiles

    $matches = @($document.worktrees | Where-Object {
            $_.active -eq $true -and
            (Normalize-BranchRuntimePath -Path ([string]$_.path)) -eq
            (Normalize-BranchRuntimePath -Path $normalizedTarget)
        })
    if ($matches.Count -ne 1) {
        throw "Expected one active registry entry for '$normalizedTarget', found $($matches.Count)."
    }

    $entry = $matches[0]
    if ([int]$entry.slot -ne $ExpectedSlot -or
        [int]$entry.frontendPort -ne $ExpectedFrontendPort -or
        [int]$entry.backendPort -ne $ExpectedBackendPort) {
        throw 'Registry entry no longer matches the verified slot and ports.'
    }

    $now = [DateTimeOffset]::Now.ToString('o')
    $entry.active = $false
    $entry.updatedAt = $now
    $metadata = [ordered]@{
        releasedAt = $now
        deletedAt = $now
        cleanupTask = $CleanupTask
        notes = 'Released after clean merged worktree removal authorized to free a pre-2026-08-12 slot.'
    }
    foreach ($property in $metadata.GetEnumerator()) {
        if ($entry.PSObject.Properties.Name -contains $property.Key) {
            $entry.($property.Key) = $property.Value
        } else {
            $entry | Add-Member -NotePropertyName $property.Key -NotePropertyValue $property.Value
        }
    }
    $document.updatedAt = $now
    Assert-BranchRuntimePortRegistryEntries -Entries @($document.worktrees) -Profiles $profiles

    $registryDirectory = Split-Path -Parent $registryPath
    $registryName = [System.IO.Path]::GetFileName($registryPath)
    $uniqueId = [Guid]::NewGuid().ToString('N')
    $tempPath = Join-Path $registryDirectory ".$registryName.$PID.$uniqueId.tmp"
    $backupPath = Join-Path $registryDirectory ".$registryName.$PID.$uniqueId.bak"
    $json = $document | ConvertTo-Json -Depth 10
    [System.IO.File]::WriteAllText($tempPath, "$json`r`n", [System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::Replace($tempPath, $registryPath, $backupPath)
    $tempPath = $null
    [System.IO.File]::Delete($backupPath)
    $backupPath = $null

    [pscustomobject]@{
        name = $entry.name
        path = $entry.path
        slot = $entry.slot
        frontendPort = $entry.frontendPort
        backendPort = $entry.backendPort
        active = $entry.active
        releasedAt = $entry.releasedAt
    } | ConvertTo-Json -Depth 3
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
