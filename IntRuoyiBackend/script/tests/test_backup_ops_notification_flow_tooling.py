from pathlib import Path


def _backup_root() -> Path:
    return Path(__file__).resolve().parents[1] / "backup-ops"


def _read_usecase(name: str) -> str:
    return (_backup_root() / "scripts" / "modules" / "UseCases" / name).read_text(encoding="utf-8")


def test_failure_paths_invoke_notification_for_all_core_usecases() -> None:
    cases = {
        "BackupNow.psm1": "backup-now",
        "BackupScheduled.psm1": "backup-scheduled",
        "RollbackApp.psm1": "rollback-app",
        "RestoreData.psm1": "restore-data",
        "Rehearsal.psm1": "rehearsal",
    }

    for filename, action in cases.items():
        text = _read_usecase(filename)
        assert "Invoke-BackupOpsNotificationCapture" in text
        assert f"-Action '{action}'" in text
        assert "Get-BackupOpsNotificationOutcomeMessage" in text


def test_restore_data_sends_start_and_finish_notifications() -> None:
    text = _read_usecase("RestoreData.psm1")

    total_notify_calls = text.count("Send-BackupOpsNotification") + text.count("Invoke-BackupOpsNotificationCapture")
    assert total_notify_calls >= 2
    assert "即将开始" in text
    assert text.index("即将开始") < text.index("New-BackupOpsPreRestoreSnapshot")


def test_backup_scheduled_sends_cleanup_notification_separately_from_final_notification() -> None:
    text = _read_usecase("BackupScheduled.psm1")

    total_notify_calls = text.count("Send-BackupOpsNotification") + text.count("Invoke-BackupOpsNotificationCapture")
    assert total_notify_calls >= 2
    assert "清理任务" in text or "清理结果" in text
