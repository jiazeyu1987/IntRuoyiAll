import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
FIXTURE_SQL = (
    REPO_ROOT.parent
    / "doc"
    / "tasks"
    / "20260831-edhr-pdfa-int-main-e2e"
    / "fixtures"
    / "edhr-pdfa-int-main-e2e-runtime-control-permission.sql"
)
ARCHIVER_FIXTURE_SQL = (
    REPO_ROOT.parent
    / "doc"
    / "tasks"
    / "20260831-edhr-pdfa-int-main-e2e"
    / "fixtures"
    / "edhr-pdfa-int-main-e2e-archiver-login.sql"
)
WORK_TASK_MAPPER = (
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
    / "dal"
    / "mysql"
    / "pro"
    / "batchrecord"
    / "MesProEdhrWorkTaskMapper.java"
)


def read_fixture() -> str:
    assert FIXTURE_SQL.exists(), f"missing fixture SQL: {FIXTURE_SQL}"
    return FIXTURE_SQL.read_text(encoding="utf-8")


def read_archiver_fixture() -> str:
    assert ARCHIVER_FIXTURE_SQL.exists(), f"missing fixture SQL: {ARCHIVER_FIXTURE_SQL}"
    return ARCHIVER_FIXTURE_SQL.read_text(encoding="utf-8")


def read_work_task_mapper() -> str:
    assert WORK_TASK_MAPPER.exists(), f"missing work task mapper: {WORK_TASK_MAPPER}"
    return WORK_TASK_MAPPER.read_text(encoding="utf-8")


def test_fixture_targets_only_test_tenant_runtime_control_permission() -> None:
    text = read_fixture()

    assert "`tenant`.`name` = '测试租户'" in text
    assert "`role`.`code` = 'super_admin'" in text
    assert "infra:runtime-control:query" in text
    assert "infra/runtime-control/index" in text
    assert "InfraRuntimeControl" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "JSON_TABLE(" in text
    assert "JSON_ARRAYAGG" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "WHERE NOT EXISTS (" in text
    assert "SIGNAL SQLSTATE '45000'" in text


def test_fixture_avoids_broad_or_fallback_permission_patterns() -> None:
    upper_text = read_fixture().upper()

    assert "DELETE FROM `SYSTEM_ROLE_MENU`" not in upper_text
    assert "DELETE FROM SYSTEM_ROLE_MENU" not in upper_text
    assert "TRUNCATE TABLE" not in upper_text
    assert "INSERT IGNORE INTO `SYSTEM_ROLE_MENU`" not in upper_text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text
    assert "`TENANT`.`ID` IN" not in upper_text
    assert "1=1" not in upper_text


def test_archiver_login_fixture_copies_default_login_hash_without_plaintext_secret() -> None:
    text = read_archiver_fixture()

    assert "`source_tenant`.`name` = '芋道源码'" in text
    assert "`target_tenant`.`name` = '测试租户'" in text
    assert "`source_user`.`username` = 'admin'" in text
    assert "`target_user`.`username` IN ('aoteman', 'xujianhai')" in text
    assert "SET `target_user`.`password` = `source_user`.`password`" in text
    assert "`target_user`.`password_update_time` = NOW()" in text
    assert "affected_rows <> 2" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "ROW_COUNT()" in text


def test_archiver_login_fixture_avoids_broad_or_plaintext_password_patterns() -> None:
    upper_text = read_archiver_fixture().upper()

    assert "ADMIN123" not in upper_text
    assert "PASSWORD` = '" not in upper_text
    assert "PASSWORD = '" not in upper_text
    assert "INSERT INTO `SYSTEM_USERS`" not in upper_text
    assert "DELETE FROM `SYSTEM_USERS`" not in upper_text
    assert "TRUNCATE TABLE" not in upper_text
    assert "`TENANT`.`ID` IN" not in upper_text
    assert "1=1" not in upper_text


def test_archive_todo_visibility_allows_closed_batch_until_archive_is_sealed() -> None:
    text = read_work_task_mapper()

    assert 'TASK_TYPE_ARCHIVE = "ARCHIVE"' in text
    assert 'TERMINAL_BATCH_STATUS_SQL = "30, 40, 50, 60"' in text
    assert 'ARCHIVE_TODO_EXCLUDED_BATCH_STATUS_SQL = "40, 50, 60"' in text
    assert re.search(
        r"selectMyPage[\s\S]*applyOpenWorkTaskBatchVisibility\([^;]*reqVO\.getTaskType\(\)",
        text,
    )
    assert re.search(
        r"countMy[\s\S]*applyOpenWorkTaskBatchVisibility\([^;]*taskType",
        text,
    )
    assert re.search(
        r"selectCandidateTodoPage[\s\S]*applyOpenWorkTaskBatchVisibility\([^;]*reqVO\.getTaskType\(\)",
        text,
    )
    assert "excludeTerminalBatchWrapper(baseMyWrapper" not in text
