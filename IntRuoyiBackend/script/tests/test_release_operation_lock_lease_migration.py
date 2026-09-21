from __future__ import annotations

import subprocess
import uuid
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql/mysql/20260921_infra_release_operation_lock_lease_fencing.sql"
LEASE_COLUMNS = (
    "executor_host",
    "executor_pid",
    "executor_identity",
    "lease_token",
    "heartbeat_at",
    "write_fence",
)


def _mysql(sql: str, database: str | None = None) -> subprocess.CompletedProcess[str]:
    command = [
        "docker", "exec", "-i", "int-ruoyi-mysql", "sh", "-lc",
        "MYSQL_PWD=\"$MYSQL_ROOT_PASSWORD\" exec mysql -uroot "
        "--default-character-set=utf8mb4 --batch --raw --skip-column-names"
        + (f" {database}" if database else ""),
    ]
    return subprocess.run(command, input=sql, text=True, capture_output=True, check=False)


def _database() -> str:
    return f"release_lock_lease_{uuid.uuid4().hex[:12]}"


def _base_schema() -> str:
    return """
CREATE TABLE infra_release_operation_lock (
  id bigint PRIMARY KEY AUTO_INCREMENT,
  target_environment varchar(32) NOT NULL,
  operation_id varchar(128) NOT NULL,
  release_tag varchar(128) NOT NULL,
  status varchar(32) NOT NULL,
  started_at datetime NOT NULL,
  finished_at datetime NULL,
  error_message text NULL,
  creator varchar(64) DEFAULT '',
  create_time datetime NOT NULL,
  updater varchar(64) DEFAULT '',
  update_time datetime NOT NULL,
  deleted bit(1) NOT NULL DEFAULT b'0',
  tenant_id bigint NOT NULL DEFAULT 0,
  UNIQUE KEY uk_release_lock_env (target_environment)
) ENGINE=InnoDB;
"""


def test_lock_lease_migration_declares_versioned_schema_contract() -> None:
    text = MIGRATION.read_text(encoding="utf-8")
    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260613_infra_release_migration_state; type=schema; riskLevel=high"
    )
    for column in LEASE_COLUMNS:
        assert f"`{column}`" in text
    assert "partial lease schema detected" in text


def test_lock_lease_migration_is_idempotent_in_real_mysql() -> None:
    database = _database()
    try:
        assert _mysql(f"CREATE DATABASE `{database}` CHARACTER SET utf8mb4;").returncode == 0
        assert _mysql(_base_schema(), database).returncode == 0
        sql = MIGRATION.read_text(encoding="utf-8")
        first = _mysql(sql, database)
        second = _mysql(sql, database)
        assert first.returncode == 0, first.stderr
        assert second.returncode == 0, second.stderr
        result = _mysql(
            "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY COLUMN_NAME) "
            "FROM information_schema.COLUMNS "
            "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='infra_release_operation_lock' "
            "AND COLUMN_NAME IN ('executor_host','executor_pid','executor_identity','lease_token','heartbeat_at','write_fence');",
            database,
        )
        assert result.returncode == 0, result.stderr
        assert result.stdout.strip().split(",") == sorted(LEASE_COLUMNS)
    finally:
        _mysql(f"DROP DATABASE IF EXISTS `{database}`;")


def test_lock_lease_migration_rejects_partial_schema_without_expanding_it() -> None:
    database = _database()
    try:
        assert _mysql(f"CREATE DATABASE `{database}` CHARACTER SET utf8mb4;").returncode == 0
        assert _mysql(_base_schema(), database).returncode == 0
        assert _mysql(
            "ALTER TABLE infra_release_operation_lock ADD COLUMN executor_host varchar(255) NULL;",
            database,
        ).returncode == 0
        result = _mysql(MIGRATION.read_text(encoding="utf-8"), database)
        assert result.returncode != 0
        assert "partial lease schema detected" in result.stderr
        count = _mysql(
            "SELECT COUNT(*) FROM information_schema.COLUMNS "
            "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='infra_release_operation_lock' "
            "AND COLUMN_NAME IN ('executor_host','executor_pid','executor_identity','lease_token','heartbeat_at','write_fence');",
            database,
        )
        assert count.returncode == 0, count.stderr
        assert count.stdout.strip() == "1"
    finally:
        _mysql(f"DROP DATABASE IF EXISTS `{database}`;")
