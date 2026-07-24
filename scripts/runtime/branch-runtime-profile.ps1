Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:PortContractVersion = '2026-07-24-branch-runtime-v1'

function Get-BranchRuntimeProfiles {
    @(
        [pscustomobject]@{
            Name = 'int_main'
            Branches = @('int_main')
            PathMarkers = @('\ProjectPackage\IntRuoyi\IntRuoyiAll\', '\IntRuoyi\')
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

function Resolve-BranchRuntimeProfile {
    param(
        [string]$RepoRoot = (Get-CurrentRepoRoot),
        [string]$Branch = (Get-GitValue -RepoRoot (Get-CurrentRepoRoot) -Arguments @('branch', '--show-current'))
    )

    $fullRoot = [System.IO.Path]::GetFullPath($RepoRoot)
    $normalizedRoot = $fullRoot.Replace('/', '\')
    $profiles = Get-BranchRuntimeProfiles

    foreach ($profile in $profiles | Where-Object { $_.Name -ne 'int_main' }) {
        foreach ($marker in $profile.PathMarkers) {
            if ($normalizedRoot.IndexOf($marker, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                if ($profile.Branches -notcontains $Branch) {
                    throw "Workspace '$fullRoot' belongs to profile '$($profile.Name)' but current branch is '$Branch'. Switch to one of: $($profile.Branches -join ', ')."
                }
                return $profile
            }
        }
    }

    foreach ($profile in $profiles) {
        if ($profile.Branches -contains $Branch) {
            return $profile
        }
    }

    throw "No branch runtime profile is registered for branch '$Branch' at '$fullRoot'."
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
