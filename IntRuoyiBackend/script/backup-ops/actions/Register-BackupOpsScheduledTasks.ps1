[CmdletBinding()]
param(
    [string]$ConfigPath = '',
    [string]$SecretsPath = '',
    [switch]$PlanOnly
)

Set-StrictMode -Version Latest
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backupOpsRoot = Split-Path -Parent $scriptDir
$configModulePath = Join-Path $backupOpsRoot 'scripts\modules\Core\Config.psm1'
Import-Module $configModulePath -Force -DisableNameChecking | Out-Null

function Resolve-BackupOpsScriptPath {
    return (Join-Path $backupOpsRoot 'scripts\backup-ops.ps1')
}

function ConvertTo-BackupOpsDailyTrigger {
    param(
        [Parameter(Mandatory)]
        [string]$Schedule
    )

    $hour, $minute = $Schedule.Split(':')
    $today = Get-Date
    $startBoundary = Get-Date -Year $today.Year -Month $today.Month -Day $today.Day -Hour ([int]$hour) -Minute ([int]$minute) -Second 0
    return New-ScheduledTaskTrigger -Daily -At $startBoundary
}

function ConvertTo-BackupOpsWeeklyTrigger {
    param(
        [Parameter(Mandatory)]
        [string]$Schedule
    )

    $parts = $Schedule.Split(' ', 2)
    $dayCode = $parts[0]
    $timeValue = $parts[1]
    $hour, $minute = $timeValue.Split(':')
    $dayMap = @{
        MON = 'Monday'
        TUE = 'Tuesday'
        WED = 'Wednesday'
        THU = 'Thursday'
        FRI = 'Friday'
        SAT = 'Saturday'
        SUN = 'Sunday'
    }
    $today = Get-Date
    $startBoundary = Get-Date -Year $today.Year -Month $today.Month -Day $today.Day -Hour ([int]$hour) -Minute ([int]$minute) -Second 0
    return New-ScheduledTaskTrigger -Weekly -DaysOfWeek $dayMap[$dayCode] -At $startBoundary
}

function ConvertTo-BackupOpsBackupTrigger {
    param(
        [Parameter(Mandatory)]
        [object]$BackupConfig
    )

    $frequency = [string]$BackupConfig.frequency
    if ([string]::IsNullOrWhiteSpace($frequency)) {
        $frequency = 'DAILY'
    }
    $frequency = $frequency.Trim().ToUpperInvariant()
    if ($frequency -eq 'DAILY') {
        return ConvertTo-BackupOpsDailyTrigger -Schedule ([string]$BackupConfig.schedule)
    }
    if ($frequency -eq 'WEEKLY') {
        $weekday = [string]$BackupConfig.weekday
        if ([string]::IsNullOrWhiteSpace($weekday)) {
            throw 'backup.weekday is required when backup.frequency is WEEKLY.'
        }
        return ConvertTo-BackupOpsWeeklyTrigger -Schedule ('{0} {1}' -f $weekday.Trim().ToUpperInvariant(), [string]$BackupConfig.schedule)
    }
    throw "Unsupported backup.frequency: $frequency"
}

function Resolve-BackupOpsRepositoryEnvironment {
    param(
        [Parameter(Mandatory)]
        [object]$Config
    )

    $repositoryEnvironment = [string]$Config.backup.repositoryEnvironment
    if ([string]::IsNullOrWhiteSpace($repositoryEnvironment)) {
        throw 'backup.repositoryEnvironment is required'
    }

    $repositoryEnvironment = $repositoryEnvironment.Trim().ToLowerInvariant()
    if ($repositoryEnvironment -notin @('test', 'backup')) {
        throw "Unsupported backup.repositoryEnvironment: $repositoryEnvironment"
    }

    return $repositoryEnvironment
}

