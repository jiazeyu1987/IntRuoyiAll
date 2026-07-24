import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVICE = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/schedule/MesProAutoScheduleServiceImpl.java"
)
TOPOLOGY_RESOLVER = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/schedule/component/ScheduleTopologyResolver.java"
)
APPLY_GUARD = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/schedule/component/ScheduleApplyGuard.java"
)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def method_body(source: str, method_name: str) -> str:
    pattern = re.compile(rf"\b(?:public|private|protected)\s+[\w<>, ?]+\s+{method_name}\s*\(")
    match = pattern.search(source)
    assert match, f"method declaration not found: {method_name}"
    start = match.start()
    brace = source.index("{", start)
    depth = 0
    for index in range(brace, len(source)):
        char = source[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return source[start : index + 1]
    raise AssertionError(f"method body not closed: {method_name}")


def test_stage2_extracts_topology_resolver_component():
    assert TOPOLOGY_RESOLVER.exists(), "阶段 2 必须抽取 ScheduleTopologyResolver 组件"

    topology_source = read_text(TOPOLOGY_RESOLVER)
    assert "class ScheduleTopologyResolver" in topology_source
    assert "validateRouteProcessTopologySnapshot(" in topology_source
    assert "orderRouteProcessesByDependency(" in topology_source
    assert "排产工序拓扑快照无效" in topology_source
    assert "排产工序直接前置快照不存在" in topology_source

    service_source = read_text(SERVICE)
    assert "ScheduleTopologyResolver" in service_source
    assert "scheduleTopologyResolver.validateRouteProcessTopologySnapshot" in service_source
    assert "scheduleTopologyResolver.orderRouteProcessesByDependency" in service_source


def test_stage2_apply_still_recomputes_and_validates_calendar_token():
    assert APPLY_GUARD.exists(), "阶段 2 必须抽取 ScheduleApplyGuard 组件"
    guard_source = read_text(APPLY_GUARD)
    assert "class ScheduleApplyGuard" in guard_source
    assert "validateCalendarContextTokenProvided(" in guard_source
    assert "validateCalendarContextToken(" in guard_source
    assert "PRO_AUTO_SCHEDULE_CALENDAR_CONTEXT_REQUIRED" in guard_source
    assert "PRO_AUTO_SCHEDULE_CALENDAR_CONTEXT_CHANGED" in guard_source

    service_source = read_text(SERVICE)
    apply_body = method_body(service_source, "applyInternal")

    assert "scheduleApplyGuard.validateCalendarContextTokenProvided(reqVO.getCalendarContextToken())" in apply_body
    assert "computeSchedule(reqVO, true)" in apply_body
    assert "scheduleApplyGuard.validateCalendarContextToken(" in apply_body
    assert apply_body.index("computeSchedule(reqVO, true)") < apply_body.index(
        "scheduleApplyGuard.validateCalendarContextToken("
    )
