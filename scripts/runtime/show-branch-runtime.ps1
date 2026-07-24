param(
    [int]$Slot = 0
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\branch-runtime-profile.ps1"

$repoRoot = Get-CurrentRepoRoot
$branch = Get-GitValue -RepoRoot $repoRoot -Arguments @('branch', '--show-current')
$profile = Resolve-BranchRuntimeProfile -RepoRoot $repoRoot -Branch $branch
$ports = Get-BranchRuntimePorts -Profile $profile -Slot $Slot

[pscustomobject]@{
    ContractVersion = $script:PortContractVersion
    RepoRoot = $repoRoot
    Branch = $branch
    Profile = $profile.Name
    Slot = $ports.Slot
    FrontendPort = $ports.FrontendPort
    BackendPort = $ports.BackendPort
    FrontendMode = $ports.FrontendMode
    FrontendUrl = $ports.FrontendUrl
    BackendHealthUrl = $ports.BackendHealthUrl
} | Format-List
