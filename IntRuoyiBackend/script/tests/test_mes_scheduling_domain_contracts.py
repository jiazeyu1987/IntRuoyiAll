from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


DOMAIN_CONTRACT_DOC = REPO_ROOT / "docs" / "system" / "mes-scheduling-domain-contracts.md"
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
SCHEDULE_TOPOLOGY_RESOLVER = (
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
    / "component"
    / "ScheduleTopologyResolver.java"
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
ROUTE_CONFIG_SERVICE = (
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
    / "route"
    / "MesProRouteScheduleConfigServiceImpl.java"
)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_mes_scheduling_domain_contract_doc_declares_risk_guardrails() -> None:
    assert DOMAIN_CONTRACT_DOC.exists()
    text = read_text(DOMAIN_CONTRACT_DOC)

    required_snippets = [
        "# MES 排产域契约",
        "## 排产域词典",
        "## 身份优先级",
        "## 身份口径判定矩阵",
        "routeProcessId 是路线工序身份",
        "processId 是基础工序身份",
        "排产工单快照",
        "当前路线工序",
        "受保护任务",
        "算法变更门禁",
        "工序身份变更门禁",
        "日历产能变更门禁",
        "工作台最近一次排产口径门禁",
        "报工联动变更门禁",
        "应用重排必须重新计算",
        "禁止用 processId 跨路线合并",
        "默认值必须明确是业务默认还是历史兼容",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_mes_scheduling_domain_glossary_declares_identity_matrix() -> None:
    text = read_text(DOMAIN_CONTRACT_DOC)

    required_terms = [
        "生产工单",
        "排产工单",
        "排产工序快照",
        "路线工序",
        "基础工序",
        "当前路线工序",
        "历史快照工序",
        "生产任务",
        "任务扩展",
        "报工记录",
        "日历规则",
        "产能计划",
        "资源池",
        "受保护任务",
        "业务主身份",
        "辅助身份",
        "禁止行为",
        "必须验证",
        "routeVersionId + routeProcessId",
        "workOrderId + routeProcessId",
        "scheduleOrderId + routeProcessId",
    ]

    for term in required_terms:
        assert term in text


def test_mes_scheduling_core_keeps_source_guardrail_patterns() -> None:
    auto_schedule_text = read_text(AUTO_SCHEDULE_SERVICE)
    topology_text = read_text(SCHEDULE_TOPOLOGY_RESOLVER)
    auto_schedule_and_components_text = auto_schedule_text + "\n" + topology_text
    schedule_order_text = read_text(SCHEDULE_ORDER_SERVICE)
    route_config_text = read_text(ROUTE_CONFIG_SERVICE)

    auto_schedule_snippets = [
        "scheduleApplyGuard.validateCalendarContextTokenProvided(reqVO.getCalendarContextToken());",
        "scheduleApplyGuard.validateCalendarContextToken(reqVO.getCalendarContextToken(),",
        "buildLinkPlans(computation);",
        "predecessorRouteProcessId",
        "dependencyAvailableFrom",
        "routeProcessAvailabilityKey(routeProcess, scheduleOrderProcess)",
        "return RouteProcessIdentity.availabilityKey(routeProcessId);",
        "return RouteProcessIdentity.legacyAvailabilityKey(routeId, processId);",
        "return LineProcessIdentity.availabilityKey(lineId, processId);",
        "lineProcessKey(lineId, routeProcess.getProcessId())",
    ]
    schedule_order_snippets = [
        "import cn.iocoder.yudao.module.mes.service.pro.schedule.identity.RouteProcessIdentity;",
        "private RouteProcessIdentity resolveRouteProcessWipKey",
        "return RouteProcessIdentity.of(scheduleOrder.getRouteId(), routeVersionId, currentRouteProcessId);",
        "buildRouteProcessPredecessorMap(route.getId(), routeProcesses)",
        "selectByRouteVersionIdAndRouteProcessId(routeVersionId, routeProcessId)",
        "isDefaultScheduleConfig(scheduleConfig)",
        "isDefaultScheduleProcess(process)",
        "private List<MesProScheduleOrderDO> selectLatestProcessWipScheduleOrders()",
        "getLatestSuccessfulApplyScheduleOrderIds()",
        "filter(order -> latestApplyScheduleOrderIds.contains(order.getId()))",
    ]
    route_config_snippets = [
        "config.setItemId(null);",
        "selectByRouteVersionIdAndRouteProcessId(routeVersion.getId(), routeProcessId)",
        "throw exception(PRO_ROUTE_SCHEDULE_CONFIG_REQUIRED",
    ]

    for snippet in auto_schedule_snippets:
        assert snippet in auto_schedule_and_components_text
    for snippet in schedule_order_snippets:
        assert snippet in schedule_order_text
    for snippet in route_config_snippets:
        assert snippet in route_config_text
