$ErrorActionPreference = 'Stop'

$scriptPath = Join-Path $PSScriptRoot '..\deploy\worktree-port-map.ps1'
if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "Missing worktree port map script: $scriptPath"
}

. $scriptPath

function Assert-Equal {
    param(
        [object]$Actual,
        [object]$Expected,
        [string]$Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message Expected=[$Expected] Actual=[$Actual]"
    }
}

function New-FakeWorktree {
    param(
        [string]$Name,
        [string]$Kind,
        [string]$Root = 'D:\ProjectPackage\Int\IntRuoyi'
    )

    $relative = if ($Name -eq 'int_main') {
        if ($Kind -eq 'frontend') { 'IntRuoyiFronted' } else { 'ruoyi-vue-pro' }
    } else {
        if ($Kind -eq 'frontend') { "worktrees\$Name\IntRuoyiFronted" } else { "worktrees\$Name\ruoyi-vue-pro" }
    }

    [PSCustomObject]@{
        Name = $Name
        Path = Join-Path $Root $relative
        Branch = if ($Name -eq 'int_main') { 'int_main' } else { "task/$Name" }
    }
}

$codexFlatBackendName = ConvertTo-IntRuoyiWorktreeName `
    -Path 'D:\ProjectPackage\Int\IntRuoyi\worktrees\ruoyi-vue-pro-dcc-nas-transfer-mirror-verify-20260525' `
    -Branch 'refs/heads/codex/dcc-nas-transfer-mirror-verify-20260525' `
    -RepoFolder 'ruoyi-vue-pro'
Assert-Equal $codexFlatBackendName 'dcc-nas-transfer-mirror-verify-20260525' 'Codex flat backend worktree should derive the branch task name.'

$flatBackendName = ConvertTo-IntRuoyiWorktreeName `
    -Path 'D:\ProjectPackage\Int\IntRuoyi\worktrees\ruoyi-vue-pro-dcc-nas-transfer-mirror-verify-20260525' `
    -Branch '' `
    -RepoFolder 'ruoyi-vue-pro'
Assert-Equal $flatBackendName 'dcc-nas-transfer-mirror-verify-20260525' 'Flat backend worktree path should derive the task name when branch is unavailable.'

$intRuoyiWorktreesFlatBackendName = ConvertTo-IntRuoyiWorktreeName `
    -Path 'D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-showroom-legacy-order-release-clean' `
    -Branch '' `
    -RepoFolder 'ruoyi-vue-pro'
Assert-Equal $intRuoyiWorktreesFlatBackendName 'showroom-legacy-order-release-clean' 'Flat backend worktree under IntRuoyiWorktrees should derive the task name when branch is unavailable.'

$intRuoyiWorktreesFlatFrontendName = ConvertTo-IntRuoyiWorktreeName `
    -Path 'D:\ProjectPackage\Int\IntRuoyiWorktrees\IntRuoyiFronted-showroom-legacy-order-release-clean' `
    -Branch '' `
    -RepoFolder 'IntRuoyiFronted'
Assert-Equal $intRuoyiWorktreesFlatFrontendName 'showroom-legacy-order-release-clean' 'Flat frontend worktree under IntRuoyiWorktrees should derive the task name when branch is unavailable.'

$detachedBackendName = ConvertTo-IntRuoyiWorktreeName `
    -Path 'D:\ProjectPackage\Int\IntRuoyiWorktrees\20260623-dcc-batch-recognition-test\ruoyi-vue-pro' `
    -Branch '' `
    -RepoFolder 'ruoyi-vue-pro'
Assert-Equal $detachedBackendName '20260623-dcc-batch-recognition-test' 'Detached backend worktree under IntRuoyiWorktrees should derive the task name from its parent directory.'

$shortBackendName = ConvertTo-IntRuoyiWorktreeName `
    -Path 'D:\ProjectPackage\Int\IntRuoyiWorktrees\r260708dccfix\b' `
    -Branch '' `
    -RepoFolder 'ruoyi-vue-pro'
Assert-Equal $shortBackendName 'r260708dccfix' 'Short backend worktree folder b should derive the task name from its parent directory.'

$shortFrontendName = ConvertTo-IntRuoyiWorktreeName `
    -Path 'D:\ProjectPackage\Int\IntRuoyiWorktrees\r260708dccfix\f' `
    -Branch '' `
    -RepoFolder 'IntRuoyiFronted'
Assert-Equal $shortFrontendName 'r260708dccfix' 'Short frontend worktree folder f should derive the task name from its parent directory.'

$releaseBackendName = ConvertTo-IntRuoyiWorktreeName `
    -Path 'D:\ProjectPackage\Int\release-worktrees\IntRuoyi-backend-20260624-bdf2e35\ruoyi-vue-pro' `
    -Branch '' `
    -RepoFolder 'ruoyi-vue-pro'
Assert-Equal $releaseBackendName 'IntRuoyi-backend-20260624-bdf2e35' 'Detached backend worktree under release-worktrees should derive the task name from its parent directory.'

