from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_dcc_directory_access_rule_manual_binding_migration_is_idempotent() -> None:
    migration = (
        REPO_ROOT / "sql/mysql/20260626_dcc_access_rule_manual_binding.sql"
    ).read_text(encoding="utf-8")

    assert migration.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260515_dcc_runtime_schema_repair; type=schema; riskLevel=medium\n"
    )

    upper = migration.upper()
    assert "DROP TABLE" not in upper
    assert "TRUNCATE TABLE" not in upper
    assert "DELETE FROM" not in upper
    assert "information_schema.columns" in migration
    assert "dcc_file_directory" in migration
    assert "access_rule_manually_bound" in migration
    assert "ALTER TABLE `dcc_file_directory` ADD COLUMN `access_rule_manually_bound`" in migration
    assert "UPDATE `dcc_file_directory`" not in migration
