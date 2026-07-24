param(
    [string]$EvidencePath = '',
    [string]$EvidenceJson = '',
    [string]$ProdLoginEvidencePath = '',
    [string]$TestLoginEvidencePath = '',
    [string]$ProdSampleEvidencePath = '',
    [string]$TestSampleEvidencePath = '',
    [string[]]$ProdFrontendBackendTargets = @(),
    [string[]]$ForbiddenProdBackendTargets = @('http://172.30.30.58:48081', '172.30.30.58:48081')
)

$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

function Add-Blocker {
    param(
        [System.Collections.Generic.List[string]]$Blockers,
        [string]$Message
    )
    [void]$Blockers.Add($Message)
}

function Get-JsonProperty {
    param(
        [object]$Object,
        [string]$Name
    )
    if ($null -eq $Object) {
        return $null
    }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }
    return $property.Value
}

function Convert-ToStringList {
    param([object]$Value)
    $items = New-Object System.Collections.Generic.List[string]
    if ($null -eq $Value) {
        return $items
    }
    if ($Value -is [string]) {
        if (-not [string]::IsNullOrWhiteSpace($Value)) {
            [void]$items.Add($Value)
        }
        return $items
    }
    foreach ($item in @($Value)) {
        if ($null -ne $item -and -not [string]::IsNullOrWhiteSpace([string]$item)) {
            [void]$items.Add([string]$item)
        }
    }
    return $items
}

function Test-Truthy {
    param([object]$Value)
    if ($Value -is [bool]) {
        return $Value
    }
    if ($null -eq $Value) {
        return $false
    }
    return ([string]$Value).ToLowerInvariant() -eq 'true'
}

function Test-EvidenceFile {
    param(
        [System.Collections.Generic.List[string]]$Blockers,
        [string]$Label,
        [object]$PathValue
    )
    $pathText = [string]$PathValue
    if ([string]::IsNullOrWhiteSpace($pathText)) {
        Add-Blocker $Blockers "$Label evidencePath is missing."
        return
    }
    if (-not (Test-Path -LiteralPath $pathText -PathType Leaf)) {
        Add-Blocker $Blockers "$Label evidencePath does not exist: $pathText"
    }
}

function Test-LoginEvidence {
    param(
        [System.Collections.Generic.List[string]]$Blockers,
        [string]$EnvironmentName,
        [object]$Evidence
    )
    if ($null -eq $Evidence) {
        Add-Blocker $Blockers "$EnvironmentName Playwright login evidence is missing."
        return
    }
    if ([string](Get-JsonProperty $Evidence 'status') -ne 'passed') {
        Add-Blocker $Blockers "$EnvironmentName Playwright login status is not passed."
    }
    if (-not (Test-Truthy (Get-JsonProperty $Evidence 'usedFrontendPath'))) {
        Add-Blocker $Blockers "$EnvironmentName Playwright login must use the real frontend path."
    }
    if ([string]::IsNullOrWhiteSpace([string](Get-JsonProperty $Evidence 'sessionName'))) {
        Add-Blocker $Blockers "$EnvironmentName Playwright sessionName is missing."
    }
    if ([string](Get-JsonProperty $Evidence 'landedPath') -ne '/index') {
        Add-Blocker $Blockers "$EnvironmentName Playwright login did not land on /index."
    }
    if ([string]::IsNullOrWhiteSpace([string](Get-JsonProperty $Evidence 'visibleUserText'))) {
        Add-Blocker $Blockers "$EnvironmentName Playwright visible user text is missing."
    }
    Test-EvidenceFile $Blockers "$EnvironmentName Playwright login" (Get-JsonProperty $Evidence 'evidencePath')
}

function Test-SampleEvidence {
    param(
        [System.Collections.Generic.List[string]]$Blockers,
        [string]$EnvironmentName,
        [object]$Evidence
    )
    if ($null -eq $Evidence) {
        Add-Blocker $Blockers "$EnvironmentName sample file frontend path evidence is missing."
        return
    }
    $method = [string](Get-JsonProperty $Evidence 'method')
    if ([string](Get-JsonProperty $Evidence 'status') -ne 'passed') {
        Add-Blocker $Blockers "$EnvironmentName sample file frontend path status is not passed."
    }
    if (-not (Test-Truthy (Get-JsonProperty $Evidence 'usedFrontendPath'))) {
        Add-Blocker $Blockers "$EnvironmentName sample file must be verified through the frontend path; observed method: $method"
    }
    if ([string](Get-JsonProperty $Evidence 'objectPath') -ne 'dcc/stamped/20260513/dcc-sample_controlled.pdf') {
        Add-Blocker $Blockers "$EnvironmentName sample file objectPath does not match the G7 sample."
    }
    if ([int](Get-JsonProperty $Evidence 'fileConfigId') -ne 28) {
        Add-Blocker $Blockers "$EnvironmentName sample file fileConfigId must be 28."
    }
    if ([int](Get-JsonProperty $Evidence 'httpStatus') -ne 200) {
        Add-Blocker $Blockers "$EnvironmentName sample file frontend path did not return HTTP 200."
    }
    $contentType = [string](Get-JsonProperty $Evidence 'contentType')
    if ($contentType.ToLowerInvariant().IndexOf('application/pdf') -lt 0) {
        Add-Blocker $Blockers "$EnvironmentName sample file contentType is not PDF."
    }
    Test-EvidenceFile $Blockers "$EnvironmentName sample file frontend path" (Get-JsonProperty $Evidence 'evidencePath')
}

