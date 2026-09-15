from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_ROOT = ROOT / "sql" / "mysql"
TARGET_PREFLIGHT_ROOT = SQL_ROOT / "target-preflight"

EXPECTED_TARGET_PREFLIGHTS = {
    "20260526_dcc_other_template_category",
    "20260629_mes_smart_scheduling_role_scope",
    "20260709_mes_rt000006_batch_record_mapping",
    "20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup",
    "20260717_mes_balloon_excel_device_workstation_binding",
    "20260717_mes_invalid_process_reference_normalization",
    "20260718_mes_puhui_schedule_admin_role_visibility",
    "20260718_mes_route_version_bpm_notify_to_inbox",
    "20260718_mes_route_version_withdraw_reopen_permission_menu",
    "20260719_dcc_obsolete_form_policy_seed",
    "20260720_edhr_release_void_form_policy_seed",
    "20260720_mes_edhr_work_task_runtime_entitlement_backfill",
    "20260720_mes_schedule_replan_form_policy_seed",
    "20260721_batch_record_bpm_policy_seed",
    "20260721_mes_route_version_publish_business_approval_policy_seed",
    "20260721_mes_schedule_replan_approval_retire",
    "20260722_edhr_release_form_policy_retire",
    "20260721_form_template_obsolete_bpm_policy_seed",
    "20260722_mes_route_creator_route_edit_permission_backfill",
    "20260723_mes_route_form_business_approval_policy_backfill",
    "20260723_mes_route_form_permission_rule_version_backfill",
    "20260723_unify_form_action_policy_into_business_approval_policy",
    "20260723_z_edhr_batch_void_business_policy_all_state",
    "20260725_system_backup_plan_menu",
    "20260728_mes_scheduler_route_flow_list_permission",
    "20260728_fix_mdm_product_menu_utf8_name",
    "20260728_user_list_access_role",
    "20260729_dcc_product_catalog_remove_subsidiary_source",
    "20260807_test_tenant1_all_role_permission_sync",
    "20260808_mes_qa_optional_equipment_items",
    "20260809_mes_team_leader_employee_scope_backfill",
    "20260811_mes_process_pool_cleaning_wash_parameter_data",
    "20260811_mes_process_pool_cleaning_process_parameter_data",
    "20260811_mes_process_pool_uv1_metering_valid_parameter",
    "20260811_mes_process_pool_uv2_two_device_runtime_config",
    "20260811_mes_qa_pressure_pump_id_seed",
    "20260812_mes_pqc_dcc_qa_c00_preflight",
    "20260812_mes_pqc_dcc_qa_c00_backfill",
    "20260812_mes_pqc_dcc_qa_c00_rollback",
    "20260812_mes_process_pool_b04091_cleaning_temperature_label",
    "20260812_mes_process_pool_b09353_cleaning_temperature_label",
    "20260812_mes_qa_pressure_pump_idi_seed",
    "20260818_mes_pressure_pump_same_name_item_convergence",
    "20260829_mes_old_form_template_binding_switch",
    "20260829_registration_certificate_management_menu_hierarchy",
    "20260830_dcc_registration_certificate_associated_company_backfill",
    "20260830_dcc_registration_certificate_notification_role_scope_backfill",
    "20260830_mdm_company_scope_crud_menu",
    "20260830_mes_process_pool_idi_device_parameter_rules",
    "20260901_dcc_registration_certificate_change_submit_role_permission",
    "20260901_dcc_registration_certificate_threshold_notify_template",
    "20260901_mes_pqc_leader_nonconformance_review_permission",
    "20260905_erp_kingdee_production_replenishment_list_sync",
    "20260907_dcc_publication_notification",
    "20260908_system_backup_evidence_export_permission",
    "20260910_erp_finance_fenbeitong_assistant_menu",
    "20260911_dcc_publish_direct_policy",
    "20260911_dcc_retire_form_center_upload_entry",
}

PREFLIGHT_METADATA_PATTERN = re.compile(
    r"^\s*--\s*release-target-preflight:\s*"
    r"migrationId=(?P<migration_id>[A-Za-z0-9_.-]+);\s*"
    r"allowedEnvironments=(?P<environments>[A-Za-z0-9_,-]+)\s*$",
    re.MULTILINE,
)
MIGRATION_METADATA_PATTERN = re.compile(r"^\s*--\s*release-migration:\s*(?P<body>.+?)\s*$", re.MULTILINE)
FORBIDDEN_PATTERN = re.compile(
    r"\b(?:insert|update|delete|replace|create|alter|drop|truncate|rename|call|do|handler|"
    r"load|lock|unlock|grant|revoke|commit|rollback|start\s+transaction|set|outfile|dumpfile|"
    r"get_lock|release_lock|release_all_locks|load_file|sleep|benchmark|delimiter|charset|into)\b|:=|"
    r"\bfor\s+update\b|\block\s+in\s+share\s+mode\b",
    re.IGNORECASE,
)
ALLOWED_ENVIRONMENTS = {"test", "prod", "backup"}


