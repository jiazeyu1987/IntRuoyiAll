from pathlib import Path


SQL_ROOT = Path(__file__).resolve().parents[2] / "sql" / "mysql"


def read_first_line(name: str) -> str:
    return (SQL_ROOT / name).read_text(encoding="utf-8").splitlines()[0]


def test_approval_center_role_visibility_has_release_metadata() -> None:
    assert read_first_line("20260630_approval_center_role_visibility.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=data; riskLevel=medium"
    )


def test_dcc_admin_full_config_managed_scope_has_release_metadata() -> None:
    assert read_first_line("20260630_dcc_admin_full_config_managed_scope.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=low"
    )


def test_mes_pro_work_order_erp_snapshot_fields_has_release_metadata() -> None:
    assert read_first_line("20260630_mes_pro_work_order_erp_snapshot_fields.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=low"
    )


def test_dcc_admin_full_config_menu_depends_on_uses_migration_ids() -> None:
    assert read_first_line("20260630_dcc_admin_full_config_menu.sql") == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema,20260515_dcc_governance_split_menu,"
        "20260529_dcc_audit_menu_permission; type=menu; riskLevel=low"
    )