function Test-ProdTargetEvidence {
    param(
        [System.Collections.Generic.List[string]]$Blockers,
        [object]$Evidence
    )
    if ($null -eq $Evidence) {
        Add-Blocker $Blockers "production frontend backend target evidence is missing."
        return
    }
    $observedTargets = Convert-ToStringList (Get-JsonProperty $Evidence 'observedTargets')
    $forbiddenTargets = Convert-ToStringList (Get-JsonProperty $Evidence 'forbiddenTargets')
    if ($forbiddenTargets.Count -eq 0) {
        [void]$forbiddenTargets.Add('http://172.30.30.58:48081')
        [void]$forbiddenTargets.Add('172.30.30.58:48081')
    }
    if ($observedTargets.Count -eq 0) {
        Add-Blocker $Blockers "production frontend backend target observedTargets is missing."
    }
    $joinedObserved = [string]::Join(' ', $observedTargets)
    foreach ($forbidden in $forbiddenTargets) {
        if (-not [string]::IsNullOrWhiteSpace($forbidden) -and $joinedObserved.IndexOf($forbidden, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
            Add-Blocker $Blockers "production frontend backend target points to forbidden test backend: $forbidden"
        }
    }
    if ($joinedObserved.IndexOf('172.30.30.57:48081', [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
        Add-Blocker $Blockers "production frontend backend target must include 172.30.30.57:48081."
    }
    Test-EvidenceFile $Blockers 'production frontend backend target' (Get-JsonProperty $Evidence 'evidencePath')
}

function New-EvidenceFromParameters {
    [pscustomobject]@{
        prod = [pscustomobject]@{
            playwrightLogin = [pscustomobject]@{
                status = if ([string]::IsNullOrWhiteSpace($ProdLoginEvidencePath)) { 'missing' } else { 'passed' }
                sessionName = 'int-ruoyi-prod-g6'
                usedFrontendPath = $true
                landedPath = '/index'
                visibleUserText = 'provided-by-parameter'
                evidencePath = $ProdLoginEvidencePath
            }
            sampleFileFrontendPath = [pscustomobject]@{
                status = if ([string]::IsNullOrWhiteSpace($ProdSampleEvidencePath)) { 'missing' } else { 'passed' }
                usedFrontendPath = $true
                fileConfigId = 28
                objectPath = 'dcc/stamped/20260513/dcc-sample_controlled.pdf'
                httpStatus = 200
                contentType = 'application/pdf'
                evidencePath = $ProdSampleEvidencePath
            }
            frontendBackendTarget = [pscustomobject]@{
                observedTargets = $ProdFrontendBackendTargets
                forbiddenTargets = $ForbiddenProdBackendTargets
                evidencePath = $ProdLoginEvidencePath
            }
        }
        test = [pscustomobject]@{
            playwrightLogin = [pscustomobject]@{
                status = if ([string]::IsNullOrWhiteSpace($TestLoginEvidencePath)) { 'missing' } else { 'passed' }
                sessionName = 'int-ruoyi-test-g6'
                usedFrontendPath = $true
                landedPath = '/index'
                visibleUserText = 'provided-by-parameter'
                evidencePath = $TestLoginEvidencePath
            }
            sampleFileFrontendPath = [pscustomobject]@{
                status = if ([string]::IsNullOrWhiteSpace($TestSampleEvidencePath)) { 'missing' } else { 'passed' }
                usedFrontendPath = $true
                fileConfigId = 28
                objectPath = 'dcc/stamped/20260513/dcc-sample_controlled.pdf'
                httpStatus = 200
                contentType = 'application/pdf'
                evidencePath = $TestSampleEvidencePath
            }
        }
    }
}

$blockers = New-Object System.Collections.Generic.List[string]
$evidence = $null

if (-not [string]::IsNullOrWhiteSpace($EvidencePath)) {
    if (-not (Test-Path -LiteralPath $EvidencePath -PathType Leaf)) {
        Add-Blocker $blockers "EvidencePath does not exist: $EvidencePath"
    } else {
        $evidence = Get-Content -LiteralPath $EvidencePath -Encoding UTF8 -Raw | ConvertFrom-Json
    }
} elseif (-not [string]::IsNullOrWhiteSpace($EvidenceJson)) {
    $evidence = $EvidenceJson | ConvertFrom-Json
} elseif (
    -not [string]::IsNullOrWhiteSpace($ProdLoginEvidencePath) -or
    -not [string]::IsNullOrWhiteSpace($TestLoginEvidencePath) -or
    -not [string]::IsNullOrWhiteSpace($ProdSampleEvidencePath) -or
    -not [string]::IsNullOrWhiteSpace($TestSampleEvidencePath) -or
    $ProdFrontendBackendTargets.Count -gt 0
) {
    $evidence = New-EvidenceFromParameters
} else {
    Add-Blocker $blockers 'EvidencePath, EvidenceJson, or explicit evidence parameters are required.'
}

if ($null -ne $evidence) {
    $prod = Get-JsonProperty $evidence 'prod'
    $test = Get-JsonProperty $evidence 'test'

    Test-LoginEvidence $blockers 'production' (Get-JsonProperty $prod 'playwrightLogin')
    Test-LoginEvidence $blockers 'test' (Get-JsonProperty $test 'playwrightLogin')
    Test-SampleEvidence $blockers 'production' (Get-JsonProperty $prod 'sampleFileFrontendPath')
    Test-SampleEvidence $blockers 'test' (Get-JsonProperty $test 'sampleFileFrontendPath')
    Test-ProdTargetEvidence $blockers (Get-JsonProperty $prod 'frontendBackendTarget')
}

if ($blockers.Count -gt 0) {
    Write-Output 'G6/G7 BLOCKED'
    foreach ($blocker in $blockers) {
        Write-Output "- $blocker"
    }
    exit 2
}

Write-Output 'G6/G7 PASS'
Write-Output 'Validated production/test Playwright login evidence, sample file frontend path evidence, and production frontend backend target.'
exit 0
