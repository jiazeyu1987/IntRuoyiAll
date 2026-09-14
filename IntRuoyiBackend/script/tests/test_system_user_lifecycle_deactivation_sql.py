from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260830_system_user_lifecycle_deactivation.sql"
TEST_TABLE_SQL_PATH = ROOT / "yudao-module-system" / "src" / "test" / "resources" / "sql" / "create_tables.sql"
SYSTEM_MAIN = ROOT / "yudao-module-system" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "system"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_user_lifecycle_deactivation_migration_adds_required_columns() -> None:
    sql = read(SQL_PATH)

    assert sql.startswith("-- release-migration:")
    assert "CREATE PROCEDURE intruoyi_add_system_users_lifecycle_column" in sql
    for column in [
        "lifecycle_document_type",
        "lifecycle_document_no",
        "lifecycle_document_time",
        "lifecycle_effective_time",
        "lifecycle_deactivated_time",
    ]:
        assert f"CALL intruoyi_add_system_users_lifecycle_column('{column}'" in sql
    assert "ALTER TABLE `system_users`\n    ADD COLUMN" not in sql


def test_user_lifecycle_deactivation_job_uses_handler_name_business_key() -> None:
    sql = read(SQL_PATH)
    job_sql = sql.split("INSERT INTO `infra_job`", 1)[1]

    assert "'userLifecycleDeactivateJob'" in job_sql
    assert "WHERE `handler_name` = 'userLifecycleDeactivateJob'" in job_sql
    assert not re.search(r"INSERT\s+INTO\s+`infra_job`\s*\(\s*`id`\s*,", job_sql, re.IGNORECASE)
    assert not re.search(r"\b(id|`id`)\s*=\s*56\d{2}\b", job_sql, re.IGNORECASE)


def test_system_users_test_schema_contains_lifecycle_columns() -> None:
    sql = read(TEST_TABLE_SQL_PATH)

    for column in [
        '"lifecycle_document_type"',
        '"lifecycle_document_no"',
        '"lifecycle_document_time"',
        '"lifecycle_effective_time"',
        '"lifecycle_deactivated_time"',
    ]:
        assert column in sql


def test_lifecycle_deactivation_backend_contract_is_wired() -> None:
    controller = read(SYSTEM_MAIN / "controller" / "admin" / "user" / "UserController.java")
    service = read(SYSTEM_MAIN / "service" / "user" / "AdminUserServiceImpl.java")
    job = read(SYSTEM_MAIN / "job" / "user" / "UserLifecycleDeactivateJob.java")

    assert '@PutMapping("/lifecycle-deactivation")' in controller
    assert "recordUserLifecycleDeactivation(reqVO)" in controller
    assert "CommonStatusEnum.isEnable(status) && user.getLifecycleDeactivatedTime() != null" in service
    assert "USER_LIFECYCLE_DEACTIVATED_ENABLE_FORBIDDEN" in service
    assert '@Component("userLifecycleDeactivateJob")' in job
    assert "implements JobHandler" in job
    assert "@TenantJob" in job
    assert "processDueLifecycleDeactivations(LocalDateTime.now(), limit)" in job


def test_lifecycle_deactivation_job_requires_explicit_positive_limit() -> None:
    job = read(SYSTEM_MAIN / "job" / "user" / "UserLifecycleDeactivateJob.java")

    assert "DEFAULT_LIMIT" not in job
    assert 'getInt("limit", ' not in job
    assert "param == null || param.isBlank()" in job
    assert "limit == null || limit <= 0" in job
