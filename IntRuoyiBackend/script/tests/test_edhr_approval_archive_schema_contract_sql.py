import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260526_edhr_approval_archive_schema_contract.sql"
TEST_SCHEMA_PATH = REPO_ROOT / "yudao-module-mes" / "src" / "test" / "resources" / "sql" / "create_tables.sql"
MES_JAVA_ROOT = REPO_ROOT / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def read_test_schema() -> str:
    return TEST_SCHEMA_PATH.read_text(encoding="utf-8")


def read_mes_java(relative_path: str) -> str:
    return (MES_JAVA_ROOT / relative_path).read_text(encoding="utf-8")


def test_edhr_approval_archive_contract_sql_declares_required_execution_fields() -> None:
    text = read_sql()

    for column in [
        "`active_context_key` varchar(255) DEFAULT NULL",
        "`process_definition_key` varchar(128) DEFAULT NULL",
        "`process_instance_id` varchar(64) DEFAULT NULL",
        "`submitted_by` bigint DEFAULT NULL",
        "`submitted_at` datetime DEFAULT NULL",
        "`approved_by` bigint DEFAULT NULL",
        "`approved_at` datetime DEFAULT NULL",
        "`rejected_by` bigint DEFAULT NULL",
        "`rejected_at` datetime DEFAULT NULL",
        "`reject_reason` varchar(500) DEFAULT NULL",
        "`closed_at` datetime DEFAULT NULL",
    ]:
        assert column in text

    assert "UNIQUE KEY `uk_execution_active_context` (`tenant_id`, `active_context_key`)" in text
    assert "UNIQUE KEY `uk_execution_process_instance` (`tenant_id`, `process_instance_id`)" in text
    assert "KEY `idx_execution_tracking`" in text


def test_edhr_execution_signature_snapshot_contract_is_frozen_in_do_and_vo() -> None:
    execution_do = read_mes_java(
        "dal/dataobject/pro/batchrecord/MesProBatchRecordExecutionDO.java"
    )
    execution_resp_vo = read_mes_java(
        "controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionRespVO.java"
    )
    signature_do = read_mes_java(
        "dal/dataobject/pro/batchrecord/MesProBatchRecordExecutionSignatureDO.java"
    )
    signature_resp_vo = read_mes_java(
        "controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionSignatureRespVO.java"
    )
    approval_snapshot_do = read_mes_java(
        "dal/dataobject/pro/batchrecord/MesProBatchRecordApprovalSnapshotDO.java"
    )

    for field in [
        "private String processInstanceId;",
        "private String processDefinitionKey;",
        "private LocalDateTime submittedAt;",
        "private LocalDateTime approvedAt;",
        "private LocalDateTime rejectedAt;",
        "private LocalDateTime closedAt;",
    ]:
        assert field in execution_do
        assert field in execution_resp_vo

    for field in [
        "private String processInstanceId;",
        "private String bpmTaskId;",
        "private String bpmTaskDefinitionKey;",
        "private String bpmTaskName;",
        "private String approvalResult;",
        "private String reason;",
        "private String actorName;",
    ]:
        assert field in signature_do
        assert field in signature_resp_vo

    for field in [
        "private Long executionId;",
        "private String processDefinitionKey;",
        "private String processInstanceId;",
        "private String approvalStatus;",
        "private String snapshotJson;",
        "private String snapshotHash;",
        "private String currentBpmTaskId;",
        "private String currentTaskDefinitionKey;",
        "private LocalDateTime submittedAt;",
        "private LocalDateTime approvedAt;",
        "private LocalDateTime rejectedAt;",
        "private LocalDateTime closedAt;",
    ]:
        assert field in approval_snapshot_do


