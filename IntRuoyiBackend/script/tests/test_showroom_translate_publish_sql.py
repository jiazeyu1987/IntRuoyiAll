from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_showroom_translate_publish_task_tables_exist_in_mysql_migration():
    sql_path = ROOT / "sql" / "mysql" / "20260626_showroom_product_translate_publish_batch_task.sql"
    assert sql_path.exists(), "missing MySQL migration for showroom product translate publish batch task"
    sql = sql_path.read_text(encoding="utf-8")
    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium\n"
    )
    assert "showroom_product_translate_publish_batch_task" in sql
    assert "showroom_product_translate_publish_batch_task_item" in sql
    assert "`tenant_id`" in sql
    assert "`status`" in sql
    assert "`current_product_id`" in sql
    assert "uk_showroom_translate_publish_batch_task_item" in sql
    assert "idx_showroom_translate_publish_task_status" in sql


def test_showroom_translate_publish_task_tables_exist_in_showroom_schema():
    sql_path = ROOT / "sql" / "showroom" / "20260519_showroom_v1_schema.sql"
    sql = sql_path.read_text(encoding="utf-8")
    assert "showroom_product_translate_publish_batch_task" in sql
    assert "showroom_product_translate_publish_batch_task_item" in sql
    assert "uk_showroom_translate_publish_batch_task_item" in sql
