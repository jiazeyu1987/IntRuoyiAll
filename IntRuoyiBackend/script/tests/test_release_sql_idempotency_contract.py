from pathlib import Path
import re


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_mes_dv_machinery_extend_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260513_mes_dv_machinery_extend.sql").read_text(encoding="utf-8")

    assert "information_schema.columns" in text
    assert "CREATE PROCEDURE intruoyi_add_mes_dv_machinery_column" in text
    assert "CALL intruoyi_add_mes_dv_machinery_column('process_name'" in text
    assert "CALL intruoyi_add_mes_dv_machinery_column('standard_hourly_capacity'" in text
    assert "ALTER TABLE `mes_dv_machinery`\n  ADD COLUMN" not in text


def test_mes_dv_machinery_process_process_id_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260513_mes_dv_machinery_process_process_id.sql").read_text(
        encoding="utf-8"
    )

    assert "information_schema.columns" in text
    assert "CREATE PROCEDURE intruoyi_add_mes_dv_machinery_process_column" in text
    assert "CALL intruoyi_add_mes_dv_machinery_process_column('process_id'" in text
    assert "ALTER TABLE `mes_dv_machinery_process`\n    ADD COLUMN" not in text


def test_mes_md_workstation_capacity_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260513_mes_md_workstation_capacity.sql").read_text(encoding="utf-8")

    assert "information_schema.columns" in text
    assert "CREATE PROCEDURE intruoyi_add_mes_md_workstation_column" in text
    assert "CALL intruoyi_add_mes_md_workstation_column('single_standard_hourly_capacity'" in text
    assert "ALTER TABLE `mes_md_workstation`\n    ADD COLUMN" not in text


def test_mes_batch_record_report_route_key_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260516_mes_batch_record_report_route_key.sql").read_text(
        encoding="utf-8"
    )

    assert "information_schema.columns" in text
    assert "information_schema.statistics" in text
    assert "CREATE PROCEDURE intruoyi_add_mes_batch_record_report_column" in text
    assert "CREATE PROCEDURE intruoyi_update_mes_batch_record_report_route_index" in text
    assert "CALL intruoyi_add_mes_batch_record_report_column('route_key'" in text
    assert "index_name = 'uk_mes_batch_record_report_sample_table'" in text
    assert "index_name = 'uk_mes_batch_record_report_sample_route_table'" in text
    assert "DROP INDEX `uk_mes_batch_record_report_sample_table`" in text
    assert "ADD UNIQUE KEY `uk_mes_batch_record_report_sample_route_table`" in text


def test_mes_route_process_batch_record_binding_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260522_mes_route_process_batch_record_binding.sql").read_text(
        encoding="utf-8"
    )

    assert "information_schema.columns" in text
    assert "CREATE PROCEDURE intruoyi_add_mes_route_process_column" in text
    assert "CALL intruoyi_add_mes_route_process_column('batch_record_report_id'" in text
    assert "ALTER TABLE `mes_pro_route_process`\n  ADD COLUMN" not in text


def test_mes_route_use_config_enabled_sql_is_idempotent_and_release_scoped() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260707_mes_route_use_config_enabled.sql").read_text(
        encoding="utf-8"
    )

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260707_mes_batch_record_extra_form_slots; type=schema; riskLevel=low"
    )
    assert "information_schema.columns" in text
    assert "CREATE PROCEDURE intruoyi_add_mes_route_use_config_enabled" in text
    assert "CALL intruoyi_add_mes_route_use_config_enabled()" in text
    assert "column_name = 'enabled'" in text
    assert "ALTER TABLE `mes_pro_route_use_config`\n  ADD COLUMN" not in text


def test_dcc_screenshot_t1_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260525_dcc_screenshot_t1.sql").read_text(encoding="utf-8")

    assert "information_schema.columns" in text
    assert "CREATE PROCEDURE intruoyi_add_dcc_controlled_file_column" in text
    assert "CALL intruoyi_add_dcc_controlled_file_column('drawing_pdf_file_id'" in text
    assert "CALL intruoyi_add_dcc_controlled_file_column('product_code'" in text
    assert "CALL intruoyi_add_dcc_controlled_file_column('need_training'" in text
    assert "CALL intruoyi_add_dcc_controlled_file_column('process_type'" in text
    assert "ALTER TABLE dcc_controlled_file\n    ADD COLUMN" not in text


def test_dcc_screenshot_t3_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260525_dcc_screenshot_t3.sql").read_text(encoding="utf-8")

    assert "information_schema.columns" in text
    assert "CREATE PROCEDURE intruoyi_add_dcc_controlled_file_training_column" in text
    assert "CALL intruoyi_add_dcc_controlled_file_training_column('training_record_file_id'" in text
    assert "ALTER TABLE dcc_controlled_file\n    ADD COLUMN" not in text


