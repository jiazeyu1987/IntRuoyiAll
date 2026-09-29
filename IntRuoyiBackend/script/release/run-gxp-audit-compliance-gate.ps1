[CmdletBinding()]
param(
    [switch]$SkipMaven,
    [switch]$SkipPolicyStatic
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $utf8NoBom
[Console]::OutputEncoding = $utf8NoBom
$OutputEncoding = $utf8NoBom

$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$workspaceRoot = (Resolve-Path (Join-Path $backendRoot '..')).Path

if ($SkipMaven -and $SkipPolicyStatic) {
    [Console]::Out.WriteLine('INCOMPLETE: all software gate stages were skipped.')
    exit 2
}

$executed = 0L
$skipped = 0L
if (-not $SkipMaven) {
    $moduleRoot = Join-Path $backendRoot 'yudao-module-system'
    $sourceRoot = Join-Path $moduleRoot 'src\test\java'
    $testFiles = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter 'GxpAudit*Test.java')
    # Required baseline plus newly added system GxpAudit tests: losing a core target is an error.
    $requiredTests = @(
        'GxpAuditPolicyBundleLoaderTest', 'GxpAuditPolicyActivationTest',
        'GxpAuditPolicyActivationReplayBehaviorTest', 'GxpAuditPolicyActivationServiceContractTest',
        'GxpAuditV2ContractTest', 'GxpAuditServiceImplTest', 'GxpAuditQueryServiceImplTest',
        'GxpAuditQueryContractTest', 'GxpAuditAttemptServiceImplTest', 'GxpAuditPersistenceModelTest'
    )
    foreach ($required in $requiredTests) {
        if (@($testFiles | Where-Object BaseName -eq $required).Count -ne 1) {
            throw "Missing or ambiguous target test file: $required.java"
        }
    }
    $targets = @($testFiles | Sort-Object FullName | ForEach-Object {
        $source = Get-Content -LiteralPath $_.FullName -Raw -Encoding utf8
        $package = [regex]::Match($source, '(?m)^\s*package\s+([\w.]+)\s*;')
        if (-not $package.Success) { throw "Missing Java package in target test: $($_.FullName)" }
        $package.Groups[1].Value + '.' + $_.BaseName
    })
    $selector = $targets -join ','
    # Surefire reportNameSuffix binds XML file names to this invocation, without deleting
    # shared target reports or trusting timestamps / reports from a previous Maven run.
    $runId = 'gxp-' + [guid]::NewGuid().ToString('N')
    $reportRoot = Join-Path $moduleRoot 'target\surefire-reports'
    [Console]::Out.WriteLine("GxP system test run=$runId targets=$($targets.Count)")
    Push-Location $backendRoot
    try {
        & mvn -pl yudao-module-system -am `
            "-Dtest=$selector" `
            '-Dsurefire.failIfNoSpecifiedTests=false' `
            "-Dsurefire.reportNameSuffix=$runId" `
            test
        $mavenExit = $LASTEXITCODE
        if ($mavenExit -ne 0) {
            [Console]::Error.WriteLine("GxP audit Maven gate failed with exit code $mavenExit")
            exit $mavenExit
        }
    } finally {
        Pop-Location
    }

    foreach ($target in $targets) {
        $report = Join-Path $reportRoot "TEST-$target-$runId.xml"
        if (-not (Test-Path -LiteralPath $report -PathType Leaf)) {
            throw "Missing current-run target Surefire report: $report"
        }
        [xml]$document = Get-Content -LiteralPath $report -Raw -Encoding utf8
        $suite = $document.DocumentElement
        if ($suite.LocalName -ne 'testsuite' -or $suite.GetAttribute('name') -ne "$target($runId)") {
            throw "Unexpected target suite identity: $report"
        }
        $counts = @{}
        foreach ($attribute in @('tests', 'skipped', 'failures', 'errors')) {
            $value = $suite.GetAttribute($attribute)
            if ($value -notmatch '^\d+$') { throw "Invalid $attribute count: $report" }
            $counts[$attribute] = [long]$value
        }
        if ($counts.failures -gt 0 -or $counts.errors -gt 0) {
            throw "Target test failures/errors in $report"
        }
        if ($counts.tests -le 0 -or $counts.skipped -ge $counts.tests) {
            throw "Target has zero executed tests: $target"
        }
        $executed += $counts.tests - $counts.skipped
        $skipped += $counts.skipped
    }
}

if (-not $SkipPolicyStatic) {
    $staticTest = Join-Path $backendRoot 'script\tests\test_gxp_audit_policy_static.py'
    $coverageGate = Join-Path $backendRoot 'script\gxp_audit_coverage_gate.py'
    foreach ($inputFile in @($staticTest, $coverageGate)) {
        if (-not (Test-Path -LiteralPath $inputFile -PathType Leaf)) {
            throw "Missing policy gate input: $inputFile"
        }
    }
    Push-Location $workspaceRoot
    try {
        & python -X utf8 -m pytest $staticTest
        $staticExit = $LASTEXITCODE
        if ($staticExit -ne 0) {
            [Console]::Error.WriteLine("GxP audit policy static gate failed with exit code $staticExit")
            exit $staticExit
        }
        & python -X utf8 $coverageGate --root $workspaceRoot
        $coverageExit = $LASTEXITCODE
        if ($coverageExit -ne 0) {
            [Console]::Error.WriteLine("GxP audit coverage gate failed with exit code $coverageExit")
            exit $coverageExit
        }
    } finally {
        Pop-Location
    }
}

if ($SkipMaven -or $SkipPolicyStatic -or $skipped -gt 0) {
    [Console]::Out.WriteLine("INCOMPLETE: testsExecuted=$executed testsSkipped=$skipped SkipMaven=$SkipMaven SkipPolicyStatic=$SkipPolicyStatic")
    exit 2
}

[Console]::Out.WriteLine("GxP audit software gate passed: testsExecuted=$executed testsSkipped=0; not a full compliance certification or production activation approval.")
