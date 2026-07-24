from pathlib import Path


def _backup_root() -> Path:
    return Path(__file__).resolve().parents[1] / "backup-ops"


def test_runtime_backup_ops_real_config_exists_with_real_hosts() -> None:
    config_path = _backup_root() / "config" / "backup-ops.config.json"
    secrets_path = _backup_root() / "config" / "backup-ops.secrets.json"

    assert config_path.exists(), "real backup ops config should exist for phase-2 integration"
    assert secrets_path.exists(), "real backup ops secrets descriptor should exist for phase-2 integration"

    config_text = config_path.read_text(encoding="utf-8")
    secrets_text = secrets_path.read_text(encoding="utf-8")

    assert '"host": "172.30.30.57"' in config_text
    assert '"host": "172.30.30.58"' in config_text
    assert '"/opt/intruoyi/runtime"' in config_text
    assert '"/mnt/nas/Backup/BackupPackage"' in config_text
    assert '"/mnt/nas/int-ruoyi/backups"' not in config_text
    assert '"minio": "ragflow_compose-minio-1"' in config_text
    assert '"sshKeyPath"' in secrets_text
    assert '"user": "root"' in secrets_text


def test_real_mysql_integration_no_longer_uses_phase1_skeleton_messages() -> None:
    text = (_backup_root() / "scripts" / "modules" / "Infra" / "MySqlOps.psm1").read_text(encoding="utf-8")

    assert "Phase-1 skeleton only" not in text
    assert "mysqldump" in text
    assert "--single-transaction" in text
    assert "--routines" in text
    assert "--triggers" in text
    assert "--hex-blob" in text
    assert "mysqladmin" in text
    assert "docker exec" in text


def test_real_docker_integration_no_longer_uses_phase1_skeleton_messages_for_target_flows() -> None:
    text = (_backup_root() / "scripts" / "modules" / "Infra" / "DockerOps.psm1").read_text(encoding="utf-8")

    for marker in [
        "运行时 .env 备份",
        "运行时 IMAGE_TAG 更新",
        "backend/frontend 重启流程",
        "backend/frontend 停机流程",
        "恢复后的健康检查与抽样验证",
    ]:
        assert marker in text

    assert "Phase-1 skeleton only" not in text
    assert "docker compose" in text
    assert "curl -fsS" in text or "Invoke-WebRequest" in text
    assert "IMAGE_TAG" in text


def test_real_object_backup_integration_no_longer_uses_phase1_skeleton_messages() -> None:
    text = (_backup_root() / "scripts" / "modules" / "Infra" / "ObjectOps.psm1").read_text(encoding="utf-8")

    assert "Phase-1 skeleton only" not in text
    assert "mc mirror" in text
    assert "objectBucket" in text
    assert "docker inspect --format" in text
    assert "Receive-BackupFileOverSsh" in text


def test_real_file_and_notify_integration_no_longer_use_phase1_skeleton_messages() -> None:
    file_text = (_backup_root() / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")
    notify_text = (_backup_root() / "scripts" / "modules" / "Infra" / "NotifyOps.psm1").read_text(encoding="utf-8")

    assert "Phase-1 skeleton only" not in file_text
    assert "Receive-BackupFileOverSsh" in file_text
    assert "Send-BackupFileOverSsh" in file_text
    assert "find " in file_text
    assert "-PlanOnly" not in notify_text
    assert "webhook" in notify_text.lower()
    assert "Invoke-RestMethod" in notify_text or "Invoke-WebRequest" in notify_text
    assert "通知未发送" in notify_text
    assert "Set-BackupOpsNotificationContext" in notify_text
    assert "Invoke-BackupOpsNotificationCapture" in notify_text


def test_backup_now_restore_and_rollback_usecases_no_longer_expect_blocked_only() -> None:
    root = _backup_root() / "scripts" / "modules" / "UseCases"
    backup_now = (root / "BackupNow.psm1").read_text(encoding="utf-8")
    backup_scheduled = (root / "BackupScheduled.psm1").read_text(encoding="utf-8")
    rollback = (root / "RollbackApp.psm1").read_text(encoding="utf-8")
    restore = (root / "RestoreData.psm1").read_text(encoding="utf-8")
    rehearsal = (root / "Rehearsal.psm1").read_text(encoding="utf-8")

    assert "Export-BackupOpsMySqlDump" in backup_now
    assert "Backup-BackupOpsObjectBucket" in backup_now
    assert "Sync-BackupOpsBackupToTestServer" in backup_now
    assert "Send-BackupOpsNotification" in backup_now
    assert "-Context $resultContext" in backup_now
    assert "Set-BackupOpsNotificationContext" in backup_now
    assert "Invoke-BackupOpsNotificationCapture" in backup_now

    assert "Send-BackupOpsNotification" in backup_scheduled
    assert "-Context $resultContext" in backup_scheduled
    assert "Set-BackupOpsNotificationContext" in backup_scheduled
    assert "cleanup" in backup_scheduled

    assert "Save-BackupOpsRuntimeEnvBackup" in rollback
    assert "Set-BackupOpsImageTag" in rollback
    assert "Restart-BackupOpsFrontendBackend" in rollback
    assert "Test-BackupOpsFrontendBackendHealth" in rollback
    assert "Send-BackupOpsNotification" in rollback
    assert "-Context $resultContext" in rollback
    assert "Set-BackupOpsNotificationContext" in rollback
    assert "Invoke-BackupOpsNotificationCapture" in rollback

    assert "Stop-BackupOpsFrontendBackend" in restore
    assert "Import-BackupOpsMySqlDump" in restore
    assert "Restore-BackupOpsObjectBucket" in restore
    assert "Test-BackupOpsRestoreValidation" in restore
    assert "Send-BackupOpsNotification" in restore
    assert "-Context $resultContext" in restore
    assert "Set-BackupOpsNotificationContext" in restore
    assert "-Status 'started'" in restore
    assert "startNotification" in restore

    assert "Send-BackupOpsNotification" in rehearsal
    assert "-Context $resultContext" in rehearsal
    assert "Set-BackupOpsNotificationContext" in rehearsal
    assert "Invoke-BackupOpsNotificationCapture" in rehearsal
