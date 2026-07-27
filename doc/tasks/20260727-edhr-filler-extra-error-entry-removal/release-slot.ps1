param(
    [string]$RegistryPath = 'D:\IntRuoyiWorktree\.ports\worktree-ports.json'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$targetName = '20260727-edhr-filler-extra-error-entry-removal-integration'
$targetPath = [System.IO.Path]::GetFullPath(
    'D:\IntRuoyiWorktree\20260727-edhr-filler-extra-error-entry-removal-integration'
).TrimEnd('\')

if (Test-Path -LiteralPath $targetPath) {
    throw "Cannot release slot while worktree path exists: $targetPath"
}

$registryFullPath = [System.IO.Path]::GetFullPath($RegistryPath)
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
try {
    try {
        $lockAcquired = $mutex.WaitOne([TimeSpan]::FromSeconds(30))
    } catch [System.Threading.AbandonedMutexException] {
        $lockAcquired = $true
    }
    if (-not $lockAcquired) {
        throw "Timed out waiting for registry lock: $registryFullPath"
    }

    $document = [System.IO.File]::ReadAllText(
        $registryFullPath,
        [System.Text.Encoding]::UTF8
    ) | ConvertFrom-Json
    $matches = @($document.worktrees | Where-Object {
        $_.active -eq $true -and
        [string]$_.name -eq $targetName -and
        [System.IO.Path]::GetFullPath([string]$_.path).TrimEnd('\') -eq $targetPath
    })
    if ($matches.Count -ne 1) {
        throw "Expected exactly one active registry entry, found $($matches.Count)"
    }

    $now = [DateTimeOffset]::Now.ToString('o')
    $entry = $matches[0]
    $entry.active = $false
    $entry.updatedAt = $now
    $entry | Add-Member -NotePropertyName deletedAt -NotePropertyValue $now -Force
    $entry | Add-Member -NotePropertyName cleanupTask `
        -NotePropertyValue '20260727-edhr-filler-extra-error-entry-removal' -Force
    $document.updatedAt = $now

    $json = $document | ConvertTo-Json -Depth 12
    [System.IO.File]::WriteAllText(
        $registryFullPath,
        "$json`r`n",
        [System.Text.UTF8Encoding]::new($false)
    )

    [pscustomobject]@{
        Name = $entry.name
        Active = $entry.active
        Slot = $entry.slot
        FrontendPort = $entry.frontendPort
        BackendPort = $entry.backendPort
        DeletedAt = $entry.deletedAt
        CleanupTask = $entry.cleanupTask
    }
} finally {
    if ($lockAcquired) {
        $mutex.ReleaseMutex()
    }
    $mutex.Dispose()
}
