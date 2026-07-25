param(
    [Nullable[int]]$Slot = $null
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\branch-runtime-profile.ps1"

$repoRoot = Get-CurrentRepoRoot
$branch = Get-GitValue -RepoRoot $repoRoot -Arguments @('branch', '--show-current')
$context = Resolve-BranchRuntimeContext -RepoRoot $repoRoot -Branch $branch -RequestedSlot $Slot
$profile = $context.Profile
$ports = $context.Ports

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
