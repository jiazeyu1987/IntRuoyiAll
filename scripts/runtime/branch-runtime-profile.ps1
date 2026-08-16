Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:PortContractVersion = '2026-08-15-branch-runtime-v4'
$script:DefaultWorktreePortRegistryPath = 'D:\IntRuoyiWorktree\.ports\worktree-ports.json'
$script:MinimumWorktreeSlot = 1
$script:LegacyMaximumWorktreeSlot = 19
$script:MaximumWorktreeSlot = 30

function Get-BranchRuntimeProfiles {
    @(
        [pscustomobject]@{
            Name = 'int_main_d'
            Branches = @('int_main')
            PathMarkers = @('\ProjectPackage\IntRuoyi\IntRuoyiAll', '\ProjectPackage\IntRuoyi\IntRuoyiAll\')
            FrontendBasePort = 8101
            BackendBasePort = 48101
            ExtendedFrontendStartPort = 8165
            ExtendedBackendStartPort = 48165
            FrontendMode = 'branch-main-d'
            EnvFile = 'IntRuoyiFronted\.env.branch-main-d'
        },
        [pscustomobject]@{
            Name = 'int_main'
            Branches = @('int_main')
            PathMarkers = @('\IntRuoyi\')
            FrontendBasePort = 8081
            BackendBasePort = 48081
            ExtendedFrontendStartPort = 8154
            ExtendedBackendStartPort = 48154
            FrontendMode = 'env.local'
            EnvFile = $null
        },
        [pscustomobject]@{
            Name = 'int_batch'
            Branches = @('int_batch')
            PathMarkers = @('\IntRuoyiBranch\BatchRecord\')
            FrontendBasePort = 8041
            BackendBasePort = 48041
            ExtendedFrontendStartPort = 8132
            ExtendedBackendStartPort = 48132
            FrontendMode = 'branch-batch'
            EnvFile = 'IntRuoyiFronted\.env.branch-batch'
        },
        [pscustomobject]@{
            Name = 'int_shedule'
            Branches = @('int_shedule', 'int_schedule')
            PathMarkers = @('\IntRuoyiBranch\Shedule\', '\IntRuoyiBranch\Schedule\')
            FrontendBasePort = 8021
            BackendBasePort = 48021
            ExtendedFrontendStartPort = 8121
            ExtendedBackendStartPort = 48121
            FrontendMode = 'branch-shedule'
            EnvFile = 'IntRuoyiFronted\.env.branch-shedule'
        },
        [pscustomobject]@{
            Name = 'int_qms'
            Branches = @('int_qms')
            PathMarkers = @('\IntRuoyiBranch\QMS\')
            FrontendBasePort = 8061
            BackendBasePort = 48061
            ExtendedFrontendStartPort = 8143
            ExtendedBackendStartPort = 48143
            FrontendMode = 'branch-qms'
            EnvFile = 'IntRuoyiFronted\.env.branch-qms'
        }
    )
}

function Get-GitValue {
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )
    $output = & git -C $RepoRoot @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "git $($Arguments -join ' ') failed in $RepoRoot"
    }
    ($output | Select-Object -First 1).Trim()
}

function Get-CurrentRepoRoot {
    $repoRoot = & git rev-parse --show-toplevel
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repoRoot)) {
        throw 'Current directory is not inside a Git repository.'
    }
    [System.IO.Path]::GetFullPath($repoRoot.Trim())
}

function Normalize-BranchRuntimePath {
    param([Parameter(Mandatory = $true)][string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        throw 'Runtime path is required.'
    }

    [System.IO.Path]::GetFullPath($Path).TrimEnd('\', '/').Replace('/', '\')
}

function Test-IsRegisteredWorktreePath {
    param([Parameter(Mandatory = $true)][string]$RepoRoot)

    $worktreeRoot = Normalize-BranchRuntimePath -Path 'D:\IntRuoyiWorktree'
    $normalizedRoot = Normalize-BranchRuntimePath -Path $RepoRoot
    $normalizedRoot.StartsWith("$worktreeRoot\", [StringComparison]::OrdinalIgnoreCase)
}

function Get-BranchRuntimePortRegistryPath {
    if (-not [string]::IsNullOrWhiteSpace($env:INTRUOYI_WORKTREE_PORT_REGISTRY)) {
        return [System.IO.Path]::GetFullPath($env:INTRUOYI_WORKTREE_PORT_REGISTRY)
    }

    $script:DefaultWorktreePortRegistryPath
}

function Read-BranchRuntimePortRegistryEntries {
    param([string]$RegistryPath = (Get-BranchRuntimePortRegistryPath))

    $registryPath = [System.IO.Path]::GetFullPath($RegistryPath)
    if (-not (Test-Path -LiteralPath $registryPath)) {
        return @()
    }

    $raw = [System.IO.File]::ReadAllText($registryPath, [System.Text.Encoding]::UTF8)
    if ([string]::IsNullOrWhiteSpace($raw)) {
        throw "Worktree port registry is empty: $registryPath"
    }

    $json = $raw | ConvertFrom-Json
    $entries = New-Object System.Collections.Generic.List[object]

    function Add-RegistryNode {
        param([object]$Node)

        if ($null -eq $Node) {
            return
        }

        if ($Node -is [System.Array]) {
            foreach ($item in $Node) {
                Add-RegistryNode -Node $item
            }
            return
        }

        $properties = @($Node.PSObject.Properties.Name)
        if ($properties -contains 'worktrees') {
            Add-RegistryNode -Node $Node.worktrees
        }
        if (($properties -contains 'path') -and ($properties -contains 'branch')) {
            [void]$entries.Add($Node)
        }
    }

    Add-RegistryNode -Node $json
    $entries.ToArray()
}

function Get-RequiredRegistryValue {
    param(
        [Parameter(Mandatory = $true)]$Entry,
        [Parameter(Mandatory = $true)][string]$PropertyName,
        [Parameter(Mandatory = $true)][string]$RepoRoot
    )

    $property = $Entry.PSObject.Properties[$PropertyName]
    if ($null -eq $property -or $null -eq $property.Value -or [string]::IsNullOrWhiteSpace([string]$property.Value)) {
        throw "Worktree port registry entry for '$RepoRoot' is missing required field '$PropertyName'."
    }

    $property.Value
}

function Test-BranchRuntimeRegistryEntryActive {
    param([Parameter(Mandatory = $true)]$Entry)

    $activeProperty = $Entry.PSObject.Properties['active']
    if ($null -eq $activeProperty -or $activeProperty.Value -isnot [bool]) {
        throw 'Worktree port registry entries must contain a boolean active field.'
    }

    [bool]$activeProperty.Value
}

function Assert-BranchRuntimePortRegistryEntries {
    param(
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][object[]]$Entries,
        [object[]]$Profiles = (Get-BranchRuntimeProfiles)
    )

    $activeEntries = @($Entries | Where-Object {
            Test-BranchRuntimeRegistryEntryActive -Entry $_
        })
    $slotOwners = @{}
    $frontendPortOwners = @{}
    $backendPortOwners = @{}
    $reservedFrontendPorts = @{}
    $reservedBackendPorts = @{}

    foreach ($profile in $Profiles) {
        $reservedFrontendPorts[[string]$profile.FrontendBasePort] = $profile.Name
        $reservedBackendPorts[[string]$profile.BackendBasePort] = $profile.Name
    }

    foreach ($entry in $activeEntries) {
        $entryPath = Normalize-BranchRuntimePath -Path ([string](Get-RequiredRegistryValue -Entry $entry -PropertyName 'path' -RepoRoot '<registry>'))
        $profileName = [string](Get-RequiredRegistryValue -Entry $entry -PropertyName 'profile' -RepoRoot $entryPath)
        $profileMatches = @($Profiles | Where-Object { $_.Name -eq $profileName })
        if ($profileMatches.Count -ne 1) {
            throw "Worktree port registry profile '$profileName' is not defined for '$entryPath'."
        }

        $slot = [int](Get-RequiredRegistryValue -Entry $entry -PropertyName 'slot' -RepoRoot $entryPath)
        if ($slot -lt $script:MinimumWorktreeSlot -or $slot -gt $script:MaximumWorktreeSlot) {
            throw "Registered worktree slot for '$entryPath' must be between $($script:MinimumWorktreeSlot) and $($script:MaximumWorktreeSlot), got $slot."
        }

        $profile = $profileMatches[0]
        $frontendPort = [int](Get-RequiredRegistryValue -Entry $entry -PropertyName 'frontendPort' -RepoRoot $entryPath)
        $backendPort = [int](Get-RequiredRegistryValue -Entry $entry -PropertyName 'backendPort' -RepoRoot $entryPath)
        $expectedPorts = Get-BranchRuntimePorts -Profile $profile -Slot $slot
        $expectedFrontendPort = $expectedPorts.FrontendPort
        $expectedBackendPort = $expectedPorts.BackendPort
        if ($frontendPort -ne $expectedFrontendPort -or $backendPort -ne $expectedBackendPort) {
            throw "Registered worktree ports for '$entryPath' does not match profile '$profileName' slot ${slot}: expected frontend $expectedFrontendPort backend $expectedBackendPort, got frontend $frontendPort backend $backendPort."
        }

        if ($reservedFrontendPorts.ContainsKey([string]$frontendPort) -or $reservedBackendPorts.ContainsKey([string]$backendPort)) {
            throw "Registered worktree '$entryPath' uses a reserved base runtime port: frontend $frontendPort backend $backendPort."
        }

        $slotKey = "$profileName/$slot"
        if ($slotOwners.ContainsKey($slotKey)) {
            throw "Duplicate active runtime slot '$slotKey' is registered for '$($slotOwners[$slotKey])' and '$entryPath'."
        }
        $slotOwners[$slotKey] = $entryPath

        if ($frontendPortOwners.ContainsKey([string]$frontendPort)) {
            throw "Duplicate active frontend port '$frontendPort' is registered for '$($frontendPortOwners[[string]$frontendPort])' and '$entryPath'."
        }
        $frontendPortOwners[[string]$frontendPort] = $entryPath

        if ($backendPortOwners.ContainsKey([string]$backendPort)) {
            throw "Duplicate active backend port '$backendPort' is registered for '$($backendPortOwners[[string]$backendPort])' and '$entryPath'."
        }
        $backendPortOwners[[string]$backendPort] = $entryPath
    }
}

function Get-RegisteredBranchRuntimeContext {
    param(
        [string]$RepoRoot = (Get-CurrentRepoRoot),
        [string]$Branch = (Get-GitValue -RepoRoot (Get-CurrentRepoRoot) -Arguments @('branch', '--show-current')),
        [object[]]$Profiles = (Get-BranchRuntimeProfiles)
    )

    $fullRoot = Normalize-BranchRuntimePath -Path $RepoRoot
    $registryPath = Get-BranchRuntimePortRegistryPath
    $entries = Read-BranchRuntimePortRegistryEntries
    $matches = @($entries | Where-Object {
            $entryPath = $_.PSObject.Properties['path']
            $null -ne $entryPath -and
                -not [string]::IsNullOrWhiteSpace([string]$entryPath.Value) -and
                (Normalize-BranchRuntimePath -Path ([string]$entryPath.Value)) -eq $fullRoot
        })

    if ($matches.Count -eq 0) {
        if (Test-IsRegisteredWorktreePath -RepoRoot $fullRoot) {
            if (-not (Test-Path -LiteralPath $registryPath)) {
                throw "Missing worktree port registry: $registryPath. Worktree '$fullRoot' must be registered before runtime startup."
            }
            throw "No worktree port registry entry is registered for '$fullRoot'."
        }
        return $null
    }

    $activeMatches = @($matches | Where-Object {
            Test-BranchRuntimeRegistryEntryActive -Entry $_
        })

    if ($activeMatches.Count -eq 0) {
        throw "Worktree port registry entry for '$fullRoot' is not active."
    }
    if ($activeMatches.Count -gt 1) {
        throw "Duplicate active worktree port registry entries for '$fullRoot'."
    }

    Assert-BranchRuntimePortRegistryEntries -Entries $entries -Profiles $Profiles

    $entry = $activeMatches[0]
    $registeredBranch = [string](Get-RequiredRegistryValue -Entry $entry -PropertyName 'branch' -RepoRoot $fullRoot)
    if ($registeredBranch -ne $Branch) {
        throw "Worktree port registry branch mismatch for '$fullRoot': registered '$registeredBranch', current '$Branch'."
    }

    $profileName = [string](Get-RequiredRegistryValue -Entry $entry -PropertyName 'profile' -RepoRoot $fullRoot)
    $profileMatches = @($Profiles | Where-Object { $_.Name -eq $profileName })
    if ($profileMatches.Count -ne 1) {
        throw "Worktree port registry profile '$profileName' is not defined for '$fullRoot'."
    }

    $slot = [int](Get-RequiredRegistryValue -Entry $entry -PropertyName 'slot' -RepoRoot $fullRoot)
    if ($slot -lt $script:MinimumWorktreeSlot -or $slot -gt $script:MaximumWorktreeSlot) {
        throw "Registered worktree slot for '$fullRoot' must be between $($script:MinimumWorktreeSlot) and $($script:MaximumWorktreeSlot), got $slot."
    }

    $profile = $profileMatches[0]
    $frontendPort = [int](Get-RequiredRegistryValue -Entry $entry -PropertyName 'frontendPort' -RepoRoot $fullRoot)
    $backendPort = [int](Get-RequiredRegistryValue -Entry $entry -PropertyName 'backendPort' -RepoRoot $fullRoot)
    $expectedPorts = Get-BranchRuntimePorts -Profile $profile -Slot $slot
    $expectedFrontendPort = $expectedPorts.FrontendPort
    $expectedBackendPort = $expectedPorts.BackendPort
    if ($frontendPort -ne $expectedFrontendPort -or $backendPort -ne $expectedBackendPort) {
        throw "Registered worktree ports for '$fullRoot' does not match profile '$profileName' slot ${slot}: expected frontend $expectedFrontendPort backend $expectedBackendPort, got frontend $frontendPort backend $backendPort."
    }

    New-BranchRuntimeContext -Profile $profile -Slot $slot
}

function Resolve-BranchRuntimeContext {
    param(
        [string]$RepoRoot = (Get-CurrentRepoRoot),
        [string]$Branch = (Get-GitValue -RepoRoot (Get-CurrentRepoRoot) -Arguments @('branch', '--show-current')),
        [Nullable[int]]$RequestedSlot = $null
    )

    $fullRoot = Normalize-BranchRuntimePath -Path $RepoRoot
    $normalizedRoot = $fullRoot
    $pathForMarkerMatch = "$normalizedRoot\"
    $profiles = Get-BranchRuntimeProfiles
    $registeredContext = Get-RegisteredBranchRuntimeContext -RepoRoot $fullRoot -Branch $Branch -Profiles $profiles

    if ($null -ne $registeredContext) {
        if ($null -ne $RequestedSlot -and [int]$RequestedSlot -ne $registeredContext.Slot) {
            throw "Requested runtime slot $RequestedSlot does not match registered slot $($registeredContext.Slot) for '$fullRoot'."
        }
        return $registeredContext
    }

    $slot = if ($null -eq $RequestedSlot) { 0 } else { [int]$RequestedSlot }

    foreach ($profile in $profiles) {
        foreach ($marker in $profile.PathMarkers) {
            if ($pathForMarkerMatch.IndexOf($marker, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                if ($profile.Branches -notcontains $Branch) {
                    throw "Workspace '$fullRoot' belongs to profile '$($profile.Name)' but current branch is '$Branch'. Switch to one of: $($profile.Branches -join ', ')."
                }
                if ($slot -ne 0) {
                    throw "Base workspace must use runtime slot 0. Additional worktrees must be registered under 'D:\IntRuoyiWorktree'."
                }
                return New-BranchRuntimeContext -Profile $profile -Slot $slot
            }
        }
    }

    $branchProfiles = @($profiles | Where-Object { $_.Branches -contains $Branch })
    if ($branchProfiles.Count -eq 1) {
        if ($slot -ne 0) {
            throw "Base workspace must use runtime slot 0. Additional worktrees must be registered under 'D:\IntRuoyiWorktree'."
        }
        return New-BranchRuntimeContext -Profile $branchProfiles[0] -Slot $slot
    }
    if ($branchProfiles.Count -gt 1) {
        throw "Runtime profile is ambiguous for branch '$Branch' at '$fullRoot'. Register worktrees under 'D:\IntRuoyiWorktree' or use a defined base workspace path."
    }

    throw "No branch runtime profile is registered for branch '$Branch' at '$fullRoot'."
}

function Resolve-BranchRuntimeProfile {
    param(
        [string]$RepoRoot = (Get-CurrentRepoRoot),
        [string]$Branch = (Get-GitValue -RepoRoot (Get-CurrentRepoRoot) -Arguments @('branch', '--show-current'))
    )

    (Resolve-BranchRuntimeContext -RepoRoot $RepoRoot -Branch $Branch).Profile
}

function Get-BranchRuntimePorts {
    param(
        [Parameter(Mandatory = $true)]$Profile,
        [int]$Slot = 0
    )

    if ($Slot -lt 0 -or $Slot -gt $script:MaximumWorktreeSlot) {
        throw "Runtime slot must be between 0 and $($script:MaximumWorktreeSlot), got $Slot."
    }

    if ($Slot -le $script:LegacyMaximumWorktreeSlot) {
        $frontendPort = $Profile.FrontendBasePort + $Slot
        $backendPort = $Profile.BackendBasePort + $Slot
    } else {
        $extendedOffset = $Slot - ($script:LegacyMaximumWorktreeSlot + 1)
        $frontendPort = $Profile.ExtendedFrontendStartPort + $extendedOffset
        $backendPort = $Profile.ExtendedBackendStartPort + $extendedOffset
    }

    [pscustomobject]@{
        Profile = $Profile.Name
        Slot = $Slot
        FrontendPort = $frontendPort
        BackendPort = $backendPort
        FrontendMode = $Profile.FrontendMode
        FrontendUrl = "http://127.0.0.1:$frontendPort"
        BackendHealthUrl = "http://127.0.0.1:$backendPort/actuator/health"
    }
}

function New-BranchRuntimeContext {
    param(
        [Parameter(Mandatory = $true)]$Profile,
        [Parameter(Mandatory = $true)][int]$Slot
    )

    [pscustomobject]@{
        ContractVersion = $script:PortContractVersion
        Profile = $Profile
        Slot = $Slot
        Ports = Get-BranchRuntimePorts -Profile $Profile -Slot $Slot
    }
}
