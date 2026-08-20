import hashlib
import json
import subprocess
from pathlib import Path

import pytest

from script.release.release_migration_policy_gate import (
    MigrationPolicyError,
    run_migration_policy_gate,
)


REPO_ROOT = Path(__file__).resolve().parents[2]


def write_sql(root: Path, name: str, body: str) -> Path:
    path = root / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(body, encoding="utf-8")
    return path


def write_frozen_registry(path: Path, entries: dict[str, str]) -> None:
    path.write_text(json.dumps({"migrations": entries}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def test_policy_gate_rejects_missing_required_metadata(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(sql_root, "20260613_missing_metadata.sql", "CREATE TABLE IF NOT EXISTS demo_table (id bigint);\n")

    with pytest.raises(MigrationPolicyError, match="missing release-migration metadata"):
        run_migration_policy_gate(sql_root)


def test_policy_gate_rejects_missing_dependency(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(
        sql_root,
        "20260613_child.sql",
        "-- release-migration: allowedEnvironments=test; dependsOn=20260613_parent; type=schema; riskLevel=medium\n"
        "CREATE TABLE IF NOT EXISTS child_table (id bigint);\n",
    )

    with pytest.raises(MigrationPolicyError, match="dependsOn missing migration"):
        run_migration_policy_gate(sql_root)


def test_policy_gate_rejects_dependency_environment_superset(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(
        sql_root,
        "20260613_parent.sql",
        "-- release-migration: allowedEnvironments=test; dependsOn=; type=schema; riskLevel=medium\n"
        "CREATE TABLE IF NOT EXISTS parent_table (id bigint);\n",
    )
    write_sql(
        sql_root,
        "20260613_child.sql",
        "-- release-migration: allowedEnvironments=test,backup; dependsOn=20260613_parent; type=schema; riskLevel=medium\n"
        "CREATE TABLE IF NOT EXISTS child_table (id bigint);\n",
    )

    with pytest.raises(MigrationPolicyError, match="allowedEnvironments must be a subset"):
        run_migration_policy_gate(sql_root)


def test_policy_gate_rejects_executable_dependency_on_evidence_only(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(
        sql_root,
        "20260613_preflight.sql",
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=preflight; riskLevel=medium\n"
        "SELECT 1;\n",
    )
    write_sql(
        sql_root,
        "20260613_schema.sql",
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260613_preflight; type=schema; riskLevel=medium\n"
        "CREATE TABLE IF NOT EXISTS child_table (id bigint);\n",
    )

    with pytest.raises(MigrationPolicyError, match="executable migration cannot depend on evidence-only migration"):
        run_migration_policy_gate(sql_root)


def test_policy_gate_rejects_frozen_checksum_change(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    sql_path = write_sql(
        sql_root,
        "20260613_frozen.sql",
        "-- release-migration: allowedEnvironments=test,backup; dependsOn=; type=schema; riskLevel=low\n"
        "CREATE TABLE IF NOT EXISTS frozen_table (id bigint);\n",
    )
    registry = tmp_path / "frozen-migrations.json"
    write_frozen_registry(registry, {"20260613_frozen": "0" * 64})

    with pytest.raises(MigrationPolicyError, match="checksum frozen violation"):
        run_migration_policy_gate(sql_root, frozen_registry_path=registry)

    write_frozen_registry(registry, {"20260613_frozen": hashlib.sha256(sql_path.read_bytes()).hexdigest()})
    report = run_migration_policy_gate(sql_root, frozen_registry_path=registry)
    assert report["status"] == "passed"
    assert report["migrationCount"] == 1


def test_policy_gate_cli_returns_nonzero_on_failure(tmp_path: Path) -> None:
    sql_root = tmp_path / "sql" / "mysql"
    sql_root.mkdir(parents=True)
    write_sql(sql_root, "20260613_missing_metadata.sql", "select 1;\n")

    result = subprocess.run(
        [
            "python",
            "-X",
            "utf8",
            str(REPO_ROOT / "script" / "release" / "run-release-migration-policy-gate.py"),
            "--sql-root",
            str(sql_root),
        ],
        cwd=REPO_ROOT,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )

    assert result.returncode == 1
    assert "missing release-migration metadata" in result.stderr


def test_build_release_invokes_migration_policy_gate_before_manifest_generation() -> None:
    script = (REPO_ROOT / "script" / "deploy" / "publish-int-ruoyi.ps1").read_text(encoding="utf-8")
    assert "function Invoke-ReleaseMigrationPolicyGate" in script
    assert "run-release-migration-policy-gate.py" in script
    assert "Migration policy gate failed; build-release is blocked before package manifest generation." in script
    assert script.index("Invoke-ReleaseMigrationPolicyGate") < script.index("$requiredSqlEntries = New-ReleaseRequiredSqlManifestEntries")


def test_dcc_browser_performance_migration_depends_on_base_migration_id() -> None:
    report = run_migration_policy_gate(REPO_ROOT / "sql" / "mysql")
    migrations = {str(entry["migrationId"]): entry for entry in report["migrations"]}

    assert "20260513_dcc_base_schema" in migrations
    assert migrations["20260617_dcc_browser_performance_indexes"]["dependsOn"] == [
        "20260513_dcc_base_schema"
    ]
