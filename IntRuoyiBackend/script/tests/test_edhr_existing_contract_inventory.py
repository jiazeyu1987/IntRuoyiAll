from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
CONTRACT_PATH = REPO_ROOT / "docs" / "edhr" / "existing-edhr-contract.md"


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"{relative_path} must exist"
    return path.read_text(encoding="utf-8")


def read_contract() -> str:
    assert CONTRACT_PATH.exists(), "docs/edhr/existing-edhr-contract.md must exist before coding T1-T6"
    return CONTRACT_PATH.read_text(encoding="utf-8")


def test_existing_edhr_contract_document_declares_reuse_gap_and_no_rewrite_boundaries() -> None:
    text = read_contract()

    for heading in [
        "# eDHR 现有合约盘点",
        "## 可复用对象",
        "## 需新增对象",
        "## 禁止改写对象",
        "## 后续 Coding 门禁",
    ]:
        assert heading in text

    for required_gap in [
        "初始化与 DHR 模板",
        "独立表单与记录本",
        "流转单",
        "标签",
        "打印管理",
        "放行前检查",
        "报表与看板",
        "CSV/OQ/PQ",
    ]:
        assert required_gap in text

    for boundary in [
        "不得改写 `/mes/pro/batch-record-execution`",
        "不得重命名 `mes_pro_batch_record_execution`",
        "不得绕过 eDHR 对象权限、操作审计、签名时间和归档证据",
        "缺真实表、菜单、权限、租户绑定或样本时必须 fail fast",
    ]:
        assert boundary in text


def test_existing_backend_controllers_keep_public_edhr_api_and_permission_contracts() -> None:
    controller_expectations = {
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrBatchExecutionController.java": [
            '@RequestMapping("/mes/pro/edhr-batch-execution")',
            '@GetMapping("/page")',
            '@GetMapping("/get")',
            '@PostMapping("/open-or-create")',
            '@PostMapping("/task/open")',
            '@PostMapping("/task/special-node/skip")',
            '@PostMapping("/task/special-node/complete")',
            '@PostMapping("/task/special-node/attachment/prepare-upload")',
            '@PostMapping("/sync-status")',
            '@PostMapping("/close")',
            '@PostMapping("/quality-reject")',
            "mes:pro-edhr-batch-execution:query",
            "mes:pro-edhr-batch-execution:create",
            "mes:pro-edhr-batch-execution:update",
            "mes:pro-edhr-batch-execution:close",
            "mes:pro-edhr-batch-execution:quality-reject",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrBatchExecutionArchiveController.java": [
            '@RequestMapping("/mes/pro/edhr-batch-execution-archive")',
            '@PostMapping("/generate")',
            '@GetMapping("/latest")',
            '@GetMapping("/download")',
            "mes:pro-edhr-batch-execution-archive:create",
            "mes:pro-edhr-batch-execution-archive:query",
            "mes:pro-edhr-batch-execution-archive:download",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrRecordChangeController.java": [
            '@RequestMapping("/mes/pro/edhr-change")',
            '@PostMapping("/void-execution/request")',
            '@PostMapping("/void-execution/approve")',
            '@PostMapping("/reopen-batch/request")',
            '@PostMapping("/reopen-batch/approve")',
            '@PostMapping("/reopen-execution/request")',
            '@PostMapping("/reopen-execution/approve")',
            '@PostMapping("/supplement/request")',
            '@PutMapping("/supplement/save-draft")',
            '@PostMapping("/supplement/submit")',
            '@PostMapping("/supplement/approve")',
            '@GetMapping("/page")',
            '@GetMapping("/get")',
            "mes:pro-edhr-change:void",
            "mes:pro-edhr-change:approve",
            "mes:pro-edhr-change:reopen",
            "mes:pro-edhr-change:supplement",
            "mes:pro-edhr-change:query",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrWorkTaskController.java": [
            '@RequestMapping("/mes/pro/edhr-work-task")',
            '@GetMapping("/my-page")',
            '@GetMapping("/done-page")',
            '@GetMapping("/candidate-todo-page")',
            '@GetMapping("/stats")',
            '@GetMapping("/route-archive-rule")',
            '@PostMapping("/route-archive-rule")',
            '@PostMapping("/candidate-signature/complete")',
            "mes:pro-edhr-work-task:query",
            "mes:pro-edhr-work-task:update",
            "mes:pro-edhr-work-task-rule:query",
            "mes:pro-edhr-work-task-rule:update",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrPermissionScopeController.java": [
            '@RequestMapping("/mes/pro/edhr-permission-scopes")',
            '@PostMapping("/save")',
            '@GetMapping("/get")',
            '@PostMapping("/evaluate")',
            "mes:pro-edhr-permission-scope:save",
            "mes:pro-edhr-permission-scope:query",
            "mes:pro-edhr-permission-scope:evaluate",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrOperationAuditController.java": [
            '@RequestMapping("/mes/pro/edhr-operation-audit")',
            '@GetMapping("/page")',
            '@GetMapping("/{id}")',
            "mes:pro-edhr-operation-audit:query",
        ],
    }

    for relative_path, expected_fragments in controller_expectations.items():
        source = read_text(relative_path)
        for fragment in expected_fragments:
            assert fragment in source, f"{relative_path} must keep {fragment}"