function Resolve-BackupOpsTaskPrincipalId {
    param(
        [Parameter(Mandatory)]
        [object]$Config
    )

    $principalId = [string]$Config.raw.secrets.taskPrincipal.principalId
    if ([string]::IsNullOrWhiteSpace($principalId)) {
        throw 'taskPrincipal.principalId is required'
    }

    $principalId = $principalId.Trim()
    $currentIdentity = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    if ($principalId.Equals($currentIdentity, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'taskPrincipal.principalId must be a dedicated scheduled-task account, not the current interactive user'
    }
    if ($principalId.Equals('SYSTEM', [System.StringComparison]::OrdinalIgnoreCase) -or
        $principalId.Equals('NT AUTHORITY\SYSTEM', [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'taskPrincipal.principalId must not be SYSTEM'
    }

    return $principalId
}

function Resolve-BackupOpsProductionAuthorizationProof {
    param(
        [Parameter(Mandatory)]
        [object]$Config
    )

    $confirmText = [string]$Config.raw.secrets.auth.productionBackupConfirmText
    if ([string]::IsNullOrWhiteSpace($confirmText)) {
        throw 'auth.productionBackupConfirmText is required'
    }

    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($confirmText)
        $hash = $sha256.ComputeHash($bytes)
        $hex = [System.BitConverter]::ToString($hash).Replace('-', '').ToLowerInvariant()
        return 'masked:sha256:{0}' -f $hex.Substring(0, 12)
    } finally {
        $sha256.Dispose()
    }
}

function ConvertTo-BackupOpsPrincipalSid {
    param(
        [Parameter(Mandatory)]
        [string]$PrincipalId
    )

    try {
        return ([System.Security.Principal.NTAccount]$PrincipalId).Translate([System.Security.Principal.SecurityIdentifier]).Value
    } catch {
        throw "taskPrincipal.principalId cannot be resolved: $PrincipalId"
    }
}

function Assert-BackupOpsBatchLogonRight {
    param(
        [Parameter(Mandatory)]
        [string]$PrincipalId
    )

    $principalSid = ConvertTo-BackupOpsPrincipalSid -PrincipalId $PrincipalId
    $exportPath = Join-Path ([System.IO.Path]::GetTempPath()) ('backup-ops-user-rights-{0}.inf' -f [System.Guid]::NewGuid().ToString('N'))
    try {
        & secedit.exe /export /cfg $exportPath /areas USER_RIGHTS | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw 'Unable to export local user-rights policy for scheduled-task principal validation.'
        }
        $policyText = [System.IO.File]::ReadAllText($exportPath, [System.Text.UTF8Encoding]::new($false))
        $batchRightLine = $policyText -split "\r?\n" | Where-Object { $_ -match '^SeBatchLogonRight\s*=' } | Select-Object -First 1
        if ([string]::IsNullOrWhiteSpace($batchRightLine) -or $batchRightLine -notmatch [regex]::Escape('*' + $principalSid)) {
            throw "taskPrincipal.principalId must have Log on as a batch job: $PrincipalId"
        }
    } finally {
        Remove-Item -LiteralPath $exportPath -Force -ErrorAction SilentlyContinue
    }
}

function Test-BackupOpsAccessRuleMatchesSid {
    param(
        [Parameter(Mandatory)]
        [System.Security.AccessControl.FileSystemAccessRule]$Rule,
        [Parameter(Mandatory)]
        [string]$Sid,
        [Parameter(Mandatory)]
        [System.Security.AccessControl.FileSystemRights]$RequiredRights
    )

    if ($Rule.AccessControlType -ne [System.Security.AccessControl.AccessControlType]::Allow) {
        return $false
    }

    try {
        $ruleSid = $Rule.IdentityReference.Translate([System.Security.Principal.SecurityIdentifier]).Value
    } catch {
        return $false
    }

    return $ruleSid -eq $Sid -and (($Rule.FileSystemRights -band $RequiredRights) -ne 0)
}

function Assert-BackupOpsPrincipalAclIdentity {
    param(
        [Parameter(Mandatory)]
        [string]$PrincipalId,
        [Parameter(Mandatory)]
        [string[]]$Paths
    )

    $principalSid = ConvertTo-BackupOpsPrincipalSid -PrincipalId $PrincipalId
    $requiredRights = [System.Security.AccessControl.FileSystemRights]::ReadAndExecute -bor [System.Security.AccessControl.FileSystemRights]::Read
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) {
            throw "ACL validation target is missing: $path"
        }
        $acl = Get-Acl -LiteralPath $path
        $hasPrincipalAccess = $false
        foreach ($rule in $acl.Access) {
            if (Test-BackupOpsAccessRuleMatchesSid -Rule $rule -Sid $principalSid -RequiredRights $requiredRights) {
                $hasPrincipalAccess = $true
                break
            }
        }
        if (-not $hasPrincipalAccess) {
            throw "Principal ACL identity mismatch for $path"
        }
    }
}