def test_dcc_screenshot_t5_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260525_dcc_screenshot_t5.sql").read_text(encoding="utf-8")

    assert "information_schema.columns" in text
    assert "CREATE PROCEDURE intruoyi_add_dcc_distribution_recipient_column" in text
    assert "CREATE PROCEDURE intruoyi_add_dcc_distribution_column" in text
    assert "CALL intruoyi_add_dcc_distribution_recipient_column('ack_comment'" in text
    assert "CALL intruoyi_add_dcc_distribution_column('recovered_by'" in text
    assert "CALL intruoyi_add_dcc_distribution_column('recovered_at'" in text
    assert "ALTER TABLE dcc_controlled_file_distribution_recipient\n    ADD COLUMN" not in text
    assert "ALTER TABLE dcc_controlled_file_distribution\n    ADD COLUMN" not in text


def test_dcc_controlled_file_change_type_sql_is_idempotent_and_release_scoped() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260715_dcc_controlled_file_change_type.sql").read_text(encoding="utf-8")

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=low"
    )
    assert "information_schema.columns" in text
    assert "CREATE PROCEDURE intruoyi_add_dcc_controlled_file_change_type" in text
    assert "CALL intruoyi_add_dcc_controlled_file_change_type()" in text
    assert "column_name = 'change_type'" in text
    assert "ALTER TABLE `dcc_controlled_file`\n      ADD COLUMN `change_type`" in text


def test_dcc_base_schema_controlled_print_menu_does_not_reuse_legacy_fixed_id() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260513_dcc_base_schema.sql").read_text(encoding="utf-8")

    match = re.search(
        r"INSERT INTO `system_menu`\s*\((?P<columns>[^)]*)\)\s*"
        r"SELECT (?P<select>[^;]*'dcc:controlled-file:print'[^;]*)"
        r"WHERE NOT EXISTS \((?P<guard>[^;]*)\);",
        text,
        re.S,
    )

    assert match, "base schema must still seed the controlled print menu permission"
    assert "`id`" not in match.group("columns"), (
        "controlled print base schema seed must not hardcode legacy menu id 6813; "
        "checksum drift replays can encounter target databases where that id is already occupied"
    )
    assert not re.search(r"\b6813\b", match.group("select")), (
        "controlled print base schema seed must allocate a safe id instead of inserting 6813"
    )
    assert "`permission` = 'dcc:controlled-file:print'" in match.group("guard")


def test_system_password_policy_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260525_system_password_policy.sql").read_text(encoding="utf-8")

    assert "information_schema.columns" in text
    assert "CREATE PROCEDURE intruoyi_add_system_users_column" in text
    assert "CALL intruoyi_add_system_users_column('password_update_time'" in text
    assert "ALTER TABLE `system_users`\n    ADD COLUMN" not in text


def test_dcc_tenant_scoped_code_indexes_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260530_dcc_tenant_scoped_code_indexes.sql").read_text(
        encoding="utf-8"
    )

    assert "information_schema.statistics" in text
    assert "CREATE PROCEDURE intruoyi_drop_dcc_index_if_exists" in text
    assert "CREATE PROCEDURE intruoyi_add_dcc_index_if_missing" in text
    assert "'dcc_file_category'" in text
    assert "'uk_dcc_file_category_code'" in text
    assert "'uk_dcc_file_category_tenant_code'" in text
    assert "'dcc_approval_position'" in text
    assert "'uk_dcc_approval_position_code'" in text
    assert "'uk_dcc_approval_position_tenant_code'" in text
    assert "ALTER TABLE `dcc_file_category`\n  DROP INDEX" not in text
    assert "ALTER TABLE `dcc_approval_position`\n  DROP INDEX" not in text


def test_mes_workstation_shift_hours_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260609_mes_md_workstation_shift_hours.sql").read_text(
        encoding="utf-8"
    )

    assert "information_schema.columns" in text
    assert "CREATE PROCEDURE intruoyi_add_mes_md_workstation_shift_hours" in text
    assert "CALL intruoyi_add_mes_md_workstation_shift_hours()" in text
    assert "ALTER TABLE `mes_md_workstation`\n    ADD COLUMN" not in text


def test_mes_schedule_issue_structured_backflow_sql_is_idempotent() -> None:
    text = (REPO_ROOT / "sql" / "mysql" / "20260624_mes_schedule_issue_structured_backflow.sql").read_text(
        encoding="utf-8"
    )

    assert "information_schema.columns" in text
    assert "information_schema.statistics" in text
    assert "PREPARE mes_schedule_issue_status_stmt FROM @mes_schedule_issue_status_sql;" in text
    assert "PREPARE mes_schedule_issue_status_index_stmt FROM @mes_schedule_issue_status_index_sql;" in text
    assert "PREPARE mes_schedule_issue_source_index_stmt FROM @mes_schedule_issue_source_index_sql;" in text
    assert "ALTER TABLE `mes_pro_schedule_issue`\n    ADD COLUMN `status`" not in text
    assert "ALTER TABLE `mes_pro_schedule_issue`\n    ADD KEY `idx_mes_pro_schedule_issue_status`" not in text
