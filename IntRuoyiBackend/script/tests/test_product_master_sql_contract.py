from pathlib import Path


def _sql_text() -> str:
    repo_root = Path(__file__).resolve().parents[2]
    return (repo_root / "sql" / "mysql" / "20260607_product_master_data.sql").read_text(
        encoding="utf-8",
    )


def test_product_master_sql_defines_core_tables_and_links() -> None:
    sql = _sql_text()

    required_tokens = [
        "CREATE TABLE IF NOT EXISTS `mdm_product`",
        "`product_code` varchar(64) NOT NULL",
        "`dcc_product_code` varchar(14) DEFAULT NULL",
        "CREATE TABLE IF NOT EXISTS `mdm_product_import_batch`",
        "CREATE TABLE IF NOT EXISTS `mdm_product_import_row`",
        "ADD COLUMN `product_master_id` bigint DEFAULT NULL",
        "dcc_controlled_file",
        "showroom_product",
    ]

    for token in required_tokens:
        assert token in sql


def test_product_master_sql_registers_permissions() -> None:
    sql = _sql_text()

    required_permissions = [
        "mdm:product:query",
        "mdm:product:create",
        "mdm:product:update",
        "mdm:product:delete",
        "mdm:product:import",
        "mdm:product:export",
        "mdm:product:map-showroom",
    ]

    for permission in required_permissions:
        assert permission in sql
