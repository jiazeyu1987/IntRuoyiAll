from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CONTROLLER = ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/task/MesProTaskController.java"
SCHEDULE_ORDER_MAPPER = ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/scheduleorder/MesProScheduleOrderMapper.java"
SCHEDULE_ORDER_PROCESS_MAPPER = ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/scheduleorder/MesProScheduleOrderProcessMapper.java"


def require_contains(source: str, needle: str, message: str) -> None:
    if needle not in source:
        raise AssertionError(message)


def main() -> None:
    controller = CONTROLLER.read_text(encoding="utf-8")
    schedule_order_mapper = SCHEDULE_ORDER_MAPPER.read_text(encoding="utf-8")
    mapper = SCHEDULE_ORDER_PROCESS_MAPPER.read_text(encoding="utf-8")

    require_contains(
        controller,
        "import cn.hutool.core.util.StrUtil;",
        "gantt controller must use explicit blank validation for process names.",
    )
    require_contains(
        controller,
        "import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;",
        "gantt controller must load schedule-order process snapshots.",
    )
    require_contains(
        controller,
        "import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;",
        "gantt controller must load schedule orders to match the current schedule-order pool.",
    )
    require_contains(
        controller,
        "import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProReplanExplanationRespVO;",
        "gantt controller must read the latest applied replan scope from the replan explanation snapshot.",
    )
    require_contains(
        controller,
        "import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProReplanExplanationSnapshotMapper;",
        "gantt controller must inject the latest replan explanation snapshot mapper.",
    )
    require_contains(
        controller,
        "import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;",
        "gantt controller must use the schedule-order status enum for active membership.",
    )
    require_contains(
        controller,
        "import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;",
        "gantt controller must inject the schedule-order process mapper.",
    )
    require_contains(
        controller,
        "import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;",
        "gantt controller must inject the schedule-order mapper.",
    )
    require_contains(
        controller,
        "private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;",
        "gantt controller must have a schedule-order process mapper dependency.",
    )
    require_contains(
        controller,
        "private MesProScheduleOrderMapper scheduleOrderMapper;",
        "gantt controller must have a schedule-order mapper dependency.",
    )
    require_contains(
        controller,
        "private MesProReplanExplanationSnapshotMapper replanExplanationSnapshotMapper;",
        "gantt controller must have the latest replan explanation snapshot dependency.",
    )
    require_contains(
        controller,
        "Map<Long, MesProScheduleOrderDO> baseActiveScheduleOrderMap = convertMap(",
        "gantt-list must build the current active schedule-order pool before filtering tasks.",
    )
    require_contains(
        controller,
        "scheduleOrderMapper.selectListByIds(",
        "gantt-list must load schedule orders by task extension scheduleOrderId.",
    )
    require_contains(
        controller,
        "MesProTaskScheduleExtDO::getScheduleOrderId",
        "gantt-list must derive schedule-order ids from task schedule extensions.",
    )
    require_contains(
        controller,
        ".filter(this::isCurrentGanttScheduleOrder)",
        "gantt-list must filter schedule orders to the active current pool.",
    )
    require_contains(
        controller,
        "Set<Long> latestAppliedReplanScheduleOrderIds = resolveLatestAppliedReplanScheduleOrderIds();",
        "gantt-list must resolve the latest successful replan schedule-order scope.",
    )
    require_contains(
        controller,
        "Map<Long, MesProScheduleOrderDO> activeScheduleOrderMap = CollUtil.isEmpty(latestAppliedReplanScheduleOrderIds)",
        "gantt-list must build one final active schedule-order scope after applying the latest replan filter.",
    )
    require_contains(
        controller,
        ".filter(entry -> latestAppliedReplanScheduleOrderIds.contains(entry.getKey()))",
        "gantt-list must narrow active schedule orders to the latest successful replan scope.",
    )
    require_contains(
        controller,
        "scheduleOrderProcessMapper.selectListByIds(",
        "gantt-list must batch-load schedule-order process snapshots by extension ids.",
    )
    require_contains(
        controller,
        "MesProTaskScheduleExtDO::getScheduleOrderProcessId",
        "gantt-list must derive snapshot ids from task schedule extensions.",
    )
    require_contains(
        controller,
        ".setProcess(resolveGanttTaskProcessName(task, taskExt, scheduleOrderProcessMap))",
        "task rows must resolve process names through the authoritative gantt helper.",
    )
    require_contains(
        controller,
        "List<MesProTaskDO> scheduledTasks = allTasks.stream()",
        "gantt-list must restrict the current schedule view to tasks that participated in scheduling.",
    )
    require_contains(
        controller,
        "isScheduledGanttProcessTask(taskExtMap.get(task.getId()), activeScheduleOrderMap)",
        "gantt-list must define scheduled participation from task schedule extensions and active schedule orders.",
    )
    require_contains(
        controller,
        "Map<Long, List<MesProTaskDO>> scheduledTaskMap",
        "gantt-list work-order project rows must be based on scheduled task grouping.",
    )
    require_contains(
        controller,
        "List<MesProWorkOrderDO> scheduledWorkOrders = workOrders.stream()",
        "gantt-list must filter out work orders that have no scheduled task rows.",
    )
    require_contains(
        controller,
        "if (CollUtil.isEmpty(woTasks))",
        "gantt-list must skip work-order project rows without scheduled tasks.",
    )
    require_contains(
        controller,
        ".setScheduleOrderProcessId(taskExt.getScheduleOrderProcessId())",
        "task rows must expose scheduleOrderProcessId for scheduled-only verification.",
    )
    require_contains(
        controller,
        ".setScheduleOrderId(taskExt.getScheduleOrderId())",
        "task rows must expose scheduleOrderId for latest-replan-scope verification.",
    )
    require_contains(
        controller,
        ".setScheduleOrderId(resolveWorkOrderScheduleOrderId(woTasks, taskExtMap))",
        "work-order project rows must expose scheduleOrderId from their scheduled child tasks.",
    )
    require_contains(
        controller,
        "GanttTaskRange workOrderRange = resolveWorkOrderGanttRange(woTasks);",
        "work-order project rows must aggregate schedule time from scheduled child tasks.",
    )
    require_contains(
        controller,
        ".setStartDate(workOrderRange.startDate()).setEndDate(workOrderRange.endDate())",
        "work-order project rows must expose aggregate start/end dates so legacy non-process tasks still produce visible summaries.",
    )
    require_contains(
        controller,
        "if (!isScheduledGanttProcessTask(taskExt, activeScheduleOrderMap))",
        "tasks without schedule participation must not be emitted as current schedule rows.",
    )
    require_contains(
        controller,
        "Map<Long, MesProScheduleOrderDO> activeScheduleOrderMap)",
        "gantt controller must centralize the scheduled participation guard.",
    )
    require_contains(
        controller,
        "taskExt.getScheduleOrderId() != null",
        "gantt controller must require schedule-order id snapshots for every task row.",
    )
    require_contains(
        controller,
        "activeScheduleOrderMap.containsKey(taskExt.getScheduleOrderId())",
        "gantt controller must only keep task rows that belong to the active schedule-order pool.",
    )
    require_contains(
        controller,
        "private boolean isCurrentGanttScheduleOrder(MesProScheduleOrderDO scheduleOrder)",
        "gantt controller must centralize active schedule-order membership.",
    )
    require_contains(
        controller,
        "replanExplanationSnapshotMapper.selectLatest()",
        "gantt controller must keep refreshes scoped to the latest successful replan snapshot.",
    )
    require_contains(
        controller,
        "MesProReplanExplanationRespVO.OrderItem::getScheduleOrderId",
        "gantt controller must derive latest scope from schedule-order ids, not work-order ids or product labels.",
    )
    require_contains(
        controller,
        "MesProScheduleOrderStatusEnum.FINISHED.getStatus()",
        "gantt controller must exclude finished schedule orders from the current schedule view.",
    )
    require_contains(
        controller,
        "MesProScheduleOrderStatusEnum.CANCELED.getStatus()",
        "gantt controller must exclude canceled schedule orders from the current schedule view.",
    )
    require_contains(
        controller,
        "scheduleOrderProcess.getScheduleOrderId(), taskExt.getScheduleOrderId()",
        "gantt controller must verify process snapshots belong to the task's active schedule order.",
    )
    require_contains(
        controller,
        "生产排产甘特图任务排产工单和工序快照不一致",
        "schedule-order/process mismatches must fail fast instead of leaking stale rows.",
    )
    require_contains(
        controller,
        "private String resolveGanttTaskProcessName(MesProTaskDO task, MesProTaskScheduleExtDO taskExt,",
        "gantt controller must centralize process-name resolution.",
    )
    require_contains(
        controller,
        "throw new IllegalStateException(\"生产排产甘特图任务引用不存在的排产工序快照",
        "missing schedule-order process snapshots must fail fast instead of returning null process.",
    )
    require_contains(
        controller,
        "throw new IllegalStateException(\"生产排产甘特图任务缺少工序名称",
        "missing process names must fail fast instead of returning null process.",
    )

    require_contains(
        mapper,
        "default List<MesProScheduleOrderProcessDO> selectListByIds(Collection<Long> ids)",
        "schedule-order process mapper must support batch loading by ids.",
    )
    require_contains(
        mapper,
        ".in(MesProScheduleOrderProcessDO::getId, ids)",
        "schedule-order process mapper batch loader must query ids explicitly.",
    )
    require_contains(
        schedule_order_mapper,
        "default List<MesProScheduleOrderDO> selectListByIds(Collection<Long> ids)",
        "schedule-order mapper must support batch loading by ids.",
    )

    print("PASS: mes pro task gantt process contract")


if __name__ == "__main__":
    main()
