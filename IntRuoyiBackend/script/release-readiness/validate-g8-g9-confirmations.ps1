param(
    [Parameter(Mandatory = $true)]
    [string]$ConfirmationPath
)

[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$blockedReasons = New-Object System.Collections.Generic.List[string]

function Add-BlockedReason {
    param([string]$Reason)
    $blockedReasons.Add($Reason) | Out-Null
}

function Write-DecisionAndExit {
    param(
        [string]$Decision,
        [int]$ExitCode
    )

    $payload = [ordered]@{
        decision = $Decision
        blockedReasons = @($blockedReasons)
    }
    $payload | ConvertTo-Json -Depth 8
    exit $ExitCode
}

function Get-JsonValue {
    param(
        [object]$Root,
        [string]$Path
    )

    $current = $Root
    foreach ($part in $Path.Split(".")) {
        if ($null -eq $current) {
            return $null
        }

        $property = $current.PSObject.Properties[$part]
        if ($null -eq $property) {
            return $null
        }

        $current = $property.Value
    }

    return $current
}

function Test-BlockedValue {
    param([object]$Value)

    if ($null -eq $Value) {
        return $true
    }

    if ($Value -is [string]) {
        $trimmed = $Value.Trim()
        if ($trimmed.Length -eq 0) {
            return $true
        }

        $lower = $trimmed.ToLowerInvariant()
        $blockedTokens = @(
            "(blocked)",
            "blocked",
            "pending",
            "placeholder",
            "todo",
            "tbd",
            "n/a"
        )
        foreach ($token in $blockedTokens) {
            if ($lower.Contains($token)) {
                return $true
            }
        }
    }

    return $false
}

function Require-Value {
    param(
        [object]$Root,
        [string]$Path
    )

    $value = Get-JsonValue -Root $Root -Path $Path
    if (Test-BlockedValue -Value $value) {
        Add-BlockedReason "$Path is required"
    }
}

function Require-Equals {
    param(
        [object]$Actual,
        [string]$Expected,
        [string]$Reason
    )

    if ((Test-BlockedValue -Value $Actual) -or ([string]$Actual -ne $Expected)) {
        Add-BlockedReason $Reason
    }
}

if (-not (Test-Path -LiteralPath $ConfirmationPath -PathType Leaf)) {
    Add-BlockedReason "ConfirmationPath does not exist: $ConfirmationPath"
    Write-DecisionAndExit -Decision "BLOCKED" -ExitCode 2
}

try {
    $rawJson = [System.IO.File]::ReadAllText(
        (Resolve-Path -LiteralPath $ConfirmationPath).Path,
        [System.Text.Encoding]::UTF8
    )
    $confirmation = $rawJson | ConvertFrom-Json
} catch {
    Add-BlockedReason "ConfirmationPath must contain valid UTF-8 JSON: $($_.Exception.Message)"
    Write-DecisionAndExit -Decision "BLOCKED" -ExitCode 2
}

$requiredPaths = @(
    "releaseId",
    "currentFaultImageTag",
    "g8.rollbackTriggerId",
    "g8.rollbackTriggerCondition",
    "g8.SelectedImageTag",
    "g8.imageTagSelectionRule",
    "g8.releaseOwnerApproval.ownerName",
    "g8.releaseOwnerApproval.approvalTime",
    "g8.releaseOwnerApproval.approvalEvidence",
    "g8.backupRecoveryOperatorApproval.ownerName",
    "g8.backupRecoveryOperatorApproval.operatorName",
    "g8.backupRecoveryOperatorApproval.approvalTime",
    "g8.backupRecoveryOperatorApproval.approvalEvidence",
    "g8.rollbackValidationEvidence.action",
    "g8.rollbackValidationEvidence.status",
    "g8.rollbackValidationEvidence.code",
    "g8.rollbackValidationEvidence.context.imageTag",
    "g8.rollbackValidationEvidence.logPath",
    "g8.rollbackValidationEvidence.reportPath",
    "g8.rollbackValidationEvidence.backendHealthEvidence",
    "g8.rollbackValidationEvidence.frontendAccessEvidence",
    "g9.restoreTriggerId",
    "g9.restoreTriggerCondition",
    "g9.SelectedBackupId",
    "g9.backupIdSelectionRule",
    "g9.preRestoreSnapshotId",
    "g9.dataOwnerApproval.ownerName",
    "g9.dataOwnerApproval.approvalTime",
    "g9.dataOwnerApproval.approvalEvidence",
    "g9.releaseOwnerApproval.ownerName",
    "g9.releaseOwnerApproval.approvalTime",
    "g9.releaseOwnerApproval.approvalEvidence",
    "g9.backupRecoveryOperatorApproval.ownerName",
    "g9.backupRecoveryOperatorApproval.operatorName",
    "g9.backupRecoveryOperatorApproval.approvalTime",
    "g9.backupRecoveryOperatorApproval.approvalEvidence",
    "g9.businessImpactScope",
    "g9.restoreValidationEvidence.action",
    "g9.restoreValidationEvidence.status",
    "g9.restoreValidationEvidence.code",
    "g9.restoreValidationEvidence.context.backupId",
    "g9.restoreValidationEvidence.context.restorePoint",
    "g9.restoreValidationEvidence.context.preRestoreSnapshotId",
    "g9.restoreValidationEvidence.context.imageTag",
    "g9.restoreValidationEvidence.logPath",
    "g9.restoreValidationEvidence.reportPath",
    "g9.restoreValidationEvidence.mysqlRestoreEvidence",
    "g9.restoreValidationEvidence.objectRestoreEvidence",
    "g9.restoreValidationEvidence.backendHealthEvidence",
    "g9.restoreValidationEvidence.frontendAccessEvidence",
    "g9.restoreValidationEvidence.loginValidationEvidence",
    "g9.restoreValidationEvidence.sampleFileEvidence"
)

foreach ($path in $requiredPaths) {
    Require-Value -Root $confirmation -Path $path
}

$currentFaultImageTag = Get-JsonValue -Root $confirmation -Path "currentFaultImageTag"
$selectedImageTag = Get-JsonValue -Root $confirmation -Path "g8.SelectedImageTag"
$rollbackEvidenceImageTag = Get-JsonValue -Root $confirmation -Path "g8.rollbackValidationEvidence.context.imageTag"

if (
    -not (Test-BlockedValue -Value $currentFaultImageTag) -and
    -not (Test-BlockedValue -Value $selectedImageTag) -and
    [string]$selectedImageTag -eq [string]$currentFaultImageTag
) {
    Add-BlockedReason "g8.SelectedImageTag must not equal currentFaultImageTag"
}

Require-Equals `
    -Actual (Get-JsonValue -Root $confirmation -Path "g8.rollbackValidationEvidence.action") `
    -Expected "rollback-app" `
    -Reason "g8.rollbackValidationEvidence.action must equal rollback-app"
Require-Equals `
    -Actual (Get-JsonValue -Root $confirmation -Path "g8.rollbackValidationEvidence.status") `
    -Expected "success" `
    -Reason "g8.rollbackValidationEvidence.status must equal success"
Require-Equals `
    -Actual (Get-JsonValue -Root $confirmation -Path "g8.rollbackValidationEvidence.code") `
    -Expected "INTBK-0000" `
    -Reason "g8.rollbackValidationEvidence.code must equal INTBK-0000"
Require-Equals `
    -Actual $rollbackEvidenceImageTag `
    -Expected ([string]$selectedImageTag) `
    -Reason "g8.rollbackValidationEvidence.context.imageTag must equal g8.SelectedImageTag"

$selectedBackupId = Get-JsonValue -Root $confirmation -Path "g9.SelectedBackupId"
$preRestoreSnapshotId = Get-JsonValue -Root $confirmation -Path "g9.preRestoreSnapshotId"

Require-Equals `
    -Actual (Get-JsonValue -Root $confirmation -Path "g9.restoreValidationEvidence.action") `
    -Expected "restore-data" `
    -Reason "g9.restoreValidationEvidence.action must equal restore-data"
Require-Equals `
    -Actual (Get-JsonValue -Root $confirmation -Path "g9.restoreValidationEvidence.status") `
    -Expected "success" `
    -Reason "g9.restoreValidationEvidence.status must equal success"
Require-Equals `
    -Actual (Get-JsonValue -Root $confirmation -Path "g9.restoreValidationEvidence.code") `
    -Expected "INTBK-0000" `
    -Reason "g9.restoreValidationEvidence.code must equal INTBK-0000"
Require-Equals `
    -Actual (Get-JsonValue -Root $confirmation -Path "g9.restoreValidationEvidence.context.backupId") `
    -Expected ([string]$selectedBackupId) `
    -Reason "g9.restoreValidationEvidence.context.backupId must equal g9.SelectedBackupId"
Require-Equals `
    -Actual (Get-JsonValue -Root $confirmation -Path "g9.restoreValidationEvidence.context.restorePoint") `
    -Expected ([string]$selectedBackupId) `
    -Reason "g9.restoreValidationEvidence.context.restorePoint must equal g9.SelectedBackupId"
Require-Equals `
    -Actual (Get-JsonValue -Root $confirmation -Path "g9.restoreValidationEvidence.context.preRestoreSnapshotId") `
    -Expected ([string]$preRestoreSnapshotId) `
    -Reason "g9.restoreValidationEvidence.context.preRestoreSnapshotId must equal g9.preRestoreSnapshotId"

if ($blockedReasons.Count -gt 0) {
    Write-DecisionAndExit -Decision "BLOCKED" -ExitCode 2
}

Write-DecisionAndExit -Decision "GO" -ExitCode 0
