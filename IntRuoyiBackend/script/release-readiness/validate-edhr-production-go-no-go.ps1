param(
    [Parameter(Mandatory = $true)]
    [string]$EvidencePath
)

$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$script:Blockers = New-Object System.Collections.Generic.List[string]
$script:Gates = [ordered]@{}
$script:ReleaseId = $null
$script:CurrentImageTag = $null
$script:BackupId = $null
$script:Environment = $null
$script:RequiredEdhrReleaseFeatureIds = @(
    'feedback-entry/open-or-create',
    'execution-detail/save-submit',
    'approval-workbench/detail-approve-reject',
    'archive-generate-download',
    'execution-list',
    'tracking-signature',
    'field-audit',
    'domain-trace',
    'permission-matrix',
    'archive-health/runtime-control'
)
$script:RequiredEdhrReleaseCheckedScripts = @(
    'e2e:edhr:approval-tracking:check',
    'e2e:edhr:execution-list:check',
    'e2e:edhr:tracking-signature:check',
    'e2e:edhr:field-audit:check',
    'e2e:edhr:domain-trace:check',
    'e2e:edhr:permission-matrix:check',
    'e2e:edhr:archive-health:check'
)
$script:RequiredEdhrReleaseCheckedE2eFiles = @(
    'tests/e2e/edhr-approval-tracking-real-flow.e2e.js',
    'tests/e2e/edhr-execution-list-real-flow.e2e.js',
    'tests/e2e/edhr-tracking-signature-real-flow.e2e.js',
    'tests/e2e/edhr-field-audit-real-flow.e2e.js',
    'tests/e2e/edhr-domain-trace-real-flow.e2e.js',
    'tests/e2e/edhr-permission-tenant-matrix.e2e.js',
    'tests/e2e/runtime-control-edhr-archive-health.e2e.js'
)

function Add-Blocker {
    param([string]$Message)
    [void]$script:Blockers.Add($Message)
}

function Set-Gate {
    param(
        [string]$Name,
        [string]$Decision,
        [object]$Details
    )
    $script:Gates[$Name] = [ordered]@{
        decision = $Decision
        details = $Details
    }
}

function Write-ResultAndExit {
    param(
        [string]$Decision,
        [int]$ExitCode
    )

    $payload = [ordered]@{
        gate = 'eDHR production Go/No-Go'
        decision = $Decision
        releaseId = $script:ReleaseId
        currentImageTag = $script:CurrentImageTag
        environment = $script:Environment
        blockedReasons = @($script:Blockers)
        gates = $script:Gates
        readOnly = $true
        sendsWebhook = $false
    }
    $payload | ConvertTo-Json -Depth 16
    exit $ExitCode
}