$runtimeCleanBuildSkipped = $false
try {
    ConvertTo-IntRuoyiWorktreeName `
        -Path 'E:\Int\CacheData\IntRuoyi\runtime\clean-build-worktrees\backend-restart-20260702-230145' `
        -Branch '' `
        -RepoFolder 'ruoyi-vue-pro' | Out-Null
} catch {
    $runtimeCleanBuildSkipped = $_.Exception.Message -like '*Cannot derive IntRuoyi worktree name*'
}
Assert-Equal $runtimeCleanBuildSkipped $true 'Runtime clean-build worktrees are not business worktrees and must be filtered before name conversion.'

$frontend = @(
    New-FakeWorktree -Name 'int_main' -Kind 'frontend'
    New-FakeWorktree -Name '20260524-doc-readiness-worktree-check' -Kind 'frontend'
    New-FakeWorktree -Name 'automation-2-ebr-visual-fidelity-20260524-review' -Kind 'frontend'
    New-FakeWorktree -Name 'edhr-test' -Kind 'frontend'
)
$backend = @(
    New-FakeWorktree -Name 'int_main' -Kind 'backend'
    New-FakeWorktree -Name '20260524-doc-readiness-worktree-check' -Kind 'backend'
    New-FakeWorktree -Name 'automation-2-ebr-visual-fidelity-20260524-review' -Kind 'backend'
    New-FakeWorktree -Name 'edhr-test' -Kind 'backend'
)

$initial = New-IntRuoyiWorktreePortPlan -FrontendWorktrees $frontend -BackendWorktrees $backend -ExistingRegistry $null
Assert-Equal $initial.Assignments.Count 4 'Initial plan should include all active paired worktrees.'
Assert-Equal $initial.Assignments[0].Name 'int_main' 'int_main should be first.'
Assert-Equal $initial.Assignments[0].FrontendPort 8081 'int_main frontend port should be fixed.'
Assert-Equal $initial.Assignments[0].BackendPort 48081 'int_main backend port should be fixed.'
Assert-Equal $initial.Assignments[1].FrontendPort 8082 'First task frontend port should increment.'
Assert-Equal $initial.Assignments[1].BackendPort 48082 'First task backend port should increment.'
Assert-Equal $initial.Assignments[3].FrontendPort 8084 'Third task frontend port should increment.'
Assert-Equal $initial.Assignments[3].BackendPort 48084 'Third task backend port should increment.'

$registry = [PSCustomObject]@{
    BaseFrontendPort = 8081
    BaseBackendPort = 48081
    Assignments = @(
        [PSCustomObject]@{ Name = 'int_main'; FrontendPort = 8081; BackendPort = 48081; Active = $true }
        [PSCustomObject]@{ Name = 'removed-task'; FrontendPort = 8082; BackendPort = 48082; Active = $false }
        [PSCustomObject]@{ Name = 'edhr-test'; FrontendPort = 8083; BackendPort = 48083; Active = $true }
    )
}
$newFrontend = @(
    New-FakeWorktree -Name 'int_main' -Kind 'frontend'
    New-FakeWorktree -Name 'edhr-test' -Kind 'frontend'
    New-FakeWorktree -Name 'new-task' -Kind 'frontend'
)
$newBackend = @(
    New-FakeWorktree -Name 'int_main' -Kind 'backend'
    New-FakeWorktree -Name 'edhr-test' -Kind 'backend'
    New-FakeWorktree -Name 'new-task' -Kind 'backend'
)
$preserved = New-IntRuoyiWorktreePortPlan -FrontendWorktrees $newFrontend -BackendWorktrees $newBackend -ExistingRegistry $registry
$newTask = $preserved.Assignments | Where-Object { $_.Name -eq 'new-task' } | Select-Object -First 1
Assert-Equal $newTask.FrontendPort 8084 'New worktree should use next frontend port after historical max.'
Assert-Equal $newTask.BackendPort 48084 'New worktree should use next backend port after historical max.'

$mismatchedBackend = @(
    New-FakeWorktree -Name 'int_main' -Kind 'backend'
    New-FakeWorktree -Name 'different-task' -Kind 'backend'
)
$mismatchFailed = $false
try {
    [void](New-IntRuoyiWorktreePortPlan -FrontendWorktrees $newFrontend -BackendWorktrees $mismatchedBackend -ExistingRegistry $null)
} catch {
    $mismatchFailed = $_.Exception.Message -like '*mismatch*'
}
Assert-Equal $mismatchFailed $true 'Mismatched frontend/backend worktree sets should fail fast.'

$currentBackendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$mainContext = New-IntRuoyiMainPortContext -CurrentBackendRepoRoot $currentBackendRoot
Assert-Equal (Split-Path -Leaf $mainContext.FrontendPath) 'IntRuoyiFronted' 'int_main context should use the current frontend root.'
Assert-Equal $mainContext.FrontendPort 8081 'int_main context frontend port should remain fixed.'
Assert-Equal $mainContext.BackendPort 48081 'int_main context backend port should remain fixed.'

Write-Host 'worktree-port-map tests passed'
