from pathlib import Path


def test_dcc_nas_transfer_task_sql_defines_required_tables() -> None:
    sql_path = (
        Path(__file__).resolve().parents[2]
        / "sql"
        / "mysql"
        / "20260523_dcc_nas_transfer_task.sql"
    )
    sql_text = sql_path.read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `dcc_controlled_file_nas_transfer_task`" in sql_text
    assert "CREATE TABLE IF NOT EXISTS `dcc_controlled_file_nas_transfer_task_item`" in sql_text
    assert "`selected_nas_paths_json` longtext NOT NULL" in sql_text
    assert "`product_master_id` bigint" in sql_text
    assert "`parent_item_id` bigint DEFAULT NULL" in sql_text
    assert "`idx_dcc_nas_transfer_task_status` (`status`, `next_check_at`)" in sql_text
    assert "`idx_dcc_nas_transfer_task_item_status` (`task_id`, `status`, `id`)" in sql_text


def test_dcc_nas_transfer_task_product_master_can_be_optional() -> None:
    sql_path = (
        Path(__file__).resolve().parents[2]
        / "sql"
        / "mysql"
        / "20260614_dcc_optional_product_binding.sql"
    )
    sql_text = sql_path.read_text(encoding="utf-8")

    assert "COLUMN_NAME = 'product_master_id'" in sql_text
    assert "IS_NULLABLE = 'NO'" in sql_text
    assert (
        "ALTER TABLE `dcc_controlled_file_nas_transfer_task` MODIFY COLUMN `product_master_id` "
        "bigint DEFAULT NULL COMMENT ''MDM product selected for DCC submit''"
    ) in sql_text
    assert "product_master_id already nullable" in sql_text
