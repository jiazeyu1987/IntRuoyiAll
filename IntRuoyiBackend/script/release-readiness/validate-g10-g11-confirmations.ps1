param(
    [Parameter(Mandatory = $true)]
    [string]$ConfirmationPath
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

function Test-Blank {
    param([object]$Value)
    if ($null -eq $Value) {
        return $true
    }
    return [string]::IsNullOrWhiteSpace([string]$Value)
}

function Test-PlaceholderText {
    param([object]$Value)
    if (Test-Blank $Value) {
        return $false
    }
    $text = ([string]$Value).Trim()
    $unspecifiedToken = [string]::Concat([char]0x672A, [char]0x6307, [char]0x5B9A)
    $tokens = @('example', 'placeholder', 'pending', $unspecifiedToken, 'BLOCKED')
    foreach ($token in $tokens) {
        if ($text.IndexOf($token, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
            return $true
        }
    }
    return $false
}

function Test-True {
    param([object]$Value)
    if ($Value -is [bool]) {
        return $Value
    }
    if ($null -eq $Value) {
        return $false
    }
    return ([string]$Value).ToLowerInvariant() -eq 'true'
}

function Convert-ToStringList {
    param([object]$Value)
    $items = New-Object System.Collections.Generic.List[string]
    if ($null -eq $Value) {
        return ,$items
    }
    if ($Value -is [string]) {
        if (-not [string]::IsNullOrWhiteSpace($Value)) {
            [void]$items.Add($Value)
        }
        return ,$items
    }
    foreach ($item in @($Value)) {
        if ($null -ne $item -and -not [string]::IsNullOrWhiteSpace([string]$item)) {
            [void]$items.Add([string]$item)
        }
    }
    return ,$items
}

function Test-RequiredText {
    param(
        [System.Collections.Generic.List[string]]$Blockers,
        [object]$Object,
        [string]$PropertyPath,
        [string]$PropertyName
    )
    if (Test-Blank (Get-JsonProperty $Object $PropertyName)) {
        Add-Blocker $Blockers "$PropertyPath is required"
    }
}

function Test-RequiredConfirmationText {
    param(
        [System.Collections.Generic.List[string]]$Blockers,
        [object]$Object,
        [string]$PropertyPath,
        [string]$PropertyName
    )
    $value = Get-JsonProperty $Object $PropertyName
    if (Test-Blank $value) {
        Add-Blocker $Blockers "$PropertyPath is required"
        return
    }
    if (Test-PlaceholderText $value) {
        Add-Blocker $Blockers "$PropertyPath must not be a placeholder"
    }
}

function Test-WebhookUrl {
    param(
        [System.Collections.Generic.List[string]]$Blockers,
        [object]$Value
    )
    $text = [string]$Value
    if (Test-Blank $text) {
        Add-Blocker $Blockers 'g10.notify.webhook.url is required'
        return
    }
    if ($text -notmatch '^https?://[^/\s]+(?:/.*)?$' -or (Test-PlaceholderText $text)) {
        Add-Blocker $Blockers 'g10.notify.webhook.url must be a real http/https URL'
    }
}

function Test-G10 {
    param(
        [System.Collections.Generic.List[string]]$Blockers,
        [object]$G10
    )

    $notify = Get-JsonProperty $G10 'notify'
    $webhook = Get-JsonProperty $notify 'webhook'

    if (-not (Test-True (Get-JsonProperty $notify 'enabled'))) {
        Add-Blocker $Blockers 'g10.notify.enabled must be true'
    }
    if ([string](Get-JsonProperty $notify 'channel') -ne 'webhook') {
        Add-Blocker $Blockers 'g10.notify.channel must be webhook'
    }
    Test-WebhookUrl $Blockers (Get-JsonProperty $webhook 'url')
    Test-RequiredConfirmationText $Blockers $G10 'g10.alertTarget' 'alertTarget'
    Test-RequiredConfirmationText $Blockers $G10 'g10.sendEvidencePath' 'sendEvidencePath'

    $alertOwner = Get-JsonProperty $G10 'alertOwner'
    Test-RequiredConfirmationText $Blockers $alertOwner 'g10.alertOwner.ownerName' 'ownerName'
    Test-RequiredConfirmationText $Blockers $alertOwner 'g10.alertOwner.contact' 'contact'

    if ([string](Get-JsonProperty $G10 'notificationStatus') -ne 'sent') {
        Add-Blocker $Blockers 'g10.notificationStatus must be sent'
    }

    $coverage = Convert-ToStringList (Get-JsonProperty $G10 'routeCoverage')
    $requiredCoverage = @(
        'backup-now',
        'backup-scheduled',
        'rollback-app',
        'restore-data started',
        'restore-data finished',
        'rehearsal',
        'cleanup'
    )
    foreach ($required in $requiredCoverage) {
        if (-not $coverage.Contains($required)) {
            Add-Blocker $Blockers "g10.routeCoverage missing $required"
        }
    }
}

function Test-G11 {
    param(
        [System.Collections.Generic.List[string]]$Blockers,
        [object]$G11
    )

    $candidateNames = New-Object System.Collections.Generic.List[string]
    $candidates = Get-JsonProperty $G11 'prodOwnerCandidates'
    foreach ($candidate in @($candidates)) {
        $name = Get-JsonProperty $candidate 'candidateName'
        if (-not (Test-Blank $name)) {
            [void]$candidateNames.Add([string]$name)
        }
    }
    $requiredRoles = @(
        'releaseOwner',
        'backupRecoveryOperator',
        'dataOwner',
        'acceptanceOwner',
        'alertOwner',
        'releaseGateReviewer'
    )
    $requiredFields = @(
        'ownerName',
        'contact',
        'approvalTime',
        'approvalEvidence',
        'currentDecision'
    )

    foreach ($role in $requiredRoles) {
        $roleConfirmation = Get-JsonProperty $G11 $role
        foreach ($field in $requiredFields) {
            if ($field -eq 'currentDecision') {
                Test-RequiredText $Blockers $roleConfirmation "g11.$role.$field" $field
            } else {
                Test-RequiredConfirmationText $Blockers $roleConfirmation "g11.$role.$field" $field
            }
        }
        $decision = [string](Get-JsonProperty $roleConfirmation 'currentDecision')
        if (-not [string]::IsNullOrWhiteSpace($decision) -and $decision -ne 'GO') {
            Add-Blocker $Blockers "g11.$role.currentDecision must be GO"
        }
        $approvalEvidence = [string](Get-JsonProperty $roleConfirmation 'approvalEvidence')
        if (
            $candidateNames.Count -gt 0 -and
            ($approvalEvidence -eq 'jiazeyu' -or $approvalEvidence -eq 'tangbin')
        ) {
            Add-Blocker $Blockers "g11.$role.approvalEvidence must not be only a candidate name"
        }
    }

    $hasRoleBlockers = $false
    foreach ($blocker in @($Blockers)) {
        if ([string]$blocker -like 'g11.*.* is required' -or [string]$blocker -like 'g11.*.currentDecision must be GO') {
            $hasRoleBlockers = $true
            break
        }
    }
    if ($candidateNames.Count -gt 0 -and $hasRoleBlockers) {
        Add-Blocker $Blockers 'g11.prodOwnerCandidates are not approvals'
    }
}

function Write-ResultAndExit {
    param(
        [System.Collections.Generic.List[string]]$Blockers
    )
    $decision = if ($Blockers.Count -eq 0) { 'GO' } else { 'BLOCKED' }
    $payload = [pscustomobject]@{
        gate = 'G10/G11'
        decision = $decision
        blockedReasons = @($Blockers)
        sendsWebhook = $false
    }
    $payload | ConvertTo-Json -Depth 8
    if ($Blockers.Count -eq 0) {
        exit 0
    }
    exit 2
}

$blockers = New-Object System.Collections.Generic.List[string]

if (-not (Test-Path -LiteralPath $ConfirmationPath -PathType Leaf)) {
    Add-Blocker $blockers "ConfirmationPath does not exist: $ConfirmationPath"
    Write-ResultAndExit $blockers
}

$confirmation = Get-Content -LiteralPath $ConfirmationPath -Encoding UTF8 -Raw | ConvertFrom-Json
Test-G10 $blockers (Get-JsonProperty $confirmation 'g10')
Test-G11 $blockers (Get-JsonProperty $confirmation 'g11')
Write-ResultAndExit $blockers
