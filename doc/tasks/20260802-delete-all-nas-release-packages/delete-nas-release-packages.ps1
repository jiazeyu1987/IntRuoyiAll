param(
    [int] $ExpectedCount = 210,
    [string] $ExpectedNameHash = 'b9dd9ba41897de2b6c502a3c8521ffa2501c9712ab39d4aca6f611c03dda525d'
)

$ErrorActionPreference = 'Stop'

$expectedCount = $ExpectedCount
$expectedNameHash = $ExpectedNameHash
$expectedUnc = '\\172.30.30.4\IT共享\Backup\ReleasePackage'
$configPath = 'D:\ProjectPackage\Int\IntRuoyiMaintance\config\runtime-control.local.yaml'

function Read-ReleaseValue {
    param(
        [Parameter(Mandatory)]
        [string] $Block,

        [Parameter(Mandatory)]
        [string] $Name
    )

    $match = [regex]::Match(
        $Block,
        "(?m)^\s{6}$([regex]::Escape($Name)):\s*'?(?<value>[^'`r`n]*)'?"
    )
    if (-not $match.Success) {
        throw "release-package.$Name missing"
    }
    return $match.Groups['value'].Value.Trim()
}

$text = [System.IO.File]::ReadAllText(
    $configPath,
    [System.Text.UTF8Encoding]::new($false)
)
$blockMatch = [regex]::Match(
    $text,
    '(?ms)^\s{4}release-package:\s*\r?\n(?<block>(?:\s{6}.+\r?\n?)*)'
)
if (-not $blockMatch.Success) {
    throw 'release-package config block not found'
}

$block = $blockMatch.Groups['block'].Value
$server = Read-ReleaseValue -Block $block -Name 'nas-server'
$share = Read-ReleaseValue -Block $block -Name 'nas-share'
$username = Read-ReleaseValue -Block $block -Name 'nas-username'
$password = Read-ReleaseValue -Block $block -Name 'nas-password'
$domain = Read-ReleaseValue -Block $block -Name 'nas-domain'
$releaseRoot = Read-ReleaseValue -Block $block -Name 'nas-release-root'

if (
    [string]::IsNullOrWhiteSpace($server) -or
    [string]::IsNullOrWhiteSpace($share) -or
    [string]::IsNullOrWhiteSpace($username) -or
    [string]::IsNullOrWhiteSpace($password) -or
    [string]::IsNullOrWhiteSpace($releaseRoot)
) {
    throw 'NAS release package config is incomplete'
}

$rootUnc = "\\$server\$share"
$configuredUnc = "$rootUnc\$($releaseRoot -replace '/', '\')"
if (-not [string]::Equals(
    $configuredUnc,
    $expectedUnc,
    [System.StringComparison]::OrdinalIgnoreCase
)) {
    throw "NAS release root mismatch: $configuredUnc"
}

$credentialUsername = if ([string]::IsNullOrWhiteSpace($domain)) {
    $username
} else {
    "$domain\$username"
}
$credential = [System.Management.Automation.PSCredential]::new(
    $credentialUsername,
    (ConvertTo-SecureString $password -AsPlainText -Force)
)

$driveName = 'IRNAS' + ([guid]::NewGuid().ToString('N').Substring(0, 8))
$drive = $null
$deletedCount = 0
$startedAt = Get-Date

