from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql/mysql/20260711_mes_batch_record_cell_link_rule.sql"


def test_batch_record_cell_link_rule_table_contract() -> None:
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_cell_link_rule`" in sql
    assert "`scope_type` varchar(32) NOT NULL" in sql
    assert "`scope_id` bigint NOT NULL" in sql
    assert "`batch_record_definition_id` bigint DEFAULT NULL" in sql
    assert "`batch_record_version_id` bigint DEFAULT NULL" in sql
    assert "`uk_mes_batch_record_cell_link_target`" in sql
    assert "`uk_mes_batch_record_cell_link_pair`" in sql


def test_batch_record_cell_link_rule_permission_tokens() -> None:
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    assert "mes:pro-batch-record-cell-link:query" in sql
    assert "mes:pro-batch-record-cell-link:update" in sql
    assert "批记录单元格链接查询" in sql
    assert "批记录单元格链接维护" in sql