def test_edhr_test_resource_schema_freezes_required_fields_and_unique_constraints() -> None:
    text = read_test_schema()
    execution_table = table_definition(text, "mes_pro_batch_record_execution")
    signature_table = table_definition(text, "mes_pro_batch_record_execution_signature")
    approval_snapshot_table = table_definition(text, "mes_pro_batch_record_approval_snapshot")

    for column in [
        '"active_context_key" varchar(512) DEFAULT NULL',
        '"process_definition_key" varchar(128) DEFAULT NULL',
        '"process_instance_id" varchar(64) DEFAULT NULL',
        '"submitted_by" bigint DEFAULT NULL',
        '"submitted_at" timestamp DEFAULT NULL',
        '"approved_by" bigint DEFAULT NULL',
        '"approved_at" timestamp DEFAULT NULL',
        '"rejected_by" bigint DEFAULT NULL',
        '"rejected_at" timestamp DEFAULT NULL',
        '"reject_reason" varchar(500) DEFAULT NULL',
        '"closed_at" timestamp DEFAULT NULL',
    ]:
        assert column in execution_table

    for column in [
        '"process_instance_id" varchar(64) DEFAULT NULL',
        '"bpm_task_id" varchar(64) DEFAULT NULL',
        '"bpm_task_definition_key" varchar(128) DEFAULT NULL',
        '"bpm_task_name" varchar(255) DEFAULT NULL',
        '"approval_result" varchar(32) DEFAULT NULL',
        '"reason" varchar(500) DEFAULT NULL',
        '"actor_name" varchar(64) DEFAULT NULL',
    ]:
        assert column in signature_table

    assert re.search(
        r'CONSTRAINT "uk_execution_active_context" UNIQUE \("tenant_id", "active_context_key"\)',
        execution_table,
    )
    assert re.search(
        r'CONSTRAINT "uk_execution_process_instance" UNIQUE \("tenant_id", "process_instance_id"\)',
        execution_table,
    )
    assert re.search(
        r'CONSTRAINT "uk_edhr_approval_execution" UNIQUE \("tenant_id", "execution_id"\)',
        approval_snapshot_table,
    )
    assert '"process_instance_id" varchar(64) DEFAULT NULL' in approval_snapshot_table
    assert re.search(
        r'CONSTRAINT "uk_edhr_approval_process_instance" UNIQUE \("tenant_id", "process_instance_id"\)',
        approval_snapshot_table,
    )


def table_definition(sql: str, table_name: str) -> str:
    pattern = rf'CREATE TABLE IF NOT EXISTS "{re.escape(table_name)}" \((?P<body>.*?)\n\);'
    match = re.search(pattern, sql, flags=re.DOTALL)
    assert match is not None, f"{table_name} must exist in test resource schema"
    return match.group("body")


def mysql_table_definition(sql: str, table_name: str) -> str:
    pattern = rf"CREATE TABLE IF NOT EXISTS `{re.escape(table_name)}` \((?P<body>.*?)\n\s*\) ENGINE="
    match = re.search(pattern, sql, flags=re.DOTALL)
    assert match is not None, f"{table_name} must exist in MySQL schema contract"
    return match.group("body")


