package cn.iocoder.yudao.module.mes.controller.admin.pro.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProReplanExplanationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttDataRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.MesProTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.MesProTaskLockReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.MesProTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.MesProTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.client.MesMdClientDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProReplanExplanationSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProReplanExplanationSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.enums.MesBizTypeConstants;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.client.MesMdClientService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.common.util.collection.MapUtils.findAndThen;

@Tag(name = "管理后台 - MES 生产任务")
@RestController
@RequestMapping("/mes/pro/task")
@Validated
public class MesProTaskController {

    @Resource
    private MesProTaskService taskService;
    @Resource
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesMdWorkstationService workstationService;
    @Resource
    private MesMdProductionLineService productionLineService;
    @Resource
    private MesProProcessService processService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesMdClientService clientService;
    @Resource
    private MesMdUnitMeasureService unitMeasureService;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Resource
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Resource
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Resource
    private MesProReplanExplanationSnapshotMapper replanExplanationSnapshotMapper;

    @PostMapping("/create")
    @Operation(summary = "创建生产任务")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:create')")
    public CommonResult<Long> createTask(@Valid @RequestBody MesProTaskSaveReqVO createReqVO) {
        return success(taskService.createTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新生产任务")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:update')")
    public CommonResult<Boolean> updateTask(@Valid @RequestBody MesProTaskSaveReqVO updateReqVO) {
        taskService.updateTask(updateReqVO);
        return success(true);
    }

    @PutMapping("/lock")
    @Operation(summary = "锁定生产任务")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:update')")
    public CommonResult<Boolean> lockTask(@Valid @RequestBody MesProTaskLockReqVO reqVO) {
        taskService.lockTask(reqVO.getTaskId(), reqVO.getLockedReason());
        return success(true);
    }

    @PutMapping("/unlock")
    @Operation(summary = "解锁生产任务")
    @Parameter(name = "taskId", description = "任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-task:update')")
    public CommonResult<Boolean> unlockTask(@RequestParam("taskId") Long taskId) {
        taskService.unlockTask(taskId);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除生产任务")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-task:delete')")
    public CommonResult<Boolean> deleteTask(@RequestParam("id") Long id) {
        taskService.deleteTask(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得生产任务")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:query')")
    public CommonResult<MesProTaskRespVO> getTask(@RequestParam("id") Long id) {
        MesProTaskDO task = taskService.getTask(id);
        if (task == null) {
            return success(null);
        }
        return success(buildTaskRespVOList(ListUtil.of(task)).get(0));
    }

    @GetMapping("/page")
    @Operation(summary = "获得生产任务分页")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:query')")
    public CommonResult<PageResult<MesProTaskRespVO>> getTaskPage(@Valid MesProTaskPageReqVO pageReqVO) {
        PageResult<MesProTaskDO> pageResult = taskService.getTaskPage(pageReqVO);
        return success(new PageResult<>(buildTaskRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/gantt-list")
    @Operation(summary = "获得甘特图任务列表", description = "后端组装工单=project + 任务=task 列表")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:query')")
    public CommonResult<List<GanttDataRespVO>> listGanttTaskList(@Valid MesProWorkOrderPageReqVO reqVO) {
        // 1.1 查询匹配的工单（不分页）
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MesProWorkOrderDO> workOrders = workOrderService.getWorkOrderPage(reqVO).getList();
        if (CollUtil.isEmpty(workOrders)) {
            return success(Collections.emptyList());
        }
        // 1.2 批量查询所有工单下的任务，按 workOrderId 分组
        List<MesProTaskDO> allTasks = taskService.getTaskListByWorkOrderIds(
                convertSet(workOrders, MesProWorkOrderDO::getId));
        Map<Long, MesProTaskScheduleExtDO> taskExtMap = convertMap(
                taskScheduleExtMapper.selectListByTaskIds(convertSet(allTasks, MesProTaskDO::getId)),
                MesProTaskScheduleExtDO::getTaskId);
        Map<Long, MesProScheduleOrderDO> baseActiveScheduleOrderMap = convertMap(
                scheduleOrderMapper.selectListByIds(taskExtMap.values().stream()
                                .map(MesProTaskScheduleExtDO::getScheduleOrderId)
                                .filter(Objects::nonNull)
                                .collect(java.util.stream.Collectors.toSet()))
                        .stream()
                        .filter(this::isCurrentGanttScheduleOrder)
                        .toList(),
                MesProScheduleOrderDO::getId);
        Set<Long> latestAppliedReplanScheduleOrderIds = resolveLatestAppliedReplanScheduleOrderIds();
        Map<Long, MesProScheduleOrderDO> activeScheduleOrderMap = CollUtil.isEmpty(latestAppliedReplanScheduleOrderIds)
                ? baseActiveScheduleOrderMap
                : baseActiveScheduleOrderMap.entrySet().stream()
                .filter(entry -> latestAppliedReplanScheduleOrderIds.contains(entry.getKey()))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> left, LinkedHashMap::new));
        List<MesProTaskDO> scheduledTasks = allTasks.stream()
                .filter(task -> isScheduledGanttProcessTask(taskExtMap.get(task.getId()), activeScheduleOrderMap))
                .toList();
        if (CollUtil.isEmpty(scheduledTasks)) {
            return success(Collections.emptyList());
        }
        Map<Long, List<MesProTaskDO>> scheduledTaskMap = convertMultiMap(scheduledTasks, MesProTaskDO::getWorkOrderId);
        List<MesProWorkOrderDO> scheduledWorkOrders = workOrders.stream()
                .filter(workOrder -> CollUtil.isNotEmpty(scheduledTaskMap.get(workOrder.getId())))
                .toList();
        if (CollUtil.isEmpty(scheduledWorkOrders)) {
            return success(Collections.emptyList());
        }
        Set<Long> scheduledWorkOrderIds = convertSet(scheduledWorkOrders, MesProWorkOrderDO::getId);

        // 2.1 查询关联数据
        java.util.Set<Long> allItemIds = new java.util.HashSet<>();
        allItemIds.addAll(convertSet(scheduledWorkOrders, MesProWorkOrderDO::getProductId));
        allItemIds.addAll(convertSet(scheduledTasks, MesProTaskDO::getItemId));
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(allItemIds);
        Map<Long, MesMdUnitMeasureDO> unitMap = unitMeasureService.getUnitMeasureMap(
                convertSet(itemMap.values(), MesMdItemDO::getUnitMeasureId));
        Map<Long, MesMdWorkstationDO> workstationMap = workstationService.getWorkstationMap(
                convertSet(scheduledTasks, MesProTaskDO::getWorkstationId));
        Map<Long, MesMdProductionLineDO> lineMap = productionLineService.getProductionLineMap(
                convertSet(workstationMap.values(), MesMdWorkstationDO::getProductionLineId));
        Map<Long, MesProScheduleOrderProcessDO> scheduleOrderProcessMap = convertMap(
                scheduleOrderProcessMapper.selectListByIds(scheduledTasks.stream()
                        .map(task -> taskExtMap.get(task.getId()))
                        .filter(Objects::nonNull)
                        .map(MesProTaskScheduleExtDO::getScheduleOrderProcessId)
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toSet())),
                MesProScheduleOrderProcessDO::getId);
        // 2.2 组装甘特图数据
        List<GanttDataRespVO> ganttData = new java.util.ArrayList<>();
        for (MesProWorkOrderDO workOrder : scheduledWorkOrders) {
            List<MesProTaskDO> woTasks = scheduledTaskMap.getOrDefault(workOrder.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(woTasks)) {
                continue;
            }
            GanttTaskRange workOrderRange = resolveWorkOrderGanttRange(woTasks);
            // 2.2.a 工单 -> project 行
            MesMdItemDO item = itemMap.get(workOrder.getProductId());
            String productName = item != null ? item.getName() : "";
            GanttDataRespVO woData = new GanttDataRespVO()
                    .setId(MesBizTypeConstants.PRO_WORKORDER + "_" + workOrder.getId())
                    .setOriginalId(workOrder.getId())
                    .setType(MesBizTypeConstants.PRO_WORKORDER)
                    .setText(buildGanttText(item, workOrder.getQuantity(), unitMap))
                    .setWorkOrderCode(workOrder.getCode())
                    .setScheduleOrderId(resolveWorkOrderScheduleOrderId(woTasks, taskExtMap))
                    .setProduct(productName)
                    .setQuantity(workOrder.getQuantity())
                    .setStartDate(workOrderRange.startDate()).setEndDate(workOrderRange.endDate())
                    .setDuration(workOrderRange.duration())
                    .setProgress(calcProgress(workOrder.getQuantityProduced(), workOrder.getQuantity()));
            if (ObjUtil.notEqual(workOrder.getParentId(), MesProWorkOrderDO.PARENT_ID_NULL)
                    && scheduledWorkOrderIds.contains(workOrder.getParentId())) {
                woData.setParent(MesBizTypeConstants.PRO_WORKORDER + "_" + workOrder.getParentId());
            }
            ganttData.add(woData);

            // 2.2.b 任务 -> task 行
            for (MesProTaskDO task : woTasks) {
                MesMdWorkstationDO ws = workstationMap.get(task.getWorkstationId());
                MesMdProductionLineDO line = ws != null ? lineMap.get(ws.getProductionLineId()) : null;
                MesMdItemDO taskItem = itemMap.get(task.getItemId());
                MesProTaskScheduleExtDO taskExt = taskExtMap.get(task.getId());
                if (!isScheduledGanttProcessTask(taskExt, activeScheduleOrderMap)) {
                    continue;
                }
                GanttDataRespVO tData = new GanttDataRespVO()
                        .setId(MesBizTypeConstants.PRO_TASK + "_" + task.getId())
                        .setOriginalId(task.getId())
                        .setType(MesBizTypeConstants.PRO_TASK)
                        .setText(buildGanttText(taskItem, task.getQuantity(), unitMap))
                        .setWorkOrderCode(workOrder.getCode())
                        .setParent(MesBizTypeConstants.PRO_WORKORDER + "_" + workOrder.getId())
                        .setWorkstation(ws != null ? ws.getName() : null)
                        .setLine(line != null ? line.getName() : null)
                        .setProcess(resolveGanttTaskProcessName(task, taskExt, scheduleOrderProcessMap))
                        .setColor(task.getColorCode())
                        .setQuantity(task.getQuantity())
                        .setScheduleSource(taskExt.getScheduleSource())
                        .setScheduleOrderId(taskExt.getScheduleOrderId())
                        .setScheduleOrderProcessId(taskExt.getScheduleOrderProcessId())
                        .setLocked(taskExt.getLocked())
                        .setRiskStatus(taskExt.getRiskStatus())
                        .setStartDate(task.getStartTime()).setEndDate(task.getEndTime())
                        .setDuration(task.getDuration() != null ? task.getDuration().longValue() : null)
                        .setProgress(calcProgress(task.getProducedQuantity(), task.getQuantity()));
                ganttData.add(tData);
            }
        }
        return success(ganttData);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出生产任务 Excel")
    @PreAuthorize("@ss.hasPermission('mes:pro-task:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTaskExcel(@Valid MesProTaskPageReqVO pageReqVO,
                                HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MesProTaskDO> list = taskService.getTaskPage(pageReqVO).getList();
        List<MesProTaskRespVO> voList = buildTaskRespVOList(list);
        ExcelUtils.write(response, "生产任务.xls", "数据", MesProTaskRespVO.class, voList);
    }

    // ==================== 拼接 VO ====================

    private List<MesProTaskRespVO> buildTaskRespVOList(List<MesProTaskDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 批量查询关联数据
        Map<Long, MesProWorkOrderDO> workOrderMap = workOrderService.getWorkOrderMap(
                convertSet(list, MesProTaskDO::getWorkOrderId));
        Map<Long, MesMdWorkstationDO> workstationMap = workstationService.getWorkstationMap(
                convertSet(list, MesProTaskDO::getWorkstationId));
        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(
                new ArrayList<>(convertSet(list, MesProTaskDO::getProcessId)));
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(
                convertSet(list, MesProTaskDO::getItemId));
        Map<Long, MesMdUnitMeasureDO> unitMeasureMap = unitMeasureService.getUnitMeasureMap(
                convertSet(itemMap.values(), MesMdItemDO::getUnitMeasureId));
        Map<Long, MesMdClientDO> clientMap = clientService.getClientMap(
                convertSet(list, MesProTaskDO::getClientId));
        // 工序的 checkFlag：批量查询后构建 routeId -> processId -> checkFlag 的双层 Map
        Set<Long> routeIds = convertSet(list, MesProTaskDO::getRouteId);
        Map<Long, Map<Long, Boolean>> routeProcessCheckFlagMap = new HashMap<>();
        if (CollUtil.isNotEmpty(routeIds)) {
            List<MesProRouteProcessDO> allRouteProcesses = routeProcessService.getRouteProcessListByRouteIdsIgnoreDeleted(routeIds);
            for (MesProRouteProcessDO rp : allRouteProcesses) {
                routeProcessCheckFlagMap
                        .computeIfAbsent(rp.getRouteId(), k -> new HashMap<>())
                        .put(rp.getProcessId(), Boolean.TRUE.equals(rp.getCheckFlag()));
            }
        }
        // 拼接 VO
        return convertList(list, task -> {
            MesProTaskRespVO vo = BeanUtils.toBean(task, MesProTaskRespVO.class);
            findAndThen(workOrderMap, task.getWorkOrderId(), wo ->
                    vo.setWorkOrderCode(wo.getCode()).setWorkOrderName(wo.getName()).setRequestDate(wo.getRequestDate()));
            findAndThen(workstationMap, task.getWorkstationId(), ws ->
                    vo.setWorkstationCode(ws.getCode()).setWorkstationName(ws.getName()));
            findAndThen(processMap, task.getProcessId(), p ->
                    vo.setProcessName(p.getName()));
            findAndThen(itemMap, task.getItemId(), item -> {
                vo.setItemCode(item.getCode()).setItemName(item.getName()).setItemSpecification(item.getSpecification());
                findAndThen(unitMeasureMap, item.getUnitMeasureId(), unit ->
                        vo.setUnitMeasureName(unit.getName()));
            });
            findAndThen(clientMap, task.getClientId(), c ->
                    vo.setClientName(c.getName()));
            findAndThen(routeProcessCheckFlagMap, task.getRouteId(), processCheckMap ->
                    findAndThen(processCheckMap, task.getProcessId(), vo::setCheckFlag));
            return vo;
        });
    }

    /**
     * 拼接甘特图显示文本，格式："[产品名][数量][单位]"
     */
    private String buildGanttText(MesMdItemDO item, BigDecimal quantity,
                                  Map<Long, MesMdUnitMeasureDO> unitMap) {
        String itemName = item != null ? item.getName() : "";
        String quantityStr = quantity != null ? quantity.stripTrailingZeros().toPlainString() : "";
        String unitName = "";
        if (item != null && item.getUnitMeasureId() != null) {
            MesMdUnitMeasureDO unit = unitMap.get(item.getUnitMeasureId());
            unitName = unit != null ? unit.getName() : "";
        }
        return itemName + quantityStr + unitName;
    }

    private GanttTaskRange resolveWorkOrderGanttRange(List<MesProTaskDO> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return new GanttTaskRange(null, null, null);
        }
        LocalDateTime startDate = tasks.stream()
                .map(MesProTaskDO::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime endDate = tasks.stream()
                .map(MesProTaskDO::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return new GanttTaskRange(startDate, endDate, null);
        }
        long hours = Math.max(1L, Duration.between(startDate, endDate).toHours());
        long workDays = Math.max(1L, (hours + 7L) / 8L);
        return new GanttTaskRange(startDate, endDate, workDays);
    }

    private boolean isScheduledGanttProcessTask(MesProTaskScheduleExtDO taskExt,
                                                Map<Long, MesProScheduleOrderDO> activeScheduleOrderMap) {
        return taskExt != null
                && taskExt.getScheduleOrderId() != null
                && taskExt.getScheduleOrderProcessId() != null
                && activeScheduleOrderMap.containsKey(taskExt.getScheduleOrderId());
    }

    private boolean isCurrentGanttScheduleOrder(MesProScheduleOrderDO scheduleOrder) {
        return scheduleOrder != null
                && ObjUtil.notEqual(scheduleOrder.getStatus(), MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                && ObjUtil.notEqual(scheduleOrder.getStatus(), MesProScheduleOrderStatusEnum.CANCELED.getStatus());
    }

    private String resolveGanttTaskProcessName(MesProTaskDO task, MesProTaskScheduleExtDO taskExt,
                                               Map<Long, MesProScheduleOrderProcessDO> scheduleOrderProcessMap) {
        if (taskExt == null || taskExt.getScheduleOrderId() == null || taskExt.getScheduleOrderProcessId() == null) {
            throw new IllegalStateException("生产排产甘特图任务未参与排产，taskId=" + task.getId());
        }
        MesProScheduleOrderProcessDO scheduleOrderProcess =
                scheduleOrderProcessMap.get(taskExt.getScheduleOrderProcessId());
        if (scheduleOrderProcess == null) {
            throw new IllegalStateException("生产排产甘特图任务引用不存在的排产工序快照，taskId="
                    + task.getId() + "，scheduleOrderProcessId=" + taskExt.getScheduleOrderProcessId());
        }
        if (!Objects.equals(scheduleOrderProcess.getScheduleOrderId(), taskExt.getScheduleOrderId())) {
            throw new IllegalStateException("生产排产甘特图任务排产工单和工序快照不一致，taskId="
                    + task.getId() + "，scheduleOrderId=" + taskExt.getScheduleOrderId()
                    + "，scheduleOrderProcessId=" + taskExt.getScheduleOrderProcessId()
                    + "，processScheduleOrderId=" + scheduleOrderProcess.getScheduleOrderId());
        }
        String snapshotProcessName = StrUtil.trim(scheduleOrderProcess.getProcessName());
        if (StrUtil.isBlank(snapshotProcessName)) {
            throw new IllegalStateException("生产排产甘特图任务缺少工序名称，taskId="
                    + task.getId() + "，scheduleOrderProcessId=" + scheduleOrderProcess.getId()
                    + "，processId=" + scheduleOrderProcess.getProcessId());
        }
        return snapshotProcessName;
    }

    private Set<Long> resolveLatestAppliedReplanScheduleOrderIds() {
        MesProReplanExplanationSnapshotDO snapshot = replanExplanationSnapshotMapper.selectLatest();
        if (snapshot == null || StrUtil.isBlank(snapshot.getSnapshotJson())) {
            return Collections.emptySet();
        }
        MesProReplanExplanationRespVO explanation = JsonUtils.parseObject(
                snapshot.getSnapshotJson(), MesProReplanExplanationRespVO.class);
        if (explanation == null || CollUtil.isEmpty(explanation.getOrders())) {
            return Collections.emptySet();
        }
        return explanation.getOrders().stream()
                .map(MesProReplanExplanationRespVO.OrderItem::getScheduleOrderId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Long resolveWorkOrderScheduleOrderId(List<MesProTaskDO> woTasks,
                                                 Map<Long, MesProTaskScheduleExtDO> taskExtMap) {
        if (CollUtil.isEmpty(woTasks)) {
            return null;
        }
        return woTasks.stream()
                .map(task -> taskExtMap.get(task.getId()))
                .filter(Objects::nonNull)
                .map(MesProTaskScheduleExtDO::getScheduleOrderId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private record GanttTaskRange(LocalDateTime startDate, LocalDateTime endDate, Long duration) {
    }

    /**
     * 计算进度 = 已生产 / 总量，返回 0~1
     */
    private float calcProgress(BigDecimal produced, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0 || produced == null) {
            return 0f;
        }
        return produced.divide(total, RoundingMode.HALF_UP).floatValue();
    }

}
