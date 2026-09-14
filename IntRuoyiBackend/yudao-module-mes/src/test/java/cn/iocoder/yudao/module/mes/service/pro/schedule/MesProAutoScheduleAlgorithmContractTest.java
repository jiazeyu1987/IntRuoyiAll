package cn.iocoder.yudao.module.mes.service.pro.schedule;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_ERP_SOURCE_CONFIRMATION_REQUIRED;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoSchedulePreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoSchedulePreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarRulesRespVO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttDataRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityActualMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProReplanExplanationSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskDependencyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderOperationLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderRouteStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesTimeUnitTypeEnum;
import cn.iocoder.yudao.module.mes.service.cal.holiday.MesCalHolidayService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanShiftService;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityMetrics;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProductService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.RouteSnapshotResolver;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleApplyGuard;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleDefaultCompatibilityPolicy;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleInputAssembler;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleTopologyResolver;
import cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_LATEST_START_ZERO_TASK_BLOCKED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_PREFLIGHT_BLOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProAutoScheduleAlgorithmContractTest {

    @InjectMocks
    private MesProAutoScheduleServiceImpl autoScheduleService;

    @Spy private ScheduleTopologyResolver scheduleTopologyResolver = new ScheduleTopologyResolver();
    @Spy private ScheduleApplyGuard scheduleApplyGuard = new ScheduleApplyGuard();
    @Spy private ScheduleDefaultCompatibilityPolicy scheduleDefaultCompatibilityPolicy = new ScheduleDefaultCompatibilityPolicy();
    @Spy private ScheduleInputAssembler scheduleInputAssembler =
            new ScheduleInputAssembler(scheduleDefaultCompatibilityPolicy);
    @Spy private CapacityWindowAllocator capacityWindowAllocator = new CapacityWindowAllocator();
    @Spy private SchedulePlanner schedulePlanner = new SchedulePlanner();
    @InjectMocks private ScheduleApplier scheduleApplier;
    private RouteSnapshotResolver routeSnapshotResolver;

    @Mock private MesProWorkOrderService workOrderService;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;
    @Mock private MesProTaskMapper taskMapper;
    @Mock private MesMdWorkstationMapper workstationMapper;
    @Mock private MesWmMaterialStockMapper materialStockMapper;
    @Mock private MesProRouteProductService routeProductService;
    @Mock private MesProRouteProcessService routeProcessService;
    @Mock private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Mock private MesProRouteService routeService;
    @Mock private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock private MesProRouteVersionMapper routeVersionMapper;
    @Mock private MesProScheduleOrderService scheduleOrderService;
    @Mock private MesMdProductionLineService productionLineService;
    @Mock private MesMdWorkstationCapacityService workstationCapacityService;
    @Mock private MesMdWorkstationMachineService workstationMachineService;
    @Mock private MesCalPlanService planService;
    @Mock private MesCalPlanShiftService planShiftService;
    @Mock private MesCalHolidayService holidayService;
    @Mock private MesProScheduleCalendarService scheduleCalendarService;
    @Mock private MesMdItemService itemService;
    @Mock private MesProProcessService processService;
    @Mock private MesMdAutoCodeRecordService autoCodeRecordService;
    @Mock private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Mock private MesProCapacityPlanMapper capacityPlanMapper;
    @Mock private MesProCapacityActualMapper capacityActualMapper;
    @Mock private MesProReplanExplanationSnapshotMapper replanExplanationSnapshotMapper;
    @Mock private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock private MesProTaskDependencyMapper taskDependencyMapper;
    @Mock private MesProScheduleIssueMapper scheduleIssueMapper;
    @Mock private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock private MesProScheduleOrderOperationLogMapper scheduleOrderOperationLogMapper;
    @Mock private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock private MesProFeedbackMapper feedbackMapper;
    @Mock private MesProBatchRecordExecutionMapper batchRecordExecutionMapper;
    @Mock private MesProEdhrBatchExecutionService edhrBatchExecutionService;

    private MesProWorkOrderDO urgentWorkOrder;
    private MesProWorkOrderDO laterWorkOrder;
    private MesProScheduleOrderDO urgentOrder;
    private MesProScheduleOrderDO laterOrder;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(routeProcessService.getProcessIdentityMap(
                        org.mockito.ArgumentMatchers.anyCollection()))
                .thenAnswer(invocation -> identityMap(invocation.getArgument(0)));
        org.mockito.Mockito.lenient().when(routeProcessService.resolveCurrentRouteProcess(
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class)))
                .thenAnswer(invocation -> resolveRouteProcess(
                        invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        org.mockito.Mockito.lenient().when(routeProcessService.resolveFrozenRouteProcess(
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class)))
                .thenAnswer(invocation -> resolveRouteProcess(
                        invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        lenient().when(routeProcessFlowEdgeMapper.selectListByRouteId(anyLong()))
                .thenReturn(List.of(routeEdge(30L, 31L, 1)));
        routeSnapshotResolver = new RouteSnapshotResolver(routeProcessService, routeProcessFlowEdgeMapper);
        ReflectionTestUtils.setField(autoScheduleService, "routeSnapshotResolver", routeSnapshotResolver);
        ReflectionTestUtils.setField(autoScheduleService, "schedulePlanner", schedulePlanner);
        ReflectionTestUtils.setField(autoScheduleService, "scheduleApplier", scheduleApplier);
        long[] generatedTaskIdSequence = {90_000L};
        lenient().doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(generatedTaskIdSequence[0]++);
            }
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));
        urgentWorkOrder = MesProWorkOrderDO.builder().id(1L).code("WO-URGENT").productId(100L)
                .quantity(new BigDecimal("10")).clientId(10L).build();
        laterWorkOrder = MesProWorkOrderDO.builder().id(2L).code("WO-LATER").productId(100L)
                .quantity(new BigDecimal("10")).clientId(10L).build();
        urgentOrder = scheduleOrder(501L, 1L, LocalDate.of(2026, 5, 14), null);
        laterOrder = scheduleOrder(502L, 2L, LocalDate.of(2026, 5, 20), 1);

        when(routeVersionMapper.selectActiveByRouteId(200L)).thenReturn(MesProRouteVersionDO.builder()
                .id(700L)
                .routeId(200L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionMapper.STATUS_ACTIVE)
                .build());
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(502L, 501L)))
                .thenReturn(List.of(laterOrder, urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("5"), null, null, false),
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), null, new BigDecimal("3"), new BigDecimal("30"), false)));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(502L)).thenReturn(List.of(
                scheduleOrderProcess(603L, 502L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("5"), null, null, false),
                scheduleOrderProcess(604L, 502L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), null, new BigDecimal("3"), new BigDecimal("30"), false)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(laterWorkOrder, urgentWorkOrder));
        lenient().when(workOrderService.getWorkOrderMap(any())).thenReturn(Map.of(1L, urgentWorkOrder, 2L, laterWorkOrder));
        lenient().when(routeProductService.getRouteProductByItemId(100L)).thenReturn(MesProRouteProductDO.builder()
                .id(20L).routeId(200L).itemId(100L).quantity(1).productionTime(BigDecimal.ONE)
                .timeUnitType(MesTimeUnitTypeEnum.HOUR.getType()).build());
        lenient().when(routeProcessService.getRouteProcessListByRouteId(200L)).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(30L).routeId(200L).processId(300L).sort(1).prepareTime(0).waitTime(0).colorCode("#1677ff").build(),
                MesProRouteProcessDO.builder().id(31L).routeId(200L).processId(301L).sort(2).prepareTime(0).waitTime(0).colorCode("#52c41a").build()));
        lenient().when(routeService.getRouteMapIgnoreDeleted(any())).thenReturn(Map.of(200L,
                MesProRouteDO.builder().id(200L).code("ROUTE-A").name("路线A").build()));
        lenient().when(routeFlowConfigMapper.selectByRouteIdAndUseType(200L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(21L)
                        .routeId(200L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(true)
                        .build());
        lenient().when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                200L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(List.of(
                MesProRouteFlowProcessConfigDO.builder()
                        .id(2101L)
                        .routeFlowConfigId(21L)
                        .routeId(200L)
                        .routeProcessId(30L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build(),
                MesProRouteFlowProcessConfigDO.builder()
                        .id(2102L)
                        .routeFlowConfigId(21L)
                        .routeId(200L)
                        .routeProcessId(31L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build()));
        lenient().when(itemService.getItemMap(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            Map<Long, MesMdItemDO> map = new LinkedHashMap<>();
            for (Long id : ids) {
                if (id == null) {
                    continue;
                }
                MesMdItemDO item = new MesMdItemDO();
                item.setId(id);
                if (Long.valueOf(100L).equals(id)) {
                    item.setName("成品A");
                    item.setCode("ITEM-100");
                } else {
                    item.setName("物料" + id);
                    item.setCode("ITEM-" + id);
                }
                map.put(id, item);
            }
            return map;
        });
        lenient().when(processService.getProcessMap(any())).thenReturn(Map.of());
        lenient().when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        lenient().when(taskMapper.selectListByWorkstationIds(any())).thenReturn(Collections.emptyList());
        lenient().when(taskScheduleExtMapper.selectListByTaskIds(any())).thenReturn(Collections.emptyList());
        lenient().when(feedbackMapper.selectListByTaskIds(any())).thenReturn(Collections.emptyList());
        lenient().when(batchRecordExecutionMapper.selectListByTaskIds(any())).thenReturn(Collections.emptyList());
        lenient().when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                MesKingdeeProductionMaterialListDO.builder()
                        .id(1L)
                        .workOrderId(1L)
                        .childMaterialId(901L)
                        .childMaterialCode("MAT-901")
                        .requiredQuantity(BigDecimal.ONE)
                        .build(),
                MesKingdeeProductionMaterialListDO.builder()
                        .id(2L)
                        .workOrderId(2L)
                        .childMaterialId(902L)
                        .childMaterialCode("MAT-902")
                        .requiredQuantity(BigDecimal.ONE)
                        .build()
        ));
        lenient().when(materialStockMapper.selectListByItemIds(any())).thenReturn(Collections.emptyList());
        lenient().when(holidayService.getHolidayList(any(), any())).thenReturn(Collections.emptyList());
        lenient().when(holidayService.getHolidayByDay(any())).thenReturn(null);
        lenient().when(scheduleCalendarService.getRules()).thenReturn(calendarRules());
        lenient().when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(800L).name("P1").processId(300L).productionLineId(900L).status(0).singleStandardHourlyCapacity(new BigDecimal("5")).build(),
                MesMdWorkstationDO.builder().id(801L).name("P2").processId(301L).productionLineId(900L).status(0).singleStandardHourlyCapacity(BigDecimal.ONE).build()));
        lenient().when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(900L,
                MesMdProductionLineDO.builder().id(900L).name("LINE-A").calendarPlanId(700L).status(0).build()));
        lenient().when(planService.getPlanMap(any())).thenReturn(Map.of(700L, MesCalPlanDO.builder()
                .id(700L).startDate(LocalDateTime.of(2026, 5, 13, 0, 0)).endDate(LocalDateTime.of(2026, 5, 22, 0, 0)).build()));
        lenient().when(planShiftService.getPlanShiftListByPlanId(700L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(701L).planId(700L).sort(1).name("DAY").startTime("08:00").endTime("16:00").build(),
                MesCalPlanShiftDO.builder().id(702L).planId(700L).sort(2).name("NIGHT").startTime("20:00").endTime("08:00").build()));
        lenient().when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(capacityPlans());
        lenient().when(capacityActualMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(Collections.emptyList());
        lenient().when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(Collections.emptyList());
        lenient().when(workstationCapacityService.getCapacityMetrics(any(), any())).thenReturn(Map.of(
                800L, MesMdWorkstationCapacityMetrics.builder().configuredWorkerCount(1).currentWorkerCount(1).todayCapacity(new BigDecimal("5")).build(),
                801L, MesMdWorkstationCapacityMetrics.builder().configuredWorkerCount(1).currentWorkerCount(1).todayCapacity(BigDecimal.ONE).build()));
        lenient().when(autoCodeRecordService.generateAutoCode(any())).thenReturn("PT-AUTO");
        lenient().when(scheduleOrderService.preflight(any())).thenReturn(passPreflightResp());
    }

    private Map<Long, Long> identityMap(java.util.Collection<Long> processIds) {
        Map<Long, Long> result = new java.util.LinkedHashMap<>();
        processIds.stream().filter(java.util.Objects::nonNull).forEach(id -> result.put(id, id));
        return result;
    }

    private MesProRouteProcessFlowEdgeDO routeEdge(Long sourceRouteProcessId, Long targetRouteProcessId, Integer sort) {
        return MesProRouteProcessFlowEdgeDO.builder()
                .sourceRouteProcessId(sourceRouteProcessId)
                .targetRouteProcessId(targetRouteProcessId)
                .sort(sort)
                .build();
    }

    private MesProRouteProcessDO resolveRouteProcess(Long routeProcessId, Long routeId, Long processId) {
        List<MesProRouteProcessDO> routeProcesses = routeProcessService.getRouteProcessListByRouteId(routeId);
        if (routeProcesses != null) {
            if (routeProcessId != null) {
                for (MesProRouteProcessDO routeProcess : routeProcesses) {
                    if (routeProcessId.equals(routeProcess.getId())) {
                        return routeProcess;
                    }
                }
            }
            if (processId != null) {
                for (MesProRouteProcessDO routeProcess : routeProcesses) {
                    if (processId.equals(routeProcess.getProcessId())) {
                        return routeProcess;
                    }
                }
            }
        }
        return MesProRouteProcessDO.builder()
                .id(routeProcessId)
                .routeId(routeId)
                .processId(processId)
                .build();
    }

    @Test
    void apply_shouldSortByPromiseDateThenPriorityAndPersistRiskAndLatestStart() {
        MesProAutoSchedulePreviewReqVO reqVO = req();
        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        autoScheduleService.apply(reqVO);

        ArgumentCaptor<MesProTaskDO> taskCaptor = ArgumentCaptor.forClass(MesProTaskDO.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).insert(taskCaptor.capture());
        List<MesProTaskDO> tasks = taskCaptor.getAllValues();
        assertEquals(1L, tasks.get(0).getWorkOrderId());
        assertEquals(2L, tasks.get(2).getWorkOrderId());
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), tasks.get(0).getStartTime());
        assertEquals(LocalDateTime.of(2026, 5, 13, 10, 0), tasks.get(0).getEndTime());
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), tasks.get(1).getStartTime());
        assertEquals(LocalDateTime.of(2026, 5, 13, 9, 0), tasks.get(1).getEndTime());

        ArgumentCaptor<MesProScheduleOrderDO> orderCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper, org.mockito.Mockito.atLeast(2)).updateById(orderCaptor.capture());
        MesProScheduleOrderDO urgentUpdate = orderCaptor.getAllValues().stream()
                .filter(order -> Long.valueOf(501L).equals(order.getId()))
                .findFirst().orElseThrow();
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), urgentUpdate.getPlannedStartTime());
        assertEquals(LocalDateTime.of(2026, 5, 13, 10, 0), urgentUpdate.getPlannedEndTime());
        assertEquals(LocalDateTime.of(2026, 5, 14, 13, 0), urgentUpdate.getLatestStartTime());
        assertFalse(urgentUpdate.getDelayRiskFlag());
        assertFalse(urgentUpdate.getStartRiskFlag());

        MesProScheduleOrderDO delayUpdate = orderCaptor.getAllValues().stream()
                .filter(order -> Long.valueOf(502L).equals(order.getId()))
                .findFirst().orElseThrow();
        assertTrue(delayUpdate.getPlannedStartTime().isAfter(urgentUpdate.getPlannedStartTime()));
        assertFalse(delayUpdate.getDelayRiskFlag());
    }

    @Test
    void previewAndApply_shouldHoldScheduleOrderWhenPlanStartsAfterLatestStart() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        MesProAutoSchedulePreviewReqVO reqVO = req(List.of(501L), LocalDateTime.of(2026, 5, 16, 8, 0));
        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        assertEquals(0, preview.getSummary().getGeneratedTaskCount());
        assertEquals(1, preview.getTasks().stream().filter(task -> Integer.valueOf(301).equals(task.getType())).count());
        assertEquals(0, preview.getTasks().stream().filter(task -> Integer.valueOf(303).equals(task.getType())).count());
        assertTrue(preview.getIssues().stream()
                .anyMatch(issue -> "LATEST_START".equals(issue.getIssueType())
                        && "WARNING".equals(issue.getSeverity())
                        && issue.getMessage().contains("晚于最晚开工")));

        ServiceException ex = assertThrows(ServiceException.class, () -> autoScheduleService.apply(reqVO));

        assertEquals(PRO_AUTO_SCHEDULE_LATEST_START_ZERO_TASK_BLOCKED.getCode(), ex.getCode());
        verify(taskMapper, never()).insert(any(MesProTaskDO.class));
        verify(taskScheduleExtMapper, never()).insert(any(MesProTaskScheduleExtDO.class));
        verify(edhrBatchExecutionService, never()).openOrCreateFromScheduleCompletion(any());
        verify(scheduleOrderMapper, never()).updateById(any(MesProScheduleOrderDO.class));
        verify(scheduleIssueMapper, never()).insertBatch(any());
    }

    @Test
    void apply_shouldFailFastWhenBackendPreflightBlocked() {
        MesProAutoSchedulePreviewReqVO reqVO = req();
        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());
        when(scheduleOrderService.preflight(any())).thenReturn(blockedPreflightResp("工艺流程批记录配置缺少默认批记录"));

        ServiceException ex = assertThrows(ServiceException.class, () -> autoScheduleService.apply(reqVO));

        assertEquals(PRO_AUTO_SCHEDULE_PREFLIGHT_BLOCKED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("工艺流程批记录配置缺少默认批记录"));
        verify(taskMapper, never()).insert(any(MesProTaskDO.class));
        verify(taskScheduleExtMapper, never()).insert(any(MesProTaskScheduleExtDO.class));
        verify(scheduleOrderMapper, never()).updateById(any(MesProScheduleOrderDO.class));
        verify(edhrBatchExecutionService, never()).openOrCreateFromScheduleCompletion(any());
    }

    @Test
    void apply_shouldRequireExplicitConfirmationWhenErpFormalSourceIsMissing() {
        MesProAutoSchedulePreviewReqVO reqVO = req();
        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());
        when(scheduleOrderService.preflight(any())).thenReturn(erpSourceWarningPreflightResp());

        ServiceException ex = assertThrows(ServiceException.class, () -> autoScheduleService.apply(reqVO));

        assertEquals(PRO_AUTO_SCHEDULE_ERP_SOURCE_CONFIRMATION_REQUIRED.getCode(), ex.getCode());
        verify(taskMapper, never()).insert(any(MesProTaskDO.class));
    }

    @Test
    void apply_shouldProceedAfterExplicitConfirmationWhenErpFormalSourceIsMissing() {
        MesProAutoSchedulePreviewReqVO reqVO = req();
        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());
        reqVO.setErpSourceRiskConfirmed(Boolean.TRUE);
        when(scheduleOrderService.preflight(any())).thenReturn(erpSourceWarningPreflightResp());

        MesProAutoScheduleApplyRespVO response = autoScheduleService.apply(reqVO);

        assertTrue(response.getApplied());
    }

    @Test
    void nightlyReplan_shouldPreviewAndApplySameScopeWithRealCalendarGuard() {
        when(scheduleOrderMapper.selectListForNightlyReplan()).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        MesProNightlyReplanService nightlyService =
                new MesProNightlyReplanServiceImpl(scheduleOrderMapper, autoScheduleService);

        MesProNightlyReplanResult result = nightlyService.executeNightlyReplan(
                LocalDateTime.of(2026, 5, 13, 8, 0));

        assertEquals(1, result.getScheduleOrderCount());
        assertTrue(result.getGeneratedTaskCount() > 0);
        verify(taskMapper, atLeastOnce()).insert(any(MesProTaskDO.class));
    }

    @Test
    void nightlyReplan_shouldStopAtErpConfirmationAfterRealPreviewWithoutTaskWrites() {
        when(scheduleOrderMapper.selectListForNightlyReplan()).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        when(scheduleOrderService.preflight(any())).thenReturn(erpSourceWarningPreflightResp());
        MesProNightlyReplanService nightlyService =
                new MesProNightlyReplanServiceImpl(scheduleOrderMapper, autoScheduleService);

        ServiceException ex = assertThrows(ServiceException.class, () -> nightlyService.executeNightlyReplan(
                LocalDateTime.of(2026, 5, 13, 8, 0)));

        assertEquals(PRO_AUTO_SCHEDULE_ERP_SOURCE_CONFIRMATION_REQUIRED.getCode(), ex.getCode());
        verify(taskMapper, never()).insert(any(MesProTaskDO.class));
        verify(taskScheduleExtMapper, never()).insert(any(MesProTaskScheduleExtDO.class));
        verify(scheduleOrderMapper, never()).updateById(any(MesProScheduleOrderDO.class));
    }

    @Test
    void preview_shouldUseNightWindowWhenScheduleProcessAllowsNightShift() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("5"), null, null, true),
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), null, new BigDecimal("3"), new BigDecimal("30"), true)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        MesProAutoSchedulePreviewReqVO reqVO = req(List.of(501L), LocalDateTime.of(2026, 5, 13, 20, 0));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);

        List<GanttDataRespVO> finiteTasks = processTasks(preview, 1L, 300L);
        List<GanttDataRespVO> infiniteTasks = processTasks(preview, 1L, 301L);
        assertEquals(1, finiteTasks.size());
        assertEquals(1, infiniteTasks.size());
        assertEquals(LocalDateTime.of(2026, 5, 13, 20, 0), finiteTasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 22, 0), finiteTasks.get(0).getEndDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 20, 0), infiniteTasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 21, 0), infiniteTasks.get(0).getEndDate());
    }

    @Test
    void preview_shouldPreferNightWindowWhenNightShiftProcessStartsDuringDayShift() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("5"), null, null, true),
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), null, new BigDecimal("3"), new BigDecimal("30"), false)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        MesProAutoSchedulePreviewReqVO reqVO = req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);

        List<GanttDataRespVO> finiteTasks = processTasks(preview, 1L, 300L);
        assertEquals(1, finiteTasks.size());
        assertEquals(LocalDateTime.of(2026, 5, 13, 20, 0), finiteTasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 22, 0), finiteTasks.get(0).getEndDate());
    }

    @Test
    void preview_shouldBlockNightShiftProcessWhenOnlyDayCapacityExists() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("5"), null, null, true)));
        lenient().when(routeProcessService.getRouteProcessListByRouteId(200L)).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(30L).routeId(200L).processId(300L).sort(1)
                        .workstationId(800L).prepareTime(0).waitTime(0).colorCode("#1677ff").build()));
        lenient().when(workstationMapper.selectByIds(any())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(800L).name("P1").processId(300L).productionLineId(900L)
                        .status(0).singleStandardHourlyCapacity(new BigDecimal("5")).build()));
        lenient().when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(
                capacityPlan(900L, LocalDateTime.of(2026, 5, 13, 0, 0), 701L)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(
                req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0)));

        assertEquals(0, preview.getSummary().getGeneratedTaskCount());
        assertEquals(1, preview.getSummary().getBlockingIssueCount());
        var capacityIssues = preview.getIssues().stream()
                .filter(issue -> "CAPACITY".equals(issue.getIssueType()))
                .toList();
        assertEquals(1, capacityIssues.size(), () -> String.valueOf(preview.getIssues()));
        assertEquals("BLOCKING", capacityIssues.get(0).getSeverity());
        assertEquals(1L, capacityIssues.get(0).getWorkOrderId());
        assertEquals(300L, capacityIssues.get(0).getProcessId());
        assertEquals("夜班工序缺少可用夜班班次或夜班产能", capacityIssues.get(0).getMessage());
    }

    @Test
    void preview_shouldPreferNightWindowsForEveryFiniteNightShiftSegment() {
        urgentWorkOrder.setQuantity(new BigDecimal("50"));
        urgentOrder = scheduleOrder(501L, 1L, LocalDate.of(2026, 5, 20), null);
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        MesProScheduleOrderProcessDO nightProcess = scheduleOrderProcess(601L, 501L, 300L, 1,
                MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("5"), null, null, true);
        nightProcess.setPlannedQuantity(new BigDecimal("50"));
        nightProcess.setRemainingQuantity(new BigDecimal("50"));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                nightProcess,
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), null, new BigDecimal("3"), new BigDecimal("30"), false)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        MesProAutoSchedulePreviewReqVO reqVO = req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);

        List<GanttDataRespVO> finiteTasks = processTasks(preview, 1L, 300L);
        assertEquals(2, finiteTasks.size());
        assertEquals(LocalDateTime.of(2026, 5, 13, 20, 0), finiteTasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 14, 4, 0), finiteTasks.get(0).getEndDate());
        assertEquals(LocalDateTime.of(2026, 5, 14, 20, 0), finiteTasks.get(1).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 14, 22, 0), finiteTasks.get(1).getEndDate());
    }

    @Test
    void preview_shouldBlockNightShiftProcessWhenLineHasOnlyDayShift() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("5"), null, null, true)));
        lenient().when(routeProcessService.getRouteProcessListByRouteId(200L)).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(30L).routeId(200L).processId(300L).sort(1)
                        .prepareTime(0).waitTime(0).colorCode("#1677ff").build()));
        lenient().when(planShiftService.getPlanShiftListByPlanId(700L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(701L).planId(700L).sort(1).name("AUTO-DAY").startTime("08:00").endTime("15:00").build()));
        lenient().when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(
                capacityPlan(900L, LocalDateTime.of(2026, 5, 13, 0, 0), 701L)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(
                req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0)));

        assertEquals(0, preview.getSummary().getGeneratedTaskCount());
        assertEquals(1, preview.getSummary().getBlockingIssueCount());
        var capacityIssues = preview.getIssues().stream()
                .filter(issue -> "CAPACITY".equals(issue.getIssueType()))
                .toList();
        assertEquals(1, capacityIssues.size(), () -> String.valueOf(preview.getIssues()));
        assertEquals("BLOCKING", capacityIssues.get(0).getSeverity());
        assertEquals(1L, capacityIssues.get(0).getWorkOrderId());
        assertEquals(300L, capacityIssues.get(0).getProcessId());
        assertEquals("夜班工序缺少可用夜班班次或夜班产能", capacityIssues.get(0).getMessage());
    }

    @Test
    void apply_shouldPersistBlockedIssueAndContinueSchedulableWorkOrders() {
        urgentWorkOrder.setProductId(101L);
        urgentOrder.setProductId(101L);
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(502L)).thenReturn(List.of(
                scheduleOrderProcess(603L, 502L, 300L, 1,
                        MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(),
                        new BigDecimal("5"), null, null, false),
                scheduleOrderProcess(604L, 502L, 301L, 2,
                        MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(),
                        null, new BigDecimal("3"), new BigDecimal("30"), false)));
        MesProAutoSchedulePreviewReqVO reqVO = req();

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());
        MesProAutoScheduleApplyRespVO response = autoScheduleService.apply(reqVO);

        assertTrue(response.getApplied());
        assertEquals(1, response.getSummary().getBlockingIssueCount());
        assertEquals(2, response.getSummary().getGeneratedTaskCount());
        assertEquals(2, response.getCreatedTaskIds().size());
        ArgumentCaptor<MesProTaskDO> taskCaptor = ArgumentCaptor.forClass(MesProTaskDO.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).insert(taskCaptor.capture());
        assertTrue(taskCaptor.getAllValues().stream().allMatch(task -> Long.valueOf(2L).equals(task.getWorkOrderId())));

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<MesProScheduleIssueDO>> issueCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(scheduleIssueMapper, org.mockito.Mockito.atLeastOnce()).insertBatch(issueCaptor.capture());
        List<MesProScheduleIssueDO> insertedIssues = issueCaptor.getAllValues().stream()
                .flatMap(List::stream)
                .toList();
        assertTrue(insertedIssues.stream().anyMatch(issue -> Long.valueOf(1L).equals(issue.getWorkOrderId())
                && "BLOCKING".equals(issue.getSeverity())
                && issue.getMessage().contains("工单未配置工艺路线")));
    }

    @Test
    void preview_shouldIgnoreCanceledHistoryTasksWhenDetectingProtectedTaskConflict() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                existingTask(7001L, 1L, 300L, MesProTaskStatusEnum.CANCELED.getStatus(),
                        LocalDateTime.of(2026, 5, 10, 8, 0)),
                existingTask(7002L, 1L, 300L, MesProTaskStatusEnum.CANCELED.getStatus(),
                        LocalDateTime.of(2026, 5, 10, 9, 0))));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(
                req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0)));

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), () -> String.valueOf(preview.getIssues()));
        assertFalse(preview.getIssues().stream()
                .anyMatch(issue -> "PROTECTED_TASK".equals(issue.getIssueType())
                        || "同一工单工序存在多个受保护任务".equals(issue.getMessage())));
        assertEquals(2, preview.getSummary().getGeneratedTaskCount());
        assertEquals(1, processTasks(preview, 1L, 300L).size());
    }

    @Test
    void preview_shouldStillBlockMultipleFinishedHistoryTasksAsProtectedConflict() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                existingTask(7101L, 1L, 300L, MesProTaskStatusEnum.FINISHED.getStatus(),
                        LocalDateTime.of(2026, 5, 10, 8, 0)),
                existingTask(7102L, 1L, 300L, MesProTaskStatusEnum.FINISHED.getStatus(),
                        LocalDateTime.of(2026, 5, 10, 9, 0))));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(
                req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0)));

        assertEquals(1, preview.getSummary().getBlockingIssueCount(), () -> String.valueOf(preview.getIssues()));
        assertTrue(preview.getIssues().stream()
                .anyMatch(issue -> "PROTECTED_TASK".equals(issue.getIssueType())
                        && "同一工单工序存在多个受保护任务".equals(issue.getMessage())));
        assertEquals(0, preview.getSummary().getGeneratedTaskCount());
    }

    @Test
    void preview_shouldIgnorePreparedTasksWhoseScheduleExtRowsWereSoftDeleted() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                existingTask(7201L, 1L, 300L, MesProTaskStatusEnum.PREPARE.getStatus(),
                        LocalDateTime.of(2026, 5, 10, 8, 0)),
                existingTask(7202L, 1L, 300L, MesProTaskStatusEnum.PREPARE.getStatus(),
                        LocalDateTime.of(2026, 5, 10, 9, 0))));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(
                req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0)));

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), () -> String.valueOf(preview.getIssues()));
        assertFalse(preview.getIssues().stream()
                .anyMatch(issue -> "PROTECTED_TASK".equals(issue.getIssueType())
                        || "同一工单工序存在多个受保护任务".equals(issue.getMessage())));
        assertEquals(2, preview.getSummary().getGeneratedTaskCount());
        assertEquals(1, processTasks(preview, 1L, 300L).size());
    }

    @Test
    void preview_shouldBlockCanceledSourceWorkOrderBeforeProtectedTaskConflict() {
        urgentWorkOrder.setStatus(MesProWorkOrderStatusEnum.CANCELED.getStatus());
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                existingTask(7301L, 1L, 300L, MesProTaskStatusEnum.FINISHED.getStatus(),
                        LocalDateTime.of(2026, 5, 10, 8, 0)),
                existingTask(7302L, 1L, 300L, MesProTaskStatusEnum.FINISHED.getStatus(),
                        LocalDateTime.of(2026, 5, 10, 9, 0))));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(
                req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0)));

        assertEquals(1, preview.getSummary().getBlockingIssueCount(), () -> String.valueOf(preview.getIssues()));
        assertTrue(preview.getIssues().stream()
                .anyMatch(issue -> "WORK_ORDER_STATUS".equals(issue.getIssueType())
                        && "生产工单已取消".equals(issue.getMessage())));
        assertFalse(preview.getIssues().stream()
                .anyMatch(issue -> "PROTECTED_TASK".equals(issue.getIssueType())
                        || "同一工单工序存在多个受保护任务".equals(issue.getMessage())));
        assertEquals(0, preview.getSummary().getGeneratedTaskCount());
    }

    @Test
    void apply_shouldRefreshAndPersistNightShiftFromProductRouteConfigBeforeScheduling() {
        urgentOrder = scheduleOrder(501L, 1L, LocalDate.of(2026, 5, 14), null);
        urgentOrder.setRouteVersionId(700L);
        MesProRouteProcessDO finiteRouteProcess = MesProRouteProcessDO.builder()
                .id(30L).routeId(200L).processId(300L).sort(1).workstationId(800L)
                .prepareTime(0).waitTime(0).colorCode("#1677ff").build();
        MesProRouteProcessDO formulaRouteProcess = MesProRouteProcessDO.builder()
                .id(31L).routeId(200L).processId(301L).sort(2).workstationId(801L)
                .prepareTime(0).waitTime(0).colorCode("#52c41a").build();
        MesMdWorkstationDO finiteWorkstation = MesMdWorkstationDO.builder()
                .id(800L).code("WS-800").name("P1").processId(300L).productionLineId(900L)
                .shiftHours(new BigDecimal("8")).status(0)
                .singleStandardHourlyCapacity(new BigDecimal("5")).build();
        MesMdWorkstationDO formulaWorkstation = MesMdWorkstationDO.builder()
                .id(801L).code("WS-801").name("P2").processId(301L).productionLineId(900L)
                .shiftHours(new BigDecimal("8")).status(0)
                .singleStandardHourlyCapacity(BigDecimal.ONE).build();
        when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L))
                .thenAnswer(invocation -> {
                    MesProScheduleOrderProcessDO finiteProcess = scheduleOrderProcess(
                            601L, 501L, 300L, 1, 30L, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(),
                            new BigDecimal("5"), null, null, false);
                    finiteProcess.setShiftHours(new BigDecimal("8"));
                    MesProScheduleOrderProcessDO formulaProcess = scheduleOrderProcess(
                            602L, 501L, 301L, 2, 31L, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(),
                            null, new BigDecimal("3"), new BigDecimal("30"), false);
                    formulaProcess.setShiftHours(new BigDecimal("8"));
                    return List.of(finiteProcess, formulaProcess);
                });
        when(routeProcessService.getRouteProcessListByRouteId(200L))
                .thenReturn(List.of(finiteRouteProcess, formulaRouteProcess));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                200L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(List.of(
                MesProRouteFlowProcessConfigDO.builder()
                        .id(2101L)
                        .routeFlowConfigId(21L)
                        .routeId(200L)
                        .routeProcessId(30L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .productionQuantityFactor(BigDecimal.ONE)
                        .build(),
                MesProRouteFlowProcessConfigDO.builder()
                        .id(2102L)
                        .routeFlowConfigId(21L)
                        .routeId(200L)
                        .routeProcessId(31L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .productionQuantityFactor(BigDecimal.ONE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(700L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(9001L).routeVersionId(700L).itemId(100L).routeProcessId(30L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("5")).nightShiftEnabled(Boolean.TRUE).calendarRuleId(1L).build(),
                MesProRouteScheduleConfigDO.builder()
                        .id(9002L).routeVersionId(700L).itemId(100L).routeProcessId(31L)
                        .capacityMode(MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode())
                        .infiniteDurationQuantityFactor(new BigDecimal("3"))
                        .infiniteDurationBaseMinutes(new BigDecimal("30"))
                        .nightShiftEnabled(Boolean.FALSE).build()));
        when(workstationMapper.selectByIds(List.of(800L, 801L)))
                .thenReturn(List.of(finiteWorkstation, formulaWorkstation));
        when(workstationCapacityService.getCapacityMetricsUsingShiftHours(
                List.of(formulaWorkstation))).thenReturn(Map.of(
                        801L, MesMdWorkstationCapacityMetrics.builder()
                                .configuredWorkerCount(1).currentWorkerCount(1)
                                .todayCapacity(new BigDecimal("8")).build()));
        when(scheduleCalendarRuleMapper.selectById(1L)).thenReturn(MesProScheduleCalendarRuleDO.builder()
                .id(1L)
                .skipStatutoryHolidays(false)
                .weekendRestMode("SINGLE")
                .dateShiftModeByDateJson("{}")
                .temporaryFreezeEnabled(false)
                .build());
        when(workOrderService.getWorkOrderList(List.of(1L))).thenReturn(List.of(urgentWorkOrder));
        MesProAutoSchedulePreviewReqVO reqVO = req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);
        assertEquals(1, preview.getTasks().size());
        GanttDataRespVO previewTask = preview.getTasks().get(0);
        assertEquals(Integer.valueOf(301), previewTask.getType());
        assertNull(previewTask.getParent());
        assertEquals("301_1", previewTask.getId());
        assertEquals(new BigDecimal("10"), previewTask.getQuantity());
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());
        autoScheduleService.apply(reqVO);

        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor = ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper, atLeastOnce()).updateById(processCaptor.capture());
        MesProScheduleOrderProcessDO refreshedProcess = processCaptor.getAllValues().stream()
                .filter(process -> Long.valueOf(601L).equals(process.getId()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertEquals(9001L, refreshedProcess.getRouteScheduleConfigId());
        assertEquals(Boolean.TRUE, refreshedProcess.getNightShiftEnabled());
        assertEquals(1L, refreshedProcess.getCalendarRuleId());
    }

    @Test
    void apply_shouldPrioritizeNightShiftScheduleOrderBeforeNormalOrder() {
        laterOrder = scheduleOrder(502L, 2L, LocalDate.of(2026, 5, 15), 1);
        urgentOrder = scheduleOrder(501L, 1L, LocalDate.of(2026, 5, 20), 9);
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(502L, 501L)))
                .thenReturn(List.of(laterOrder, urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(),
                        new BigDecimal("5"), null, null, true),
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(),
                        null, new BigDecimal("3"), new BigDecimal("30"), true)));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(502L)).thenReturn(List.of(
                scheduleOrderProcess(603L, 502L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(),
                        new BigDecimal("5"), null, null, false),
                scheduleOrderProcess(604L, 502L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(),
                        null, new BigDecimal("3"), new BigDecimal("30"), false)));

        MesProAutoSchedulePreviewReqVO reqVO = req();
        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        autoScheduleService.apply(reqVO);

        ArgumentCaptor<MesProTaskDO> taskCaptor = ArgumentCaptor.forClass(MesProTaskDO.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).insert(taskCaptor.capture());
        List<MesProTaskDO> tasks = taskCaptor.getAllValues();
        assertEquals(4, tasks.size());
        assertEquals(1L, tasks.get(0).getWorkOrderId());
        assertEquals(LocalDateTime.of(2026, 5, 13, 20, 0), tasks.get(0).getStartTime());
        assertEquals(2L, tasks.get(2).getWorkOrderId());
        assertTrue(tasks.get(2).getStartTime().isAfter(tasks.get(0).getStartTime()));
    }

    @Test
    void preview_shouldAllowNightShiftProcessToSpanDayAndNightWithoutRequiringBoth() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("2"), null, null, true),
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), null, new BigDecimal("0"), new BigDecimal("30"), false)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        MesProAutoSchedulePreviewReqVO reqVO = req(List.of(501L), LocalDateTime.of(2026, 5, 13, 15, 0));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);

        List<GanttDataRespVO> finiteTasks = processTasks(preview, 1L, 300L);
        assertEquals(1, finiteTasks.size());
        assertEquals(LocalDateTime.of(2026, 5, 13, 20, 0), finiteTasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 14, 1, 0), finiteTasks.get(0).getEndDate());
    }

    @Test
    void preview_shouldKeepDayShiftOnlyWhenNightShiftDisabled() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("5"), null, null, false),
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), null, new BigDecimal("3"), new BigDecimal("30"), false)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        MesProAutoSchedulePreviewReqVO reqVO = req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);

        List<GanttDataRespVO> finiteTasks = processTasks(preview, 1L, 300L);
        List<GanttDataRespVO> infiniteTasks = processTasks(preview, 1L, 301L);
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), finiteTasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 10, 0), finiteTasks.get(0).getEndDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), infiniteTasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 9, 0), infiniteTasks.get(0).getEndDate());
    }

    @Test
    void preview_shouldUseScheduleProcessCalendarRuleInsteadOfGlobalCalendarWhenConfigured() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(),
                        new BigDecimal("5"), null, null, false, 900L),
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(),
                        null, new BigDecimal("0"), new BigDecimal("30"), false, 900L)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        MesProScheduleCalendarRulesRespVO globalRules = calendarRules();
        globalRules.setDateShiftModeByDate(Map.of("2026-05-13", "REST"));
        lenient().when(scheduleCalendarService.getRules()).thenReturn(globalRules);
        when(scheduleCalendarRuleMapper.selectById(900L)).thenReturn(MesProScheduleCalendarRuleDO.builder()
                .id(900L)
                .skipStatutoryHolidays(Boolean.FALSE)
                .weekendRestMode("NONE")
                .dateShiftModeByDateJson("{\"2026-05-13\":\"DAY\"}")
                .build());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0)));

        List<GanttDataRespVO> tasks = preview.getTasks().stream()
                .filter(task -> Integer.valueOf(303).equals(task.getType()))
                .toList();
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), tasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 10, 0), tasks.get(0).getEndDate());
    }

    @Test
    void preview_shouldUseScheduleOrderProcessHourlyCapacityWhenResourcePoolCapacityIsZero() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("5"), null, null, false),
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), null, new BigDecimal("0"), new BigDecimal("30"), false)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        lenient().when(workstationCapacityService.getCapacityMetrics(any(), any())).thenReturn(Map.of(
                800L, MesMdWorkstationCapacityMetrics.builder().configuredWorkerCount(1).currentWorkerCount(1).todayCapacity(BigDecimal.ZERO).build(),
                801L, MesMdWorkstationCapacityMetrics.builder().configuredWorkerCount(1).currentWorkerCount(1).todayCapacity(BigDecimal.ONE).build()));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0)));

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), () -> String.valueOf(preview.getIssues()));
        List<GanttDataRespVO> tasks = preview.getTasks().stream()
                .filter(task -> Integer.valueOf(303).equals(task.getType()))
                .toList();
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), tasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 10, 0), tasks.get(0).getEndDate());
    }

    @Test
    void preview_shouldMatchScheduleOrderProcessByRouteSortWhenRouteProcessIdDrifted() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(routeProcessService.getRouteProcessListByRouteId(200L)).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(9300L).routeId(200L).processId(3300L).sort(1)
                        .prepareTime(0).waitTime(0).colorCode("#1677ff").build(),
                MesProRouteProcessDO.builder().id(9301L).routeId(200L).processId(3301L).sort(2)
                        .prepareTime(0).waitTime(0).colorCode("#52c41a").build()));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(),
                        new BigDecimal("5"), null, null, false),
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(),
                        null, new BigDecimal("0"), new BigDecimal("30"), false)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        lenient().when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(800L).name("P1").processId(3300L).productionLineId(900L)
                        .status(0).singleStandardHourlyCapacity(BigDecimal.ZERO).build(),
                MesMdWorkstationDO.builder().id(801L).name("P2").processId(3301L).productionLineId(900L)
                        .status(0).singleStandardHourlyCapacity(BigDecimal.ZERO).build()));
        lenient().when(workstationCapacityService.getCapacityMetrics(any(), any())).thenReturn(Map.of(
                800L, MesMdWorkstationCapacityMetrics.builder().configuredWorkerCount(1).currentWorkerCount(1).todayCapacity(BigDecimal.ZERO).build(),
                801L, MesMdWorkstationCapacityMetrics.builder().configuredWorkerCount(1).currentWorkerCount(1).todayCapacity(BigDecimal.ONE).build()));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(
                req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0)));

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), () -> String.valueOf(preview.getIssues()));
        List<GanttDataRespVO> tasks = preview.getTasks().stream()
                .filter(task -> Integer.valueOf(303).equals(task.getType()))
                .toList();
        assertEquals(2, tasks.size());
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), tasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 10, 0), tasks.get(0).getEndDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), tasks.get(1).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 30), tasks.get(1).getEndDate());
    }

    @Test
    void preview_shouldScheduleRemainingSnapshotProcessWhenCurrentRouteRemovedIt() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(routeProcessService.getRouteProcessListByRouteId(200L)).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(9300L).routeId(200L).processId(300L).sort(1)
                        .prepareTime(0).waitTime(0).colorCode("#1677ff").build()));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(),
                        new BigDecimal("5"), null, null, false),
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(),
                        new BigDecimal("5"), null, null, false)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));
        lenient().when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(800L).name("P1").processId(300L).productionLineId(900L)
                        .status(0).singleStandardHourlyCapacity(BigDecimal.ONE).build(),
                MesMdWorkstationDO.builder().id(801L).name("P2").processId(301L).productionLineId(900L)
                        .status(0).singleStandardHourlyCapacity(BigDecimal.ONE).build()));
        lenient().when(workstationCapacityService.getCapacityMetrics(any(), any())).thenReturn(Map.of(
                800L, MesMdWorkstationCapacityMetrics.builder().configuredWorkerCount(1).currentWorkerCount(1).todayCapacity(BigDecimal.ONE).build(),
                801L, MesMdWorkstationCapacityMetrics.builder().configuredWorkerCount(1).currentWorkerCount(1).todayCapacity(BigDecimal.ONE).build()));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(
                req(List.of(501L), LocalDateTime.of(2026, 5, 13, 8, 0)));

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), () -> String.valueOf(preview.getIssues()));
        assertEquals(2, preview.getSummary().getGeneratedTaskCount());
        List<GanttDataRespVO> tasks = preview.getTasks().stream()
                .filter(task -> Integer.valueOf(303).equals(task.getType()))
                .toList();
        assertEquals(2, tasks.size());
        assertFalse(preview.getIssues().stream().anyMatch(issue -> "ACTIVE_TASK".equals(issue.getIssueType())));
    }

    @Test
    void preview_shouldSplitFiniteDayShiftProcessAcrossShiftWindows() {
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                scheduleOrderProcess(601L, 501L, 300L, 1, MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("5"), null, null, false),
                scheduleOrderProcess(602L, 501L, 301L, 2, MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), null, new BigDecimal("0"), new BigDecimal("30"), false)));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(req(List.of(501L), LocalDateTime.of(2026, 5, 13, 15, 0)));

        List<GanttDataRespVO> finiteTasks = processTasks(preview, 1L, 300L);
        List<GanttDataRespVO> infiniteTasks = processTasks(preview, 1L, 301L);
        assertEquals(LocalDateTime.of(2026, 5, 13, 15, 0), finiteTasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 16, 0), finiteTasks.get(0).getEndDate());
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0), finiteTasks.get(1).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 14, 9, 0), finiteTasks.get(1).getEndDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 15, 0), infiniteTasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 15, 30), infiniteTasks.get(0).getEndDate());
    }

    @Test
    void preview_shouldNotDelayProcessByScheduleProcessPlannedStartTime() {
        MesProScheduleOrderProcessDO finiteProcess = scheduleOrderProcess(601L, 501L, 300L, 1,
                MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), new BigDecimal("5"), null, null, false);
        finiteProcess.setPlannedStartTime(LocalDateTime.of(2026, 5, 14, 8, 0));
        MesProScheduleOrderProcessDO formulaProcess = scheduleOrderProcess(602L, 501L, 301L, 2,
                MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), null, new BigDecimal("3"), new BigDecimal("30"), false);
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(urgentOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L))
                .thenReturn(List.of(finiteProcess, formulaProcess));
        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(urgentWorkOrder));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(req(List.of(501L),
                LocalDateTime.of(2026, 5, 13, 8, 0)));

        List<GanttDataRespVO> finiteTasks = processTasks(preview, 1L, 300L);
        assertEquals(1, finiteTasks.size());
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), finiteTasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 13, 10, 0), finiteTasks.get(0).getEndDate());
    }

    @Test
    void calculateRequiredProcessMinutes_shouldApplyWorkerEfficiencyOnlyToResourceCalculatedDuration() {
        routeVersionMapper.selectActiveByRouteId(200L);
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .prepareTime(0)
                .waitTime(0)
                .build();
        MesProScheduleOrderProcessDO resourceCalculated = MesProScheduleOrderProcessDO.builder()
                .capacityMode(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode())
                .build();
        MesProScheduleOrderProcessDO manualOverride = MesProScheduleOrderProcessDO.builder()
                .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                .build();
        MesProScheduleOrderProcessDO infiniteFormula = MesProScheduleOrderProcessDO.builder()
                .capacityMode(MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode())
                .infiniteDurationQuantityFactor(new BigDecimal("3"))
                .infiniteDurationBaseMinutes(new BigDecimal("30"))
                .build();

        Integer durationAtThirtyPerHour = ReflectionTestUtils.invokeMethod(autoScheduleService,
                "calculateRequiredProcessMinutes", new BigDecimal("30"), routeProcess,
                new BigDecimal("30"), resourceCalculated);
        Integer durationAtFifteenPerHour = ReflectionTestUtils.invokeMethod(autoScheduleService,
                "calculateRequiredProcessMinutes", new BigDecimal("30"), routeProcess,
                new BigDecimal("15"), resourceCalculated);
        Integer manualBefore = ReflectionTestUtils.invokeMethod(autoScheduleService,
                "calculateRequiredProcessMinutes", new BigDecimal("30"), routeProcess,
                new BigDecimal("12"), manualOverride);
        Integer manualAfter = ReflectionTestUtils.invokeMethod(autoScheduleService,
                "calculateRequiredProcessMinutes", new BigDecimal("30"), routeProcess,
                new BigDecimal("12"), manualOverride);
        Integer formulaBefore = ReflectionTestUtils.invokeMethod(autoScheduleService,
                "calculateRequiredProcessMinutes", new BigDecimal("30"), routeProcess,
                new BigDecimal("30"), infiniteFormula);
        Integer formulaAfter = ReflectionTestUtils.invokeMethod(autoScheduleService,
                "calculateRequiredProcessMinutes", new BigDecimal("30"), routeProcess,
                new BigDecimal("15"), infiniteFormula);

        assertEquals(60, durationAtThirtyPerHour);
        assertEquals(120, durationAtFifteenPerHour);
        assertEquals(manualBefore, manualAfter);
        assertEquals(150, manualAfter);
        assertEquals(formulaBefore, formulaAfter);
        assertEquals(120, formulaAfter);
    }

    private MesProAutoSchedulePreviewReqVO req() {
        return req(List.of(502L, 501L), LocalDateTime.of(2026, 5, 13, 8, 0));
    }

    private MesProAutoSchedulePreviewReqVO req(List<Long> scheduleOrderIds, LocalDateTime startTime) {
        MesProAutoSchedulePreviewReqVO reqVO = new MesProAutoSchedulePreviewReqVO();
        reqVO.setScheduleOrderIds(scheduleOrderIds);
        reqVO.setStartTime(startTime);
        reqVO.setRuntimeCapacityBasis("PLANNED");
        reqVO.setPreserveManualLockedTasks(true);
        reqVO.setReason("业务原因：自动排产发布");
        return reqVO;
    }

    private List<GanttDataRespVO> processTasks(MesProAutoSchedulePreviewRespVO preview, Long workOrderId) {
        return preview.getTasks().stream()
                .filter(task -> Integer.valueOf(303).equals(task.getType()))
                .filter(task -> ("301_" + workOrderId).equals(task.getParent()))
                .sorted(Comparator
                        .comparing(GanttDataRespVO::getStartDate)
                        .thenComparing(GanttDataRespVO::getEndDate))
                .toList();
    }

    private List<GanttDataRespVO> processTasks(MesProAutoSchedulePreviewRespVO preview, Long workOrderId,
                                               Long processId) {
        String idPrefix = "303_preview_" + workOrderId + "_" + processId + "_";
        return processTasks(preview, workOrderId).stream()
                .filter(task -> task.getId() != null && task.getId().startsWith(idPrefix))
                .toList();
    }

    private MesProScheduleOrderDO scheduleOrder(Long id, Long workOrderId, LocalDate promiseDate, Integer priorityNo) {
        return MesProScheduleOrderDO.builder()
                .id(id).workOrderId(workOrderId).productId(100L).quantity(new BigDecimal("10"))
                .routeId(200L)
                .promiseDate(promiseDate).priorityNo(priorityNo)
                .routeVersionId(700L)
                .routeStatus(MesProScheduleOrderRouteStatusEnum.READY.getStatus())
                .autoSchedulable(Boolean.TRUE).build();
    }

    private MesProScheduleOrderProcessDO scheduleOrderProcess(Long id, Long scheduleOrderId, Long processId, Integer sort,
            String capacityMode, BigDecimal hourlyCapacity, BigDecimal quantityFactor, BigDecimal baseMinutes,
            Boolean nightShiftEnabled) {
        return scheduleOrderProcess(id, scheduleOrderId, processId, sort, capacityMode, hourlyCapacity, quantityFactor,
                baseMinutes, nightShiftEnabled, null);
    }

    private MesProScheduleOrderProcessDO scheduleOrderProcess(Long id, Long scheduleOrderId, Long processId, Integer sort,
            String capacityMode, BigDecimal hourlyCapacity, BigDecimal quantityFactor, BigDecimal baseMinutes,
            Boolean nightShiftEnabled, Long calendarRuleId) {
        return scheduleOrderProcess(id, scheduleOrderId, processId, sort, null, capacityMode, hourlyCapacity,
                quantityFactor, baseMinutes, nightShiftEnabled, calendarRuleId);
    }

    private MesProScheduleOrderProcessDO scheduleOrderProcess(Long id, Long scheduleOrderId, Long processId, Integer sort,
            Long routeProcessId, String capacityMode, BigDecimal hourlyCapacity, BigDecimal quantityFactor,
            BigDecimal baseMinutes, Boolean nightShiftEnabled) {
        return scheduleOrderProcess(id, scheduleOrderId, processId, sort, routeProcessId, capacityMode, hourlyCapacity,
                quantityFactor, baseMinutes, nightShiftEnabled, null);
    }

    private MesProScheduleOrderProcessDO scheduleOrderProcess(Long id, Long scheduleOrderId, Long processId, Integer sort,
            Long routeProcessId, String capacityMode, BigDecimal hourlyCapacity, BigDecimal quantityFactor,
            BigDecimal baseMinutes, Boolean nightShiftEnabled, Long calendarRuleId) {
        Long resolvedRouteProcessId = routeProcessId != null ? routeProcessId : defaultRouteProcessId(sort);
        return MesProScheduleOrderProcessDO.builder()
                .id(id).scheduleOrderId(scheduleOrderId).routeVersionId(700L)
                .routeProcessId(resolvedRouteProcessId).processId(processId).sort(sort).enabled(Boolean.TRUE)
                .capacityMode(capacityMode).hourlyCapacityTotal(hourlyCapacity)
                .infiniteDurationQuantityFactor(quantityFactor).infiniteDurationBaseMinutes(baseMinutes)
                .plannedQuantity(new BigDecimal("10")).remainingQuantity(new BigDecimal("10"))
                .nightShiftEnabled(nightShiftEnabled).calendarRuleId(calendarRuleId).build();
    }

    private MesProTaskDO existingTask(Long id, Long workOrderId, Long processId, Integer status,
                                      LocalDateTime updateTime) {
        MesProTaskDO task = MesProTaskDO.builder()
                .id(id)
                .code("PT-" + id)
                .name("历史任务-" + id)
                .workOrderId(workOrderId)
                .routeId(200L)
                .processId(processId)
                .itemId(100L)
                .quantity(new BigDecimal("10"))
                .status(status)
                .build();
        task.setUpdateTime(updateTime);
        return task;
    }

    private Long defaultRouteProcessId(Integer sort) {
        if (sort == null) {
            return null;
        }
        return 29L + sort;
    }

    private MesProScheduleCalendarRulesRespVO calendarRules() {
        MesProScheduleCalendarRulesRespVO rules = new MesProScheduleCalendarRulesRespVO();
        rules.setSkipStatutoryHolidays(false);
        rules.setWeekendRestMode("SINGLE");
        rules.setDateShiftModeByDate(new LinkedHashMap<>());
        rules.setSimulationCurrentDate("2026-05-13");
        return rules;
    }

    private List<MesProCapacityPlanDO> capacityPlans() {
        return List.of(
                capacityPlan(900L, LocalDateTime.of(2026, 5, 13, 0, 0), 701L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 13, 0, 0), 702L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 14, 0, 0), 701L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 14, 0, 0), 702L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 15, 0, 0), 701L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 15, 0, 0), 702L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 16, 0, 0), 701L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 16, 0, 0), 702L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 18, 0, 0), 701L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 18, 0, 0), 702L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 19, 0, 0), 701L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 19, 0, 0), 702L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 20, 0, 0), 701L),
                capacityPlan(900L, LocalDateTime.of(2026, 5, 20, 0, 0), 702L));
    }

    private MesProScheduleOrderPreflightRespVO passPreflightResp() {
        MesProScheduleOrderPreflightRespVO respVO = new MesProScheduleOrderPreflightRespVO();
        respVO.setResult("PASS");
        respVO.setSummary(new MesProScheduleOrderPreflightSummaryRespVO());
        return respVO;
    }

    private MesProScheduleOrderPreflightRespVO blockedPreflightResp(String message) {
        MesProScheduleOrderPreflightRespVO respVO = new MesProScheduleOrderPreflightRespVO();
        MesProScheduleOrderPreflightSummaryRespVO summary = new MesProScheduleOrderPreflightSummaryRespVO();
        summary.setBlockedCount(1);
        respVO.setResult("BLOCKED");
        respVO.setSummary(summary);
        var issue = new cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightIssueRespVO();
        issue.setSeverity("BLOCKED");
        issue.setMessage(message);
        respVO.setIssues(List.of(issue));
        return respVO;
    }

    private MesProScheduleOrderPreflightRespVO erpSourceWarningPreflightResp() {
        MesProScheduleOrderPreflightRespVO respVO = new MesProScheduleOrderPreflightRespVO();
        MesProScheduleOrderPreflightSummaryRespVO summary = new MesProScheduleOrderPreflightSummaryRespVO();
        summary.setWarnCount(1);
        respVO.setResult("WARN");
        respVO.setSummary(summary);
        var issue = new cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightIssueRespVO();
        issue.setReasonCode("WARN_ERP_SYNC_RECORD_MISSING");
        issue.setSeverity("WARN");
        issue.setMessage("未找到生产工单的 ERP 正式同步记录或正式 ID/编号");
        respVO.setIssues(List.of(issue));
        return respVO;
    }

    private MesProCapacityPlanDO capacityPlan(Long lineId, LocalDateTime date, Long shiftId) {
        return MesProCapacityPlanDO.builder()
                .lineId(lineId).calendarDate(date).shiftId(shiftId).capacityMinutes(480).enabled(Boolean.TRUE).build();
    }
}