function Assert-BackupOpsSecretsAcl {
    param(
        [Parameter(Mandatory)]
        [string]$Path,
        [switch]$RejectOrdinaryUserWrite
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Secrets file is missing: $Path"
    }

    if (-not $RejectOrdinaryUserWrite) {
        return
    }

    $ordinaryUserSids = @(
        'S-1-1-0',
        'S-1-5-11',
        'S-1-5-32-545'
    )
    $writeRights =
        [System.Security.AccessControl.FileSystemRights]::Write -bor
        [System.Security.AccessControl.FileSystemRights]::Modify -bor
        [System.Security.AccessControl.FileSystemRights]::FullControl -bor
        [System.Security.AccessControl.FileSystemRights]::WriteData -bor
        [System.Security.AccessControl.FileSystemRights]::AppendData -bor
        [System.Security.AccessControl.FileSystemRights]::WriteAttributes -bor
        [System.Security.AccessControl.FileSystemRights]::WriteExtendedAttributes -bor
        [System.Security.AccessControl.FileSystemRights]::ChangePermissions -bor
        [System.Security.AccessControl.FileSystemRights]::TakeOwnership

    $acl = Get-Acl -LiteralPath $Path
    foreach ($rule in $acl.Access) {
        if ($rule.AccessControlType -ne [System.Security.AccessControl.AccessControlType]::Allow) {
            continue
        }
        try {
            $ruleSid = $Rule.IdentityReference.Translate([System.Security.Principal.SecurityIdentifier]).Value
        } catch {
            continue
        }
        if ($ordinaryUserSids -contains $ruleSid -and (($rule.FileSystemRights -band $writeRights) -ne 0)) {
            throw "Ordinary users must not have write access to secrets: $Path"
        }
    }
}

function New-BackupOpsScheduledTaskPlan {
    param(
        [Parameter(Mandatory)]
        [string]$TaskName,
        [Parameter(Mandatory)]
        [string]$Mode,
        [Parameter(Mandatory)]
        [string]$ConfigPath,
        [Parameter(Mandatory)]
        [string]$SecretsPath,
        [Parameter(Mandatory)]
        [object]$Trigger,
        [Parameter(Mandatory)]
        [string]$PrincipalId,
        [ValidateSet('test', 'backup')]
        [string]$repositoryEnvironment = '',
        [string]$OperatorName = '',
        [string]$TargetEnvironment = '',
        [string]$ProductionAuthorizationProof = ''
    )

    $scriptPath = Resolve-BackupOpsScriptPath
    $argumentParts = @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-File', ('"{0}"' -f $scriptPath),
        '-Mode', $Mode,
        '-ConfigPath', ('"{0}"' -f $ConfigPath),
        '-SecretsPath', ('"{0}"' -f $SecretsPath),
        '-NonInteractive'
    )
    if (-not [string]::IsNullOrWhiteSpace($TargetEnvironment)) {
        $argumentParts += @('-TargetEnvironment', $TargetEnvironment)
    }
    if (-not [string]::IsNullOrWhiteSpace($repositoryEnvironment)) {
        $argumentParts += @('-RepositoryEnvironment', $repositoryEnvironment)
    }
    if (-not [string]::IsNullOrWhiteSpace($OperatorName)) {
        $argumentParts += @('-OperatorName', ('"{0}"' -f $OperatorName))
    }
    $arguments = $argumentParts -join ' '

    return [pscustomobject]([ordered]@{
            taskName                     = $TaskName
            mode                         = $Mode
            executable                   = 'powershell.exe'
            arguments                    = $arguments
            trigger                      = $Trigger
            principalId                  = $PrincipalId
            logonType                    = 'S4U'
            runLevel                     = 'Limited'
            productionAuthorizationProof = $ProductionAuthorizationProof
        })
}

