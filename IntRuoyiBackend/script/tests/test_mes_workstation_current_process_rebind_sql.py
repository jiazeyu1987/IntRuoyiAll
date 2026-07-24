from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260717_mes_workstation_current_process_rebind.sql"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_current_process_rebind_sql_has_release_contract_and_backup() -> None:
    text = read_sql()

    assert text.startswith("-- release-migration:")
    assert "allowedEnvironments=test,backup,prod" in text
    assert "type=data" in text
    assert "riskLevel=medium" in text
    assert "CREATE TABLE IF NOT EXISTS `mes_md_workstation_process_rebind_20260717`" in text
    assert "INSERT IGNORE INTO `mes_md_workstation_process_rebind_20260717`" in text
    assert "`old_process_id`" in text
    assert "`new_process_id`" in text


def test_current_process_rebind_sql_only_updates_uniquely_mapped_current_processes() -> None:
    text = read_sql()

    assert "JOIN `mes_pro_process` old_process" in text
    assert "old_process.`deleted` = b'1'" in text
    assert "current_process.`deleted` = b'0'" in text
    assert "HAVING COUNT(DISTINCT current_process.`id`) = 1" in text
    assert "ambiguous_active_process_code_count" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "INSERT INTO `mes_md_workstation`" not in text


def test_current_process_rebind_sql_binds_route_process_only_after_master_rebind() -> None:
    text = read_sql()

    assert "UPDATE `mes_md_workstation` workstation" in text
    assert "SET workstation.`process_id` = rebind.`new_process_id`" in text
    assert "UPDATE `mes_pro_route_process` route_process" in text
    assert "SET route_process.`workstation_id` = unique_workstation.`workstation_id`" in text
    assert "route_process.`workstation_id` IS NULL" in text
    assert "COUNT(DISTINCT workstation.`id`) = 1" in text
