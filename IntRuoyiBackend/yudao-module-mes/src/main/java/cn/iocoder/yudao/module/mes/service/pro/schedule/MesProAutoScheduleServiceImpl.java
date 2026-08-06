package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.*;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightIssueRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarWorkOrderAnalysisRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarRulesRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttDataRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttLinkRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.holiday.MesCalHolidayDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderOperationLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderOperationLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import cn.iocoder.yudao.module.mes.enums.MesBizTypeConstants;
import cn.iocoder.yudao.module.mes.enums.cal.MesCalHolidayTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesTimeUnitTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.schedule.CapacityWindowAllocator.ScheduleWindowResult;
import cn.iocoder.yudao.module.mes.service.pro.schedule.CapacityWindowAllocator.ShiftWindow;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.AutoScheduleCalendarContext;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.CandidateLinePlan;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.DailyProcessCapacityLedger;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.LinkPlan;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.PlannedTask;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.PreviewStep;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.ProcessLineCandidate;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.ProcessResourcePool;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.RejectedLatestStartPlan;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.ScheduleComputation;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.ScheduleIssueDraft;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleApplyGuard;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleDefaultCompatibilityPolicy;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleInputAssembler;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.RouteSnapshotResolver;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleTopologyResolver;
import cn.iocoder.yudao.module.mes.service.pro.schedule.identity.LineProcessIdentity;
import cn.iocoder.yudao.module.mes.service.pro.schedule.identity.RouteProcessIdentity;
import cn.iocoder.yudao.module.mes.service.pro.schedule.identity.ScheduleOrderProcessIdentity;
import cn.iocoder.yudao.module.mes.service.cal.holiday.MesCalHolidayService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanShiftService;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityMetrics;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.EdhrScheduleCompletionCreateCommand;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteFlowContextMatcher;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProductService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderService;
import cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderService.ProcessProgressMetrics;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

@Service
@Validated
public class MesProAutoScheduleServiceImpl implements MesProAutoScheduleService {

    private static final String CAPACITY_MODE_PLANNED = "PLANNED";
    private static final String CAPACITY_MODE_ACTUAL = "ACTUAL";
    private static final String ISSUE_TYPE_ROUTE = "ROUTE";
    private static final String ISSUE_TYPE_ROUTE_PROCESS = "ROUTE_PROCESS";
    private static final String ISSUE_TYPE_MATERIAL = "MATERIAL";
    private static final String ISSUE_TYPE_MATERIAL_DEMAND = "MATERIAL_DEMAND";
    private static final String ISSUE_TYPE_WORKSTATION = "WORKSTATION";
    private static final String ISSUE_TYPE_LINE = "LINE";
    private static final String ISSUE_TYPE_CALENDAR = "CALENDAR";
    private static final String ISSUE_TYPE_CAPACITY = "CAPACITY";
    private static final String ISSUE_TYPE_ACTIVE_TASK = "ACTIVE_TASK";
    private static final String ISSUE_STATUS_OPEN = "OPEN";
    private static final String ISSUE_STATUS_RESOLVED = "RESOLVED";
    private static final String ISSUE_TYPE_PROTECTED = "PROTECTED_TASK";
    private static final String ISSUE_TYPE_LATEST_START = "LATEST_START";
    private static final String ISSUE_TYPE_PREFLIGHT = "PREFLIGHT";
    private static final String ISSUE_TYPE_MANUAL_NIGHT_SHIFT_CANCEL = "MANUAL_NIGHT_SHIFT_CANCEL";
    private static final String ISSUE_SEVERITY_BLOCKING = "BLOCKING";
    private static final String ISSUE_SEVERITY_WARNING = "WARNING";
    private static final String PROTECTION_REASON_FINISHED = "FINISHED";
    private static final String PROTECTION_REASON_FEEDBACK = "FEEDBACK";
    private static final String PROTECTION_REASON_IN_PROGRESS = "IN_PROGRESS";
    private static final String PROTECTION_REASON_LOCKED = "LOCKED";
    private static final String PROTECTION_REASON_MANUAL = "MANUAL";
    private static final String ISSUE_MESSAGE_ROUTE_CALENDAR_CAPACITY_INSUFFICIENT =
            "路线工序可用日历产能不足";
    private static final int MAX_SHIFT_CAPACITY_ISSUES = 1;
    private static final int LINE_CAPACITY_EXTENSION_BATCH_DAYS = 30;
    private static final int LINE_CAPACITY_SEARCH_DAY_LIMIT = 3660;
    private static final String SCHEDULE_SOURCE_AUTO = "AUTO";
    private static final String SCHEDULE_SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_TYPE_NIGHT_SHIFT_CANCEL = "NIGHT_SHIFT_CANCEL";
    private static final String RISK_STATUS_NONE = "NONE";
    private static final String RISK_STATUS_BLOCKED = "BLOCKED";
    private static final String PREFLIGHT_RESULT_BLOCKED = "BLOCKED";
    private static final String DEPENDENCY_TYPE_FS = "0";
    private static final String CALENDAR_CONTEXT_TIME_ZONE = ZoneId.systemDefault().getId();
    private static final String CAPACITY_SOURCE_MACHINE = "MACHINE";
    private static final String CAPACITY_SOURCE_WORKER = "WORKER";
    private static final String CAPACITY_SOURCE_ROUTE_PROCESS = "ROUTE_PROCESS";
    private static final String OPERATION_AUTO_APPLY = "AUTO_APPLY";
    private static final String OPERATION_REPLAN_APPLY = "REPLAN_APPLY";
    private static final String REPLAN_TRIGGER_MANUAL = "MANUAL";
    private static final String REPLAN_TRIGGER_NIGHTLY = "NIGHTLY";
    @Resource
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;
    @Resource
    private MesProTaskMapper taskMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesWmMaterialStockMapper materialStockMapper;
    @Resource
    private MesProRouteProductService routeProductService;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProRouteService routeService;
    @Resource
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProScheduleOrderService scheduleOrderService;
    @Resource
    private MesMdProductionLineService productionLineService;
    @Resource
    private MesMdWorkstationCapacityService workstationCapacityService;
    @Resource
    private MesMdWorkstationMachineService workstationMachineService;
    @Resource
    private MesCalPlanService planService;
    @Resource
    private MesCalPlanShiftService planShiftService;
    @Resource
    private MesCalHolidayService holidayService;
    @Resource
    private MesProScheduleCalendarService scheduleCalendarService;
    @Resource
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesProProcessService processService;
    @Resource
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Resource
    private MesProCapacityPlanMapper capacityPlanMapper;
    @Resource
    private MesProCapacityActualMapper capacityActualMapper;
    @Resource
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Resource
    private MesProTaskDependencyMapper taskDependencyMapper;
    @Resource
    private MesProScheduleIssueMapper scheduleIssueMapper;
    @Resource
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Resource
    private MesProScheduleOrderOperationLogMapper scheduleOrderOperationLogMapper;
    @Resource
    private MesProReplanExplanationSnapshotMapper replanExplanationSnapshotMapper;
    @Resource
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Resource
    private MesProFeedbackMapper feedbackMapper;
    @Resource
    private MesKingdeeProductionMaterialListSyncService productionMaterialListSyncService;
    @Resource
    private ScheduleTopologyResolver scheduleTopologyResolver;
    @Resource
    private ScheduleApplyGuard scheduleApplyGuard;
    @Resource
    private ScheduleDefaultCompatibilityPolicy scheduleDefaultCompatibilityPolicy;
    @Resource
    private ScheduleInputAssembler scheduleInputAssembler;
    @Resource
    private RouteSnapshotResolver routeSnapshotResolver;
    @Resource
    private CapacityWindowAllocator capacityWindowAllocator;
    @Resource
    private SchedulePlanner schedulePlanner;
    @Resource
    private ScheduleApplier scheduleApplier;

    @Override
    public MesProAutoSchedulePreviewRespVO preview(MesProAutoSchedulePreviewReqVO reqVO) {
        ScheduleComputation computation = computeSchedule(reqVO, false);
        return buildPreviewResp(computation);
    }

    @Override
    public MesProAutoScheduleReplanPreviewRespVO replanPreview(MesProAutoScheduleReplanReqVO reqVO) {
        ScheduleComputation computation = computeSchedule(reqVO, false);
        return buildReplanPreviewResp(computation);
    }