def _executable_sql(text: str) -> str:
    if "\\" in text or "/*" in text:
        raise AssertionError("client commands, backslash escapes and block comments are forbidden")
    output = list(text)
    quote: str | None = None
    index = 0
    while index < len(text):
        char = text[index]
        if quote is None:
            if char == "#" or (text.startswith("--", index) and (index + 2 == len(text) or text[index + 2].isspace())):
                while index < len(text) and text[index] not in "\r\n":
                    output[index] = " "
                    index += 1
                continue
            if char in ("'", '"', "`"):
                quote = char
                output[index] = " "
            index += 1
            continue
        if char == quote:
            if index + 1 < len(text) and text[index + 1] == quote:
                output[index] = output[index + 1] = " "
                index += 2
                continue
            output[index] = " "
            quote = None
            index += 1
            continue
        if char != "\n":
            output[index] = " "
        index += 1
    assert quote is None, "target preflight SQL has an unterminated string literal"
    return "".join(output)


def _metadata_fields(sql: str) -> dict[str, str]:
    match = MIGRATION_METADATA_PATTERN.search(sql)
    assert match, "release migration metadata is required"
    fields: dict[str, str] = {}
    for segment in match.group("body").split(";"):
        segment = segment.strip()
        if not segment:
            continue
        key, value = segment.split("=", 1)
        fields[key.strip()] = value.strip()
    return fields


def test_target_preflight_file_set_matches_release_plan() -> None:
    actual = {path.name.removesuffix(".preflight.sql") for path in TARGET_PREFLIGHT_ROOT.glob("*.preflight.sql")}

    assert actual == EXPECTED_TARGET_PREFLIGHTS


def test_target_preflights_are_read_only_single_statement_contracts() -> None:
    for migration_id in EXPECTED_TARGET_PREFLIGHTS:
        path = TARGET_PREFLIGHT_ROOT / f"{migration_id}.preflight.sql"
        migration_path = SQL_ROOT / f"{migration_id}.sql"
        text = path.read_text(encoding="utf-8")
        migration_text = migration_path.read_text(encoding="utf-8")

        preflight_metadata = PREFLIGHT_METADATA_PATTERN.findall(text)
        assert len(preflight_metadata) == 1
        bound_migration_id, environments_text = preflight_metadata[0]
        assert bound_migration_id == migration_id

        environments = {item.strip() for item in environments_text.split(",") if item.strip()}
        assert environments
        assert environments <= ALLOWED_ENVIRONMENTS

        fields = _metadata_fields(migration_text)
        migration_type = fields["type"]
        assert migration_type != "schema" or fields.get("requiresTargetPreflight") == "true"
        migration_environments = {item.strip() for item in fields["allowedEnvironments"].split(",") if item.strip()}
        assert environments <= migration_environments

        marker = f"TARGET_PREFLIGHT_PASS:{migration_id}"
        assert text.count(marker) == 1

        executable = _executable_sql(text).strip()
        assert not FORBIDDEN_PATTERN.search(executable)

        statements = [statement.strip() for statement in executable.split(";") if statement.strip()]
        assert len(statements) == 1
        assert re.match(r"^(?:select|with)\b", statements[0], re.IGNORECASE)


def test_mes_smart_scheduling_role_scope_preflight_matches_active_route_menu_contract() -> None:
    text = (TARGET_PREFLIGHT_ROOT / "20260629_mes_smart_scheduling_role_scope.preflight.sql").read_text(encoding="utf-8")

    for menu_id in ("900120", "5590", "5580", "5550", "5262", "5540", "900104", "5985", "5551", "5552", "5553", "5532", "5535", "5555", "5969", "900200", "5723", "5730"):
        assert menu_id in text

    assert "900121" in text
    assert "900122" in text
    assert "5726" in text
    assert "5727" in text
    assert "old_route_menus" in text
    assert "new_route_menus" in text


def test_puhui_schedule_admin_preflight_accepts_root_smart_scheduling_menu() -> None:
    text = (TARGET_PREFLIGHT_ROOT / "20260718_mes_puhui_schedule_admin_role_visibility.preflight.sql").read_text(encoding="utf-8")

    assert "900120" in text
    assert "parent_id IN (0, 5100)" in text
    assert "MesProPuhuiSchedule" in text
    assert "mes:pro-puhui-schedule:query" in text


def test_old_form_template_binding_preflight_rejects_missing_jimu_layout_before_migration() -> None:
    text = (TARGET_PREFLIGHT_ROOT / "20260829_mes_old_form_template_binding_switch.preflight.sql").read_text(encoding="utf-8")

    assert "mes_pro_route_flow_process_batch_record" in text
    assert "bpm_form_template_version" in text
    assert "rb.form_template_id IS NOT NULL" in text
    assert "rb.batch_record_report_id IS NULL" in text
    assert "JSON_VALID(tv.jimu_schema_json)" in text
    assert "$.sheetLayoutJson" in text
    assert "tv.id IS NULL" in text


def test_balloon_xlsx_cleanup_preflight_matches_current_or_legacy_target_contract() -> None:
    text = (TARGET_PREFLIGHT_ROOT / "20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup.preflight.sql").read_text(encoding="utf-8")

    for token in [
        "ROUTE-XLSX-00001",
        "ROUTE-XLSX-00002",
        "B320",
        "Z2620",
        "reported_quantity",
        "total_active_process_count",
        "already_normalized_target_count",
        "legacy_cleanup_target_count",
        "legacy_reported_schedule_count",
    ]:
        assert token in text, f"preflight must check the same target-data contract as migration SQL via: {token}"
