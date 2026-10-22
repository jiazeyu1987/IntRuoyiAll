$ErrorActionPreference = 'Stop'

$script:backupNowPath = Join-Path $PSScriptRoot '..\scripts\modules\UseCases\BackupNow.psm1'
$script:fileOpsPath = Join-Path $PSScriptRoot '..\scripts\modules\Infra\FileOps.psm1'
$script:objectOpsPath = Join-Path $PSScriptRoot '..\scripts\modules\Infra\ObjectOps.psm1'
$script:executorPath = Join-Path $PSScriptRoot '..\..\..\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\service\runtimecontrol\RuntimeControlCommandExecutorImpl.java'

$script:backupNowSource = [System.IO.File]::ReadAllText($script:backupNowPath, [System.Text.UTF8Encoding]::new($false))
$script:fileOpsSource = [System.IO.File]::ReadAllText($script:fileOpsPath, [System.Text.UTF8Encoding]::new($false))
$script:objectOpsSource = [System.IO.File]::ReadAllText($script:objectOpsPath, [System.Text.UTF8Encoding]::new($false))
$script:executorSource = [System.IO.File]::ReadAllText($script:executorPath, [System.Text.UTF8Encoding]::new($false))

Describe 'backup-now cleanup and large payload timeout contract' {
    It 'resets-the-test-backup-repository-before-creating-a-new-backup' {
        $script:backupNowSource | Should Match 'Invoke-BackupOpsRemoteRepositoryReset'
        $cleanupIndex = $script:backupNowSource.IndexOf('Invoke-BackupOpsRemoteRepositoryReset')
        $workspaceIndex = $script:backupNowSource.IndexOf('New-BackupOpsBackupWorkspace')
        $cleanupIndex | Should BeGreaterThan -1
        $workspaceIndex | Should BeGreaterThan $cleanupIndex
    }

    It 'limits-remote-delete-to-the-test-BackupPackage-direct-children-and-object-store' {
        $script:fileOpsSource | Should Match 'EXPECTED_ROOT = "/mnt/nas/Backup/BackupPackage"'
        $script:fileOpsSource | Should Match 'BACKUP_POINT_RE = re\.compile\(r"\^\\d\{8\}-\\d\{6\}\(\\\.creating\)\?\$"\)'
        $script:fileOpsSource | Should Match 'item\.is_symlink\(\)'
        $script:fileOpsSource | Should Match '"object-store"'
        $script:fileOpsSource | Should Match '"\.restore-stage"'
        $script:fileOpsSource | Should Not Match 'shutil\.rmtree\(root\)'
    }

    It 'allows-real-100-gib-object-copy-and-runtime-control-operation-to-run-for-twelve-hours' {
        $script:objectOpsSource | Should Match '\$objectCopyTimeoutSeconds\s*=\s*43200'
        $script:executorSource | Should Match 'OPERATION_COMMAND_TIMEOUT\s*=\s*Duration\.ofHours\(12\)'
    }
}
