Set-StrictMode -Version Latest

$script:Utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$script:ModulePath = Join-Path $PSScriptRoot '..\backup-ops\scripts\modules\UseCases\Rehearsal.psm1'
$script:TempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('backup-ops-rehearsal-tests-' + [System.Guid]::NewGuid().ToString('N'))
$script:SentFiles = @()
$script:VerificationStates = @()
$script:FailEvidenceUpload = $false
$script:LatestBackupRequests = 0

function Assert-True {
    param(
        [Parameter(Mandatory = $true)]
        [bool]$Condition,
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Reset-TestState {
    $script:SentFiles = @()
    $script:VerificationStates = @()
    $script:FailEvidenceUpload = $false
    $script:LatestBackupRequests = 0
    if (Test-Path -LiteralPath $script:TempRoot) {
        Remove-Item -LiteralPath $script:TempRoot -Recurse -Force
    }
    [void][System.IO.Directory]::CreateDirectory($script:TempRoot)
}

function New-TestConfig {
    return [pscustomobject]@{
        servers = [pscustomobject]@{
            test = [pscustomobject]@{
                host = '127.0.0.1'
                backupPointsRoot = '/backup/int-ruoyi/points'
            }
        }
        ssh = [pscustomobject]@{
            user = 'backup'
            port = 22
        }
        auth = [pscustomobject]@{
            sshKeyPath = 'C:\keys\backup'
            knownHostsPath = 'C:\keys\known_hosts'
        }
    }
}

function Start-BackupOpsLogSession {
    param([object]$Config, [string]$Action, [string]$OperatorName, [object]$StartedAt)
    $logPath = Join-Path $script:TempRoot 'rehearsal.log'
    [System.IO.File]::WriteAllText($logPath, '', $script:Utf8NoBom)
    return [pscustomobject]@{
        LogPath = $logPath
        ReportPath = $null
    }
}

function Show-BackupOpsProgress {
    param([int]$Current, [int]$Total, [string]$Message)
}

function Get-BackupOpsRehearsalCandidate {
    param([object]$Config, [string]$BackupId, [object]$LogSession)
    return [pscustomobject]@{
        backupId = $BackupId
        imageTag = 'registry.example/int-ruoyi:20260526'
    }
}

function Get-BackupOpsLatestBackup {
    param([object]$Config, [object]$LogSession)
    $script:LatestBackupRequests += 1
    return [pscustomobject]@{
        BackupId = 'backup-20260526-latest'
        ImageTag = 'registry.example/int-ruoyi:latest'
    }
}

function Restore-BackupOpsRehearsalRuntime {
    param([object]$Config, [string]$BackupId, [object]$LogSession)
    return [pscustomobject]@{
        imageTag = 'registry.example/int-ruoyi:20260526'
    }
}

function Test-BackupOpsRehearsalValidation {
    param([object]$Config, [string]$BackupId, [object]$LogSession)
    return [pscustomobject]@{
        checks = [pscustomobject]@{
            backend = 'OK'
            frontend = 'OK'
            login = 'OK'
            sampleFile = 'OK'
        }
    }
}

function Set-BackupOpsRehearsalVerificationState {
    param([object]$Config, [string]$BackupId, [string]$RehearsalStatus, [string]$Note = '', [object]$LogSession)
    $script:VerificationStates += [pscustomobject]@{
        BackupId = $BackupId
        Status = $RehearsalStatus
        Note = $Note
    }
}

function Send-BackupFileOverSsh {
    param([hashtable]$Request)
    if ($script:FailEvidenceUpload -and [string]$Request.RemotePath -like '*/rehearsal-report.json') {
        throw 'simulated evidence upload failure'
    }

    $script:SentFiles += [pscustomobject]@{
        Host = [string]$Request.Host
        User = [string]$Request.User
        KeyPath = [string]$Request.KeyPath
        RemotePath = [string]$Request.RemotePath
        LocalPath = [string]$Request.LocalPath
        Text = [System.IO.File]::ReadAllText([string]$Request.LocalPath, $script:Utf8NoBom)
    }
}

function Write-BackupOpsLog {
    param([object]$Session, [string]$Message, [string]$Level = 'INFO')
}

function Publish-BackupOpsReport {
    param([object]$Config, [string]$Action, [string]$Status, [object]$StartedAt, [object]$CompletedAt, [string]$Summary, [hashtable]$Context, [object]$LogSession)
    return [pscustomobject]@{
        LogPath = $LogSession.LogPath
        ReportPath = Join-Path $script:TempRoot 'rehearsal.report.md'
    }
}

function Send-BackupOpsNotification {
    param([object]$Config, [string]$Action, [string]$Status, [string]$Summary, [hashtable]$Context, [object]$LogSession)
    return [pscustomobject]@{ status = 'sent' }
}

function Invoke-BackupOpsNotificationCapture {
    param([object]$Config, [string]$Action, [string]$Status, [string]$Summary, [hashtable]$Context, [object]$LogSession)
    return [pscustomobject]@{ status = 'captured' }
}

function Set-BackupOpsNotificationContext {
    param([hashtable]$Context, [object]$NotificationResult)
}

function Get-BackupOpsNotificationOutcomeMessage {
    param([object]$NotificationResult)
    return ''
}

function New-BackupOpsResult {
    param([string]$Action, [string]$Status, [string]$Code, [string]$Message, [object]$StartedAt, [object]$CompletedAt, [string]$LogPath, [string]$ReportPath, [hashtable]$Context)
    return [pscustomobject]@{
        action = $Action
        status = $Status
        code = $Code
        message = $Message
        startedAt = $StartedAt
        completedAt = $CompletedAt
        logPath = $LogPath
        reportPath = $ReportPath
        context = $Context
    }
}

function Complete-BackupOpsOutcome {
    param([object]$Config, [string]$Action, [string]$Status, [string]$Code, [string]$Message, [object]$StartedAt, [object]$CompletedAt, [hashtable]$Context, [object]$LogSession)
    return [pscustomobject]@{
        action = $Action
        status = $Status
        code = $Code
        message = $Message
        startedAt = $StartedAt
        completedAt = $CompletedAt
        context = $Context
    }
}

Import-Module $script:ModulePath -Force -ErrorAction Stop

try {
    Reset-TestState
    Write-Host 'BDD: 默认 operator rehearsal 必须显式选择备份点 -> Given 未传 OperatorName When 未传 SelectedBackupId Then blocked 且不得自动选择 latest'
    $defaultOperatorBlocked = Invoke-RehearsalUseCase -Config (New-TestConfig) -SelectedBackupId ''
    Assert-True -Condition ($defaultOperatorBlocked.status -eq 'blocked') -Message 'Expected default operator rehearsal without backup id to be blocked.'
    Assert-True -Condition ($defaultOperatorBlocked.code -eq 'INTBK-7001') -Message 'Expected INTBK-7001 for missing default operator rehearsal backup id.'
    Assert-True -Condition ($defaultOperatorBlocked.message.Contains('SelectedBackupId')) -Message 'Expected SelectedBackupId guidance for default operator.'
    Assert-True -Condition ($script:LatestBackupRequests -eq 0) -Message 'Default operator rehearsal must not request the latest backup.'
    Assert-True -Condition (@($script:SentFiles).Count -eq 0) -Message 'Default operator rehearsal must not upload evidence when backup id is missing.'
    Write-Host 'GREEN: powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_rehearsal_evidence.ps1 -> PASS default operator backup id guard scenario'

    Reset-TestState
    Write-Host 'BDD: 手工 rehearsal 必须显式选择备份点 -> Given operator 不是 scheduler When 未传 SelectedBackupId Then blocked 且不得自动选择 latest'
    $manualBlocked = Invoke-RehearsalUseCase -Config (New-TestConfig) -SelectedBackupId '' -OperatorName 'worker-a'
    Assert-True -Condition ($manualBlocked.status -eq 'blocked') -Message 'Expected manual rehearsal without backup id to be blocked.'
    Assert-True -Condition ($manualBlocked.code -eq 'INTBK-7001') -Message 'Expected INTBK-7001 for missing manual rehearsal backup id.'
    Assert-True -Condition ($manualBlocked.message.Contains('SelectedBackupId')) -Message 'Expected SelectedBackupId guidance.'
    Assert-True -Condition ($script:LatestBackupRequests -eq 0) -Message 'Manual rehearsal must not request the latest backup.'
    Assert-True -Condition (@($script:SentFiles).Count -eq 0) -Message 'Manual rehearsal must not upload evidence when backup id is missing.'
    Write-Host 'GREEN: powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_rehearsal_evidence.ps1 -> PASS manual backup id guard scenario'

    Reset-TestState
    Write-Host 'BDD: 计划任务 rehearsal 可选择最近备份 -> Given operator 是 scheduler When 未传 SelectedBackupId Then 使用 latest 并写回证据'
    $scheduled = Invoke-RehearsalUseCase -Config (New-TestConfig) -SelectedBackupId '' -OperatorName 'scheduler'
    Assert-True -Condition ($scheduled.status -eq 'success') -Message 'Expected scheduled rehearsal to auto-select latest backup.'
    Assert-True -Condition ($script:LatestBackupRequests -eq 1) -Message 'Expected exactly one latest backup lookup for scheduled rehearsal.'
    Assert-True -Condition ($scheduled.context.backupId -eq 'backup-20260526-latest') -Message 'Expected scheduled rehearsal context to use latest backup.'
    $scheduledJsonUpload = $script:SentFiles | Where-Object { $_.RemotePath -eq '/backup/int-ruoyi/points/backup-20260526-latest/manifest/rehearsal-report.json' } | Select-Object -First 1
    Assert-True -Condition ($null -ne $scheduledJsonUpload) -Message 'Expected scheduled rehearsal evidence upload.'
    Write-Host 'GREEN: powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_rehearsal_evidence.ps1 -> PASS scheduled latest scenario'

    Reset-TestState
    Write-Host 'BDD: 正式 rehearsal 成功写回 manifest 证据 -> Given 有备份点 When 演练成功 Then manifest 目录存在 PASSED JSON 与现场快照'
    $success = Invoke-RehearsalUseCase -Config (New-TestConfig) -SelectedBackupId 'backup-20260526-010000' -OperatorName 'worker-a'
    Assert-True -Condition ($success.status -eq 'success') -Message 'Expected successful rehearsal result.'
    Assert-True -Condition (@($script:VerificationStates | Where-Object { $_.BackupId -eq 'backup-20260526-010000' -and $_.Status -eq 'PASSED' }).Count -eq 1) -Message 'Expected successful rehearsal to mark manifest rehearsalStatus as PASSED.'
    $jsonUpload = $script:SentFiles | Where-Object { $_.RemotePath -eq '/backup/int-ruoyi/points/backup-20260526-010000/manifest/rehearsal-report.json' } | Select-Object -First 1
    $snapshotUpload = $script:SentFiles | Where-Object { $_.RemotePath -eq '/backup/int-ruoyi/points/backup-20260526-010000/manifest/现场快照.md' } | Select-Object -First 1
    Assert-True -Condition ($null -ne $jsonUpload) -Message 'Expected rehearsal-report.json upload.'
    Assert-True -Condition ($jsonUpload.Host -eq '127.0.0.1') -Message 'Expected upload to use configured test host.'
    Assert-True -Condition ($jsonUpload.KeyPath -eq 'C:\keys\backup') -Message 'Expected upload to use configured ssh key.'
    Assert-True -Condition ($null -ne $snapshotUpload) -Message 'Expected site snapshot upload.'
    $report = $jsonUpload.Text | ConvertFrom-Json
    Assert-True -Condition ($report.status -eq 'PASSED') -Message 'Expected PASSED status in rehearsal-report.json.'
    Assert-True -Condition (-not [string]::IsNullOrWhiteSpace([string]$report.lastVerifiedAt)) -Message 'Expected lastVerifiedAt in rehearsal-report.json.'
    Assert-True -Condition ($snapshotUpload.Text.Contains('演练对象: 恢复演练')) -Message 'Expected rehearsal object in snapshot.'
    Assert-True -Condition ($snapshotUpload.Text.Contains('备份点: backup-20260526-010000')) -Message 'Expected backup id in snapshot.'
    Assert-True -Condition ($snapshotUpload.Text.Contains('backend: OK')) -Message 'Expected check summary in snapshot.'
    Write-Host 'GREEN: powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_rehearsal_evidence.ps1 -> PASS success evidence scenario'

    Reset-TestState
    Write-Host 'BDD: 证据写回失败不得伪造 PASSED -> Given 上传失败 When 演练进入证据写回 Then 返回失败且备份点 pending-review'
    $script:FailEvidenceUpload = $true
    $failure = Invoke-RehearsalUseCase -Config (New-TestConfig) -SelectedBackupId 'backup-20260526-020000' -OperatorName 'worker-a'
    Assert-True -Condition ($failure.status -eq 'fail') -Message 'Expected failed rehearsal result when evidence upload fails.'
    Assert-True -Condition (@($script:VerificationStates | Where-Object { $_.Status -eq 'pending-review' }).Count -eq 1) -Message 'Expected backup point to be marked pending-review.'
    Assert-True -Condition (@($script:SentFiles | Where-Object { $_.RemotePath -like '*/rehearsal-report.json' }).Count -eq 0) -Message 'Expected no uploaded PASSED report after evidence upload failure.'
    Write-Host 'GREEN: powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_rehearsal_evidence.ps1 -> PASS failed evidence scenario'

    Reset-TestState
    Write-Host 'BDD: 缺少 SSH 前置条件必须失败 -> Given 缺少 sshKeyPath When 写回证据 Then fail-fast 且不上传报告'
    $badConfig = New-TestConfig
    $badConfig.auth.sshKeyPath = ''
    $blocked = Invoke-RehearsalUseCase -Config $badConfig -SelectedBackupId 'backup-20260526-030000' -OperatorName 'worker-a'
    Assert-True -Condition ($blocked.status -eq 'blocked') -Message 'Expected blocked rehearsal result when sshKeyPath is missing.'
    Assert-True -Condition ($blocked.code -eq 'INTBK-7002') -Message 'Expected INTBK-7002 for missing evidence upload precondition.'
    Assert-True -Condition (@($script:SentFiles | Where-Object { $_.RemotePath -like '*/rehearsal-report.json' }).Count -eq 0) -Message 'Expected no uploaded report when ssh precondition is missing.'
    Write-Host 'GREEN: powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_rehearsal_evidence.ps1 -> PASS missing ssh precondition scenario'
}
finally {
    Remove-Module Rehearsal -Force -ErrorAction SilentlyContinue
    if (Test-Path -LiteralPath $script:TempRoot) {
        Remove-Item -LiteralPath $script:TempRoot -Recurse -Force
    }
}