try {
    $drive = New-PSDrive `
        -Name $driveName `
        -PSProvider FileSystem `
        -Root $rootUnc `
        -Credential $credential `
        -ErrorAction Stop

    $releasePath = Join-Path ("$driveName`:") ($releaseRoot -replace '/', '\')
    $releaseItem = Get-Item -LiteralPath $releasePath -Force -ErrorAction Stop
    if (-not $releaseItem.PSIsContainer) {
        throw 'NAS release root is not a directory'
    }

    $releaseFull = $releaseItem.FullName.TrimEnd('\')
    $packages = @(
        Get-ChildItem `
            -LiteralPath $releasePath `
            -Directory `
            -Force `
            -ErrorAction Stop |
            Sort-Object Name
    )
    if ($packages.Count -ne $expectedCount) {
        throw "NAS release package count changed before delete: expected $expectedCount, actual $($packages.Count)"
    }

    foreach ($package in $packages) {
        if (($package.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "NAS release package is a reparse point: $($package.Name)"
        }
        $parentFull = $package.Parent.FullName.TrimEnd('\')
        if (-not [string]::Equals(
            $parentFull,
            $releaseFull,
            [System.StringComparison]::OrdinalIgnoreCase
        )) {
            throw "NAS release package is not a direct child: $($package.FullName)"
        }
    }

    $nameText = (($packages | ForEach-Object Name) -join "`n")
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $actualNameHash = [Convert]::ToHexString(
            $sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($nameText))
        ).ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
    if ($actualNameHash -ne $expectedNameHash) {
        throw "NAS release package set changed before delete: expected hash $expectedNameHash, actual $actualNameHash"
    }

    foreach ($package in $packages) {
        $parentFull = $package.Parent.FullName.TrimEnd('\')
        if (-not [string]::Equals(
            $parentFull,
            $releaseFull,
            [System.StringComparison]::OrdinalIgnoreCase
        )) {
            throw "Delete target escaped release root: $($package.FullName)"
        }

        Remove-Item `
            -LiteralPath $package.FullName `
            -Recurse `
            -Force `
            -ErrorAction Stop

        $deletedCount += 1
        if (($deletedCount % 10) -eq 0 -or $deletedCount -eq $expectedCount) {
            Write-Host "Deleted $deletedCount/$expectedCount release package directories"
        }
    }

    $remainingPackages = @(
        Get-ChildItem `
            -LiteralPath $releasePath `
            -Directory `
            -Force `
            -ErrorAction Stop
    )
    $remainingFiles = @(
        Get-ChildItem `
            -LiteralPath $releasePath `
            -File `
            -Recurse `
            -Force `
            -ErrorAction Stop
    )
    [Int64] $remainingBytes = 0
    foreach ($file in $remainingFiles) {
        $remainingBytes += [Int64] $file.Length
    }

    if ($remainingPackages.Count -ne 0) {
        throw "NAS release packages remain after delete: $($remainingPackages.Count)"
    }

    [pscustomobject]@{
        configuredPath = $configuredUnc
        expectedPackageCount = $expectedCount
        deletedPackageCount = $deletedCount
        remainingPackageCount = $remainingPackages.Count
        remainingFileCount = $remainingFiles.Count
        remainingBytes = $remainingBytes
        rootStillExists = (Test-Path -LiteralPath $releasePath -PathType Container)
        startedAt = $startedAt.ToString('yyyy-MM-dd HH:mm:ss')
        completedAt = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
    } | ConvertTo-Json -Depth 3
} catch {
    $primaryError = $_
    $remainingCount = $null
    if ($null -ne $drive) {
        try {
            $releasePathForError = Join-Path `
                ("$driveName`:") `
                ($releaseRoot -replace '/', '\')
            if (Test-Path -LiteralPath $releasePathForError -PathType Container) {
                $remainingCount = @(
                    Get-ChildItem `
                        -LiteralPath $releasePathForError `
                        -Directory `
                        -Force `
                        -ErrorAction Stop
                ).Count
            }
        } catch {
            $remainingCount = 'unavailable'
        }
    }

    Write-Error (
        "NAS release package deletion failed after deleting {0} directories; remaining={1}; error={2}" -f
        $deletedCount,
        $remainingCount,
        $primaryError.Exception.Message
    )
    throw $primaryError
} finally {
    if ($null -ne $drive) {
        try {
            Remove-PSDrive -Name $driveName -Force -ErrorAction Stop
        } catch {
            Write-Warning "Failed to remove temporary NAS PSDrive $driveName"
        }
    }
}
