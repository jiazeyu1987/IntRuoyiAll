from pathlib import Path

BACKEND = Path(__file__).resolve().parents[3]


def test_runtime_restore_and_restart_share_persistent_fence():
    restart = (BACKEND / 'script/deploy/restart-int-ruoyi-remote.ps1').read_text(encoding='utf-8-sig')
    assert 'Get-RemoteRuntimeLeaseShell -Action Fence' in restart
    assert restart.index('Complete-RemoteRuntimeLease') > restart.index('Wait-HttpOk -Url')
    transport = (BACKEND / 'script/backup-ops/scripts/modules/Infra/SshOps.psm1').read_text(encoding='utf-8-sig')
    assert 'Get-RemoteRuntimeLeaseShell -Action Fence' in transport
    assert 'Send-RemoteRuntimeLeaseFile' in transport
    launcher = (BACKEND / 'script/backup-ops/scripts/backup-ops.ps1').read_text(encoding='utf-8-sig')
    assert "if ([string]$result.status -eq 'success') { Complete-BackupRuntimeResourceLease }" in launcher
    assert 'RESTORE_TARGET_MAPPING_MISMATCH' in launcher


def test_legacy_executor_explicitly_refuses_independent_release():
    script = (BACKEND / 'script/deploy/publish-int-ruoyi.ps1').read_text(encoding='utf-8-sig')
    assert "if ($DeployIntent -eq 'independent-backup')" in script
    assert 'INDEPENDENT_BACKUP_CONTROLLED_EXECUTOR_REQUIRED' in script
