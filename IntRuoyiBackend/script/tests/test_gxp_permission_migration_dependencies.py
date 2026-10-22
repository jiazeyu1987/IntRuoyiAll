from pathlib import Path

from script.release.release_migration_policy_gate import run_migration_policy_gate


BACKEND_ROOT = Path(__file__).resolve().parents[2]


def test_permission_receipt_migration_has_a_deployable_dependency_closure() -> None:
    sql_root = BACKEND_ROOT / "sql" / "mysql"
    migration_ids = [
        "20260908_gxp_audit_trail_core",
        "20260924_gxp_audit_event_v2",
        "20260929_system_permission_command_receipt",
    ]

    report = run_migration_policy_gate(
        sql_root, sql_paths=[sql_root / f"{migration_id}.sql" for migration_id in migration_ids]
    )

    assert report["status"] == "passed"
    assert report["migrationCount"] == 3
    migrations = {entry["migrationId"]: entry for entry in report["migrations"]}
    assert set(migrations) == set(migration_ids)
    assert migrations[migration_ids[1]]["dependsOn"] == [migration_ids[0]]
    assert migrations[migration_ids[2]]["dependsOn"] == [migration_ids[1]]