def test_edhr_approval_archive_contract_sql_declares_snapshot_signature_and_archive_evidence() -> None:
    text = read_sql()
    approval_snapshot_table = mysql_table_definition(text, "mes_pro_batch_record_approval_snapshot")

    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_approval_snapshot`" in text
    for column in [
        "`process_instance_id` varchar(64) DEFAULT NULL",
        "`approval_status` varchar(32) NOT NULL",
        "`snapshot_json` longtext NOT NULL",
        "`snapshot_hash` char(64) NOT NULL",
        "`current_bpm_task_id` varchar(64) DEFAULT NULL",
        "`submit_signature_id` bigint DEFAULT NULL",
        "`approve_signature_id` bigint DEFAULT NULL",
        "`reject_signature_id` bigint DEFAULT NULL",
        "`closed_at` datetime DEFAULT NULL",
    ]:
        assert column in approval_snapshot_table

    assert "MODIFY COLUMN `process_instance_id` varchar(64) DEFAULT NULL COMMENT 'BPM流程实例ID'" in text

    assert "UNIQUE KEY `uk_edhr_approval_execution` (`tenant_id`, `execution_id`)" in text
    assert "UNIQUE KEY `uk_edhr_approval_process_instance` (`tenant_id`, `process_instance_id`)" in text

    for column in [
        "`process_instance_id` varchar(64) DEFAULT NULL",
        "`bpm_task_id` varchar(64) DEFAULT NULL",
        "`bpm_task_definition_key` varchar(128) DEFAULT NULL",
        "`bpm_task_name` varchar(255) DEFAULT NULL",
        "`approval_result` varchar(32) DEFAULT NULL",
        "`reason` varchar(500) DEFAULT NULL",
        "`actor_name` varchar(64) DEFAULT NULL",
    ]:
        assert column in text

    assert "`approval_snapshot_id` bigint DEFAULT NULL" in text
    assert "`approval_snapshot_hash` char(64) DEFAULT NULL" in text


def test_edhr_approval_archive_contract_sql_declares_permissions_and_history_gate() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-batch-record-execution:approve",
        "mes:pro-batch-record-execution:track",
        "mes:pro-batch-record-execution:signature-query",
        "mes:pro-batch-record-execution-archive:query",
        "mes:pro-batch-record-execution-archive:create",
        "mes:pro-batch-record-execution-archive:download",
    ]:
        assert permission in text

    assert "SELECT 'EDHR_SUBMITTED_ARCHIVE_RISK'" in text
    assert "`execution`.`status` <> 3" in text
    assert "UPDATE `mes_pro_batch_record_execution` SET `status` = 3" not in text


def test_archive_permissions_are_inserted_as_system_menu_rows() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-batch-record-execution-archive:query",
        "mes:pro-batch-record-execution-archive:create",
        "mes:pro-batch-record-execution-archive:download",
    ]:
        pattern = (
            r"INSERT INTO `system_menu`"
            r".*?SELECT\s+9000\d+,\s*'[^']+',\s*'"
            + re.escape(permission)
            + r"'"
            r".*?WHERE NOT EXISTS\s*\("
            r"\s*SELECT 1 FROM `system_menu`"
            r".*?WHERE `permission`\s*=\s*'"
            + re.escape(permission)
            + r"'"
            r"\s*\)"
        )
        assert re.search(pattern, text, flags=re.DOTALL | re.IGNORECASE), (
            f"{permission} must be inserted into system_menu with WHERE NOT EXISTS"
        )
        assert re.search(
            rf"SELECT\s+9000\d+,\s*'[^']+',\s*'{re.escape(permission)}',\s*3,\s*\d+,\s*900002",
            text,
            flags=re.DOTALL | re.IGNORECASE,
        ), f"{permission} must be inserted under eDHR parent menu 900002"


def test_edhr_permissions_use_existing_parent_and_repair_wrong_parent() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-batch-record-execution:approve",
        "mes:pro-batch-record-execution:track",
        "mes:pro-batch-record-execution:signature-query",
    ]:
        assert not re.search(
            rf"SELECT\s+9000\d+,\s*'[^']+',\s*'{re.escape(permission)}',\s*3,\s*\d+,\s*900010",
            text,
            flags=re.DOTALL | re.IGNORECASE,
        ), f"{permission} must not be inserted under missing parent menu 900010"
        assert re.search(
            rf"SELECT\s+9000\d+,\s*'[^']+',\s*'{re.escape(permission)}',\s*3,\s*\d+,\s*900002",
            text,
            flags=re.DOTALL | re.IGNORECASE,
        ), f"{permission} must be inserted under eDHR parent menu 900002"

    update_pattern = (
        r"UPDATE `system_menu`"
        r".*?SET `parent_id`\s*=\s*900002"
        r".*?WHERE `permission` IN \("
        r".*?'mes:pro-batch-record-execution:approve'"
        r".*?'mes:pro-batch-record-execution:track'"
        r".*?'mes:pro-batch-record-execution:signature-query'"
        r".*?'mes:pro-batch-record-execution-archive:query'"
        r".*?'mes:pro-batch-record-execution-archive:create'"
        r".*?'mes:pro-batch-record-execution-archive:download'"
        r".*?\)"
        r".*?`parent_id`\s*<>\s*900002"
    )
    assert re.search(update_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
        "existing eDHR permission rows with wrong parent_id must be repaired idempotently"
    )


def test_edhr_real_router_menu_pages_are_inserted_under_mes_production_parent() -> None:
    text = read_sql()

    for menu_id, path, component, component_name, permission in [
        (
            900033,
            "feedback/edhr-batch-execution",
            "mes/pro/edhr-batch/BatchExecutionListPage",
            "MesProEdhrBatchExecutionListPage",
            "mes:pro-edhr-batch-execution:query",
        ),
        (
            900024,
            "feedback/edhr-approval",
            "mes/pro/edhr/ApprovalPage",
            "MesProFeedbackEdhrApproval",
            "mes:pro-batch-record-execution:approve",
        ),
        (
            900025,
            "feedback/edhr-tracking",
            "mes/pro/edhr/TrackingPage",
            "MesProFeedbackEdhrTracking",
            "mes:pro-batch-record-execution:track",
        ),
        (
            900026,
            "feedback/edhr-signatures",
            "mes/pro/edhr/SignaturePage",
            "MesProFeedbackEdhrSignatures",
            "mes:pro-batch-record-execution:signature-query",
        ),
    ]:
        insert_pattern = (
            r"INSERT INTO `system_menu`"
            rf".*?SELECT\s+{menu_id},\s*'[^']+',\s*'{re.escape(permission)}',\s*2,\s*\d+,\s*5700,"
            rf"\s*'{re.escape(path)}',\s*'[^']*',\s*'{re.escape(component)}',\s*'{re.escape(component_name)}'"
        )
        assert re.search(insert_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
            f"{path} must be inserted as a real type=2 menu page under MES production parent 5700 "
            f"with component_name {component_name}"
        )

        dedupe_pattern = (
            r"WHERE NOT EXISTS\s*\("
            r"\s*SELECT 1 FROM `system_menu`"
            rf".*?`id`\s*=\s*{menu_id}"
            rf".*?OR `path`\s*=\s*'{re.escape(path)}'"
            r"\s*\)"
        )
        assert re.search(dedupe_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
            f"{path} menu insert must be idempotent by stable id/path"
        )


def test_edhr_real_router_menu_pages_repair_component_name_with_null_safe_match() -> None:
    text = read_sql()

    update_pattern = (
        r"UPDATE `system_menu`"
        r".*?SET `component_name`\s*=\s*CASE `id`"
        r".*?WHEN 900033 THEN 'MesProEdhrBatchExecutionListPage'"
        r".*?WHEN 900024 THEN 'MesProFeedbackEdhrApproval'"
        r".*?WHEN 900025 THEN 'MesProFeedbackEdhrTracking'"
        r".*?WHEN 900026 THEN 'MesProFeedbackEdhrSignatures'"
        r".*?WHERE `id` IN \(900033, 900024, 900025, 900026\)"
        r".*?NOT\s*\(`component_name`\s*<=>\s*CASE `id`"
        r".*?WHEN 900033 THEN 'MesProEdhrBatchExecutionListPage'"
        r".*?WHEN 900024 THEN 'MesProFeedbackEdhrApproval'"
        r".*?WHEN 900025 THEN 'MesProFeedbackEdhrTracking'"
        r".*?WHEN 900026 THEN 'MesProFeedbackEdhrSignatures'"
        r".*?END\)"
    )
    assert re.search(update_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
        "existing type=2 eDHR menu rows must repair wrong or NULL component_name values "
        "with MySQL null-safe comparison"
    )


def test_edhr_real_router_menu_pages_stay_under_mes_parent_after_permission_parent_repair() -> None:
    text = read_sql()

    page_parent_repair_pattern = (
        r"UPDATE `system_menu`"
        r".*?SET `parent_id`\s*=\s*5700"
        r".*?`type`\s*=\s*2"
        r".*?WHERE `id` IN \(900033, 900024, 900025, 900026\)"
        r".*?\(`parent_id`\s*<>\s*5700 OR `type`\s*<>\s*2\)"
    )
    assert re.search(page_parent_repair_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
        "real eDHR menu pages must be repaired back under MES production parent 5700"
    )

    permission_parent_repair_pattern = (
        r"UPDATE `system_menu`"
        r".*?SET `parent_id`\s*=\s*900002"
        r".*?WHERE `permission` IN \("
        r".*?'mes:pro-batch-record-execution:approve'"
        r".*?'mes:pro-batch-record-execution:track'"
        r".*?'mes:pro-batch-record-execution:signature-query'"
        r".*?'mes:pro-batch-record-execution-archive:query'"
        r".*?'mes:pro-batch-record-execution-archive:create'"
        r".*?'mes:pro-batch-record-execution-archive:download'"
        r".*?\)"
        r".*?AND `type`\s*=\s*3"
        r".*?AND `parent_id`\s*<>\s*900002"
    )
    assert re.search(permission_parent_repair_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
        "permission parent repair must only touch type=3 button permissions, not type=2 menu pages"
    )


def test_existing_feedback_menu_chain_is_required_before_grant_merge() -> None:
    text = read_sql()

    required_feedback_pattern = (
        r"CREATE TEMPORARY TABLE `tmp_edhr_approval_required_feedback_menu_ids` AS"
        r".*?FROM `system_menu`"
        r".*?WHERE `deleted`\s*=\s*b'0'"
        r".*?`id`\s+IN\s*\("
        r"\s*5550\s*,\s*5551\s*,\s*5552\s*,\s*5553\s*,\s*5554\s*,\s*5555\s*,\s*5969\s*"
        r"\)"
    )
    assert re.search(required_feedback_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
        "production feedback page/permission menus 5550-5555 and 5969 must be read from existing "
        "system_menu rows, not recreated or skipped"
    )

    missing_feedback_signal = (
        r"IF\s*\(\s*SELECT COUNT\(\*\)"
        r".*?FROM `tmp_edhr_approval_required_feedback_menu_ids`"
        r".*?\)\s*<>\s*7\s*THEN"
        r".*?SIGNAL SQLSTATE '45000'"
        r".*?Missing production feedback system_menu"
    )
    assert re.search(missing_feedback_signal, text, flags=re.DOTALL | re.IGNORECASE), (
        "missing or deleted production feedback menus must fail fast before tenant package/role grants"
    )


def test_tenant_packages_with_mes_or_edhr_parent_merge_new_menu_ids() -> None:
    text = read_sql()

    assert "CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_menu_ids`" in text
    assert "CREATE TEMPORARY TABLE `tmp_edhr_approval_required_feedback_menu_ids`" in text
    assert "CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_target_packages`" in text
    assert "CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_package_menu_ids`" in text
    assert "CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_package_menu_json`" in text

    target_pattern = (
        r"CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_target_packages` AS"
        r".*?FROM `system_tenant_package` AS `package`"
        r".*?JSON_TABLE\("
        r".*?AS `existing_menu`"
        r".*?JSON_VALID\(`package`\.`menu_ids`\)"
        r".*?`existing_menu`\.`menu_id`\s+IN\s*\(\s*5700\s*,\s*900002\s*\)"
    )
    assert re.search(target_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
        "tenant package merge must target only packages whose menu_ids contain MES parent 5700 or eDHR parent 900002"
    )

    menu_ids_pattern = (
        r"WHERE `deleted`\s*=\s*b'0'"
        r".*?`id`\s+IN\s*\("
        r"\s*5550\s*,\s*5551\s*,\s*5552\s*,\s*5553\s*,\s*5554\s*,\s*5555\s*,\s*5969\s*,"
        r"\s*900017\s*,\s*900018\s*,\s*900019\s*,\s*900020\s*,\s*900021\s*,\s*900022\s*,"
        r"\s*900033\s*,\s*900024\s*,\s*900025\s*,\s*900026\s*"
        r"\)"
    )
    assert re.search(menu_ids_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
        "tenant package merge must include production feedback menus, archive buttons, "
        "approval buttons, and real eDHR menu pages"
    )

    update_pattern = (
        r"UPDATE `system_tenant_package` AS `package`"
        r".*?INNER JOIN `tmp_edhr_approval_permission_package_menu_json` AS `merged`"
        r".*?SET `package`\.`menu_ids`\s*=\s*`merged`\.`menu_ids`"
        r".*?WHERE `package`\.`deleted`\s*=\s*b'0'"
    )
    assert re.search(update_pattern, text, flags=re.DOTALL | re.IGNORECASE)


