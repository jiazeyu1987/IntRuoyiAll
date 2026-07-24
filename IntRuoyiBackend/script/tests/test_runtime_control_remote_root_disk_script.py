from pathlib import Path


def _script_text() -> str:
    script = Path(__file__).resolve().parents[1] / "deploy" / "manage-int-ruoyi-remote-root-disk.ps1"
    return script.read_text(encoding="utf-8")


def test_remote_root_disk_script_has_fixed_environment_hosts() -> None:
    text = _script_text()

    assert "test = '172.30.30.58'" in text
    assert "prod = '172.30.30.57'" in text
    assert "backup = '172.30.30.59'" in text
    assert "targetEnvironment=test/prod/backup" in text
    assert "target proof failed" in text


def test_remote_root_disk_cleanup_is_limited_to_temp_paths() -> None:
    text = _script_text()

    assert "$BackupTmpPath = '/opt/intruoyi/ops/backup/tmp'" in text
    assert "$SystemTmpPath = '/tmp'" in text
    assert "find \"$BACKUP_TMP\" -xdev -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +" in text
    assert "find \"$SYSTEM_TMP\" -xdev -mindepth 1 -maxdepth 1" in text
    assert "! -name 'systemd-private-*'" in text
    assert "ReleasePackage" not in text
    assert "/mnt/nas" not in text
    assert "fstab" not in text


def test_remote_root_disk_protects_prod_and_backup_cleanup() -> None:
    text = _script_text()

    assert "$Mode -eq 'cleanup'" in text
    assert "$TargetEnvironment -eq 'prod' -or $TargetEnvironment -eq 'backup'" in text
    assert "$ProdConfirmText -ne 'PROD'" in text
    assert "protected remote root cleanup requires ProdConfirmText=PROD" in text


def test_remote_root_disk_status_requires_runtime_ip_proof() -> None:
    text = _script_text()

    assert "hostname -I" in text
    assert "grep -qw \"$EXPECTED_IP\"" in text
    assert "status output does not prove $TargetEnvironment server $expectedHost" in text
    assert "status output mountPoint must be /" in text
