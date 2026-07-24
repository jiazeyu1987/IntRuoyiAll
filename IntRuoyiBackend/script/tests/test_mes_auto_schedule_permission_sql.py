from pathlib import Path


SQL_PATH = Path("sql/mysql/20260624_mes_auto_schedule_permission_split.sql")


def test_auto_schedule_permission_split_release_metadata_uses_migration_id_stem():
    first_line = SQL_PATH.read_text(encoding="utf-8").splitlines()[0]

    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260610_mes_scheduler_workbench_p7; type=data; riskLevel=medium"
    )


def test_auto_schedule_permission_split_sql_is_idempotent_and_scoped():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "mes:pro-auto-schedule:preview" in sql
    assert "mes:pro-auto-schedule:apply" in sql
    assert "mes:pro-auto-schedule:replan" in sql
    assert "900180" in sql
    assert "900181" in sql
    assert "900182" in sql
    assert "NOT EXISTS" in sql
    assert "system_role_menu" in sql
    assert "111 AS `role_id`, 122 AS `tenant_id`" in sql
    assert "1 AS `role_id`, 1 AS `tenant_id`" in sql
    assert "DELETE FROM" not in sql.upper()
    assert "TRUNCATE" not in sql.upper()
    assert "DROP TABLE" not in sql.upper()


def test_auto_schedule_permission_split_sql_syncs_tenant_package_menu_ids():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "system_tenant_package" in sql
    assert "JSON_VALID(`package`.`menu_ids`)" in sql
    assert "JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900120' AS JSON), '$')" in sql
    assert "900180" in sql
    assert "900181" in sql
    assert "900182" in sql
    assert "JSON_ARRAYAGG(`menu_id`) OVER" in sql
    assert "tmp_mes_auto_schedule_package_menu_ids" in sql
