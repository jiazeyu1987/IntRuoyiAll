import hashlib
from pathlib import Path

import pytest

from script.release.release_migration_manifest import (
    MigrationManifestError,
    build_migration_manifest,
)


REPO_ROOT = Path(__file__).resolve().parents[2]


def write_sql(root: Path, name: str, body: str) -> Path:
    path = root / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(body, encoding="utf-8")
    return path


def test_migration_manifest_contains_structured_sql_metadata(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(
        sql_root,
        "20260612_runtime_nightly_release_job.sql",
        "CREATE TABLE IF NOT EXISTS infra_runtime_release_job (id bigint PRIMARY KEY);\n",
    )
    sql_path = write_sql(
        sql_root,
        "20260613_runtime_control_release_package_config.sql",
        "-- release-migration: allowedEnvironments=test,backup; dependsOn=20260612_runtime_nightly_release_job; type=schema; riskLevel=medium\n"
        "CREATE TABLE IF NOT EXISTS infra_runtime_release_package_config (id bigint PRIMARY KEY);\n",
    )

    entries = build_migration_manifest(sql_root)

    assert entries[1:] == [
        {
            "migrationId": "20260613_runtime_control_release_package_config",
            "file": "sql/mysql/20260613_runtime_control_release_package_config.sql",
            "sha256": hashlib.sha256(sql_path.read_bytes()).hexdigest(),
            "type": "schema",
            "allowedEnvironments": ["test", "backup"],
            "dependsOn": ["20260612_runtime_nightly_release_job"],
            "riskLevel": "medium",
            "requiresTargetPreflight": False,
            "applyOrder": None,
            "sessionProfile": "",
            "approvedHook": "",
            "resultAssertion": "",
        }
    ]


def test_migration_manifest_contains_requires_target_preflight_metadata(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(
        sql_root,
        "20260613_target_bound_menu.sql",
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=high; requiresTargetPreflight=true\n"
        "SELECT 1;\n",
    )

    entries = build_migration_manifest(sql_root)

    assert entries[0]["requiresTargetPreflight"] is True


def test_migration_manifest_rejects_invalid_requires_target_preflight_metadata(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(
        sql_root,
        "20260613_invalid_target_preflight.sql",
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=high; requiresTargetPreflight=yes\n"
        "SELECT 1;\n",
    )

    with pytest.raises(MigrationManifestError, match="invalid requiresTargetPreflight"):
        build_migration_manifest(sql_root)


def test_migration_manifest_excludes_target_preflight_contract_files(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(
        sql_root,
        "20260613_release_schema.sql",
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium\n"
        "SELECT 1;\n",
    )
    write_sql(
        sql_root / "target-preflight",
        "20260613_release_schema.preflight.sql",
        "-- release-target-preflight: migrationId=20260613_release_schema; allowedEnvironments=test,backup,prod\n"
        "SELECT 'TARGET_PREFLIGHT_PASS:20260613_release_schema';\n",
    )

    entries = build_migration_manifest(sql_root)

    assert [entry["migrationId"] for entry in entries] == ["20260613_release_schema"]


def test_migration_manifest_rejects_duplicate_migration_id(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(sql_root, "20260613_duplicate.sql", "select 1;\n")
    write_sql(sql_root, "subdir/20260613_duplicate.sql", "select 2;\n")

    with pytest.raises(MigrationManifestError, match="duplicate migrationId"):
        build_migration_manifest(sql_root)


def test_migration_manifest_rejects_missing_dependency(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(
        sql_root,
        "20260613_child.sql",
        "-- release-migration: dependsOn=20260613_parent\n"
        "select 1;\n",
    )

    with pytest.raises(MigrationManifestError, match="dependsOn missing migration"):
        build_migration_manifest(sql_root)


@pytest.mark.parametrize("migration_type", ["preflight", "backfill", "postflight", "rollback-dry-run"])
def test_migration_manifest_accepts_gated_migration_phase_types(
    tmp_path: Path,
    migration_type: str,
) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(
        sql_root,
        f"20260814_c015_{migration_type}.sql",
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        f"dependsOn=; type={migration_type}; riskLevel=high\n"
        "SELECT 1;\n",
    )

    entries = build_migration_manifest(sql_root)

    assert entries[0]["type"] == migration_type


def test_publish_release_manifest_writes_structured_migration_fields() -> None:
    script = (REPO_ROOT / "script" / "deploy" / "publish-int-ruoyi.ps1").read_text(encoding="utf-8")

    assert "migrationId = [System.IO.Path]::GetFileNameWithoutExtension($fileName)" in script
    assert "type = 'schema'" in script
    assert "allowedEnvironments = @($entry.Environments)" in script
    assert "dependsOn = @()" in script
    assert "riskLevel = 'medium'" in script
    assert "migrationId = [string]$requiredSql.migrationId" in script
    assert "allowedEnvironments = @($requiredSql.allowedEnvironments)" in script
    assert "dependsOn = @($requiredSql.dependsOn)" in script