def test_tenant_admin_role_menu_restores_and_inserts_new_edhr_menu_ids() -> None:
    text = read_sql()

    target_roles_pattern = (
        r"CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_target_roles` AS"
        r".*?FROM `system_tenant` AS `tenant`"
        r".*?INNER JOIN `tmp_edhr_approval_permission_target_packages` AS `target_package`"
        r".*?`target_package`\.`package_id`\s*=\s*`tenant`\.`package_id`"
        r".*?INNER JOIN `system_role` AS `role`"
        r".*?`role`\.`tenant_id`\s*=\s*`tenant`\.`id`"
        r".*?`role`\.`code`\s*=\s*'tenant_admin'"
        r".*?`role`\.`deleted`\s*=\s*b'0'"
        r".*?WHERE `tenant`\.`deleted`\s*=\s*b'0'"
    )
    assert re.search(target_roles_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
        "role menu grant must target only tenant_admin roles for tenants whose package contains 5700 or 900002"
    )
    target_roles_sql = re.search(
        r"CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_target_roles` AS"
        r"(?P<body>.*?)WHERE `tenant`\.`deleted`\s*=\s*b'0'",
        text,
        flags=re.DOTALL | re.IGNORECASE,
    )
    assert target_roles_sql is not None
    assert "system_role_menu" not in target_roles_sql.group("body"), (
        "target tenant_admin roles must be selected from tenant package scope, "
        "not only from roles that already have soft-deleted system_role_menu rows"
    )

    restore_pattern = (
        r"UPDATE `system_role_menu` AS `role_menu`"
        r".*?INNER JOIN `tmp_edhr_approval_permission_target_roles` AS `target_role`"
        r".*?`target_role`\.`tenant_id`\s*=\s*`role_menu`\.`tenant_id`"
        r".*?`target_role`\.`role_id`\s*=\s*`role_menu`\.`role_id`"
        r".*?INNER JOIN `tmp_edhr_approval_permission_menu_ids` AS `permission_menu`"
        r".*?`permission_menu`\.`id`\s*=\s*`role_menu`\.`menu_id`"
        r".*?SET `role_menu`\.`deleted`\s*=\s*b'0'"
        r".*?WHERE `role_menu`\.`deleted`\s*=\s*b'1'"
    )
    assert re.search(restore_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
        "soft-deleted system_role_menu rows for 5550-5555, 5969, and 900017-900026 must be restored"
    )

    insert_pattern = (
        r"INSERT INTO `system_role_menu`"
        r"\s*\(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`\)"
        r".*?FROM `tmp_edhr_approval_permission_target_roles` AS `target_role`"
        r".*?CROSS JOIN `tmp_edhr_approval_permission_menu_ids` AS `permission_menu`"
        r".*?WHERE NOT EXISTS\s*\("
        r".*?FROM `system_role_menu` AS `existing`"
        r".*?`existing`\.`tenant_id`\s*=\s*`target_role`\.`tenant_id`"
        r".*?`existing`\.`role_id`\s*=\s*`target_role`\.`role_id`"
        r".*?`existing`\.`menu_id`\s*=\s*`permission_menu`\.`id`"
        r".*?`existing`\.`deleted`\s*=\s*b'0'"
        r".*?\)"
    )
    assert re.search(insert_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
        "missing system_role_menu rows for 5550-5555, 5969, and 900017-900026 must be inserted idempotently"
    )


