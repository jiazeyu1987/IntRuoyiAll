from pathlib import Path


SQL_PATH = (
    Path(__file__).resolve().parents[2]
    / "sql"
    / "mysql"
    / "20260624_mes_feedback_surplus_pool.sql"
)


def test_feedback_surplus_pool_sql_creates_pool_and_allocation_tables():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260610_mes_feedback_import_attribution_p6; type=schema; riskLevel=medium\n"
    )
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_feedback_surplus_pool`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_feedback_surplus_allocation`" in sql
    assert "`source_type` varchar(64) NOT NULL" in sql
    assert "`total_quantity` decimal(24,6) NOT NULL" in sql
    assert "`available_quantity` decimal(24,6) NOT NULL" in sql
    assert "`target_type` varchar(64) NOT NULL" in sql
    assert "`target_order_label` varchar(128) NOT NULL" in sql
    assert "`target_product_label` varchar(128) NOT NULL" in sql


def test_feedback_surplus_pool_sql_records_supported_source_and_target_labels():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "CURRENT_ORDER_OVERPRODUCE" in sql
    assert "EXTERNAL_OTHER_ORDER" in sql
    assert "其他订单" in sql
    assert "其他产品" in sql
    assert "idx_mes_pro_feedback_surplus_pool_import_record" in sql
    assert "idx_mes_pro_feedback_surplus_allocation_pool" in sql


if __name__ == "__main__":
    test_feedback_surplus_pool_sql_creates_pool_and_allocation_tables()
    test_feedback_surplus_pool_sql_records_supported_source_and_target_labels()
    print("PASS: MES feedback surplus pool SQL contract")