$config = Import-BackupOpsConfiguration -ConfigPath $ConfigPath -SecretsPath $SecretsPath
$resolvedConfigPath = if ([string]::IsNullOrWhiteSpace($ConfigPath)) { (Get-BackupOpsDefaultConfigPaths).configPath } else { $ConfigPath }
$resolvedSecretsPath = if ([string]::IsNullOrWhiteSpace($SecretsPath)) { (Get-BackupOpsDefaultConfigPaths).secretsPath } else { $SecretsPath }
$repositoryEnvironment = Resolve-BackupOpsRepositoryEnvironment -Config $config
$principalId = Resolve-BackupOpsTaskPrincipalId -Config $config
$productionAuthorizationProof = Resolve-BackupOpsProductionAuthorizationProof -Config $config
$backupOpsScriptPath = Resolve-BackupOpsScriptPath

Assert-BackupOpsBatchLogonRight -PrincipalId $principalId
Assert-BackupOpsPrincipalAclIdentity -PrincipalId $principalId -Paths @($backupOpsScriptPath, $resolvedConfigPath, $resolvedSecretsPath)
Assert-BackupOpsSecretsAcl -Path $resolvedSecretsPath -RejectOrdinaryUserWrite

$backupPlan = New-BackupOpsScheduledTaskPlan `
    -TaskName 'IntRuoyi Backup Scheduled' `
    -Mode 'backup-scheduled' `
    -ConfigPath $resolvedConfigPath `
    -SecretsPath $resolvedSecretsPath `
    -Trigger (ConvertTo-BackupOpsBackupTrigger -BackupConfig $config.backup) `
    -PrincipalId $principalId `
    -TargetEnvironment 'prod' `
    -RepositoryEnvironment $repositoryEnvironment `
    -ProductionAuthorizationProof $productionAuthorizationProof `
    -OperatorName 'scheduler'

$rehearsalPlan = New-BackupOpsScheduledTaskPlan `
    -TaskName 'IntRuoyi Rehearsal' `
    -Mode 'rehearsal' `
    -ConfigPath $resolvedConfigPath `
    -SecretsPath $resolvedSecretsPath `
    -Trigger (ConvertTo-BackupOpsWeeklyTrigger -Schedule ([string]$config.rehearsal.schedule)) `
    -PrincipalId $principalId `
    -OperatorName 'scheduler'

$plans = @($backupPlan, $rehearsalPlan)
if ($PlanOnly) {
    $plans | Select-Object taskName, mode, executable, arguments, principalId, logonType, runLevel, productionAuthorizationProof | ConvertTo-Json -Depth 6
    exit 0
}

$principal = New-ScheduledTaskPrincipal -UserId $principalId -LogonType S4U -RunLevel Limited
foreach ($plan in $plans) {
    $action = New-ScheduledTaskAction -Execute $plan.executable -Argument $plan.arguments
    Register-ScheduledTask -TaskName $plan.taskName -Action $action -Trigger $plan.trigger -Principal $principal -Description "IntRuoyi $($plan.mode)" -Force | Out-Null
}

$plans | Select-Object taskName, mode, principalId, logonType, runLevel | Format-Table -AutoSize
