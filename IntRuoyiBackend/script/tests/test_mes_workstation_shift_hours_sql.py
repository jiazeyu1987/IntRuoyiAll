from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_workstation_shift_hours_mysql_migration_is_present() -> None:
    migration = (
        REPO_ROOT / "sql" / "mysql" / "20260609_mes_md_workstation_shift_hours.sql"
    ).read_text(encoding="utf-8")

    assert "ALTER TABLE `mes_md_workstation`" in migration
    assert "ADD COLUMN `shift_hours` decimal(10,2) NULL COMMENT '班次小时数'" in migration


def test_workstation_shift_hours_is_present_in_consolidated_sql_and_h2_schema() -> None:
    consolidated = (REPO_ROOT / "sql" / "mysql" / "ruoyi-vue-pro.sql").read_text(
        encoding="utf-8"
    )
    h2_schema = (
        REPO_ROOT / "yudao-module-mes" / "src" / "test" / "resources" / "sql" / "create_tables.sql"
    ).read_text(encoding="utf-8")

    assert "column_name = 'shift_hours'" in consolidated
    assert "ADD COLUMN `shift_hours` decimal(10,2) NULL COMMENT ''班次小时数''" in consolidated
    assert '"shift_hours" decimal(10,2) DEFAULT NULL' in h2_schema