def test_tenant_package_menu_merge_is_idempotent_and_json_validated() -> None:
    text = read_sql()

    assert "JSON_VALID(`package`.`menu_ids`)" in text
    invalid_json_signal = (
        r"IF EXISTS\s*\("
        r".*?FROM `system_tenant_package` AS `package`"
        r".*?NOT JSON_VALID\(`package`\.`menu_ids`\)"
        r".*?\)\s*THEN"
        r".*?SIGNAL SQLSTATE '45000'"
        r".*?system_tenant_package\.menu_ids"
        r".*?END IF;"
    )
    assert re.search(invalid_json_signal, text, flags=re.DOTALL | re.IGNORECASE), (
        "invalid system_tenant_package.menu_ids JSON must fail fast before package merge"
    )

    assert "PRIMARY KEY (`package_id`, `menu_id`)" in text
    assert "INSERT IGNORE INTO `tmp_edhr_approval_permission_package_menu_ids`" in text
    assert "JSON_ARRAYAGG(`menu_id`)" in text
    assert "ORDER BY `package_id`, `menu_id`" in text

    missing_menu_signal = (
        r"IF\s*\(\s*SELECT COUNT\(\*\)"
        r".*?FROM `tmp_edhr_approval_permission_menu_ids`"
        r".*?\)\s*<>\s*17\s*THEN"
        r".*?SIGNAL SQLSTATE '45000'"
    )
    assert re.search(missing_menu_signal, text, flags=re.DOTALL | re.IGNORECASE), (
        "missing production feedback, eDHR page, or permission menu rows must fail fast before merging tenant packages"
    )


