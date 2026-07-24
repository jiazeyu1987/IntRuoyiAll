from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


IDENTITY_PACKAGE = (
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
    / "schedule"
    / "identity"
)
AUTO_SCHEDULE_SERVICE = (
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
    / "schedule"
    / "MesProAutoScheduleServiceImpl.java"
)
SCHEDULE_ORDER_SERVICE = (
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
    / "scheduleorder"
    / "MesProScheduleOrderServiceImpl.java"
)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_stage_1_declares_named_identity_key_objects() -> None:
    expected_files = {
        "RouteProcessIdentity.java": [
            "record RouteProcessIdentity",
            "routeVersionId",
            "routeProcessId",
            "availabilityKey",
            "ROUTE_PROCESS_",
        ],
        "ScheduleOrderProcessIdentity.java": [
            "record ScheduleOrderProcessIdentity",
            "scheduleOrderId",
            "routeProcessId",
            "workOrderProcessKey",
            "scheduleOrderProcessTaskKey",
            "_SOP_",
        ],
        "TaskAttributionIdentity.java": [
            "record TaskAttributionIdentity",
            "taskId",
            "scheduleOrderProcessId",
            "scheduleOrderId",
            "workOrderId",
        ],
        "LineProcessIdentity.java": [
            "record LineProcessIdentity",
            "lineId",
            "processId",
            "availabilityKey",
        ],
    }

    for file_name, snippets in expected_files.items():
        path = IDENTITY_PACKAGE / file_name
        assert path.exists(), f"missing identity object {file_name}"
        text = read_text(path)
        for snippet in snippets:
            assert snippet in text, f"{file_name} missing {snippet}"


def test_stage_1_routes_high_risk_keys_through_identity_objects() -> None:
    auto_schedule_text = read_text(AUTO_SCHEDULE_SERVICE)
    schedule_order_text = read_text(SCHEDULE_ORDER_SERVICE)

    required_auto_schedule_snippets = [
        "import cn.iocoder.yudao.module.mes.service.pro.schedule.identity.LineProcessIdentity;",
        "import cn.iocoder.yudao.module.mes.service.pro.schedule.identity.RouteProcessIdentity;",
        "import cn.iocoder.yudao.module.mes.service.pro.schedule.identity.ScheduleOrderProcessIdentity;",
        "LineProcessIdentity.availabilityKey(lineId, processId)",
        "RouteProcessIdentity.availabilityKey(routeProcessId)",
        "RouteProcessIdentity.legacyAvailabilityKey(routeId, processId)",
        "ScheduleOrderProcessIdentity.workOrderProcessKey(workOrderId, processId)",
        "ScheduleOrderProcessIdentity.scheduleOrderProcessTaskKey(workOrderId, scheduleOrderProcessId)",
    ]
    required_schedule_order_snippets = [
        "import cn.iocoder.yudao.module.mes.service.pro.schedule.identity.RouteProcessIdentity;",
        "Map<RouteProcessIdentity, List<MesProScheduleOrderProcessDO>> wipProcessesByRouteProcess",
        "private RouteProcessIdentity resolveRouteProcessWipKey",
        "Long currentRouteProcessId = routeProcessService.resolveCurrentRouteProcess(",
        "return RouteProcessIdentity.of(scheduleOrder.getRouteId(), routeVersionId, currentRouteProcessId);",
    ]
    forbidden_snippets = [
        "private record RouteProcessWipKey",
        "return lineId + \"_\" + processId;",
        "return \"ROUTE_PROCESS_\" + routeProcessId;",
        "return \"ROUTE_PROCESS_\" + routeId + \"_\" + processId;",
        "return workOrderId + \"_\" + processId;",
        "return workOrderId + \"_SOP_\" + scheduleOrderProcessId;",
    ]

    for snippet in required_auto_schedule_snippets:
        assert snippet in auto_schedule_text
    for snippet in required_schedule_order_snippets:
        assert snippet in schedule_order_text
    for snippet in forbidden_snippets:
        assert snippet not in auto_schedule_text
        assert snippet not in schedule_order_text
