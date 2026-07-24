from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql" / "mysql" / "20260704_showroom_product_target_market_text.sql"


def test_showroom_product_target_market_release_migration_is_publishable() -> None:
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=medium"
    )
    assert "ALTER TABLE `showroom_product_revision`" in sql
    assert "MODIFY COLUMN `target_market` text DEFAULT NULL" in sql
    assert "Rollback: ALTER TABLE showroom_product_revision MODIFY COLUMN target_market varchar(255) DEFAULT NULL" in sql
