from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260714_mes_edhr_version_governance_menu_removal.sql"
CONTROLLER_PATH = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "controller"
    / "admin"
    / "pro"
    / "batchrecord"
    / "MesProBatchRecordVersionGovernanceController.java"
)
SERVICE_PATH = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "service"
    / "pro"
    / "batchrecord"
    / "MesProBatchRecordVersionGovernanceService.java"
)


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR version governance menu removal SQL must exist."
    return SQL_PATH.read_text(encoding="utf-8")


def executable_sql(sql: str) -> str:
    return "\n".join(
        line for line in sql.splitlines()
        if not line.lstrip().startswith("--")
    ).upper()


def test_version_governance_page_menu_is_soft_removed_without_backend_permission_loss() -> None:
    sql = read_sql()

    for required in [
        "release-migration: allowedEnvironments=test,backup,prod",
        "dependsOn=20260708_mes_batch_record_version_phase_one",
        "type=menu",
        "900303",
        "eDHR版本治理",
        "/mes/pro/feedback/edhr-version-governance",
        "mes/pro/edhr-version-governance/VersionGovernancePage",
        "MesProEdhrVersionGovernancePage",
        "mes:pro-batch-record-version:governance-query",
        "`deleted` = b'1'",
        "`visible` = b'0'",
    ]:
        assert required in sql

    for retained_permission in [
        "mes:pro-batch-record-version:confirm",
        "mes:pro-batch-record-version:import",
        "mes:pro-batch-record-version:rollback-request",
    ]:
        assert retained_permission in sql

    assert "parent_id` = 900220" in sql or "`parent_id` = 900220" in sql


def test_version_governance_menu_removal_updates_role_and_package_bindings_idempotently() -> None:
    sql = read_sql()

    for required in [
        "system_role_menu",
        "system_tenant_package",
        "JSON_VALID",
        "JSON_TABLE",
        "JSON_ARRAYAGG",
        "tmp_mes_edhr_version_governance_package_menu_ids",
        "ensure_mes_edhr_version_governance_menu_removed",
        "SIGNAL SQLSTATE '45000'",
    ]:
        assert required in sql

    upper_sql = executable_sql(sql)
    assert "DELETE FROM" not in upper_sql
    assert "TRUNCATE" not in upper_sql
    assert "DROP TABLE" not in upper_sql


def test_backend_version_governance_controller_and_service_are_retained() -> None:
    controller = CONTROLLER_PATH.read_text(encoding="utf-8")
    service = SERVICE_PATH.read_text(encoding="utf-8")

    assert '@RequestMapping("/mes/pro/batch-record-version/governance")' in controller

    for required_endpoint in [
        '@GetMapping("/summary")',
        '@GetMapping("/impact")',
        '@GetMapping("/inspection")',
        '@GetMapping("/metrics")',
        '@PostMapping("/rollback/request")',
        '@GetMapping("/migration-diff")',
        '@PostMapping("/migration-confirm")',
        '@PostMapping("/draft-reupload")',
    ]:
        assert required_endpoint in controller

    for required_method in [
        "getSummary",
        "getImpact",
        "getInspection",
        "getMetrics",
        "requestRollback",
        "getMigrationDiff",
        "confirmMigrationItems",
        "reuploadDraft",
    ]:
        assert required_method in service