function Get-JsonValue {
    param(
        [object]$Root,
        [string]$Path
    )

    $current = $Root
    foreach ($part in $Path.Split('.')) {
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
        $text = $Value.Trim()
        if ($text.Length -eq 0) {
            return $true
        }
        $lower = $text.ToLowerInvariant()
        $unspecifiedToken = [string]::Concat([char]0x672A, [char]0x6307, [char]0x5B9A)
        $tokens = @(
            'pending',
            'placeholder',
            'blocked',
            'todo',
            'tbd',
            'n/a',
            'example',
            $unspecifiedToken
        )
        foreach ($token in $tokens) {
            if ($lower.Contains($token.ToLowerInvariant())) {
                return $true
            }
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
    return ([string]$Value).Trim().ToLowerInvariant() -eq 'true'
}

function Require-UsableValue {
    param(
        [object]$Root,
        [string]$Path
    )

    $value = Get-JsonValue -Root $Root -Path $Path
    if (Test-BlockedValue -Value $value) {
        Add-Blocker "$Path is required and must not be a placeholder"
    }
    return $value
}

function Require-ExactValue {
    param(
        [object]$Root,
        [string]$Path,
        [string]$Expected,
        [string]$Message
    )

    $value = Get-JsonValue -Root $Root -Path $Path
    if ((Test-BlockedValue -Value $value) -or ([string]$value -ne $Expected)) {
        Add-Blocker $Message
    }
    return $value
}

function Require-SuccessStatus {
    param(
        [object]$Root,
        [string]$Path
    )

    return Require-ExactValue `
        -Root $Root `
        -Path $Path `
        -Expected 'success' `
        -Message "$Path must equal success"
}

function Resolve-EvidencePath {
    param(
        [string]$BaseDirectory,
        [object]$PathValue,
        [string]$FieldName
    )

    if (Test-BlockedValue -Value $PathValue) {
        Add-Blocker "$FieldName is required and must not be a placeholder"
        return $null
    }

    $pathText = [string]$PathValue
    if ([System.IO.Path]::IsPathRooted($pathText)) {
        return $pathText
    }
    return [System.IO.Path]::GetFullPath([System.IO.Path]::Combine($BaseDirectory, $pathText))
}

function Require-ReadableEvidenceFile {
    param(
        [string]$BaseDirectory,
        [object]$PathValue,
        [string]$FieldName
    )

    $resolvedPath = Resolve-EvidencePath `
        -BaseDirectory $BaseDirectory `
        -PathValue $PathValue `
        -FieldName $FieldName
    if ($null -eq $resolvedPath) {
        return $null
    }

    if (-not (Test-Path -LiteralPath $resolvedPath -PathType Leaf)) {
        Add-Blocker "$FieldName file does not exist: $resolvedPath"
        return $null
    }

    $stream = $null
    try {
        $stream = [System.IO.File]::Open(
            $resolvedPath,
            [System.IO.FileMode]::Open,
            [System.IO.FileAccess]::Read,
            [System.IO.FileShare]::ReadWrite
        )
    } catch {
        Add-Blocker "$FieldName file is not readable: $resolvedPath; $($_.Exception.Message)"
        return $null
    } finally {
        if ($null -ne $stream) {
            $stream.Dispose()
        }
    }

    return $resolvedPath
}

function Read-RequiredEvidenceText {
    param(
        [string]$BaseDirectory,
        [object]$PathValue,
        [string]$FieldName
    )

    $resolvedPath = Require-ReadableEvidenceFile `
        -BaseDirectory $BaseDirectory `
        -PathValue $PathValue `
        -FieldName $FieldName
    if ($null -eq $resolvedPath) {
        return $null
    }

    try {
        $text = [System.IO.File]::ReadAllText($resolvedPath, [System.Text.Encoding]::UTF8)
        return [pscustomobject]@{
            Path = $resolvedPath
            Text = $text
        }
    } catch {
        Add-Blocker "$FieldName file is not readable: $resolvedPath; $($_.Exception.Message)"
        return $null
    }
}

function Read-RequiredEvidenceJson {
    param(
        [string]$BaseDirectory,
        [object]$PathValue,
        [string]$FieldName
    )

    $file = Read-RequiredEvidenceText `
        -BaseDirectory $BaseDirectory `
        -PathValue $PathValue `
        -FieldName $FieldName
    if ($null -eq $file) {
        return $null
    }

    try {
        return $file.Text | ConvertFrom-Json
    } catch {
        Add-Blocker "$FieldName must contain valid UTF-8 JSON: $($_.Exception.Message)"
        return $null
    }
}

function Read-ResolvedEvidenceJson {
    param(
        [string]$ResolvedPath,
        [string]$FieldName
    )

    if ($null -eq $ResolvedPath) {
        return $null
    }

    try {
        $rawJson = [System.IO.File]::ReadAllText($ResolvedPath, [System.Text.Encoding]::UTF8)
        return $rawJson | ConvertFrom-Json
    } catch {
        Add-Blocker "$FieldName must contain valid UTF-8 JSON: $($_.Exception.Message)"
        return $null
    }
}

function Test-ContainsCiSkipFlag {
    param([object]$Text)

    if ($null -eq $Text) {
        return $false
    }

    $textValue = [string]$Text
    return (
        $textValue.IndexOf('maven.test.skip=true', [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
        $textValue.IndexOf('skipTests', [System.StringComparison]::OrdinalIgnoreCase) -ge 0
    )
}

function Require-NoCiSkipFlag {
    param(
        [object]$Text,
        [string]$FieldName
    )

    if (Test-ContainsCiSkipFlag -Text $Text) {
        Add-Blocker "$FieldName must not contain maven.test.skip=true or skipTests"
    }
}

function Require-EdhrReleaseCoverageCommand {
    param(
        [object]$Command,
        [string]$FieldName
    )

    if (Test-BlockedValue -Value $Command) {
        return
    }

    $commandText = [string]$Command
    $normalized = [regex]::Replace($commandText.Replace('\', '/'), '\s+', ' ').Trim()
    $reportValuePattern = '(?:"(?!-)[A-Za-z0-9_./:=\-]+"|''(?!-)[A-Za-z0-9_./:=\-]+''|(?!-)[A-Za-z0-9_./:=\-]+)'
    $reportArgPattern = "(?:\s+--report(?:=$reportValuePattern|\s+$reportValuePattern))?"
    $hasPackageGate = [regex]::IsMatch(
        $normalized,
        "^(?:pnpm|npm|yarn)(?:\.cmd)?\s+(?:run\s+)?e2e:edhr:release:check$reportArgPattern$",
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    $hasNodeGate = [regex]::IsMatch(
        $normalized,
        "^node(?:\.exe)?\s+(?:\.?/)?scripts/edhr-release-e2e-coverage-gate\.mjs\s+--check$reportArgPattern$",
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
    )

    if (-not ($hasPackageGate -or $hasNodeGate)) {
        Add-Blocker "$FieldName must run a canonical eDHR release coverage check command"
    }
}

function Require-ExactRequiredStringArray {
    param(
        [object]$Root,
        [string]$PropertyName,
        [string]$FieldName,
        [string[]]$RequiredItems
    )

    $property = $Root.PSObject.Properties[$PropertyName]
    if ($null -eq $property -or $null -eq $property.Value) {
        Add-Blocker "$FieldName.$PropertyName must be a non-empty array"
        return
    }

    $items = @($property.Value)
    if ($items.Count -eq 0) {
        Add-Blocker "$FieldName.$PropertyName must be a non-empty array"
        return
    }

    if ($items.Count -ne $RequiredItems.Count) {
        Add-Blocker "$FieldName.$PropertyName must contain exactly $($RequiredItems.Count) required items"
    }

    $requiredSet = New-Object System.Collections.Generic.HashSet[string]
    foreach ($requiredItem in $RequiredItems) {
        [void]$requiredSet.Add($requiredItem)
    }

    $actualSet = New-Object System.Collections.Generic.HashSet[string]
    $itemIndex = 0
    foreach ($item in $items) {
        if (Test-BlockedValue -Value $item) {
            Add-Blocker "$FieldName.$PropertyName[$itemIndex] is required and must not be a placeholder"
        } else {
            $itemText = [string]$item
            if (-not $actualSet.Add($itemText)) {
                Add-Blocker "$FieldName.$PropertyName duplicate item: $itemText"
            }
            if (-not $requiredSet.Contains($itemText)) {
                Add-Blocker "$FieldName.$PropertyName unexpected item: $itemText"
            }
        }
        $itemIndex += 1
    }

    foreach ($requiredItem in $RequiredItems) {
        if (-not $actualSet.Contains($requiredItem)) {
            Add-Blocker "$FieldName.$PropertyName missing required item: $requiredItem"
        }
    }
}

function Test-EdhrReleaseCoverageReport {
    param(
        [object]$ReportFile,
        [string]$FieldName
    )

    if ($null -eq $ReportFile) {
        return
    }

    $reportText = [string]$ReportFile.Text
    if ([string]::IsNullOrWhiteSpace($reportText)) {
        return
    }

    try {
        $report = $reportText | ConvertFrom-Json
    } catch {
        Add-Blocker "$FieldName must contain valid UTF-8 JSON: $($_.Exception.Message)"
        return
    }

    $schemaVersion = Get-JsonValue -Root $report -Path 'schemaVersion'
    if (Test-BlockedValue -Value $schemaVersion) {
        Add-Blocker "$FieldName.schemaVersion is required and must not be a placeholder"
    } elseif ([string]$schemaVersion -ne '1') {
        Add-Blocker "$FieldName.schemaVersion must equal 1"
    }

    $status = Get-JsonValue -Root $report -Path 'status'
    if ((Test-BlockedValue -Value $status) -or [string]$status -ne 'passed') {
        Add-Blocker "$FieldName.status must equal passed"
    }

    $mode = Get-JsonValue -Root $report -Path 'mode'
    if ((Test-BlockedValue -Value $mode) -or [string]$mode -ne 'check') {
        Add-Blocker "$FieldName.mode must equal check"
    }

    $realGateClaimed = Get-JsonValue -Root $report -Path 'realGateClaimed'
    if (-not ($realGateClaimed -is [bool]) -or $realGateClaimed -ne $false) {
        Add-Blocker "$FieldName.realGateClaimed must equal false"
    }

    $reportCommand = Get-JsonValue -Root $report -Path 'command'
    if (Test-BlockedValue -Value $reportCommand) {
        Add-Blocker "$FieldName.command is required and must not be a placeholder"
    } else {
        Require-EdhrReleaseCoverageCommand `
            -Command $reportCommand `
            -FieldName "$FieldName.command"
    }

    Require-ExactRequiredStringArray `
        -Root $report `
        -PropertyName 'checkedScripts' `
        -FieldName $FieldName `
        -RequiredItems $script:RequiredEdhrReleaseCheckedScripts
    Require-ExactRequiredStringArray `
        -Root $report `
        -PropertyName 'checkedE2eFiles' `
        -FieldName $FieldName `
        -RequiredItems $script:RequiredEdhrReleaseCheckedE2eFiles

    $featuresValue = Get-JsonValue -Root $report -Path 'features'
    $features = @()
    if ($null -eq $featuresValue -or @($featuresValue).Count -eq 0) {
        Add-Blocker "$FieldName.features must be a non-empty array"
    } else {
        $features = @($featuresValue)
    }

    $featureCountValue = Get-JsonValue -Root $report -Path 'featureCount'
    $featureCount = 0
    if (
        (Test-BlockedValue -Value $featureCountValue) -or
        -not [int]::TryParse([string]$featureCountValue, [ref]$featureCount) -or
        $featureCount -ne $features.Count
    ) {
        Add-Blocker "$FieldName.featureCount must equal features length"
    }
    if ($features.Count -ne $script:RequiredEdhrReleaseFeatureIds.Count) {
        Add-Blocker "$FieldName.featureCount must equal required eDHR release feature count"
    }

    $actualFeatureIds = New-Object System.Collections.Generic.List[string]
    $seenFeatureIds = New-Object System.Collections.Generic.HashSet[string]
    $featureIndex = 0
    foreach ($feature in $features) {
        $featureId = Get-JsonValue -Root $feature -Path 'featureId'
        if (Test-BlockedValue -Value $featureId) {
            Add-Blocker "$FieldName.features[$featureIndex].featureId is required and must not be a placeholder"
        } else {
            $featureIdText = [string]$featureId
            [void]$actualFeatureIds.Add($featureIdText)
            if (-not $seenFeatureIds.Add($featureIdText)) {
                Add-Blocker "$FieldName duplicate featureId: $featureIdText"
            }
            if ($script:RequiredEdhrReleaseFeatureIds -notcontains $featureIdText) {
                Add-Blocker "$FieldName unexpected featureId: $featureIdText"
            }
        }

        $featureStatus = Get-JsonValue -Root $feature -Path 'status'
        if ((Test-BlockedValue -Value $featureStatus) -or [string]$featureStatus -ne 'passed') {
            $featureLabel = if (Test-BlockedValue -Value $featureId) { "#$featureIndex" } else { [string]$featureId }
            Add-Blocker "$FieldName feature $featureLabel status must equal passed"
        }

        $featureIndex += 1
    }

    foreach ($requiredFeatureId in $script:RequiredEdhrReleaseFeatureIds) {
        if ($actualFeatureIds -notcontains $requiredFeatureId) {
            Add-Blocker "$FieldName missing required featureId: $requiredFeatureId"
        }
    }

    $failuresProperty = $report.PSObject.Properties['failures']
    if ($null -eq $failuresProperty) {
        Add-Blocker "$FieldName.failures must be an empty array"
    } elseif (@($failuresProperty.Value).Count -ne 0) {
        Add-Blocker "$FieldName.failures must be empty"
    }
}

function Convert-OutputToJsonObject {
    param(
        [string]$OutputText,
        [string]$GateName
    )

    if ([string]::IsNullOrWhiteSpace($OutputText)) {
        Add-Blocker "$GateName confirmation validator produced no JSON output"
        return $null
    }

    try {
        return $OutputText | ConvertFrom-Json
    } catch {
        Add-Blocker "$GateName confirmation validator output must be JSON: $($_.Exception.Message)"
        return $null
    }
}

function Invoke-ConfirmationValidator {
    param(
        [string]$GateName,
        [string]$ValidatorPath,
        [string]$ConfirmationPath
    )

    if (-not (Test-Path -LiteralPath $ValidatorPath -PathType Leaf)) {
        Add-Blocker "$GateName validator does not exist: $ValidatorPath"
        Set-Gate -Name $GateName -Decision 'NO-GO' -Details 'validator missing'
        return
    }
    if (-not (Test-Path -LiteralPath $ConfirmationPath -PathType Leaf)) {
        Add-Blocker "$GateName confirmation file does not exist: $ConfirmationPath"
        Set-Gate -Name $GateName -Decision 'NO-GO' -Details 'confirmation missing'
        return
    }

    $powerShellExe = Join-Path -Path $PSHOME -ChildPath 'powershell.exe'
    $output = & $powerShellExe `
        -NoLogo `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File $ValidatorPath `
        -ConfirmationPath $ConfirmationPath 2>&1
    $exitCode = $LASTEXITCODE
    $outputText = ($output | Out-String).Trim()
    $nestedResult = Convert-OutputToJsonObject -OutputText $outputText -GateName $GateName

    if ($exitCode -ne 0) {
        $nestedReasons = @()
        if ($null -ne $nestedResult) {
            $nestedReasons = @($nestedResult.blockedReasons)
        }
        if ($nestedReasons.Count -eq 0) {
            Add-Blocker "$GateName confirmation failed with exit code $exitCode"
        } else {
            foreach ($reason in $nestedReasons) {
                Add-Blocker "$GateName confirmation failed: $reason"
            }
        }
        Set-Gate -Name $GateName -Decision 'NO-GO' -Details $nestedReasons
        return
    }

    if ($null -eq $nestedResult -or [string]$nestedResult.decision -ne 'GO') {
        Add-Blocker "$GateName confirmation did not return GO"
        Set-Gate -Name $GateName -Decision 'NO-GO' -Details $outputText
        return
    }

    Set-Gate -Name $GateName -Decision 'GO' -Details 'nested validator returned GO'
}

function Test-ProtectedStorage {
    param(
        [object]$Evidence,
        [string]$BaseDirectory
    )

    $before = $script:Blockers.Count
    Require-ExactValue `
        -Root $Evidence `
        -Path 'protectedStorageVerifierEvidence.status' `
        -Expected 'PASS' `
        -Message 'protectedStorageVerifierEvidence.status must equal PASS' | Out-Null

    if (-not (Test-True (Get-JsonValue -Root $Evidence -Path 'protectedStorageVerifierEvidence.objectLock.enabled'))) {
        Add-Blocker 'protectedStorageVerifierEvidence.objectLock.enabled must be true'
    }
    Require-UsableValue -Root $Evidence -Path 'protectedStorageVerifierEvidence.objectLock.mode' | Out-Null
    Require-ReadableEvidenceFile `
        -BaseDirectory $BaseDirectory `
        -PathValue (Get-JsonValue -Root $Evidence -Path 'protectedStorageVerifierEvidence.objectLock.evidencePath') `
        -FieldName 'protectedStorageVerifierEvidence.objectLock.evidencePath' | Out-Null
    Require-UsableValue -Root $Evidence -Path 'protectedStorageVerifierEvidence.retention.mode' | Out-Null
    $retentionDays = Require-UsableValue -Root $Evidence -Path 'protectedStorageVerifierEvidence.retention.days'
    if ($null -ne $retentionDays) {
        $days = 0
        if (-not [int]::TryParse([string]$retentionDays, [ref]$days) -or $days -le 0) {
            Add-Blocker 'protectedStorageVerifierEvidence.retention.days must be a positive integer'
        }
    }
    Require-ReadableEvidenceFile `
        -BaseDirectory $BaseDirectory `
        -PathValue (Get-JsonValue -Root $Evidence -Path 'protectedStorageVerifierEvidence.retention.evidencePath') `
        -FieldName 'protectedStorageVerifierEvidence.retention.evidencePath' | Out-Null
    if (-not (Test-True (Get-JsonValue -Root $Evidence -Path 'protectedStorageVerifierEvidence.legalHold.required'))) {
        Add-Blocker 'protectedStorageVerifierEvidence.legalHold.required must be true'
    }
    Require-ReadableEvidenceFile `
        -BaseDirectory $BaseDirectory `
        -PathValue (Get-JsonValue -Root $Evidence -Path 'protectedStorageVerifierEvidence.legalHold.evidencePath') `
        -FieldName 'protectedStorageVerifierEvidence.legalHold.evidencePath' | Out-Null

    $decision = if ($script:Blockers.Count -eq $before) { 'GO' } else { 'NO-GO' }
    Set-Gate -Name 'protectedStorage' -Decision $decision -Details 'PASS evidence with objectLock, retention and legalHold proofs required'
}

function Test-BackupNow {
    param(
        [object]$Evidence,
        [string]$ExpectedImageTag,
        [string]$ExpectedBackupId,
        [string]$BaseDirectory
    )

    $before = $script:Blockers.Count
    Require-SuccessStatus -Root $Evidence -Path 'backupNowReport.status' | Out-Null
    Require-ExactValue `
        -Root $Evidence `
        -Path 'backupNowReport.code' `
        -Expected 'INTBK-0000' `
        -Message 'backupNowReport.code must equal INTBK-0000' | Out-Null
    $backupImageTag = Require-UsableValue -Root $Evidence -Path 'backupNowReport.currentImageTag'
    if (-not (Test-BlockedValue -Value $backupImageTag) -and [string]$backupImageTag -ne $ExpectedImageTag) {
        Add-Blocker 'backupNowReport.currentImageTag must equal currentImageTag'
    }
    $backupId = Require-UsableValue -Root $Evidence -Path 'backupNowReport.backupId'
    if (
        -not (Test-BlockedValue -Value $backupId) -and
        -not (Test-BlockedValue -Value $ExpectedBackupId) -and
        [string]$backupId -ne [string]$ExpectedBackupId
    ) {
        Add-Blocker 'backupNowReport.backupId must equal backupId'
    }

    $manifestFile = Read-RequiredEvidenceText `
        -BaseDirectory $BaseDirectory `
        -PathValue (Get-JsonValue -Root $Evidence -Path 'backupNowReport.manifestPath') `
        -FieldName 'backupNowReport.manifestPath'
    $manifestName = $null
    if ($null -ne $manifestFile) {
        $manifestName = [System.IO.Path]::GetFileName([string]$manifestFile.Path)
        try {
            $manifest = $manifestFile.Text | ConvertFrom-Json
            $manifestBackupId = Get-JsonValue -Root $manifest -Path 'backupId'
            if (
                (Test-BlockedValue -Value $manifestBackupId) -or
                -not (Test-BlockedValue -Value $ExpectedBackupId) -and
                [string]$manifestBackupId -ne [string]$ExpectedBackupId
            ) {
                Add-Blocker 'backupNowReport.manifestPath backupId must equal backupId'
            }
            $manifestImageTag = Get-JsonValue -Root $manifest -Path 'currentImageTag'
            if (
                (Test-BlockedValue -Value $manifestImageTag) -or
                -not (Test-BlockedValue -Value $ExpectedImageTag) -and
                [string]$manifestImageTag -ne [string]$ExpectedImageTag
            ) {
                Add-Blocker 'backupNowReport.manifestPath currentImageTag must equal currentImageTag'
            }
        } catch {
            Add-Blocker "backupNowReport.manifestPath must contain valid UTF-8 JSON: $($_.Exception.Message)"
        }
    }

    $checksumsFile = Read-RequiredEvidenceText `
        -BaseDirectory $BaseDirectory `
        -PathValue (Get-JsonValue -Root $Evidence -Path 'backupNowReport.checksumsPath') `
        -FieldName 'backupNowReport.checksumsPath'
    if ($null -ne $checksumsFile) {
        $checksumText = [string]$checksumsFile.Text
        if ([string]::IsNullOrWhiteSpace($checksumText)) {
            Add-Blocker 'backupNowReport.checksumsPath must not be empty'
        } else {
            $containsBackupId = (
                -not (Test-BlockedValue -Value $ExpectedBackupId) -and
                $checksumText.IndexOf([string]$ExpectedBackupId, [System.StringComparison]::OrdinalIgnoreCase) -ge 0
            )
            $containsManifestName = (
                -not [string]::IsNullOrWhiteSpace($manifestName) -and
                $checksumText.IndexOf($manifestName, [System.StringComparison]::OrdinalIgnoreCase) -ge 0
            )
            $containsChecksumItem = [regex]::IsMatch($checksumText, '(?im)\b[0-9a-f]{64}\b')
            if (-not $containsChecksumItem) {
                Add-Blocker 'backupNowReport.checksumsPath must contain at least one SHA-256 checksum item'
            }
            if (-not ($containsBackupId -or $containsManifestName)) {
                Add-Blocker 'backupNowReport.checksumsPath must reference backupId or manifest file name'
            }
        }
    }

    $decision = if ($script:Blockers.Count -eq $before) { 'GO' } else { 'NO-GO' }
    Set-Gate -Name 'backupNow' -Decision $decision -Details 'success INTBK-0000 report with image tag, backupId, manifest and checksums required'
    return $backupId
}

function Test-Rehearsal {
    param(
        [object]$Evidence,
        [object]$ExpectedBackupId,
        [string]$BaseDirectory
    )

    $before = $script:Blockers.Count
    $status = Get-JsonValue -Root $Evidence -Path 'rehearsalReport.status'
    if (
        (Test-BlockedValue -Value $status) -or
        (@('PASSED', 'success') -notcontains [string]$status)
    ) {
        Add-Blocker 'rehearsalReport.status must equal PASSED or success'
    }
    Require-SuccessStatus -Root $Evidence -Path 'rehearsalReport.result' | Out-Null
    $rehearsalBackupId = Require-UsableValue -Root $Evidence -Path 'rehearsalReport.backupId'
    if (
        -not (Test-BlockedValue -Value $rehearsalBackupId) -and
        -not (Test-BlockedValue -Value $ExpectedBackupId) -and
        [string]$rehearsalBackupId -ne [string]$ExpectedBackupId
    ) {
        Add-Blocker 'rehearsalReport.backupId must equal backupNowReport.backupId'
    }
    Require-ReadableEvidenceFile `
        -BaseDirectory $BaseDirectory `
        -PathValue (Get-JsonValue -Root $Evidence -Path 'rehearsalReport.reportPath') `
        -FieldName 'rehearsalReport.reportPath' | Out-Null
    $archiveId = Require-UsableValue -Root $Evidence -Path 'rehearsalReport.edhrArchiveEvidence.archiveId'
    Require-UsableValue -Root $Evidence -Path 'rehearsalReport.edhrArchiveEvidence.archivePath' | Out-Null
    Require-ReadableEvidenceFile `
        -BaseDirectory $BaseDirectory `
        -PathValue (Get-JsonValue -Root $Evidence -Path 'rehearsalReport.edhrArchiveEvidence.evidencePath') `
        -FieldName 'rehearsalReport.edhrArchiveEvidence.evidencePath' | Out-Null
    $hashAlgorithm = Require-UsableValue -Root $Evidence -Path 'rehearsalReport.edhrHashEvidence.algorithm'
    if (
        -not (Test-BlockedValue -Value $hashAlgorithm) -and
        [string]$hashAlgorithm -ne 'SHA-256'
    ) {
        Add-Blocker 'rehearsalReport.edhrHashEvidence.algorithm must equal SHA-256'
    }
    $sha256 = Require-UsableValue -Root $Evidence -Path 'rehearsalReport.edhrHashEvidence.sha256'
    if (
        -not (Test-BlockedValue -Value $sha256) -and
        [string]$sha256 -notmatch '^[0-9a-fA-F]{64}$'
    ) {
        Add-Blocker 'rehearsalReport.edhrHashEvidence.sha256 must be a 64 character hex SHA-256 digest'
    }
    Require-ReadableEvidenceFile `
        -BaseDirectory $BaseDirectory `
        -PathValue (Get-JsonValue -Root $Evidence -Path 'rehearsalReport.edhrHashEvidence.evidencePath') `
        -FieldName 'rehearsalReport.edhrHashEvidence.evidencePath' | Out-Null
    Require-SuccessStatus -Root $Evidence -Path 'rehearsalReport.restoreValidationEvidence.status' | Out-Null
    $validatedArchiveId = Require-UsableValue -Root $Evidence -Path 'rehearsalReport.restoreValidationEvidence.validatedArchiveId'
    if (
        -not (Test-BlockedValue -Value $validatedArchiveId) -and
        -not (Test-BlockedValue -Value $archiveId) -and
        [string]$validatedArchiveId -ne [string]$archiveId
    ) {
        Add-Blocker 'rehearsalReport.restoreValidationEvidence.validatedArchiveId must equal rehearsalReport.edhrArchiveEvidence.archiveId'
    }
    Require-UsableValue -Root $Evidence -Path 'rehearsalReport.restoreValidationEvidence.sampleRecord' | Out-Null
    Require-ReadableEvidenceFile `
        -BaseDirectory $BaseDirectory `
        -PathValue (Get-JsonValue -Root $Evidence -Path 'rehearsalReport.restoreValidationEvidence.evidencePath') `
        -FieldName 'rehearsalReport.restoreValidationEvidence.evidencePath' | Out-Null

    $decision = if ($script:Blockers.Count -eq $before) { 'GO' } else { 'NO-GO' }
    Set-Gate -Name 'rehearsal' -Decision $decision -Details 'PASSED rehearsal with same backupId and eDHR archive, hash and restore validation evidence required'
}

function Add-StringEvidence {
    param(
        [object]$Value,
        [System.Collections.Generic.List[string]]$Strings
    )

    if ($null -eq $Value) {
        return
    }
    if ($Value -is [string]) {
        [void]$Strings.Add($Value)
        return
    }
    if ($Value -is [System.Collections.IEnumerable] -and -not ($Value -is [string])) {
        foreach ($item in @($Value)) {
            Add-StringEvidence -Value $item -Strings $Strings
        }
        return
    }
    foreach ($property in @($Value.PSObject.Properties)) {
        Add-StringEvidence -Value $property.Value -Strings $Strings
    }
}

function Test-CiEvidence {
    param(
        [object]$Evidence,
        [string]$BaseDirectory
    )

    $before = $script:Blockers.Count
    $e2eCommand = $null
    $e2eReportFile = $null
    $sections = @(
        'backendTests',
        'frontendTests',
        'e2eGates'
    )
    foreach ($section in $sections) {
        $statusPath = "ciEvidence.$section.status"
        $status = Get-JsonValue -Root $Evidence -Path $statusPath
        if ((Test-BlockedValue -Value $status) -or [string]$status -ne 'passed') {
            Add-Blocker "$statusPath must equal passed"
        }

        $commandPath = "ciEvidence.$section.command"
        $command = Require-UsableValue -Root $Evidence -Path $commandPath
        if (-not (Test-BlockedValue -Value $command)) {
            Require-NoCiSkipFlag -Text $command -FieldName $commandPath
        }
        if ($section -eq 'e2eGates') {
            $e2eCommand = $command
        }

        $reportPath = "ciEvidence.$section.reportPath"
        $reportFile = Read-RequiredEvidenceText `
            -BaseDirectory $BaseDirectory `
            -PathValue (Get-JsonValue -Root $Evidence -Path $reportPath) `
            -FieldName $reportPath
        if ($null -ne $reportFile) {
            if ([string]::IsNullOrWhiteSpace([string]$reportFile.Text)) {
                Add-Blocker "$reportPath must not be empty"
            } else {
                Require-NoCiSkipFlag -Text $reportFile.Text -FieldName $reportPath
            }
        }
        if ($section -eq 'e2eGates') {
            $e2eReportFile = $reportFile
        }
    }

    Require-EdhrReleaseCoverageCommand `
        -Command $e2eCommand `
        -FieldName 'ciEvidence.e2eGates.command'
    Test-EdhrReleaseCoverageReport `
        -ReportFile $e2eReportFile `
        -FieldName 'ciEvidence.e2eGates.reportPath'

    $decision = if ($script:Blockers.Count -eq $before) { 'GO' } else { 'NO-GO' }
    Set-Gate -Name 'ciEvidence' -Decision $decision -Details 'backend tests, frontend tests and eDHR release E2E coverage gate must pass without skip flags'
}

function Test-G8G9TopLevelBinding {
    param(
        [object]$Confirmation,
        [string]$ExpectedReleaseId,
        [string]$ExpectedImageTag,
        [string]$ExpectedBackupId
    )

    if ($null -eq $Confirmation) {
        return
    }

    $releaseId = Get-JsonValue -Root $Confirmation -Path 'releaseId'
    if (
        (Test-BlockedValue -Value $releaseId) -or
        -not (Test-BlockedValue -Value $ExpectedReleaseId) -and
        [string]$releaseId -ne $ExpectedReleaseId
    ) {
        Add-Blocker 'G8/G9 releaseId must equal releaseId'
    }

    $currentFaultImageTag = Get-JsonValue -Root $Confirmation -Path 'currentFaultImageTag'
    if (
        (Test-BlockedValue -Value $currentFaultImageTag) -or
        -not (Test-BlockedValue -Value $ExpectedImageTag) -and
        [string]$currentFaultImageTag -ne $ExpectedImageTag
    ) {
        Add-Blocker 'G8/G9 currentFaultImageTag must equal currentImageTag'
    }

    $selectedBackupId = Get-JsonValue -Root $Confirmation -Path 'g9.SelectedBackupId'
    if (
        (Test-BlockedValue -Value $selectedBackupId) -or
        -not (Test-BlockedValue -Value $ExpectedBackupId) -and
        [string]$selectedBackupId -ne $ExpectedBackupId
    ) {
        Add-Blocker 'G8/G9 g9.SelectedBackupId must equal backupId'
    }
}

function Test-G10G11TopLevelBinding {
    param(
        [object]$Confirmation,
        [string]$ExpectedReleaseId
    )

    if ($null -eq $Confirmation) {
        return
    }

    $releaseId = Get-JsonValue -Root $Confirmation -Path 'releaseId'
    if (
        (Test-BlockedValue -Value $releaseId) -or
        -not (Test-BlockedValue -Value $ExpectedReleaseId) -and
        [string]$releaseId -ne $ExpectedReleaseId
    ) {
        Add-Blocker 'G10/G11 releaseId must equal releaseId'
    }
}

if (-not (Test-Path -LiteralPath $EvidencePath -PathType Leaf)) {
    Add-Blocker "EvidencePath does not exist: $EvidencePath"
    Write-ResultAndExit -Decision 'NO-GO' -ExitCode 2
}

$resolvedEvidencePath = (Resolve-Path -LiteralPath $EvidencePath).Path
$evidenceDirectory = Split-Path -Parent $resolvedEvidencePath

try {
    $rawJson = [System.IO.File]::ReadAllText($resolvedEvidencePath, [System.Text.Encoding]::UTF8)
    $evidence = $rawJson | ConvertFrom-Json
} catch {
    Add-Blocker "EvidencePath must contain valid UTF-8 JSON: $($_.Exception.Message)"
    Write-ResultAndExit -Decision 'BLOCKED' -ExitCode 2
}

$script:ReleaseId = Require-UsableValue -Root $evidence -Path 'releaseId'
$script:CurrentImageTag = Require-UsableValue -Root $evidence -Path 'currentImageTag'
$script:BackupId = Require-UsableValue -Root $evidence -Path 'backupId'
$script:Environment = Require-UsableValue -Root $evidence -Path 'environment'
if (
    -not (Test-BlockedValue -Value $script:Environment) -and
    @('production', 'prod') -notcontains [string]$script:Environment
) {
    Add-Blocker 'environment must equal production or prod'
}

Test-ProtectedStorage -Evidence $evidence -BaseDirectory $evidenceDirectory
$backupId = Test-BackupNow `
    -Evidence $evidence `
    -ExpectedImageTag ([string]$script:CurrentImageTag) `
    -ExpectedBackupId ([string]$script:BackupId) `
    -BaseDirectory $evidenceDirectory
Test-Rehearsal `
    -Evidence $evidence `
    -ExpectedBackupId $backupId `
    -BaseDirectory $evidenceDirectory

$g8g9Path = Require-ReadableEvidenceFile `
    -BaseDirectory $evidenceDirectory `
    -PathValue (Get-JsonValue -Root $evidence -Path 'g8g9ConfirmationPath') `
    -FieldName 'g8g9ConfirmationPath'
$g10g11Path = Require-ReadableEvidenceFile `
    -BaseDirectory $evidenceDirectory `
    -PathValue (Get-JsonValue -Root $evidence -Path 'g10g11ConfirmationPath') `
    -FieldName 'g10g11ConfirmationPath'

if ($null -ne $g8g9Path) {
    $g8g9Confirmation = Read-ResolvedEvidenceJson `
        -ResolvedPath $g8g9Path `
        -FieldName 'g8g9ConfirmationPath'
    Test-G8G9TopLevelBinding `
        -Confirmation $g8g9Confirmation `
        -ExpectedReleaseId ([string]$script:ReleaseId) `
        -ExpectedImageTag ([string]$script:CurrentImageTag) `
        -ExpectedBackupId ([string]$script:BackupId)
    Invoke-ConfirmationValidator `
        -GateName 'G8/G9' `
        -ValidatorPath (Join-Path -Path $PSScriptRoot -ChildPath 'validate-g8-g9-confirmations.ps1') `
        -ConfirmationPath $g8g9Path
}
if ($null -ne $g10g11Path) {
    $g10g11Confirmation = Read-ResolvedEvidenceJson `
        -ResolvedPath $g10g11Path `
        -FieldName 'g10g11ConfirmationPath'
    Test-G10G11TopLevelBinding `
        -Confirmation $g10g11Confirmation `
        -ExpectedReleaseId ([string]$script:ReleaseId)
    Invoke-ConfirmationValidator `
        -GateName 'G10/G11' `
        -ValidatorPath (Join-Path -Path $PSScriptRoot -ChildPath 'validate-g10-g11-confirmations.ps1') `
        -ConfirmationPath $g10g11Path
}

Test-CiEvidence -Evidence $evidence -BaseDirectory $evidenceDirectory

if ($script:Blockers.Count -gt 0) {
    Write-ResultAndExit -Decision 'NO-GO' -ExitCode 2
}

Write-ResultAndExit -Decision 'GO' -ExitCode 0
