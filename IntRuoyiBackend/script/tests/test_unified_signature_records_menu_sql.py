from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260714_unified_signature_records_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"{SQL_PATH} must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def test_unified_signature_records_menu_updates_900411_and_soft_deletes_900412():
    sql = read_sql()

    assert "SET @unified_signature_records_menu_id := 900411;" in sql
    assert "SET @legacy_batch_signature_menu_id := 900412;" in sql
    assert "`name` = '签名记录'" in sql
    assert "`path` = 'signature-records'" in sql
    assert "`component_name` = 'SignatureGovernanceSignatureRecords'" in sql
    unified_update = sql.split("UPDATE `system_menu`\nSET `name` = '签名记录'", 1)[1]
    unified_update = unified_update.split("UPDATE `system_menu`\nSET `name` = '批记录签名记录'", 1)[0]
    assert "`always_show` = b'0'" in unified_update
    assert "`id` = @legacy_batch_signature_menu_id" in sql
    assert "`deleted` = b'1'" in sql
    assert "'批记录签名记录'" in sql
    assert "'dcc:controlled-file:signature:manage'" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Missing unified signature records menu 900411" in sql
    assert "Missing legacy batch signature menu 900412" in sql


def test_unified_signature_records_menu_preserves_permission_items_and_role_bindings():
    sql = read_sql()

    assert "'dcc:controlled-file:signature:manage'" in sql
    assert "'mes:pro-batch-record-execution:signature-query'" in sql
    assert "Missing DCC signature permission menu" in sql
    assert "Missing eDHR signature permission menu" in sql
    assert "`parent_id` = @unified_signature_records_menu_id" in sql
    assert "INSERT INTO `system_role_menu`" in sql
    assert "legacy_role_menu" in sql
    assert "src.`menu_id` IN (@unified_signature_records_menu_id, @legacy_batch_signature_menu_id)" in sql
    assert "existing.`menu_id` = @unified_signature_records_menu_id" in sql
    assert "UPDATE `system_role_menu`" in sql
    assert "`menu_id` = @legacy_batch_signature_menu_id" in sql


def test_unified_signature_records_menu_does_not_resurrect_legacy_batch_route():
    sql = read_sql()

    permission_update = sql.split("UPDATE `system_menu`\nSET `parent_id` = @unified_signature_records_menu_id", 1)[1]
    permission_update = permission_update.split("INSERT INTO `system_role_menu`", 1)[0]

    assert "`permission` IN (" in permission_update
    assert "`id` NOT IN (@unified_signature_records_menu_id, @legacy_batch_signature_menu_id)" in permission_update
    assert "`deleted` = b'0'" in permission_update


def test_unified_signature_records_permission_items_stay_hidden_leaf_children():
    sql = read_sql()

    permission_update = sql.split("UPDATE `system_menu`\nSET `parent_id` = @unified_signature_records_menu_id", 1)[1]
    permission_update = permission_update.split("INSERT INTO `system_role_menu`", 1)[0]

    assert "`visible` = b'0'" in permission_update
    assert "`always_show` = b'0'" in permission_update
    assert "`deleted` = b'0'" in permission_update
    assert "`permission` IN (" in permission_update
    assert "`type` = 3" in permission_update
    assert "`name` IN ('文件签名记录', '批记录签名记录')" not in permission_update


def test_unified_signature_records_menu_does_not_create_unconfirmed_sources():
    sql = read_sql()

    assert "排产" not in sql
    assert "文控" not in sql
    assert "SCHEDULING" not in sql
    assert "DOCUMENT_CONTROL" not in sql
    assert "mock" not in sql.lower()
    assert "fallback" not in sql.lower()
