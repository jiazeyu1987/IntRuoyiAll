from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260613_infra_release_migration_state.sql"


def normalized_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8").lower()


def test_release_migration_state_table_exists_with_required_columns() -> None:
    sql = normalized_sql()

    assert "create table if not exists `infra_release_migration`" in sql
    for column in (
        "`id` bigint",
        "`release_tag` varchar(128)",
        "`migration_id` varchar(191)",
        "`file_name` varchar(512)",
        "`sha256` varchar(64)",
        "`target_environment` varchar(32)",
        "`status` varchar(32)",
        "`started_at` datetime",
        "`finished_at` datetime",
        "`error_message` text",
        "`operation_id` varchar(128)",
    ):
        assert column in sql


def test_release_migration_state_table_has_environment_migration_unique_key() -> None:
    sql = normalized_sql()

    assert "unique key `uk_infra_release_migration_env_id` (`target_environment`, `migration_id`)" in sql
    assert "key `idx_infra_release_migration_operation` (`operation_id`)" in sql
    assert "key `idx_infra_release_migration_release_tag` (`release_tag`)" in sql


def test_release_operation_lock_table_is_unique_per_environment() -> None:
    sql = normalized_sql()

    assert "create table if not exists `infra_release_operation_lock`" in sql
    assert "`operation_id` varchar(128) not null comment '发布操作 id'" in sql
    assert "`status` varchar(32) not null comment '状态：running/applied/failed'" in sql
    assert "unique key `uk_infra_release_operation_lock_env` (`target_environment`)" in sql


def test_release_migration_state_status_values_are_documented() -> None:
    sql = normalized_sql()

    for status in ("RUNNING", "APPLIED", "SKIPPED_ALREADY_APPLIED", "FAILED"):
        assert status.lower() in sql
