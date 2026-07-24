from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL = (REPO_ROOT / "sql/mysql/20260717_mes_edhr_filler_minimal_permissions.sql").read_text(
    encoding="utf-8"
)


def test_filler_permission_sql_declares_release_metadata() -> None:
    assert SQL.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260611_mes_edhr_work_task_flow; type=permission; riskLevel=low\n"
    )


def test_filler_permission_sql_is_superseded_by_dynamic_entitlement_policy() -> None:
    assert "superseded by 20260718_system_entitlement_management.sql" in SQL
    assert "MES_EDHR_FILLER_MINIMAL" in SQL
    assert "system_entitlement_policy" in SQL


def test_filler_permission_sql_does_not_mutate_static_roles_or_users() -> None:
    forbidden_static_mutations = {
        "system_user_role",
        "system_role_menu",
        "INSERT INTO system_role",
        "INSERT INTO `system_role`",
        "@EDHR_FILLER_ROLE_CODE",
    }
    normalized_sql = SQL.lower()
    for forbidden in forbidden_static_mutations:
        assert forbidden.lower() not in normalized_sql

    assert "INSERT INTO `system_menu`" not in SQL
    assert "UPDATE `system_menu`" not in SQL
