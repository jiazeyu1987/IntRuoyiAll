from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
DOMAIN_CONTRACT_DOC = ROOT / "docs/system/mes-scheduling-domain-contracts.md"
AUTO_SCHEDULE_SERVICE = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/schedule/MesProAutoScheduleServiceImpl.java"
)
SCHEDULE_ORDER_SERVICE = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/scheduleorder/MesProScheduleOrderServiceImpl.java"
)
CALENDAR_SERVICE = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/schedule/MesProScheduleCalendarServiceImpl.java"
)
CALENDAR_RULE_SUPPORT = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/schedule/MesProScheduleCalendarRuleSupport.java"
)
DEFAULT_POLICY = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/schedule/component/ScheduleDefaultCompatibilityPolicy.java"
)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_stage3_domain_contract_classifies_default_boundaries():
    text = read_text(DOMAIN_CONTRACT_DOC)

    required_snippets = [
        "### 默认值分类清单",
        "业务默认",
        "历史兼容读取",
        "必须失败",
        "默认排产配置 warning",
        "默认资源快照 warning",
        "`TenantUtils.executeIgnore`",
        "`catch ignored`",
        "`10.5h`",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_stage3_default_policy_has_named_business_and_legacy_boundaries():
    assert DEFAULT_POLICY.exists(), "阶段 3 必须集中命名默认值与历史兼容边界"

    policy = read_text(DEFAULT_POLICY)
    required_policy_snippets = [
        "class ScheduleDefaultCompatibilityPolicy",
        "businessDefaultCapacityMode",
        "businessDefaultPreserveManualLockedTasks",
        "historicalSnapshotScheduleQuantity",
        "historicalReadRouteMapIgnoreDeleted",
        "warnDefaultRouteScheduleConfig",
        "warnDefaultResourceSnapshot",
        "failFastMissingRouteScheduleConfig",
        "failFastMissingCalendarOrCapacity",
    ]
    for snippet in required_policy_snippets:
        assert snippet in policy

    auto_schedule = read_text(AUTO_SCHEDULE_SERVICE)
    schedule_order = read_text(SCHEDULE_ORDER_SERVICE)
    assert "ScheduleDefaultCompatibilityPolicy" in auto_schedule
    assert "scheduleDefaultCompatibilityPolicy.businessDefaultCapacityMode(" in auto_schedule
    assert "scheduleDefaultCompatibilityPolicy.historicalSnapshotScheduleQuantity(" in auto_schedule
    assert "ScheduleDefaultCompatibilityPolicy" in schedule_order
    assert "scheduleDefaultCompatibilityPolicy.warnDefaultRouteScheduleConfig(" in schedule_order


def test_stage3_historical_route_reads_are_not_direct_tenant_ignore_calls():
    policy = read_text(DEFAULT_POLICY)
    calendar_service = read_text(CALENDAR_SERVICE)

    assert "TenantUtils.executeIgnore(callable)" in policy
    assert "ScheduleDefaultCompatibilityPolicy" in calendar_service
    assert "scheduleDefaultCompatibilityPolicy.historicalReadRouteMapIgnoreDeleted(" in calendar_service
    assert "TenantUtils.executeIgnore" not in calendar_service


def test_stage3_calendar_parse_compatibility_is_named_not_ignored():
    rule_support = read_text(CALENDAR_RULE_SUPPORT)

    assert "catch (Exception ignored)" not in rule_support
    assert "catch (DateTimeParseException parseFailure)" in rule_support


def test_stage3_missing_formal_prerequisites_remain_blocking():
    auto_schedule = read_text(AUTO_SCHEDULE_SERVICE)
    schedule_order = read_text(SCHEDULE_ORDER_SERVICE)

    blocking_snippets = [
        "PRO_AUTO_SCHEDULE_CALENDAR_REQUIRED",
        "PRO_AUTO_SCHEDULE_CAPACITY_REQUIRED",
        "PRO_AUTO_SCHEDULE_ACTUAL_CAPACITY_REQUIRED",
        "BLOCKED_ROUTE_SCHEDULE_CONFIG_MISSING",
        "BLOCKED_INVALID_FINITE_CAPACITY",
        "BLOCKED_CALENDAR_RULE_MISSING",
    ]
    combined = auto_schedule + "\n" + schedule_order
    for snippet in blocking_snippets:
        assert snippet in combined

    forbidden_new_success_defaults = [
        "defaultSuccess",
        "mockSuccess",
        "ignoreMissingCalendar",
        "ignoreMissingCapacity",
        "autoCreateDefaultRoute",
        "autoCreateDefaultCapacity",
    ]
    for snippet in forbidden_new_success_defaults:
        assert snippet not in combined