def test_existing_legacy_batch_record_execution_contract_stays_reusable_for_edhr() -> None:
    controller_expectations = {
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordExecutionController.java": [
            '@RequestMapping("/mes/pro/batch-record-execution")',
            "mes:pro-batch-record-execution:create",
            "mes:pro-batch-record-execution:query",
            "mes:pro-batch-record-execution:update",
            "mes:pro-batch-record-execution:approve",
            "mes:pro-batch-record-execution:track",
            "mes:pro-batch-record-execution:signature-query",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordExecutionArchiveController.java": [
            '@RequestMapping("/mes/pro/batch-record-execution-archive")',
            "mes:pro-batch-record-execution-archive:create",
            "mes:pro-batch-record-execution-archive:query",
            "mes:pro-batch-record-execution-archive:download",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordExecutionFieldAuditController.java": [
            '@RequestMapping("/mes/pro/batch-record-execution/field-audit")',
            "mes:pro-batch-record-execution:field-audit-update",
            "mes:pro-batch-record-execution:field-audit-query",
            "mes:pro-batch-record-execution:field-audit-verify",
            "mes:pro-batch-record-execution:field-audit-export",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordDomainTraceController.java": [
            '@RequestMapping("/mes/pro/batch-record-execution/domain-trace")',
            "mes:pro-batch-record-execution:domain-trace-query",
            "mes:pro-batch-record-execution:domain-trace-verify",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordExecutionAttachmentController.java": [
            '@RequestMapping("/mes/pro/batch-record-execution/attachment")',
            '@PostMapping("/prepare-upload")',
            "mes:pro-batch-record-execution:field-audit-update",
        ],
    }

    for relative_path, expected_fragments in controller_expectations.items():
        source = read_text(relative_path)
        for fragment in expected_fragments:
            assert fragment in source, f"{relative_path} must keep {fragment}"


def test_existing_schema_and_menu_migrations_cover_current_edhr_surface() -> None:
    sql_expectations = {
        "sql/mysql/20260608_edhr_batch_execution_schema.sql": [
            "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_batch_execution`",
            "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_batch_execution_task`",
            "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_batch_execution_signature`",
            "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_batch_execution_archive`",
            "mes:pro-edhr-batch-execution:query",
            "mes:pro-edhr-batch-execution:create",
            "mes:pro-edhr-batch-execution:update",
            "mes:pro-edhr-batch-execution:close",
            "mes:pro-edhr-batch-execution:quality-reject",
        ],
        "sql/mysql/20260611_mes_edhr_work_task_flow.sql": [
            "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_work_task_assignment_rule`",
            "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_work_task`",
            "mes:pro-edhr-work-task:query",
            "mes:pro-edhr-work-task:update",
            "mes:pro-edhr-work-task-rule:query",
            "mes:pro-edhr-work-task-rule:update",
        ],
        "sql/mysql/20260612_mes_edhr_record_change_menu.sql": [
            "mes:pro-edhr-change:query",
            "mes:pro-edhr-change:void",
            "mes:pro-edhr-change:reopen",
            "mes:pro-edhr-change:supplement",
            "mes:pro-edhr-change:approve",
        ],
        "sql/mysql/20260615_mes_edhr_tail_four_goals.sql": [
            "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_operation_audit_event`",
            "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_permission_scope`",
            "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_permission_rule`",
            "mes:pro-edhr-operation-audit:query",
            "mes:pro-edhr-permission-scope:query",
            "mes:pro-edhr-permission-scope:save",
            "mes:pro-edhr-permission-scope:evaluate",
        ],
        "sql/mysql/20260526_edhr_field_audit_schema.sql": [
            "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_field_audit_batch`",
            "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_field_audit_item`",
            "mes:pro-batch-record-execution:field-audit-update",
            "mes:pro-batch-record-execution:field-audit-query",
            "mes:pro-batch-record-execution:field-audit-verify",
            "mes:pro-batch-record-execution:field-audit-export",
        ],
        "sql/mysql/20260528_edhr_domain_trace_schema.sql": [
            "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_domain_trace_snapshot`",
            "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_domain_trace_item`",
            "mes:pro-batch-record-execution:domain-trace-query",
            "mes:pro-batch-record-execution:domain-trace-verify",
        ],
        "sql/mysql/20260525_edhr_archive_schema.sql": [
            "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_archive`",
            "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_archive_event`",
        ],
        "sql/mysql/20260612_mes_edhr_attachment_ledger.sql": [
            "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_attachment`",
        ],
    }

    for relative_path, expected_fragments in sql_expectations.items():
        sql = read_text(relative_path)
        for fragment in expected_fragments:
            assert fragment in sql, f"{relative_path} must declare {fragment}"
