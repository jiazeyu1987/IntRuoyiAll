from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = REPO_ROOT / "sql" / "mysql" / "20260724_system_codex_test_management.sql"


def migration_text() -> str:
    return MIGRATION.read_text(encoding="utf-8").replace("`", "")


def test_codex_test_management_migration_exists_with_schema_and_permission_seed() -> None:
    sql = migration_text().lower()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedenvironments=test,backup,prod; "
        "dependson=20260721_admin_full_scope_role_standardization; type=schema; risklevel=medium"
    )
    for table in (
        "system_codex_test_case",
        "system_codex_test_checkpoint",
        "system_codex_test_execution",
        "system_codex_test_execution_case",
        "system_codex_test_checkpoint_result",
        "system_codex_test_artifact",
        "system_codex_test_runner_session",
    ):
        assert f"create table if not exists {table}" in sql

    for permission in (
        "system:codex-test:query",
        "system:codex-test:create",
        "system:codex-test:update",
        "system:codex-test:delete",
        "system:codex-test:execute",
        "system:codex-test:cancel",
        "system:codex-test:artifact",
    ):
        assert permission in sql

    assert "codex_test_admin" in sql
    assert "username = 'admin'" in sql
    assert "json_valid(package.menu_ids)" in sql
    assert "alter table system_tenant_package modify column menu_ids longtext" in sql


def test_codex_test_management_migration_uses_stable_business_keys_for_menu_and_role() -> None:
    sql = migration_text().lower()

    assert "permission = 'system:codex-test:query'" in sql
    assert "where code = 'codex_test_admin'" in sql
    assert "set v_codex_test_admin_role_id = last_insert_id()" in sql
    assert "role id" not in sql or "is already occupied" not in sql
