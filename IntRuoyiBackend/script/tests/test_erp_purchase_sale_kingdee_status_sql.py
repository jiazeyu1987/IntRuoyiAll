from pathlib import Path


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260612_erp_purchase_sale_kingdee_status.sql"


def test_purchase_sale_kingdee_status_sql_adds_source_status_columns():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "erp_purchase_order" in sql
    assert "erp_sale_order" in sql
    assert "ADD COLUMN `kingdee_close_status` varchar(16) DEFAULT NULL" in sql
    assert "ADD COLUMN `kingdee_cancel_status` varchar(16) DEFAULT NULL" in sql
    assert "AFTER `remark`" in sql
    assert "AFTER `kingdee_close_status`" in sql


def test_purchase_sale_kingdee_status_sql_is_idempotent():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "information_schema.COLUMNS" in sql
    assert "COLUMN_NAME = 'kingdee_close_status'" in sql
    assert "COLUMN_NAME = 'kingdee_cancel_status'" in sql
    assert "PREPARE stmt FROM @erp_purchase_order_close_status_sql" in sql
    assert "PREPARE stmt FROM @erp_sale_order_cancel_status_sql" in sql
