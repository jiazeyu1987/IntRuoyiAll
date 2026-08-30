from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_local_restart_applies_mes_route_use_config_enabled_migration() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260707_mes_route_use_config_enabled.sql" in text
    assert "MES route use config enabled column" in text
    assert "TABLE_NAME = 'mes_pro_route_use_config'" in text or "table_name = 'mes_pro_route_use_config'" in text
    assert "COLUMN_NAME = 'enabled'" in text or "column_name = 'enabled'" in text


def test_local_restart_applies_system_user_lifecycle_deactivation_migration() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260830_system_user_lifecycle_deactivation.sql" in text
    assert "System user lifecycle deactivation schema" in text
    assert "TABLE_NAME = 'system_users'" in text
    assert "'lifecycle_document_type'" in text
    assert "'lifecycle_document_no'" in text
    assert "'lifecycle_document_time'" in text
    assert "'lifecycle_effective_time'" in text
    assert "'lifecycle_deactivated_time'" in text
    assert "INDEX_NAME = 'idx_system_users_lifecycle_due'" in text
    assert "handler_name = 'userLifecycleDeactivateJob'" in text


def test_local_restart_applies_idi_device_parameter_rules_migration() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260830_mes_process_pool_idi_device_parameter_rules.sql" in text
    assert "MES process pool IDI device parameter rules" in text
    assert "RT000028-IDI" in text
    assert "_utf8mb4 0xe68c89e58e8be5bc8fe79083e59b8ae689a9e58585e58e8be58a9be6b3b5" in text
    assert "project.`project_name` = '按压式球囊扩充压力泵'" not in text
    assert "B09393" in text
    assert "COUNT(DISTINCT target_rule.`parameter_code`)" in text
    assert "'ROUGH_WASH_COUNT'" in text
    assert "'ROUGH_WASH_MEDIUM'" in text
    assert "'ROUGH_WASH_POWER'" in text
    assert "'ROUGH_WASH_ROOM_TEMPERATURE'" in text
    assert "'ROUGH_WASH_TIME'" in text


def test_local_restart_accepts_completed_route_flow_unification() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")
    start = text.index("Name = 'MES route use config enabled column'")
    end = text.index(
        "ScriptPath = Join-Path $RepoRoot 'sql\\mysql\\20260707_mes_route_use_config_enabled.sql'",
        start,
    )
    block = text[start:end]

    assert "table_name = 'mes_pro_route_flow_config'" in block
    assert "column_name = 'enabled'" in block
    assert "table_name = 'mes_pro_route_use_config_legacy_20260709'" in block


def test_local_restart_applies_dcc_tenant_scoped_code_index_migration() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260614_dcc_master_directory_identity.sql" in text
    assert "DCC master directory identity" in text
    assert "COLUMN_NAME = 'directory_id'" in text
    assert "INDEX_COLUMNS = 'category_id,directory_id,file_name'" in text
    assert "20260530_dcc_exact_nas_identifier_collation.sql" in text
    assert "DCC exact NAS identifier collation" in text
    assert "TABLE_NAME = 'dcc_controlled_file_nas_transfer_task_item'" in text
    assert "TABLE_NAME = 'dcc_controlled_file_master'" in text
    assert "TABLE_NAME = 'dcc_controlled_file'" in text
    assert "nas_path" in text
    assert "COLLATION_NAME = 'utf8mb4_bin'" in text
    assert "20260530_dcc_tenant_scoped_code_indexes.sql" in text
    assert "uk_dcc_file_category_tenant_code" in text
    assert "uk_dcc_approval_position_tenant_code" in text
    assert "ProbeSql" in text


def test_local_restart_applies_dcc_file_category_batch_task_migrations() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")
    schema_migration = "20260710_dcc_file_category_batch_task.sql"
    unique_guard_migration = "20260710_dcc_batch_recognition_active_task_unique_guard.sql"

    assert schema_migration in text
    assert unique_guard_migration in text
    assert text.index(schema_migration) < text.index(unique_guard_migration)
    for marker in (
        "DCC file-category batch recognition schema",
        "COLUMN_NAME IN (",
        "'recognition_type'",
        "'unclassified_count'",
        "'ambiguous_count'",
        "'conflict_count'",
        "INDEX_NAME = 'idx_dcc_batch_recognition_task_type_status'",
        "DCC batch recognition active-task unique guard",
        "COLUMN_NAME = 'active_recognition_type'",
        "INDEX_NAME = 'uk_dcc_batch_recognition_task_active_type'",
    ):
        assert marker in text


def test_local_restart_applies_mes_feedback_source_import_record_migration() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260624_mes_feedback_surplus_pool.sql" in text
    assert "MES feedback surplus pool tables" in text
    assert "TABLE_NAME = 'mes_pro_feedback_surplus_pool'" in text
    assert "TABLE_NAME = 'mes_pro_feedback_surplus_allocation'" in text
    assert "20260626_mes_feedback_import_reattribute_link.sql" in text
    assert "MES feedback import reattribute source link" in text
    assert "TABLE_NAME = 'mes_pro_feedback'" in text
    assert "COLUMN_NAME = 'source_import_record_id'" in text
    assert "INDEX_NAME = 'idx_mes_pro_feedback_source_import_record_id'" in text


def test_local_restart_applies_scheduler_workbench_permission_split_migration() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260624_mes_scheduler_workbench_permission_split.sql" in text
    assert "MES scheduler workbench permission split" in text
    assert "`id` = 900170" in text
    assert "`permission` = 'mes:pro-scheduler-workbench:update'" in text
    assert "`id` = 900171" in text
    assert "`permission` = 'mes:pro-scheduler-workbench:smoke-test'" in text
    assert "`role_id` = 1" in text
    assert "`tenant_id` = 1" in text
    assert "`menu_id` = 900170" in text
    assert "`menu_id` = 900171" in text