def test_edhr_execution_indexes_are_created_idempotently() -> None:
    text = read_sql()

    for index_name in [
        "uk_execution_active_context",
        "uk_execution_process_instance",
        "idx_execution_tracking",
    ]:
        pattern = (
            r"IF NOT EXISTS\s*\(\s*SELECT 1\s+FROM information_schema\.STATISTICS"
            r".*?TABLE_SCHEMA\s*=\s*DATABASE\(\)"
            r".*?TABLE_NAME\s*=\s*'mes_pro_batch_record_execution'"
            rf".*?INDEX_NAME\s*=\s*'{index_name}'"
            r".*?\)\s*THEN"
            r".*?ALTER TABLE `mes_pro_batch_record_execution`"
            rf".*?`{index_name}`"
            r".*?END IF;"
        )
        assert re.search(pattern, text, flags=re.DOTALL | re.IGNORECASE), (
            f"{index_name} must be protected by information_schema.STATISTICS before ALTER TABLE"
        )


def test_required_tables_fail_fast_before_schema_mutation() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_batch_record_execution",
        "mes_pro_batch_record_execution_signature",
        "mes_pro_batch_record_execution_archive",
    ]:
        pattern = (
            r"IF NOT EXISTS\s*\(\s*SELECT 1\s+FROM information_schema\.TABLES"
            r".*?TABLE_SCHEMA\s*=\s*DATABASE\(\)"
            rf".*?TABLE_NAME\s*=\s*'{table_name}'"
            r".*?\)\s*THEN"
            r".*?SIGNAL SQLSTATE '45000'"
            rf".*?{table_name}"
            r".*?END IF;"
        )
        assert re.search(pattern, text, flags=re.DOTALL | re.IGNORECASE), (
            f"{table_name} must fail fast with SIGNAL SQLSTATE '45000'"
        )

    first_signal = text.index("SIGNAL SQLSTATE '45000'")
    first_mutation = min(
        text.index("ALTER TABLE `mes_pro_batch_record_execution`"),
        text.index("CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_approval_snapshot`"),
        text.index("ALTER TABLE `mes_pro_batch_record_execution_signature`"),
        text.index("ALTER TABLE `mes_pro_batch_record_execution_archive`"),
    )
    assert first_signal < first_mutation


