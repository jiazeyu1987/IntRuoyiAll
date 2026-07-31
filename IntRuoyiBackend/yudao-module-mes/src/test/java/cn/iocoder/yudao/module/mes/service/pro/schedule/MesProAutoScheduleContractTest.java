package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoSchedulePreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoSchedulePreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleReplanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarRulesRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightSummaryRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityActualMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskDependencyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderRouteStatusEnum;
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
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_ORDER_NOT_SCHEDULABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_SCOPE_EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProAutoScheduleContractTest {

    @InjectMocks
    private MesProAutoScheduleServiceImpl autoScheduleService;

    @Spy
    private ScheduleTopologyResolver scheduleTopologyResolver = new ScheduleTopologyResolver();

    @Spy
    private ScheduleApplyGuard scheduleApplyGuard = new ScheduleApplyGuard();

    @Spy
    private ScheduleDefaultCompatibilityPolicy scheduleDefaultCompatibilityPolicy = new ScheduleDefaultCompatibilityPolicy();

    @Spy
    private ScheduleInputAssembler scheduleInputAssembler =
            new ScheduleInputAssembler(scheduleDefaultCompatibilityPolicy);

    @Spy
    private CapacityWindowAllocator capacityWindowAllocator = new CapacityWindowAllocator();

    @Spy
    private SchedulePlanner schedulePlanner = new SchedulePlanner();

    @InjectMocks
    private ScheduleApplier scheduleApplier;

    private RouteSnapshotResolver routeSnapshotResolver;

    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;
    @Mock
    private MesProTaskMapper taskMapper;
    @Mock
    private MesMdWorkstationMapper workstationMapper;
    @Mock
    private MesWmMaterialStockMapper materialStockMapper;
    @Mock
    private MesProRouteProductService routeProductService;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProScheduleOrderService scheduleOrderService;
    @Mock
    private MesMdProductionLineService productionLineService;
    @Mock
    private MesMdWorkstationCapacityService workstationCapacityService;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;
    @Mock
    private MesCalPlanService planService;
    @Mock
    private MesCalPlanShiftService planShiftService;
    @Mock
    private MesCalHolidayService holidayService;
    @Mock
    private MesProScheduleCalendarService scheduleCalendarService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Mock
    private MesProCapacityPlanMapper capacityPlanMapper;
    @Mock
    private MesProCapacityActualMapper capacityActualMapper;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock
    private MesProTaskDependencyMapper taskDependencyMapper;
    @Mock
    private MesProScheduleIssueMapper scheduleIssueMapper;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProBatchRecordExecutionMapper batchRecordExecutionMapper;
    @Mock
    private MesKingdeeProductionMaterialListSyncService productionMaterialListSyncService;

    private MesProWorkOrderDO workOrder;
    private MesProScheduleOrderDO scheduleOrder;
    private MesProScheduleOrderProcessDO scheduleOrderProcess;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(routeProcessService.getProcessIdentityMap(
                        org.mockito.ArgumentMatchers.anyCollection()))
                .thenAnswer(invocation -> identityMap(invocation.getArgument(0)));
        lenient().when(routeProcessFlowEdgeMapper.selectListByRouteId(anyLong())).thenAnswer(invocation -> {
            Long routeId = invocation.getArgument(0);
            if (Objects.equals(routeId, 20L)) {
                return List.of(routeEdge(3L, 4L, 1));
            }
            if (Objects.equals(routeId, 200L)) {
                return List.of(routeEdge(30L, 31L, 1));
            }
            return Collections.emptyList();
        });
        routeSnapshotResolver = new RouteSnapshotResolver(routeProcessService, routeProcessFlowEdgeMapper);
        ReflectionTestUtils.setField(autoScheduleService, "routeSnapshotResolver", routeSnapshotResolver);
        ReflectionTestUtils.setField(autoScheduleService, "schedulePlanner", schedulePlanner);
        ReflectionTestUtils.setField(autoScheduleService, "scheduleApplier", scheduleApplier);
        lenient().when(routeService.getRouteMapIgnoreDeleted(any())).thenReturn(Collections.emptyMap());
        lenient().when(routeScheduleConfigMapper.selectListByRouteVersionId(anyLong()))
                .thenReturn(Collections.<MesProRouteScheduleConfigDO>emptyList());
        org.mockito.Mockito.lenient().when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO
                        .builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(0))
                        .useType(cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build());
        workOrder = MesProWorkOrderDO.builder()
                .id(1L)
                .code("WO-001")
                .productId(100L)
                .quantity(BigDecimal.ONE)
                .clientId(10L)
                .build();
        scheduleOrder = MesProScheduleOrderDO.builder()
                .id(501L)
                .workOrderId(1L)
                .routeId(20L)
                .productId(100L)
                .quantity(BigDecimal.ONE)
                .promiseDate(LocalDate.of(2026, 5, 20))
                .priorityNo(20)
                .routeVersionId(700L)
                .routeStatus(MesProScheduleOrderRouteStatusEnum.READY.getStatus())
                .autoSchedulable(Boolean.TRUE)
                .build();
        scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(601L)
                .scheduleOrderId(501L)
                .processId(300L)
                .sort(1)
                .build();

        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(workOrder));
        lenient().when(workOrderService.getWorkOrderMap(any())).thenReturn(Map.of(workOrder.getId(), workOrder));
        lenient().when(routeProductService.getRouteProductByItemId(100L)).thenReturn(MesProRouteProductDO.builder()
                .id(2L).routeId(20L).itemId(100L).quantity(1).productionTime(new BigDecimal("8"))
                .timeUnitType(MesTimeUnitTypeEnum.HOUR.getType()).build());
        lenient().when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(MesProRouteProcessDO.builder()
                .id(3L).routeId(20L).processId(300L).sort(1).prepareTime(0).waitTime(0).colorCode("#1677ff").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(100L);
        productItem.setCode("ITEM-PROD");
        productItem.setName("成品A");
        lenient().when(itemService.getItemMap(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            Map<Long, MesMdItemDO> map = new LinkedHashMap<>();
            for (Long id : ids) {
                if (id == null) {
                    continue;
                }
                if (Long.valueOf(100L).equals(id)) {
                    map.put(100L, productItem);
                    continue;
                }
                MesMdItemDO materialItem = new MesMdItemDO();
                materialItem.setId(id);
                materialItem.setCode("ITEM-" + id);
                materialItem.setName("物料" + id);
                map.put(id, materialItem);
            }
            return map;
        });
        lenient().when(processService.getProcessMap(any())).thenReturn(Map.of());
        lenient().when(taskScheduleExtMapper.selectListByTaskIds(any())).thenReturn(Collections.emptyList());
        lenient().when(feedbackMapper.selectListByTaskIds(any())).thenReturn(Collections.emptyList());
        lenient().when(batchRecordExecutionMapper.selectListByTaskIds(any())).thenReturn(Collections.emptyList());
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(any())).thenReturn(Collections.emptyList());
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        lenient().when(taskMapper.selectListByWorkstationIds(any())).thenReturn(Collections.emptyList());
        lenient().when(capacityActualMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(Collections.emptyList());
        lenient().when(holidayService.getHolidayByDay(any())).thenReturn(null);
        lenient().when(holidayService.getHolidayList(any(), any())).thenReturn(Collections.emptyList());
        lenient().when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                MesKingdeeProductionMaterialListDO.builder()
                        .id(1L)
                        .workOrderId(1L)
                        .childMaterialId(901L)
                        .childMaterialCode("MAT-901")
                        .requiredQuantity(BigDecimal.ONE)
                        .build()
        ));
        lenient().when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, MesMdWorkstationCapacityMetrics.builder()
                        .configuredWorkerCount(1).currentWorkerCount(1).machineryStandardHourlyCapacity(BigDecimal.ZERO)
                        .todayCapacity(BigDecimal.ONE).build()));
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

    private MesProScheduleOrderPreflightRespVO passPreflightResp() {
        MesProScheduleOrderPreflightRespVO respVO = new MesProScheduleOrderPreflightRespVO();
        respVO.setResult("PASS");
        respVO.setSummary(new MesProScheduleOrderPreflightSummaryRespVO());
        return respVO;
    }

    @Test
    void preview_shouldRejectWorkOrderIdsOnlyAndNeverResolveScheduleOrderFallback() {
        MesProAutoSchedulePreviewReqVO reqVO = buildReq();
        reqVO.setWorkOrderIds(List.of(1L));
        reqVO.setScheduleOrderIds(null);

        ServiceException exception = assertThrows(ServiceException.class, () -> autoScheduleService.preview(reqVO));

        assertEquals(PRO_AUTO_SCHEDULE_SCOPE_EMPTY.getCode(), exception.getCode());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
    }

    @Test
    void replanPreview_shouldRejectWorkOrderIdsOnlyAndNeverResolveScheduleOrderFallback() {
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setWorkOrderIds(List.of(1L));
        reqVO.setScheduleOrderIds(null);

        ServiceException exception = assertThrows(ServiceException.class, () -> autoScheduleService.replanPreview(reqVO));

        assertEquals(PRO_AUTO_SCHEDULE_SCOPE_EMPTY.getCode(), exception.getCode());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
    }

    @Test
    void preview_shouldFailFastWhenRequestedScheduleOrderIsMissingFromAutoSchedulableScope() {
        when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L, 502L))).thenReturn(List.of(scheduleOrder));

        MesProAutoSchedulePreviewReqVO reqVO = buildReq();
        reqVO.setWorkOrderIds(null);
        reqVO.setScheduleOrderIds(List.of(501L, 502L));
        ServiceException exception = assertThrows(ServiceException.class,
                () -> autoScheduleService.preview(reqVO));

        assertEquals(PRO_AUTO_SCHEDULE_ORDER_NOT_SCHEDULABLE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("ID=502"));
        assertTrue(exception.getMessage().contains("不存在或已删除"));
    }

    @Test
    void preview_shouldReturnServiceExceptionWithScheduleOrderCodeWhenRequestedOrderIsNotSchedulable() {
        MesProScheduleOrderDO frozenOrder = MesProScheduleOrderDO.builder()
                .id(502L)
                .code("SCH-ERR-502")
                .workOrderId(2L)
                .productId(100L)
                .quantity(BigDecimal.ONE)
                .promiseDate(LocalDate.of(2026, 5, 21))
                .priorityNo(30)
                .routeStatus(MesProScheduleOrderRouteStatusEnum.READY.getStatus())
                .autoSchedulable(Boolean.TRUE)
                .frozen(Boolean.TRUE)
                .build();
        when(scheduleOrderMapper.selectByIds(List.of(501L, 502L))).thenReturn(List.of(scheduleOrder, frozenOrder));
        when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L, 502L))).thenReturn(List.of(scheduleOrder));

        MesProAutoSchedulePreviewReqVO reqVO = buildReq();
        reqVO.setScheduleOrderIds(List.of(501L, 502L));

        ServiceException exception = assertThrows(ServiceException.class, () -> autoScheduleService.preview(reqVO));

        assertEquals(PRO_AUTO_SCHEDULE_ORDER_NOT_SCHEDULABLE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("SCH-ERR-502"));
        assertTrue(exception.getMessage().contains("已冻结"));
    }

    @Test
    void preview_shouldWarnAndKeepGeneratedTasksWhenProductionMaterialListMissing() {
        scheduleOrder.setCode("SCH-APPLY-501");
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(30L)
                .name("WS-1")
                .processId(300L)
                .productionLineId(40L)
                .status(0)
                .build();
        MesMdProductionLineDO productionLine = MesMdProductionLineDO.builder()
                .id(40L)
                .name("LINE-1")
                .calendarPlanId(50L)
                .status(0)
                .workshopId(60L)
                .build();
        MesCalPlanDO plan = MesCalPlanDO.builder()
                .id(50L)
                .startDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .endDate(LocalDateTime.of(2026, 5, 20, 0, 0))
                .build();
        MesCalPlanShiftDO shift = MesCalPlanShiftDO.builder()
                .id(11L)
                .planId(50L)
                .name("DAY")
                .startTime("08:00")
                .endTime("16:00")
                .build();
        MesProCapacityPlanDO capacityPlan = MesProCapacityPlanDO.builder()
                .id(70L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .shiftId(11L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();
        when(scheduleOrderMapper.selectByIds(List.of(501L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L))).thenReturn(List.of(scheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(scheduleOrderProcess));
        when(routeVersionMapper.selectActiveByRouteId(20L)).thenReturn(MesProRouteVersionDO.builder()
                .id(700L)
                .routeId(20L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionMapper.STATUS_ACTIVE)
                .build());
        when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(scheduleCalendarService.getRules()).thenReturn(buildCalendarRules());

        MesProAutoSchedulePreviewReqVO reqVO = buildReq();
        reqVO.setScheduleOrderIds(List.of(501L));
        reqVO.setReason("手动排产发布");
        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);

        assertEquals(0, preview.getSummary().getBlockingIssueCount());
        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
        assertEquals("MATERIAL_DEMAND", preview.getIssues().get(0).getIssueType());
        assertEquals("WARNING", preview.getIssues().get(0).getSeverity());
        assertEquals("工单缺少生产用料清单", preview.getIssues().get(0).getMessage());
    }

    private MesProAutoSchedulePreviewReqVO buildReq() {
        MesProAutoSchedulePreviewReqVO reqVO = new MesProAutoSchedulePreviewReqVO();
        reqVO.setStartTime(LocalDateTime.of(2026, 5, 14, 8, 0));
        reqVO.setRuntimeCapacityBasis("PLANNED");
        reqVO.setPreserveManualLockedTasks(true);
        return reqVO;
    }

    private MesProScheduleCalendarRulesRespVO buildCalendarRules() {
        MesProScheduleCalendarRulesRespVO rulesRespVO = new MesProScheduleCalendarRulesRespVO();
        rulesRespVO.setSkipStatutoryHolidays(false);
        rulesRespVO.setWeekendRestMode("NONE");
        rulesRespVO.setDateShiftModeByDate(new LinkedHashMap<>());
        rulesRespVO.setSimulationCurrentDate("2026-05-14");
        return rulesRespVO;
    }

    private MesProAutoScheduleReplanReqVO buildReplanReq() {
        MesProAutoScheduleReplanReqVO reqVO = new MesProAutoScheduleReplanReqVO();
        reqVO.setStartTime(LocalDateTime.of(2026, 5, 14, 8, 0));
        reqVO.setRuntimeCapacityBasis("PLANNED");
        reqVO.setPreserveManualLockedTasks(true);
        return reqVO;
    }

}
