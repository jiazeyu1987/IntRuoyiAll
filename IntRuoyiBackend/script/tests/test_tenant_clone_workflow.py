from __future__ import annotations

import json

from tenant_clone_fixtures import (
    JOB_CODE,
    parse_json_stdout,
    run_tenant_clone,
    write_job_without_backup,
    write_target_counts,
)


def write_backup_index(path) -> None:
    path.write_text(
        json.dumps(
            {
                "jobCode": JOB_CODE,
                "tables": [{"table": "system_dept", "rows": 2, "file": "system_dept.jsonl"}],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )


def test_execute_requires_backup_path_when_target_has_rows_even_with_clear_confirmation(tmp_path) -> None:
    target_counts = write_target_counts(tmp_path / "target-counts.json", rows=3)

    completed = run_tenant_clone(
        "execute",
        "--job-code",
        JOB_CODE,
        "--target-counts",
        str(target_counts),
        "--confirm-clear-target",
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload["jobCode"] == JOB_CODE
    assert payload["phase"] == "BACKUP_VALIDATE"
    assert payload["errorCode"] == "TENANT_CLONE_BACKUP_FAILED"
    assert "backup" in payload["message"].lower()
    assert payload["targetExistingRows"] == 3


def test_execute_fails_fast_when_clone_write_path_is_not_implemented(tmp_path) -> None:
    target_counts = write_target_counts(tmp_path / "target-counts.json", rows=3)
    backup_dir = tmp_path / "backup"
    backup_dir.mkdir()
    backup_index = backup_dir / "backup-index.json"
    write_backup_index(backup_index)

    completed = run_tenant_clone(
        "execute",
        "--job-code",
        JOB_CODE,
        "--target-counts",
        str(target_counts),
        "--confirm-clear-target",
        "--backup-dir",
        str(backup_dir),
        "--backup-index",
        str(backup_index),
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload["jobCode"] == JOB_CODE
    assert payload["phase"] == "CLONE_WRITE"
    assert payload["errorCode"] == "TENANT_CLONE_WRITE_PATH_NOT_IMPLEMENTED"
    assert payload["targetExistingRows"] == 3
    assert payload["clearedRows"] == 0


def test_execute_does_not_clear_target_when_confirmation_is_missing(tmp_path) -> None:
    target_counts = write_target_counts(tmp_path / "target-counts.json", rows=2)

    completed = run_tenant_clone(
        "execute",
        "--job-code",
        JOB_CODE,
        "--target-counts",
        str(target_counts),
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload["phase"] == "CLEAR_TARGET_CONFIRMATION"
    assert payload["errorCode"] == "TENANT_CLONE_TARGET_NOT_EMPTY_CONFIRM_REQUIRED"
    assert payload["targetExistingRows"] == 2
    assert payload["clearedRows"] == 0


def test_rollback_fails_fast_when_backup_index_is_missing(tmp_path) -> None:
    job_state = write_job_without_backup(tmp_path / "job-state.json")

    completed = run_tenant_clone(
        "rollback",
        "--job-code",
        JOB_CODE,
        "--job-state",
        str(job_state),
        "--confirm-restore-target",
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload["jobCode"] == JOB_CODE
    assert payload["phase"] == "ROLLBACK_VALIDATE"
    assert payload["errorCode"] == "TENANT_CLONE_BACKUP_MISSING"
    assert payload["restoredRows"] == 0
    assert "backup" in payload["message"].lower()


def test_rollback_fails_fast_when_restore_path_is_not_implemented(tmp_path) -> None:
    backup_index = tmp_path / "backup-index.json"
    write_backup_index(backup_index)

    completed = run_tenant_clone(
        "rollback",
        "--job-code",
        JOB_CODE,
        "--backup-index",
        str(backup_index),
        "--confirm-restore-target",
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload["jobCode"] == JOB_CODE
    assert payload["phase"] == "RESTORE_TARGET"
    assert payload["errorCode"] == "TENANT_CLONE_RESTORE_PATH_NOT_IMPLEMENTED"
    assert payload["restoredRows"] == 0
