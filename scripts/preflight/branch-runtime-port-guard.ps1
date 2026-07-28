Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (& git rev-parse --show-toplevel).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repoRoot)) {
    throw 'Current directory is not inside a Git repository.'
}
$repoRoot = [System.IO.Path]::GetFullPath($repoRoot)

. (Join-Path $repoRoot 'scripts\runtime\branch-runtime-profile.ps1')

function Read-Text {
    param([Parameter(Mandatory = $true)][string]$RelativePath)
    $path = Join-Path $repoRoot $RelativePath
    if (-not (Test-Path $path)) {
        throw "Missing required file: $RelativePath"
    }
    [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
}

function Assert-Contains {
    param(
        [Parameter(Mandatory = $true)][string]$RelativePath,
        [Parameter(Mandatory = $true)][string[]]$Needles
    )
    $content = Read-Text -RelativePath $RelativePath
    foreach ($needle in $Needles) {
        if (-not $content.Contains($needle)) {
            throw "File '$RelativePath' is missing required text: $needle"
        }
    }
}

function Assert-EnvPort {
    param(
        [Parameter(Mandatory = $true)][string]$RelativePath,
        [Parameter(Mandatory = $true)][int]$FrontendPort,
        [Parameter(Mandatory = $true)][int]$BackendPort
    )
    Assert-Contains -RelativePath $RelativePath -Needles @(
        "VITE_PORT=$FrontendPort",
        "VITE_BASE_URL='http://127.0.0.1:$BackendPort'",
        "VITE_PROXY_TARGET='http://127.0.0.1:$BackendPort'"
    )
}

$branch = Get-GitValue -RepoRoot $repoRoot -Arguments @('branch', '--show-current')
$context = Resolve-BranchRuntimeContext -RepoRoot $repoRoot -Branch $branch
$profile = $context.Profile
$ports = $context.Ports

Assert-Contains -RelativePath 'docs\branch-runtime-ports.md' -Needles @(
    $script:PortContractVersion,
    '`int_main_d`',
    'D:\ProjectPackage\IntRuoyi\IntRuoyiAll',
    '`8101`',
    '`48101`',
    '`int_main`',
    'E:\IntRuoyi',
    '`8081`',
    '`48081`',
    '`int_batch`',
    'E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll',
    '`8041`',
    '`48041`',
    '`int_shedule`',
    'E:\IntRuoyiBranch\Shedule\IntRuoyiAll',
    '`8021`',
    '`48021`',
    '`int_qms`',
    'E:\IntRuoyiBranch\QMS\IntRuoyiAll',
    '`8061`',
    '`48061`',
    '`1..19`',
    'reserve-worktree-slot.ps1'
)

Assert-Contains -RelativePath 'docs\local-runtime.md' -Needles @(
    'PORT_CONTRACT_VERSION: 2026-07-26-branch-runtime-v3',
    '`int_main_d`',
    '`8101`',
    '`48101`',
    '`int_main`',
    '`8081`',
    '`48081`',
    '`int_batch`',
    '`8041`',
    '`48041`',
    '`int_shedule`',
    '`8021`',
    '`48021`',
    '`int_qms`',
    '`8061`',
    '`48061`',
    '`1..19`',
    'reserve-worktree-slot.ps1'
)

Assert-Contains -RelativePath 'docs\worktree-restrictions.md' -Needles @(
    'PORT_CONTRACT_VERSION: 2026-07-26-branch-runtime-v3',
    '`int_main_d` profile',
    '`int_main` profile',
    '`int_batch` profile',
    '`int_shedule` profile',
    '`int_qms` profile',
    'profile',
    'slot = 1..19',
    'reserve-worktree-slot.ps1'
)

Assert-Contains -RelativePath 'docs\codex-branch-runtime-handoff.md' -Needles @(
    'PORT_CONTRACT_VERSION: 2026-07-26-branch-runtime-v3',
    'reserve-worktree-slot.ps1',
    '`int_main_d`',
    '`1..19`'
)

Assert-Contains -RelativePath 'AGENTS.md' -Needles @(
    'Branch runtime port matrix',
    'docs\branch-runtime-ports.md',
    'int_main_d=8101/48101',
    'slot in `1..19`',
    'reserve-worktree-slot.ps1'
)

Assert-Contains -RelativePath 'IntRuoyiFronted\vite.config.ts' -Needles @(
    'processEnvOverrides',
    '...processEnvOverrides'
)

Assert-EnvPort -RelativePath 'IntRuoyiFronted\.env.branch-batch' -FrontendPort 8041 -BackendPort 48041
Assert-EnvPort -RelativePath 'IntRuoyiFronted\.env.branch-shedule' -FrontendPort 8021 -BackendPort 48021
Assert-EnvPort -RelativePath 'IntRuoyiFronted\.env.branch-qms' -FrontendPort 8061 -BackendPort 48061
Assert-EnvPort -RelativePath 'IntRuoyiFronted\.env.branch-main-d' -FrontendPort 8101 -BackendPort 48101

$legacySheduleEnv = Join-Path $repoRoot 'IntRuoyiFronted\.env.shedule'
if (Test-Path $legacySheduleEnv) {
    Assert-EnvPort -RelativePath 'IntRuoyiFronted\.env.shedule' -FrontendPort 8021 -BackendPort 48021
}

foreach ($required in @(
    'scripts\runtime\branch-runtime-profile.ps1',
    'scripts\runtime\reserve-worktree-slot.ps1',
    'scripts\runtime\show-branch-runtime.ps1',
    'scripts\runtime\start-branch-frontend.ps1',
    'scripts\runtime\start-branch-backend.ps1',
    'scripts\git\install-branch-runtime-hooks.ps1',
    '.githooks\pre-commit',
    '.githooks\pre-merge-commit',
    '.githooks\post-merge',
    '.githooks\pre-push'
)) {
    if (-not (Test-Path (Join-Path $repoRoot $required))) {
        throw "Missing required file: $required"
    }
}

$registryEntries = @(Read-BranchRuntimePortRegistryEntries)
Assert-BranchRuntimePortRegistryEntries -Entries $registryEntries

$hooksPath = (& git -C $repoRoot config --get core.hooksPath)
if ($LASTEXITCODE -ne 0 -or $hooksPath -ne '.githooks') {
    throw "Git hooks are not installed for this workspace. Run scripts\git\install-branch-runtime-hooks.ps1."
}

Write-Output "Branch runtime port guard passed for $branch/$($profile.Name): frontend $($ports.FrontendPort), backend $($ports.BackendPort)."
