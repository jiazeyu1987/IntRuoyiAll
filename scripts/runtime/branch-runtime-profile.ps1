Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:PortContractVersion = '2026-07-24-branch-runtime-v2'
$script:DefaultWorktreePortRegistryPath = 'D:\IntRuoyiWorktree\.ports\worktree-ports.json'

function Get-BranchRuntimeProfiles {
    @(
        [pscustomobject]@{
            Name = 'int_main_d'
            Branches = @('int_main')
            PathMarkers = @('\ProjectPackage\IntRuoyi\IntRuoyiAll', '\ProjectPackage\IntRuoyi\IntRuoyiAll\')
            FrontendBasePort = 8101
            BackendBasePort = 48101
            FrontendMode = 'branch-main-d'
            EnvFile = 'IntRuoyiFronted\.env.branch-main-d'
        },
        [pscustomobject]@{
            Name = 'int_main'
            Branches = @('int_main')
            PathMarkers = @('\IntRuoyi\')
            FrontendBasePort = 8081
            BackendBasePort = 48081
            FrontendMode = 'env.local'
            EnvFile = $null
        },
        [pscustomobject]@{
            Name = 'int_batch'
            Branches = @('int_batch')
            PathMarkers = @('\IntRuoyiBranch\BatchRecord\')
            FrontendBasePort = 8041
            BackendBasePort = 48041
            FrontendMode = 'branch-batch'
            EnvFile = 'IntRuoyiFronted\.env.branch-batch'
        },
        [pscustomobject]@{
            Name = 'int_shedule'
            Branches = @('int_shedule', 'int_schedule')
            PathMarkers = @('\IntRuoyiBranch\Shedule\', '\IntRuoyiBranch\Schedule\')
            FrontendBasePort = 8021
            BackendBasePort = 48021
            FrontendMode = 'branch-shedule'
            EnvFile = 'IntRuoyiFronted\.env.branch-shedule'
        },
        [pscustomobject]@{
            Name = 'int_qms'
            Branches = @('int_qms')
            PathMarkers = @('\IntRuoyiBranch\QMS\')
            FrontendBasePort = 8061
            BackendBasePort = 48061
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
    $registryPath = Get-BranchRuntimePortRegistryPath
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
            $activeProperty = $_.PSObject.Properties['active']
            $null -ne $activeProperty -and [bool]$activeProperty.Value
        })

    if ($activeMatches.Count -eq 0) {
        throw "Worktree port registry entry for '$fullRoot' is not active."
    }
    if ($activeMatches.Count -gt 1) {
        throw "Duplicate active worktree port registry entries for '$fullRoot'."
    }

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
    if ($slot -le 0) {
        throw "Registered worktree slot for '$fullRoot' must be a positive integer, got $slot."
    }

    $profile = $profileMatches[0]
    $frontendPort = [int](Get-RequiredRegistryValue -Entry $entry -PropertyName 'frontendPort' -RepoRoot $fullRoot)
    $backendPort = [int](Get-RequiredRegistryValue -Entry $entry -PropertyName 'backendPort' -RepoRoot $fullRoot)
    $expectedFrontendPort = $profile.FrontendBasePort + $slot
    $expectedBackendPort = $profile.BackendBasePort + $slot
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
            if ($normalizedRoot.IndexOf($marker, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                if ($profile.Branches -notcontains $Branch) {
                    throw "Workspace '$fullRoot' belongs to profile '$($profile.Name)' but current branch is '$Branch'. Switch to one of: $($profile.Branches -join ', ')."
                }
                return New-BranchRuntimeContext -Profile $profile -Slot $slot
            }
        }
    }

    foreach ($profile in $profiles) {
        if ($profile.Branches -contains $Branch) {
            return New-BranchRuntimeContext -Profile $profile -Slot $slot
        }
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

    if ($Slot -lt 0) {
        throw "Runtime slot must be zero or positive, got $Slot."
    }

    [pscustomobject]@{
        Profile = $Profile.Name
        Slot = $Slot
        FrontendPort = $Profile.FrontendBasePort + $Slot
        BackendPort = $Profile.BackendBasePort + $Slot
        FrontendMode = $Profile.FrontendMode
        FrontendUrl = "http://127.0.0.1:$($Profile.FrontendBasePort + $Slot)"
        BackendHealthUrl = "http://127.0.0.1:$($Profile.BackendBasePort + $Slot)/actuator/health"
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
