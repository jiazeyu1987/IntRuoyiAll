from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql" / "mysql" / "20260903_mes_process_pool_device_selection_mode.sql"


def test_device_selection_mode_sql_has_release_migration_metadata():
    first_line = SQL.read_text(encoding="utf-8").splitlines()[0]
    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260810_mes_process_pool_device_parameter_select_options; "
        "type=schema; riskLevel=medium"
    )