def test_required_tables_are_not_silently_skipped_and_archive_is_checked_before_risk_query() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_batch_record_execution",
        "mes_pro_batch_record_execution_signature",
        "mes_pro_batch_record_execution_archive",
    ]:
        silent_skip_pattern = (
            r"IF EXISTS\s*\(\s*SELECT 1\s+FROM information_schema\.TABLES"
            r".*?TABLE_SCHEMA\s*=\s*DATABASE\(\)"
            rf".*?TABLE_NAME\s*=\s*'{table_name}'"
            r".*?\)\s*THEN"
        )
        assert not re.search(silent_skip_pattern, text, flags=re.DOTALL | re.IGNORECASE), (
            f"{table_name} must not be guarded by table-level IF EXISTS silent skip"
        )

    archive_signal = re.search(
        r"IF NOT EXISTS\s*\(\s*SELECT 1\s+FROM information_schema\.TABLES"
        r".*?TABLE_NAME\s*=\s*'mes_pro_batch_record_execution_archive'"
        r".*?\)\s*THEN"
        r".*?SIGNAL SQLSTATE '45000'",
        text,
        flags=re.DOTALL | re.IGNORECASE,
    )
    assert archive_signal is not None
    assert archive_signal.start() < text.index("SELECT 'EDHR_SUBMITTED_ARCHIVE_RISK'")
