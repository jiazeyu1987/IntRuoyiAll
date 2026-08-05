$ErrorActionPreference = 'Continue'

$mainRoot = 'E:\IntRuoyi'
$allowedRoot = (Resolve-Path -LiteralPath 'D:\IntRuoyiWorktree').Path.TrimEnd('\')
$targets = @(
    'D:\IntRuoyiWorktree\2020804_qa',
    'D:\IntRuoyiWorktree\20260731_shengchanbanzuzhang',
    'D:\IntRuoyiWorktree\20260803_pqf',
    'D:\IntRuoyiWorktree\20260805-integrate-production-personnel',
    'D:\IntRuoyiWorktree\20260805-process-loss-reasons',
    'D:\IntRuoyiWorktree\controlled-file-category-e2e-20260803',
    'D:\IntRuoyiWorktree\dcc-approval-action-panel-left-column',
    'D:\IntRuoyiWorktree\dcc-approval-role-display',
    'D:\IntRuoyiWorktree\form-center-route-missing-20260803',
    'D:\IntRuoyiWorktree\pml-test-r260731',
    'D:\IntRuoyiWorktree\process-pool-full-chain-closure',
    'D:\IntRuoyiWorktree\production-leader-tab-20260804',
    'D:\IntRuoyiWorktree\profile-nas-table-auto-sync',
    'D:\IntRuoyiWorktree\r260730a-release-app',
    'D:\IntRuoyiWorktree\r260731a-release-app',
    'D:\IntRuoyiWorktree\r260731b-release-app',
    'D:\IntRuoyiWorktree\r260731c-release-app',
    'D:\IntRuoyiWorktree\r260803a-release-app',
    'D:\IntRuoyiWorktree\release-third-party-feedback-20260801',
    'D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803',
    'D:\IntRuoyiWorktree\third-party-feedback-import-20260802',
    'D:\IntRuoyiWorktree\worktree_20260803_p0',
    'D:\IntRuoyiWorktree\worktree_20260805_ac_m20_pqc_review'
)

$registeredRaw = @(git -C $mainRoot worktree list --porcelain)
$results = @()
$emptyMirror = Join-Path $PSScriptRoot 'empty-node-modules-mirror'
if (-not (Test-Path -LiteralPath $emptyMirror)) {
    New-Item -ItemType Directory -Path $emptyMirror | Out-Null
}

if (@(Get-ChildItem -LiteralPath $emptyMirror -Force).Count -ne 0) {
    throw "Empty mirror directory is not empty: $emptyMirror"
}

foreach ($path in $targets) {
    $existsBefore = Test-Path -LiteralPath $path
    if (-not $existsBefore) {
        $results += [pscustomobject]@{ Path = $path; Status = 'ALREADY_ABSENT'; ExistsAfter = $false; Message = '' }
        continue
    }

    $resolved = (Resolve-Path -LiteralPath $path).Path
    $normalized = $resolved.Replace('/', '\')
    $underRoot = $normalized.StartsWith($allowedRoot, [System.StringComparison]::OrdinalIgnoreCase)
    $registered = $registeredRaw -contains ('worktree ' + $path.Replace('\', '/'))
    $gitFile = Test-Path -LiteralPath (Join-Path $path '.git')
    $procCount = @(Get-CimInstance Win32_Process | Where-Object {
            $cmd = $_.CommandLine
            $_.ProcessId -ne $PID -and $cmd -and (
                $cmd.IndexOf($path, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
                $cmd.IndexOf($path.Replace('\', '/'), [System.StringComparison]::OrdinalIgnoreCase) -ge 0
            )
        }).Count

    if (-not $underRoot -or $registered -or $gitFile -or $procCount -gt 0) {
        $results += [pscustomobject]@{
            Path = $path
            Status = 'SKIPPED_GATE'
            ExistsAfter = $true
            Message = "underRoot=$underRoot registered=$registered gitFile=$gitFile procCount=$procCount"
        }
        continue
    }

    try {
        $nodeModules = Join-Path $path 'IntRuoyiFronted\node_modules'
        if (Test-Path -LiteralPath $nodeModules) {
            $robocopyOutput = @(robocopy $emptyMirror $nodeModules /MIR /R:0 /W:0 /NFL /NDL /NJH /NJS /NC /NS /NP)
            $robocopyExit = $LASTEXITCODE
            if ($robocopyExit -gt 7) {
                throw "robocopy mirror failed exit=$robocopyExit output=$($robocopyOutput -join ' | ')"
            }
        }
        Remove-Item -LiteralPath $path -Recurse -Force -ErrorAction Stop
        $existsAfter = Test-Path -LiteralPath $path
        $results += [pscustomobject]@{
            Path = $path
            Status = if ($existsAfter) { 'DELETE_RETURNED_EXISTS' } else { 'RESIDUAL_REMOVED' }
            ExistsAfter = $existsAfter
            Message = ''
        }
    } catch {
        $existsAfter = Test-Path -LiteralPath $path
        $results += [pscustomobject]@{
            Path = $path
            Status = 'DELETE_FAILED'
            ExistsAfter = $existsAfter
            Message = $_.Exception.Message
        }
    }
}

$results | ForEach-Object {
    '{0}`t{1}`t{2}`t{3}' -f $_.Status, $_.ExistsAfter, $_.Path, $_.Message
}

'SUMMARY'
$results | Group-Object Status | Sort-Object Name | ForEach-Object {
    '{0}={1}' -f $_.Name, $_.Count
}

'FAILED_OR_SKIPPED'
$results | Where-Object { $_.Status -notin @('RESIDUAL_REMOVED', 'ALREADY_ABSENT') } | ConvertTo-Json -Depth 3