def test_local_restart_scheduler_workbench_probe_uses_literal_powershell_here_string() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")
    start = text.index("Name = 'MES scheduler workbench permission split'")
    end = text.index("ScriptPath = Join-Path $RepoRoot 'sql\\mysql\\20260624_mes_scheduler_workbench_permission_split.sql'", start)
    block = text[start:end]

    assert "ProbeSql = @'" in block
    assert 'ProbeSql = @"' not in block


def test_local_restart_applies_edhr_flow_intervention_runtime_table_migration() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260626_mes_edhr_flow_intervention_runtime.sql" in text
    assert "MES eDHR flow intervention runtime tables" in text
    assert "TABLE_NAME = 'mes_pro_edhr_flow_event'" in text
    assert "TABLE_NAME = 'mes_pro_edhr_flow_intervention'" in text


def test_local_restart_applies_mes_replan_operation_log_reason_nullable_migration() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260626_mes_replan_operation_log_reason_nullable.sql" in text
    assert "MES replan operation log reason nullable" in text
    assert "TABLE_NAME = 'mes_pro_schedule_order_operation_log'" in text
    assert "COLUMN_NAME = 'reason'" in text


def test_local_restart_applies_dcc_access_rule_manual_binding_migration() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260626_dcc_access_rule_manual_binding.sql" in text
    assert "DCC access-rule manual binding marker" in text
    assert "TABLE_NAME = 'dcc_file_directory'" in text
    assert "COLUMN_NAME = 'access_rule_manually_bound'" in text


def test_local_restart_applies_business_approval_policy_form_slots_schema_migration() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260721_form_action_policy_approval_mode.sql" in text
    assert "Business approval policy form slots schema" in text
    assert "TABLE_NAME = 'bpm_business_approval_policy'" in text
    assert "'form_policy_type'" in text
    assert "'form_slots_json'" in text
    assert text.index("20260721_form_action_policy_approval_mode.sql") < text.index(
        "20260721_mes_route_version_publish_business_approval_policy_seed.sql"
    )


def test_local_restart_repairs_system_nas_menu_titles() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260520_system_nas_management_menu.sql" in text
    assert "System NAS management menu titles" in text
    assert "`name` = _utf8mb4 0x4e415320e7aea1e79086" in text
    assert "`name` = _utf8mb4 0x4e415320e9858de7bdaee69fa5e8afa2" in text
    assert "`name` = _utf8mb4 0x4e415320e9858de7bdaee4bf9de5ad98" in text
    assert "`name` = _utf8mb4 0x4e415320e8bf9ee68ea5e6b58be8af95" in text
    assert "`id` IN (5900, 5901, 5902, 5903)" in text


def test_local_restart_system_nas_menu_probe_uses_literal_powershell_here_string() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")
    start = text.index("Name = 'System NAS management menu titles'")
    end = text.index("ScriptPath = Join-Path $RepoRoot 'sql\\mysql\\20260520_system_nas_management_menu.sql'", start)
    block = text[start:end]

    assert "ProbeSql = @'" in block
    assert 'ProbeSql = @"' not in block


def test_local_restart_sql_probe_passes_multiline_sql_through_stdin() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "$stdout = Invoke-LocalMySqlCommand -MySqlArguments @('-N', '-B') -InputText $Sql" in text
    assert "$stdout = Invoke-LocalMySqlCommand -MySqlArguments @('-N', '-B', '-e', $Sql)" not in text


def test_local_restart_sql_probe_accepts_success_line_not_warning_text() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")
    start = text.index("function Test-LocalSqlProbe")
    end = text.index("function Invoke-LocalSqlScript", start)
    block = text[start:end]

    assert "$probeLines = @($stdout -split \"`r?`n\"" in block
    assert "$probeLines -contains '1'" in block
    assert "$stdout.Trim() -eq '1'" not in block


def test_local_restart_writes_mysql_stdin_as_utf8_bytes() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")
    start = text.index("function Invoke-ProcessWithCapture")
    end = text.index("function Invoke-LocalMySqlCommand", start)
    block = text[start:end]

    assert "[System.Text.UTF8Encoding]::new($false).GetBytes($InputText)" in block
    assert "$process.StandardInput.BaseStream.Write($inputBytes, 0, $inputBytes.Length)" in block
    assert "$process.StandardInput.Write($InputText)" not in block


def test_local_restart_reads_persistent_dcc_download_encryption_env() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "function Import-PersistentEnvironmentVariable" in text
    assert "[System.EnvironmentVariableTarget]::User" in text
    assert "[System.EnvironmentVariableTarget]::Machine" in text
    assert "Import-PersistentEnvironmentVariable $Name" in text


def test_local_restart_uses_spring_boot_executable_backend_jar() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")
    start = text.index("function Start-Backend")
    end = text.index("function Start-Website", start)
    block = text[start:end]

    assert "target\\yudao-server-exec.jar" in block
    assert "target\\yudao-server.jar" not in block
    assert "Missing executable backend jar after package" in block


def test_local_restart_backend_executable_jar_failure_message() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "Missing executable backend jar after package" in text


def test_local_restart_backend_package_runs_from_backend_repo_root() -> None:
    script_path = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    text = script_path.read_text(encoding="utf-8")
    start = text.index("function Start-Backend")
    end = text.index("function Start-Website", start)
    block = text[start:end]

    assert "Push-Location -LiteralPath $RepoRoot" in block
    assert "Pop-Location" in block
    assert block.index("Push-Location -LiteralPath $RepoRoot") < block.index("& mvn -pl yudao-server -am -DskipTests package")
    assert block.index("Pop-Location") > block.index("& mvn -pl yudao-server -am -DskipTests package")
