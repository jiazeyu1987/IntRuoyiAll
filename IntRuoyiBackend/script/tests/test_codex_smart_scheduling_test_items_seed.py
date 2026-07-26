from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = REPO_ROOT / "sql" / "mysql" / "20260726_system_codex_smart_scheduling_test_items.sql"

CASE_NAMES = (
    "智能排产-全链路冒烟：入池、预览、发布、日历、报工闭环",
    "智能排产-只读一致性：工作台、排产工单、排程日历",
    "智能排产-可点击安全巡检：危险写入必须显式确认",
)

EXPECTED_CHECKPOINTS = {
    CASE_NAMES[0]: (
        "生产工单入池并保留来源快照",
        "自动排产预览返回工序快照和阻断明细",
        "确认发布后排产工单状态与排程日历同步",
        "第三方报工导入和归因不绕过审批",
        "审批通过后排产进度按工序回写",
    ),
    CASE_NAMES[1]: (
        "工作台八项指标可见且接口成功",
        "排产工单列表展示分层进度字段",
        "排程日历展示班次、短缺和锁定状态",
        "只读模式不得产生 MES 写请求",
    ),
    CASE_NAMES[2]: (
        "智能排产页面入口全部可访问",
        "危险写入按钮只允许打开确认入口",
        "安全按钮不得触发 MES 写请求",
        "检查结果必须记录页面和按钮明细",
    ),
}


def migration_text() -> str:
    return MIGRATION.read_text(encoding="utf-8")


def test_smart_scheduling_test_items_migration_exists_with_release_metadata() -> None:
    sql = migration_text()
    first_line = sql.splitlines()[0]

    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260724_system_codex_test_management; type=seed; riskLevel=low"
    )
    assert "CREATE PROCEDURE ensure_system_codex_smart_scheduling_test_items" in sql
    assert "CALL ensure_system_codex_smart_scheduling_test_items();" in sql
    assert "DROP PROCEDURE IF EXISTS ensure_system_codex_smart_scheduling_test_items;" in sql


def test_smart_scheduling_test_items_seed_required_cases_and_runner_guardrails() -> None:
    sql = migration_text()
    normalized = sql.replace("`", "").lower()

    for case_name in CASE_NAMES:
        assert case_name in sql

    assert "smart-scheduling-smoke-real-flow.e2e.js" in sql
    assert "smart-scheduling-target-alignment-readonly.e2e.js" in sql
    assert "smart-scheduling-clickable-coverage.e2e.js" in sql
    assert "playwright" in normalized
    assert "真实页面" in sql
    assert "api-only" in normalized
    assert "runner 本地凭据映射" in sql
    assert "111111" not in sql
    assert "password" not in normalized
    assert "parallel_safe" in normalized
    assert "project" in normalized
    assert "'智能排产'" in sql
    assert "b'0'" in normalized
    assert "default_execution_mode" in normalized
    assert "'sequential'" in normalized


def test_smart_scheduling_test_items_seed_checkpoints_are_complete() -> None:
    sql = migration_text()

    for case_name, checkpoints in EXPECTED_CHECKPOINTS.items():
        assert case_name in sql
        for checkpoint in checkpoints:
            assert checkpoint in sql

    assert "system_codex_test_checkpoint" in sql
    assert "severity" in sql
    assert "CRITICAL" in sql
    assert "MAJOR" in sql
