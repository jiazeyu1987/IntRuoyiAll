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

$backendRoot = Resolve-Path (Join-Path -Path $PSScriptRoot -ChildPath '..\..')
$workspaceRoot = Resolve-Path (Join-Path -Path $backendRoot -ChildPath '..')

if (-not $SkipMaven) {
    Push-Location $backendRoot
    try {
        & mvn -pl yudao-module-signature -am `
            '-Dtest=GxpAuditTrailSchemaContractTest,GxpAuditTrailServiceContractTest' `
            '-Dsurefire.failIfNoSpecifiedTests=false' `
            test
        if ($LASTEXITCODE -ne 0) {
            throw "GxP audit Maven contract gate failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

if (-not $SkipPolicyStatic) {
    Push-Location $workspaceRoot
    try {
        & python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_gxp_audit_policy_static.py
        if ($LASTEXITCODE -ne 0) {
            throw "GxP audit policy static gate failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

[Console]::Out.WriteLine('GxP audit compliance gate passed.')
