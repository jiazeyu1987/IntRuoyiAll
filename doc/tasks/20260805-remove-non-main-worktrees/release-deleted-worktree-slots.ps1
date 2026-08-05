$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = 'E:\IntRuoyi'
$registryPath = 'D:\IntRuoyiWorktree\.ports\worktree-ports.json'
$cleanupTask = '20260805-remove-non-main-worktrees'
$targetPaths = @(
    'D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803',
    'D:\IntRuoyiWorktree\production-leader-tab-20260804',
    'D:\IntRuoyiWorktree\profile-nas-table-auto-sync',
    'D:\IntRuoyiWorktree\20260805-process-loss-reasons',
    'D:\IntRuoyiWorktree\worktree_20260805_ac_m20_pqc_review',
    'D:\IntRuoyiWorktree\20260805-integrate-production-personnel',
    'D:\IntRuoyiWorktree\profile-erp-table-auto-sync'
)

. "$repoRoot\scripts\runtime\branch-runtime-profile.ps1"

$registryFullPath = [System.IO.Path]::GetFullPath($registryPath)
$registryDirectory = Split-Path -Parent $registryFullPath
$hash = [System.Security.Cryptography.SHA256]::Create()
try {
    $registryHash = [System.BitConverter]::ToString(
        $hash.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($registryFullPath.ToUpperInvariant()))
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
        throw "Timed out waiting for the worktree port registry lock: $registryFullPath"
    }

    $raw = [System.IO.File]::ReadAllText($registryFullPath, [System.Text.Encoding]::UTF8)
    $document = $raw | ConvertFrom-Json
    $profiles = Get-BranchRuntimeProfiles
    $registeredRaw = @(git -C $repoRoot worktree list --porcelain)
    $now = [DateTimeOffset]::Now.ToString('o')
    $normalizedTargets = @{}

    foreach ($target in $targetPaths) {
        $normalized = Normalize-BranchRuntimePath -Path $target
        if (Test-Path -LiteralPath $normalized) {
            throw "Target path still exists and cannot release registry slot: $normalized"
        }
        if ($registeredRaw -contains ('worktree ' + $normalized.Replace('\', '/'))) {
            throw "Target path is still registered as a Git worktree: $normalized"
        }
        $normalizedTargets[$normalized.ToUpperInvariant()] = $normalized
    }

    $released = @()
    foreach ($entry in @($document.worktrees)) {
        $pathProperty = $entry.PSObject.Properties['path']
        if ($null -eq $pathProperty -or [string]::IsNullOrWhiteSpace([string]$pathProperty.Value)) {
            continue
        }
        $entryPath = Normalize-BranchRuntimePath -Path ([string]$pathProperty.Value)
        if (-not $normalizedTargets.ContainsKey($entryPath.ToUpperInvariant())) {
            continue
        }
        if (-not (Test-BranchRuntimeRegistryEntryActive -Entry $entry)) {
            continue
        }

        $entry.active = $false
        $entry.updatedAt = $now
        if ($null -eq $entry.PSObject.Properties['deletedAt']) {
            $entry | Add-Member -NotePropertyName deletedAt -NotePropertyValue $now
        } else {
            $entry.deletedAt = $now
        }
        if ($null -eq $entry.PSObject.Properties['cleanupTask']) {
            $entry | Add-Member -NotePropertyName cleanupTask -NotePropertyValue $cleanupTask
        } else {
            $entry.cleanupTask = $cleanupTask
        }
        $note = 'Released after user-authorized deletion of non-int_main Git worktrees; Git registration and physical directory are absent.'
        if ($null -eq $entry.PSObject.Properties['notes']) {
            $entry | Add-Member -NotePropertyName notes -NotePropertyValue $note
        } else {
            $entry.notes = $note
        }
        $released += $entryPath
    }

    Assert-BranchRuntimePortRegistryEntries -Entries @($document.worktrees) -Profiles $profiles
    $document.updatedAt = $now

    $tempPath = Join-Path $registryDirectory ".$([System.IO.Path]::GetFileName($registryFullPath)).$PID.$([Guid]::NewGuid().ToString('N')).tmp"
    $json = $document | ConvertTo-Json -Depth 8
    [System.IO.File]::WriteAllText($tempPath, "$json`r`n", [System.Text.UTF8Encoding]::new($false))
    $backupPath = Join-Path $registryDirectory ".$([System.IO.Path]::GetFileName($registryFullPath)).$PID.$([Guid]::NewGuid().ToString('N')).bak"
    [System.IO.File]::Replace($tempPath, $registryFullPath, $backupPath)
    [System.IO.File]::Delete($backupPath)
    $tempPath = $null
    $backupPath = $null

    [pscustomobject]@{
        releasedCount = $released.Count
        releasedPaths = $released
        updatedAt = $now
    } | ConvertTo-Json -Depth 4
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
