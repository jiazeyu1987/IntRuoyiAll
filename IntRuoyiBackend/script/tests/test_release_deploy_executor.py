import pytest

from script.release.release_deploy_executor import (
    ReleaseDeployExecutionError,
    execute_preflight_apply_migrations,
)


def item(migration_id: str, action: str = "APPLY", file_name: str | None = None) -> dict:
    return {
        "migrationId": migration_id,
        "action": action,
        "file": file_name or f"{migration_id}.sql",
        "sha256": f"sha-{migration_id}",
    }


def test_executor_runs_only_apply_items_and_records_statuses() -> None:
    executed: list[str] = []
    states: list[tuple[str, str]] = []

    summary = execute_preflight_apply_migrations(
        {"status": "passed", "items": [item("already", "SKIP_ALREADY_APPLIED"), item("next")]},
        execute_sql=lambda entry: executed.append(entry["migrationId"]),
        record_state=lambda entry, status, error_message=None: states.append((entry["migrationId"], status)),
    )

    assert executed == ["next"]
    assert states == [
        ("already", "SKIPPED_ALREADY_APPLIED"),
        ("next", "RUNNING"),
        ("next", "APPLIED"),
    ]
    assert summary["applied"] == ["next"]
    assert summary["skipped"] == ["already"]


def test_executor_accepts_environment_skips_without_recording_state() -> None:
    executed: list[str] = []
    states: list[tuple[str, str]] = []

    summary = execute_preflight_apply_migrations(
        {"status": "passed", "items": [item("test_only", "SKIP_ENV_NOT_ALLOWED"), item("next")]},
        execute_sql=lambda entry: executed.append(entry["migrationId"]),
        record_state=lambda entry, status, error_message=None: states.append((entry["migrationId"], status)),
    )

    assert executed == ["next"]
    assert states == [
        ("next", "RUNNING"),
        ("next", "APPLIED"),
    ]
    assert summary["applied"] == ["next"]
    assert summary["skipped"] == ["test_only"]


def test_executor_blocks_plan_with_blocked_item_before_sql_execution() -> None:
    executed: list[str] = []
    states: list[tuple[str, str]] = []

    with pytest.raises(ReleaseDeployExecutionError, match="blocked preflight item"):
        execute_preflight_apply_migrations(
            {"status": "blocked", "items": [item("bad", "BLOCKED_CHECKSUM_MISMATCH"), item("next")]},
            execute_sql=lambda entry: executed.append(entry["migrationId"]),
            record_state=lambda entry, status, error_message=None: states.append((entry["migrationId"], status)),
        )

    assert executed == []
    assert states == []


def test_executor_records_failed_and_stops_subsequent_items() -> None:
    executed: list[str] = []
    states: list[tuple[str, str, str | None]] = []

    def execute_sql(entry: dict) -> None:
        executed.append(entry["migrationId"])
        raise RuntimeError("mysql failed")

    with pytest.raises(ReleaseDeployExecutionError, match="mysql failed"):
        execute_preflight_apply_migrations(
            {"status": "passed", "items": [item("first"), item("second")]},
            execute_sql=execute_sql,
            record_state=lambda entry, status, error_message=None: states.append(
                (entry["migrationId"], status, error_message)
            ),
        )

    assert executed == ["first"]
    assert states == [
        ("first", "RUNNING", None),
        ("first", "FAILED", "mysql failed"),
    ]


def test_executor_requires_passed_preflight_plan() -> None:
    with pytest.raises(ReleaseDeployExecutionError, match="preflight plan status must be passed"):
        execute_preflight_apply_migrations(
            {"status": "unknown", "items": [item("next")]},
            execute_sql=lambda entry: None,
            record_state=lambda entry, status, error_message=None: None,
        )
