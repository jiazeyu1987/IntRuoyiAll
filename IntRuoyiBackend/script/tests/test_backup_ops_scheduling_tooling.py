from pathlib import Path


def _backup_root() -> Path:
    return Path(__file__).resolve().parents[1] / "backup-ops"


def test_schedule_registration_script_exists_and_registers_daily_and_weekly_tasks() -> None:
    script_path = _backup_root() / "actions" / "Register-BackupOpsScheduledTasks.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert script_path.exists()
    assert "Register-ScheduledTask" in text
    assert "backup-scheduled" in text
    assert "rehearsal" in text
    assert "IntRuoyi Backup Scheduled" in text
    assert "IntRuoyi Rehearsal" in text
    assert "-OperatorName" in text
    assert "'scheduler'" in text


def test_schedule_registration_script_supports_plan_only_preview() -> None:
    text = (_backup_root() / "actions" / "Register-BackupOpsScheduledTasks.ps1").read_text(encoding="utf-8")

    assert "PlanOnly" in text
    assert "ConvertTo-Json" in text or "Format-Table" in text
