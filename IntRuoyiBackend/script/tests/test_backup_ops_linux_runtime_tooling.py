from pathlib import Path


def _backup_root() -> Path:
    return Path(__file__).resolve().parents[1] / "backup-ops"


def test_linux_runtime_entry_and_launcher_exist() -> None:
    root = _backup_root()
    python_entry = root / "linux" / "backup_ops_linux.py"
    shell_launcher = root / "linux" / "backup-ops-linux.sh"

    assert python_entry.exists()
    assert shell_launcher.exists()


def test_linux_runtime_config_example_exists_with_linux_paths() -> None:
    text = (_backup_root() / "config" / "backup-ops.linux-local.example.json").read_text(encoding="utf-8")

    assert '"/opt/intruoyi/runtime"' in text
    assert '"/mnt/nas/Backup/BackupPackage"' in text
    assert '"/mnt/nas/int-ruoyi/backups"' not in text
    assert '"/opt/intruoyi/runtime/data/backup-ops/logs"' in text
    assert '"mode": "linux-local"' in text


def test_linux_runtime_entry_declares_backup_and_restore_modes_without_powershell_dependency() -> None:
    text = (_backup_root() / "linux" / "backup_ops_linux.py").read_text(encoding="utf-8")

    assert "backup-now" in text
    assert "restore-data" in text
    assert "subprocess" in text
    assert "docker exec" in text
    assert "PowerShell" not in text


def test_linux_runtime_projects_backup_now_and_restore_data_target_environment_explicitly() -> None:
    text = (_backup_root() / "linux" / "backup_ops_linux.py").read_text(encoding="utf-8")
    config_text = (_backup_root() / "config" / "backup-ops.linux-local.example.json").read_text(encoding="utf-8")

    assert "--target-environment" in text
    assert "--production-backup-confirm-text" in text
    assert "project_target_environment" in text
    assert "production_backup_confirm_text" in text
    assert "PROD-BACKUP-172.30.30.57" in text
    assert "Production backup confirmation is required" in text
    assert '{"test", "backup"}' in text
    assert '{"backup-now", "backup-scheduled", "rollback-app", "restore-data"}' in text
    assert '["servers", target_environment, "runtimeDir"]' in text
    assert '["servers", target_environment, "tmpRoot"]' in text
    assert 'projected["servers"]["production"]["host"] = target_host' in text
    assert 'projected["servers"]["production"]["appDir"] = target_runtime_dir' in text
    assert 'projected["servers"]["production"]["tmpRoot"] = target_tmp_root' in text
    assert "target-environment test/backup is only supported for backup-now, backup-scheduled, rollback-app and restore-data" in text
    assert "restore-data only supports --target-environment test or backup" in text
    assert '"tmpRoot": "/opt/intruoyi/runtime/data/backup-ops/tmp"' in config_text
    assert '"dccTenantId": 122' in config_text
    assert '"backup": {' in config_text
    assert '"host": "127.0.0.1"' in config_text


def test_linux_runtime_uses_explicit_minio_client_image() -> None:
    text = (_backup_root() / "linux" / "backup_ops_linux.py").read_text(encoding="utf-8")
    config_text = (_backup_root() / "config" / "backup-ops.linux-local.example.json").read_text(encoding="utf-8")

    assert '"minioClientImage": "quay.io/minio/mc:latest"' in config_text
    assert 'get_required(config, ["tools", "minioClientImage"], "tools.minioClientImage")' in text
    assert 'shlex.quote(minio_client_image)' in text
    assert ' minio/mc ' not in text
