from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260829_mes_old_form_template_binding_switch.sql"
ELECTRONIC_BATCH_RECORD_EXPR = (
    "CONVERT(UNHEX('E794B5E5AD90E689B9E8AEB0E5BD95') USING utf8mb4) COLLATE utf8mb4_unicode_ci"
)


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing old form template binding switch migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_old_form_template_binding_switch_declares_contract_and_guards() -> None:
    sql = read_sql()
    upper = sql.upper()

    assert "release-migration: allowedEnvironments=test,backup,prod" in sql
    assert "dependsOn=20260722_mes_route_form_center_runtime_columns,20260829_mes_form_center_unified_import_menu" in sql
    assert "type=data" in sql
    assert "riskLevel=high" in sql
    assert "CREATE PROCEDURE `migrate_mes_old_form_template_bindings_to_form_center`()" in sql
    assert "CALL `migrate_mes_old_form_template_bindings_to_form_center`();" in sql
    assert "DROP PROCEDURE IF EXISTS `migrate_mes_old_form_template_bindings_to_form_center`;" in sql
    assert "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;" in sql
    assert "SET SESSION group_concat_max_len = 16777216;" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql

    for destructive in ("DROP TABLE", "TRUNCATE TABLE", "DELETE FROM"):
        assert destructive not in upper

    for table in (
        "bpm_form_template_version",
        "mes_pro_route_flow_process_batch_record",
        "mes_pro_route_version",
        "mes_pro_batch_record_definition",
        "mes_pro_batch_record_version",
        "mes_pro_batch_record_report",
        "jimu_report_category",
        "jimu_report",
    ):
        assert f"TABLE_NAME = '{table}'" in sql
        assert f"SET MESSAGE_TEXT = '{table} is missing'" in sql


def test_old_form_template_binding_switch_uses_version_identity_not_name_guessing() -> None:
    sql = read_sql()

    assert "CONCAT('FORMTPL:', tv.`id`)" in sql
    assert "CONCAT('FORMTPL:', tv.`id`) COLLATE utf8mb4_unicode_ci AS `report_id`" in sql
    assert "LOWER(SHA2(FROM_BASE64(tv.`source_file_content`), 256)) COLLATE utf8mb4_unicode_ci" in sql
    assert "tv.`id` = rb.`last_published_template_version_id`" in sql
    assert "tv.`template_id` = rb.`form_template_id`" in sql
    assert "tv.`tenant_id` = rb.`tenant_id`" in sql
    assert ELECTRONIC_BATCH_RECORD_EXPR in sql
    assert "Source form template content is missing" in sql
    assert "Form template Jimu schema is invalid" in sql
    assert "Target FORM_TEMPLATE report id conflicts" in sql
    assert "Target FORM_TEMPLATE sample key conflicts" in sql


def test_old_form_template_binding_switch_creates_new_center_records() -> None:
    sql = read_sql()

    assert "INSERT IGNORE INTO `mes_pro_batch_record_definition`" in sql
    assert "`route_key`" in sql
    assert "INSERT IGNORE INTO `mes_pro_batch_record_version`" in sql
    assert "'APPROVED'" in sql
    assert "LOWER(SHA2(FROM_BASE64(tv.`source_file_content`), 256))" in sql
    assert "INSERT INTO `mes_pro_batch_record_report`" in sql
    assert "`report_id`" in sql
    assert "`batch_record_definition_id`" in sql
    assert "`batch_record_version_id`" in sql
    assert "`source_table_index`" in sql
    assert "CREATE TEMPORARY TABLE `tmp_mes_old_form_template_definition_versions` AS" in sql
    assert "CREATE TEMPORARY TABLE `tmp_mes_old_form_template_latest_version` AS" in sql
    assert "CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_form_items` AS" in sql
    assert "CREATE TEMPORARY TABLE `tmp_mes_old_form_template_snapshot_converted_reports` AS" in sql
    assert "CREATE TEMPORARY TABLE `tmp_mes_old_form_template_jimu_report_scope` AS" in sql
    assert "INSERT INTO `jimu_report`" in sql
    assert "JSON_UNQUOTE(JSON_EXTRACT(tv.`jimu_schema_json`, '$.sheetLayoutJson'))" in sql
    assert "Form template designer json is invalid" in sql
    assert "GROUP_CONCAT(CAST(`report_json` AS CHAR CHARACTER SET utf8mb4) ORDER BY `binding_ord` SEPARATOR ',')" in sql
    assert "GROUP_CONCAT(CAST(`config_json` AS CHAR CHARACTER SET utf8mb4) ORDER BY `config_ord` SEPARATOR ',')" in sql
    assert "JSON_SET(" in sql
    assert "'$.formBindings', JSON_ARRAY()" in sql
    assert "'$.batchRecordReports'," in sql


def test_old_form_template_binding_switch_preserves_legacy_template_trace_fields() -> None:
    sql = read_sql()

    assert "UPDATE `mes_pro_route_flow_process_batch_record` rb" in sql
    assert "rb.`batch_record_report_id` = m.`report_id`" in sql
    assert "rb.`batch_record_definition_id` = m.`definition_id`" in sql
    assert "rb.`batch_record_version_id` = m.`version_id`" in sql
    assert "rb.`form_binding_key` = NULL" not in sql
    assert "rb.`global_sync_key` = NULL" not in sql
    assert "rb.`form_template_id` = NULL" not in sql
    assert "rb.`last_published_template_version_id` = NULL" not in sql
    assert "rb.`candidate_source_type` = NULL" not in sql
    assert "rb.`record_category_snapshot_hash` = LOWER(SHA2(CONCAT(" in sql
    assert "rb.`slot_config_snapshot_hash` = LOWER(SHA2(CONCAT(" in sql


def test_old_form_template_binding_switch_marks_legacy_versions_bound() -> None:
    sql = read_sql()

    assert "UPDATE `bpm_form_template_version` tv" in sql
    assert "tv.`batch_record_report_id` = m.`report_id`" in sql
    assert "tv.`batch_record_binding_status` = 'BOUND'" in sql
    assert "tv.`batch_record_binding_error` = NULL" in sql
    assert "Old form template route bindings remain after switch" in sql
    assert "Old form template route snapshot bindings remain after switch" in sql
    assert "Route snapshot JSON is invalid and contains old form template binding" in sql
    assert "Migrated route bindings missing target report metadata" in sql