    private List<Long> normalizedScheduleOrderIds(MesProAutoScheduleReplanReqVO reqVO) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getScheduleOrderIds())) {
            throw exception(PRO_AUTO_SCHEDULE_SCOPE_EMPTY);
        }
        return reqVO.getScheduleOrderIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProAutoScheduleApplyRespVO apply(MesProAutoSchedulePreviewReqVO reqVO) {
        return applyInternal(reqVO, OPERATION_AUTO_APPLY, null);
    }

    private MesProAutoScheduleApplyRespVO applyInternal(MesProAutoSchedulePreviewReqVO reqVO, String operationType,
                                                        String replanTriggerSource) {
        prepareApplyReason(reqVO, operationType);
        scheduleApplyGuard.validateCalendarContextTokenProvided(reqVO.getCalendarContextToken());
        ScheduleComputation computation = computeSchedule(reqVO, true);
        validateNoFrozenWorkOrders(reqVO.getWorkOrderIds());
        scheduleApplyGuard.validateCalendarContextToken(reqVO.getCalendarContextToken(),
                computation.calendarContext == null ? null : computation.calendarContext.token);
        computation.issues.addAll(validateApplyPreflight(reqVO, computation));
        if (hasGlobalBlockingIssues(computation.issues)) {
            throwBlockingIssue(computation, reqVO.getRuntimeCapacityBasis(), computation.issues);
        }
        validateLatestStartZeroTask(computation);
        Set<Long> blockedWorkOrderIds = blockingWorkOrderIds(computation.issues);
        List<Long> applyWorkOrderIds = reqVO.getWorkOrderIds().stream()
                .filter(workOrderId -> !blockedWorkOrderIds.contains(workOrderId))
                .toList();
        if (CollUtil.isNotEmpty(applyWorkOrderIds)) {
            workOrderMapper.selectListByIdsForUpdate(applyWorkOrderIds);
        }

        List<Long> existingScopeTaskIds = computation.scopeTasks.stream()
                .filter(task -> !computation.nonBlockingSkippedWorkOrderIds.contains(task.getWorkOrderId()))
                .filter(task -> !blockedWorkOrderIds.contains(task.getWorkOrderId()))
                .map(MesProTaskDO::getId)
                .toList();
        List<Long> deleteTaskIds = computation.replaceableScopeTasks.stream()
                .filter(task -> !computation.nonBlockingSkippedWorkOrderIds.contains(task.getWorkOrderId()))
                .filter(task -> !blockedWorkOrderIds.contains(task.getWorkOrderId()))
                .map(MesProTaskDO::getId)
                .toList();
        ScheduleApplier.ApplyResult applyCleanup = scheduleApplier.deleteReplaceableTasks(
                ScheduleApplier.ApplyCommand.forReplaceableTaskCleanup(
                        existingScopeTaskIds, deleteTaskIds, reqVO.getWorkOrderIds()));
        deleteTaskIds = applyCleanup.getDeletedTaskIds();

        String requestId = UUID.randomUUID().toString();
        List<Long> createdTaskIds = new ArrayList<>();
        Map<String, Long> firstTaskIdsByWorkOrderProcess = new HashMap<>();
        Map<String, Long> lastTaskIdsByWorkOrderProcess = new HashMap<>();
        Map<String, MesProScheduleOrderDO> scheduleOrderByWorkOrderId = computation.scheduleOrders.stream()
                .collect(Collectors.toMap(order -> String.valueOf(order.getWorkOrderId()), order -> order, (left, right) -> left));
        Map<String, MesProScheduleOrderProcessDO> scheduleOrderProcessByWorkOrderProcess = new HashMap<>();
        computation.scheduleOrderProcessesByOrderId.forEach((scheduleOrderId, processes) -> {
            MesProScheduleOrderDO scheduleOrder = computation.scheduleOrderMap.get(scheduleOrderId);
            if (scheduleOrder == null) {
                return;
            }
            for (MesProScheduleOrderProcessDO process : processes) {
                scheduleOrderProcessByWorkOrderProcess.put(taskKey(scheduleOrder.getWorkOrderId(), process.getProcessId()), process);
            }
        });
        List<MesProTaskDO> appliedPreservedTasks = computation.preservedTasks.stream()
                .filter(task -> !blockedWorkOrderIds.contains(task.getWorkOrderId()))
                .toList();
        for (MesProTaskDO preservedTask : appliedPreservedTasks) {
            String key = taskKey(preservedTask.getWorkOrderId(), preservedTask.getProcessId());
            firstTaskIdsByWorkOrderProcess.putIfAbsent(key, preservedTask.getId());
            lastTaskIdsByWorkOrderProcess.put(key, preservedTask.getId());
        }
        syncPreservedTaskScheduleRelations(computation, scheduleOrderByWorkOrderId, scheduleOrderProcessByWorkOrderProcess);

        for (PlannedTask plan : computation.generatedTasks) {
            MesProTaskDO task = MesProTaskDO.builder()
                    .code(autoCodeRecordService.generateAutoCode("PRO_TASK_CODE"))
                    .name(buildTaskName(computation.itemMap.get(plan.itemId), plan.quantity))
                    .workOrderId(plan.workOrderId)
                    .workstationId(plan.workstationId)
                    .routeId(plan.routeId)
                    .processId(plan.processId)
                    .itemId(plan.itemId)
                    .quantity(plan.quantity)
                    .producedQuantity(BigDecimal.ZERO)
                    .qualifyQuantity(BigDecimal.ZERO)
                    .unqualifyQuantity(BigDecimal.ZERO)
                    .changedQuantity(BigDecimal.ZERO)
                    .clientId(plan.clientId)
                    .startTime(plan.startTime)
                    .duration(plan.durationBlocks)
                    .endTime(plan.endTime)
                    .colorCode(plan.colorCode)
                    .status(MesProTaskStatusEnum.PREPARE.getStatus())
                    .remark("AUTO")
                    .build();
            MesProTaskScheduleExtDO ext = MesProTaskScheduleExtDO.builder()
                    .scheduleOrderId(resolveScheduleOrderId(scheduleOrderByWorkOrderId, plan.workOrderId))
                    .scheduleOrderProcessId(resolveScheduleOrderProcessId(scheduleOrderProcessByWorkOrderProcess,
                            plan.workOrderId, plan.processId, plan.scheduleOrderProcessId))
                    .scheduleSource(SCHEDULE_SOURCE_AUTO)
                    .locked(Boolean.FALSE)
                    .generatedRequestId(requestId)
                    .riskStatus(RISK_STATUS_NONE)
                    .remark("AUTO")
                    .build();
            Long taskId = scheduleApplier.insertTaskWithScheduleExt(task, ext);
            createdTaskIds.add(taskId);
            String workOrderProcessKey = taskKey(plan.workOrderId, plan.processId);
            firstTaskIdsByWorkOrderProcess.putIfAbsent(workOrderProcessKey, taskId);
            lastTaskIdsByWorkOrderProcess.put(workOrderProcessKey, taskId);
        }

        List<MesProTaskDependencyDO> dependencies = new ArrayList<>();
        for (LinkPlan linkPlan : computation.linkPlans) {
            Long sourceTaskId = lastTaskIdsByWorkOrderProcess.get(taskKey(linkPlan.workOrderId, linkPlan.sourceProcessId));
            Long targetTaskId = firstTaskIdsByWorkOrderProcess.get(taskKey(linkPlan.workOrderId, linkPlan.targetProcessId));
            if (sourceTaskId == null || targetTaskId == null) {
                continue;
            }
            dependencies.add(MesProTaskDependencyDO.builder()
                    .sourceTaskId(sourceTaskId)
                    .targetTaskId(targetTaskId)
                    .sourceProcessId(linkPlan.sourceProcessId)
                    .targetProcessId(linkPlan.targetProcessId)
                    .dependencyType(DEPENDENCY_TYPE_FS)
                    .enabled(Boolean.TRUE)
                    .build());
        }
        scheduleApplier.insertDependencies(dependencies);
        scheduleApplier.insertIssues(computation.issues);

        scheduleApplier.syncQuantityScheduled(applyWorkOrderIds);
        scheduleApplier.syncScheduleOrderPlanFields(buildScheduleOrderPlanFieldUpdates(computation));
        List<ScheduleIssueDraft> edhrBatchCreationIssues = scheduleApplier
                .createEdhrBatchExecutionsAfterScheduleCompletion(buildEdhrBatchExecutionCompletionCommands(computation));
        if (CollUtil.isNotEmpty(edhrBatchCreationIssues)) {
            computation.issues.addAll(edhrBatchCreationIssues);
            scheduleApplier.insertIssues(edhrBatchCreationIssues);
        }

        MesProAutoScheduleApplyRespVO respVO = new MesProAutoScheduleApplyRespVO();
        respVO.setApplied(CollUtil.isNotEmpty(applyWorkOrderIds));
        respVO.setSummary(buildSummary(computation, computation.issues));
        respVO.setCreatedTaskIds(createdTaskIds);
        respVO.setDeletedTaskIds(deleteTaskIds);
        respVO.setPreservedTaskIds(appliedPreservedTasks.stream().map(MesProTaskDO::getId).toList());
        respVO.setIssues(buildIssueRespList(computation.issues.stream().map(issue -> issue.toDO(null)).toList()));
        insertScheduleApplyEventLogs(operationType, reqVO, computation, respVO, requestId);
        if (OPERATION_REPLAN_APPLY.equals(operationType)) {
            insertReplanExplanationSnapshot(reqVO, computation, respVO, requestId, replanTriggerSource);
        }
        return respVO;
    }

    private void prepareApplyReason(MesProAutoSchedulePreviewReqVO reqVO, String operationType) {
        if (OPERATION_REPLAN_APPLY.equals(operationType)) {
            normalizeOptionalApplyReason(reqVO);
            return;
        }
        validateRequiredApplyReason(reqVO);
    }

    private void validateRequiredApplyReason(MesProAutoSchedulePreviewReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getReason())) {
            throw exception(PRO_SCHEDULE_ORDER_REASON_REQUIRED);
        }
        reqVO.setReason(StrUtil.trim(reqVO.getReason()));
    }

    private void normalizeOptionalApplyReason(MesProAutoSchedulePreviewReqVO reqVO) {
        reqVO.setReason(StrUtil.emptyToNull(StrUtil.trim(reqVO.getReason())));
    }

    private String resolveShiftCodeForTask(MesProTaskDO task) {
        if (task == null || task.getStartTime() == null || task.getEndTime() == null) {
            return null;
        }
        int startHour = task.getStartTime().getHour();
        int endHour = task.getEndTime().getHour();
        if (startHour >= 20 || endHour <= 8 || task.getEndTime().toLocalDate().isAfter(task.getStartTime().toLocalDate())) {
            return "NIGHT";
        }
        return "DAY";
    }

    private List<EdhrScheduleCompletionCreateCommand> buildEdhrBatchExecutionCompletionCommands(ScheduleComputation computation) {
        List<EdhrScheduleCompletionCreateCommand> commands = new ArrayList<>();
        Set<Long> blockedWorkOrderIds = blockingWorkOrderIds(computation.issues);
        Map<Long, MesProWorkOrderDO> workOrderMap = computation.scheduleOrders.stream()
                .map(MesProScheduleOrderDO::getWorkOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toSet(), ids -> {
                    if (ids.isEmpty()) {
                        return Collections.<Long, MesProWorkOrderDO>emptyMap();
                    }
                    return workOrderService.getWorkOrderMap(ids);
                }));
        Set<Long> edhrEnabledRouteIds = computation.scheduleOrders.stream()
                .map(MesProScheduleOrderDO::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .filter(this::hasEnabledEdhrBatchConfig)
                .collect(Collectors.toSet());
        for (MesProScheduleOrderDO scheduleOrder : computation.scheduleOrders) {
            if (blockedWorkOrderIds.contains(scheduleOrder.getWorkOrderId())
                    || computation.latestStartRejectedPlans.containsKey(scheduleOrder.getWorkOrderId())
                    || CollUtil.isEmpty(computation.finalSteps.get(scheduleOrder.getWorkOrderId()))
                    || !edhrEnabledRouteIds.contains(scheduleOrder.getRouteId())) {
                continue;
            }
            MesProWorkOrderDO workOrder = workOrderMap.get(scheduleOrder.getWorkOrderId());
            EdhrScheduleCompletionCreateCommand command = new EdhrScheduleCompletionCreateCommand()
                    .setScheduleOrderId(scheduleOrder.getId())
                    .setScheduleOrderCode(scheduleOrder.getCode())
                    .setWorkOrderId(scheduleOrder.getWorkOrderId())
                    .setBatchCode(workOrder == null ? null : workOrder.getBatchCode())
                    .setProductId(scheduleOrder.getProductId())
                    .setRouteId(scheduleOrder.getRouteId())
                    .setRemark("排产完成自动创建");
            commands.add(command);
        }
        return commands;
    }

    private boolean hasEnabledEdhrBatchConfig(Long routeId) {
        String useType = MesProRouteFlowConfigTypeEnum.BATCH.getType();
        MesProRouteFlowConfigDO flowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, useType);
        if (!MesProRouteFlowContextMatcher.isEnabledFlowContext(flowConfig, routeId, useType)) {
            return false;
        }
        List<MesProRouteFlowProcessConfigDO> processConfigs = Optional.ofNullable(
                        routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId, useType))
                .orElse(Collections.emptyList())
                .stream()
                .filter(config -> MesProRouteFlowContextMatcher.isProcessConfigOwnedBy(
                        flowConfig, config, routeId, useType))
                .filter(config -> config.getId() != null && config.getRouteProcessId() != null)
                .toList();
        if (CollUtil.isEmpty(processConfigs)) {
            return false;
        }
        Map<Long, MesProRouteFlowProcessConfigDO> processConfigById = processConfigs.stream()
                .collect(Collectors.toMap(MesProRouteFlowProcessConfigDO::getId, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        List<Long> routeProcessIds = processConfigs.stream()
                .map(MesProRouteFlowProcessConfigDO::getRouteProcessId)
                .distinct()
                .toList();
        return Optional.ofNullable(routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                        routeProcessIds, useType))
                .orElse(Collections.emptyList())
                .stream()
                .filter(record -> StrUtil.isNotBlank(record.getBatchRecordReportId()))
                .filter(record -> record.getReportSort() != null && record.getReportSort() > 0)
                .anyMatch(record -> isOwnedBatchRecordConfig(record, processConfigById.get(record.getRouteFlowProcessConfigId()), useType));
    }

    private boolean isOwnedBatchRecordConfig(MesProRouteFlowProcessBatchRecordDO record,
                                             MesProRouteFlowProcessConfigDO processConfig,
                                             String useType) {
        return record != null && processConfig != null
                && Objects.equals(useType, record.getUseType())
                && Objects.equals(useType, processConfig.getUseType())
                && Objects.equals(record.getRouteFlowProcessConfigId(), processConfig.getId())
                && Objects.equals(record.getRouteId(), processConfig.getRouteId())
                && Objects.equals(record.getRouteProcessId(), processConfig.getRouteProcessId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProAutoScheduleApplyRespVO replanApply(MesProAutoScheduleReplanReqVO reqVO) {
        return applyInternal(reqVO, OPERATION_REPLAN_APPLY, REPLAN_TRIGGER_MANUAL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProAutoScheduleApplyRespVO replanApplyForNightly(MesProAutoScheduleReplanReqVO reqVO) {
        return applyInternal(reqVO, OPERATION_REPLAN_APPLY, REPLAN_TRIGGER_NIGHTLY);
    }

    @Override
    public MesProLatestScheduleApplyRespVO getLatestSuccessfulScheduleApply() {
        MesProScheduleOrderOperationLogDO latestLog = scheduleOrderOperationLogMapper.selectLatestByOperationTypes(
                List.of(OPERATION_AUTO_APPLY, OPERATION_REPLAN_APPLY));
        MesProLatestScheduleApplyRespVO response = new MesProLatestScheduleApplyRespVO();
        if (latestLog == null) {
            response.setHasData(Boolean.FALSE);
            return response;
        }
        if (latestLog.getCreateTime() == null) {
            throw new IllegalStateException("最近一次成功排产操作日志缺少创建时间，operationLogId=" + latestLog.getId());
        }
        response.setHasData(Boolean.TRUE);
        response.setAppliedAt(latestLog.getCreateTime());
        response.setOperationType(latestLog.getOperationType());
        response.setScheduleOrderId(latestLog.getScheduleOrderId());
        response.setScheduleOrderCode(latestLog.getScheduleOrderCode());
        response.setOperatorId(latestLog.getOperatorId());
        response.setOperatorName(latestLog.getOperatorName());
        response.setReason(latestLog.getReason());
        return response;
    }

    @Override
    public MesProReplanExplanationRespVO getLatestReplanExplanation() {
        MesProReplanExplanationSnapshotDO snapshot = replanExplanationSnapshotMapper.selectLatest();
        if (snapshot == null) {
            MesProReplanExplanationRespVO response = new MesProReplanExplanationRespVO();
            response.setHasData(Boolean.FALSE);
            return response;
        }
        MesProReplanExplanationRespVO response = JsonUtils.parseObject(
                snapshot.getSnapshotJson(), MesProReplanExplanationRespVO.class);
        if (response == null) {
            throw new IllegalStateException("最近一次重排说明快照无法解析，snapshotId=" + snapshot.getId());
        }
        return response;
    }

    private void insertReplanExplanationSnapshot(MesProAutoSchedulePreviewReqVO reqVO,
                                                 ScheduleComputation computation,
                                                 MesProAutoScheduleApplyRespVO applyRespVO,
                                                 String requestId,
                                                 String triggerSource) {
        if (!REPLAN_TRIGGER_MANUAL.equals(triggerSource) && !REPLAN_TRIGGER_NIGHTLY.equals(triggerSource)) {
            throw new IllegalStateException("重排说明缺少有效触发来源");
        }
        LocalDateTime appliedAt = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = REPLAN_TRIGGER_NIGHTLY.equals(triggerSource)
                ? "系统"
                : SecurityFrameworkUtils.getLoginUserNickname();
        MesProReplanExplanationRespVO response = buildReplanExplanationResponse(
                reqVO, computation, applyRespVO, requestId, triggerSource, operatorId, operatorName, appliedAt);
        replanExplanationSnapshotMapper.insert(MesProReplanExplanationSnapshotDO.builder()
                .requestId(requestId)
                .triggerSource(triggerSource)
                .capacityMode(reqVO.getRuntimeCapacityBasis())
                .reason(reqVO.getReason())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .requestStartTime(computation.requestStartTime)
                .appliedAt(appliedAt)
                .snapshotJson(JsonUtils.toJsonString(response))
                .build());
    }

    private MesProReplanExplanationRespVO buildReplanExplanationResponse(
            MesProAutoSchedulePreviewReqVO reqVO,
            ScheduleComputation computation,
            MesProAutoScheduleApplyRespVO applyRespVO,
            String requestId,
            String triggerSource,
            Long operatorId,
            String operatorName,
            LocalDateTime appliedAt) {
        MesProReplanExplanationRespVO response = new MesProReplanExplanationRespVO();
        response.setHasData(Boolean.TRUE);
        response.setRequestId(requestId);
        response.setTriggerSource(triggerSource);
        response.setCapacityMode(reqVO.getRuntimeCapacityBasis());
        response.setReason(reqVO.getReason());
        response.setOperatorId(operatorId);
        response.setOperatorName(operatorName);
        response.setRequestStartTime(computation.requestStartTime);
        response.setAppliedAt(appliedAt);
        List<MesProReplanExplanationRespVO.MaterialItem> materials = buildExplanationMaterials(computation);
        response.setMaterials(materials);
        response.setOrders(buildExplanationOrders(computation));
        response.setWorkOrders(buildExplanationWorkOrders(computation));
        response.setDailyExplanations(schedulePlanner.buildDailyExplanations(
                computation, (lineId, planDate, scheduleOrderProcess) ->
                        resolveDailyAvailableWindowMinutes(computation, lineId, planDate, scheduleOrderProcess)));
        List<MesProAutoScheduleProtectedTaskRespVO> protectedTasks = buildProtectedTaskRespList(computation);
        response.setProtectedTasks(protectedTasks);
        response.setProtectionSummary(buildProtectionSummary(protectedTasks));
        response.setIssues(buildIssueRespList(computation.issues.stream()
                .map(issue -> issue.toDO(null))
                .toList()));
        response.setSummary(buildExplanationSummary(computation, applyRespVO, materials));
        return response;
    }

    private MesProReplanExplanationRespVO.Summary buildExplanationSummary(
            ScheduleComputation computation,
            MesProAutoScheduleApplyRespVO applyRespVO,
            List<MesProReplanExplanationRespVO.MaterialItem> materials) {
        MesProReplanExplanationRespVO.Summary summary = new MesProReplanExplanationRespVO.Summary();
        summary.setScheduleOrderCount(computation.scheduleOrders.size());
        summary.setWorkOrderCount(computation.workOrders.size());
        summary.setRouteCount((int) computation.scheduleOrders.stream()
                .map(MesProScheduleOrderDO::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .count());
        summary.setProcessCount(computation.routeProcessesByWorkOrderId.values().stream()
                .mapToInt(List::size)
                .sum());
        summary.setGeneratedTaskCount(CollUtil.size(applyRespVO.getCreatedTaskIds()));
        summary.setDeletedTaskCount(CollUtil.size(applyRespVO.getDeletedTaskIds()));
        summary.setPreservedTaskCount(CollUtil.size(applyRespVO.getPreservedTaskIds()));
        summary.setBlockingIssueCount((int) computation.issues.stream()
                .filter(issue -> ISSUE_SEVERITY_BLOCKING.equals(issue.severity))
                .count());
        summary.setWarningIssueCount((int) computation.issues.stream()
                .filter(issue -> ISSUE_SEVERITY_WARNING.equals(issue.severity))
                .count());
        summary.setShortageCount((int) materials.stream()
                .filter(material -> material.getShortageQty() != null
                        && material.getShortageQty().compareTo(BigDecimal.ZERO) > 0)
                .count());
        TimeRange generatedTaskTimeRange = buildGeneratedTaskTimeRange(computation);
        summary.setStartTime(generatedTaskTimeRange.startTime);
        summary.setEndTime(generatedTaskTimeRange.endTime);
        return summary;
    }

    private TimeRange buildGeneratedTaskTimeRange(ScheduleComputation computation) {
        List<LocalDateTime> startTimes = computation.generatedTasks.stream()
                .map(task -> task.startTime)
                .filter(Objects::nonNull)
                .toList();
        List<LocalDateTime> endTimes = computation.generatedTasks.stream()
                .map(task -> task.endTime)
                .filter(Objects::nonNull)
                .toList();
        return new TimeRange(
                startTimes.stream().min(LocalDateTime::compareTo).orElse(null),
                endTimes.stream().max(LocalDateTime::compareTo).orElse(null));
    }

    private List<MesProReplanExplanationRespVO.OrderItem> buildExplanationOrders(ScheduleComputation computation) {
        List<MesProReplanExplanationRespVO.OrderItem> result = new ArrayList<>();
        int rank = 1;
        for (MesProScheduleOrderDO scheduleOrder : computation.scheduleOrders) {
            MesProWorkOrderDO workOrder = computation.workOrderMap.get(scheduleOrder.getWorkOrderId());
            MesMdItemDO product = computation.itemMap.get(scheduleOrder.getProductId());
            MesProRouteDO route = computation.routeMap.get(scheduleOrder.getRouteId());
            MesProReplanExplanationRespVO.OrderItem item = new MesProReplanExplanationRespVO.OrderItem();
            item.setRank(rank++);
            item.setScheduleOrderId(scheduleOrder.getId());
            item.setScheduleOrderCode(scheduleOrder.getCode());
            item.setWorkOrderId(scheduleOrder.getWorkOrderId());
            item.setWorkOrderCode(workOrder == null ? scheduleOrder.getErpWorkOrderCode() : workOrder.getCode());
            item.setProductId(scheduleOrder.getProductId());
            item.setProductCode(product == null ? null : product.getCode());
            item.setProductName(product == null ? null : product.getName());
            item.setQuantity(scheduleOrder.getQuantity());
            item.setPromiseDate(scheduleOrder.getPromiseDate());
            item.setPriorityNo(scheduleOrder.getPriorityNo());
            item.setRouteId(scheduleOrder.getRouteId());
            item.setRouteCode(route == null ? null : route.getCode());
            item.setRouteName(route == null ? null : route.getName());
            item.setProcessCount(computation.routeProcessesByWorkOrderId
                    .getOrDefault(scheduleOrder.getWorkOrderId(), Collections.emptyList()).size());
            result.add(item);
        }
        return result;
    }

    private List<MesProReplanExplanationRespVO.WorkOrderItem> buildExplanationWorkOrders(
            ScheduleComputation computation) {
        return computation.workOrderAnalyses.stream().map(analysis -> {
            MesProReplanExplanationRespVO.WorkOrderItem item = new MesProReplanExplanationRespVO.WorkOrderItem();
            item.setWorkOrderId(analysis.getWorkOrderId());
            item.setWorkOrderCode(analysis.getWorkOrderCode());
            item.setProductId(analysis.getProductId());
            item.setProductCode(analysis.getProductCode());
            item.setProductName(analysis.getProductName());
            item.setQuantity(analysis.getQuantity());
            item.setRouteId(analysis.getLineId());
            item.setRouteCode(analysis.getLineCode());
            item.setRouteName(analysis.getLineName());
            item.setStartTime(analysis.getStartTime());
            item.setEndTime(analysis.getEndTime());
            item.setBottleneckProcessId(analysis.getBottleneckProcessId());
            item.setBottleneckProcessName(analysis.getBottleneckProcessName());
            item.setBottleneckHourlyCapacity(analysis.getBottleneckHourlyCapacity());
            item.setProcesses(analysis.getProcesses().stream().map(process -> {
                MesProReplanExplanationRespVO.ProcessItem processItem =
                        new MesProReplanExplanationRespVO.ProcessItem();
                processItem.setProcessId(process.getProcessId());
                processItem.setProcessName(process.getProcessName());
                processItem.setProcessSort(process.getProcessSort());
                processItem.setScheduledQuantity(process.getScheduledQuantity());
                processItem.setCapacitySource(process.getCapacitySource());
                processItem.setShiftNames(resolveProcessShiftNames(
                        computation, analysis.getWorkOrderId(), process.getProcessId()));
                processItem.setWorkstationCount(process.getWorkstationCount());
                processItem.setWorkstationNames(process.getWorkstationNames());
                processItem.setMachineCount(process.getMachineCount());
                processItem.setConfiguredWorkerCount(process.getConfiguredWorkerCount());
                processItem.setCurrentWorkerCount(process.getCurrentWorkerCount());
                processItem.setEffectiveHourlyCapacity(process.getEffectiveHourlyCapacity());
                processItem.setPlannedDurationMinutes(process.getPlannedDurationMinutes());
                processItem.setStartTime(process.getStartTime());
                processItem.setEndTime(process.getEndTime());
                processItem.setBottleneck(process.getBottleneck());
                return processItem;
            }).toList());
            return item;
        }).toList();
    }

    private int resolveDailyAvailableWindowMinutes(ScheduleComputation computation,
                                                   Long lineId,
                                                   LocalDate planDate,
                                                   MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (lineId == null) {
            return capacityWindowAllocator.resolveRouteProcessDailyWindowMinutes(scheduleOrderProcess);
        }
        List<ShiftWindow> windows = computation.shiftWindowsByLineId
                .getOrDefault(lineId, Collections.emptyList())
                .stream()
                .filter(window -> ObjUtil.equal(window.calendarDate, planDate))
                .toList();
        if (scheduleOrderProcess != null) {
            windows = capacityWindowAllocator.filterWindowsForScheduleProcess(windows, scheduleOrderProcess,
                    date -> resolveCalendarShiftMode(computation, date, scheduleOrderProcess));
        }
        return windows.stream()
                .mapToInt(window -> {
                    if (window.startTime == null || window.usableEnd == null || !window.usableEnd.isAfter(window.startTime)) {
                        return 0;
                    }
                    return Math.toIntExact(Duration.between(window.startTime, window.usableEnd).toMinutes());
                })
                .sum();
    }

    private List<String> resolveProcessShiftNames(ScheduleComputation computation, Long workOrderId, Long processId) {
        return computation.finalSteps.getOrDefault(workOrderId, Collections.emptyList()).stream()
                .filter(step -> ObjUtil.equal(step.processId, processId))
                .map(step -> resolveShiftDisplayName(step.startTime, step.endTime))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String resolveShiftDisplayName(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return null;
        }
        if (startTime.getHour() >= 20 || endTime.getHour() <= 8
                || endTime.toLocalDate().isAfter(startTime.toLocalDate())) {
            return "夜班";
        }
        return "白班";
    }

    private List<MesProReplanExplanationRespVO.MaterialItem> buildExplanationMaterials(
            ScheduleComputation computation) {
        Map<Long, BigDecimal> requiredByItemId = new LinkedHashMap<>();
        computation.materialDemandByWorkOrderId.values().forEach(demandByItemId ->
                demandByItemId.forEach((itemId, requiredQty) ->
                        requiredByItemId.merge(itemId, requiredQty, BigDecimal::add)));
        return requiredByItemId.entrySet().stream().map(entry -> {
            MesMdItemDO material = computation.itemMap.get(entry.getKey());
            if (material == null) {
                throw new IllegalStateException("重排说明物料主数据不存在，materialId=" + entry.getKey());
            }
            BigDecimal availableQty = computation.availableStockByItemId
                    .getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal shortageQty = entry.getValue().subtract(availableQty).max(BigDecimal.ZERO);
            MesProReplanExplanationRespVO.MaterialItem item =
                    new MesProReplanExplanationRespVO.MaterialItem();
            item.setMaterialId(entry.getKey());
            item.setMaterialCode(material.getCode());
            item.setMaterialName(material.getName());
            item.setRequiredQty(entry.getValue());
            item.setAvailableQty(availableQty);
            item.setShortageQty(shortageQty);
            item.setOrderContributions(buildMaterialContributions(computation, entry.getKey()));
            return item;
        }).toList();
    }

    private List<MesProReplanExplanationRespVO.MaterialContribution> buildMaterialContributions(
            ScheduleComputation computation, Long materialId) {
        List<MesProReplanExplanationRespVO.MaterialContribution> result = new ArrayList<>();
        for (MesProWorkOrderDO workOrder : computation.workOrders) {
            BigDecimal requiredQty = computation.materialDemandByWorkOrderId
                    .getOrDefault(workOrder.getId(), Collections.emptyMap())
                    .get(materialId);
            if (requiredQty == null) {
                continue;
            }
            MesProScheduleOrderDO scheduleOrder = findScheduleOrderByWorkOrderId(computation, workOrder.getId());
            MesProReplanExplanationRespVO.MaterialContribution contribution =
                    new MesProReplanExplanationRespVO.MaterialContribution();
            contribution.setScheduleOrderId(scheduleOrder == null ? null : scheduleOrder.getId());
            contribution.setScheduleOrderCode(scheduleOrder == null ? null : scheduleOrder.getCode());
            contribution.setWorkOrderId(workOrder.getId());
            contribution.setWorkOrderCode(workOrder.getCode());
            contribution.setRequiredQty(requiredQty);
            result.add(contribution);
        }
        return result;
    }

    private MesProReplanExplanationRespVO.ProtectionSummary buildProtectionSummary(
            List<MesProAutoScheduleProtectedTaskRespVO> protectedTasks) {
        MesProReplanExplanationRespVO.ProtectionSummary summary =
                new MesProReplanExplanationRespVO.ProtectionSummary();
        summary.setTotalCount(protectedTasks.size());
        summary.setFeedbackCount(countProtectionReason(protectedTasks, PROTECTION_REASON_FEEDBACK));
        summary.setInProgressCount(countProtectionReason(protectedTasks, PROTECTION_REASON_IN_PROGRESS));
        summary.setFinishedCount(countProtectionReason(protectedTasks, PROTECTION_REASON_FINISHED));
        summary.setLockedCount(countProtectionReason(protectedTasks, PROTECTION_REASON_LOCKED));
        summary.setManualCount(countProtectionReason(protectedTasks, PROTECTION_REASON_MANUAL));
        summary.setOtherCount(protectedTasks.size() - summary.getFeedbackCount() - summary.getInProgressCount()
                - summary.getFinishedCount() - summary.getLockedCount() - summary.getManualCount());
        return summary;
    }

    private int countProtectionReason(List<MesProAutoScheduleProtectedTaskRespVO> protectedTasks, String reason) {
        return (int) protectedTasks.stream()
                .filter(task -> reason.equals(task.getProtectionReason()))
                .count();
    }

    private void insertScheduleApplyEventLogs(String operationType, MesProAutoSchedulePreviewReqVO reqVO,
                                              ScheduleComputation computation,
                                              MesProAutoScheduleApplyRespVO respVO,
                                              String requestId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operationType", operationType);
        payload.put("requestId", requestId);
        payload.put("scheduleOrderIds", reqVO.getScheduleOrderIds());
        payload.put("workOrderIds", reqVO.getWorkOrderIds());
        payload.put("capacityMode", reqVO.getRuntimeCapacityBasis());
        payload.put("reason", reqVO.getReason());
        payload.put("createdTaskIds", respVO.getCreatedTaskIds());
        payload.put("deletedTaskIds", respVO.getDeletedTaskIds());
        payload.put("preservedTaskIds", respVO.getPreservedTaskIds());
        payload.put("issueCount", computation.issues == null ? 0 : computation.issues.size());
        payload.put("summary", respVO.getSummary());
        String afterSnapshotJson = JsonUtils.toJsonString(payload);
        for (MesProScheduleOrderDO scheduleOrder : computation.scheduleOrders) {
            scheduleOrderOperationLogMapper.insert(MesProScheduleOrderOperationLogDO.builder()
                    .scheduleOrderId(scheduleOrder.getId())
                    .scheduleOrderCode(scheduleOrder.getCode())
                    .operationType(operationType)
                    .beforeSnapshotJson(JsonUtils.toJsonString(scheduleOrder))
                    .afterSnapshotJson(afterSnapshotJson)
                    .reason(reqVO.getReason())
                    .operatorId(SecurityFrameworkUtils.getLoginUserId())
                    .operatorName(SecurityFrameworkUtils.getLoginUserNickname())
                    .build());
        }
    }

    @Override
    public List<MesProAutoScheduleIssueRespVO> getIssues(MesProAutoScheduleIssueQueryReqVO reqVO) {
        List<MesProScheduleIssueDO> issues = scheduleIssueMapper.selectList(
                reqVO.getWorkOrderId(), reqVO.getTaskId(), reqVO.getIssueType(), reqVO.getSeverity());
        return buildIssueRespList(issues);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createIssue(MesProAutoScheduleIssueCreateReqVO reqVO) {
        MesProScheduleIssueDO issue = MesProScheduleIssueDO.builder()
                .issueType(reqVO.getIssueType())
                .severity(reqVO.getSeverity())
                .workOrderId(reqVO.getWorkOrderId())
                .taskId(reqVO.getTaskId())
                .processId(reqVO.getProcessId())
                .workstationId(reqVO.getWorkstationId())
                .calendarDate(reqVO.getOccurredAt())
                .message(reqVO.getMessage())
                .resolved(Boolean.FALSE)
                .status(ISSUE_STATUS_OPEN)
                .sourceType(reqVO.getSourceType())
                .sourceId(reqVO.getSourceId())
                .build();
        scheduleIssueMapper.insert(issue);
        return issue.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long cancelNightShift(@Valid MesProAutoScheduleCancelNightShiftReqVO reqVO) {
        MesProTaskDO task = taskMapper.selectById(reqVO.getTaskId());
        if (task == null) {
            throw new IllegalStateException("night shift task not found, id=" + reqVO.getTaskId());
        }
        if (!"NIGHT".equals(resolveShiftCodeForTask(task))) {
            throw new IllegalStateException("task is not a night shift task");
        }

        taskDependencyMapper.deleteByTaskIds(List.of(task.getId()));
        scheduleIssueMapper.deleteByTaskIds(List.of(task.getId()));
        taskScheduleExtMapper.deleteByTaskIds(List.of(task.getId()));
        taskMapper.deleteById(task.getId());

        MesProScheduleIssueDO issue = MesProScheduleIssueDO.builder()
                .issueType(ISSUE_TYPE_MANUAL_NIGHT_SHIFT_CANCEL)
                .severity(ISSUE_SEVERITY_WARNING)
                .workOrderId(task.getWorkOrderId())
                .taskId(task.getId())
                .processId(task.getProcessId())
                .workstationId(task.getWorkstationId())
                .calendarDate(task.getStartTime())
                .message(reqVO.getReason())
                .resolved(Boolean.TRUE)
                .status(ISSUE_STATUS_RESOLVED)
                .sourceType(SOURCE_TYPE_NIGHT_SHIFT_CANCEL)
                .sourceId(task.getId())
                .resolutionReason(reqVO.getReason())
                .resolvedBy(SecurityFrameworkUtils.getLoginUserId())
                .resolvedAt(LocalDateTime.now())
                .build();
        scheduleIssueMapper.insert(issue);
        return issue.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveIssue(MesProAutoScheduleIssueResolveReqVO reqVO) {
        MesProScheduleIssueDO existing = scheduleIssueMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw new IllegalStateException("????????id=" + reqVO.getId());
        }
        MesProScheduleIssueDO updateObj = new MesProScheduleIssueDO();
        updateObj.setId(reqVO.getId());
        updateObj.setResolved(Boolean.TRUE);
        updateObj.setStatus(ISSUE_STATUS_RESOLVED);
        updateObj.setResolutionReason(reqVO.getResolutionReason());
        updateObj.setResolvedBy(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setResolvedAt(LocalDateTime.now());
        scheduleIssueMapper.updateById(updateObj);
    }

    @Override
    public List<GanttLinkRespVO> getDependencies(List<Long> workOrderIds, List<Long> taskIds) {
        Set<Long> resolvedTaskIds = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(taskIds)) {
            resolvedTaskIds.addAll(taskIds);
        }
        if (CollUtil.isNotEmpty(workOrderIds)) {
            resolvedTaskIds.addAll(taskMapper.selectListByWorkOrderIds(workOrderIds).stream()
                    .map(MesProTaskDO::getId)
                    .toList());
        }
        if (resolvedTaskIds.isEmpty()) {
            return Collections.emptyList();
        }
        return taskDependencyMapper.selectListByTaskIds(resolvedTaskIds).stream()
                .map(this::toGanttLink)
                .toList();
    }

    private void validateNoFrozenWorkOrders(Collection<Long> workOrderIds) {
        if (CollUtil.isEmpty(workOrderIds)) {
            return;
        }
        List<MesProWorkOrderDO> workOrders = workOrderService.getWorkOrderList(workOrderIds);
        if (workOrders.stream().anyMatch(workOrder -> Boolean.TRUE.equals(workOrder.getTemporaryFrozen()))) {
            throw exception(PRO_AUTO_SCHEDULE_FROZEN_WORK_ORDER);
        }
    }

    private void resolveScheduleOrderScope(ScheduleComputation computation, MesProAutoSchedulePreviewReqVO reqVO,
                                           boolean persistRouteConfigSnapshot) {
        List<MesProScheduleOrderDO> scheduleOrders;
        if (CollUtil.isNotEmpty(reqVO.getScheduleOrderIds())) {
            List<Long> requestedIds = reqVO.getScheduleOrderIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            List<MesProScheduleOrderDO> requestedOrders = scheduleOrderMapper.selectByIds(requestedIds);
            scheduleOrders = scheduleOrderMapper.selectAutoSchedulableByIds(requestedIds);
            validateRequestedScheduleOrderScope(requestedIds, requestedOrders, scheduleOrders);
        } else {
            scheduleOrders = Collections.emptyList();
        }
        if (CollUtil.isEmpty(scheduleOrders)) {
            computation.scheduleOrders = Collections.emptyList();
            computation.scheduleOrderMap = Collections.emptyMap();
            computation.scheduleOrderProcessesByOrderId = Collections.emptyMap();
            return;
        }
        Map<Long, MesProScheduleOrderDO> scheduleOrderMap = new LinkedHashMap<>();
        for (MesProScheduleOrderDO scheduleOrder : scheduleOrders) {
            if (scheduleOrder.getWorkOrderId() == null) {
                continue;
            }
            scheduleOrderMap.putIfAbsent(scheduleOrder.getId(), scheduleOrder);
        }
        Map<Long, List<MesProScheduleOrderProcessDO>> processMap = new LinkedHashMap<>();
        for (MesProScheduleOrderDO scheduleOrder : scheduleOrderMap.values()) {
            processMap.put(scheduleOrder.getId(), refreshScheduleOrderProcessesFromRouteConfig(scheduleOrder,
                    scheduleOrderProcessMapper.selectListByScheduleOrderId(scheduleOrder.getId()),
                    persistRouteConfigSnapshot));
        }
        computation.scheduleOrders = schedulePlanner.sortScheduleOrders(scheduleOrderMap.values(), processMap);
        computation.scheduleOrderMap = computation.scheduleOrders.stream()
                .collect(Collectors.toMap(MesProScheduleOrderDO::getId, order -> order));
        Set<Long> workOrderIds = computation.scheduleOrders.stream()
                .map(MesProScheduleOrderDO::getWorkOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        reqVO.setWorkOrderIds(new ArrayList<>(workOrderIds));
        computation.scheduleOrderProcessesByOrderId = processMap;
    }

    private List<MesProScheduleOrderProcessDO> refreshScheduleOrderProcessesFromRouteConfig(
            MesProScheduleOrderDO scheduleOrder, List<MesProScheduleOrderProcessDO> processes,
            boolean persistRouteConfigSnapshot) {
        if (scheduleOrder == null || CollUtil.isEmpty(processes)) {
            return processes;
        }
        MesProRouteVersionDO routeVersion = resolveLatestPublishedRouteVersion(scheduleOrder);
        Long routeVersionId = routeVersion.getId();
        List<MesProRouteScheduleConfigDO> configs = routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId);
        if (CollUtil.isEmpty(configs)) {
            return processes;
        }
        Map<Long, MesProRouteProcessDO> latestRouteProcessByConfigRouteProcessId =
                resolveLatestPublishedRouteProcesses(scheduleOrder.getRouteId(), configs);
        normalizeScheduleOrderProcessIdentities(scheduleOrder, processes, latestRouteProcessByConfigRouteProcessId,
                persistRouteConfigSnapshot);
        Map<Long, MesProRouteScheduleConfigDO> configByRouteProcessId =
                buildLatestPublishedConfigByRouteProcessId(configs, latestRouteProcessByConfigRouteProcessId);
        if (configByRouteProcessId.isEmpty()) {
            return processes;
        }
        Map<Long, MesProRouteProcessDO> latestRouteProcessById =
                buildRouteProcessById(latestRouteProcessByConfigRouteProcessId.values());
        Map<Long, MesProRouteFlowProcessConfigDO> scheduleRouteFlowConfigByRouteProcessId =
                resolveScheduleRouteFlowConfigByRouteProcessId(scheduleOrder.getRouteId());
        Map<Long, LatestPublishedCapacitySnapshot> capacitySnapshotByRouteProcessId =
                buildLatestPublishedCapacitySnapshotByRouteProcessId(configByRouteProcessId,
                        latestRouteProcessById, buildScheduleOrderProcessByRouteProcessId(processes));
        Set<Long> changedProcessIds = new LinkedHashSet<>();
        for (MesProScheduleOrderProcessDO process : processes) {
            if (process == null || process.getRouteProcessId() == null) {
                continue;
            }
            MesProRouteScheduleConfigDO config = configByRouteProcessId.get(process.getRouteProcessId());
            if (config == null) {
                continue;
            }
            LatestPublishedCapacitySnapshot capacitySnapshot =
                    capacitySnapshotByRouteProcessId.get(process.getRouteProcessId());
            MesProRouteProcessDO latestRouteProcess = latestRouteProcessById.get(process.getRouteProcessId());
            BigDecimal productionQuantityFactor = resolveProductionQuantityFactor(latestRouteProcess,
                    scheduleRouteFlowConfigByRouteProcessId.get(process.getRouteProcessId()));
            BigDecimal plannedQuantity = calculateLatestPlannedQuantity(scheduleOrder, productionQuantityFactor);
            boolean changed = isRouteScheduleSnapshotChanged(process, config)
                    || isRouteScheduleCapacitySnapshotChanged(process, capacitySnapshot)
                    || isRouteScheduleQuantitySnapshotChanged(process, productionQuantityFactor, plannedQuantity);
            process.setRouteVersionId(routeVersionId);
            process.setRouteScheduleConfigId(config.getId());
            process.setCapacityMode(config.getCapacityMode());
            process.setInfiniteDurationQuantityFactor(config.getInfiniteDurationQuantityFactor());
            process.setInfiniteDurationBaseMinutes(config.getInfiniteDurationBaseMinutes());
            process.setNightShiftEnabled(Boolean.TRUE.equals(config.getNightShiftEnabled()));
            process.setCalendarRuleId(config.getCalendarRuleId());
            process.setProductionQuantityFactor(productionQuantityFactor);
            process.setPlannedQuantity(plannedQuantity);
            applyLatestPublishedCapacitySnapshot(process, capacitySnapshot);
            if (changed && process.getId() != null) {
                changedProcessIds.add(process.getId());
            }
        }
        refreshScheduleOrderProcessProgress(scheduleOrder, processes, changedProcessIds);
        if (persistRouteConfigSnapshot && CollUtil.isNotEmpty(changedProcessIds)) {
            for (MesProScheduleOrderProcessDO process : processes) {
                if (process != null && changedProcessIds.contains(process.getId())) {
                    persistRouteScheduleConfigSnapshot(process);
                }
            }
        }
        return processes;
    }

    private Map<Long, MesProRouteFlowProcessConfigDO> resolveScheduleRouteFlowConfigByRouteProcessId(Long routeId) {
        String useType = MesProRouteFlowConfigTypeEnum.SCHEDULE.getType();
        MesProRouteFlowConfigDO flowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, useType);
        if (!MesProRouteFlowContextMatcher.isEnabledFlowContext(flowConfig, routeId, useType)) {
            return Collections.emptyMap();
        }
        List<MesProRouteFlowProcessConfigDO> configs = routeFlowProcessConfigMapper
                .selectListByRouteIdAndUseType(routeId, useType);
        if (CollUtil.isEmpty(configs)) {
            return Collections.emptyMap();
        }
        return configs.stream()
                .filter(config -> MesProRouteFlowContextMatcher.isProcessConfigOwnedBy(
                        flowConfig, config, routeId, useType))
                .filter(config -> config.getRouteProcessId() != null)
                .collect(Collectors.toMap(MesProRouteFlowProcessConfigDO::getRouteProcessId, config -> config,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private BigDecimal resolveProductionQuantityFactor(MesProRouteProcessDO routeProcess,
                                                       MesProRouteFlowProcessConfigDO scheduleRouteFlowConfig) {
        if (scheduleRouteFlowConfig == null || scheduleRouteFlowConfig.getProductionQuantityFactor() == null) {
            Long routeProcessId = routeProcess == null ? null : routeProcess.getId();
            throw exception(PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID, routeProcessId);
        }
        BigDecimal factor = scheduleRouteFlowConfig.getProductionQuantityFactor();
        if (factor.compareTo(BigDecimal.ZERO) <= 0) {
            Long routeProcessId = routeProcess == null ? scheduleRouteFlowConfig.getRouteProcessId() : routeProcess.getId();
            throw exception(PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID, routeProcessId);
        }
        return factor.setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLatestPlannedQuantity(MesProScheduleOrderDO scheduleOrder,
                                                      BigDecimal productionQuantityFactor) {
        BigDecimal baseQuantity = normalizeProcessQuantity(scheduleOrder == null ? null : scheduleOrder.getQuantity());
        if (productionQuantityFactor == null || productionQuantityFactor.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID, null);
        }
        return baseQuantity.multiply(productionQuantityFactor)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private void refreshScheduleOrderProcessProgress(MesProScheduleOrderDO scheduleOrder,
                                                     List<MesProScheduleOrderProcessDO> processes,
                                                     Set<Long> changedProcessIds) {
        if (scheduleOrder == null || scheduleOrder.getId() == null || CollUtil.isEmpty(processes)) {
            return;
        }
        Map<Long, ProcessProgressMetrics> metricsByProcessId = Objects.requireNonNull(
                scheduleOrderService.calculateProcessProgressMetrics(scheduleOrder.getId(), processes),
                "process progress metrics must not be null");
        if (metricsByProcessId.isEmpty()) {
            return;
        }
        for (MesProScheduleOrderProcessDO process : processes) {
            if (process == null || process.getId() == null) {
                continue;
            }
            ProcessProgressMetrics metrics = metricsByProcessId.get(process.getId());
            if (metrics == null) {
                continue;
            }
            if (isRouteScheduleProgressSnapshotChanged(process, metrics)) {
                changedProcessIds.add(process.getId());
            }
            process.setReportedQuantity(metrics.reportedQuantity());
            process.setRemainingQuantity(metrics.remainingQuantity());
            process.setProgressPercent(metrics.progressPercent());
        }
    }

    private boolean isRouteScheduleQuantitySnapshotChanged(MesProScheduleOrderProcessDO process,
                                                           BigDecimal productionQuantityFactor,
                                                           BigDecimal plannedQuantity) {
        return !processQuantityEquals(process.getProductionQuantityFactor(), productionQuantityFactor)
                || !processQuantityEquals(process.getPlannedQuantity(), plannedQuantity);
    }

    private boolean isRouteScheduleProgressSnapshotChanged(MesProScheduleOrderProcessDO process,
                                                           ProcessProgressMetrics metrics) {
        return !processQuantityEquals(process.getReportedQuantity(), metrics.reportedQuantity())
                || !processQuantityEquals(process.getRemainingQuantity(), metrics.remainingQuantity())
                || !processQuantityEquals(process.getProgressPercent(), metrics.progressPercent());
    }

    private boolean processQuantityEquals(BigDecimal left, BigDecimal right) {
        return normalizeProcessQuantity(left).compareTo(normalizeProcessQuantity(right)) == 0;
    }

    private BigDecimal normalizeProcessQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(6) : value.setScale(6, RoundingMode.HALF_UP);
    }

    private Map<Long, MesProRouteProcessDO> buildRouteProcessById(Collection<MesProRouteProcessDO> routeProcesses) {
        return Optional.ofNullable(routeProcesses).orElse(Collections.emptyList()).stream()
                .filter(routeProcess -> routeProcess != null && routeProcess.getId() != null)
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, routeProcess -> routeProcess,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, MesProScheduleOrderProcessDO> buildScheduleOrderProcessByRouteProcessId(
            Collection<MesProScheduleOrderProcessDO> processes) {
        return Optional.ofNullable(processes).orElse(Collections.emptyList()).stream()
                .filter(process -> process != null && process.getRouteProcessId() != null)
                .collect(Collectors.toMap(MesProScheduleOrderProcessDO::getRouteProcessId, process -> process,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, LatestPublishedCapacitySnapshot> buildLatestPublishedCapacitySnapshotByRouteProcessId(
            Map<Long, MesProRouteScheduleConfigDO> configByRouteProcessId,
            Map<Long, MesProRouteProcessDO> routeProcessById,
            Map<Long, MesProScheduleOrderProcessDO> scheduleOrderProcessByRouteProcessId) {
        if (configByRouteProcessId.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, MesMdWorkstationDO> workstationByRouteProcessId =
                loadLatestPublishedWorkstationByRouteProcessId(configByRouteProcessId, routeProcessById);
        Map<Long, MesMdWorkstationCapacityMetrics> metricsByWorkstationId =
                loadLatestPublishedCapacityMetrics(configByRouteProcessId, workstationByRouteProcessId);
        Map<Long, LatestPublishedCapacitySnapshot> result = new LinkedHashMap<>();
        boolean unboundWorkbenchShiftHoursResolved = false;
        BigDecimal unboundWorkbenchShiftHours = null;
        for (Map.Entry<Long, MesProRouteScheduleConfigDO> entry : configByRouteProcessId.entrySet()) {
            MesProRouteProcessDO routeProcess = routeProcessById.get(entry.getKey());
            MesMdWorkstationDO workstation = workstationByRouteProcessId.get(entry.getKey());
            if (shouldUseWorkbenchShiftHoursForUnboundRouteProcess(entry.getValue(), routeProcess, workstation)
                    && !unboundWorkbenchShiftHoursResolved) {
                unboundWorkbenchShiftHours = resolveUnifiedWorkbenchShiftHoursOrNull(routeProcess);
                unboundWorkbenchShiftHoursResolved = true;
            }
            MesMdWorkstationCapacityMetrics metrics = workstation == null
                    ? null : metricsByWorkstationId.get(workstation.getId());
            result.put(entry.getKey(), buildLatestPublishedCapacitySnapshot(
                    entry.getValue(), routeProcess, workstation, metrics,
                    unboundWorkbenchShiftHours));
        }
        return result;
    }

    private boolean shouldUseWorkbenchShiftHoursForUnboundRouteProcess(MesProRouteScheduleConfigDO config,
                                                                       MesProRouteProcessDO routeProcess,
                                                                       MesMdWorkstationDO workstation) {
        return workstation == null
                && routeProcess != null
                && routeProcess.getWorkstationId() == null
                && config != null
                && MesProScheduleCapacityModeEnum.isManualOverrideLike(config.getCapacityMode());
    }

    private Map<Long, MesMdWorkstationDO> loadLatestPublishedWorkstationByRouteProcessId(
            Map<Long, MesProRouteScheduleConfigDO> configByRouteProcessId,
            Map<Long, MesProRouteProcessDO> routeProcessById) {
        if (CollUtil.isEmpty(configByRouteProcessId)) {
            return Collections.emptyMap();
        }
        Set<Long> workstationIds = new LinkedHashSet<>();
        for (Map.Entry<Long, MesProRouteScheduleConfigDO> entry : configByRouteProcessId.entrySet()) {
            MesProRouteProcessDO routeProcess = routeProcessById.get(entry.getKey());
            if (routeProcess == null || routeProcess.getId() == null) {
                continue;
            }
            if (routeProcess.getWorkstationId() == null) {
                if (requiresLatestPublishedWorkstation(entry.getValue())) {
                    throw exception(PRO_AUTO_SCHEDULE_WORKSTATION_REQUIRED);
                }
                continue;
            }
            workstationIds.add(routeProcess.getWorkstationId());
        }
        List<MesMdWorkstationDO> workstations = CollUtil.isEmpty(workstationIds)
                ? Collections.emptyList()
                : ObjUtil.defaultIfNull(workstationMapper.selectByIds(new ArrayList<>(workstationIds)),
                Collections.emptyList());
        normalizeLatestPublishedWorkstationProcessIds(routeProcessById.values(), workstations);
        Map<Long, MesMdWorkstationDO> workstationById = workstations.stream()
                .filter(workstation -> workstation != null && workstation.getId() != null)
                .collect(Collectors.toMap(MesMdWorkstationDO::getId, workstation -> workstation,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, MesMdWorkstationDO> result = new LinkedHashMap<>();
        for (Map.Entry<Long, MesProRouteScheduleConfigDO> entry : configByRouteProcessId.entrySet()) {
            MesProRouteProcessDO routeProcess = routeProcessById.get(entry.getKey());
            if (routeProcess == null || routeProcess.getId() == null) {
                continue;
            }
            if (routeProcess.getWorkstationId() == null && !requiresLatestPublishedWorkstation(entry.getValue())) {
                continue;
            }
            MesMdWorkstationDO workstation = workstationById.get(routeProcess.getWorkstationId());
            if (workstation == null || !Objects.equals(CommonStatusEnum.ENABLE.getStatus(), workstation.getStatus())) {
                throw exception(PRO_AUTO_SCHEDULE_WORKSTATION_REQUIRED);
            }
            if (!Objects.equals(workstation.getProcessId(), routeProcess.getProcessId())) {
                throw exception(PRO_WORKSTATION_PROCESS_MISMATCH);
            }
            result.put(routeProcess.getId(), workstation);
        }
        return result;
    }

    private boolean requiresLatestPublishedWorkstation(MesProRouteScheduleConfigDO config) {
        return config == null
                || !MesProScheduleCapacityModeEnum.isManualOverrideLike(config.getCapacityMode());
    }

    private void normalizeLatestPublishedWorkstationProcessIds(Collection<MesProRouteProcessDO> routeProcesses,
                                                               List<MesMdWorkstationDO> workstations) {
        Set<Long> processIds = Optional.ofNullable(routeProcesses).orElse(Collections.emptyList()).stream()
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(processIds) || CollUtil.isEmpty(workstations)) {
            return;
        }
        Map<Long, Long> processIdentityMap = ObjUtil.defaultIfNull(
                routeProcessService.getProcessIdentityMap(processIds), Collections.emptyMap());
        workstations.forEach(workstation -> {
            Long currentProcessId = processIdentityMap.get(workstation.getProcessId());
            if (currentProcessId != null) {
                workstation.setProcessId(currentProcessId);
            }
        });
    }

    private Map<Long, MesMdWorkstationCapacityMetrics> loadLatestPublishedCapacityMetrics(
            Map<Long, MesProRouteScheduleConfigDO> configByRouteProcessId,
            Map<Long, MesMdWorkstationDO> workstationByRouteProcessId) {
        List<MesMdWorkstationDO> resourceWorkstations = configByRouteProcessId.entrySet().stream()
                .filter(entry -> entry.getValue() != null
                        && !MesProScheduleCapacityModeEnum.isManualOverrideLike(entry.getValue().getCapacityMode()))
                .map(entry -> workstationByRouteProcessId.get(entry.getKey()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(resourceWorkstations)) {
            return Collections.emptyMap();
        }
        return ObjUtil.defaultIfNull(workstationCapacityService.getCapacityMetricsUsingShiftHours(resourceWorkstations),
                Collections.emptyMap());
    }

    private LatestPublishedCapacitySnapshot buildLatestPublishedCapacitySnapshot(
            MesProRouteScheduleConfigDO config,
            MesProRouteProcessDO routeProcess,
            MesMdWorkstationDO workstation,
            MesMdWorkstationCapacityMetrics metrics,
            BigDecimal unboundWorkbenchShiftHours) {
        if (config == null || routeProcess == null) {
            throw exception(PRO_AUTO_SCHEDULE_WORKSTATION_REQUIRED);
        }
        BigDecimal shiftHours = requireLatestPublishedShiftHours(routeProcess, workstation, unboundWorkbenchShiftHours);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("routeScheduleConfigId", config.getId());
        payload.put("routeProcessId", routeProcess.getId());
        payload.put("processId", routeProcess.getProcessId());
        if (workstation != null) {
            payload.put("workstationId", workstation.getId());
            payload.put("workstationCode", workstation.getCode());
            payload.put("workstationName", workstation.getName());
            payload.put("productionLineId", workstation.getProductionLineId());
        }
        payload.put("capacityMode", config.getCapacityMode());
        payload.put("configVersion", config.getConfigVersion());
        payload.put("shiftHours", shiftHours);
        payload.put("nightShiftEnabled", Boolean.TRUE.equals(config.getNightShiftEnabled()));
        payload.put("calendarRuleId", config.getCalendarRuleId());

        if (MesProScheduleCapacityModeEnum.isManualOverrideLike(config.getCapacityMode())) {
            BigDecimal hourlyCapacity = ObjUtil.defaultIfNull(config.getHourlyCapacity(), BigDecimal.ZERO);
            if (hourlyCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_AUTO_SCHEDULE_CAPACITY_REQUIRED);
            }
            BigDecimal shiftCapacity = hourlyCapacity.multiply(shiftHours);
            payload.put("capacitySource", CAPACITY_SOURCE_ROUTE_PROCESS);
            payload.put("hourlyCapacityTotal", hourlyCapacity);
            payload.put("shiftCapacityTotal", shiftCapacity);
            return new LatestPublishedCapacitySnapshot(CAPACITY_SOURCE_ROUTE_PROCESS, hourlyCapacity,
                    shiftHours, shiftCapacity, JsonUtils.toJsonString(payload));
        }

        MesMdWorkstationCapacityMetrics checkedMetrics =
                requireLatestPublishedCapacityMetrics(routeProcess, workstation, metrics);
        BigDecimal shiftCapacity = ObjUtil.defaultIfNull(checkedMetrics.getTodayCapacity(), BigDecimal.ZERO);
        BigDecimal hourlyCapacity = shiftCapacity.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : shiftCapacity.divide(shiftHours, 8, RoundingMode.HALF_UP).stripTrailingZeros();
        String capacitySource = resolveLatestPublishedCapacitySource(checkedMetrics);
        payload.put("capacitySource", capacitySource);
        payload.put("hourlyCapacityTotal", hourlyCapacity);
        payload.put("shiftCapacityTotal", shiftCapacity);
        payload.put("machineryStandardHourlyCapacity", checkedMetrics.getMachineryStandardHourlyCapacity());
        payload.put("configuredWorkerCount", checkedMetrics.getConfiguredWorkerCount());
        payload.put("currentWorkerCount", checkedMetrics.getCurrentWorkerCount());
        if (MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(config.getCapacityMode())) {
            payload.put("infiniteDurationQuantityFactor", config.getInfiniteDurationQuantityFactor());
            payload.put("infiniteDurationBaseMinutes", config.getInfiniteDurationBaseMinutes());
        }
        return new LatestPublishedCapacitySnapshot(capacitySource, hourlyCapacity, shiftHours, shiftCapacity,
                JsonUtils.toJsonString(payload));
    }

    private BigDecimal requireLatestPublishedShiftHours(MesProRouteProcessDO routeProcess,
                                                        MesMdWorkstationDO workstation,
                                                        BigDecimal unboundWorkbenchShiftHours) {
        if (workstation != null) {
            return scheduleDefaultCompatibilityPolicy.shiftHoursOrDefault(workstation.getShiftHours());
        }
        BigDecimal shiftHours = normalizePositiveShiftHours(unboundWorkbenchShiftHours);
        return shiftHours == null ? scheduleDefaultCompatibilityPolicy.defaultShiftHoursWhenMissing() : shiftHours;
    }

    private BigDecimal resolveUnifiedWorkbenchShiftHoursOrNull(MesProRouteProcessDO routeProcess) {
        List<MesMdWorkstationDO> workstations = ObjUtil.defaultIfNull(
                workstationMapper.selectListForShiftHours(), Collections.emptyList());
        if (CollUtil.isEmpty(workstations)) {
            return null;
        }
        boolean hasMissingShiftHours = workstations.stream()
                .map(workstation -> workstation == null ? null : workstation.getShiftHours())
                .anyMatch(shiftHours -> normalizePositiveShiftHours(shiftHours) == null);
        if (hasMissingShiftHours) {
            return scheduleDefaultCompatibilityPolicy.defaultShiftHoursWhenMissing();
        }
        BigDecimal unifiedShiftHours = null;
        for (MesMdWorkstationDO workstation : workstations) {
            BigDecimal shiftHours = normalizePositiveShiftHours(workstation == null ? null : workstation.getShiftHours());
            Long workstationId = workstation == null ? null : workstation.getId();
            if (unifiedShiftHours == null) {
                unifiedShiftHours = shiftHours;
                continue;
            }
            if (unifiedShiftHours.compareTo(shiftHours) != 0) {
                throw exception(PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED,
                        routeProcess == null ? null : routeProcess.getId(), workstationId);
            }
        }
        return unifiedShiftHours;
    }

    private BigDecimal normalizePositiveShiftHours(BigDecimal shiftHours) {
        return shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0 ? null : shiftHours;
    }

    private MesMdWorkstationCapacityMetrics requireLatestPublishedCapacityMetrics(
            MesProRouteProcessDO routeProcess,
            MesMdWorkstationDO workstation,
            MesMdWorkstationCapacityMetrics metrics) {
        if (metrics == null || metrics.getTodayCapacity() == null
                || metrics.getTodayCapacity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_AUTO_SCHEDULE_CAPACITY_REQUIRED);
        }
        return metrics;
    }

    private String resolveLatestPublishedCapacitySource(MesMdWorkstationCapacityMetrics metrics) {
        return metrics.getMachineryStandardHourlyCapacity() != null
                && metrics.getMachineryStandardHourlyCapacity().compareTo(BigDecimal.ZERO) > 0
                ? CAPACITY_SOURCE_MACHINE : CAPACITY_SOURCE_WORKER;
    }

    private boolean isRouteScheduleCapacitySnapshotChanged(MesProScheduleOrderProcessDO process,
                                                           LatestPublishedCapacitySnapshot capacitySnapshot) {
        return capacitySnapshot == null
                || !Objects.equals(process.getCapacitySource(), capacitySnapshot.capacitySource)
                || !Objects.equals(process.getHourlyCapacityTotal(), capacitySnapshot.hourlyCapacityTotal)
                || !Objects.equals(process.getShiftHours(), capacitySnapshot.shiftHours)
                || !Objects.equals(process.getShiftCapacityTotal(), capacitySnapshot.shiftCapacityTotal)
                || !Objects.equals(process.getResourceSnapshotJson(), capacitySnapshot.resourceSnapshotJson);
    }

    private void applyLatestPublishedCapacitySnapshot(MesProScheduleOrderProcessDO process,
                                                      LatestPublishedCapacitySnapshot capacitySnapshot) {
        process.setCapacitySource(capacitySnapshot.capacitySource);
        process.setHourlyCapacityTotal(capacitySnapshot.hourlyCapacityTotal);
        process.setShiftHours(capacitySnapshot.shiftHours);
        process.setShiftCapacityTotal(capacitySnapshot.shiftCapacityTotal);
        process.setResourceSnapshotJson(capacitySnapshot.resourceSnapshotJson);
    }

    private static final class LatestPublishedCapacitySnapshot {
        private final String capacitySource;
        private final BigDecimal hourlyCapacityTotal;
        private final BigDecimal shiftHours;
        private final BigDecimal shiftCapacityTotal;
        private final String resourceSnapshotJson;

        private LatestPublishedCapacitySnapshot(String capacitySource,
                                                BigDecimal hourlyCapacityTotal,
                                                BigDecimal shiftHours,
                                                BigDecimal shiftCapacityTotal,
                                                String resourceSnapshotJson) {
            this.capacitySource = capacitySource;
            this.hourlyCapacityTotal = hourlyCapacityTotal;
            this.shiftHours = shiftHours;
            this.shiftCapacityTotal = shiftCapacityTotal;
            this.resourceSnapshotJson = resourceSnapshotJson;
        }
    }

    private void normalizeScheduleOrderProcessIdentities(MesProScheduleOrderDO scheduleOrder,
                                                          List<MesProScheduleOrderProcessDO> processes,
                                                          Map<Long, MesProRouteProcessDO> latestRouteProcessByConfigRouteProcessId,
                                                          boolean persistIdentitySnapshot) {
        if (scheduleOrder.getRouteId() == null || CollUtil.isEmpty(latestRouteProcessByConfigRouteProcessId)) {
            return;
        }
        Map<Long, MesProRouteProcessDO> latestRouteProcessById = latestRouteProcessByConfigRouteProcessId.values()
                .stream()
                .filter(routeProcess -> routeProcess != null && routeProcess.getId() != null)
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, routeProcess -> routeProcess,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, Long> currentRouteProcessIdBySnapshotId = new LinkedHashMap<>();
        Set<Long> changedProcessIds = new LinkedHashSet<>();
        for (MesProScheduleOrderProcessDO process : processes) {
            if (process == null || process.getRouteProcessId() == null || Boolean.FALSE.equals(process.getEnabled())) {
                continue;
            }
            Long snapshotRouteProcessId = process.getRouteProcessId();
            MesProRouteProcessDO latestRouteProcess = resolveLatestPublishedRouteProcess(
                    scheduleOrder.getRouteId(), process, latestRouteProcessById);
            currentRouteProcessIdBySnapshotId.put(snapshotRouteProcessId, latestRouteProcess.getId());
            boolean routeProcessChanged = !Objects.equals(snapshotRouteProcessId, latestRouteProcess.getId());
            boolean processChanged = !Objects.equals(process.getProcessId(), latestRouteProcess.getProcessId());
            if (routeProcessChanged || processChanged) {
                process.setRouteProcessId(latestRouteProcess.getId());
                process.setProcessId(latestRouteProcess.getProcessId());
                changedProcessIds.add(process.getId());
            }
        }
        for (MesProScheduleOrderProcessDO process : processes) {
            if (process == null || process.getPredecessorRouteProcessId() == null || Boolean.FALSE.equals(process.getEnabled())) {
                continue;
            }
            Long snapshotPredecessorId = process.getPredecessorRouteProcessId();
            Long currentPredecessorId = currentRouteProcessIdBySnapshotId.computeIfAbsent(
                    snapshotPredecessorId,
                    id -> resolveLatestPublishedRouteProcessId(
                            scheduleOrder.getRouteId(), id, latestRouteProcessById));
            if (!Objects.equals(snapshotPredecessorId, currentPredecessorId)) {
                process.setPredecessorRouteProcessId(currentPredecessorId);
                changedProcessIds.add(process.getId());
            }
        }
        if (persistIdentitySnapshot) {
            processes.stream()
                    .filter(process -> process != null && changedProcessIds.contains(process.getId()))
                    .forEach(this::persistRouteProcessIdentitySnapshot);
        }
    }

    private void persistRouteProcessIdentitySnapshot(MesProScheduleOrderProcessDO process) {
        MesProScheduleOrderProcessDO updateObj = new MesProScheduleOrderProcessDO();
        updateObj.setId(process.getId());
        updateObj.setRouteProcessId(process.getRouteProcessId());
        updateObj.setPredecessorRouteProcessId(process.getPredecessorRouteProcessId());
        updateObj.setProcessId(process.getProcessId());
        scheduleOrderProcessMapper.updateById(updateObj);
    }

    private Map<Long, MesProRouteProcessDO> resolveLatestPublishedRouteProcesses(
            Long routeId, List<MesProRouteScheduleConfigDO> configs) {
        Map<Long, MesProRouteProcessDO> result = new LinkedHashMap<>();
        for (MesProRouteScheduleConfigDO config : Optional.ofNullable(configs).orElse(Collections.emptyList())) {
            if (config == null || config.getRouteProcessId() == null) {
                continue;
            }
            MesProRouteProcessDO routeProcess = routeProcessService.resolveFrozenRouteProcess(
                    config.getRouteProcessId(), routeId, null);
            result.put(config.getRouteProcessId(), routeProcess);
        }
        return result;
    }

    private Map<Long, MesProRouteScheduleConfigDO> buildLatestPublishedConfigByRouteProcessId(
            List<MesProRouteScheduleConfigDO> configs,
            Map<Long, MesProRouteProcessDO> latestRouteProcessByConfigRouteProcessId) {
        Map<Long, MesProRouteScheduleConfigDO> result = new LinkedHashMap<>();
        for (MesProRouteScheduleConfigDO config : Optional.ofNullable(configs).orElse(Collections.emptyList())) {
            if (config == null || config.getRouteProcessId() == null) {
                continue;
            }
            MesProRouteProcessDO routeProcess = latestRouteProcessByConfigRouteProcessId.get(config.getRouteProcessId());
            if (routeProcess == null || routeProcess.getId() == null) {
                continue;
            }
            result.putIfAbsent(routeProcess.getId(), config);
        }
        return result;
    }

    private MesProRouteProcessDO resolveLatestPublishedRouteProcess(
            Long routeId,
            MesProScheduleOrderProcessDO process,
            Map<Long, MesProRouteProcessDO> latestRouteProcessById) {
        Long routeProcessId = process.getRouteProcessId();
        MesProRouteProcessDO exact = latestRouteProcessById.get(routeProcessId);
        if (exact != null) {
            return exact;
        }
        MesProRouteProcessDO resolved = routeProcessService.resolveCurrentRouteProcess(
                routeProcessId, routeId, process.getProcessId());
        MesProRouteProcessDO latest = resolved == null ? null : latestRouteProcessById.get(resolved.getId());
        if (latest != null) {
            return latest;
        }
        throw new IllegalStateException("最新已发布路线配置缺少排产工序，routeId=" + routeId
                + ", scheduleOrderProcessId=" + process.getId()
                + ", routeProcessId=" + routeProcessId
                + ", processId=" + process.getProcessId());
    }

    private Long resolveLatestPublishedRouteProcessId(
            Long routeId,
            Long routeProcessId,
            Map<Long, MesProRouteProcessDO> latestRouteProcessById) {
        MesProRouteProcessDO exact = latestRouteProcessById.get(routeProcessId);
        if (exact != null) {
            return exact.getId();
        }
        MesProRouteProcessDO resolved = routeProcessService.resolveCurrentRouteProcess(routeProcessId, routeId, null);
        MesProRouteProcessDO latest = resolved == null ? null : latestRouteProcessById.get(resolved.getId());
        if (latest != null) {
            return latest.getId();
        }
        throw new IllegalStateException("最新已发布路线配置缺少前置工序，routeId=" + routeId
                + ", predecessorRouteProcessId=" + routeProcessId);
    }

    private boolean isRouteScheduleSnapshotChanged(MesProScheduleOrderProcessDO process,
                                                   MesProRouteScheduleConfigDO config) {
        return !Objects.equals(process.getRouteScheduleConfigId(), config.getId())
                || !Objects.equals(process.getRouteVersionId(), config.getRouteVersionId())
                || !Objects.equals(process.getCapacityMode(), config.getCapacityMode())
                || !Objects.equals(process.getHourlyCapacityTotal(), config.getHourlyCapacity())
                || !Objects.equals(process.getInfiniteDurationQuantityFactor(), config.getInfiniteDurationQuantityFactor())
                || !Objects.equals(process.getInfiniteDurationBaseMinutes(), config.getInfiniteDurationBaseMinutes())
                || !Objects.equals(process.getNightShiftEnabled(), Boolean.TRUE.equals(config.getNightShiftEnabled()))
                || !Objects.equals(process.getCalendarRuleId(), config.getCalendarRuleId());
    }

    private void persistRouteScheduleConfigSnapshot(MesProScheduleOrderProcessDO process) {
        MesProScheduleOrderProcessDO updateObj = new MesProScheduleOrderProcessDO();
        updateObj.setId(process.getId());
        updateObj.setRouteVersionId(process.getRouteVersionId());
        updateObj.setRouteScheduleConfigId(process.getRouteScheduleConfigId());
        updateObj.setCapacitySource(process.getCapacitySource());
        updateObj.setCapacityMode(process.getCapacityMode());
        updateObj.setHourlyCapacityTotal(process.getHourlyCapacityTotal());
        updateObj.setInfiniteDurationQuantityFactor(process.getInfiniteDurationQuantityFactor());
        updateObj.setInfiniteDurationBaseMinutes(process.getInfiniteDurationBaseMinutes());
        updateObj.setShiftHours(process.getShiftHours());
        updateObj.setShiftCapacityTotal(process.getShiftCapacityTotal());
        updateObj.setProductionQuantityFactor(process.getProductionQuantityFactor());
        updateObj.setResourceSnapshotJson(process.getResourceSnapshotJson());
        updateObj.setPlannedQuantity(process.getPlannedQuantity());
        updateObj.setReportedQuantity(process.getReportedQuantity());
        updateObj.setRemainingQuantity(process.getRemainingQuantity());
        updateObj.setProgressPercent(process.getProgressPercent());
        updateObj.setNightShiftEnabled(process.getNightShiftEnabled());
        updateObj.setCalendarRuleId(process.getCalendarRuleId());
        scheduleOrderProcessMapper.updateById(updateObj);
    }

    private MesProRouteVersionDO resolveLatestPublishedRouteVersion(MesProScheduleOrderDO scheduleOrder) {
        if (scheduleOrder.getRouteId() == null) {
            throw exception(PRO_AUTO_SCHEDULE_ROUTE_VERSION_REQUIRED, scheduleOrder.getId());
        }
        MesProRouteVersionDO activeRouteVersion = routeVersionMapper.selectActiveByRouteId(scheduleOrder.getRouteId());
        if (activeRouteVersion != null && activeRouteVersion.getId() != null) {
            return activeRouteVersion;
        }
        throw exception(PRO_AUTO_SCHEDULE_ROUTE_VERSION_REQUIRED, scheduleOrder.getId());
    }

    private void validateRequestedScheduleOrderScope(List<Long> requestedIds,
                                                     List<MesProScheduleOrderDO> requestedOrders,
                                                     List<MesProScheduleOrderDO> schedulableOrders) {
        if (CollUtil.isEmpty(requestedIds)) {
            return;
        }
        Map<Long, MesProScheduleOrderDO> requestedMap = Optional.ofNullable(requestedOrders)
                .orElse(Collections.emptyList())
                .stream()
                .filter(order -> order.getId() != null)
                .collect(Collectors.toMap(MesProScheduleOrderDO::getId, order -> order, (left, right) -> left));
        Set<Long> schedulableIds = Optional.ofNullable(schedulableOrders)
                .orElse(Collections.emptyList())
                .stream()
                .filter(order -> !isTerminalScheduleOrder(order))
                .map(MesProScheduleOrderDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<String> blockedOrders = requestedIds.stream()
                .filter(id -> !schedulableIds.contains(id))
                .map(id -> describeBlockedScheduleOrder(id, requestedMap.get(id)))
                .toList();
        if (CollUtil.isNotEmpty(blockedOrders)) {
            throw exception(PRO_AUTO_SCHEDULE_ORDER_NOT_SCHEDULABLE, String.join("；", blockedOrders));
        }
    }

    private String describeBlockedScheduleOrder(Long id, MesProScheduleOrderDO order) {
        if (order == null) {
            return "ID=" + id + "（不存在或已删除）";
        }
        List<String> reasons = new ArrayList<>();
        if (Boolean.TRUE.equals(order.getFrozen())) {
            reasons.add("已冻结");
        }
        if (Boolean.FALSE.equals(order.getAutoSchedulable())) {
            reasons.add("不可自动排产");
        }
        String statusName = resolveScheduleOrderStatusName(order.getStatus());
        if (StrUtil.isNotBlank(statusName)) {
            reasons.add(statusName);
        }
        if (CollUtil.isEmpty(reasons)) {
            reasons.add("不满足自动排产筛选条件");
        }
        String code = StrUtil.blankToDefault(order.getCode(), "ID=" + id);
        return code + "（" + String.join("、", reasons) + "）";
    }

    private boolean isTerminalScheduleOrder(MesProScheduleOrderDO order) {
        if (order == null) {
            return false;
        }
        return ObjUtil.equal(order.getStatus(), MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                || ObjUtil.equal(order.getStatus(), MesProScheduleOrderStatusEnum.CANCELED.getStatus());
    }

    private String resolveScheduleOrderStatusName(Integer status) {
        if (ObjUtil.equal(status, MesProScheduleOrderStatusEnum.FINISHED.getStatus())) {
            return "已完成";
        }
        if (ObjUtil.equal(status, MesProScheduleOrderStatusEnum.CANCELED.getStatus())) {
            return "已取消";
        }
        return null;
    }

    private ScheduleComputation computeSchedule(MesProAutoSchedulePreviewReqVO reqVO,
                                                boolean persistRouteConfigSnapshot) {
        ScheduleInputAssembler.ScheduleInputContext inputContext = scheduleInputAssembler.assemble(reqVO);
        ScheduleComputation computation = new ScheduleComputation();
        computation.replanMode = inputContext.replanMode();
        computation.capacityMode = inputContext.capacityMode();
        computation.requestStartTime = inputContext.requestStartTime();
        computation.preserveManualLockedTasks = inputContext.preserveManualLockedTasks();
        resolveScheduleOrderScope(computation, reqVO, persistRouteConfigSnapshot);
        if (CollUtil.isEmpty(computation.scheduleOrders)) {
            throw exception(PRO_AUTO_SCHEDULE_SCOPE_EMPTY);
        }
        computation.workOrders = schedulePlanner.orderWorkOrdersByScheduleOrder(
                workOrderService.getWorkOrderList(reqVO.getWorkOrderIds()), computation.scheduleOrders);
        if (CollUtil.isEmpty(computation.workOrders)) {
            throw exception(PRO_AUTO_SCHEDULE_SCOPE_EMPTY);
        }
        if (computation.workOrders.stream().anyMatch(workOrder -> Boolean.TRUE.equals(workOrder.getTemporaryFrozen()))) {
            throw exception(PRO_AUTO_SCHEDULE_FROZEN_WORK_ORDER);
        }
        computation.workOrderMap = computation.workOrders.stream()
                .collect(Collectors.toMap(MesProWorkOrderDO::getId, workOrder -> workOrder));

        collectRouteContexts(computation);
        collectMasterData(computation);
        collectExistingTasks(computation);
        initializeProcessCapacityLedger(computation);
        validateMaterialAvailability(computation);

        if (!hasGlobalBlockingIssues(computation.issues)) {
            scheduleTasks(computation);
        }
        validateAttributableProcessActiveTaskCoverage(computation);
        validateGeneratedProcessQuantityTieOut(computation);

        computation.previewTasks = buildPreviewTasks(computation);
        computation.previewLinks = buildPreviewLinks(computation);
        return computation;
    }

    private void collectRouteContexts(ScheduleComputation computation) {
        Set<Long> routeIds = new LinkedHashSet<>();
        for (MesProWorkOrderDO workOrder : computation.workOrders) {
            MesProRouteProductDO routeProduct = routeProductService.getRouteProductByItemId(workOrder.getProductId());
            if (routeProduct == null) {
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_ROUTE, workOrder.getId(), null, null, null,
                        "工单未配置工艺路线"));
                continue;
            }
            MesProScheduleOrderDO scheduleOrder = findScheduleOrderByWorkOrderId(computation, workOrder.getId());
            List<MesProScheduleOrderProcessDO> scheduleOrderProcesses = scheduleOrder == null
                    ? null
                    : computation.scheduleOrderProcessesByOrderId.get(scheduleOrder.getId());
            if (!isScheduleRouteFlowConfigEnabled(routeProduct.getRouteId())
                    && !hasEnabledRemainingScheduleOrderProcesses(scheduleOrderProcesses)) {
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_ROUTE, workOrder.getId(), null, null, null,
                        "工艺路线已被禁用"));
                continue;
            }
            List<MesProRouteProcessDO> routeProcesses = routeProcessService.getRouteProcessListByRouteId(routeProduct.getRouteId())
                    .stream()
                    .sorted(Comparator.comparing(MesProRouteProcessDO::getSort))
                    .toList();
            if (CollUtil.isEmpty(routeProcesses)) {
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_ROUTE_PROCESS, workOrder.getId(), null, null, null,
                        "工艺路线未配置工序"));
                continue;
            }
            RouteSnapshotResolver.ResolvedRoutePlan resolvedRoutePlan =
                    routeSnapshotResolver.resolve(routeProduct.getRouteId(), routeProcesses, scheduleOrderProcesses);
            if (resolvedRoutePlan.topologyValidationError() != null) {
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_ROUTE, workOrder.getId(), null, null, null,
                        resolvedRoutePlan.topologyValidationError()));
                continue;
            }
            if (scheduleOrder != null && CollUtil.isNotEmpty(resolvedRoutePlan.scheduleOrderProcesses())) {
                scheduleOrderProcesses = resolvedRoutePlan.scheduleOrderProcesses();
                computation.scheduleOrderProcessesByOrderId.put(scheduleOrder.getId(), scheduleOrderProcesses);
            }
            routeProcesses = resolvedRoutePlan.routeProcesses();
            mergeWorkstationProcessAliases(computation, resolvedRoutePlan);
            routeProcesses = restrictToActiveSnapshotRouteProcesses(routeProcesses, scheduleOrderProcesses);
            String topologyValidationError =
                    validateRouteProcessTopologySnapshot(computation, workOrder.getId(), routeProcesses);
            if (topologyValidationError != null) {
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_ROUTE, workOrder.getId(), null, null, null,
                        topologyValidationError));
                continue;
            }
            computation.routeProductByWorkOrderId.put(workOrder.getId(), routeProduct);
            computation.routeProcessesByWorkOrderId.put(workOrder.getId(), routeProcesses);
            routeIds.add(routeProduct.getRouteId());
        }
        if (CollUtil.isNotEmpty(routeIds)) {
            computation.routeMap = Objects.requireNonNull(
                    routeService.getRouteMapIgnoreDeleted(routeIds), "route map must not be null");
        }
    }

    private boolean isScheduleRouteFlowConfigEnabled(Long routeId) {
        if (routeId == null) {
            return false;
        }
        MesProRouteFlowConfigDO flowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
        return MesProRouteFlowContextMatcher.isEnabledFlowContext(
                flowConfig, routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
    }

    private boolean hasEnabledRemainingScheduleOrderProcesses(List<MesProScheduleOrderProcessDO> processes) {
        return Optional.ofNullable(processes).orElse(Collections.emptyList()).stream()
                .anyMatch(process -> Boolean.TRUE.equals(process.getEnabled()) && hasRemainingQuantity(process));
    }

    private void mergeWorkstationProcessAliases(ScheduleComputation computation,
                                                RouteSnapshotResolver.ResolvedRoutePlan resolvedRoutePlan) {
        if (computation == null || resolvedRoutePlan == null
                || CollUtil.isEmpty(resolvedRoutePlan.workstationProcessAliasesByRouteProcessId())) {
            return;
        }
        resolvedRoutePlan.workstationProcessAliasesByRouteProcessId().forEach((routeProcessId, aliases) ->
                computation.workstationProcessAliasesByRouteProcessId
                        .computeIfAbsent(routeProcessId, key -> new LinkedHashSet<>())
                        .addAll(aliases));
    }

    private void collectMasterData(ScheduleComputation computation) {
        Set<Long> processIds = computation.routeProcessesByWorkOrderId.values().stream()
                .flatMap(Collection::stream)
                .map(MesProRouteProcessDO::getProcessId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> boundWorkstationIds = computation.routeProcessesByWorkOrderId.values().stream()
                .flatMap(Collection::stream)
                .map(MesProRouteProcessDO::getWorkstationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Long> processIdentityMap = routeProcessService.getProcessIdentityMap(processIds);
        List<MesMdWorkstationDO> processWorkstations = processIdentityMap.isEmpty()
                ? Collections.emptyList()
                : workstationMapper.selectListByProcessIds(new ArrayList<>(processIdentityMap.keySet()),
                        CommonStatusEnum.ENABLE.getStatus());
        normalizeWorkstationProcessIds(processIdentityMap, processWorkstations);
        List<MesMdWorkstationDO> boundWorkstations = boundWorkstationIds.isEmpty()
                ? Collections.emptyList()
                : workstationMapper.selectByIds(boundWorkstationIds).stream()
                .filter(workstation -> ObjUtil.equal(workstation.getStatus(), CommonStatusEnum.ENABLE.getStatus()))
                .toList();
        normalizeWorkstationProcessIds(computation, boundWorkstations);
        computation.workstationMap = new LinkedHashMap<>();
        processWorkstations.forEach(workstation -> computation.workstationMap.put(workstation.getId(), workstation));
        boundWorkstations.forEach(workstation -> computation.workstationMap.put(workstation.getId(), workstation));
        rebuildWorkstationsByRouteProcess(computation);
        computation.itemMap = itemService.getItemMap(computation.workOrders.stream()
                .map(MesProWorkOrderDO::getProductId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        Set<Long> itemIds = new LinkedHashSet<>();
        computation.materialDemandByWorkOrderId = buildProductionMaterialDemandMap(computation, false);
        computation.materialDemandByWorkOrderId.values().forEach(demandByItemId -> itemIds.addAll(demandByItemId.keySet()));
        if (!itemIds.isEmpty()) {
            computation.itemMap.putAll(itemService.getItemMap(itemIds));
        }
        computation.processMap = processService.getProcessMap(processIds);

        List<MesWmMaterialStockDO> stockList = materialStockMapper.selectListByItemIds(itemIds);
        computation.availableStockByItemId = stockList.stream()
                .collect(Collectors.groupingBy(MesWmMaterialStockDO::getItemId,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, MesWmMaterialStockDO::getQuantity, BigDecimal::add)));

        refreshLineResourceData(computation);
    }

    private void normalizeWorkstationProcessIds(ScheduleComputation computation,
                                                Collection<MesMdWorkstationDO> workstations) {
        if (CollUtil.isEmpty(workstations)) {
            return;
        }
        Set<Long> processIds = computation.routeProcessesByWorkOrderId.values().stream()
                .flatMap(Collection::stream)
                .map(MesProRouteProcessDO::getProcessId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Long> processIdentityMap = routeProcessService.getProcessIdentityMap(processIds);
        normalizeWorkstationProcessIds(processIdentityMap, workstations);
    }

    private void normalizeWorkstationProcessIds(Map<Long, Long> processIdentityMap,
                                                Collection<MesMdWorkstationDO> workstations) {
        if (CollUtil.isEmpty(processIdentityMap) || CollUtil.isEmpty(workstations)) {
            return;
        }
        workstations.forEach(workstation -> {
            Long currentProcessId = processIdentityMap.get(workstation.getProcessId());
            if (currentProcessId != null) {
                workstation.setProcessId(currentProcessId);
            }
        });
    }

    private void rebuildWorkstationsByRouteProcess(ScheduleComputation computation) {
        computation.workstationsByRouteProcessId = new LinkedHashMap<>();
        List<MesProRouteProcessDO> allRouteProcesses = computation.routeProcessesByWorkOrderId.values().stream()
                .flatMap(Collection::stream)
                .toList();
        Map<Long, List<MesMdWorkstationDO>> workstationsByProcessId = computation.workstationMap.values().stream()
                .collect(Collectors.groupingBy(MesMdWorkstationDO::getProcessId, LinkedHashMap::new, Collectors.toList()));
        for (MesProRouteProcessDO routeProcess : allRouteProcesses) {
            if (routeProcess.getWorkstationId() == null) {
                computation.workstationsByRouteProcessId.put(routeProcess.getId(),
                        workstationsByProcessId.getOrDefault(routeProcess.getProcessId(), Collections.emptyList()));
                continue;
            }
            MesMdWorkstationDO workstation = computation.workstationMap.get(routeProcess.getWorkstationId());
            computation.workstationsByRouteProcessId.put(routeProcess.getId(),
                    workstation == null ? Collections.emptyList()
                            : List.of(copyWorkstationForRouteProcess(workstation, routeProcess.getProcessId())));
        }
    }

    private MesMdWorkstationDO copyWorkstationForRouteProcess(MesMdWorkstationDO workstation, Long processId) {
        MesMdWorkstationDO copy = BeanUtils.toBean(workstation, MesMdWorkstationDO.class);
        copy.setProcessId(processId);
        return copy;
    }

    private boolean isBoundWorkstationProcessMatched(ScheduleComputation computation,
                                                     MesProRouteProcessDO routeProcess,
                                                     MesMdWorkstationDO workstation) {
        if (routeProcess == null || workstation == null) {
            return false;
        }
        Long routeProcessId = routeProcess.getId();
        Long routeProcessProcessId = routeProcess.getProcessId();
        Long workstationProcessId = workstation.getProcessId();
        if (ObjUtil.equal(workstationProcessId, routeProcessProcessId)) {
            return true;
        }
        if (computation == null || routeProcessId == null) {
            return false;
        }
        Set<Long> aliases = computation.workstationProcessAliasesByRouteProcessId.get(routeProcessId);
        return aliases != null
                && aliases.contains(routeProcessProcessId)
                && aliases.contains(workstationProcessId);
    }

    private void refreshLineResourceData(ScheduleComputation computation) {
        List<MesMdWorkstationDO> workstations = new ArrayList<>(computation.workstationMap.values());
        List<MesMdWorkstationDO> resourcePoolWorkstations = buildResourcePoolWorkstations(computation, workstations);
        Set<Long> lineIds = resourcePoolWorkstations.stream()
                .map(MesMdWorkstationDO::getProductionLineId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        computation.lineMap = productionLineService.getProductionLineMap(lineIds);

        Set<Long> planIds = computation.lineMap.values().stream()
                .map(MesMdProductionLineDO::getCalendarPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        computation.planMap = planService.getPlanMap(planIds);
        computation.shiftListByPlanId = new HashMap<>();
        for (Long planId : planIds) {
            computation.shiftListByPlanId.put(planId, planShiftService.getPlanShiftListByPlanId(planId));
        }

        computation.calendarSummary = Objects.requireNonNull(
                scheduleCalendarService.getRules(),
                "Schedule calendar rules are required for auto schedule");
        computation.processCalendarSummariesByRuleId = loadProcessCalendarSummaries(computation);
        computation.requestStartTime = resolveEffectiveRequestStartTime(computation.requestStartTime,
                computation.calendarSummary, computation.replanMode);
        LocalDateTime capacityStart = reqDateStart(computation.requestStartTime);
        if (CAPACITY_MODE_PLANNED.equals(computation.capacityMode)) {
            ensurePlannedCapacityCoverage(computation, lineIds, capacityStart.toLocalDate());
        }
        List<MesProCapacityPlanDO> capacityPlanList = capacityPlanMapper.selectListByLineIdsAndDate(lineIds, capacityStart);
        List<MesProCapacityActualDO> capacityActualList = capacityActualMapper.selectListByLineIdsAndDate(lineIds, capacityStart);
        computation.shiftMap = new HashMap<>();
        for (List<MesCalPlanShiftDO> shiftList : computation.shiftListByPlanId.values()) {
            for (MesCalPlanShiftDO shift : shiftList) {
                computation.shiftMap.put(shift.getId(), shift);
            }
        }
        computation.processResourcePoolByLineProcessKey = buildProcessResourcePools(computation, resourcePoolWorkstations);
        computation.calendarContext = buildCalendarContext(computation, capacityPlanList, capacityActualList,
                computation.calendarSummary, true);
        computation.processCalendarContextByRuleId = buildProcessCalendarContexts(computation, capacityPlanList,
                capacityActualList);
        computation.shiftWindowsByLineId = capacityWindowAllocator.buildShiftWindows(computation.capacityMode,
                computation.lineMap, computation.shiftMap, computation.planMap, capacityPlanList, capacityActualList);
    }

    private List<MesMdWorkstationDO> buildResourcePoolWorkstations(ScheduleComputation computation,
                                                                   Collection<MesMdWorkstationDO> workstations) {
        Map<String, MesMdWorkstationDO> result = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(workstations)) {
            workstations.forEach(workstation -> result.put(workstationContextKey(workstation), workstation));
        }
        computation.workstationsByRouteProcessId.values().stream()
                .flatMap(Collection::stream)
                .forEach(workstation -> result.put(workstationContextKey(workstation), workstation));
        return new ArrayList<>(result.values());
    }

    private String workstationContextKey(MesMdWorkstationDO workstation) {
        return workstation.getId() + ":" + workstation.getProcessId();
    }

    private void ensurePlannedCapacityCoverage(ScheduleComputation computation,
                                               Set<Long> lineIds,
                                               LocalDate capacityStartDate) {
        if (CollUtil.isEmpty(lineIds) || capacityStartDate == null) {
            return;
        }
        LocalDate requiredEndDate = resolvePlannedCapacityCoverageEndDate(computation, capacityStartDate);
        scheduleCalendarService.ensureCapacityPlanCoverage(lineIds, capacityStartDate, requiredEndDate);
    }

    private LocalDate resolvePlannedCapacityCoverageEndDate(ScheduleComputation computation, LocalDate defaultDate) {
        if (CollUtil.isEmpty(computation.lineMap)) {
            return defaultDate;
        }
        LocalDate requiredEndDate = computation.lineMap.values().stream()
                .map(MesMdProductionLineDO::getCalendarPlanId)
                .filter(Objects::nonNull)
                .map(computation.planMap::get)
                .filter(Objects::nonNull)
                .map(MesCalPlanDO::getEndDate)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .max(LocalDate::compareTo)
                .orElse(defaultDate);
        return requiredEndDate.isBefore(defaultDate) ? defaultDate : requiredEndDate;
    }

    private Map<Long, MesProScheduleCalendarRulesRespVO> loadProcessCalendarSummaries(ScheduleComputation computation) {
        Set<Long> calendarRuleIds = computation.scheduleOrderProcessesByOrderId.values().stream()
                .flatMap(Collection::stream)
                .map(MesProScheduleOrderProcessDO::getCalendarRuleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(calendarRuleIds)) {
            return Collections.emptyMap();
        }
        Map<Long, MesProScheduleCalendarRulesRespVO> result = new LinkedHashMap<>();
        for (Long calendarRuleId : calendarRuleIds) {
            MesProScheduleCalendarRuleDO rule = scheduleCalendarRuleMapper.selectById(calendarRuleId);
            if (rule == null) {
                throw new IllegalStateException("工序绑定的日历规则不存在，calendarRuleId=" + calendarRuleId);
            }
            result.put(calendarRuleId, toCalendarRulesResp(rule, computation.calendarSummary.getSimulationCurrentDate()));
        }
        return result;
    }

    private void collectExistingTasks(ScheduleComputation computation) {
        computation.scopeTasks = taskMapper.selectListByWorkOrderIds(computation.workOrderMap.keySet());
        Set<Long> scopeTaskIds = computation.scopeTasks.stream()
                .map(MesProTaskDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProTaskScheduleExtDO> extMap = taskScheduleExtMapper.selectListByTaskIds(
                new ArrayList<>(scopeTaskIds)).stream()
                .collect(Collectors.toMap(MesProTaskScheduleExtDO::getTaskId, ext -> ext));
        computation.taskExtMap = extMap;
        computation.feedbackByTaskId = feedbackMapper.selectListByTaskIds(scopeTaskIds).stream()
                .filter(feedback -> feedback.getTaskId() != null)
                .filter(feedback -> ObjUtil.notEqual(feedback.getStatus(), MesProFeedbackStatusEnum.PREPARE.getStatus()))
                .collect(Collectors.groupingBy(MesProFeedbackDO::getTaskId, LinkedHashMap::new, Collectors.toList()));
        Map<String, List<MesProTaskDO>> tasksByWorkOrderProcess = computation.scopeTasks.stream()
                .collect(Collectors.groupingBy(task -> taskKey(task.getWorkOrderId(), task.getProcessId()),
                        LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<MesProTaskDO>> entry : tasksByWorkOrderProcess.entrySet()) {
            List<MesProTaskDO> tasks = entry.getValue().stream()
                    .sorted(Comparator.comparing(MesProTaskDO::getUpdateTime, Comparator.nullsLast(LocalDateTime::compareTo))
                            .thenComparing(MesProTaskDO::getId, Comparator.nullsLast(Long::compareTo))
                            .reversed())
                    .toList();
            List<MesProTaskDO> protectedTasks = new ArrayList<>();
            for (MesProTaskDO task : tasks) {
                MesProTaskScheduleExtDO ext = extMap.get(task.getId());
                String protectionReason = resolveProtectionReason(computation, task, ext);
                if (protectionReason != null) {
                    protectedTasks.add(task);
                    computation.protectionReasonByTaskId.put(task.getId(), protectionReason);
                } else {
                    computation.replaceableScopeTasks.add(task);
                }
            }

            if (protectedTasks.size() <= 1) {
                for (MesProTaskDO protectedTask : protectedTasks) {
                    preserveTask(computation, entry.getKey(), protectedTask);
                }
                continue;
            }

            List<MesProTaskDO> stronglyProtectedTasks = protectedTasks.stream()
                    .filter(task -> {
                        MesProTaskScheduleExtDO ext = extMap.get(task.getId());
                        return MesProTaskStatusEnum.isEndStatus(task.getStatus())
                                || (ext != null && Boolean.TRUE.equals(ext.getLocked()));
                    })
                    .toList();
            if (stronglyProtectedTasks.size() > 1) {
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_PROTECTED,
                        stronglyProtectedTasks.get(0).getWorkOrderId(),
                        stronglyProtectedTasks.get(0).getProcessId(),
                        null, null, "同一工单工序存在多个受保护任务"));
                stronglyProtectedTasks.forEach(task -> preserveTask(computation, entry.getKey(), task));
                continue;
            }

            MesProTaskDO winner = protectedTasks.get(0);
            preserveTask(computation, entry.getKey(), winner);
            protectedTasks.stream()
                    .skip(1)
                    .filter(task -> !MesProTaskStatusEnum.isEndStatus(task.getStatus()))
                    .forEach(computation.replaceableScopeTasks::add);
        }

        hydrateProtectedTaskWorkstations(computation);
        collectLineAvailabilityFromExistingTasks(computation);
    }

    private void hydrateProtectedTaskWorkstations(ScheduleComputation computation) {
        if (CollUtil.isEmpty(computation.preservedTasks)) {
            return;
        }
        Set<Long> missingWorkstationIds = computation.preservedTasks.stream()
                .map(MesProTaskDO::getWorkstationId)
                .filter(Objects::nonNull)
                .filter(workstationId -> !computation.workstationMap.containsKey(workstationId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesMdWorkstationDO> loadedWorkstationMap = missingWorkstationIds.isEmpty()
                ? Collections.emptyMap()
                : ObjUtil.defaultIfNull(workstationMapper.selectByIds(missingWorkstationIds),
                        Collections.<MesMdWorkstationDO>emptyList()).stream()
                .collect(Collectors.toMap(MesMdWorkstationDO::getId, workstation -> workstation,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, MesMdWorkstationDO> workstationsToAdd = new LinkedHashMap<>();
        for (MesProTaskDO protectedTask : computation.preservedTasks) {
            Long workstationId = protectedTask.getWorkstationId();
            if (isProgressOnlyProtectedTask(computation, protectedTask)) {
                continue;
            }
            if (workstationId == null) {
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_WORKSTATION,
                        protectedTask.getWorkOrderId(), protectedTask.getProcessId(), null, null,
                        "受保护任务未绑定工作站"));
                continue;
            }
            if (computation.workstationMap.containsKey(workstationId)) {
                continue;
            }
            MesMdWorkstationDO workstation = loadedWorkstationMap.get(workstationId);
            if (workstation == null) {
                MesMdWorkstationDO replacementWorkstation = resolveProtectedTaskReplacementWorkstation(computation, protectedTask);
                if (replacementWorkstation != null) {
                    protectedTask.setWorkstationId(replacementWorkstation.getId());
                    workstationsToAdd.putIfAbsent(replacementWorkstation.getId(), replacementWorkstation);
                    continue;
                }
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_WORKSTATION,
                        protectedTask.getWorkOrderId(), protectedTask.getProcessId(), workstationId, null,
                        "受保护任务工作站不存在或已删除"));
                continue;
            }
            if (!ObjUtil.equal(workstation.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
                MesMdWorkstationDO replacementWorkstation = resolveProtectedTaskReplacementWorkstation(computation, protectedTask);
                if (replacementWorkstation != null && !ObjUtil.equal(replacementWorkstation.getId(), workstation.getId())) {
                    protectedTask.setWorkstationId(replacementWorkstation.getId());
                    workstationsToAdd.putIfAbsent(replacementWorkstation.getId(), replacementWorkstation);
                    continue;
                }
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_WORKSTATION,
                        protectedTask.getWorkOrderId(), protectedTask.getProcessId(), workstationId, null,
                        "受保护任务工作站已禁用"));
                continue;
            }
            workstationsToAdd.putIfAbsent(workstation.getId(), workstation);
        }
        if (workstationsToAdd.isEmpty()) {
            return;
        }
        normalizeWorkstationProcessIds(computation, workstationsToAdd.values());
        workstationsToAdd.values().forEach(workstation -> computation.workstationMap.put(workstation.getId(), workstation));
        rebuildWorkstationsByRouteProcess(computation);
        refreshLineResourceData(computation);
    }

    private MesMdWorkstationDO resolveProtectedTaskReplacementWorkstation(ScheduleComputation computation,
                                                                          MesProTaskDO protectedTask) {
        MesProRouteProcessDO routeProcess = resolveProtectedTaskRouteProcess(computation, protectedTask);
        if (routeProcess == null) {
            return null;
        }
        if (routeProcess.getWorkstationId() != null) {
            MesMdWorkstationDO boundWorkstation = computation.workstationMap.get(routeProcess.getWorkstationId());
            if (isUsableProtectedTaskReplacementWorkstation(computation, routeProcess, boundWorkstation)) {
                return boundWorkstation;
            }
        }
        List<MesMdWorkstationDO> candidates = computation.workstationsByRouteProcessId
                .getOrDefault(routeProcess.getId(), Collections.emptyList())
                .stream()
                .filter(workstation -> isUsableProtectedTaskReplacementWorkstation(computation, routeProcess, workstation))
                .collect(Collectors.toMap(MesMdWorkstationDO::getId, workstation -> workstation,
                        (left, right) -> left, LinkedHashMap::new))
                .values()
                .stream()
                .toList();
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    private MesProRouteProcessDO resolveProtectedTaskRouteProcess(ScheduleComputation computation,
                                                                  MesProTaskDO protectedTask) {
        if (computation == null || protectedTask == null) {
            return null;
        }
        List<MesProRouteProcessDO> routeProcesses = computation.routeProcessesByWorkOrderId
                .getOrDefault(protectedTask.getWorkOrderId(), Collections.emptyList());
        if (CollUtil.isEmpty(routeProcesses)) {
            return null;
        }
        Long processId = protectedTask.getProcessId();
        if (processId != null) {
            Optional<MesProRouteProcessDO> sameProcessRouteProcess = routeProcesses.stream()
                    .filter(routeProcess -> ObjUtil.equal(routeProcess.getProcessId(), processId))
                    .findFirst();
            if (sameProcessRouteProcess.isPresent()) {
                return sameProcessRouteProcess.get();
            }
            Optional<MesProRouteProcessDO> aliasedRouteProcess = routeProcesses.stream()
                    .filter(routeProcess -> {
                        Set<Long> aliases = computation.workstationProcessAliasesByRouteProcessId.get(routeProcess.getId());
                        return aliases != null && aliases.contains(processId);
                    })
                    .findFirst();
            if (aliasedRouteProcess.isPresent()) {
                return aliasedRouteProcess.get();
            }
        }
        MesProScheduleOrderDO scheduleOrder = computation.scheduleOrders.stream()
                .filter(order -> ObjUtil.equal(order.getWorkOrderId(), protectedTask.getWorkOrderId()))
                .findFirst()
                .orElse(null);
        if (scheduleOrder == null) {
            return null;
        }
        List<MesProScheduleOrderProcessDO> scheduleOrderProcesses = computation.scheduleOrderProcessesByOrderId
                .getOrDefault(scheduleOrder.getId(), Collections.emptyList());
        Optional<Long> routeProcessId = scheduleOrderProcesses.stream()
                .filter(process -> ObjUtil.equal(process.getProcessId(), processId))
                .map(MesProScheduleOrderProcessDO::getRouteProcessId)
                .filter(Objects::nonNull)
                .findFirst();
        return routeProcessId
                .flatMap(id -> routeProcesses.stream()
                        .filter(routeProcess -> ObjUtil.equal(routeProcess.getId(), id))
                        .findFirst())
                .orElse(null);
    }

    private boolean isUsableProtectedTaskReplacementWorkstation(ScheduleComputation computation,
                                                                MesProRouteProcessDO routeProcess,
                                                                MesMdWorkstationDO workstation) {
        if (workstation == null || !ObjUtil.equal(workstation.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            return false;
        }
        if (routeProcess != null
                && routeProcess.getWorkstationId() != null
                && ObjUtil.equal(routeProcess.getWorkstationId(), workstation.getId())) {
            return true;
        }
        return isBoundWorkstationProcessMatched(computation, routeProcess, workstation);
    }

    private void collectLineAvailabilityFromExistingTasks(ScheduleComputation computation) {
        Set<Long> processWorkstationIds = computation.workstationMap.keySet();
        if (CollUtil.isEmpty(processWorkstationIds)) {
            return;
        }
        List<MesProTaskDO> candidateLineTasks = taskMapper.selectListByWorkstationIds(processWorkstationIds);
        Set<Long> activeLineWorkOrderIds = resolveActiveLineWorkOrderIds(candidateLineTasks);
        for (MesProTaskDO task : candidateLineTasks) {
            if (!activeLineWorkOrderIds.contains(task.getWorkOrderId())) {
                continue;
            }
            if (task.getEndTime() == null || !task.getEndTime().isAfter(computation.requestStartTime)) {
                continue;
            }
            if (computation.replaceableScopeTasks.stream().anyMatch(replaceable -> ObjUtil.equal(replaceable.getId(), task.getId()))) {
                continue;
            }
            if (isProgressOnlyProtectedTask(computation, task)) {
                continue;
            }
            MesMdWorkstationDO workstation = computation.workstationMap.get(task.getWorkstationId());
            if (workstation == null || workstation.getProductionLineId() == null) {
                continue;
            }
            computation.lineProcessAvailableFrom.merge(lineProcessKey(workstation.getProductionLineId(), task.getProcessId()), task.getEndTime(),
                    (oldVal, newVal) -> oldVal.isAfter(newVal) ? oldVal : newVal);
        }
    }

    private void initializeProcessCapacityLedger(ScheduleComputation computation) {
        DailyProcessCapacityLedger ledger = new DailyProcessCapacityLedger();
        Set<Long> replaceableTaskIds = computation.replaceableScopeTasks.stream()
                .map(MesProTaskDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProTaskDO> lineResourceTasks = CollUtil.isEmpty(computation.workstationMap)
                ? Collections.emptyList()
                : taskMapper.selectListByWorkstationIds(computation.workstationMap.keySet());
        List<MesProTaskDO> contextTasks = new ArrayList<>();
        contextTasks.addAll(computation.preservedTasks);
        contextTasks.addAll(lineResourceTasks);
        Map<Long, MesProScheduleOrderDO> effectiveScheduleOrderByWorkOrderId =
                loadEffectiveScheduleOrderByWorkOrderId(contextTasks);
        Map<Long, List<MesProScheduleOrderProcessDO>> processByScheduleOrderId =
                loadScheduleOrderProcessesByOrderId(effectiveScheduleOrderByWorkOrderId.values());

        Set<Long> seededTaskIds = new LinkedHashSet<>();
        for (MesProTaskDO preservedTask : computation.preservedTasks) {
            if (isProgressOnlyProtectedTask(computation, preservedTask)) {
                continue;
            }
            seedTaskDailyProcessCapacity(computation, ledger, preservedTask,
                    effectiveScheduleOrderByWorkOrderId, processByScheduleOrderId, seededTaskIds);
        }
        for (MesProTaskDO lineResourceTask : lineResourceTasks) {
            if (lineResourceTask.getId() != null && replaceableTaskIds.contains(lineResourceTask.getId())) {
                continue;
            }
            if (isProgressOnlyProtectedTask(computation, lineResourceTask)) {
                continue;
            }
            seedTaskDailyProcessCapacity(computation, ledger, lineResourceTask,
                    effectiveScheduleOrderByWorkOrderId, processByScheduleOrderId, seededTaskIds);
        }
        computation.processCapacityLedger = ledger;
    }

    private Map<Long, MesProScheduleOrderDO> loadEffectiveScheduleOrderByWorkOrderId(List<MesProTaskDO> tasks) {
        Set<Long> workOrderIds = Optional.ofNullable(tasks).orElse(Collections.emptyList()).stream()
                .map(MesProTaskDO::getWorkOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(workOrderIds)) {
            return Collections.emptyMap();
        }
        return scheduleOrderMapper.selectEffectiveListByWorkOrderIds(workOrderIds).stream()
                .collect(Collectors.toMap(MesProScheduleOrderDO::getWorkOrderId, order -> order,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, List<MesProScheduleOrderProcessDO>> loadScheduleOrderProcessesByOrderId(
            Collection<MesProScheduleOrderDO> scheduleOrders) {
        Set<Long> scheduleOrderIds = Optional.ofNullable(scheduleOrders).orElse(Collections.emptyList()).stream()
                .map(MesProScheduleOrderDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(scheduleOrderIds)) {
            return Collections.emptyMap();
        }
        return scheduleOrderProcessMapper.selectListByScheduleOrderIds(scheduleOrderIds).stream()
                .collect(Collectors.groupingBy(MesProScheduleOrderProcessDO::getScheduleOrderId,
                        LinkedHashMap::new, Collectors.toList()));
    }

    private void seedTaskDailyProcessCapacity(ScheduleComputation computation,
                                              DailyProcessCapacityLedger ledger,
                                              MesProTaskDO task,
                                              Map<Long, MesProScheduleOrderDO> effectiveScheduleOrderByWorkOrderId,
                                              Map<Long, List<MesProScheduleOrderProcessDO>> processByScheduleOrderId,
                                              Set<Long> seededTaskIds) {
        if (task == null || task.getId() == null || !seededTaskIds.add(task.getId())) {
            return;
        }
        if (ObjUtil.equal(task.getStatus(), MesProTaskStatusEnum.CANCELED.getStatus())) {
            return;
        }
        MesProScheduleOrderDO scheduleOrder = resolveSeedScheduleOrder(computation, task,
                effectiveScheduleOrderByWorkOrderId);
        if (scheduleOrder == null || !isAttributable(scheduleOrder)) {
            return;
        }
        if (task.getQuantity() == null || task.getStartTime() == null) {
            computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_CAPACITY, task.getWorkOrderId(),
                    task.getProcessId(), task.getWorkstationId(), null,
                    "已有有效任务缺少开始时间或数量，无法计算已占日产能"));
            return;
        }
        MesProScheduleOrderProcessDO scheduleOrderProcess = resolveSeedScheduleOrderProcess(computation, task,
                scheduleOrder, processByScheduleOrderId);
        Long processId = scheduleOrderProcess != null && scheduleOrderProcess.getProcessId() != null
                ? scheduleOrderProcess.getProcessId()
                : task.getProcessId();
        if (processId == null) {
            computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_CAPACITY, task.getWorkOrderId(),
                    null, task.getWorkstationId(), null,
                    "已有有效任务缺少工序，无法计算已占日产能"));
            return;
        }
        boolean routeProcessDailyCapacity = isRouteProcessDailyCapacity(scheduleOrderProcess);
        Long lineId = resolveTaskCapacityLedgerLineId(computation, task, scheduleOrderProcess);
        if (!routeProcessDailyCapacity && task.getWorkstationId() != null && lineId == null) {
            computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_CAPACITY, task.getWorkOrderId(),
                    processId, task.getWorkstationId(), null,
                    "已有有效任务工作站缺少产线，无法计算已占日产能"));
            return;
        }
        LocalDate taskEndDate = resolveTaskEndDate(task);
        LocalDate capacityStartDate = computation.requestStartTime == null
                ? task.getStartTime().toLocalDate()
                : computation.requestStartTime.toLocalDate();
        if (taskEndDate.isBefore(capacityStartDate)) {
            return;
        }
        LocalDate cursor = task.getStartTime().toLocalDate().isBefore(capacityStartDate)
                ? capacityStartDate
                : task.getStartTime().toLocalDate();
        for (; !cursor.isAfter(taskEndDate); cursor = cursor.plusDays(1)) {
            BigDecimal dailyQuantity = calculateDailyTaskQuantity(task, cursor);
            ledger.reserve(lineId, processId, cursor, scheduleOrderProcess, dailyQuantity);
        }
    }

    private Long resolveTaskCapacityLedgerLineId(ScheduleComputation computation,
                                                 MesProTaskDO task,
                                                 MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (isRouteProcessDailyCapacity(scheduleOrderProcess)) {
            return null;
        }
        return resolveTaskLineId(computation, task);
    }

    private boolean isRouteProcessDailyCapacity(MesProScheduleOrderProcessDO scheduleOrderProcess) {
        return scheduleOrderProcess != null
                && CAPACITY_SOURCE_ROUTE_PROCESS.equals(scheduleOrderProcess.getCapacitySource())
                && hasDailyProcessCapacityLimit(scheduleOrderProcess);
    }

    private MesProScheduleOrderDO resolveSeedScheduleOrder(ScheduleComputation computation,
                                                           MesProTaskDO task,
                                                           Map<Long, MesProScheduleOrderDO> effectiveScheduleOrderByWorkOrderId) {
        MesProScheduleOrderDO scopedScheduleOrder = findScheduleOrderByWorkOrderId(computation, task.getWorkOrderId());
        return scopedScheduleOrder != null
                ? scopedScheduleOrder
                : effectiveScheduleOrderByWorkOrderId.get(task.getWorkOrderId());
    }

    private MesProScheduleOrderProcessDO resolveSeedScheduleOrderProcess(
            ScheduleComputation computation,
            MesProTaskDO task,
            MesProScheduleOrderDO scheduleOrder,
            Map<Long, List<MesProScheduleOrderProcessDO>> processByScheduleOrderId) {
        if (scheduleOrder == null || scheduleOrder.getId() == null) {
            return null;
        }
        List<MesProScheduleOrderProcessDO> processes = computation.scheduleOrderProcessesByOrderId
                .getOrDefault(scheduleOrder.getId(), processByScheduleOrderId
                        .getOrDefault(scheduleOrder.getId(), Collections.emptyList()));
        if (CollUtil.isEmpty(processes)) {
            return null;
        }
        MesProTaskScheduleExtDO ext = computation.taskExtMap.get(task.getId());
        if (ext != null && ext.getScheduleOrderProcessId() != null) {
            MesProScheduleOrderProcessDO matchedByExt = processes.stream()
                    .filter(process -> ObjUtil.equal(process.getId(), ext.getScheduleOrderProcessId()))
                    .findFirst()
                    .orElse(null);
            if (matchedByExt != null) {
                return matchedByExt;
            }
        }
        return processes.stream()
                .filter(process -> ObjUtil.equal(process.getProcessId(), task.getProcessId()))
                .findFirst()
                .orElse(null);
    }

    private Long resolveTaskLineId(ScheduleComputation computation, MesProTaskDO task) {
        if (task == null || task.getWorkstationId() == null) {
            return null;
        }
        MesMdWorkstationDO workstation = computation.workstationMap.get(task.getWorkstationId());
        return workstation == null ? null : workstation.getProductionLineId();
    }

    private LocalDate resolveTaskEndDate(MesProTaskDO task) {
        if (task.getEndTime() == null || !task.getEndTime().isAfter(task.getStartTime())) {
            return task.getStartTime().toLocalDate();
        }
        return task.getEndTime().minusNanos(1).toLocalDate();
    }

    private BigDecimal calculateDailyTaskQuantity(MesProTaskDO task, LocalDate selectedDate) {
        if (task.getEndTime() == null || !task.getEndTime().isAfter(task.getStartTime())) {
            return selectedDate.equals(task.getStartTime().toLocalDate()) ? task.getQuantity() : BigDecimal.ZERO;
        }
        long totalMinutes = Duration.between(task.getStartTime(), task.getEndTime()).toMinutes();
        if (totalMinutes <= 0) {
            return task.getQuantity();
        }
        LocalDateTime dayStart = selectedDate.atStartOfDay();
        LocalDateTime dayEnd = selectedDate.plusDays(1).atStartOfDay();
        LocalDateTime overlapStart = task.getStartTime().isAfter(dayStart) ? task.getStartTime() : dayStart;
        LocalDateTime overlapEnd = task.getEndTime().isBefore(dayEnd) ? task.getEndTime() : dayEnd;
        if (!overlapEnd.isAfter(overlapStart)) {
            return BigDecimal.ZERO;
        }
        long overlapMinutes = Duration.between(overlapStart, overlapEnd).toMinutes();
        if (overlapMinutes <= 0) {
            return BigDecimal.ZERO;
        }
        if (overlapMinutes >= totalMinutes) {
            return task.getQuantity();
        }
        return task.getQuantity()
                .multiply(BigDecimal.valueOf(overlapMinutes))
                .divide(BigDecimal.valueOf(totalMinutes), 6, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private Set<Long> resolveActiveLineWorkOrderIds(List<MesProTaskDO> candidateLineTasks) {
        if (CollUtil.isEmpty(candidateLineTasks)) {
            return Collections.emptySet();
        }
        Set<Long> workOrderIds = candidateLineTasks.stream()
                .map(MesProTaskDO::getWorkOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(workOrderIds)) {
            return Collections.emptySet();
        }
        return scheduleOrderMapper.selectEffectiveListByWorkOrderIds(workOrderIds).stream()
                .map(MesProScheduleOrderDO::getWorkOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void preserveTask(ScheduleComputation computation, String workOrderProcessKey, MesProTaskDO task) {
        computation.preservedTasks.add(task);
        computation.preservedTaskIds.add(task.getId());
        computation.preservedTaskByWorkOrderProcess
                .computeIfAbsent(workOrderProcessKey, key -> new ArrayList<>())
                .add(task);
    }

    private String resolveProtectionReason(ScheduleComputation computation,
                                           MesProTaskDO task,
                                           MesProTaskScheduleExtDO ext) {
        if (MesProTaskStatusEnum.isEndStatus(task.getStatus())) {
            return PROTECTION_REASON_FINISHED;
        }
        if (ObjUtil.equal(task.getStatus(), MesProTaskStatusEnum.IN_PROGRESS.getStatus())) {
            return PROTECTION_REASON_IN_PROGRESS;
        }
        if (CollUtil.isNotEmpty(computation.feedbackByTaskId.get(task.getId()))) {
            return PROTECTION_REASON_FEEDBACK;
        }
        if (ext != null && Boolean.TRUE.equals(ext.getLocked())) {
            return PROTECTION_REASON_LOCKED;
        }
        if (Boolean.TRUE.equals(computation.preserveManualLockedTasks)
                && (ext == null || ObjUtil.notEqual(SCHEDULE_SOURCE_AUTO, ext.getScheduleSource()))) {
            return PROTECTION_REASON_MANUAL;
        }
        return null;
    }

    private void validateMaterialAvailability(ScheduleComputation computation) {
        hydrateMissingProductionMaterialLists(computation);
        for (MesProWorkOrderDO workOrder : computation.workOrders) {
            if (!computation.routeProcessesByWorkOrderId.containsKey(workOrder.getId())) {
                continue;
            }
            if (!computation.workOrderIdsWithProductionMaterialList.contains(workOrder.getId())) {
                computation.issues.add(ScheduleIssueDraft.warning(ISSUE_TYPE_MATERIAL_DEMAND, workOrder.getId(), null, null, null,
                        "工单缺少生产用料清单"));
                continue;
            }
            Map<Long, BigDecimal> materialDemandByItemId = computation.materialDemandByWorkOrderId.get(workOrder.getId());
            if (materialDemandByItemId == null || materialDemandByItemId.isEmpty()) {
                continue;
            }
        }

        Map<Long, BigDecimal> requiredByItem = new LinkedHashMap<>();
        computation.materialDemandByWorkOrderId.values().forEach(demandByItemId ->
                demandByItemId.forEach((itemId, quantity) -> requiredByItem.merge(itemId, quantity, BigDecimal::add)));
        for (Map.Entry<Long, BigDecimal> entry : requiredByItem.entrySet()) {
            BigDecimal available = computation.availableStockByItemId.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            if (available.compareTo(entry.getValue()) < 0) {
                BigDecimal shortage = entry.getValue().subtract(available);
                for (MesProWorkOrderDO workOrder : computation.workOrders) {
                    Map<Long, BigDecimal> materialDemandByItemId = computation.materialDemandByWorkOrderId.get(workOrder.getId());
                    if (materialDemandByItemId == null || materialDemandByItemId.isEmpty()) {
                        continue;
                    }
                    boolean currentWorkOrderUsesItem = materialDemandByItemId.containsKey(entry.getKey());
                    if (!currentWorkOrderUsesItem) {
                        continue;
                    }
                    String message = buildMaterialShortageMessage(computation, entry.getKey(), shortage);
                    computation.issues.add(ScheduleIssueDraft.warning(ISSUE_TYPE_MATERIAL, workOrder.getId(), null, null, entry.getKey(),
                            message)
                            .withQty(entry.getValue(), available, shortage));
                }
            }
        }
    }

    private String buildMaterialShortageMessage(ScheduleComputation computation, Long materialId, BigDecimal shortage) {
        MesMdItemDO item = computation.itemMap.get(materialId);
        if (item == null) {
            throw new IllegalStateException("缺料物料主数据不存在，materialId=" + materialId);
        }
        return "物料缺料：" + buildMaterialLabel(item) + "，缺少 " + formatQuantity(shortage);
    }

    private String buildMaterialLabel(MesMdItemDO item) {
        boolean hasCode = StrUtil.isNotBlank(item.getCode());
        boolean hasName = StrUtil.isNotBlank(item.getName());
        if (!hasCode && !hasName) {
            throw new IllegalStateException("缺料物料缺少编码和名称，materialId=" + item.getId());
        }
        if (hasCode && hasName) {
            return item.getName() + "（" + item.getCode() + "）";
        }
        return hasName ? item.getName() : item.getCode();
    }

    private String formatQuantity(BigDecimal quantity) {
        if (quantity == null) {
            throw new IllegalStateException("缺料数量不能为空");
        }
        return quantity.stripTrailingZeros().toPlainString();
    }

    private void scheduleTasks(ScheduleComputation computation) {
        for (MesProWorkOrderDO workOrder : computation.workOrders) {
            if (hasBlockingIssueForWorkOrder(computation.issues, workOrder.getId())) {
                continue;
            }
            MesProRouteProductDO routeProduct = computation.routeProductByWorkOrderId.get(workOrder.getId());
            List<MesProRouteProcessDO> routeProcesses = computation.routeProcessesByWorkOrderId.get(workOrder.getId());
            if (routeProduct == null || CollUtil.isEmpty(routeProcesses)) {
                continue;
            }
            int issueStartIndex = computation.issues.size();
            CandidateLinePlan selectedPlan = selectBestLinePlan(computation, workOrder, routeProduct, routeProcesses);
            if (selectedPlan == null) {
                List<ScheduleIssueDraft> newIssues = computation.issues.subList(
                        issueStartIndex, computation.issues.size());
                if (CollUtil.isNotEmpty(newIssues)
                        && newIssues.stream().allMatch(this::isNonBlockingSchedulingWarning)) {
                    computation.nonBlockingSkippedWorkOrderIds.add(workOrder.getId());
                }
                continue;
            }
            MesProScheduleOrderDO scheduleOrder = findScheduleOrderByWorkOrderId(computation, workOrder.getId());
            if (schedulePlanner.violatesLatestStartConstraint(computation, scheduleOrder, selectedPlan.steps,
                    capacityWindowAllocator, latestStartShiftModeResolver(computation))) {
                computation.issues.add(ScheduleIssueDraft.warning(ISSUE_TYPE_LATEST_START, workOrder.getId(), null, null, null,
                        "计划开工时间晚于最晚开工时间"));
                if (!computation.replanMode) {
                    LocalDateTime latestStartTime = schedulePlanner.calculateLatestStartTime(computation, scheduleOrder,
                            selectedPlan.steps, capacityWindowAllocator, latestStartShiftModeResolver(computation));
                    computation.latestStartRejectedPlans.put(workOrder.getId(),
                            RejectedLatestStartPlan.from(selectedPlan, latestStartTime));
                    List<PreviewStep> preservedSteps = selectedPlan.steps.stream()
                            .filter(step -> !step.generated)
                            .toList();
                    if (CollUtil.isNotEmpty(preservedSteps)) {
                        computation.finalSteps.put(workOrder.getId(), preservedSteps);
                    }
                    computation.workOrderAnalyses.add(selectedPlan.analysis);
                    continue;
                }
            }
            computation.generatedTasks.addAll(selectedPlan.plans);
            if (selectedPlan.capacityLedgerAfterPlan != null) {
                computation.processCapacityLedger = selectedPlan.capacityLedgerAfterPlan;
            }
            computation.finalSteps.put(workOrder.getId(), selectedPlan.steps);
            selectedPlan.processAvailableUntilByKey.forEach((key, endTime) ->
                    computation.lineProcessAvailableFrom.merge(key, endTime,
                            (oldVal, newVal) -> oldVal.isAfter(newVal) ? oldVal : newVal));
            computation.workOrderAnalyses.add(selectedPlan.analysis);
        }

        schedulePlanner.buildLinkPlans(computation);
    }

    private CandidateLinePlan selectBestLinePlan(ScheduleComputation computation,
                                                 MesProWorkOrderDO workOrder,
                                                 MesProRouteProductDO routeProduct,
                                                 List<MesProRouteProcessDO> routeProcesses) {
        return simulateProcessLinePlan(computation, workOrder, routeProduct, routeProcesses);
    }

    private ScheduleIssueDraft firstMissingLineIssue(ScheduleComputation computation,
                                                     MesProWorkOrderDO workOrder,
                                                     List<MesProRouteProcessDO> routeProcesses,
                                                     Long fixedLineId) {
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            List<MesMdWorkstationDO> processStations = computation.workstationsByRouteProcessId.getOrDefault(routeProcess.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(processStations)) {
                return ScheduleIssueDraft.blocking(ISSUE_TYPE_WORKSTATION, workOrder.getId(), routeProcess.getProcessId(), null, null,
                        "工序缺少可用工作站或产线绑定");
            }
            List<MesMdWorkstationDO> enabledStations = processStations.stream()
                    .filter(workstation -> workstation.getProductionLineId() != null)
                    .filter(workstation -> {
                        MesMdProductionLineDO line = computation.lineMap.get(workstation.getProductionLineId());
                        return line != null && ObjUtil.equal(line.getStatus(), CommonStatusEnum.ENABLE.getStatus());
                    })
                    .toList();
            if (CollUtil.isEmpty(enabledStations)) {
                return ScheduleIssueDraft.blocking(
                        hasAnyWorkstationWithoutLine(processStations) ? ISSUE_TYPE_LINE : ISSUE_TYPE_WORKSTATION,
                        workOrder.getId(), routeProcess.getProcessId(), null, null, "工序缺少可用工作站或产线绑定");
            }
            if (fixedLineId != null && enabledStations.stream().noneMatch(workstation -> ObjUtil.equal(workstation.getProductionLineId(), fixedLineId))) {
                return ScheduleIssueDraft.blocking(ISSUE_TYPE_LINE, workOrder.getId(), routeProcess.getProcessId(), null, null,
                        "锁定产线缺少该工序可用工作站");
            }
        }
        return ScheduleIssueDraft.blocking(ISSUE_TYPE_LINE, workOrder.getId(), null, null, null,
                fixedLineId != null ? "锁定产线缺少对应工序可用工作站" : "工序缺少可用工作站或产线绑定");
    }

    private CandidateLinePlan simulateProcessLinePlan(ScheduleComputation computation,
                                                      MesProWorkOrderDO workOrder,
                                                      MesProRouteProductDO routeProduct,
                                                      List<MesProRouteProcessDO> routeProcesses) {
        CandidateLinePlan candidate = CandidateLinePlan.success();
        DailyProcessCapacityLedger candidateLedger = computation.processCapacityLedger.copy();
        candidate.analysis = MesProScheduleCalendarWorkOrderAnalysisRespVO.builder()
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .productId(workOrder.getProductId())
                .productCode(resolveItemCode(computation.itemMap.get(workOrder.getProductId())))
                .productName(resolveItemName(computation.itemMap.get(workOrder.getProductId())))
                .quantity(workOrder.getQuantity())
                .conflict(Boolean.FALSE)
                .processes(new ArrayList<>())
                .build();

        List<MesProRouteProcessDO> orderedRouteProcesses =
                orderRouteProcessesByDependency(computation, workOrder.getId(), routeProcesses);
        Map<Long, LocalDateTime> routeProcessEndTimeMap = new LinkedHashMap<>();
        for (MesProRouteProcessDO routeProcess : orderedRouteProcesses) {
            MesProScheduleOrderProcessDO scheduleOrderProcess = findScheduleOrderProcess(
                    computation, workOrder.getId(), routeProcess);
            if (scheduleOrderProcess == null) {
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_ROUTE, workOrder.getId(),
                        routeProcess.getProcessId(), null, null, "排产工序缺少路线快照"));
                return null;
            }
            if (scheduleOrderProcess.getPredecessorRouteProcessId() == null) {
                if (Boolean.FALSE.equals(scheduleOrderProcess.getRootProcessFlag())) {
                    computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_ROUTE, workOrder.getId(),
                            routeProcess.getProcessId(), null, null, "排产工序缺少直接前置关系快照"));
                    return null;
                }
            } else {
                LocalDateTime predecessorScheduledEndTime =
                        routeProcessEndTimeMap.get(scheduleOrderProcess.getPredecessorRouteProcessId());
                if (predecessorScheduledEndTime == null) {
                    computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_ROUTE, workOrder.getId(),
                            routeProcess.getProcessId(), null, null, "直接前置工序尚未完成排产"));
                    return null;
                }
            }
            String workOrderProcessKey = taskKey(workOrder.getId(), routeProcess.getProcessId());
            List<MesProTaskDO> protectedTasks = computation.preservedTaskByWorkOrderProcess.getOrDefault(
                    workOrderProcessKey, Collections.emptyList());
            if (protectedTasks.size() > 1) {
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_PROTECTED, workOrder.getId(),
                        routeProcess.getProcessId(), null, null, "同一工单工序存在多个受保护任务"));
                return null;
            }

            MesProTaskDO protectedTask = CollUtil.isEmpty(protectedTasks) ? null : protectedTasks.get(0);
            boolean useProtectedTaskResource = shouldUseProtectedTaskResourceForFuturePlanning(computation, protectedTask);
            MesMdWorkstationDO protectedWorkstation = !useProtectedTaskResource ? null
                    : computation.workstationMap.get(protectedTask.getWorkstationId());
            Long requiredLineId = protectedWorkstation != null ? protectedWorkstation.getProductionLineId() : null;
            if (protectedTask != null && (protectedTask.getStartTime() == null || protectedTask.getEndTime() == null)) {
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_PROTECTED, workOrder.getId(),
                        routeProcess.getProcessId(), protectedTask.getWorkstationId(), null, "受保护任务缺少开始或结束时间"));
                return null;
            }
            if (useProtectedTaskResource && requiredLineId == null) {
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_LINE, workOrder.getId(),
                        routeProcess.getProcessId(), protectedTask.getWorkstationId(), null, "受保护任务未绑定产线"));
                return null;
            }
            BigDecimal processScheduleQuantity = resolveProcessScheduleQuantity(workOrder.getQuantity(), scheduleOrderProcess,
                    protectedTask);

            if (protectedTask == null && hasKnownNoRemainingQuantity(scheduleOrderProcess)) {
                rememberRouteProcessEndTime(routeProcessEndTimeMap, routeProcess, scheduleOrderProcess,
                        computation.requestStartTime);
                continue;
            }

            ProcessLineCandidate processCandidate = selectBestProcessLineCandidate(computation, workOrder, routeProduct,
                    routeProcess, scheduleOrderProcess, processScheduleQuantity, requiredLineId, candidateLedger);
            if (processCandidate == null) {
                return null;
            }
            LocalDateTime dependencyReleasedAt = computation.requestStartTime;

            if (protectedTask != null) {
                candidate.steps.add(PreviewStep.fromExisting(protectedTask));
                if (hasRemainingQuantity(scheduleOrderProcess)) {
                    candidate.analysis.getProcesses().add(buildProcessAnalysis(routeProcess, processCandidate.pool,
                            processScheduleQuantity, null, protectedTask.getStartTime(), protectedTask.getEndTime()));
                } else {
                    candidate.analysis.getProcesses().add(buildProcessAnalysis(routeProcess, processCandidate.pool,
                            BigDecimal.ZERO, null, protectedTask.getStartTime(), protectedTask.getEndTime()));
                    rememberRouteProcessEndTime(routeProcessEndTimeMap, routeProcess, scheduleOrderProcess,
                            protectedTask.getEndTime());
                    continue;
                }
            }

            if (processCandidate.capacityLedgerAfterPlan != null) {
                candidateLedger = processCandidate.capacityLedgerAfterPlan;
            }
            processCandidate.plans.forEach(plan -> plan.dependencyReleasedAt = dependencyReleasedAt);
            candidate.plans.addAll(processCandidate.plans);
            processCandidate.plans.forEach(plan -> candidate.steps.add(PreviewStep.fromPlan(plan)));
            PlannedTask lastPlan = processCandidate.plans.get(processCandidate.plans.size() - 1);
            rememberRouteProcessEndTime(routeProcessEndTimeMap, routeProcess, scheduleOrderProcess, lastPlan.endTime);
            candidate.processAvailableUntilByKey.put(
                    resolveAvailabilityKey(processCandidate, routeProcess, scheduleOrderProcess), lastPlan.endTime);
            if (protectedTask == null) {
                candidate.analysis.getProcesses().add(buildProcessAnalysis(routeProcess, processCandidate.pool,
                        processScheduleQuantity, processCandidate.requiredMinutes,
                        processCandidate.plans.get(0).startTime, lastPlan.endTime));
            }
        }

        candidate.startTime = candidate.steps.stream()
                .map(step -> step.startTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        candidate.endTime = candidate.steps.stream()
                .map(step -> step.endTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        candidate.analysis.setStartTime(candidate.startTime);
        candidate.analysis.setEndTime(candidate.endTime);
        applyAnalysisLineSummary(candidate.analysis, routeProduct, computation.routeMap);
        markBottleneck(candidate.analysis);
        candidate.capacityLedgerAfterPlan = candidateLedger;
        return candidate;
    }

    private void rememberRouteProcessEndTime(Map<Long, LocalDateTime> routeProcessEndTimeMap,
                                             MesProRouteProcessDO routeProcess,
                                             MesProScheduleOrderProcessDO scheduleOrderProcess,
                                             LocalDateTime endTime) {
        if (routeProcess != null && routeProcess.getId() != null) {
            routeProcessEndTimeMap.put(routeProcess.getId(), endTime);
        }
        if (scheduleOrderProcess != null && scheduleOrderProcess.getRouteProcessId() != null) {
            routeProcessEndTimeMap.put(scheduleOrderProcess.getRouteProcessId(), endTime);
        }
    }

    private ProcessLineCandidate selectBestProcessLineCandidate(ScheduleComputation computation,
                                                                MesProWorkOrderDO workOrder,
                                                                MesProRouteProductDO routeProduct,
                                                                MesProRouteProcessDO routeProcess,
                                                                MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                                BigDecimal processScheduleQuantity,
                                                                Long requiredLineId,
                                                                DailyProcessCapacityLedger capacityLedger) {
        DailyProcessCapacityLedger ledgerSnapshot = capacityLedger == null
                ? computation.processCapacityLedger
                : capacityLedger;
        if (shouldUseRouteProcessCapacityCandidate(routeProcess, scheduleOrderProcess, requiredLineId)) {
            ProcessLineCandidate routeProcessCandidate = simulateRouteProcessCandidate(computation, workOrder, routeProduct,
                    routeProcess, scheduleOrderProcess, processScheduleQuantity, ledgerSnapshot.copy());
            if (hasFailureIssues(routeProcessCandidate)) {
                computation.issues.addAll(routeProcessCandidate.failureIssues);
                return null;
            }
            return routeProcessCandidate;
        }
        return schedulePlanner.selectBestProcessLineCandidate(computation, routeProcess, requiredLineId,
                lineId -> simulateProcessLineCandidate(computation, workOrder, routeProduct, routeProcess,
                        scheduleOrderProcess, processScheduleQuantity, lineId, ledgerSnapshot.copy()),
                () -> simulateRouteProcessCandidate(computation, workOrder, routeProduct, routeProcess,
                        scheduleOrderProcess, processScheduleQuantity, ledgerSnapshot.copy()));
    }

    private boolean shouldUseRouteProcessCapacityCandidate(MesProRouteProcessDO routeProcess,
                                                           MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                           Long requiredLineId) {
        return requiredLineId == null
                && routeProcess != null
                && routeProcess.getWorkstationId() == null
                && scheduleOrderProcess != null
                && CAPACITY_SOURCE_ROUTE_PROCESS.equals(scheduleOrderProcess.getCapacitySource())
                && hasDailyProcessCapacityLimit(scheduleOrderProcess);
    }

    private ProcessLineCandidate simulateRouteProcessCandidate(ScheduleComputation computation,
                                                               MesProWorkOrderDO workOrder,
                                                                MesProRouteProductDO routeProduct,
                                                                MesProRouteProcessDO routeProcess,
                                                                MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                                BigDecimal processScheduleQuantity,
                                                                DailyProcessCapacityLedger capacityLedger) {
        ProcessResourcePool pool = buildRouteProcessResourcePool(computation, routeProcess, scheduleOrderProcess);
        boolean infiniteCapacity = scheduleOrderProcess != null
                && MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(scheduleOrderProcess.getCapacityMode());
        if (!infiniteCapacity && (pool.effectiveHourlyCapacity == null
                || pool.effectiveHourlyCapacity.compareTo(BigDecimal.ZERO) <= 0)) {
            return ProcessLineCandidate.failed(ScheduleIssueDraft.blocking(ISSUE_TYPE_CAPACITY, workOrder.getId(),
                    routeProcess.getProcessId(), null, null, "路线工序缺少标准小时产能"));
        }
        int requiredMinutes = calculateRequiredProcessMinutes(processScheduleQuantity, routeProcess,
                pool.effectiveHourlyCapacity, scheduleOrderProcess);
        // 工序依赖只校验路线快照；可开始时间由本次排产起点和同资源占用决定。
        LocalDateTime availableFrom = computation.requestStartTime;
        String routeProcessAvailabilityKey = routeProcessAvailabilityKey(routeProcess, scheduleOrderProcess);
        availableFrom = maxTime(availableFrom,
                computation.lineProcessAvailableFrom.get(routeProcessAvailabilityKey));
        String businessLineName = buildScheduleRouteLineLabel(computation, routeProduct.getRouteId());
        String businessLineCode = buildScheduleRouteLineCode(computation, routeProduct.getRouteId());
        BigDecimal targetQuantity = normalizeScheduledQuantity(processScheduleQuantity, workOrder.getQuantity());
        int windowSearchMinutes = calculateRouteProcessWindowSearchMinutes(routeProcess, scheduleOrderProcess,
                targetQuantity, requiredMinutes, availableFrom, capacityLedger);
        List<ShiftWindow> processWindows = capacityWindowAllocator.buildRouteProcessShiftWindows(
                scheduleOrderProcess, availableFrom, windowSearchMinutes,
                date -> resolveCalendarShiftMode(computation, date, scheduleOrderProcess));
        if (CollUtil.isEmpty(processWindows)) {
            return ProcessLineCandidate.failed(ScheduleIssueDraft.blocking(ISSUE_TYPE_CALENDAR, workOrder.getId(),
                    routeProcess.getProcessId(), null, null, "路线工序缺少可用工作日历"));
        }
        List<PlannedTask> processPlans = infiniteCapacity
                ? schedulePlanner.allocateInfiniteProcessPlans(availableFrom, requiredMinutes, processWindows, workOrder, routeProduct,
                routeProcess, pool, null, businessLineName, scheduleOrderProcess, targetQuantity, capacityWindowAllocator, capacityLedger)
                : schedulePlanner.allocateFiniteProcessPlans(availableFrom, requiredMinutes, processWindows, workOrder, routeProduct,
                routeProcess, pool, null, businessLineName, scheduleOrderProcess, targetQuantity, capacityLedger);
        if (CollUtil.isEmpty(processPlans)) {
            boolean nightShiftEnabled = scheduleOrderProcess != null
                    && Boolean.TRUE.equals(scheduleOrderProcess.getNightShiftEnabled());
            ScheduleIssueDraft issue = nightShiftEnabled
                    ? ScheduleIssueDraft.blocking(ISSUE_TYPE_CAPACITY, workOrder.getId(),
                    routeProcess.getProcessId(), null, null, "夜班路线工序缺少可用夜班日历")
                    : (hasDailyProcessCapacityLimit(scheduleOrderProcess)
                    ? ScheduleIssueDraft.blocking(ISSUE_TYPE_CAPACITY, workOrder.getId(),
                    routeProcess.getProcessId(), null, null,
                    buildProcessCapacityInsufficientMessage(scheduleOrderProcess, targetQuantity))
                    : ScheduleIssueDraft.warning(ISSUE_TYPE_CAPACITY, workOrder.getId(),
                    routeProcess.getProcessId(), null, null,
                    ISSUE_MESSAGE_ROUTE_CALENDAR_CAPACITY_INSUFFICIENT));
            return ProcessLineCandidate.failed(issue);
        }
        return ProcessLineCandidate.success(null, businessLineCode, businessLineName,
                routeProcessAvailabilityKey, pool, processPlans, requiredMinutes, capacityLedger);
    }

    private ProcessLineCandidate simulateProcessLineCandidate(ScheduleComputation computation,
                                                              MesProWorkOrderDO workOrder,
                                                              MesProRouteProductDO routeProduct,
                                                              MesProRouteProcessDO routeProcess,
                                                              MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                              BigDecimal processScheduleQuantity,
                                                              Long lineId,
                                                              DailyProcessCapacityLedger capacityLedger) {
        MesMdProductionLineDO line = computation.lineMap.get(lineId);
        if (line == null) {
            return ProcessLineCandidate.failed(ScheduleIssueDraft.blocking(ISSUE_TYPE_LINE, workOrder.getId(),
                    routeProcess.getProcessId(), null, null, "工作站绑定的产线不存在"));
        }
        if (line.getCalendarPlanId() == null || computation.planMap.get(line.getCalendarPlanId()) == null) {
            return ProcessLineCandidate.failed(ScheduleIssueDraft.blocking(ISSUE_TYPE_CALENDAR, workOrder.getId(),
                    routeProcess.getProcessId(), null, null, "产线未配置排班计划"));
        }
        ProcessResourcePool pool = computation.processResourcePoolByLineProcessKey.get(lineProcessKey(lineId, routeProcess.getProcessId()));
        if (pool == null) {
            return ProcessLineCandidate.failed(ScheduleIssueDraft.blocking(ISSUE_TYPE_LINE, workOrder.getId(),
                    routeProcess.getProcessId(), null, null, "工序缺少可用工作站或产线绑定"));
        }

        boolean infiniteCapacity = scheduleOrderProcess != null
                && MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(scheduleOrderProcess.getCapacityMode());
        BigDecimal effectiveHourlyCapacity = resolveEffectiveHourlyCapacity(scheduleOrderProcess, pool);
        if (!infiniteCapacity && (effectiveHourlyCapacity == null || effectiveHourlyCapacity.compareTo(BigDecimal.ZERO) <= 0)) {
            return ProcessLineCandidate.failed(ScheduleIssueDraft.blocking(ISSUE_TYPE_CAPACITY, workOrder.getId(),
                    routeProcess.getProcessId(), null, null, resolveZeroCapacityMessage(pool)));
        }

        int requiredMinutes = calculateRequiredProcessMinutes(processScheduleQuantity, routeProcess,
                effectiveHourlyCapacity, scheduleOrderProcess);
        String businessLineName = buildScheduleRouteLineLabel(computation, routeProduct.getRouteId());
        String businessLineCode = buildScheduleRouteLineCode(computation, routeProduct.getRouteId());
        LocalDateTime availableFrom = computation.requestStartTime;
        availableFrom = maxTime(availableFrom, computation.lineProcessAvailableFrom.get(lineProcessKey(lineId, routeProcess.getProcessId())));
        BigDecimal targetQuantity = normalizeScheduledQuantity(processScheduleQuantity, workOrder.getQuantity());
        ProcessLineCandidate candidate = buildProcessLineCandidateFromCurrentWindows(computation, workOrder, routeProduct,
                routeProcess, scheduleOrderProcess, pool, lineId, businessLineName, businessLineCode, availableFrom,
                requiredMinutes, targetQuantity, infiniteCapacity, capacityLedger == null
                        ? new DailyProcessCapacityLedger() : capacityLedger.copy());
        if (!shouldExtendPlannedLineCapacity(computation, candidate)) {
            return candidate;
        }
        return extendPlannedLineCapacityAndRetry(computation, workOrder, routeProduct, routeProcess, scheduleOrderProcess,
                pool, lineId, businessLineName, businessLineCode, availableFrom, requiredMinutes, targetQuantity,
                infiniteCapacity, capacityLedger == null ? new DailyProcessCapacityLedger() : capacityLedger);
    }

    private ProcessLineCandidate buildProcessLineCandidateFromCurrentWindows(
            ScheduleComputation computation,
            MesProWorkOrderDO workOrder,
            MesProRouteProductDO routeProduct,
            MesProRouteProcessDO routeProcess,
            MesProScheduleOrderProcessDO scheduleOrderProcess,
            ProcessResourcePool pool,
            Long lineId,
            String businessLineName,
            String businessLineCode,
            LocalDateTime availableFrom,
            int requiredMinutes,
            BigDecimal targetQuantity,
            boolean infiniteCapacity,
            DailyProcessCapacityLedger capacityLedger) {
        List<ShiftWindow> windows = computation.shiftWindowsByLineId.getOrDefault(lineId, Collections.emptyList());
        if (windows.isEmpty()) {
            return ProcessLineCandidate.failed(ScheduleIssueDraft.blocking(ISSUE_TYPE_CAPACITY, workOrder.getId(),
                    routeProcess.getProcessId(), null, null,
                    CAPACITY_MODE_ACTUAL.equals(computation.capacityMode) ? "产线缺少实际产能" : "产线缺少计划产能"));
        }
        List<ShiftWindow> effectiveWindows = routeProcess.getWorkstationId() == null
                ? capacityWindowAllocator.appendUnboundNightWindowsFromLineCapacity(lineId, windows, scheduleOrderProcess,
                computation.lineMap, computation.shiftListByPlanId)
                : windows;
        List<ShiftWindow> processWindows = capacityWindowAllocator.filterWindowsForScheduleProcess(
                effectiveWindows, scheduleOrderProcess, date -> resolveCalendarShiftMode(computation, date, scheduleOrderProcess));
        List<ScheduleIssueDraft> missingShiftIssues = buildMissingShiftCapacityIssues(
                computation, workOrder, routeProcess, scheduleOrderProcess, lineId, effectiveWindows,
                availableFrom, requiredMinutes);
        if (CollUtil.isNotEmpty(missingShiftIssues)) {
            return ProcessLineCandidate.failed(missingShiftIssues);
        }
        List<PlannedTask> processPlans = infiniteCapacity
                ? schedulePlanner.allocateInfiniteProcessPlans(availableFrom, requiredMinutes, processWindows, workOrder, routeProduct,
                routeProcess, pool, lineId, businessLineName, scheduleOrderProcess, targetQuantity, capacityWindowAllocator, capacityLedger)
                : schedulePlanner.allocateFiniteProcessPlans(availableFrom, requiredMinutes, processWindows, workOrder, routeProduct,
                routeProcess, pool, lineId, businessLineName, scheduleOrderProcess, targetQuantity, capacityLedger);
        if (CollUtil.isEmpty(processPlans)) {
            return ProcessLineCandidate.failed(ScheduleIssueDraft.blocking(ISSUE_TYPE_CAPACITY, workOrder.getId(),
                    routeProcess.getProcessId(), null, null,
                    buildLineCapacityInsufficientMessage(computation, scheduleOrderProcess)));
        }
        return ProcessLineCandidate.success(lineId, businessLineCode, businessLineName, null, pool, processPlans,
                requiredMinutes, capacityLedger);
    }

    private boolean shouldExtendPlannedLineCapacity(ScheduleComputation computation, ProcessLineCandidate candidate) {
        return CAPACITY_MODE_PLANNED.equals(computation.capacityMode) && hasFailureIssues(candidate);
    }

    private boolean hasFailureIssues(ProcessLineCandidate candidate) {
        return candidate != null && CollUtil.isNotEmpty(candidate.failureIssues);
    }

    private ProcessLineCandidate extendPlannedLineCapacityAndRetry(
            ScheduleComputation computation,
            MesProWorkOrderDO workOrder,
            MesProRouteProductDO routeProduct,
            MesProRouteProcessDO routeProcess,
            MesProScheduleOrderProcessDO scheduleOrderProcess,
            ProcessResourcePool pool,
            Long lineId,
            String businessLineName,
            String businessLineCode,
            LocalDateTime availableFrom,
            int requiredMinutes,
            BigDecimal targetQuantity,
            boolean infiniteCapacity,
            DailyProcessCapacityLedger capacityLedger) {
        LocalDate extensionStartDate = resolveNextLineCapacityExtensionStartDate(computation, lineId, availableFrom.toLocalDate());
        LocalDate searchLimitDate = availableFrom.toLocalDate().plusDays(LINE_CAPACITY_SEARCH_DAY_LIMIT);
        int extensionBatchDays = calculateLineCapacityExtensionBatchDays(scheduleOrderProcess, targetQuantity);
        while (!extensionStartDate.isAfter(searchLimitDate)) {
            LocalDate extensionEndDate = extensionStartDate.plusDays(extensionBatchDays - 1L);
            if (extensionEndDate.isAfter(searchLimitDate)) {
                extensionEndDate = searchLimitDate;
            }
            scheduleCalendarService.ensureCapacityPlanCoverage(Set.of(lineId), extensionStartDate, extensionEndDate);
            refreshCapacityWindowsAfterPlannedExtension(computation);
            ProcessLineCandidate retried = buildProcessLineCandidateFromCurrentWindows(computation, workOrder, routeProduct,
                    routeProcess, scheduleOrderProcess, pool, lineId, businessLineName, businessLineCode, availableFrom,
                    requiredMinutes, targetQuantity, infiniteCapacity, capacityLedger.copy());
            if (!hasFailureIssues(retried)) {
                return retried;
            }
            extensionStartDate = extensionEndDate.plusDays(1);
        }
        String failureMessage = scheduleOrderProcess != null
                && Boolean.TRUE.equals(scheduleOrderProcess.getNightShiftEnabled())
                ? buildLineCapacityInsufficientMessage(computation, scheduleOrderProcess)
                : buildLineCapacitySearchLimitMessage(scheduleOrderProcess, targetQuantity);
        ScheduleIssueDraft searchLimitIssue = ScheduleIssueDraft.blocking(ISSUE_TYPE_CAPACITY, workOrder.getId(),
                routeProcess.getProcessId(), null, null, failureMessage);
        return ProcessLineCandidate.failed(searchLimitIssue);
    }

    private LocalDate resolveNextLineCapacityExtensionStartDate(ScheduleComputation computation,
                                                               Long lineId,
                                                               LocalDate fallbackDate) {
        return computation.shiftWindowsByLineId.getOrDefault(lineId, Collections.emptyList()).stream()
                .map(window -> window.calendarDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .map(date -> date.plusDays(1))
                .orElse(fallbackDate);
    }

    private int calculateLineCapacityExtensionBatchDays(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                        BigDecimal targetQuantity) {
        int requiredDays = LINE_CAPACITY_EXTENSION_BATCH_DAYS;
        if (hasDailyProcessCapacityLimit(scheduleOrderProcess)
                && targetQuantity != null && targetQuantity.compareTo(BigDecimal.ZERO) > 0) {
            requiredDays = Math.max(requiredDays, targetQuantity
                    .divide(scheduleOrderProcess.getShiftCapacityTotal(), 0, RoundingMode.CEILING)
                    .intValue() + 5);
        }
        return Math.min(365, Math.max(1, requiredDays));
    }

    private void refreshCapacityWindowsAfterPlannedExtension(ScheduleComputation computation) {
        Set<Long> lineIds = computation.lineMap.keySet().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(lineIds)) {
            return;
        }
        LocalDateTime capacityStart = reqDateStart(computation.requestStartTime);
        List<MesProCapacityPlanDO> capacityPlanList = capacityPlanMapper.selectListByLineIdsAndDate(lineIds, capacityStart);
        List<MesProCapacityActualDO> capacityActualList = capacityActualMapper.selectListByLineIdsAndDate(lineIds, capacityStart);
        computation.calendarContext = buildCalendarContext(computation, capacityPlanList, capacityActualList,
                computation.calendarSummary, true);
        computation.processCalendarContextByRuleId = buildProcessCalendarContexts(computation, capacityPlanList,
                capacityActualList);
        computation.shiftWindowsByLineId = capacityWindowAllocator.buildShiftWindows(computation.capacityMode,
                computation.lineMap, computation.shiftMap, computation.planMap, capacityPlanList, capacityActualList);
    }

    private String buildLineCapacityInsufficientMessage(ScheduleComputation computation,
                                                        MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (scheduleOrderProcess != null && Boolean.TRUE.equals(scheduleOrderProcess.getNightShiftEnabled())) {
            return "夜班工序缺少可用夜班班次或夜班产能";
        }
        return CAPACITY_MODE_ACTUAL.equals(computation.capacityMode) ? "产线实际班次产能不足" : "产线可用班次产能不足";
    }

    private String buildLineCapacitySearchLimitMessage(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                       BigDecimal targetQuantity) {
        String processName = scheduleOrderProcess == null
                ? "当前工序"
                : StrUtil.blankToDefault(scheduleOrderProcess.getProcessName(),
                StrUtil.blankToDefault(scheduleOrderProcess.getProcessCode(), "工序#" + scheduleOrderProcess.getProcessId()));
        String quantityText = targetQuantity == null ? "剩余数量" : formatQuantity(targetQuantity);
        return processName + "已向后搜索 " + LINE_CAPACITY_SEARCH_DAY_LIMIT
                + " 天仍未找到足够产线计划产能，" + quantityText + " 件无法全部排产";
    }

    private int calculateRouteProcessWindowSearchMinutes(MesProRouteProcessDO routeProcess,
                                                         MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                         BigDecimal targetQuantity,
                                                         int requiredMinutes,
                                                         LocalDateTime availableFrom,
                                                         DailyProcessCapacityLedger capacityLedger) {
        if (!hasDailyProcessCapacityLimit(scheduleOrderProcess) || targetQuantity == null
                || targetQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return requiredMinutes;
        }
        int dailyWindowMinutes = capacityWindowAllocator.resolveRouteProcessDailyWindowMinutes(scheduleOrderProcess);
        if (dailyWindowMinutes <= 0) {
            return requiredMinutes;
        }
        int requiredDays = calculateRouteProcessCapacitySearchDays(routeProcess, scheduleOrderProcess,
                targetQuantity, availableFrom, capacityLedger);
        long capacityLimitedMinutes = (long) requiredDays * dailyWindowMinutes;
        return (int) Math.min(Integer.MAX_VALUE, Math.max((long) requiredMinutes, capacityLimitedMinutes));
    }

    private int calculateRouteProcessCapacitySearchDays(MesProRouteProcessDO routeProcess,
                                                        MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                        BigDecimal targetQuantity,
                                                        LocalDateTime availableFrom,
                                                        DailyProcessCapacityLedger capacityLedger) {
        BigDecimal remainingQuantity = targetQuantity;
        Long processId = routeProcess == null ? null : routeProcess.getProcessId();
        LocalDate startDate = availableFrom == null ? null : availableFrom.toLocalDate();
        if (processId == null || startDate == null || capacityLedger == null) {
            throw new IllegalStateException("路线工序日产能窗口搜索缺少必要上下文");
        }
        for (int dayOffset = 0; dayOffset < LINE_CAPACITY_SEARCH_DAY_LIMIT; dayOffset++) {
            LocalDate planDate = startDate.plusDays(dayOffset);
            BigDecimal remainingDailyCapacity = capacityLedger.remaining(null, processId, planDate, scheduleOrderProcess);
            if (remainingDailyCapacity != null && remainingDailyCapacity.compareTo(BigDecimal.ZERO) > 0) {
                remainingQuantity = remainingQuantity.subtract(remainingDailyCapacity.setScale(0, RoundingMode.FLOOR));
            }
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                return dayOffset + 1;
            }
        }
        return LINE_CAPACITY_SEARCH_DAY_LIMIT;
    }

    private boolean hasDailyProcessCapacityLimit(MesProScheduleOrderProcessDO scheduleOrderProcess) {
        return scheduleOrderProcess != null
                && scheduleOrderProcess.getShiftCapacityTotal() != null
                && scheduleOrderProcess.getShiftCapacityTotal().compareTo(BigDecimal.ZERO) > 0;
    }

    private String buildProcessCapacityInsufficientMessage(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                           BigDecimal targetQuantity) {
        String processName = scheduleOrderProcess == null
                ? "当前工序"
                : StrUtil.blankToDefault(scheduleOrderProcess.getProcessName(),
                StrUtil.blankToDefault(scheduleOrderProcess.getProcessCode(), "工序#" + scheduleOrderProcess.getProcessId()));
        String quantityText = targetQuantity == null ? "剩余数量" : formatQuantity(targetQuantity);
        return processName + "未来可用工作日历产能不足，" + quantityText + " 件无法全部排产";
    }

    private void applyAnalysisLineSummary(MesProScheduleCalendarWorkOrderAnalysisRespVO analysis,
                                          MesProRouteProductDO routeProduct,
                                          Map<Long, MesProRouteDO> routeMap) {
        if (analysis == null || routeProduct == null || routeProduct.getRouteId() == null) {
            return;
        }
        MesProRouteDO route = routeMap.get(routeProduct.getRouteId());
        analysis.setLineId(routeProduct.getRouteId());
        analysis.setLineCode(buildScheduleRouteLineCode(routeProduct.getRouteId(), route));
        analysis.setLineName(buildScheduleRouteLineLabel(routeProduct.getRouteId(), route));
        analysis.setConflict(Boolean.FALSE);
        analysis.setConflictMessage(null);
    }

    private String buildScheduleRouteLineLabel(ScheduleComputation computation, Long routeId) {
        return buildScheduleRouteLineLabel(routeId, routeId == null ? null : computation.routeMap.get(routeId));
    }

    private String buildScheduleRouteLineLabel(Long routeId, MesProRouteDO route) {
        if (routeId == null) {
            return "未绑定工艺路线";
        }
        if (route == null) {
            return "工艺路线不存在";
        }
        return StrUtil.blankToDefault(route.getName(), StrUtil.blankToDefault(route.getCode(), "工艺路线#" + routeId));
    }

    private String buildScheduleRouteLineCode(ScheduleComputation computation, Long routeId) {
        return buildScheduleRouteLineCode(routeId, routeId == null ? null : computation.routeMap.get(routeId));
    }

    private String buildScheduleRouteLineCode(Long routeId, MesProRouteDO route) {
        if (routeId == null || route == null) {
            return "";
        }
        return StrUtil.blankToDefault(route.getCode(), String.valueOf(routeId));
    }

    private MesProScheduleOrderProcessDO findScheduleOrderProcess(ScheduleComputation computation, Long workOrderId,
                                                                   MesProRouteProcessDO routeProcess) {
        MesProScheduleOrderDO scheduleOrder = findScheduleOrderByWorkOrderId(computation, workOrderId);
        if (scheduleOrder == null || routeProcess == null) {
            return null;
        }
        List<MesProScheduleOrderProcessDO> processes = computation.scheduleOrderProcessesByOrderId
                .getOrDefault(scheduleOrder.getId(), Collections.emptyList());
        MesProScheduleOrderProcessDO matchedByProcessId = processes.stream()
                .filter(process -> ObjUtil.equal(process.getProcessId(), routeProcess.getProcessId()))
                .findFirst()
                .orElse(null);
        if (matchedByProcessId != null) {
            return matchedByProcessId;
        }
        MesProScheduleOrderProcessDO matchedByRouteProcessId = processes.stream()
                .filter(process -> ObjUtil.equal(process.getRouteProcessId(), routeProcess.getId()))
                .findFirst()
                .orElse(null);
        if (matchedByRouteProcessId != null) {
            return matchedByRouteProcessId;
        }
        if (routeProcess.getSort() == null) {
            return null;
        }
        return processes.stream()
                .filter(process -> ObjUtil.equal(process.getSort(), routeProcess.getSort()))
                .findFirst()
                .orElse(null);
    }

    private MesProScheduleOrderDO findScheduleOrderByWorkOrderId(ScheduleComputation computation, Long workOrderId) {
        return computation.scheduleOrders.stream()
                .filter(order -> ObjUtil.equal(order.getWorkOrderId(), workOrderId))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal resolveEffectiveHourlyCapacity(MesProScheduleOrderProcessDO scheduleOrderProcess, ProcessResourcePool pool) {
        if (scheduleOrderProcess != null
                && MesProScheduleCapacityModeEnum.isManualOverrideLike(scheduleOrderProcess.getCapacityMode())
                && scheduleOrderProcess.getHourlyCapacityTotal() != null) {
            return scheduleOrderProcess.getHourlyCapacityTotal();
        }
        return pool.effectiveHourlyCapacity;
    }

    private ProcessResourcePool buildRouteProcessResourcePool(ScheduleComputation computation,
                                                              MesProRouteProcessDO routeProcess,
                                                              MesProScheduleOrderProcessDO scheduleOrderProcess) {
        String processName = scheduleOrderProcess != null && StrUtil.isNotBlank(scheduleOrderProcess.getProcessName())
                ? scheduleOrderProcess.getProcessName()
                : Optional.ofNullable(computation.processMap.get(routeProcess.getProcessId()))
                .map(MesProProcessDO::getName)
                .orElse(null);
        ProcessResourcePool pool = new ProcessResourcePool(null, routeProcess.getProcessId(), processName);
        pool.capacitySource = CAPACITY_SOURCE_ROUTE_PROCESS;
        pool.effectiveHourlyCapacity = scheduleOrderProcess != null
                && scheduleOrderProcess.getHourlyCapacityTotal() != null
                ? scheduleOrderProcess.getHourlyCapacityTotal()
                : BigDecimal.ZERO;
        return pool;
    }

    private List<ScheduleIssueDraft> buildMissingShiftCapacityIssues(ScheduleComputation computation,
                                                                     MesProWorkOrderDO workOrder,
                                                                     MesProRouteProcessDO routeProcess,
                                                                     MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                                     Long lineId,
                                                                     List<ShiftWindow> windows,
                                                                     LocalDateTime availableFrom,
                                                                     int requiredMinutes) {
        MesMdProductionLineDO line = computation.lineMap.get(lineId);
        if (line == null || line.getCalendarPlanId() == null) {
            return Collections.emptyList();
        }
        MesCalPlanDO plan = computation.planMap.get(line.getCalendarPlanId());
        List<MesCalPlanShiftDO> shifts = computation.shiftListByPlanId
                .getOrDefault(line.getCalendarPlanId(), Collections.emptyList()).stream()
                .sorted(Comparator
                        .comparing((MesCalPlanShiftDO shift) -> ObjUtil.defaultIfNull(shift.getSort(), Integer.MAX_VALUE))
                        .thenComparing(shift -> StrUtil.blankToDefault(shift.getStartTime(), "00:00")))
                .toList();
        if (CollUtil.isEmpty(shifts)) {
            return Collections.emptyList();
        }
        Map<String, ShiftWindow> windowMap = windows.stream()
                .collect(Collectors.toMap(window -> capacityWindowAllocator.buildShiftCapacityKey(window.calendarDate, window.shiftId),
                        window -> window, (left, right) -> left, LinkedHashMap::new));
        LocalDate endDate = resolveMissingShiftIssueEndDate(plan, windowMap.values(), availableFrom.toLocalDate());
        boolean nightShiftEnabled = scheduleOrderProcess != null && Boolean.TRUE.equals(scheduleOrderProcess.getNightShiftEnabled());
        LocalDateTime cursor = availableFrom;
        int remainingMinutes = requiredMinutes;
        boolean requiredWindowCovered = false;
        Set<String> seenKeys = new LinkedHashSet<>();
        List<ScheduleIssueDraft> issues = new ArrayList<>();
        for (LocalDate date = availableFrom.toLocalDate();
             !date.isAfter(endDate)
                    && (remainingMinutes > 0 || !requiredWindowCovered);
             date = date.plusDays(1)) {
            int issueCountBeforeDate = issues.size();
            String dateShiftMode = resolveCalendarShiftMode(computation, date, scheduleOrderProcess);
            for (MesCalPlanShiftDO shift : shifts) {
                if (!capacityWindowAllocator.isWindowAllowedByCalendarMode(dateShiftMode, shift, nightShiftEnabled)) {
                    continue;
                }
                boolean nightWindow = capacityWindowAllocator.isNightShift(shift);
                if (!nightShiftEnabled && nightWindow) {
                    continue;
                }
                LocalDateTime shiftStart = capacityWindowAllocator.buildShiftDateTime(date, shift.getStartTime());
                LocalDateTime shiftEnd = capacityWindowAllocator.buildShiftDateTime(date, shift.getEndTime());
                if (!shiftEnd.isAfter(shiftStart)) {
                    shiftEnd = shiftEnd.plusDays(1);
                }
                if (!shiftEnd.isAfter(cursor)) {
                    continue;
                }
                LocalDateTime segmentStart = shiftStart.isAfter(cursor) ? shiftStart : cursor;
                ShiftWindow window = windowMap.get(capacityWindowAllocator.buildShiftCapacityKey(date, shift.getId()));
                if (window == null || !window.usableEnd.isAfter(segmentStart)) {
                    if (seenKeys.add(capacityWindowAllocator.buildShiftCapacityKey(date, shift.getId()))
                            && shouldReportMissingShiftCapacity(nightShiftEnabled, nightWindow)
                            && issues.size() < MAX_SHIFT_CAPACITY_ISSUES) {
                        issues.add(buildMissingShiftCapacityIssue(workOrder, routeProcess, shift, date));
                    }
                    continue;
                }
                long usableMinutes = Duration.between(segmentStart, window.usableEnd).toMinutes();
                if (usableMinutes <= 0) {
                    if (seenKeys.add(capacityWindowAllocator.buildShiftCapacityKey(date, shift.getId()))
                            && shouldReportMissingShiftCapacity(nightShiftEnabled, nightWindow)
                            && issues.size() < MAX_SHIFT_CAPACITY_ISSUES) {
                        issues.add(buildMissingShiftCapacityIssue(workOrder, routeProcess, shift, date));
                    }
                    continue;
                }
                if (!nightWindow || nightShiftEnabled) {
                    requiredWindowCovered = true;
                }
                remainingMinutes -= (int) Math.min(usableMinutes, remainingMinutes);
                cursor = window.usableEnd;
                if (remainingMinutes <= 0 && requiredWindowCovered) {
                    break;
                }
            }
        }
        return remainingMinutes > 0 || !requiredWindowCovered ? issues : Collections.emptyList();
    }

    private boolean shouldReportMissingShiftCapacity(boolean nightShiftEnabled, boolean nightWindow) {
        return nightShiftEnabled ? nightWindow : !nightWindow;
    }

    private ScheduleIssueDraft buildMissingShiftCapacityIssue(MesProWorkOrderDO workOrder,
                                                              MesProRouteProcessDO routeProcess,
                                                              MesCalPlanShiftDO shift,
                                                              LocalDate date) {
        String shiftName = StrUtil.blankToDefault(shift.getName(), "未命名");
        String message = capacityWindowAllocator.isNightShift(shift)
                ? String.format("夜班工序缺少可用夜班班次或夜班产能：%s %s", date, shiftName)
                : String.format("%s %s班次产能缺失", date, shiftName);
        return ScheduleIssueDraft.blocking(ISSUE_TYPE_CAPACITY, workOrder.getId(), routeProcess.getProcessId(), null, null,
                        message)
                .withCalendarShift(date, shift.getId());
    }

    private LocalDate resolveMissingShiftIssueEndDate(MesCalPlanDO plan,
                                                      Collection<ShiftWindow> windows,
                                                      LocalDate fallbackDate) {
        LocalDate maxWindowDate = windows.stream()
                .map(window -> window.calendarDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(fallbackDate);
        LocalDate planEndDate = plan != null && plan.getEndDate() != null
                ? plan.getEndDate().toLocalDate()
                : maxWindowDate;
        return maxWindowDate.isAfter(planEndDate) ? maxWindowDate : planEndDate;
    }

    private List<MesProRouteProcessDO> orderRouteProcessesByDependency(
            ScheduleComputation computation,
            Long workOrderId,
            List<MesProRouteProcessDO> routeProcesses) {
        MesProScheduleOrderDO scheduleOrder = findScheduleOrderByWorkOrderId(computation, workOrderId);
        List<MesProScheduleOrderProcessDO> snapshotProcesses = scheduleOrder == null
                ? Collections.emptyList()
                : computation.scheduleOrderProcessesByOrderId
                .getOrDefault(scheduleOrder.getId(), Collections.emptyList());
        snapshotProcesses = activeTopologyScheduleOrderProcesses(snapshotProcesses);
        if (!hasRouteProcessTopologySnapshot(snapshotProcesses) || hasInactiveTopologyPredecessor(snapshotProcesses)) {
            return routeProcesses.stream()
                    .sorted(Comparator.comparing(MesProRouteProcessDO::getSort,
                                    Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(MesProRouteProcessDO::getId, Comparator.nullsLast(Long::compareTo)))
                    .toList();
        }
        return scheduleTopologyResolver.orderRouteProcessesByDependency(
                scheduleOrder, workOrderId, snapshotProcesses, routeProcesses);
    }

    private String validateRouteProcessTopologySnapshot(
            ScheduleComputation computation,
            Long workOrderId,
            List<MesProRouteProcessDO> routeProcesses) {
        MesProScheduleOrderDO scheduleOrder = findScheduleOrderByWorkOrderId(computation, workOrderId);
        List<MesProScheduleOrderProcessDO> snapshotProcesses =
                scheduleOrder == null ? Collections.emptyList()
                        : computation.scheduleOrderProcessesByOrderId
                        .getOrDefault(scheduleOrder.getId(), Collections.emptyList());
        snapshotProcesses = activeTopologyScheduleOrderProcesses(snapshotProcesses);
        if (hasInactiveTopologyPredecessor(snapshotProcesses)) {
            return null;
        }
        if (!hasRouteProcessTopologySnapshot(snapshotProcesses)) {
            return null;
        }
        return scheduleTopologyResolver.validateRouteProcessTopologySnapshot(
                scheduleOrder, workOrderId, snapshotProcesses, routeProcesses);
    }

    private boolean hasRouteProcessTopologySnapshot(Collection<MesProScheduleOrderProcessDO> snapshotProcesses) {
        return CollUtil.isNotEmpty(snapshotProcesses)
                && snapshotProcesses.stream().anyMatch(item -> item != null
                && (item.getPredecessorRouteProcessId() != null || item.getRootProcessFlag() != null));
    }

    private List<MesProRouteProcessDO> restrictToActiveSnapshotRouteProcesses(
            List<MesProRouteProcessDO> routeProcesses,
            Collection<MesProScheduleOrderProcessDO> snapshotProcesses) {
        List<MesProScheduleOrderProcessDO> activeSnapshotProcesses =
                activeTopologyScheduleOrderProcesses(snapshotProcesses);
        Set<Long> activeRouteProcessIds = activeSnapshotProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getRouteProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Integer> activeRouteProcessSorts = activeSnapshotProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getSort)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (activeRouteProcessIds.isEmpty() && activeRouteProcessSorts.isEmpty()) {
            return routeProcesses;
        }
        List<MesProRouteProcessDO> activeRouteProcesses = routeProcesses.stream()
                .filter(process -> activeRouteProcessIds.contains(process.getId())
                        || activeRouteProcessSorts.contains(process.getSort()))
                .toList();
        return activeRouteProcesses.isEmpty() ? routeProcesses : activeRouteProcesses;
    }

    private boolean hasInactiveTopologyPredecessor(Collection<MesProScheduleOrderProcessDO> snapshotProcesses) {
        if (CollUtil.isEmpty(snapshotProcesses)) {
            return false;
        }
        Set<Long> activeRouteProcessIds = snapshotProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getRouteProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return snapshotProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getPredecessorRouteProcessId)
                .filter(Objects::nonNull)
                .anyMatch(predecessorRouteProcessId -> !activeRouteProcessIds.contains(predecessorRouteProcessId));
    }

    private List<MesProScheduleOrderProcessDO> activeTopologyScheduleOrderProcesses(
            Collection<MesProScheduleOrderProcessDO> processes) {
        if (CollUtil.isEmpty(processes)) {
            return Collections.emptyList();
        }
        return processes.stream()
                .filter(this::isActiveTopologyScheduleOrderProcess)
                .toList();
    }

    private boolean isActiveTopologyScheduleOrderProcess(MesProScheduleOrderProcessDO process) {
        return process != null
                && !Boolean.FALSE.equals(process.getEnabled())
                && process.getRouteProcessId() != null;
    }

    private void hydrateMissingProductionMaterialLists(ScheduleComputation computation) {
        List<String> missingOrderNos = computation.workOrders.stream()
                .filter(workOrder -> computation.routeProcessesByWorkOrderId.containsKey(workOrder.getId()))
                .filter(workOrder -> !computation.workOrderIdsWithProductionMaterialList.contains(workOrder.getId())
                        || computation.workOrderIdsWithUnmappedProductionMaterialList.contains(workOrder.getId()))
                .map(MesProWorkOrderDO::getCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(missingOrderNos)) {
            return;
        }
        productionMaterialListSyncService.syncByProductionOrderNos(missingOrderNos);
        computation.workOrderIdsWithProductionMaterialList.clear();
        computation.workOrderIdsWithUnmappedProductionMaterialList.clear();
        computation.materialDemandByWorkOrderId = buildProductionMaterialDemandMap(computation, true);
        refreshMaterialDemandMasterData(computation);
    }

    private void refreshMaterialDemandMasterData(ScheduleComputation computation) {
        Set<Long> itemIds = computation.materialDemandByWorkOrderId.values().stream()
                .flatMap(demandByItemId -> demandByItemId.keySet().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (itemIds.isEmpty()) {
            computation.availableStockByItemId = Collections.emptyMap();
            return;
        }
        computation.itemMap.putAll(itemService.getItemMap(itemIds));
        List<MesWmMaterialStockDO> stockList = materialStockMapper.selectListByItemIds(itemIds);
        computation.availableStockByItemId = stockList.stream()
                .collect(Collectors.groupingBy(MesWmMaterialStockDO::getItemId,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, MesWmMaterialStockDO::getQuantity, BigDecimal::add)));
    }

    private Map<Long, Map<Long, BigDecimal>> buildProductionMaterialDemandMap(ScheduleComputation computation,
                                                                               boolean recordBlockingIssues) {
        List<MesKingdeeProductionMaterialListDO> rows = productionMaterialListMapper.selectListByWorkOrderIds(computation.workOrderMap.keySet());
        Map<Long, List<MesKingdeeProductionMaterialListDO>> rowsByWorkOrderId = rows.stream()
                .filter(row -> row.getWorkOrderId() != null)
                .collect(Collectors.groupingBy(MesKingdeeProductionMaterialListDO::getWorkOrderId,
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<Long, Map<Long, BigDecimal>> demandByWorkOrderId = new LinkedHashMap<>();
        for (MesProWorkOrderDO workOrder : computation.workOrders) {
            if (!computation.routeProcessesByWorkOrderId.containsKey(workOrder.getId())) {
                continue;
            }
            List<MesKingdeeProductionMaterialListDO> workOrderRows = rowsByWorkOrderId.getOrDefault(workOrder.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(workOrderRows)) {
                demandByWorkOrderId.put(workOrder.getId(), Collections.emptyMap());
                continue;
            }
            computation.workOrderIdsWithProductionMaterialList.add(workOrder.getId());
            Map<Long, BigDecimal> demandByItemId = new LinkedHashMap<>();
            for (MesKingdeeProductionMaterialListDO row : workOrderRows) {
                if (row.getChildMaterialId() == null) {
                    computation.workOrderIdsWithUnmappedProductionMaterialList.add(workOrder.getId());
                    if (recordBlockingIssues) {
                        computation.issues.add(ScheduleIssueDraft.warning(ISSUE_TYPE_MATERIAL_DEMAND, workOrder.getId(), null, null, null,
                                buildProductionMaterialChildMappingMessage(row)));
                    }
                    continue;
                }
                if (row.getRequiredQuantity() == null) {
                    computation.issues.add(ScheduleIssueDraft.warning(ISSUE_TYPE_MATERIAL_DEMAND, workOrder.getId(), null, null, row.getChildMaterialId(),
                            "生产用料清单缺少应发数量"));
                    continue;
                }
                demandByItemId.merge(row.getChildMaterialId(), row.getRequiredQuantity(), BigDecimal::add);
            }
            demandByWorkOrderId.put(workOrder.getId(), demandByItemId);
        }
        return demandByWorkOrderId;
    }

    private String buildProductionMaterialChildMappingMessage(MesKingdeeProductionMaterialListDO row) {
        String code = StrUtil.blankToDefault(row.getChildMaterialCode(), "未填写编码");
        String name = StrUtil.blankToDefault(row.getChildMaterialName(), "未填写名称");
        return "生产用料清单子项未映射本地物料：" + code + " / " + name;
    }

    private MesProScheduleCalendarWorkOrderAnalysisRespVO.ProcessAnalysisItem buildProcessAnalysis(MesProRouteProcessDO routeProcess,
                                                                                                    ProcessResourcePool pool,
                                                                                                    BigDecimal scheduledQuantity,
                                                                                                    Integer plannedDurationMinutes,
                                                                                                    LocalDateTime startTime,
                                                                                                    LocalDateTime endTime) {
        return MesProScheduleCalendarWorkOrderAnalysisRespVO.ProcessAnalysisItem.builder()
                .processId(routeProcess.getProcessId())
                .processName(resolveProcessName(pool.processName, routeProcess.getProcessId()))
                .processSort(routeProcess.getSort())
                .scheduledQuantity(scheduledQuantity)
                .capacitySource(pool.capacitySource)
                .workstationCount(pool.workstationCount)
                .workstationNames(pool.workstationNames)
                .machineCount(pool.machineCount)
                .configuredWorkerCount(pool.configuredWorkerCount)
                .currentWorkerCount(pool.currentWorkerCount)
                .effectiveHourlyCapacity(pool.effectiveHourlyCapacity)
                .plannedDurationMinutes(plannedDurationMinutes)
                .startTime(startTime)
                .endTime(endTime)
                .bottleneck(Boolean.FALSE)
                .build();
    }

    private void markBottleneck(MesProScheduleCalendarWorkOrderAnalysisRespVO analysis) {
        if (analysis == null || CollUtil.isEmpty(analysis.getProcesses())) {
            return;
        }
        MesProScheduleCalendarWorkOrderAnalysisRespVO.ProcessAnalysisItem bottleneck = analysis.getProcesses().stream()
                .filter(item -> item.getEffectiveHourlyCapacity() != null)
                .min(Comparator.comparing(MesProScheduleCalendarWorkOrderAnalysisRespVO.ProcessAnalysisItem::getEffectiveHourlyCapacity)
                        .thenComparing(item -> ObjUtil.defaultIfNull(item.getProcessSort(), Integer.MAX_VALUE)))
                .orElse(null);
        if (bottleneck == null) {
            return;
        }
        bottleneck.setBottleneck(Boolean.TRUE);
        analysis.setBottleneckProcessId(bottleneck.getProcessId());
        analysis.setBottleneckProcessName(bottleneck.getProcessName());
        analysis.setBottleneckHourlyCapacity(bottleneck.getEffectiveHourlyCapacity());
    }

    private String resolveZeroCapacityMessage(ProcessResourcePool pool) {
        if (pool.machineCount > 0) {
            return "设备工序未配置标准小时产能";
        }
        if (pool.currentWorkerCount <= 0 && pool.configuredWorkerCount <= 0) {
            return "无设备且配置人数为0";
        }
        if (!pool.workerHourlyCapacityConfigured) {
            return "无设备且单人标准小时产能为空";
        }
        return "工序有效产能为0";
    }

    private List<GanttDataRespVO> buildPreviewTasks(ScheduleComputation computation) {
        List<GanttDataRespVO> ganttTasks = new ArrayList<>();
        AtomicLong previewTaskId = new AtomicLong(-1L);
        for (MesProWorkOrderDO workOrder : computation.workOrders) {
            MesMdItemDO item = computation.itemMap.get(workOrder.getProductId());
            ganttTasks.add(new GanttDataRespVO()
                    .setId(MesBizTypeConstants.PRO_WORKORDER + "_" + workOrder.getId())
                    .setOriginalId(workOrder.getId())
                    .setType(MesBizTypeConstants.PRO_WORKORDER)
                    .setText(buildGanttText(item, workOrder.getQuantity()))
                    .setWorkOrderCode(workOrder.getCode())
                    .setProduct(item != null ? item.getName() : null)
                    .setQuantity(workOrder.getQuantity())
                    .setProgress(BigDecimal.ZERO.floatValue()));

            for (PreviewStep step : computation.finalSteps.getOrDefault(workOrder.getId(), Collections.emptyList())) {
                ganttTasks.add(step.toGanttDataRespVO(
                        previewTaskId.getAndDecrement(),
                        computation.workstationMap.get(step.workstationId),
                        computation.processMap.get(step.processId),
                        computation.itemMap.get(step.itemId),
                        workOrder.getCode()));
            }
        }
        return ganttTasks;
    }

    private List<GanttLinkRespVO> buildPreviewLinks(ScheduleComputation computation) {
        AtomicLong linkId = new AtomicLong(1L);
        return computation.linkPlans.stream()
                .map(linkPlan -> {
                    PreviewStep source = findLastStep(computation.finalSteps.get(linkPlan.workOrderId), linkPlan.sourceProcessId);
                    PreviewStep target = findFirstStep(computation.finalSteps.get(linkPlan.workOrderId), linkPlan.targetProcessId);
                    if (source == null || target == null) {
                        return null;
                    }
                    return new GanttLinkRespVO()
                            .setId(String.valueOf(linkId.getAndIncrement()))
                            .setSource(source.ganttNodeId())
                            .setTarget(target.ganttNodeId())
                            .setType(DEPENDENCY_TYPE_FS);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private MesProAutoSchedulePreviewRespVO buildPreviewResp(ScheduleComputation computation) {
        List<ScheduleIssueDraft> previewIssues = buildPreviewIssues(computation.issues);
        MesProAutoSchedulePreviewRespVO respVO = new MesProAutoSchedulePreviewRespVO();
        respVO.setPreviewOnly(Boolean.TRUE);
        respVO.setCalendarContextToken(computation.calendarContext == null ? null : computation.calendarContext.token);
        respVO.setCalendarSummary(computation.calendarSummary);
        respVO.setSummary(buildSummary(computation, previewIssues));
        respVO.setTasks(computation.previewTasks);
        respVO.setLinks(computation.previewLinks);
        respVO.setIssues(buildIssueRespList(previewIssues.stream().map(issue -> issue.toDO(issue.id)).toList()));
        respVO.setWorkOrderAnalyses(computation.workOrderAnalyses);
        return respVO;
    }

    private MesProAutoScheduleReplanPreviewRespVO buildReplanPreviewResp(ScheduleComputation computation) {
        List<ScheduleIssueDraft> previewIssues = buildPreviewIssues(computation.issues);
        MesProAutoScheduleReplanPreviewRespVO respVO = new MesProAutoScheduleReplanPreviewRespVO();
        respVO.setPreviewOnly(Boolean.TRUE);
        respVO.setCalendarContextToken(computation.calendarContext == null ? null : computation.calendarContext.token);
        respVO.setCalendarSummary(computation.calendarSummary);
        respVO.setSummary(buildSummary(computation, previewIssues));
        respVO.setTasks(computation.previewTasks);
        respVO.setLinks(computation.previewLinks);
        respVO.setIssues(buildIssueRespList(previewIssues.stream().map(issue -> issue.toDO(issue.id)).toList()));
        respVO.setWorkOrderAnalyses(computation.workOrderAnalyses);
        respVO.setProtectedTasks(buildProtectedTaskRespList(computation));
        return respVO;
    }

    private List<ScheduleIssueDraft> buildPreviewIssues(List<ScheduleIssueDraft> issues) {
        return issues;
    }

    private List<MesProAutoScheduleProtectedTaskRespVO> buildProtectedTaskRespList(ScheduleComputation computation) {
        if (CollUtil.isEmpty(computation.preservedTasks)) {
            return Collections.emptyList();
        }
        return computation.preservedTasks.stream()
                .map(task -> {
                    MesProAutoScheduleProtectedTaskRespVO respVO = new MesProAutoScheduleProtectedTaskRespVO();
                    respVO.setTaskId(task.getId());
                    respVO.setTaskCode(task.getCode());
                    respVO.setWorkOrderId(task.getWorkOrderId());
                    respVO.setProcessId(task.getProcessId());
                    respVO.setWorkstationId(task.getWorkstationId());
                    respVO.setStatus(task.getStatus());
                    respVO.setStartTime(task.getStartTime());
                    respVO.setEndTime(task.getEndTime());
                    respVO.setProtectionReason(computation.protectionReasonByTaskId.get(task.getId()));
                    MesProWorkOrderDO workOrder = computation.workOrderMap.get(task.getWorkOrderId());
                    if (workOrder != null) {
                        respVO.setWorkOrderCode(workOrder.getCode());
                    }
                    cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO process =
                            computation.processMap.get(task.getProcessId());
                    if (process != null) {
                        respVO.setProcessName(process.getName());
                    }
                    MesMdWorkstationDO workstation = computation.workstationMap.get(task.getWorkstationId());
                    if (workstation != null) {
                        respVO.setWorkstationName(workstation.getName());
                    }
                    MesProTaskScheduleExtDO ext = computation.taskExtMap.get(task.getId());
                    respVO.setScheduleSource(ext != null ? ext.getScheduleSource() : SCHEDULE_SOURCE_MANUAL);
                    respVO.setLocked(ext != null && Boolean.TRUE.equals(ext.getLocked()));
                    return respVO;
                })
                .toList();
    }

    private MesProAutoScheduleSummaryRespVO buildSummary(ScheduleComputation computation, List<ScheduleIssueDraft> issues) {
        MesProAutoScheduleSummaryRespVO summary = new MesProAutoScheduleSummaryRespVO();
        summary.setWorkOrderCount(computation.workOrders.size());
        summary.setGeneratedTaskCount(computation.generatedTasks.size());
        summary.setPreservedTaskCount(computation.preservedTasks.size());
        summary.setBlockingIssueCount((int) issues.stream().filter(issue -> ISSUE_SEVERITY_BLOCKING.equals(issue.severity)).count());
        summary.setShortageCount((int) issues.stream().filter(issue -> ISSUE_TYPE_MATERIAL.equals(issue.issueType)).count());
        Set<Long> blockedWorkOrderIds = blockingWorkOrderIds(issues);
        summary.setBlockedWorkOrderCount(blockedWorkOrderIds.size());
        summary.setSkippedWorkOrderCount(computation.nonBlockingSkippedWorkOrderIds.size());
        summary.setAppliedWorkOrderCount((int) computation.workOrders.stream()
                .map(MesProWorkOrderDO::getId)
                .filter(Objects::nonNull)
                .filter(workOrderId -> !blockedWorkOrderIds.contains(workOrderId))
                .filter(workOrderId -> !computation.nonBlockingSkippedWorkOrderIds.contains(workOrderId))
                .count());

        List<LocalDateTime> startTimes = computation.finalSteps.values().stream()
                .flatMap(Collection::stream)
                .map(step -> step.startTime)
                .filter(Objects::nonNull)
                .toList();
        List<LocalDateTime> endTimes = computation.finalSteps.values().stream()
                .flatMap(Collection::stream)
                .map(step -> step.endTime)
                .filter(Objects::nonNull)
                .toList();
        summary.setStartTime(startTimes.stream().min(LocalDateTime::compareTo).orElse(null));
        summary.setEndTime(endTimes.stream().max(LocalDateTime::compareTo).orElse(null));
        return summary;
    }

    private List<MesProAutoScheduleIssueRespVO> buildIssueRespList(List<MesProScheduleIssueDO> issues) {
        if (CollUtil.isEmpty(issues)) {
            return Collections.emptyList();
        }
        Set<Long> workOrderIds = issues.stream().map(MesProScheduleIssueDO::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> processIds = issues.stream().map(MesProScheduleIssueDO::getProcessId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> workstationIds = issues.stream().map(MesProScheduleIssueDO::getWorkstationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> itemIds = issues.stream().map(MesProScheduleIssueDO::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> shiftIds = issues.stream().map(MesProScheduleIssueDO::getShiftId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, MesProWorkOrderDO> workOrderMap = workOrderService.getWorkOrderMap(workOrderIds);
        Map<Long, MesMdWorkstationDO> workstationMap = workstationIds.isEmpty()
                ? Collections.emptyMap()
                : workstationMapper.selectByIds(workstationIds).stream().collect(Collectors.toMap(MesMdWorkstationDO::getId, workstation -> workstation));
        Map<Long, MesMdItemDO> itemMap = itemIds.isEmpty() ? Collections.emptyMap() : itemService.getItemMap(itemIds);
        Map<Long, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO> processMap = processIds.isEmpty()
                ? Collections.emptyMap()
                : processService.getProcessMap(processIds);

        Map<Long, MesCalPlanShiftDO> shiftMap = new HashMap<>();
        if (CollUtil.isNotEmpty(shiftIds)) {
            for (Long shiftId : shiftIds) {
                MesCalPlanShiftDO shift = this.shiftMapFromLoaded(shiftId);
                if (shift != null) {
                    shiftMap.put(shiftId, shift);
                }
            }
        }

        return issues.stream().map(issue -> {
            MesProAutoScheduleIssueRespVO respVO = new MesProAutoScheduleIssueRespVO();
            respVO.setId(issue.getId());
            respVO.setIssueType(issue.getIssueType());
            respVO.setSeverity(issue.getSeverity());
            respVO.setWorkOrderId(issue.getWorkOrderId());
            respVO.setTaskId(issue.getTaskId());
            respVO.setProcessId(issue.getProcessId());
            respVO.setWorkstationId(issue.getWorkstationId());
            respVO.setMaterialId(issue.getMaterialId());
            respVO.setCalendarDate(issue.getCalendarDate());
            respVO.setShiftId(issue.getShiftId());
            respVO.setRequiredQty(issue.getRequiredQty());
            respVO.setAvailableQty(issue.getAvailableQty());
            respVO.setShortageQty(issue.getShortageQty());
            respVO.setMessage(issue.getMessage());
            respVO.setResolved(issue.getResolved());
            respVO.setStatus(issue.getStatus());
            respVO.setSourceType(issue.getSourceType());
            respVO.setSourceId(issue.getSourceId());
            respVO.setResolutionReason(issue.getResolutionReason());
            respVO.setResolvedBy(issue.getResolvedBy());
            respVO.setResolvedAt(issue.getResolvedAt());

            MesProWorkOrderDO workOrder = issue.getWorkOrderId() == null ? null : workOrderMap.get(issue.getWorkOrderId());
            if (workOrder != null) {
                respVO.setWorkOrderCode(workOrder.getCode());
            }
            MesMdWorkstationDO workstation = issue.getWorkstationId() == null ? null : workstationMap.get(issue.getWorkstationId());
            if (workstation != null) {
                respVO.setWorkstationName(workstation.getName());
            }
            MesMdItemDO item = issue.getMaterialId() == null ? null : itemMap.get(issue.getMaterialId());
            if (item != null) {
                respVO.setMaterialCode(item.getCode());
                respVO.setMaterialName(item.getName());
            }
            cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO process =
                    issue.getProcessId() == null ? null : processMap.get(issue.getProcessId());
            if (process != null) {
                respVO.setProcessName(process.getName());
            }
            MesCalPlanShiftDO shift = issue.getShiftId() == null ? null : shiftMap.get(issue.getShiftId());
            if (shift != null) {
                respVO.setShiftName(shift.getName());
            }
            return respVO;
        }).toList();
    }

    private Map<String, ProcessResourcePool> buildProcessResourcePools(ScheduleComputation computation,
                                                                       List<MesMdWorkstationDO> workstations) {
        if (CollUtil.isEmpty(workstations)) {
            return Collections.emptyMap();
        }
        List<MesMdWorkstationDO> eligibleWorkstations = workstations.stream()
                .filter(workstation -> workstation.getProductionLineId() != null)
                .filter(workstation -> {
                    MesMdProductionLineDO line = computation.lineMap.get(workstation.getProductionLineId());
                    return line != null && ObjUtil.equal(line.getStatus(), CommonStatusEnum.ENABLE.getStatus());
                })
                .toList();
        if (eligibleWorkstations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> workstationIds = eligibleWorkstations.stream().map(MesMdWorkstationDO::getId).toList();
        Map<Long, MesMdWorkstationCapacityMetrics> capacityMetrics = workstationCapacityService.getCapacityMetrics(eligibleWorkstations, BigDecimal.ONE);
        Map<Long, List<MesMdWorkstationMachineDO>> machinesByWorkstationId = ObjUtil.defaultIfNull(
                        workstationMachineService.getWorkstationMachineListByWorkstationIds(workstationIds),
                        Collections.<MesMdWorkstationMachineDO>emptyList())
                .stream()
                .collect(Collectors.groupingBy(MesMdWorkstationMachineDO::getWorkstationId, LinkedHashMap::new, Collectors.toList()));
        Map<String, ProcessResourcePool> poolMap = new LinkedHashMap<>();
        for (MesMdWorkstationDO workstation : eligibleWorkstations) {
            String key = lineProcessKey(workstation.getProductionLineId(), workstation.getProcessId());
            ProcessResourcePool pool = poolMap.computeIfAbsent(key, ignored ->
                    new ProcessResourcePool(workstation.getProductionLineId(),
                            workstation.getProcessId(),
                            resolveProcessName(computation.processMap.get(workstation.getProcessId()) != null
                                    ? computation.processMap.get(workstation.getProcessId()).getName() : null, workstation.getProcessId())));
            MesMdWorkstationCapacityMetrics metrics = capacityMetrics.getOrDefault(workstation.getId(),
                    MesMdWorkstationCapacityMetrics.builder()
                            .configuredWorkerCount(0)
                            .currentWorkerCount(0)
                            .machineryStandardHourlyCapacity(BigDecimal.ZERO)
                            .todayCapacity(BigDecimal.ZERO)
                            .build());
            List<MesMdWorkstationMachineDO> machines = machinesByWorkstationId.getOrDefault(workstation.getId(), Collections.emptyList());
            pool.addWorkstation(workstation, metrics, machines);
        }
        return poolMap;
    }

    private int calculateRequiredProcessMinutes(BigDecimal workOrderQuantity,
                                                MesProRouteProcessDO routeProcess,
                                                BigDecimal effectiveHourlyCapacity,
                                                MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (scheduleOrderProcess != null
                && MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode().equals(scheduleOrderProcess.getCapacityMode())) {
            BigDecimal quantityFactor = scheduleOrderProcess.getInfiniteDurationQuantityFactor();
            BigDecimal baseMinutes = scheduleOrderProcess.getInfiniteDurationBaseMinutes();
            if (quantityFactor == null || baseMinutes == null || workOrderQuantity == null) {
                throw exception(PRO_AUTO_SCHEDULE_INFINITE_DURATION_FORMULA_REQUIRED);
            }
            return quantityFactor.multiply(workOrderQuantity)
                    .add(baseMinutes)
                    .setScale(0, RoundingMode.UP)
                    .intValue();
        }
        BigDecimal capacity = effectiveHourlyCapacity == null ? BigDecimal.ZERO : effectiveHourlyCapacity;
        if (capacity.compareTo(BigDecimal.ZERO) <= 0 || workOrderQuantity == null) {
            return Integer.MAX_VALUE;
        }
        int totalMinutes = workOrderQuantity
                .divide(capacity, 8, RoundingMode.UP)
                .multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.UP)
                .intValue();
        totalMinutes += ObjUtil.defaultIfNull(routeProcess.getPrepareTime(), 0);
        totalMinutes += ObjUtil.defaultIfNull(routeProcess.getWaitTime(), 0);
        return Math.max(totalMinutes, 1);
    }

    private List<ScheduleApplier.ScheduleOrderPlanFieldUpdate> buildScheduleOrderPlanFieldUpdates(ScheduleComputation computation) {
        List<ScheduleApplier.ScheduleOrderPlanFieldUpdate> updates = new ArrayList<>();
        Set<Long> blockedWorkOrderIds = blockingWorkOrderIds(computation.issues);
        for (MesProScheduleOrderDO scheduleOrder : computation.scheduleOrders) {
            if (blockedWorkOrderIds.contains(scheduleOrder.getWorkOrderId())) {
                continue;
            }
            List<PreviewStep> steps = computation.finalSteps.getOrDefault(scheduleOrder.getWorkOrderId(), Collections.emptyList());
            if (CollUtil.isEmpty(steps)) {
                ScheduleApplier.ScheduleOrderPlanFieldUpdate rejectedUpdate =
                        buildLatestStartRejectedPlanUpdate(computation, scheduleOrder);
                if (rejectedUpdate != null) {
                    updates.add(rejectedUpdate);
                }
                continue;
            }
            LocalDateTime plannedStartTime = steps.stream()
                    .map(step -> step.startTime)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            LocalDateTime plannedEndTime = steps.stream()
                    .map(step -> step.endTime)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            LocalDateTime latestStartTime = schedulePlanner.calculateLatestStartTime(computation, scheduleOrder, steps,
                    capacityWindowAllocator, latestStartShiftModeResolver(computation));
            updates.add(ScheduleApplier.ScheduleOrderPlanFieldUpdate.of(
                    scheduleOrder.getId(),
                    resolveAppliedScheduleOrderStatus(scheduleOrder),
                    plannedStartTime,
                    plannedEndTime,
                    latestStartTime,
                    plannedEndTime != null && scheduleOrder.getPromiseDate() != null
                            && plannedEndTime.toLocalDate().isAfter(scheduleOrder.getPromiseDate()),
                    plannedStartTime != null && latestStartTime != null
                            && plannedStartTime.isAfter(latestStartTime)));
        }
        return updates;
    }

    private Integer resolveAppliedScheduleOrderStatus(MesProScheduleOrderDO scheduleOrder) {
        Integer status = scheduleOrder.getStatus();
        if (ObjUtil.equal(status, MesProScheduleOrderStatusEnum.CANCELED.getStatus())
                || ObjUtil.equal(status, MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                || ObjUtil.equal(status, MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())) {
            return status;
        }
        return MesProScheduleOrderStatusEnum.SCHEDULED.getStatus();
    }

    private ScheduleApplier.ScheduleOrderPlanFieldUpdate buildLatestStartRejectedPlanUpdate(
            ScheduleComputation computation, MesProScheduleOrderDO scheduleOrder) {
        RejectedLatestStartPlan rejectedPlan = computation.latestStartRejectedPlans.get(scheduleOrder.getWorkOrderId());
        if (rejectedPlan == null) {
            return null;
        }
        return ScheduleApplier.ScheduleOrderPlanFieldUpdate.of(
                scheduleOrder.getId(),
                null,
                rejectedPlan.plannedStartTime,
                rejectedPlan.plannedEndTime,
                rejectedPlan.latestStartTime,
                rejectedPlan.plannedEndTime != null && scheduleOrder.getPromiseDate() != null
                        && rejectedPlan.plannedEndTime.toLocalDate().isAfter(scheduleOrder.getPromiseDate()),
                rejectedPlan.plannedStartTime != null && rejectedPlan.latestStartTime != null
                        && rejectedPlan.plannedStartTime.isAfter(rejectedPlan.latestStartTime));
    }

    private SchedulePlanner.LatestStartCalendarShiftModeResolver latestStartShiftModeResolver(
            ScheduleComputation computation) {
        return (date, scheduleOrderProcess) -> resolveCalendarShiftMode(computation, date, scheduleOrderProcess);
    }

    private String lineProcessKey(Long lineId, Long processId) {
        return LineProcessIdentity.availabilityKey(lineId, processId);
    }

    private String resolveAvailabilityKey(ProcessLineCandidate processCandidate,
                                          MesProRouteProcessDO routeProcess,
                                          MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (StrUtil.isNotBlank(processCandidate.availabilityKey)) {
            return processCandidate.availabilityKey;
        }
        return lineProcessKey(processCandidate.lineId, routeProcess.getProcessId());
    }

    private String routeProcessAvailabilityKey(MesProRouteProcessDO routeProcess,
                                               MesProScheduleOrderProcessDO scheduleOrderProcess) {
        Long routeProcessId = routeProcess == null ? null : routeProcess.getId();
        if (routeProcessId == null && scheduleOrderProcess != null) {
            routeProcessId = scheduleOrderProcess.getRouteProcessId();
        }
        if (routeProcessId != null) {
            return RouteProcessIdentity.availabilityKey(routeProcessId);
        }
        Long routeId = routeProcess == null ? null : routeProcess.getRouteId();
        Long processId = routeProcess == null ? null : routeProcess.getProcessId();
        return RouteProcessIdentity.legacyAvailabilityKey(routeId, processId);
    }

    private String resolveItemCode(MesMdItemDO item) {
        return item != null ? item.getCode() : null;
    }

    private String resolveItemName(MesMdItemDO item) {
        return item != null ? item.getName() : null;
    }

    private String resolveProcessName(String processName, Long processId) {
        if (processName != null && !processName.isBlank()) {
            return processName;
        }
        return processId != null ? "工序#" + processId : "";
    }

    private LocalDateTime resolveEffectiveRequestStartTime(LocalDateTime requestStartTime,
                                                           MesProScheduleCalendarRulesRespVO calendarSummary,
                                                           boolean replanMode) {
        if (requestStartTime == null || calendarSummary == null || calendarSummary.getSimulationCurrentDate() == null) {
            return requestStartTime;
        }
        if (replanMode) {
            return requestStartTime;
        }
        LocalDate simulationCurrentDate = parseCalendarDate(calendarSummary.getSimulationCurrentDate());
        return maxTime(requestStartTime, LocalDateTime.of(simulationCurrentDate, requestStartTime.toLocalTime()));
    }

    private AutoScheduleCalendarContext buildCalendarContext(ScheduleComputation computation,
                                                             List<MesProCapacityPlanDO> capacityPlanList,
                                                             List<MesProCapacityActualDO> capacityActualList,
                                                             MesProScheduleCalendarRulesRespVO calendarSummary,
                                                             boolean includeProcessCalendarRules) {
        LocalDate horizonStartDate = computation.requestStartTime.toLocalDate();
        LocalDate horizonEndDate = resolveCalendarHorizonEnd(computation, capacityPlanList, capacityActualList, horizonStartDate);
        Set<String> holidayDateSet = loadHolidayDateSet(horizonStartDate, horizonEndDate);
        Map<String, String> effectiveShiftModeByDate = new LinkedHashMap<>();
        for (LocalDate cursor = horizonStartDate; !cursor.isAfter(horizonEndDate); cursor = cursor.plusDays(1)) {
            effectiveShiftModeByDate.put(cursor.toString(), resolveDateShiftMode(calendarSummary, cursor, holidayDateSet));
        }

        Map<String, Object> tokenPayload = new LinkedHashMap<>();
        tokenPayload.put("timeZone", CALENDAR_CONTEXT_TIME_ZONE);
        tokenPayload.put("capacityMode", computation.capacityMode);
        tokenPayload.put("requestStartTime", computation.requestStartTime.toString());
        tokenPayload.put("horizonStartDate", horizonStartDate.toString());
        tokenPayload.put("horizonEndDate", horizonEndDate.toString());
        tokenPayload.put("simulationCurrentDate", calendarSummary.getSimulationCurrentDate());
        tokenPayload.put("weekendRestMode",
                normalizeWeekendRestMode(calendarSummary.getWeekendRestMode()));
        tokenPayload.put("skipStatutoryHolidays",
                Boolean.TRUE.equals(calendarSummary.getSkipStatutoryHolidays()));
        tokenPayload.put("dateShiftModeByDate",
                filterDateShiftModeByDate(
                        normalizeDateShiftModeByDate(calendarSummary.getDateShiftModeByDate()),
                        horizonStartDate,
                        horizonEndDate));
        tokenPayload.put("holidayDateSet", new ArrayList<>(holidayDateSet));
        tokenPayload.put("effectiveShiftModeByDate", effectiveShiftModeByDate);
        tokenPayload.put("lineIds", computation.lineMap.keySet().stream().sorted().toList());
        if (includeProcessCalendarRules) {
            tokenPayload.put("processCalendarRules", buildProcessCalendarRuleTokenPayload(computation,
                    horizonStartDate, horizonEndDate));
        }
        String token = DigestUtil.sha256Hex(JsonUtils.toJsonString(tokenPayload));

        return new AutoScheduleCalendarContext(horizonStartDate, horizonEndDate, holidayDateSet, effectiveShiftModeByDate, token);
    }

    private Map<Long, AutoScheduleCalendarContext> buildProcessCalendarContexts(ScheduleComputation computation,
                                                                                List<MesProCapacityPlanDO> capacityPlanList,
                                                                                List<MesProCapacityActualDO> capacityActualList) {
        if (CollUtil.isEmpty(computation.processCalendarSummariesByRuleId)) {
            return Collections.emptyMap();
        }
        Map<Long, AutoScheduleCalendarContext> result = new LinkedHashMap<>();
        for (Map.Entry<Long, MesProScheduleCalendarRulesRespVO> entry : computation.processCalendarSummariesByRuleId.entrySet()) {
            result.put(entry.getKey(), buildCalendarContext(computation, capacityPlanList, capacityActualList,
                    entry.getValue(), false));
        }
        return result;
    }

    private Map<String, Object> buildProcessCalendarRuleTokenPayload(ScheduleComputation computation,
                                                                     LocalDate horizonStartDate,
                                                                     LocalDate horizonEndDate) {
        if (CollUtil.isEmpty(computation.processCalendarSummariesByRuleId)) {
            return Collections.emptyMap();
        }
        Map<String, Object> payload = new TreeMap<>();
        for (Map.Entry<Long, MesProScheduleCalendarRulesRespVO> entry : computation.processCalendarSummariesByRuleId.entrySet()) {
            MesProScheduleCalendarRulesRespVO rule = entry.getValue();
            Map<String, Object> rulePayload = new LinkedHashMap<>();
            rulePayload.put("id", entry.getKey());
            rulePayload.put("weekendRestMode", normalizeWeekendRestMode(rule.getWeekendRestMode()));
            rulePayload.put("skipStatutoryHolidays", Boolean.TRUE.equals(rule.getSkipStatutoryHolidays()));
            rulePayload.put("dateShiftModeByDate", filterDateShiftModeByDate(
                    normalizeDateShiftModeByDate(rule.getDateShiftModeByDate()), horizonStartDate, horizonEndDate));
            payload.put(String.valueOf(entry.getKey()), rulePayload);
        }
        return payload;
    }

    private LocalDate resolveCalendarHorizonEnd(ScheduleComputation computation,
                                                List<MesProCapacityPlanDO> capacityPlanList,
                                                List<MesProCapacityActualDO> capacityActualList,
                                                LocalDate defaultDate) {
        if (CAPACITY_MODE_ACTUAL.equals(computation.capacityMode)) {
            return capacityActualList.stream()
                    .map(MesProCapacityActualDO::getCalendarDate)
                    .filter(Objects::nonNull)
                    .map(LocalDateTime::toLocalDate)
                    .max(LocalDate::compareTo)
                    .orElse(defaultDate);
        }
        return capacityPlanList.stream()
                .map(MesProCapacityPlanDO::getCalendarDate)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .max(LocalDate::compareTo)
                .orElse(defaultDate);
    }

    private Set<String> loadHolidayDateSet(LocalDate startDate, LocalDate endDate) {
        List<MesCalHolidayDO> holidays = holidayService.getHolidayList(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        if (holidays == null || holidays.isEmpty()) {
            return Collections.emptySet();
        }
        return holidays.stream()
                .filter(holiday -> holiday.getDay() != null)
                .filter(holiday -> ObjUtil.equal(holiday.getType(), MesCalHolidayTypeEnum.HOLIDAY.getType()))
                .map(holiday -> holiday.getDay().toLocalDate().toString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String resolveCalendarShiftMode(ScheduleComputation computation, LocalDate date) {
        String mode = computation.calendarContext.effectiveShiftModeByDate.get(date.toString());
        if (mode != null) {
            return mode;
        }
        return resolveDateShiftMode(computation.calendarSummary, date, computation.calendarContext.holidayDateSet);
    }

    private String resolveCalendarShiftMode(ScheduleComputation computation, LocalDate date,
                                            MesProScheduleOrderProcessDO scheduleOrderProcess) {
        AutoScheduleCalendarContext calendarContext = resolveCalendarContext(computation, scheduleOrderProcess);
        MesProScheduleCalendarRulesRespVO calendarSummary = resolveCalendarSummary(computation, scheduleOrderProcess);
        String mode = calendarContext.effectiveShiftModeByDate.get(date.toString());
        if (mode != null) {
            return mode;
        }
        return resolveDateShiftMode(calendarSummary, date, calendarContext.holidayDateSet);
    }

    private AutoScheduleCalendarContext resolveCalendarContext(ScheduleComputation computation,
                                                               MesProScheduleOrderProcessDO scheduleOrderProcess) {
        Long calendarRuleId = scheduleOrderProcess == null ? null : scheduleOrderProcess.getCalendarRuleId();
        if (calendarRuleId == null) {
            return computation.calendarContext;
        }
        AutoScheduleCalendarContext context = computation.processCalendarContextByRuleId.get(calendarRuleId);
        if (context == null) {
            throw new IllegalStateException("工序绑定的日历规则未加载，calendarRuleId=" + calendarRuleId);
        }
        return context;
    }

    private MesProScheduleCalendarRulesRespVO resolveCalendarSummary(ScheduleComputation computation,
                                                                     MesProScheduleOrderProcessDO scheduleOrderProcess) {
        Long calendarRuleId = scheduleOrderProcess == null ? null : scheduleOrderProcess.getCalendarRuleId();
        if (calendarRuleId == null) {
            return computation.calendarSummary;
        }
        MesProScheduleCalendarRulesRespVO calendarSummary = computation.processCalendarSummariesByRuleId.get(calendarRuleId);
        if (calendarSummary == null) {
            throw new IllegalStateException("工序绑定的日历规则未加载，calendarRuleId=" + calendarRuleId);
        }
        return calendarSummary;
    }

    private MesProScheduleCalendarRulesRespVO toCalendarRulesResp(MesProScheduleCalendarRuleDO rule,
                                                                  String simulationCurrentDate) {
        MesProScheduleCalendarRulesRespVO response = new MesProScheduleCalendarRulesRespVO();
        response.setId(rule.getId());
        response.setSkipStatutoryHolidays(Boolean.TRUE.equals(rule.getSkipStatutoryHolidays()));
        response.setWeekendRestMode(normalizeWeekendRestMode(rule.getWeekendRestMode()));
        response.setDateShiftModeByDate(MesProScheduleCalendarRuleSupport.parseDateShiftModeByDate(
                rule.getDateShiftModeByDateJson()));
        response.setSimulationCurrentDate(simulationCurrentDate);
        response.setTemporaryFreezeEnabled(Boolean.TRUE.equals(rule.getTemporaryFreezeEnabled()));
        response.setCalendarContextToken(MesProScheduleCalendarRuleSupport.buildCalendarContextToken(response));
        return response;
    }

    private String resolveDateShiftMode(MesProScheduleCalendarRulesRespVO calendarSummary,
                                        LocalDate date,
                                        Set<String> holidayDateSet) {
        return MesProScheduleCalendarRuleSupport.resolveDateShiftMode(date, calendarSummary, holidayDateSet);
    }

    private Map<String, String> normalizeDateShiftModeByDate(Map<String, String> input) {
        return MesProScheduleCalendarRuleSupport.normalizeDateShiftModeByDate(input);
    }

    private Map<String, String> filterDateShiftModeByDate(Map<String, String> input,
                                                          LocalDate startDate,
                                                          LocalDate endDate) {
        if (input.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            LocalDate date = parseCalendarDate(entry.getKey());
            if (date.isBefore(startDate) || date.isAfter(endDate)) {
                continue;
            }
            filtered.put(entry.getKey(), entry.getValue());
        }
        return filtered;
    }

    private String normalizeWeekendRestMode(String weekendRestMode) {
        return MesProScheduleCalendarRuleSupport.normalizeWeekendRestMode(weekendRestMode);
    }

    private LocalDate parseCalendarDate(String dateText) {
        try {
            return LocalDate.parse(dateText);
        } catch (Exception ex) {
            throw exception(PRO_SCHEDULE_CALENDAR_INVALID_DATE);
        }
    }

    private String resolveShiftCode(MesCalPlanShiftDO shift) {
        return MesProScheduleCalendarRuleSupport.resolveShiftCode(shift);
    }

    private List<ScheduleIssueDraft> validateApplyPreflight(MesProAutoSchedulePreviewReqVO reqVO,
                                                            ScheduleComputation computation) {
        MesProScheduleOrderPreflightReqVO preflightReqVO = new MesProScheduleOrderPreflightReqVO();
        preflightReqVO.setScopeType("SELECTED");
        preflightReqVO.setScheduleOrderIds(new ArrayList<>(reqVO.getScheduleOrderIds()));
        preflightReqVO.setIncludeAdmissionDiff(Boolean.FALSE);
        preflightReqVO.setStartTime(reqVO.getStartTime());
        preflightReqVO.setCapacityMode(reqVO.getRuntimeCapacityBasis());
        MesProScheduleOrderPreflightRespVO preflightResp = Objects.requireNonNull(
                scheduleOrderService.preflight(preflightReqVO), "schedule preflight returned null");
        boolean blocked = PREFLIGHT_RESULT_BLOCKED.equals(preflightResp.getResult())
                || (preflightResp.getSummary() != null
                && ObjUtil.defaultIfNull(preflightResp.getSummary().getBlockedCount(), 0) > 0);
        if (!blocked) {
            return Collections.emptyList();
        }
        List<MesProScheduleOrderPreflightIssueRespVO> blockedIssues =
                Optional.ofNullable(preflightResp.getIssues()).orElse(Collections.emptyList()).stream()
                        .filter(issue -> PREFLIGHT_RESULT_BLOCKED.equals(issue.getSeverity()))
                        .toList();
        Map<Long, Long> workOrderIdByScheduleOrderId = computation.scheduleOrders.stream()
                .filter(order -> order.getId() != null && order.getWorkOrderId() != null)
                .collect(Collectors.toMap(MesProScheduleOrderDO::getId, MesProScheduleOrderDO::getWorkOrderId,
                        (left, right) -> left, LinkedHashMap::new));
        List<ScheduleIssueDraft> drafts = new ArrayList<>();
        for (MesProScheduleOrderPreflightIssueRespVO issue : blockedIssues) {
            Long workOrderId = issue.getWorkOrderId();
            if (workOrderId == null && issue.getScheduleOrderId() != null) {
                workOrderId = workOrderIdByScheduleOrderId.get(issue.getScheduleOrderId());
            }
            if (workOrderId == null) {
                throw exception(PRO_AUTO_SCHEDULE_PREFLIGHT_BLOCKED, resolvePreflightBlockedMessage(preflightResp));
            }
            drafts.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_PREFLIGHT, workOrderId, issue.getProcessId(),
                    null, null, StrUtil.blankToDefault(issue.getMessage(), "排产前检查存在阻断项")));
        }
        if (drafts.isEmpty()) {
            throw exception(PRO_AUTO_SCHEDULE_PREFLIGHT_BLOCKED, resolvePreflightBlockedMessage(preflightResp));
        }
        return drafts;
    }

    private String resolvePreflightBlockedMessage(MesProScheduleOrderPreflightRespVO preflightResp) {
        return Optional.ofNullable(preflightResp.getIssues()).orElse(Collections.emptyList()).stream()
                .filter(issue -> PREFLIGHT_RESULT_BLOCKED.equals(issue.getSeverity()))
                .map(MesProScheduleOrderPreflightIssueRespVO::getMessage)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.collectingAndThen(Collectors.joining("；"),
                        message -> StrUtil.blankToDefault(message, "存在阻断项")));
    }

    private void validateLatestStartZeroTask(ScheduleComputation computation) {
        if (computation.latestStartRejectedPlans.isEmpty()) {
            return;
        }
        boolean hasEffectiveTasks = computation.finalSteps.values().stream()
                .flatMap(Collection::stream)
                .findAny()
                .isPresent();
        if (computation.generatedTasks.isEmpty() && !hasEffectiveTasks) {
            String scheduleOrderCodes = computation.scheduleOrders.stream()
                    .filter(order -> computation.latestStartRejectedPlans.containsKey(order.getWorkOrderId()))
                    .map(MesProScheduleOrderDO::getCode)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining("、"));
            throw exception(PRO_AUTO_SCHEDULE_LATEST_START_ZERO_TASK_BLOCKED,
                    StrUtil.blankToDefault(scheduleOrderCodes, "当前排产范围"));
        }
    }

    private void validateAttributableProcessActiveTaskCoverage(ScheduleComputation computation) {
        if (hasGlobalBlockingIssues(computation.issues)) {
            return;
        }
        Set<Long> blockedWorkOrderIds = blockingWorkOrderIds(computation.issues);
        Set<String> generatedTaskKeys = computation.generatedTasks.stream()
                .map(plan -> taskKey(plan.workOrderId, plan.processId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        computation.generatedTasks.stream()
                .filter(plan -> plan.scheduleOrderProcessId != null)
                .map(plan -> scheduleOrderProcessTaskKey(plan.workOrderId, plan.scheduleOrderProcessId))
                .forEach(generatedTaskKeys::add);
        for (MesProScheduleOrderDO scheduleOrder : computation.scheduleOrders) {
            if (!isAttributable(scheduleOrder)) {
                continue;
            }
            if (blockedWorkOrderIds.contains(scheduleOrder.getWorkOrderId())) {
                continue;
            }
            if (computation.nonBlockingSkippedWorkOrderIds.contains(scheduleOrder.getWorkOrderId())) {
                continue;
            }
            if (computation.latestStartRejectedPlans.containsKey(scheduleOrder.getWorkOrderId())) {
                continue;
            }
            for (MesProScheduleOrderProcessDO process : computation.scheduleOrderProcessesByOrderId
                    .getOrDefault(scheduleOrder.getId(), Collections.emptyList())) {
                if (process == null || Boolean.FALSE.equals(process.getEnabled()) || !hasRemainingQuantity(process)) {
                    continue;
                }
                String workOrderProcessKey = taskKey(scheduleOrder.getWorkOrderId(), process.getProcessId());
                String scheduleOrderProcessKey = scheduleOrderProcessTaskKey(scheduleOrder.getWorkOrderId(), process.getId());
                if (generatedTaskKeys.contains(workOrderProcessKey) || generatedTaskKeys.contains(scheduleOrderProcessKey)) {
                    continue;
                }
                boolean hasPreservedActiveTask = computation.preservedTaskByWorkOrderProcess
                        .getOrDefault(workOrderProcessKey, Collections.emptyList()).stream()
                        .anyMatch(task -> !MesProTaskStatusEnum.isEndStatus(task.getStatus()));
                if (hasPreservedActiveTask) {
                    continue;
                }
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_ACTIVE_TASK,
                        scheduleOrder.getWorkOrderId(),
                        process.getProcessId(),
                        null,
                        null,
                        buildMissingActiveTaskMessage(process)));
            }
        }
    }

    private String buildMissingActiveTaskMessage(MesProScheduleOrderProcessDO process) {
        String processName = StrUtil.blankToDefault(process.getProcessName(), process.getProcessCode());
        if (StrUtil.isBlank(processName)) {
            processName = process.getProcessId() == null ? "当前工序" : "工序#" + process.getProcessId();
        }
        return processName + "仍有剩余报工量，但结果中没有活动任务承接，不能发布重排";
    }

    private void validateGeneratedProcessQuantityTieOut(ScheduleComputation computation) {
        if (hasGlobalBlockingIssues(computation.issues)) {
            return;
        }
        Set<Long> blockedWorkOrderIds = blockingWorkOrderIds(computation.issues);
        for (MesProScheduleOrderDO scheduleOrder : computation.scheduleOrders) {
            if (!isAttributable(scheduleOrder)
                    || blockedWorkOrderIds.contains(scheduleOrder.getWorkOrderId())
                    || computation.nonBlockingSkippedWorkOrderIds.contains(scheduleOrder.getWorkOrderId())
                    || computation.latestStartRejectedPlans.containsKey(scheduleOrder.getWorkOrderId())) {
                continue;
            }
            for (MesProScheduleOrderProcessDO process : computation.scheduleOrderProcessesByOrderId
                    .getOrDefault(scheduleOrder.getId(), Collections.emptyList())) {
                if (process == null || Boolean.FALSE.equals(process.getEnabled()) || !hasRemainingQuantity(process)) {
                    continue;
                }
                BigDecimal expectedQuantity = normalizeTaskQuantity(process.getRemainingQuantity());
                BigDecimal generatedQuantity = sumGeneratedProcessQuantity(
                        computation, scheduleOrder.getWorkOrderId(), process);
                if (generatedQuantity.compareTo(expectedQuantity) == 0) {
                    continue;
                }
                computation.issues.add(ScheduleIssueDraft.blocking(ISSUE_TYPE_ACTIVE_TASK,
                        scheduleOrder.getWorkOrderId(),
                        process.getProcessId(),
                        null,
                        null,
                        buildProcessQuantityTieOutMessage(process, expectedQuantity, generatedQuantity)));
            }
        }
    }

    private BigDecimal sumGeneratedProcessQuantity(ScheduleComputation computation,
                                                   Long workOrderId,
                                                   MesProScheduleOrderProcessDO process) {
        return computation.generatedTasks.stream()
                .filter(plan -> ObjUtil.equal(plan.workOrderId, workOrderId))
                .filter(plan -> ObjUtil.equal(plan.scheduleOrderProcessId, process.getId())
                        || (plan.scheduleOrderProcessId == null && ObjUtil.equal(plan.processId, process.getProcessId())))
                .map(plan -> ObjUtil.defaultIfNull(plan.quantity, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String buildProcessQuantityTieOutMessage(MesProScheduleOrderProcessDO process,
                                                     BigDecimal expectedQuantity,
                                                     BigDecimal generatedQuantity) {
        String processName = StrUtil.blankToDefault(process.getProcessName(), process.getProcessCode());
        if (StrUtil.isBlank(processName)) {
            processName = process.getProcessId() == null ? "当前工序" : "工序#" + process.getProcessId();
        }
        return processName + "剩余数量为 " + formatQuantity(expectedQuantity)
                + "，自动排产生成数量为 " + formatQuantity(generatedQuantity)
                + "，数量不一致，不能发布";
    }

    private boolean hasBlockingIssues(List<ScheduleIssueDraft> issues) {
        return issues.stream().anyMatch(issue -> ISSUE_SEVERITY_BLOCKING.equals(issue.severity));
    }

    private boolean hasGlobalBlockingIssues(List<ScheduleIssueDraft> issues) {
        return issues.stream()
                .anyMatch(issue -> ISSUE_SEVERITY_BLOCKING.equals(issue.severity) && issue.workOrderId == null);
    }

    private boolean hasBlockingIssueForWorkOrder(List<ScheduleIssueDraft> issues, Long workOrderId) {
        if (workOrderId == null) {
            return false;
        }
        return issues.stream()
                .anyMatch(issue -> ISSUE_SEVERITY_BLOCKING.equals(issue.severity)
                        && ObjUtil.equal(workOrderId, issue.workOrderId));
    }

    private Set<Long> blockingWorkOrderIds(List<ScheduleIssueDraft> issues) {
        return issues.stream()
                .filter(issue -> ISSUE_SEVERITY_BLOCKING.equals(issue.severity))
                .map(issue -> issue.workOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isNonBlockingSchedulingWarning(ScheduleIssueDraft issue) {
        return issue != null
                && ISSUE_SEVERITY_WARNING.equals(issue.severity)
                && ISSUE_TYPE_CAPACITY.equals(issue.issueType)
                && ISSUE_MESSAGE_ROUTE_CALENDAR_CAPACITY_INSUFFICIENT.equals(issue.message);
    }

    private void throwBlockingIssue(ScheduleComputation computation, String capacityMode, List<ScheduleIssueDraft> issues) {
        ScheduleIssueDraft first = issues.stream()
                .filter(issue -> ISSUE_SEVERITY_BLOCKING.equals(issue.severity))
                .findFirst()
                .orElseThrow(() -> exception(PRO_AUTO_SCHEDULE_PROTECTED_TASK_CONFLICT));
        String scheduleOrderLabel = resolveIssueScheduleOrderLabel(computation, first);
        String reason = resolveBlockingIssueReason(capacityMode, first);
        if (StrUtil.isNotBlank(scheduleOrderLabel) && StrUtil.isNotBlank(reason)) {
            throw exception(PRO_AUTO_SCHEDULE_ORDER_BLOCKED, scheduleOrderLabel, reason);
        }
        throw switch (first.issueType) {
            case ISSUE_TYPE_ROUTE -> exception(PRO_AUTO_SCHEDULE_ROUTE_REQUIRED);
            case ISSUE_TYPE_ROUTE_PROCESS -> exception(PRO_AUTO_SCHEDULE_ROUTE_PROCESS_REQUIRED);
            case ISSUE_TYPE_WORKSTATION -> exception(PRO_AUTO_SCHEDULE_WORKSTATION_REQUIRED);
            case ISSUE_TYPE_LINE -> exception(PRO_AUTO_SCHEDULE_PRODUCTION_LINE_REQUIRED);
            case ISSUE_TYPE_CALENDAR -> exception(PRO_AUTO_SCHEDULE_CALENDAR_REQUIRED);
            case ISSUE_TYPE_MATERIAL_DEMAND -> exception(PRO_AUTO_SCHEDULE_PRODUCTION_MATERIAL_REQUIRED);
            case ISSUE_TYPE_MATERIAL -> exception(PRO_AUTO_SCHEDULE_MATERIAL_SHORTAGE_BLOCKED);
            case ISSUE_TYPE_ACTIVE_TASK -> exception(PRO_AUTO_SCHEDULE_ACTIVE_TASK_REQUIRED);
            case ISSUE_TYPE_CAPACITY -> CAPACITY_MODE_ACTUAL.equals(normalizeCapacityMode(capacityMode))
                    ? exception(PRO_AUTO_SCHEDULE_ACTUAL_CAPACITY_REQUIRED)
                    : exception(PRO_AUTO_SCHEDULE_CAPACITY_REQUIRED);
            default -> exception(PRO_AUTO_SCHEDULE_PROTECTED_TASK_CONFLICT);
        };
    }

    private String resolveIssueScheduleOrderLabel(ScheduleComputation computation, ScheduleIssueDraft issue) {
        if (computation == null || issue == null || issue.workOrderId == null) {
            return null;
        }
        MesProScheduleOrderDO scheduleOrder = findScheduleOrderByWorkOrderId(computation, issue.workOrderId);
        if (scheduleOrder == null) {
            return null;
        }
        return StrUtil.blankToDefault(scheduleOrder.getCode(), "ID=" + scheduleOrder.getId());
    }

    private String resolveBlockingIssueReason(String capacityMode, ScheduleIssueDraft issue) {
        if (issue == null) {
            return null;
        }
        if (StrUtil.isNotBlank(issue.message)) {
            return issue.message;
        }
        return switch (issue.issueType) {
            case ISSUE_TYPE_ROUTE -> PRO_AUTO_SCHEDULE_ROUTE_REQUIRED.getMsg();
            case ISSUE_TYPE_ROUTE_PROCESS -> PRO_AUTO_SCHEDULE_ROUTE_PROCESS_REQUIRED.getMsg();
            case ISSUE_TYPE_WORKSTATION -> PRO_AUTO_SCHEDULE_WORKSTATION_REQUIRED.getMsg();
            case ISSUE_TYPE_LINE -> PRO_AUTO_SCHEDULE_PRODUCTION_LINE_REQUIRED.getMsg();
            case ISSUE_TYPE_CALENDAR -> PRO_AUTO_SCHEDULE_CALENDAR_REQUIRED.getMsg();
            case ISSUE_TYPE_MATERIAL_DEMAND -> PRO_AUTO_SCHEDULE_PRODUCTION_MATERIAL_REQUIRED.getMsg();
            case ISSUE_TYPE_MATERIAL -> PRO_AUTO_SCHEDULE_MATERIAL_SHORTAGE_BLOCKED.getMsg();
            case ISSUE_TYPE_ACTIVE_TASK -> PRO_AUTO_SCHEDULE_ACTIVE_TASK_REQUIRED.getMsg();
            case ISSUE_TYPE_CAPACITY -> CAPACITY_MODE_ACTUAL.equals(normalizeCapacityMode(capacityMode))
                    ? PRO_AUTO_SCHEDULE_ACTUAL_CAPACITY_REQUIRED.getMsg()
                    : PRO_AUTO_SCHEDULE_CAPACITY_REQUIRED.getMsg();
            default -> PRO_AUTO_SCHEDULE_PROTECTED_TASK_CONFLICT.getMsg();
        };
    }

    private String normalizeCapacityMode(String capacityMode) {
        return scheduleDefaultCompatibilityPolicy.businessDefaultCapacityMode(
                capacityMode, CAPACITY_MODE_ACTUAL, CAPACITY_MODE_PLANNED);
    }

    private boolean hasAnyWorkstationWithoutLine(List<MesMdWorkstationDO> workstations) {
        return workstations != null && workstations.stream().anyMatch(item -> item.getProductionLineId() == null);
    }

    private int calculateDurationMinutes(MesProRouteProductDO routeProduct, BigDecimal workOrderQuantity,
                                         int routeProcessCount, MesProRouteProcessDO routeProcess) {
        BigDecimal baseMinutes = convertToMinutes(routeProduct.getProductionTime(), routeProduct.getTimeUnitType());
        BigDecimal quantityFactor = BigDecimal.ONE;
        if (routeProduct.getQuantity() != null && routeProduct.getQuantity() > 0 && workOrderQuantity != null) {
            quantityFactor = workOrderQuantity.divide(new BigDecimal(routeProduct.getQuantity()), 4, RoundingMode.UP);
        }
        BigDecimal perProcessMinutes = baseMinutes.multiply(quantityFactor)
                .divide(new BigDecimal(Math.max(routeProcessCount, 1)), 0, RoundingMode.UP);
        int totalMinutes = perProcessMinutes.intValue();
        totalMinutes += ObjUtil.defaultIfNull(routeProcess.getPrepareTime(), 0);
        totalMinutes += ObjUtil.defaultIfNull(routeProcess.getWaitTime(), 0);
        return Math.max(totalMinutes, 1);
    }

    private BigDecimal convertToMinutes(BigDecimal productionTime, String timeUnitType) {
        if (productionTime == null) {
            return BigDecimal.valueOf(480);
        }
        if (MesTimeUnitTypeEnum.HOUR.getType().equalsIgnoreCase(timeUnitType)) {
            return productionTime.multiply(BigDecimal.valueOf(60));
        }
        if (MesTimeUnitTypeEnum.DAY.getType().equalsIgnoreCase(timeUnitType)) {
            return productionTime.multiply(BigDecimal.valueOf(480));
        }
        return productionTime;
    }

    private LocalDateTime buildShiftDateTime(LocalDate date, String hhmm) {
        LocalTime time = parseShiftTime(hhmm);
        return LocalDateTime.of(date, time);
    }

    private LocalTime parseShiftTime(String hhmm) {
        return LocalTime.parse(hhmm != null && hhmm.length() == 5 ? hhmm : "00:00");
    }

    private LocalDateTime reqDateStart(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atStartOfDay();
    }

    private MesCalPlanShiftDO shiftMapFromLoaded(Long shiftId) {
        return shiftId == null ? null : planShiftService.getPlanShift(shiftId);
    }

    private GanttLinkRespVO toGanttLink(MesProTaskDependencyDO dependency) {
        return new GanttLinkRespVO()
                .setId(String.valueOf(dependency.getId()))
                .setSource(MesBizTypeConstants.PRO_TASK + "_" + dependency.getSourceTaskId())
                .setTarget(MesBizTypeConstants.PRO_TASK + "_" + dependency.getTargetTaskId())
                .setType(ObjUtil.defaultIfNull(dependency.getDependencyType(), DEPENDENCY_TYPE_FS));
    }

    private String buildGanttText(MesMdItemDO item, BigDecimal quantity) {
        String itemName = item != null ? item.getName() : "";
        String quantityStr = quantity != null ? quantity.stripTrailingZeros().toPlainString() : "";
        return itemName + quantityStr;
    }

    private String buildTaskName(MesMdItemDO item, BigDecimal quantity) {
        String itemName = item != null ? item.getName() : "AUTO";
        return itemName + "【" + quantity.stripTrailingZeros().toPlainString() + "】";
    }

    private BigDecimal resolveProcessScheduleQuantity(BigDecimal workOrderQuantity,
                                                      MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                      MesProTaskDO protectedTask) {
        if (scheduleOrderProcess != null && scheduleOrderProcess.getRemainingQuantity() != null) {
            return scheduleOrderProcess.getRemainingQuantity();
        }
        if (scheduleOrderProcess != null
                && scheduleOrderProcess.getPlannedQuantity() != null
                && scheduleOrderProcess.getPlannedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            return scheduleOrderProcess.getPlannedQuantity();
        }
        return workOrderQuantity;
    }

    private BigDecimal normalizeScheduledQuantity(BigDecimal scheduledQuantity, BigDecimal fallbackQuantity) {
        return normalizeTaskQuantity(scheduleDefaultCompatibilityPolicy.historicalSnapshotScheduleQuantity(
                scheduledQuantity, fallbackQuantity));
    }

    private BigDecimal normalizeTaskQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return null;
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return quantity.setScale(0, RoundingMode.UP);
    }

    private PreviewStep findFirstStep(List<PreviewStep> steps, Long processId) {
        if (CollUtil.isEmpty(steps)) {
            return null;
        }
        return steps.stream().filter(step -> ObjUtil.equal(step.processId, processId)).findFirst().orElse(null);
    }

    private PreviewStep findLastStep(List<PreviewStep> steps, Long processId) {
        if (CollUtil.isEmpty(steps)) {
            return null;
        }
        List<PreviewStep> matchedSteps = steps.stream()
                .filter(step -> ObjUtil.equal(step.processId, processId))
                .toList();
        return matchedSteps.isEmpty() ? null : matchedSteps.get(matchedSteps.size() - 1);
    }

    private String taskKey(Long workOrderId, Long processId) {
        return ScheduleOrderProcessIdentity.workOrderProcessKey(workOrderId, processId);
    }

    private String scheduleOrderProcessTaskKey(Long workOrderId, Long scheduleOrderProcessId) {
        return ScheduleOrderProcessIdentity.scheduleOrderProcessTaskKey(workOrderId, scheduleOrderProcessId);
    }

    private boolean shouldUseProtectedTaskResourceForFuturePlanning(ScheduleComputation computation,
                                                                    MesProTaskDO protectedTask) {
        return protectedTask != null && !isProgressOnlyProtectedTask(computation, protectedTask);
    }

    private boolean isProgressOnlyProtectedTask(ScheduleComputation computation, MesProTaskDO task) {
        if (computation == null || task == null || task.getId() == null) {
            return false;
        }
        String reason = computation.protectionReasonByTaskId.get(task.getId());
        return PROTECTION_REASON_FEEDBACK.equals(reason) || PROTECTION_REASON_FINISHED.equals(reason);
    }

    private Long resolveScheduleOrderId(Map<String, MesProScheduleOrderDO> scheduleOrderByWorkOrderId, Long workOrderId) {
        MesProScheduleOrderDO scheduleOrder = scheduleOrderByWorkOrderId.get(String.valueOf(workOrderId));
        return scheduleOrder == null ? null : scheduleOrder.getId();
    }

    private Long resolveScheduleOrderProcessId(Map<String, MesProScheduleOrderProcessDO> processMap,
                                                Long workOrderId, Long processId, Long matchedScheduleOrderProcessId) {
        if (matchedScheduleOrderProcessId != null) {
            return matchedScheduleOrderProcessId;
        }
        MesProScheduleOrderProcessDO process = processMap.get(taskKey(workOrderId, processId));
        return process == null ? null : process.getId();
    }

    private void syncPreservedTaskScheduleRelations(ScheduleComputation computation,
                                                    Map<String, MesProScheduleOrderDO> scheduleOrderByWorkOrderId,
                                                    Map<String, MesProScheduleOrderProcessDO> scheduleOrderProcessByWorkOrderProcess) {
        List<ScheduleApplier.PreservedTaskScheduleRelation> relations = new ArrayList<>();
        Set<Long> blockedWorkOrderIds = blockingWorkOrderIds(computation.issues);
        for (MesProTaskDO preservedTask : computation.preservedTasks) {
            if (blockedWorkOrderIds.contains(preservedTask.getWorkOrderId())) {
                continue;
            }
            Long scheduleOrderId = resolveScheduleOrderId(scheduleOrderByWorkOrderId, preservedTask.getWorkOrderId());
            Long scheduleOrderProcessId = resolveScheduleOrderProcessId(
                    scheduleOrderProcessByWorkOrderProcess, preservedTask.getWorkOrderId(), preservedTask.getProcessId(), null);
            if (scheduleOrderId == null || scheduleOrderProcessId == null) {
                continue;
            }
            relations.add(ScheduleApplier.PreservedTaskScheduleRelation.of(
                    preservedTask.getId(),
                    preservedTask.getStatus(),
                    scheduleOrderId,
                    scheduleOrderProcessId,
                    computation.taskExtMap.get(preservedTask.getId())));
        }
        scheduleApplier.syncPreservedTaskScheduleRelations(relations);
    }

    private boolean hasRemainingQuantity(MesProScheduleOrderProcessDO process) {
        return process != null
                && process.getRemainingQuantity() != null
                && process.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasKnownNoRemainingQuantity(MesProScheduleOrderProcessDO process) {
        return process != null
                && process.getRemainingQuantity() != null
                && process.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0;
    }

    private boolean isAttributable(MesProScheduleOrderDO scheduleOrder) {
        if (scheduleOrder == null) {
            return false;
        }
        Integer status = scheduleOrder.getStatus();
        return !ObjUtil.equal(status, MesProScheduleOrderStatusEnum.CANCELED.getStatus())
                && !ObjUtil.equal(status, MesProScheduleOrderStatusEnum.FINISHED.getStatus());
    }

    private LocalDateTime maxTime(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private static final class TimeRange {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        private TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

}
