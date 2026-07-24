package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleCancelNightShiftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoSchedulePreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoSchedulePreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleReplanPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleReplanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProLatestScheduleApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProReplanExplanationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarRulesRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttDataRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightSummaryRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
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
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProReplanExplanationSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderOperationLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
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
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesTimeUnitTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.EdhrScheduleCompletionCreateCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.cal.holiday.MesCalHolidayService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanShiftService;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityMetrics;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
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
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncResult;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_FROZEN_WORK_ORDER;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_CALENDAR_CONTEXT_CHANGED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_ORDER_BLOCKED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_PRODUCTION_MATERIAL_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_ROUTE_VERSION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_CALENDAR_INVALID_DATE_SHIFT_MODE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProAutoScheduleServiceImplTest {

    @Test
    void dependencies_shouldInjectProductionMaterialListSyncService() throws NoSuchFieldException {
        assertNotNull(MesProAutoScheduleServiceImpl.class
                .getDeclaredField("productionMaterialListSyncService")
                .getAnnotation(Resource.class));
    }

    @Test
    void resolveProcessScheduleQuantity_shouldPreferRemainingQuantityOverPlannedQuantity() {
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .remainingQuantity(new BigDecimal("60"))
                .plannedQuantity(new BigDecimal("100"))
                .build();

        BigDecimal result = ReflectionTestUtils.invokeMethod(autoScheduleService,
                "resolveProcessScheduleQuantity", new BigDecimal("200"), process, null);

        assertEquals(new BigDecimal("60"), result);
    }

    @Test
    void validateGeneratedProcessQuantityTieOut_shouldBlockWhenGeneratedQuantityDiffersFromRemaining() {
        SchedulePlanner.ScheduleComputation computation = new SchedulePlanner.ScheduleComputation();
        computation.scheduleOrders.add(MesProScheduleOrderDO.builder()
                .id(20L)
                .workOrderId(10L)
                .build());
        computation.scheduleOrderProcessesByOrderId.put(20L, List.of(MesProScheduleOrderProcessDO.builder()
                .id(30L)
                .processId(40L)
                .processName("灌装")
                .enabled(Boolean.TRUE)
                .remainingQuantity(new BigDecimal("50"))
                .build()));
        SchedulePlanner.PlannedTask generated = new SchedulePlanner.PlannedTask();
        generated.workOrderId = 10L;
        generated.scheduleOrderProcessId = 30L;
        generated.processId = 40L;
        generated.quantity = new BigDecimal("49");
        computation.generatedTasks.add(generated);

        ReflectionTestUtils.invokeMethod(autoScheduleService,
                "validateGeneratedProcessQuantityTieOut", computation);

        assertEquals(1, computation.issues.size());
        assertEquals("BLOCKING", computation.issues.get(0).severity);
        assertTrue(computation.issues.get(0).message.contains("剩余数量为 50"));
        assertTrue(computation.issues.get(0).message.contains("自动排产生成数量为 49"));
    }

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
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
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
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
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
    private MesProScheduleOrderOperationLogMapper scheduleOrderOperationLogMapper;
    @Mock
    private MesProReplanExplanationSnapshotMapper replanExplanationSnapshotMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProEdhrBatchExecutionService edhrBatchExecutionService;
    @Mock
    private MesKingdeeProductionMaterialListSyncService productionMaterialListSyncService;

    private MesProWorkOrderDO workOrder;
    private MesProRouteProductDO routeProduct;
    private MesProRouteProcessDO routeProcess;
    private MesMdItemDO productItem;
    private MesMdItemDO materialItem;
    private MesMdWorkstationDO workstation;
    private MesMdProductionLineDO productionLine;
    private MesCalPlanDO plan;
    private MesCalPlanShiftDO shift;
    private MesProCapacityPlanDO capacityPlan;
    private MesWmMaterialStockDO stock;
    private MesProScheduleOrderDO scheduleOrder;
    private MesProScheduleOrderProcessDO scheduleOrderProcess;

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
        ReflectionTestUtils.setField(autoScheduleService, "scheduleApplier", scheduleApplier);
        long[] generatedTaskIdSequence = {90_000L};
        lenient().doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(generatedTaskIdSequence[0]++);
            }
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));
        workOrder = MesProWorkOrderDO.builder()
                .id(1L)
                .code("WO-001")
                .productId(100L)
                .batchCode("BATCH-SCHEDULE-001")
                .quantity(BigDecimal.ONE)
                .clientId(10L)
                .build();
        routeProduct = MesProRouteProductDO.builder()
                .id(2L)
                .routeId(20L)
                .itemId(100L)
                .quantity(1)
                .productionTime(new BigDecimal("8"))
                .timeUnitType(MesTimeUnitTypeEnum.HOUR.getType())
                .build();
        routeProcess = MesProRouteProcessDO.builder()
                .id(3L)
                .routeId(20L)
                .processId(300L)
                .sort(1)
                .prepareTime(0)
                .waitTime(0)
                .colorCode("#1677ff")
                .build();
        productItem = new MesMdItemDO();
        productItem.setId(100L);
        productItem.setCode("ITEM-PROD");
        productItem.setName("成品A");
        materialItem = new MesMdItemDO();
        materialItem.setId(200L);
        materialItem.setCode("ITEM-BOM");
        materialItem.setName("物料B");
        workstation = MesMdWorkstationDO.builder()
                .id(30L)
                .name("WS-1")
                .processId(300L)
                .productionLineId(40L)
                .singleStandardHourlyCapacity(BigDecimal.ONE)
                .shiftHours(BigDecimal.ONE)
                .status(0)
                .build();
        productionLine = MesMdProductionLineDO.builder()
                .id(40L)
                .name("LINE-1")
                .calendarPlanId(50L)
                .status(0)
                .workshopId(60L)
                .build();
        plan = MesCalPlanDO.builder()
                .id(50L)
                .startDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .endDate(LocalDateTime.of(2026, 5, 20, 0, 0))
                .build();
        shift = MesCalPlanShiftDO.builder()
                .id(11L)
                .planId(50L)
                .name("DAY")
                .startTime("08:00")
                .endTime("16:00")
                .build();
        capacityPlan = MesProCapacityPlanDO.builder()
                .id(70L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .shiftId(11L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();
        stock = MesWmMaterialStockDO.builder()
                .id(80L)
                .itemId(200L)
                .quantity(new BigDecimal("10"))
                .frozen(Boolean.FALSE)
                .build();
        scheduleOrder = MesProScheduleOrderDO.builder()
                .id(501L)
                .workOrderId(1L)
                .code("SCH-001")
                .erpWorkOrderCode("WO-001")
                .productId(100L)
                .routeId(20L)
                .routeVersionId(700L)
                .routeVersion("V1")
                .scheduleConfigVersion("V1")
                .build();
        scheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(601L)
                .scheduleOrderId(501L)
                .routeVersionId(700L)
                .routeProcessId(3L)
                .predecessorRouteProcessId(null)
                .rootProcessFlag(Boolean.TRUE)
                .processId(300L)
                .sort(1)
                .build();

        lenient().when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(workOrder));
        lenient().when(workOrderService.getWorkOrderMap(any())).thenReturn(Map.of(workOrder.getId(), workOrder));
        lenient().when(routeProductService.getRouteProductByItemId(100L)).thenReturn(routeProduct);
        lenient().when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(routeProcess));
        lenient().when(routeService.getRouteMapIgnoreDeleted(any())).thenReturn(Map.of(20L,
                MesProRouteDO.builder().id(20L).code("ROUTE-20").name("球囊扩张导管").build()));
        lenient().when(itemService.getItemMap(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            Map<Long, MesMdItemDO> map = new LinkedHashMap<>();
            if (ids.contains(100L)) {
                map.put(100L, productItem);
            }
            if (ids.contains(200L)) {
                map.put(200L, materialItem);
            }
            return map;
        });
        lenient().when(processService.getProcessMap(any())).thenReturn(Map.of());
        lenient().when(taskScheduleExtMapper.selectListByTaskIds(any())).thenReturn(Collections.emptyList());
        lenient().when(feedbackMapper.selectListByTaskIds(any())).thenReturn(Collections.emptyList());
        lenient().when(scheduleOrderMapper.selectByIds(any())).thenReturn(List.of(scheduleOrder));
        lenient().when(scheduleOrderMapper.selectAutoSchedulableByIds(any())).thenReturn(List.of(scheduleOrder));
        lenient().when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(any())).thenReturn(List.of(scheduleOrder));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(anyLong())).thenReturn(List.of(scheduleOrderProcess));
        lenient().when(routeFlowConfigMapper.selectByRouteIdAndUseType(20L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(90000L)
                        .routeId(20L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build());
        lenient().when(routeScheduleConfigMapper.selectListByRouteVersionId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(routeVersionMapper.selectByRouteIdAndVersionNo(anyLong(), anyString())).thenReturn(null);
        lenient().when(routeVersionMapper.selectActiveByRouteId(anyLong())).thenReturn(MesProRouteVersionDO.builder()
                .id(700L)
                .routeId(20L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build());
        lenient().when(taskMapper.selectListByWorkstationIds(any())).thenReturn(Collections.emptyList());
        lenient().when(capacityActualMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(Collections.emptyList());
        lenient().when(holidayService.getHolidayByDay(any())).thenReturn(null);
        lenient().when(holidayService.getHolidayList(any(), any())).thenReturn(Collections.emptyList());
        lenient().when(scheduleCalendarService.getRules()).thenReturn(buildCalendarRules("2026-05-14", Collections.emptyMap()));
        lenient().when(productionMaterialListMapper.selectListByWorkOrderIds(any()))
                .thenReturn(buildProductionMaterialListRows(BigDecimal.ONE));
        lenient().when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(Collections.emptyList());
        lenient().when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));
        lenient().when(workstationCapacityService.getCapacityMetricsUsingShiftHours(any()))
                .thenAnswer(invocation -> {
                    Collection<MesMdWorkstationDO> workstations = invocation.getArgument(0);
                    Map<Long, MesMdWorkstationCapacityMetrics> result = new LinkedHashMap<>();
                    for (MesMdWorkstationDO current : workstations) {
                        BigDecimal shiftHours = current.getShiftHours() == null ? BigDecimal.ONE : current.getShiftHours();
                        BigDecimal hourlyCapacity = current.getSingleStandardHourlyCapacity() == null
                                ? BigDecimal.ONE : current.getSingleStandardHourlyCapacity();
                        result.put(current.getId(), buildCapacityMetrics(1, 1, BigDecimal.ZERO,
                                hourlyCapacity.multiply(shiftHours)));
                    }
                    return result;
                });
        lenient().when(workstationMapper.selectByIds(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            if (ids == null) {
                return Collections.emptyList();
            }
            return ids.contains(30L) ? List.of(workstation) : Collections.emptyList();
        });
        lenient().when(scheduleOrderService.preflight(any())).thenReturn(passPreflightResp());
        lenient().when(scheduleOrderService.calculateProcessProgressMetrics(anyLong(), anyList()))
                .thenReturn(Collections.emptyMap());
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

    private MesProScheduleOrderProcessDO buildScheduleOrderProcessSnapshot(Long id, Long routeProcessId,
                                                                            Long processId, Integer sort) {
        return MesProScheduleOrderProcessDO.builder()
                .id(id)
                .scheduleOrderId(scheduleOrder.getId())
                .routeVersionId(scheduleOrder.getRouteVersionId())
                .routeProcessId(routeProcessId)
                .predecessorRouteProcessId(null)
                .rootProcessFlag(null)
                .processId(processId)
                .sort(sort)
                .enabled(Boolean.TRUE)
                .plannedQuantity(workOrder.getQuantity())
                .remainingQuantity(workOrder.getQuantity())
                .reportedQuantity(BigDecimal.ZERO)
                .build();
    }

    private void stubScheduleOrderProcessesWithoutTopology(MesProScheduleOrderProcessDO... additionalProcesses) {
        scheduleOrderProcess.setRootProcessFlag(null);
        scheduleOrderProcess.setPredecessorRouteProcessId(null);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setPlannedQuantity(workOrder.getQuantity());
        scheduleOrderProcess.setRemainingQuantity(workOrder.getQuantity());
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        List<MesProScheduleOrderProcessDO> processes = new ArrayList<>();
        processes.add(scheduleOrderProcess);
        Collections.addAll(processes, additionalProcesses);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(anyLong())).thenReturn(processes);
    }

    private void stubScheduleOrderProcessesWithParallelRoots(MesProScheduleOrderProcessDO... additionalProcesses) {
        List<MesProScheduleOrderProcessDO> processes = new ArrayList<>();
        processes.add(scheduleOrderProcess);
        Collections.addAll(processes, additionalProcesses);
        for (MesProScheduleOrderProcessDO process : processes) {
            process.setPredecessorRouteProcessId(null);
            process.setRootProcessFlag(Boolean.TRUE);
            process.setEnabled(Boolean.TRUE);
            if (process.getPlannedQuantity() == null) {
                process.setPlannedQuantity(workOrder.getQuantity());
            }
            if (process.getRemainingQuantity() == null) {
                process.setRemainingQuantity(workOrder.getQuantity());
            }
            if (process.getReportedQuantity() == null) {
                process.setReportedQuantity(BigDecimal.ZERO);
            }
        }
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(anyLong())).thenReturn(processes);
    }

    @Test
    void refreshScheduleOrderProcessesFromRouteConfig_shouldUseLatestPublishedRouteVersionAndProcessIdentity() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L)
                .routeId(20L)
                .routeVersionId(700L)
                .routeVersion("V1")
                .build();
        MesProScheduleOrderProcessDO first = MesProScheduleOrderProcessDO.builder()
                .id(601L)
                .scheduleOrderId(501L)
                .routeProcessId(3L)
                .processId(300L)
                .enabled(Boolean.TRUE)
                .rootProcessFlag(Boolean.TRUE)
                .build();
        MesProScheduleOrderProcessDO second = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(501L)
                .routeProcessId(4L)
                .predecessorRouteProcessId(3L)
                .processId(301L)
                .enabled(Boolean.TRUE)
                .rootProcessFlag(Boolean.FALSE)
                .build();
        MesProRouteProcessDO latestFirst = MesProRouteProcessDO.builder()
                .id(30L).routeId(20L).processId(300L).workstationId(30L).sort(1).build();
        MesProRouteProcessDO latestSecond = MesProRouteProcessDO.builder()
                .id(31L).routeId(20L).processId(301L).workstationId(31L).sort(2).build();
        MesMdWorkstationDO secondWorkstation = MesMdWorkstationDO.builder()
                .id(31L)
                .name("WS-2")
                .processId(301L)
                .productionLineId(40L)
                .singleStandardHourlyCapacity(BigDecimal.ONE)
                .shiftHours(BigDecimal.ONE)
                .status(0)
                .build();
        when(routeVersionMapper.selectActiveByRouteId(20L)).thenReturn(MesProRouteVersionDO.builder()
                .id(800L)
                .routeId(20L)
                .versionNo("V2")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(800L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(801L)
                        .routeVersionId(800L)
                        .routeProcessId(30L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("25"))
                        .nightShiftEnabled(Boolean.TRUE)
                        .calendarRuleId(11L)
                        .build(),
                MesProRouteScheduleConfigDO.builder()
                        .id(802L)
                        .routeVersionId(800L)
                        .routeProcessId(31L)
                        .capacityMode(MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode())
                        .hourlyCapacity(new BigDecimal("10"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .calendarRuleId(12L)
                        .build()));
        doReturn(latestFirst).when(routeProcessService).resolveFrozenRouteProcess(30L, 20L, null);
        doReturn(latestSecond).when(routeProcessService).resolveFrozenRouteProcess(31L, 20L, null);
        doReturn(latestFirst).when(routeProcessService).resolveCurrentRouteProcess(3L, 20L, 300L);
        doReturn(latestSecond).when(routeProcessService).resolveCurrentRouteProcess(4L, 20L, 301L);
        when(workstationMapper.selectByIds(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            List<MesMdWorkstationDO> rows = new ArrayList<>();
            if (ids.contains(30L)) {
                rows.add(workstation);
            }
            if (ids.contains(31L)) {
                rows.add(secondWorkstation);
            }
            return rows;
        });

        @SuppressWarnings("unchecked")
        List<MesProScheduleOrderProcessDO> result = ReflectionTestUtils.invokeMethod(autoScheduleService,
                "refreshScheduleOrderProcessesFromRouteConfig", order, List.of(first, second), false);

        assertNotNull(result);
        assertEquals(800L, first.getRouteVersionId());
        assertEquals(30L, first.getRouteProcessId());
        assertEquals(300L, first.getProcessId());
        assertEquals(801L, first.getRouteScheduleConfigId());
        assertEquals(new BigDecimal("25"), first.getHourlyCapacityTotal());
        assertTrue(first.getNightShiftEnabled());
        assertEquals(800L, second.getRouteVersionId());
        assertEquals(31L, second.getRouteProcessId());
        assertEquals(30L, second.getPredecessorRouteProcessId());
        assertEquals(301L, second.getProcessId());
        assertEquals(802L, second.getRouteScheduleConfigId());
        assertEquals(MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), second.getCapacityMode());
        verify(routeVersionMapper).selectActiveByRouteId(20L);
        verify(routeScheduleConfigMapper, never()).selectListByRouteVersionId(700L);
    }

    @Test
    void replanPreview_shouldRefreshDailyCapacityLimitFromLatestPublishedRouteConfigWhenSnapshotCapacityIsStale() {
        workOrder.setQuantity(new BigDecimal("120"));
        routeProduct.setQuantity(120);
        routeProcess.setWorkstationId(30L);
        workstation.setShiftHours(new BigDecimal("8"));
        shift.setSort(1);
        shift.setStartTime("00:00");
        shift.setEndTime("00:00");
        capacityPlan.setCapacityMinutes(1440);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("120"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("120"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(BigDecimal.ZERO);
        scheduleOrderProcess.setShiftCapacityTotal(BigDecimal.ZERO);
        when(routeScheduleConfigMapper.selectListByRouteVersionId(700L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(9001L)
                        .routeVersionId(700L)
                        .routeProcessId(3L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("12.5"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .build()));
        MesProCapacityPlanDO nextDayCapacityPlan = MesProCapacityPlanDO.builder()
                .id(71L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0))
                .shiftId(11L)
                .capacityMinutes(1440)
                .enabled(Boolean.TRUE)
                .build();
        when(workstationMapper.selectByIds(any())).thenReturn(List.of(workstation));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any()))
                .thenReturn(List.of(capacityPlan, nextDayCapacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        List<GanttDataRespVO> generatedTasks = preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK, task.getType()))
                .toList();
        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(2, generatedTasks.size());
        assertEquals(new BigDecimal("100"), generatedTasks.get(0).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 14, 0, 0), generatedTasks.get(0).getStartDate());
        assertEquals(new BigDecimal("20"), generatedTasks.get(1).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 15, 0, 0), generatedTasks.get(1).getStartDate());
        assertEquals(new BigDecimal("8"), scheduleOrderProcess.getShiftHours());
        assertEquals(new BigDecimal("100.0"), scheduleOrderProcess.getShiftCapacityTotal());
    }

    @Test
    void refreshScheduleOrderProcessesFromRouteConfig_shouldUseLatestPublishedFiniteHourlyConfigWithoutWorkstation() {
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(new BigDecimal("10.5"));
        scheduleOrderProcess.setShiftCapacityTotal(new BigDecimal("10.5"));
        routeProcess.setWorkstationId(null);
        when(routeScheduleConfigMapper.selectListByRouteVersionId(700L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(9002L)
                        .routeVersionId(700L)
                        .routeProcessId(3L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("25.714286"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .build()));

        @SuppressWarnings("unchecked")
        List<MesProScheduleOrderProcessDO> refreshed = ReflectionTestUtils.invokeMethod(autoScheduleService,
                "refreshScheduleOrderProcessesFromRouteConfig", scheduleOrder, List.of(scheduleOrderProcess), false);

        assertNotNull(refreshed);
        assertEquals(700L, scheduleOrderProcess.getRouteVersionId());
        assertEquals(9002L, scheduleOrderProcess.getRouteScheduleConfigId());
        assertEquals(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode(), scheduleOrderProcess.getCapacityMode());
        assertEquals("ROUTE_PROCESS", scheduleOrderProcess.getCapacitySource());
        assertEquals(new BigDecimal("25.714286"), scheduleOrderProcess.getHourlyCapacityTotal());
        assertEquals(new BigDecimal("10.5"), scheduleOrderProcess.getShiftHours());
        assertEquals(new BigDecimal("270.0000030"), scheduleOrderProcess.getShiftCapacityTotal());
    }

    @Test
    void replanPreview_shouldUseLatestPublishedUnboundRouteProcessCapacityInsteadOfCurrentProcessWorkstation() {
        workOrder.setQuantity(new BigDecimal("120"));
        routeProduct.setQuantity(120);
        routeProcess.setWorkstationId(null);
        workstation.setShiftHours(new BigDecimal("8"));
        shift.setSort(1);
        shift.setStartTime("08:00");
        shift.setEndTime("16:00");
        capacityPlan.setCapacityMinutes(480);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("120"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("120"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(new BigDecimal("8"));
        scheduleOrderProcess.setShiftCapacityTotal(new BigDecimal("8"));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(700L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(9003L)
                        .routeVersionId(700L)
                        .routeProcessId(3L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("12.5"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .build()));
        MesProCapacityPlanDO nextDayCapacityPlan = MesProCapacityPlanDO.builder()
                .id(72L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0))
                .shiftId(11L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any()))
                .thenReturn(List.of(capacityPlan, nextDayCapacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        List<GanttDataRespVO> generatedTasks = preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK, task.getType()))
                .toList();
        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(2, generatedTasks.size());
        assertEquals(new BigDecimal("100"), generatedTasks.get(0).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0), generatedTasks.get(0).getStartDate());
        assertNull(generatedTasks.get(0).getWorkstation());
        assertEquals(new BigDecimal("20"), generatedTasks.get(1).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 15, 8, 0), generatedTasks.get(1).getStartDate());
        assertNull(generatedTasks.get(1).getWorkstation());
    }

    @Test
    void replanPreview_shouldKeepLatestPublishedUnboundRouteProcessWhenCurrentRouteIsBoundToWorkstation() {
        workOrder.setQuantity(new BigDecimal("50"));
        routeProduct.setQuantity(50);
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(13L)
                .routeId(20L)
                .processId(300L)
                .workstationId(30L)
                .sort(1)
                .prepareTime(0)
                .waitTime(0)
                .build();
        MesProRouteProcessDO publishedRouteProcess = MesProRouteProcessDO.builder()
                .id(3L)
                .routeId(20L)
                .processId(300L)
                .workstationId(null)
                .sort(1)
                .prepareTime(0)
                .waitTime(0)
                .build();
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("50"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("50"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(new BigDecimal("8"));
        scheduleOrderProcess.setShiftCapacityTotal(new BigDecimal("8"));
        workstation.setShiftHours(new BigDecimal("8"));
        shift.setSort(1);
        shift.setStartTime("08:00");
        shift.setEndTime("16:00");
        capacityPlan.setCapacityMinutes(480);
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(currentRouteProcess));
        doReturn(publishedRouteProcess).when(routeProcessService).resolveFrozenRouteProcess(3L, 20L, null);
        doReturn(publishedRouteProcess).when(routeProcessService).resolveFrozenRouteProcess(3L, 20L, 300L);
        when(routeScheduleConfigMapper.selectListByRouteVersionId(700L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(9006L)
                        .routeVersionId(700L)
                        .routeProcessId(3L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("12.5"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .build()));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        List<GanttDataRespVO> generatedTasks = preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK, task.getType()))
                .toList();
        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(1, generatedTasks.size());
        assertEquals(new BigDecimal("50"), generatedTasks.get(0).getQuantity());
        assertNull(generatedTasks.get(0).getWorkstation());
    }

    @Test
    void replanPreview_shouldReserveFeedbackProtectedRouteProcessCapacityWithoutLineKey() {
        workOrder.setQuantity(new BigDecimal("100"));
        routeProduct.setQuantity(100);
        routeProcess.setWorkstationId(null);
        workstation.setShiftHours(new BigDecimal("8"));
        shift.setSort(1);
        shift.setStartTime("08:00");
        shift.setEndTime("16:00");
        capacityPlan.setCapacityMinutes(480);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("100"));
        scheduleOrderProcess.setRemainingQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setReportedQuantity(new BigDecimal("80"));
        scheduleOrderProcess.setCapacitySource("ROUTE_PROCESS");
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(new BigDecimal("12.5"));
        scheduleOrderProcess.setShiftHours(new BigDecimal("8"));
        scheduleOrderProcess.setShiftCapacityTotal(new BigDecimal("100"));
        MesProTaskDO feedbackTask = MesProTaskDO.builder()
                .id(32L)
                .code("PT-FEEDBACK-CAPACITY")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(new BigDecimal("80"))
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 14, 24))
                .status(0)
                .build();
        MesProWorkOrderDO secondWorkOrder = MesProWorkOrderDO.builder()
                .id(2L)
                .code("WO-002")
                .productId(100L)
                .batchCode("BATCH-SCHEDULE-002")
                .quantity(new BigDecimal("100"))
                .clientId(10L)
                .build();
        MesProScheduleOrderDO secondScheduleOrder = MesProScheduleOrderDO.builder()
                .id(502L)
                .workOrderId(2L)
                .code("SCH-002")
                .erpWorkOrderCode("WO-002")
                .productId(100L)
                .routeId(20L)
                .routeVersionId(700L)
                .routeVersion("V1")
                .scheduleConfigVersion("V1")
                .build();
        MesProScheduleOrderProcessDO secondProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(502L)
                .routeVersionId(700L)
                .routeProcessId(3L)
                .predecessorRouteProcessId(null)
                .rootProcessFlag(Boolean.TRUE)
                .processId(300L)
                .sort(1)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("100"))
                .remainingQuantity(new BigDecimal("100"))
                .reportedQuantity(BigDecimal.ZERO)
                .capacitySource("ROUTE_PROCESS")
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacityTotal(new BigDecimal("12.5"))
                .shiftHours(new BigDecimal("8"))
                .shiftCapacityTotal(new BigDecimal("100"))
                .build();
        MesProCapacityPlanDO nextDayCapacityPlan = MesProCapacityPlanDO.builder()
                .id(72L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0))
                .shiftId(11L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();
        when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(workOrder, secondWorkOrder));
        when(scheduleOrderMapper.selectByIds(any())).thenReturn(List.of(scheduleOrder, secondScheduleOrder));
        when(scheduleOrderMapper.selectAutoSchedulableByIds(any())).thenReturn(List.of(scheduleOrder, secondScheduleOrder));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(any())).thenReturn(List.of(scheduleOrder, secondScheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(anyLong())).thenAnswer(invocation ->
                Objects.equals(invocation.getArgument(0), 502L) ? List.of(secondProcess) : List.of(scheduleOrderProcess));
        when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                buildProductionMaterialListRow(1L, 200L, BigDecimal.ONE),
                buildProductionMaterialListRow(2L, 200L, BigDecimal.ONE)));
        stock.setQuantity(new BigDecimal("200"));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any()))
                .thenReturn(List.of(capacityPlan, nextDayCapacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(feedbackTask));
        when(taskScheduleExtMapper.selectListByTaskIds(any())).thenReturn(List.of(MesProTaskScheduleExtDO.builder()
                .taskId(32L)
                .scheduleOrderId(501L)
                .scheduleOrderProcessId(601L)
                .scheduleSource("AUTO")
                .locked(Boolean.FALSE)
                .riskStatus("NONE")
                .build()));
        when(feedbackMapper.selectListByTaskIds(any())).thenReturn(List.of(MesProFeedbackDO.builder()
                .id(303L)
                .taskId(32L)
                .workOrderId(1L)
                .status(1)
                .feedbackQuantity(new BigDecimal("80"))
                .build()));
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setScheduleOrderIds(List.of(501L, 502L));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);

        List<GanttDataRespVO> generatedTasks = preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK, task.getType()))
                .filter(task -> task.getId() != null && task.getId().contains("_preview_"))
                .toList();
        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(2, generatedTasks.size());
        assertEquals(new BigDecimal("20"), generatedTasks.get(0).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0), generatedTasks.get(0).getStartDate());
        assertEquals(new BigDecimal("80"), generatedTasks.get(1).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 15, 8, 0), generatedTasks.get(1).getStartDate());
    }

    @Test
    void replanPreview_shouldExtendRouteProcessVirtualWindowsWhenPriorOrdersConsumeDailyCapacity() {
        workOrder.setQuantity(new BigDecimal("120"));
        routeProduct.setQuantity(120);
        routeProcess.setWorkstationId(null);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("120"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("120"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(new BigDecimal("8"));
        scheduleOrderProcess.setShiftCapacityTotal(new BigDecimal("8"));
        MesProWorkOrderDO secondWorkOrder = MesProWorkOrderDO.builder()
                .id(2L)
                .code("WO-002")
                .productId(100L)
                .batchCode("BATCH-SCHEDULE-002")
                .quantity(new BigDecimal("120"))
                .clientId(10L)
                .build();
        MesProScheduleOrderDO secondScheduleOrder = MesProScheduleOrderDO.builder()
                .id(502L)
                .workOrderId(2L)
                .code("SCH-002")
                .erpWorkOrderCode("WO-002")
                .productId(100L)
                .routeId(20L)
                .routeVersionId(700L)
                .routeVersion("V1")
                .scheduleConfigVersion("V1")
                .build();
        MesProScheduleOrderProcessDO secondProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(502L)
                .routeVersionId(700L)
                .routeProcessId(3L)
                .predecessorRouteProcessId(null)
                .rootProcessFlag(Boolean.TRUE)
                .processId(300L)
                .sort(1)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("120"))
                .remainingQuantity(new BigDecimal("120"))
                .reportedQuantity(BigDecimal.ZERO)
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacityTotal(BigDecimal.ONE)
                .shiftHours(new BigDecimal("8"))
                .shiftCapacityTotal(new BigDecimal("8"))
                .build();
        when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(workOrder, secondWorkOrder));
        when(scheduleOrderMapper.selectByIds(any())).thenReturn(List.of(scheduleOrder, secondScheduleOrder));
        when(scheduleOrderMapper.selectAutoSchedulableByIds(any())).thenReturn(List.of(scheduleOrder, secondScheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(anyLong())).thenAnswer(invocation ->
                Objects.equals(invocation.getArgument(0), 502L) ? List.of(secondProcess) : List.of(scheduleOrderProcess));
        when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                buildProductionMaterialListRow(1L, 200L, BigDecimal.ONE),
                buildProductionMaterialListRow(2L, 200L, BigDecimal.ONE)));
        stock.setQuantity(new BigDecimal("1000"));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(700L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(9004L)
                        .routeVersionId(700L)
                        .routeProcessId(3L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("12.5"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .build()));
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setScheduleOrderIds(List.of(501L, 502L));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);

        List<GanttDataRespVO> generatedTasks = preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK, task.getType()))
                .toList();
        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(4, generatedTasks.size());
        assertEquals(new BigDecimal("100"), generatedTasks.get(0).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0), generatedTasks.get(0).getStartDate());
        assertEquals(new BigDecimal("20"), generatedTasks.get(1).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 15, 8, 0), generatedTasks.get(1).getStartDate());
        assertEquals(new BigDecimal("80"), generatedTasks.get(2).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 15, 9, 36), generatedTasks.get(2).getStartDate());
        assertEquals(new BigDecimal("40"), generatedTasks.get(3).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 16, 8, 0), generatedTasks.get(3).getStartDate());
    }

    @Test
    void replanPreview_shouldKeepSearchingRouteProcessWindowsForLargeOrderAfterPriorRouteProcessCapacityUse() {
        workOrder.setQuantity(new BigDecimal("100"));
        routeProduct.setQuantity(100);
        routeProcess.setWorkstationId(null);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("100"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("100"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(new BigDecimal("10.5"));
        scheduleOrderProcess.setShiftCapacityTotal(new BigDecimal("10.5"));
        MesProWorkOrderDO secondWorkOrder = MesProWorkOrderDO.builder()
                .id(2L)
                .code("WO-002")
                .productId(100L)
                .batchCode("BATCH-SCHEDULE-002")
                .quantity(new BigDecimal("1000"))
                .clientId(10L)
                .build();
        MesProScheduleOrderDO secondScheduleOrder = MesProScheduleOrderDO.builder()
                .id(502L)
                .workOrderId(2L)
                .code("SCH-002")
                .erpWorkOrderCode("WO-002")
                .productId(100L)
                .routeId(20L)
                .routeVersionId(700L)
                .routeVersion("V1")
                .scheduleConfigVersion("V1")
                .build();
        MesProScheduleOrderProcessDO secondProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(502L)
                .routeVersionId(700L)
                .routeProcessId(3L)
                .predecessorRouteProcessId(null)
                .rootProcessFlag(Boolean.TRUE)
                .processId(300L)
                .sort(1)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("1000"))
                .remainingQuantity(new BigDecimal("1000"))
                .reportedQuantity(BigDecimal.ZERO)
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacityTotal(BigDecimal.ONE)
                .shiftHours(new BigDecimal("10.5"))
                .shiftCapacityTotal(new BigDecimal("10.5"))
                .build();
        when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(workOrder, secondWorkOrder));
        when(scheduleOrderMapper.selectByIds(any())).thenReturn(List.of(scheduleOrder, secondScheduleOrder));
        when(scheduleOrderMapper.selectAutoSchedulableByIds(any())).thenReturn(List.of(scheduleOrder, secondScheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(anyLong())).thenAnswer(invocation ->
                Objects.equals(invocation.getArgument(0), 502L) ? List.of(secondProcess) : List.of(scheduleOrderProcess));
        when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                buildProductionMaterialListRow(1L, 200L, BigDecimal.ONE),
                buildProductionMaterialListRow(2L, 200L, BigDecimal.ONE)));
        stock.setQuantity(new BigDecimal("2000"));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(700L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(9005L)
                        .routeVersionId(700L)
                        .routeProcessId(3L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("70.476190"))
                        .nightShiftEnabled(Boolean.FALSE)
                        .build()));
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setScheduleOrderIds(List.of(501L, 502L));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);

        List<GanttDataRespVO> generatedTasks = preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK, task.getType()))
                .toList();
        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(0, new BigDecimal("1100").compareTo(generatedTasks.stream()
                .map(GanttDataRespVO::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        Map<LocalDate, BigDecimal> quantityByDate = generatedTasks.stream()
                .collect(Collectors.groupingBy(task -> task.getStartDate().toLocalDate(), LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, GanttDataRespVO::getQuantity, BigDecimal::add)));
        assertTrue(quantityByDate.values().stream()
                .allMatch(quantity -> quantity.compareTo(new BigDecimal("740")) <= 0), quantityByDate.toString());
        assertTrue(quantityByDate.keySet().stream()
                .anyMatch(date -> date.isAfter(LocalDate.of(2026, 5, 14))), quantityByDate.toString());
    }

    @Test
    void refreshScheduleOrderProcessesFromRouteConfig_shouldRejectScheduleOrderWithoutLatestPublishedRouteVersion() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(503L)
                .code("SCH-MISSING-ROUTE-VERSION")
                .routeId(20L)
                .routeVersion("V1")
                .scheduleConfigVersion("V1")
                .build();
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(603L)
                .scheduleOrderId(503L)
                .routeProcessId(3L)
                .processId(300L)
                .rootProcessFlag(Boolean.TRUE)
                .enabled(Boolean.TRUE)
                .build();
        when(routeVersionMapper.selectActiveByRouteId(20L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                ReflectionTestUtils.invokeMethod(autoScheduleService,
                        "refreshScheduleOrderProcessesFromRouteConfig", order, List.of(process), false));

        assertEquals(PRO_AUTO_SCHEDULE_ROUTE_VERSION_REQUIRED.getCode(), ex.getCode());
        verify(routeVersionMapper, never()).selectByRouteIdAndVersionNo(anyLong(), anyString());
        verify(routeVersionMapper).selectActiveByRouteId(20L);
        verify(routeScheduleConfigMapper, never()).selectListByRouteVersionId(anyLong());
    }

    @Test
    void preview_shouldExposeShortageAsWarningAndKeepGeneratedTasks() {
        MesWmMaterialStockDO shortageStock = MesWmMaterialStockDO.builder()
                .id(81L)
                .itemId(200L)
                .quantity(new BigDecimal("0.5"))
                .frozen(Boolean.FALSE)
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(shortageStock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        assertTrue(preview.getPreviewOnly());
        assertEquals(0, preview.getSummary().getBlockingIssueCount());
        assertEquals(1, preview.getSummary().getShortageCount());
        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
        assertEquals("MATERIAL", preview.getIssues().get(0).getIssueType());
        assertEquals("WARNING", preview.getIssues().get(0).getSeverity());
        assertEquals(2, preview.getTasks().size());
        verify(taskMapper, never()).insert(any(MesProTaskDO.class));
        verify(taskScheduleExtMapper, never()).insert(any(MesProTaskScheduleExtDO.class));
        verify(workOrderMapper, never()).updateQuantityScheduled(anyLong(), any());
    }

    @Test
    void preview_shouldAggregateRequiredQuantityFromProductionMaterialList() {
        MesWmMaterialStockDO shortageStock = MesWmMaterialStockDO.builder()
                .id(82L)
                .itemId(200L)
                .quantity(new BigDecimal("2"))
                .frozen(Boolean.FALSE)
                .build();
        when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                buildProductionMaterialListRow(1L, 200L, BigDecimal.ONE),
                buildProductionMaterialListRow(1L, 200L, new BigDecimal("2"))));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(shortageStock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        assertEquals(1, preview.getSummary().getShortageCount());
        assertEquals("MATERIAL", preview.getIssues().get(0).getIssueType());
        assertEquals(0, new BigDecimal("3").compareTo(preview.getIssues().get(0).getRequiredQty()));
        assertEquals(0, new BigDecimal("2").compareTo(preview.getIssues().get(0).getAvailableQty()));
        assertEquals(0, BigDecimal.ONE.compareTo(preview.getIssues().get(0).getShortageQty()));
    }

    @Test
    void replanPreview_shouldExposeMaterialNameAndShortageQuantityInShortageMessage() {
        MesWmMaterialStockDO shortageStock = MesWmMaterialStockDO.builder()
                .id(83L)
                .itemId(200L)
                .quantity(new BigDecimal("0.5"))
                .frozen(Boolean.FALSE)
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(shortageStock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(1, preview.getSummary().getShortageCount());
        assertEquals("MATERIAL", preview.getIssues().get(0).getIssueType());
        assertEquals("ITEM-BOM", preview.getIssues().get(0).getMaterialCode());
        assertEquals("物料B", preview.getIssues().get(0).getMaterialName());
        assertEquals(0, new BigDecimal("0.5").compareTo(preview.getIssues().get(0).getShortageQty()));
        String message = preview.getIssues().get(0).getMessage();
        assertFalse(message.contains("物料库存不足"));
        assertTrue(message.contains("ITEM-BOM"));
        assertTrue(message.contains("物料B"));
        assertTrue(message.contains("0.5"));
    }

    @Test
    void preview_shouldWarnAndKeepGeneratedTasksWhenProductionMaterialListMissing() {
        when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(Collections.emptyList());
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(0, preview.getSummary().getShortageCount());
        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
        assertEquals("MATERIAL_DEMAND", preview.getIssues().get(0).getIssueType());
        assertEquals("WARNING", preview.getIssues().get(0).getSeverity());
        assertEquals("工单缺少生产用料清单", preview.getIssues().get(0).getMessage());
        assertTrue(preview.getTasks() != null);
    }

    @Test
    void preview_shouldScheduleByRouteProcessWhenCandidateWorkstationHasNoProductionLine() {
        workstation.setProductionLineId(null);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(new BigDecimal("8"));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Collections.emptyMap());
        when(planService.getPlanMap(any())).thenReturn(Collections.emptyMap());
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(Collections.emptyList());
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount());
        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
        assertEquals(2, preview.getTasks().size());
        var generatedTask = preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK,
                        readRequiredField(task, "type", Integer.class)))
                .findFirst()
                .orElseThrow();
        assertNull(generatedTask.getWorkstation());
        assertEquals("球囊扩张导管", readRequiredStringField(generatedTask, "line"));
        verify(taskMapper, never()).insert(any(MesProTaskDO.class));
    }

    @Test
    void preview_shouldKeepSameProcessIndependentAcrossScheduleRoutes() {
        workstation.setProductionLineId(null);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(new BigDecimal("8"));
        MesProWorkOrderDO secondWorkOrder = MesProWorkOrderDO.builder()
                .id(2L)
                .code("WO-002")
                .productId(101L)
                .batchCode("BATCH-SCHEDULE-002")
                .quantity(BigDecimal.ONE)
                .clientId(10L)
                .build();
        MesProRouteProductDO secondRouteProduct = MesProRouteProductDO.builder()
                .id(12L)
                .routeId(21L)
                .itemId(101L)
                .quantity(1)
                .productionTime(new BigDecimal("8"))
                .timeUnitType(MesTimeUnitTypeEnum.HOUR.getType())
                .build();
        MesProRouteProcessDO secondRouteProcess = MesProRouteProcessDO.builder()
                .id(4L)
                .routeId(21L)
                .processId(300L)
                .sort(1)
                .prepareTime(0)
                .waitTime(0)
                .colorCode("#1677ff")
                .build();
        MesProScheduleOrderDO secondScheduleOrder = MesProScheduleOrderDO.builder()
                .id(502L)
                .workOrderId(2L)
                .code("SCH-002")
                .erpWorkOrderCode("WO-002")
                .productId(101L)
                .routeId(21L)
                .routeVersionId(701L)
                .routeVersion("V1")
                .scheduleConfigVersion("V1")
                .build();
        MesProScheduleOrderProcessDO secondScheduleOrderProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(502L)
                .routeVersionId(701L)
                .routeProcessId(4L)
                .predecessorRouteProcessId(null)
                .rootProcessFlag(Boolean.TRUE)
                .processId(300L)
                .processName("吹球囊成型")
                .sort(1)
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacityTotal(BigDecimal.ONE)
                .shiftHours(new BigDecimal("8"))
                .build();
        MesMdItemDO secondProductItem = new MesMdItemDO();
        secondProductItem.setId(101L);
        secondProductItem.setCode("ITEM-PROD-2");
        secondProductItem.setName("成品B");
        when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(workOrder, secondWorkOrder));
        when(routeProductService.getRouteProductByItemId(100L)).thenReturn(routeProduct);
        when(routeProductService.getRouteProductByItemId(101L)).thenReturn(secondRouteProduct);
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(21L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(90001L)
                        .routeId(21L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build());
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(routeProcess));
        when(routeProcessService.getRouteProcessListByRouteId(21L)).thenReturn(List.of(secondRouteProcess));
        when(routeService.getRouteMapIgnoreDeleted(any())).thenReturn(Map.of(
                20L, MesProRouteDO.builder().id(20L).code("ROUTE-20").name("球囊扩张导管").build(),
                21L, MesProRouteDO.builder().id(21L).code("ROUTE-21").name("球囊扩张导管二线").build()));
        when(scheduleOrderMapper.selectByIds(List.of(501L, 502L))).thenReturn(List.of(scheduleOrder, secondScheduleOrder));
        when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L, 502L))).thenReturn(List.of(scheduleOrder, secondScheduleOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(scheduleOrderProcess));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(502L)).thenReturn(List.of(secondScheduleOrderProcess));
        when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                buildProductionMaterialListRow(1L, 200L, BigDecimal.ONE),
                buildProductionMaterialListRow(2L, 200L, BigDecimal.ONE)));
        doAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            Map<Long, MesMdItemDO> map = new LinkedHashMap<>();
            if (ids.contains(100L)) {
                map.put(100L, productItem);
            }
            if (ids.contains(101L)) {
                map.put(101L, secondProductItem);
            }
            if (ids.contains(200L)) {
                map.put(200L, materialItem);
            }
            return map;
        }).when(itemService).getItemMap(any());
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Collections.emptyMap());
        when(planService.getPlanMap(any())).thenReturn(Collections.emptyMap());
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(Collections.emptyList());
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        MesProAutoSchedulePreviewReqVO reqVO = buildReq();
        reqVO.setScheduleOrderIds(List.of(501L, 502L));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(reqVO);

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        var generatedTasks = preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK,
                        readRequiredField(task, "type", Integer.class)))
                .toList();
        assertEquals(2, generatedTasks.size());
        assertEquals(2, generatedTasks.stream()
                .filter(task -> LocalDateTime.of(2026, 5, 14, 8, 0).equals(
                        readRequiredField(task, "startDate", LocalDateTime.class)))
                .count());
    }

    @Test
    void preview_shouldAllowCrossLineSchedulingWhenEachProcessHasOwnEnabledLine() {
        MesProRouteProcessDO secondRouteProcess = MesProRouteProcessDO.builder()
                .id(4L)
                .routeId(20L)
                .processId(301L)
                .sort(2)
                .prepareTime(0)
                .waitTime(0)
                .colorCode("#1677ff")
                .build();
        MesMdWorkstationDO secondWorkstation = MesMdWorkstationDO.builder()
                .id(31L)
                .name("WS-2")
                .processId(301L)
                .productionLineId(41L)
                .status(0)
                .build();
        MesMdProductionLineDO secondLine = MesMdProductionLineDO.builder()
                .id(41L)
                .name("LINE-2")
                .calendarPlanId(50L)
                .status(0)
                .workshopId(60L)
                .build();
        MesProScheduleOrderProcessDO secondScheduleProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(501L)
                .routeVersionId(700L)
                .routeProcessId(4L)
                .predecessorRouteProcessId(3L)
                .rootProcessFlag(Boolean.FALSE)
                .processId(301L)
                .sort(2)
                .build();
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(routeProcess, secondRouteProcess));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L))
                .thenReturn(List.of(scheduleOrderProcess, secondScheduleProcess));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation, secondWorkstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine, 41L, secondLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(
                        30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE),
                        31L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));
        stubScheduleOrderProcessesWithParallelRoots(buildScheduleOrderProcessSnapshot(602L, 4L, 301L, 2));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan,
                MesProCapacityPlanDO.builder()
                        .id(71L)
                        .lineId(41L)
                        .calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                        .shiftId(11L)
                        .capacityMinutes(480)
                        .enabled(Boolean.TRUE)
                        .build()));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(2, preview.getSummary().getGeneratedTaskCount());
        assertTrue(preview.getIssues().isEmpty());
        assertEquals(3, preview.getTasks().size());
        assertEquals(2L, preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK, readRequiredField(task, "type", Integer.class)))
                .count());
        assertEquals(Set.of("球囊扩张导管"), preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK, readRequiredField(task, "type", Integer.class)))
                .map(task -> readRequiredStringField(task, "line"))
                .collect(Collectors.toSet()));
    }

    @Test
    void preview_shouldRejectTemporaryFrozenWorkOrder() {
        workOrder.setTemporaryFrozen(Boolean.TRUE);

        ServiceException ex = assertThrows(ServiceException.class, () -> autoScheduleService.preview(buildReq()));

        assertEquals(PRO_AUTO_SCHEDULE_FROZEN_WORK_ORDER.getCode(), ex.getCode());
        verify(routeProductService, never()).getRouteProductByItemId(anyLong());
    }

    @Test
    void apply_shouldRejectTemporaryFrozenWorkOrder() {
        workOrder.setTemporaryFrozen(Boolean.TRUE);
        MesProAutoSchedulePreviewReqVO reqVO = buildReq();
        reqVO.setCalendarContextToken("token-1");

        ServiceException ex = assertThrows(ServiceException.class, () -> autoScheduleService.apply(reqVO));

        assertEquals(PRO_AUTO_SCHEDULE_FROZEN_WORK_ORDER.getCode(), ex.getCode());
        verify(taskMapper, never()).insert(any(MesProTaskDO.class));
    }

    @Test
    void apply_shouldRejectMissingReason() {
        MesProAutoSchedulePreviewReqVO reqVO = buildReq();
        reqVO.setReason("   ");
        reqVO.setCalendarContextToken("token-1");

        ServiceException ex = assertThrows(ServiceException.class, () -> autoScheduleService.apply(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_REASON_REQUIRED.getCode(), ex.getCode());
        verify(taskMapper, never()).insert(any(MesProTaskDO.class));
    }

    @Test
    void preview_shouldRejectInvalidLegacyCalendarRule() {
        MesCalPlanShiftDO nightShift = MesCalPlanShiftDO.builder()
                .id(12L)
                .planId(50L)
                .sort(2)
                .name("NIGHT")
                .startTime("20:00")
                .endTime("08:00")
                .build();
        MesProCapacityPlanDO nightCapacityPlan = MesProCapacityPlanDO.builder()
                .id(71L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .shiftId(12L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift, nightShift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan, nightCapacityPlan));
        scheduleOrderProcess.setNightShiftEnabled(Boolean.TRUE);
        when(scheduleCalendarService.getRules()).thenReturn(buildCalendarRules("2026-05-13", Map.of("2026-05-14", "NIGHT_LEGACY")));

        ServiceException ex = assertThrows(ServiceException.class, () -> autoScheduleService.preview(buildReq()));

        assertEquals(PRO_SCHEDULE_CALENDAR_INVALID_DATE_SHIFT_MODE.getCode(), ex.getCode());
    }

    @Test
    void preview_shouldChangeWhenSimulationDateChanges() {
        MesProCapacityPlanDO nextDayCapacityPlan = MesProCapacityPlanDO.builder()
                .id(72L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0))
                .shiftId(11L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan, nextDayCapacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(scheduleCalendarService.getRules()).thenReturn(buildCalendarRules("2026-05-14", Collections.emptyMap()));

        MesProAutoSchedulePreviewRespVO todayPreview = autoScheduleService.preview(buildReq());

        when(scheduleCalendarService.getRules()).thenReturn(buildCalendarRules("2026-05-15", Collections.emptyMap()));

        MesProAutoSchedulePreviewRespVO simulatedTomorrowPreview = autoScheduleService.preview(buildReq());

        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0), todayPreview.getSummary().getStartTime());
        assertEquals(LocalDateTime.of(2026, 5, 15, 8, 0), simulatedTomorrowPreview.getSummary().getStartTime());
    }

    @Test
    void apply_shouldCreateTaskAndSyncQuantityScheduled() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(autoCodeRecordService.generateAutoCode(anyString())).thenReturn("PT-001");
        doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            task.setId(500L + storedTasks.size());
            storedTasks.add(task);
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());
        MesProAutoSchedulePreviewReqVO applyReq = buildReq();
        writeRequiredStringField(applyReq, "calendarContextToken", readRequiredStringField(preview, "calendarContextToken"));

        var response = autoScheduleService.apply(applyReq);

        assertTrue(response.getApplied());
        assertEquals(1, response.getCreatedTaskIds().size());
        assertEquals(1, storedTasks.size());
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0), storedTasks.get(0).getStartTime());
        assertEquals(LocalDateTime.of(2026, 5, 14, 9, 0), storedTasks.get(0).getEndTime());
        assertEquals(1, storedTasks.get(0).getDuration());

        verify(taskScheduleExtMapper, times(1)).insert(any(MesProTaskScheduleExtDO.class));
        ArgumentCaptor<BigDecimal> quantityCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(workOrderMapper).updateQuantityScheduled(eq(1L), quantityCaptor.capture());
        assertEquals(0, BigDecimal.ONE.compareTo(quantityCaptor.getValue()));
        ArgumentCaptor<MesProScheduleOrderDO> scheduleOrderUpdateCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper, atLeastOnce()).updateById(scheduleOrderUpdateCaptor.capture());
        assertTrue(scheduleOrderUpdateCaptor.getAllValues().stream()
                        .anyMatch(update -> Objects.equals(501L, update.getId())
                                && Objects.equals(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus(), update.getStatus())),
                "自动排产发布创建任务后必须把排产工单状态回写为已排产");
        verify(scheduleIssueMapper, never()).insertBatch(any());
    }

    @Test
    void apply_shouldSkipEdhrBatchCreationWhenRouteHasNoBatchRecordBinding() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(autoCodeRecordService.generateAutoCode(anyString())).thenReturn("PT-001");
        doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            task.setId(800L + storedTasks.size());
            storedTasks.add(task);
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(20L, "BATCH"))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(91000L).routeId(20L).useType("BATCH").enabled(Boolean.FALSE).build());
        lenient().when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(20L, "BATCH")).thenReturn(List.of(
                MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(91000L)
                        .routeId(20L)
                        .routeProcessId(3L)
                        .useType("BATCH")
                        .enabled(Boolean.TRUE)
                        .executionMode("SEQUENTIAL")
                        .build()));
        lenient().doThrow(new IllegalStateException("unexpected eDHR trigger"))
                .when(edhrBatchExecutionService).openOrCreateFromScheduleCompletion(any());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());
        MesProAutoSchedulePreviewReqVO applyReq = buildReq();
        writeRequiredStringField(applyReq, "calendarContextToken", readRequiredStringField(preview, "calendarContextToken"));

        var response = assertDoesNotThrow(() -> autoScheduleService.apply(applyReq));

        assertTrue(response.getApplied());
        verify(edhrBatchExecutionService, never()).openOrCreateFromScheduleCompletion(any());
    }

    @Test
    void apply_shouldNotEnterTransactionalEdhrCreationWhenSchedulePrerequisiteMissing() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        workOrder.setBatchCode(null);
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(autoCodeRecordService.generateAutoCode(anyString())).thenReturn("PT-001");
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(20L, "BATCH"))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(91000L).routeId(20L).useType("BATCH").enabled(Boolean.TRUE).build());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(20L, "BATCH")).thenReturn(List.of(
                MesProRouteFlowProcessConfigDO.builder()
                        .id(91001L)
                        .routeFlowConfigId(91000L)
                        .routeId(20L)
                        .routeProcessId(3L)
                        .useType("BATCH")
                        .enabled(Boolean.TRUE)
                        .executionMode("SEQUENTIAL")
                        .build()));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(List.of(3L), "BATCH"))
                .thenReturn(List.of(MesProRouteFlowProcessBatchRecordDO.builder()
                        .routeFlowProcessConfigId(91001L)
                        .routeId(20L)
                        .routeProcessId(3L)
                        .useType("BATCH")
                        .batchRecordReportId("REPORT-001")
                        .reportSort(1)
                        .build()));
        doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            task.setId(850L + storedTasks.size());
            storedTasks.add(task);
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));
        when(edhrBatchExecutionService.getScheduleCompletionMissingItems(any()))
                .thenReturn(List.of("批次号"));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());
        MesProAutoSchedulePreviewReqVO applyReq = buildReq();
        writeRequiredStringField(applyReq, "calendarContextToken", readRequiredStringField(preview, "calendarContextToken"));

        var response = assertDoesNotThrow(() -> autoScheduleService.apply(applyReq));

        assertTrue(response.getApplied());
        assertEquals(1, response.getCreatedTaskIds().size());
        verify(workOrderMapper).updateQuantityScheduled(eq(1L), any(BigDecimal.class));
        ArgumentCaptor<List<MesProScheduleIssueDO>> issueCaptor = ArgumentCaptor.forClass(List.class);
        verify(scheduleIssueMapper).insertBatch(issueCaptor.capture());
        assertTrue(issueCaptor.getValue().stream()
                .anyMatch(issue -> "EDHR_BATCH_CREATION".equals(issue.getIssueType())
                        && "WARNING".equals(issue.getSeverity())
                        && issue.getMessage().contains("批次号")));
        verify(edhrBatchExecutionService).getScheduleCompletionMissingItems(any());
        verify(edhrBatchExecutionService, never()).openOrCreateFromScheduleCompletion(any());
    }

    @Test
    void apply_shouldTriggerEdhrBatchCreationAfterScheduleComplete() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(20L, "BATCH"))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(91000L).routeId(20L).useType("BATCH").enabled(Boolean.TRUE).build());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(20L, "BATCH")).thenReturn(List.of(
                MesProRouteFlowProcessConfigDO.builder()
                        .id(91001L)
                        .routeFlowConfigId(91000L)
                        .routeId(20L)
                        .routeProcessId(3L)
                        .useType("BATCH")
                        .enabled(Boolean.TRUE)
                        .executionMode("SEQUENTIAL")
                        .build()));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(List.of(3L), "BATCH"))
                .thenReturn(List.of(MesProRouteFlowProcessBatchRecordDO.builder()
                        .routeFlowProcessConfigId(91001L)
                        .routeId(20L)
                        .routeProcessId(3L)
                        .useType("BATCH")
                        .batchRecordReportId("REPORT-001")
                        .reportSort(1)
                        .build()));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(autoCodeRecordService.generateAutoCode(anyString())).thenReturn("PT-001");
        doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            task.setId(900L + storedTasks.size());
            storedTasks.add(task);
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());
        MesProAutoSchedulePreviewReqVO applyReq = buildReq();
        writeRequiredStringField(applyReq, "calendarContextToken", readRequiredStringField(preview, "calendarContextToken"));

        autoScheduleService.apply(applyReq);

        ArgumentCaptor<EdhrScheduleCompletionCreateCommand> captor =
                ArgumentCaptor.forClass(EdhrScheduleCompletionCreateCommand.class);
        verify(edhrBatchExecutionService).openOrCreateFromScheduleCompletion(captor.capture());
        EdhrScheduleCompletionCreateCommand command = captor.getValue();
        assertEquals(1L, command.getWorkOrderId());
        assertEquals("BATCH-SCHEDULE-001", command.getBatchCode());
        assertEquals(20L, command.getRouteId());
        assertEquals(100L, command.getProductId());
    }

    @Test
    void apply_shouldCreateTaskAndPersistShortageWarnings() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        MesWmMaterialStockDO shortageStock = MesWmMaterialStockDO.builder()
                .id(81L)
                .itemId(200L)
                .quantity(new BigDecimal("0.5"))
                .frozen(Boolean.FALSE)
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(shortageStock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(autoCodeRecordService.generateAutoCode(anyString())).thenReturn("PT-001");
        doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            task.setId(700L + storedTasks.size());
            storedTasks.add(task);
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());
        MesProAutoSchedulePreviewReqVO applyReq = buildReq();
        writeRequiredStringField(applyReq, "calendarContextToken", readRequiredStringField(preview, "calendarContextToken"));

        var response = autoScheduleService.apply(applyReq);

        assertTrue(response.getApplied());
        assertEquals(1, response.getCreatedTaskIds().size());
        assertEquals(1, storedTasks.size());
        assertEquals(0, response.getSummary().getBlockingIssueCount());
        assertEquals(1, response.getSummary().getShortageCount());
        verify(taskScheduleExtMapper, times(1)).insert(any(MesProTaskScheduleExtDO.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesProScheduleIssueDO>> issueCaptor = (ArgumentCaptor<List<MesProScheduleIssueDO>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(scheduleIssueMapper).insertBatch(issueCaptor.capture());
        assertEquals(1, issueCaptor.getValue().size());
        assertEquals("MATERIAL", issueCaptor.getValue().get(0).getIssueType());
        assertEquals("WARNING", issueCaptor.getValue().get(0).getSeverity());
        assertEquals(0, new BigDecimal("0.5").compareTo(issueCaptor.getValue().get(0).getShortageQty()));
    }

    @Test
    void replanPreview_shouldExposeProtectedLockedTask() {
        MesProTaskDO lockedTask = MesProTaskDO.builder()
                .id(10L)
                .code("PT-LOCK-001")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 16, 0))
                .status(0)
                .build();
        MesProTaskScheduleExtDO lockedExt = MesProTaskScheduleExtDO.builder()
                .taskId(10L)
                .scheduleSource("AUTO")
                .locked(Boolean.TRUE)
                .lockedReason("browser-lock")
                .riskStatus("NONE")
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(lockedTask));
        when(taskScheduleExtMapper.selectListByTaskIds(any())).thenReturn(List.of(lockedExt));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(1, preview.getProtectedTasks().size());
        assertEquals(10L, preview.getProtectedTasks().get(0).getTaskId());
        assertEquals("LOCKED", preview.getProtectedTasks().get(0).getProtectionReason());
        assertEquals(1, preview.getSummary().getPreservedTaskCount());
        assertEquals(0, preview.getSummary().getGeneratedTaskCount());
    }

    @Test
    void replanApply_shouldReuseApplyPathAndPreserveLockedTask() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        MesProTaskDO lockedTask = MesProTaskDO.builder()
                .id(20L)
                .code("PT-LOCK-002")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 16, 0))
                .status(0)
                .build();
        storedTasks.add(lockedTask);
        MesProTaskScheduleExtDO lockedExt = MesProTaskScheduleExtDO.builder()
                .taskId(20L)
                .scheduleSource("AUTO")
                .locked(Boolean.TRUE)
                .lockedReason("browser-lock")
                .riskStatus("NONE")
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(taskScheduleExtMapper.selectListByTaskIds(any())).thenReturn(List.of(lockedExt));
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        var response = autoScheduleService.replanApply(reqVO);

        assertTrue(response.getApplied());
        assertEquals(0, response.getDeletedTaskIds().size());
        assertEquals(1, response.getPreservedTaskIds().size());
        assertEquals(20L, response.getPreservedTaskIds().get(0));
        verify(taskMapper, never()).deleteById(eq(20L));
    }

    @Test
    void replanApply_shouldPersistCompleteExplanationSnapshot() {
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        scheduleOrder.setPromiseDate(LocalDate.of(2026, 5, 20));
        scheduleOrder.setPriorityNo(3);

        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        autoScheduleService.replanApply(reqVO);

        ArgumentCaptor<MesProReplanExplanationSnapshotDO> snapshotCaptor =
                ArgumentCaptor.forClass(MesProReplanExplanationSnapshotDO.class);
        verify(replanExplanationSnapshotMapper).insert(snapshotCaptor.capture());
        MesProReplanExplanationSnapshotDO snapshot = snapshotCaptor.getValue();
        assertEquals("MANUAL", snapshot.getTriggerSource());
        assertEquals("PLANNED", snapshot.getCapacityMode());
        assertNotNull(snapshot.getAppliedAt());
        assertTrue(snapshot.getSnapshotJson().contains("\"scheduleOrderCount\":1"));
        assertTrue(snapshot.getSnapshotJson().contains("\"requiredQty\":1"));
        assertTrue(snapshot.getSnapshotJson().contains("\"availableQty\":10"));
        assertTrue(snapshot.getSnapshotJson().contains("\"workOrderCode\":\"WO-001\""));
        assertTrue(snapshot.getSnapshotJson().contains("\"effectiveHourlyCapacity\""));
        MesProReplanExplanationRespVO explanation = JsonUtils.parseObject(
                snapshot.getSnapshotJson(), MesProReplanExplanationRespVO.class);
        assertNotNull(explanation.getDailyExplanations());
        assertEquals(1, explanation.getDailyExplanations().size());
        MesProReplanExplanationRespVO.DailyExplanationItem daily = explanation.getDailyExplanations().get(0);
        assertEquals(LocalDate.of(2026, 5, 14), daily.getPlanDate());
        assertEquals(1L, daily.getWorkOrderId());
        assertEquals(300L, daily.getProcessId());
        assertEquals(new BigDecimal("1"), daily.getPlannedQuantity());
        assertEquals(1, daily.getGeneratedTaskCount());
        assertEquals(480, daily.getAvailableWindowMinutes());
        assertEquals(60, daily.getUsedWindowMinutes());
        assertEquals(0, daily.getProtectedOccupiedMinutes());
        assertEquals("CAPACITY_WINDOW", daily.getReasonCode());
    }

    @Test
    void replanApply_shouldAggregateProtectedAndGeneratedDailyExplanationByScheduleOrderProcess() {
        workOrder.setQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ONE);
        scheduleOrderProcess.setRemainingQuantity(BigDecimal.ONE);
        MesProTaskDO protectedTask = MesProTaskDO.builder()
                .id(960L)
                .code("PT-PROTECTED-EXPLAIN")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 9, 0))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(protectedTask));

        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        autoScheduleService.replanApply(reqVO);

        ArgumentCaptor<MesProReplanExplanationSnapshotDO> snapshotCaptor =
                ArgumentCaptor.forClass(MesProReplanExplanationSnapshotDO.class);
        verify(replanExplanationSnapshotMapper).insert(snapshotCaptor.capture());
        MesProReplanExplanationRespVO explanation = JsonUtils.parseObject(
                snapshotCaptor.getValue().getSnapshotJson(), MesProReplanExplanationRespVO.class);

        assertNotNull(explanation.getDailyExplanations());
        assertEquals(1, explanation.getDailyExplanations().size());
        MesProReplanExplanationRespVO.DailyExplanationItem daily = explanation.getDailyExplanations().get(0);
        assertEquals(LocalDate.of(2026, 5, 14), daily.getPlanDate());
        assertEquals(601L, daily.getScheduleOrderProcessId());
        assertEquals(new BigDecimal("2"), daily.getPlannedQuantity());
        assertEquals(1, daily.getGeneratedTaskCount());
        assertEquals(60, daily.getUsedWindowMinutes());
        assertEquals(60, daily.getProtectedOccupiedMinutes());
        assertEquals("CAPACITY_WINDOW_WITH_PROTECTED", daily.getReasonCode());
        assertEquals(BigDecimal.ZERO, daily.getRemainingQuantityAfter());
    }

    @Test
    void replanApply_shouldUseGeneratedTaskRangeForExplanationSummaryWhenProtectedTaskIsHistorical() {
        workOrder.setQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ONE);
        scheduleOrderProcess.setRemainingQuantity(BigDecimal.ONE);
        MesProTaskDO protectedTask = MesProTaskDO.builder()
                .id(961L)
                .code("PT-PROTECTED-HISTORICAL")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 9, 0))
                .status(MesProTaskStatusEnum.FINISHED.getStatus())
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(protectedTask));

        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        autoScheduleService.replanApply(reqVO);

        ArgumentCaptor<MesProReplanExplanationSnapshotDO> snapshotCaptor =
                ArgumentCaptor.forClass(MesProReplanExplanationSnapshotDO.class);
        verify(replanExplanationSnapshotMapper).insert(snapshotCaptor.capture());
        MesProReplanExplanationRespVO explanation = JsonUtils.parseObject(
                snapshotCaptor.getValue().getSnapshotJson(), MesProReplanExplanationRespVO.class);

        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0), explanation.getSummary().getStartTime());
        assertEquals(LocalDateTime.of(2026, 5, 14, 9, 0), explanation.getSummary().getEndTime());
    }

    @Test
    void replanApply_whenExplanationSnapshotInsertFails_shouldFailAndUseRollbackTransaction() throws Exception {
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        doThrow(new IllegalStateException("snapshot insert failed"))
                .when(replanExplanationSnapshotMapper).insert(any(MesProReplanExplanationSnapshotDO.class));

        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> autoScheduleService.replanApply(reqVO));
        assertEquals("snapshot insert failed", exception.getMessage());

        Transactional transactional = MesProAutoScheduleServiceImpl.class
                .getDeclaredMethod("replanApply", MesProAutoScheduleReplanReqVO.class)
                .getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertArrayEquals(new Class<?>[]{Exception.class}, transactional.rollbackFor());
    }

    @Test
    void getLatestReplanExplanation_shouldReturnNoDataWhenSnapshotDoesNotExist() {
        when(replanExplanationSnapshotMapper.selectLatest()).thenReturn(null);

        MesProReplanExplanationRespVO response = autoScheduleService.getLatestReplanExplanation();

        assertFalse(response.getHasData());
    }

    @Test
    void getLatestReplanExplanation_shouldReturnStoredSuccessfulSnapshotWithoutRecalculation() {
        MesProReplanExplanationSnapshotDO snapshot = new MesProReplanExplanationSnapshotDO()
                .setId(9001L)
                .setSnapshotJson("""
                        {"hasData":true,"requestId":"REQ-9001","triggerSource":"NIGHTLY",
                        "operatorName":"系统","capacityMode":"PLANNED",
                        "summary":{"scheduleOrderCount":2,"generatedTaskCount":6}}
                        """);
        when(replanExplanationSnapshotMapper.selectLatest()).thenReturn(snapshot);

        MesProReplanExplanationRespVO response = autoScheduleService.getLatestReplanExplanation();

        assertTrue(response.getHasData());
        assertEquals("REQ-9001", response.getRequestId());
        assertEquals("NIGHTLY", response.getTriggerSource());
        assertEquals("系统", response.getOperatorName());
        assertEquals(2, response.getSummary().getScheduleOrderCount());
        assertEquals(6, response.getSummary().getGeneratedTaskCount());
        verifyNoInteractions(materialStockMapper, capacityPlanMapper, taskMapper);
    }

    @Test
    void getLatestSuccessfulScheduleApply_shouldReturnNoDataWhenApplyLogDoesNotExist() {
        when(scheduleOrderOperationLogMapper.selectLatestByOperationTypes(List.of("AUTO_APPLY", "REPLAN_APPLY")))
                .thenReturn(null);

        MesProLatestScheduleApplyRespVO response = autoScheduleService.getLatestSuccessfulScheduleApply();

        assertFalse(response.getHasData());
        verifyNoInteractions(replanExplanationSnapshotMapper, materialStockMapper, capacityPlanMapper, taskMapper);
    }

    @Test
    void getLatestSuccessfulScheduleApply_shouldReturnLatestAutoOrReplanOperationLogTime() {
        LocalDateTime latestTime = LocalDateTime.of(2026, 7, 21, 9, 30);
        MesProScheduleOrderOperationLogDO latestLog = MesProScheduleOrderOperationLogDO.builder()
                .id(8801L)
                .scheduleOrderId(501L)
                .scheduleOrderCode("SO-501")
                .operationType("AUTO_APPLY")
                .operatorId(1001L)
                .operatorName("排产员")
                .reason("发布排产")
                .build();
        latestLog.setCreateTime(latestTime);
        when(scheduleOrderOperationLogMapper.selectLatestByOperationTypes(List.of("AUTO_APPLY", "REPLAN_APPLY")))
                .thenReturn(latestLog);

        MesProLatestScheduleApplyRespVO response = autoScheduleService.getLatestSuccessfulScheduleApply();

        assertTrue(response.getHasData());
        assertEquals(latestTime, response.getAppliedAt());
        assertEquals("AUTO_APPLY", response.getOperationType());
        assertEquals(1001L, response.getOperatorId());
        assertEquals("排产员", response.getOperatorName());
        assertEquals(501L, response.getScheduleOrderId());
        assertEquals("SO-501", response.getScheduleOrderCode());
        verifyNoInteractions(replanExplanationSnapshotMapper, materialStockMapper, capacityPlanMapper, taskMapper);
    }

    @Test
    void replanApply_shouldAllowMissingReason() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        MesProTaskDO lockedTask = MesProTaskDO.builder()
                .id(21L)
                .code("PT-LOCK-003")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 16, 0))
                .status(0)
                .build();
        storedTasks.add(lockedTask);
        MesProTaskScheduleExtDO lockedExt = MesProTaskScheduleExtDO.builder()
                .taskId(21L)
                .scheduleSource("AUTO")
                .locked(Boolean.TRUE)
                .lockedReason("browser-lock")
                .riskStatus("NONE")
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(taskScheduleExtMapper.selectListByTaskIds(any())).thenReturn(List.of(lockedExt));
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setReason("   ");
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        var response = autoScheduleService.replanApply(reqVO);

        assertTrue(response.getApplied());
        assertEquals(0, response.getDeletedTaskIds().size());
        assertEquals(1, response.getPreservedTaskIds().size());
        assertEquals(21L, response.getPreservedTaskIds().get(0));
        verify(taskMapper, never()).deleteById(eq(21L));
    }

    @Test
    void replanPreview_shouldNormalizeRequestStartTimeToWholeDayDate() {
        workOrder.setQuantity(new BigDecimal("8"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("8"));
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("8"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setStartTime(LocalDateTime.of(2026, 5, 14, 15, 30));

        autoScheduleService.replanPreview(reqVO);

        ArgumentCaptor<LocalDateTime> capacityStartCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(capacityPlanMapper).selectListByLineIdsAndDate(any(), capacityStartCaptor.capture());
        assertEquals(LocalDateTime.of(2026, 5, 14, 0, 0), capacityStartCaptor.getValue());
    }

    @Test
    void replanPreview_shouldStartFromTomorrowWholeDayWhenTomorrowDateIsSelected() {
        LocalDate tomorrow = LocalDate.of(2026, 5, 15);
        MesProCapacityPlanDO tomorrowCapacityPlan = MesProCapacityPlanDO.builder()
                .id(71L)
                .lineId(40L)
                .calendarDate(tomorrow.atStartOfDay())
                .shiftId(11L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();
        workOrder.setQuantity(new BigDecimal("8"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("8"));
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("8"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any()))
                .thenReturn(List.of(capacityPlan, tomorrowCapacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setStartTime(LocalDateTime.of(2026, 5, 15, 15, 30));

        autoScheduleService.replanPreview(reqVO);

        ArgumentCaptor<LocalDateTime> capacityStartCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(capacityPlanMapper).selectListByLineIdsAndDate(any(), capacityStartCaptor.capture());
        assertEquals(LocalDateTime.of(2026, 5, 15, 0, 0), capacityStartCaptor.getValue());
    }

    @Test
    void preview_shouldKeepExactRequestStartTimeForNormalAutoSchedule() {
        workOrder.setQuantity(new BigDecimal("1"));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0), preview.getSummary().getStartTime());
    }

    @Test
    void replanPreview_shouldExposeProtectedFeedbackTask() {
        MesProTaskDO feedbackTask = MesProTaskDO.builder()
                .id(30L)
                .code("PT-FEEDBACK-001")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 16, 0))
                .status(0)
                .build();
        MesProFeedbackDO feedback = MesProFeedbackDO.builder()
                .id(301L)
                .taskId(30L)
                .workOrderId(1L)
                .status(1)
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(feedbackTask));
        when(feedbackMapper.selectListByTaskIds(any())).thenReturn(List.of(feedback));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(1, preview.getProtectedTasks().size());
        assertEquals(30L, preview.getProtectedTasks().get(0).getTaskId());
        assertEquals("FEEDBACK", preview.getProtectedTasks().get(0).getProtectionReason());
        assertEquals(1, preview.getSummary().getPreservedTaskCount());
        assertEquals(0, preview.getSummary().getGeneratedTaskCount());
    }

    @Test
    void replanPreview_shouldScheduleRemainingQuantityWhenFeedbackTaskProtected() {
        workOrder.setQuantity(new BigDecimal("1000"));
        routeProduct.setQuantity(1000);
        routeProduct.setProductionTime(BigDecimal.ONE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("1000"));
        scheduleOrderProcess.setReportedQuantity(new BigDecimal("200"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("800"));
        scheduleOrderProcess.setCapacityMode(null);
        MesProTaskDO feedbackTask = MesProTaskDO.builder()
                .id(31L)
                .code("PT-FEEDBACK-002")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(new BigDecimal("1000"))
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 8, 12))
                .status(0)
                .build();
        MesProFeedbackDO feedback = MesProFeedbackDO.builder()
                .id(302L)
                .taskId(31L)
                .workOrderId(1L)
                .status(1)
                .feedbackQuantity(new BigDecimal("200"))
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(
                MesWmMaterialStockDO.builder()
                        .id(84L)
                        .itemId(200L)
                        .quantity(new BigDecimal("1000"))
                        .frozen(Boolean.FALSE)
                        .build()));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(feedbackTask));
        when(feedbackMapper.selectListByTaskIds(any())).thenReturn(List.of(feedback));
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("1000"))));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(1, preview.getProtectedTasks().size());
        assertEquals(31L, preview.getProtectedTasks().get(0).getTaskId());
        assertEquals("FEEDBACK", preview.getProtectedTasks().get(0).getProtectionReason());
        assertEquals(1, preview.getSummary().getPreservedTaskCount());
        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
        var generatedTasks = preview.getTasks().stream()
                .filter(task -> task.getId() != null && task.getId().contains("_preview_"))
                .toList();
        assertEquals(1, generatedTasks.size());
        assertEquals(0, new BigDecimal("800").compareTo(
                generatedTasks.get(0).getQuantity()));
    }

    @Test
    void replanPreview_shouldAllowProtectedTaskBeforePredecessorWhenProcessesCanStartInParallel() {
        MesProRouteProcessDO downstreamRouteProcess = MesProRouteProcessDO.builder()
                .id(4L)
                .routeId(20L)
                .processId(301L)
                .sort(2)
                .prepareTime(0)
                .waitTime(0)
                .build();
        MesProScheduleOrderProcessDO downstreamScheduleProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(501L)
                .routeVersionId(700L)
                .routeProcessId(4L)
                .predecessorRouteProcessId(3L)
                .rootProcessFlag(Boolean.FALSE)
                .processId(301L)
                .sort(2)
                .plannedQuantity(BigDecimal.ONE)
                .reportedQuantity(BigDecimal.ONE)
                .remainingQuantity(BigDecimal.ZERO)
                .build();
        MesMdWorkstationDO downstreamWorkstation = MesMdWorkstationDO.builder()
                .id(31L)
                .name("WS-2")
                .processId(301L)
                .productionLineId(40L)
                .status(0)
                .build();
        MesProTaskDO feedbackTask = MesProTaskDO.builder()
                .id(32L)
                .code("PT-FEEDBACK-INVERTED")
                .workOrderId(1L)
                .workstationId(31L)
                .routeId(20L)
                .processId(301L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 30))
                .endTime(LocalDateTime.of(2026, 5, 14, 9, 0))
                .status(0)
                .build();
        MesProFeedbackDO feedback = MesProFeedbackDO.builder()
                .id(303L)
                .taskId(32L)
                .workOrderId(1L)
                .status(1)
                .feedbackQuantity(BigDecimal.ONE)
                .build();
        when(routeProcessService.getRouteProcessListByRouteId(20L))
                .thenReturn(List.of(routeProcess, downstreamRouteProcess));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(anyLong()))
                .thenReturn(List.of(scheduleOrderProcess, downstreamScheduleProcess));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any()))
                .thenReturn(List.of(workstation, downstreamWorkstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(feedbackTask));
        when(feedbackMapper.selectListByTaskIds(any())).thenReturn(List.of(feedback));
        when(workstationCapacityService.getCapacityMetrics(any(), any())).thenReturn(Map.of(
                30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE),
                31L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertFalse(preview.getIssues().stream().anyMatch(issue ->
                "PROTECTED_TASK".equals(issue.getIssueType())
                        && "WARNING".equals(issue.getSeverity())
                        && "受保护任务早于直接前置工序结束".equals(issue.getMessage())));
        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
        assertEquals(1, preview.getSummary().getPreservedTaskCount());
    }

    @Test
    void replanPreview_shouldScheduleRouteProcessBeyondInitialWindowWhenRouteCapacityExists() {
        workOrder.setQuantity(new BigDecimal("91"));
        routeProduct.setQuantity(91);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(BigDecimal.ONE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("91"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("91"));
        MesProTaskDO existingTask = MesProTaskDO.builder()
                .id(60L)
                .code("PT-KEEP-CAPACITY-WARN")
                .workOrderId(1L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(new BigDecimal("91"))
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 20, 16, 0))
                .status(0)
                .build();
        MesProTaskScheduleExtDO existingTaskExt = MesProTaskScheduleExtDO.builder()
                .taskId(60L)
                .scheduleSource("AUTO")
                .locked(Boolean.FALSE)
                .riskStatus("NONE")
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(
                MesWmMaterialStockDO.builder()
                        .id(85L)
                        .itemId(200L)
                        .quantity(new BigDecimal("1000"))
                        .frozen(Boolean.FALSE)
                        .build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(any()))
                .thenReturn(buildProductionMaterialListRows(new BigDecimal("1000")));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(Collections.emptyList());
        when(productionLineService.getProductionLineMap(any())).thenReturn(Collections.emptyMap());
        when(planService.getPlanMap(any())).thenReturn(Collections.emptyMap());
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(existingTask));
        when(taskScheduleExtMapper.selectListByTaskIds(any())).thenReturn(List.of(existingTaskExt));

        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertFalse(preview.getIssues().stream().anyMatch(issue ->
                "CAPACITY".equals(issue.getIssueType())
                        && "路线工序可用日历产能不足".equals(issue.getMessage())));
        assertEquals(91, preview.getSummary().getGeneratedTaskCount());
        assertTrue(preview.getTasks().stream().anyMatch(task -> "301_1".equals(task.getParent())));
    }

    @Test
    void replanPreview_shouldUseRouteProcessCapacityWhenLineCalendarCapacityIsInsufficient() {
        workOrder.setQuantity(new BigDecimal("91"));
        routeProduct.setQuantity(91);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(BigDecimal.ONE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("91"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("91"));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(
                MesWmMaterialStockDO.builder()
                        .id(86L)
                        .itemId(200L)
                        .quantity(new BigDecimal("1000"))
                        .frozen(Boolean.FALSE)
                        .build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(any()))
                .thenReturn(buildProductionMaterialListRows(new BigDecimal("1000")));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertFalse(preview.getIssues().stream().anyMatch(issue ->
                "CAPACITY".equals(issue.getIssueType())
                        && "路线工序可用日历产能不足".equals(issue.getMessage())));
        assertEquals(91, preview.getSummary().getGeneratedTaskCount());
        assertTrue(preview.getTasks().stream().anyMatch(task -> "301_1".equals(task.getParent())));
    }

    @Test
    void replanPreview_shouldMoveToLaterCapacityInsteadOfBlockingOnFirstMissingShift() {
        workOrder.setQuantity(new BigDecimal("2"));
        routeProduct.setQuantity(2);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(BigDecimal.ONE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("2"));
        MesProCapacityPlanDO laterCapacityPlan = MesProCapacityPlanDO.builder()
                .id(71L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0))
                .shiftId(11L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(
                MesWmMaterialStockDO.builder()
                        .id(87L)
                        .itemId(200L)
                        .quantity(new BigDecimal("1000"))
                        .frozen(Boolean.FALSE)
                        .build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(any()))
                .thenReturn(buildProductionMaterialListRows(new BigDecimal("1000")));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(laterCapacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
        assertTrue(preview.getTasks().stream()
                .filter(task -> readRequiredStringField(task, "id").contains("_preview_"))
                .anyMatch(task -> LocalDateTime.of(2026, 5, 15, 8, 0).equals(
                        readRequiredField(task, "startDate", LocalDateTime.class))));
    }

    @Test
    void replanPreview_shouldMoveRouteProcessCapacityStartPastRestDate() {
        workOrder.setQuantity(BigDecimal.ONE);
        routeProduct.setQuantity(1);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(BigDecimal.ONE);
        scheduleOrderProcess.setPlannedQuantity(BigDecimal.ONE);
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setRemainingQuantity(BigDecimal.ONE);
        when(scheduleCalendarService.getRules()).thenReturn(buildCalendarRules("2026-05-14",
                Map.of("2026-05-14", MesProScheduleCalendarRuleSupport.DATE_SHIFT_REST)));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(Collections.emptyList());
        when(productionLineService.getProductionLineMap(any())).thenReturn(Collections.emptyMap());
        when(planService.getPlanMap(any())).thenReturn(Collections.emptyMap());
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        var generatedTask = preview.getTasks().stream()
                .filter(task -> readRequiredStringField(task, "id").contains("_preview_"))
                .findFirst()
                .orElseThrow();
        assertNotEquals(LocalDate.of(2026, 5, 14), generatedTask.getStartDate().toLocalDate());
        assertEquals(LocalDateTime.of(2026, 5, 15, 8, 0), generatedTask.getStartDate());
    }

    @Test
    void replanPreview_shouldUseExistingSnapshotWhenRouteFlowConfigDisabled() {
        workOrder.setQuantity(new BigDecimal("2"));
        routeProduct.setQuantity(2);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(BigDecimal.ONE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("2"));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(20L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(90000L)
                        .routeId(20L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.FALSE)
                        .build());
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(
                        30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE),
                        31L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
        assertFalse(preview.getIssues().stream().anyMatch(issue ->
                "工艺路线已被禁用".equals(issue.getMessage())));
    }

    @Test
    void replanPreview_shouldIgnoreDisabledStaleSnapshotProcessIdentity() {
        workOrder.setQuantity(new BigDecimal("2"));
        routeProduct.setQuantity(2);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(BigDecimal.ONE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("2"));
        MesProScheduleOrderProcessDO disabledStaleProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(501L)
                .routeVersionId(700L)
                .routeProcessId(999L)
                .processId(301L)
                .sort(2)
                .enabled(Boolean.FALSE)
                .plannedQuantity(BigDecimal.TEN)
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(BigDecimal.TEN)
                .build();
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L))
                .thenReturn(List.of(scheduleOrderProcess, disabledStaleProcess));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(
                        30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE),
                        31L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
        verify(routeProcessService, never()).resolveCurrentRouteProcess(eq(999L), eq(20L), eq(301L));
    }

    @Test
    void replanPreview_shouldExposeProtectedInProgressTask() {
        MesProTaskDO inProgressTask = MesProTaskDO.builder()
                .id(35L)
                .code("PT-RUNNING-001")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 16, 0))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(inProgressTask));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(1, preview.getProtectedTasks().size());
        assertEquals(35L, preview.getProtectedTasks().get(0).getTaskId());
        assertEquals("IN_PROGRESS", preview.getProtectedTasks().get(0).getProtectionReason());
        assertEquals(1, preview.getSummary().getPreservedTaskCount());
        assertEquals(0, preview.getSummary().getGeneratedTaskCount());
    }

    @Test
    void replanPreview_shouldIgnoreFinishedScheduleOrderTasksWhenReservingLineProcessCapacity() {
        MesProTaskDO hiddenFinishedOrderTask = MesProTaskDO.builder()
                .id(36L)
                .code("PT-HIDDEN-FINISHED")
                .workOrderId(2L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 12, 0))
                .status(0)
                .build();
        MesProScheduleOrderDO finishedScheduleOrder = MesProScheduleOrderDO.builder()
                .id(502L)
                .workOrderId(2L)
                .code("SCH-FINISHED")
                .erpWorkOrderCode("WO-FINISHED")
                .productId(100L)
                .routeId(20L)
                .status(MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(taskMapper.selectListByWorkstationIds(any())).thenReturn(List.of(hiddenFinishedOrderTask));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(any())).thenReturn(List.of(scheduleOrder));
        lenient().when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(2L))).thenReturn(List.of(finishedScheduleOrder));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
        Object generatedTask = preview.getTasks().stream()
                .filter(task -> Integer.valueOf(303).equals(task.getType()))
                .findFirst()
                .orElseThrow();
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0),
                readRequiredField(generatedTask, "startDate", LocalDateTime.class));
        assertEquals(LocalDateTime.of(2026, 5, 14, 9, 0),
                readRequiredField(generatedTask, "endDate", LocalDateTime.class));
    }

    @Test
    void replanApply_shouldNotPreserveScheduleTaskOnlyBecauseEdhrExecutionExists() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        MesProTaskDO edhrTask = MesProTaskDO.builder()
                .id(40L)
                .code("PT-EDHR-001")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 16, 0))
                .status(0)
                .build();
        storedTasks.add(edhrTask);
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        MesProTaskScheduleExtDO autoExt = MesProTaskScheduleExtDO.builder()
                .taskId(40L)
                .scheduleSource("AUTO")
                .build();
        when(taskScheduleExtMapper.selectListByTaskIds(any())).thenReturn(List.of(autoExt));
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setPreserveManualLockedTasks(false);
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);
        assertEquals(0, preview.getProtectedTasks().size());
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        var response = autoScheduleService.replanApply(reqVO);

        assertTrue(response.getApplied());
        assertEquals(1, response.getDeletedTaskIds().size());
        assertEquals(40L, response.getDeletedTaskIds().get(0));
        assertEquals(0, response.getPreservedTaskIds().size());
        verify(taskMapper).deleteById(eq(40L));
    }

    @Test
    void autoScheduleService_shouldNotDependOnBatchRecordExecutionTable() throws IOException {
        String source = Files.readString(Path.of("src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java"),
                StandardCharsets.UTF_8);

        assertFalse(source.contains("MesProBatchRecordExecutionMapper"));
        assertFalse(source.contains("batchRecordExecutionMapper"));
        assertFalse(source.contains("batchExecutionByTaskId"));
        assertFalse(source.contains("return \"EDHR\""));
    }

    @Test
    void apply_shouldRejectWhenCalendarContextChangesAfterPreview() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(scheduleCalendarService.getRules()).thenReturn(buildCalendarRules("2026-05-14", Collections.emptyMap()));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());
        String calendarContextToken = readRequiredStringField(preview, "calendarContextToken");
        MesProScheduleCalendarRulesRespVO calendarSummary = readRequiredField(preview, "calendarSummary", MesProScheduleCalendarRulesRespVO.class);
        assertEquals("2026-05-14", calendarSummary.getSimulationCurrentDate());

        MesProAutoSchedulePreviewReqVO applyReq = buildReq();
        writeRequiredStringField(applyReq, "calendarContextToken", calendarContextToken);
        when(scheduleCalendarService.getRules()).thenReturn(buildCalendarRules("2026-05-15", Collections.emptyMap()));

        ServiceException ex = assertThrows(ServiceException.class, () -> autoScheduleService.apply(applyReq));
        assertEquals(PRO_AUTO_SCHEDULE_CALENDAR_CONTEXT_CHANGED.getCode(), ex.getCode());
        assertTrue(storedTasks.isEmpty());
    }

    @Test
    void apply_shouldWarnAndInsertTasksWhenProductionMaterialListMissing() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(Collections.emptyList());
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());
        MesProAutoSchedulePreviewReqVO applyReq = buildReq();
        writeRequiredStringField(applyReq, "calendarContextToken", readRequiredStringField(preview, "calendarContextToken"));

        var response = autoScheduleService.apply(applyReq);

        assertTrue(response.getApplied());
        verify(taskMapper).insert(any(MesProTaskDO.class));
        verify(scheduleIssueMapper).insertBatch(argThat(issues -> issues != null && issues.stream()
                .anyMatch(issue -> "MATERIAL_DEMAND".equals(issue.getIssueType())
                        && "WARNING".equals(issue.getSeverity())
                        && "工单缺少生产用料清单".equals(issue.getMessage()))));
    }

    @Test
    void apply_shouldSyncByWorkOrderCodeBeforeBlockingMissingProductionMaterialList() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        when(productionMaterialListMapper.selectListByWorkOrderIds(any()))
                .thenReturn(Collections.emptyList())
                .thenReturn(buildProductionMaterialListRows(BigDecimal.ONE));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(productionMaterialListSyncService.syncByProductionOrderNos(argThat(orderNos ->
                orderNos != null && orderNos.size() == 1 && orderNos.contains("WO-001"))))
                .thenReturn(new MesKingdeeProductionMaterialListSyncResult());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());
        MesProAutoSchedulePreviewReqVO applyReq = buildReq();
        writeRequiredStringField(applyReq, "calendarContextToken", readRequiredStringField(preview, "calendarContextToken"));

        var response = autoScheduleService.apply(applyReq);

        assertTrue(response.getApplied());
        verify(productionMaterialListSyncService, atLeastOnce()).syncByProductionOrderNos(argThat(orderNos ->
                orderNos != null && orderNos.size() == 1 && orderNos.contains("WO-001")));
        verify(taskMapper).insert(any(MesProTaskDO.class));
    }

    @Test
    void apply_shouldResyncProductionMaterialListWhenChildMaterialMappingMissing() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        MesKingdeeProductionMaterialListDO unmappedRow = buildProductionMaterialListRow(1L, null, BigDecimal.ONE)
                .setChildMaterialCode("A002.09.002.230396")
                .setChildMaterialName("外标签 (INT)");
        MesKingdeeProductionMaterialListDO remappedRow = buildProductionMaterialListRow(1L, 200L, BigDecimal.ONE)
                .setChildMaterialCode("A002.09.002.230396")
                .setChildMaterialName("外标签 (INT)");
        when(productionMaterialListMapper.selectListByWorkOrderIds(any()))
                .thenReturn(List.of(unmappedRow))
                .thenReturn(List.of(remappedRow));
        when(materialStockMapper.selectListByItemIds(argThat(itemIds -> itemIds != null && itemIds.contains(200L))))
                .thenReturn(List.of(stock));
        when(materialStockMapper.selectListByItemIds(argThat(itemIds -> itemIds == null || !itemIds.contains(200L))))
                .thenReturn(Collections.emptyList());
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(productionMaterialListSyncService.syncByProductionOrderNos(argThat(orderNos ->
                orderNos != null && orderNos.size() == 1 && orderNos.contains("WO-001"))))
                .thenReturn(new MesKingdeeProductionMaterialListSyncResult());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());
        MesProAutoSchedulePreviewReqVO applyReq = buildReq();
        writeRequiredStringField(applyReq, "calendarContextToken", readRequiredStringField(preview, "calendarContextToken"));

        var response = autoScheduleService.apply(applyReq);

        assertTrue(response.getApplied());
        verify(productionMaterialListSyncService, atLeastOnce()).syncByProductionOrderNos(argThat(orderNos ->
                orderNos != null && orderNos.size() == 1 && orderNos.contains("WO-001")));
        verify(taskMapper).insert(any(MesProTaskDO.class));
    }

    @Test
    void apply_shouldWarnAndInsertTasksWithChildMaterialCodeWhenProductionMaterialChildMappingMissing() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        MesKingdeeProductionMaterialListDO unmappedRow = buildProductionMaterialListRow(1L, null, BigDecimal.ONE)
                .setChildMaterialCode("A002.09.002.230396")
                .setChildMaterialName("外标签 (INT)");
        when(productionMaterialListMapper.selectListByWorkOrderIds(any()))
                .thenReturn(List.of(unmappedRow))
                .thenReturn(List.of(unmappedRow));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(Collections.emptyList());
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(productionMaterialListSyncService.syncByProductionOrderNos(argThat(orderNos ->
                orderNos != null && orderNos.size() == 1 && orderNos.contains("WO-001"))))
                .thenReturn(new MesKingdeeProductionMaterialListSyncResult());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());
        MesProAutoSchedulePreviewReqVO applyReq = buildReq();
        writeRequiredStringField(applyReq, "calendarContextToken", readRequiredStringField(preview, "calendarContextToken"));

        var response = autoScheduleService.apply(applyReq);

        assertTrue(response.getApplied());
        verify(taskMapper).insert(any(MesProTaskDO.class));
        verify(scheduleIssueMapper).insertBatch(argThat(issues -> issues != null && issues.stream()
                .anyMatch(issue -> "MATERIAL_DEMAND".equals(issue.getIssueType())
                        && "WARNING".equals(issue.getSeverity())
                        && issue.getMessage().contains("A002.09.002.230396")
                        && issue.getMessage().contains("外标签 (INT)"))));
    }

    @Test
    void preview_shouldKeepWholeRouteOnSingleLineAndExposeWorkOrderAnalysis() {
        productionLine.setCode("AUTO-LINE-01");
        productionLine.setName("AutoScheduleLine");
        workOrder.setQuantity(new BigDecimal("99"));
        routeProcess = MesProRouteProcessDO.builder()
                .id(3L)
                .routeId(20L)
                .processId(300L)
                .sort(1)
                .prepareTime(0)
                .waitTime(0)
                .colorCode("#1677ff")
                .build();
        MesProRouteProcessDO secondProcess = MesProRouteProcessDO.builder()
                .id(4L)
                .routeId(20L)
                .processId(301L)
                .sort(2)
                .prepareTime(0)
                .waitTime(0)
                .colorCode("#52c41a")
                .build();
        MesProScheduleOrderProcessDO secondScheduleProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(501L)
                .routeVersionId(700L)
                .routeProcessId(4L)
                .predecessorRouteProcessId(3L)
                .rootProcessFlag(Boolean.FALSE)
                .processId(301L)
                .sort(2)
                .build();
        MesMdWorkstationDO line1Process1 = MesMdWorkstationDO.builder().id(30L).name("L1-P1").processId(300L).productionLineId(40L).status(0).build();
        MesMdWorkstationDO line1Process2 = MesMdWorkstationDO.builder().id(31L).name("L1-P2").processId(301L).productionLineId(40L).status(0).build();
        MesMdWorkstationDO line2Process1 = MesMdWorkstationDO.builder().id(32L).name("L2-P1").processId(300L).productionLineId(41L).status(0).build();
        MesMdWorkstationDO line2Process2 = MesMdWorkstationDO.builder().id(33L).name("L2-P2").processId(301L).productionLineId(41L).status(0).build();
        MesMdProductionLineDO line2 = MesMdProductionLineDO.builder()
                .id(41L).name("LINE-2").code("LINE-2").calendarPlanId(51L).status(0).workshopId(60L).build();
        MesCalPlanDO plan2 = MesCalPlanDO.builder()
                .id(51L)
                .startDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .endDate(LocalDateTime.of(2026, 5, 20, 0, 0))
                .build();
        MesCalPlanShiftDO shift2 = MesCalPlanShiftDO.builder()
                .id(12L)
                .planId(51L)
                .name("DAY")
                .startTime("08:00")
                .endTime("16:00")
                .build();
        MesProCapacityPlanDO capacityPlan2 = MesProCapacityPlanDO.builder()
                .id(71L)
                .lineId(41L)
                .calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .shiftId(12L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(routeProcess, secondProcess));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L))
                .thenReturn(List.of(scheduleOrderProcess, secondScheduleProcess));
        when(processService.getProcessMap(any())).thenReturn(Map.of(
                300L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder().id(300L).name("P1").build(),
                301L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder().id(301L).name("P2").build()
        ));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(line1Process1, line1Process2, line2Process1, line2Process2));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine, 41L, line2));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan, 51L, plan2));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(planShiftService.getPlanShiftListByPlanId(51L)).thenReturn(List.of(shift2));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan, capacityPlan2));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any())).thenReturn(Map.of(
                30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("99")),
                31L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("99")),
                32L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("99")),
                33L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("99"))
        ));
        stubScheduleOrderProcessesWithParallelRoots(buildScheduleOrderProcessSnapshot(602L, 4L, 301L, 2));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        Set<String> lineNames = preview.getTasks().stream()
                .filter(task -> Integer.valueOf(303).equals(task.getType()))
                .map(task -> task.getLine())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertEquals(Set.of("球囊扩张导管"), lineNames);

        @SuppressWarnings("unchecked")
        List<Object> analyses = readRequiredField(preview, "workOrderAnalyses", List.class);
        assertEquals(1, analyses.size());
        assertEquals("球囊扩张导管", readRequiredField(analyses.get(0), "lineName", String.class));
    }

    @Test
    void preview_shouldUseResourceCapacityAndAggregateParallelWorkstationsForDurationAndBottleneck() {
        workOrder.setQuantity(new BigDecimal("99"));
        routeProcess = MesProRouteProcessDO.builder()
                .id(3L)
                .routeId(20L)
                .processId(300L)
                .sort(1)
                .prepareTime(0)
                .waitTime(0)
                .colorCode("#1677ff")
                .build();
        MesProRouteProcessDO secondProcess = MesProRouteProcessDO.builder()
                .id(4L)
                .routeId(20L)
                .processId(301L)
                .sort(2)
                .prepareTime(0)
                .waitTime(0)
                .colorCode("#52c41a")
                .build();
        MesProScheduleOrderProcessDO secondScheduleProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(501L)
                .routeVersionId(700L)
                .routeProcessId(4L)
                .predecessorRouteProcessId(3L)
                .rootProcessFlag(Boolean.FALSE)
                .processId(301L)
                .sort(2)
                .build();
        MesMdWorkstationDO line1Process1a = MesMdWorkstationDO.builder()
                .id(30L).name("L1-P1-A").processId(300L).productionLineId(40L).status(0).singleStandardHourlyCapacity(new BigDecimal("50")).build();
        MesMdWorkstationDO line1Process1b = MesMdWorkstationDO.builder()
                .id(31L).name("L1-P1-B").processId(300L).productionLineId(40L).status(0).singleStandardHourlyCapacity(new BigDecimal("49")).build();
        MesMdWorkstationDO line1Process2 = MesMdWorkstationDO.builder()
                .id(32L).name("L1-P2").processId(301L).productionLineId(40L).status(0).singleStandardHourlyCapacity(new BigDecimal("25")).build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(routeProcess, secondProcess));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L))
                .thenReturn(List.of(scheduleOrderProcess, secondScheduleProcess));
        when(processService.getProcessMap(any())).thenReturn(Map.of(
                300L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder().id(300L).name("P1").build(),
                301L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder().id(301L).name("P2").build()
        ));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(line1Process1a, line1Process1b, line1Process2));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any())).thenReturn(Map.of(
                30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("50")),
                31L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("49")),
                32L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("25"))
        ));
        stubScheduleOrderProcessesWithParallelRoots(buildScheduleOrderProcessSnapshot(602L, 4L, 301L, 2));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        var generatedTasks = preview.getTasks().stream()
                .filter(task -> Integer.valueOf(303).equals(task.getType()))
                .toList();
        assertEquals(2, generatedTasks.size());
        Object firstProcessTask = generatedTasks.stream()
                .filter(task -> task.getId().contains("_300_"))
                .findFirst()
                .orElseThrow();
        Object secondProcessTask = generatedTasks.stream()
                .filter(task -> task.getId().contains("_301_"))
                .findFirst()
                .orElseThrow();
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0), generatedTasks.get(0).getStartDate());
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0),
                readRequiredField(firstProcessTask, "startDate", LocalDateTime.class));
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0),
                readRequiredField(secondProcessTask, "startDate", LocalDateTime.class));
        assertEquals(LocalDateTime.of(2026, 5, 14, 9, 0),
                readRequiredField(firstProcessTask, "endDate", LocalDateTime.class));
        assertEquals(LocalDateTime.of(2026, 5, 14, 11, 58),
                readRequiredField(secondProcessTask, "endDate", LocalDateTime.class));
        assertEquals(0, preview.getLinks().size());

        @SuppressWarnings("unchecked")
        List<Object> analyses = readRequiredField(preview, "workOrderAnalyses", List.class);
        assertEquals(1, analyses.size());
    }

    @Test
    void preview_shouldScheduleDifferentProcessesInParallelByProcessCapacity() {
        MesProRouteProcessDO secondProcess = MesProRouteProcessDO.builder()
                .id(4L)
                .routeId(20L)
                .processId(301L)
                .sort(2)
                .prepareTime(0)
                .waitTime(0)
                .colorCode("#52c41a")
                .build();
        MesProScheduleOrderProcessDO secondScheduleProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(501L)
                .routeVersionId(700L)
                .routeProcessId(4L)
                .predecessorRouteProcessId(3L)
                .rootProcessFlag(Boolean.FALSE)
                .processId(301L)
                .sort(2)
                .build();
        MesMdWorkstationDO processOneStation = MesMdWorkstationDO.builder()
                .id(30L).name("L1-P1").processId(300L).productionLineId(40L).status(0).build();
        MesMdWorkstationDO processTwoStation = MesMdWorkstationDO.builder()
                .id(31L).name("L1-P2").processId(301L).productionLineId(40L).status(0).build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(routeProcess, secondProcess));
        lenient().when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L))
                .thenReturn(List.of(scheduleOrderProcess, secondScheduleProcess));
        when(processService.getProcessMap(any())).thenReturn(Map.of(
                300L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder().id(300L).name("P1").build(),
                301L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder().id(301L).name("P2").build()
        ));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(processOneStation, processTwoStation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any())).thenReturn(Map.of(
                30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE),
                31L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)
        ));
        stubScheduleOrderProcessesWithParallelRoots(buildScheduleOrderProcessSnapshot(602L, 4L, 301L, 2));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        var generatedTasks = preview.getTasks().stream()
                .filter(task -> Integer.valueOf(303).equals(task.getType()))
                .toList();
        assertEquals(2, generatedTasks.size());
        Object firstProcessTask = generatedTasks.stream()
                .filter(task -> task.getId().contains("_300_"))
                .findFirst()
                .orElseThrow();
        Object secondProcessTask = generatedTasks.stream()
                .filter(task -> task.getId().contains("_301_"))
                .findFirst()
                .orElseThrow();
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0),
                readRequiredField(firstProcessTask, "startDate", LocalDateTime.class));
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0),
                readRequiredField(secondProcessTask, "startDate", LocalDateTime.class));
        assertEquals(LocalDateTime.of(2026, 5, 14, 9, 0),
                readRequiredField(firstProcessTask, "endDate", LocalDateTime.class));
        assertEquals(LocalDateTime.of(2026, 5, 14, 9, 0),
                readRequiredField(secondProcessTask, "endDate", LocalDateTime.class));
        assertEquals(0, preview.getLinks().size());
    }

    @Test
    void preview_shouldAllocateWholeUnitQuantitiesWhenFiniteProcessSpansMultipleWindows() {
        workOrder.setQuantity(new BigDecimal("233"));
        routeProcess.setPrepareTime(0);
        routeProcess.setWaitTime(0);
        MesProCapacityPlanDO nextDayCapacityPlan = MesProCapacityPlanDO.builder()
                .id(71L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0))
                .shiftId(11L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(productionMaterialListMapper.selectListByWorkOrderIds(any()))
                .thenReturn(buildProductionMaterialListRows(new BigDecimal("233")));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any()))
                .thenReturn(List.of(capacityPlan, nextDayCapacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("25"))));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        var generatedTasks = preview.getTasks().stream()
                .filter(task -> Integer.valueOf(303).equals(task.getType()))
                .sorted(Comparator.comparing(task -> task.getStartDate()))
                .toList();
        assertEquals(2, generatedTasks.size());
        assertEquals(0, new BigDecimal("200").compareTo(generatedTasks.get(0).getQuantity()));
        assertEquals(0, new BigDecimal("33").compareTo(generatedTasks.get(1).getQuantity()));
        assertEquals(0, generatedTasks.stream()
                .map(task -> task.getQuantity().stripTrailingZeros().scale())
                .filter(scale -> scale > 0)
                .count());
        BigDecimal totalQuantity = generatedTasks.stream()
                .map(task -> task.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("233").compareTo(totalQuantity));
    }

    @Test
    void preview_shouldRoundFractionalRemainingProcessQuantityToWholeUnits() {
        workOrder.setQuantity(new BigDecimal("0.5"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("0.500000"));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(productionMaterialListMapper.selectListByWorkOrderIds(any()))
                .thenReturn(buildProductionMaterialListRows(BigDecimal.ONE));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        var generatedTask = preview.getTasks().stream()
                .filter(task -> Integer.valueOf(303).equals(task.getType()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, BigDecimal.ONE.compareTo(generatedTask.getQuantity()));
        assertTrue(generatedTask.getQuantity().stripTrailingZeros().scale() <= 0);
    }

    @Test
    void preview_shouldBlockWhenWorkerDrivenProcessHasNoConfiguredWorkers() {
        workOrder.setQuantity(new BigDecimal("99"));
        workstation.setSingleStandardHourlyCapacity(new BigDecimal("30"));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(0, 0, BigDecimal.ZERO, BigDecimal.ZERO)));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        assertEquals(1, preview.getSummary().getBlockingIssueCount());
        assertEquals(0, preview.getSummary().getGeneratedTaskCount());
        assertEquals("无设备且配置人数为0", preview.getIssues().get(0).getMessage());
    }

    @Test
    void replanPreview_shouldUseDayCapacityForNightShiftWhenNightShiftEnabled() {
        workOrder.setQuantity(new BigDecimal("60"));
        MesCalPlanShiftDO nightShift = MesCalPlanShiftDO.builder()
                .id(12L)
                .planId(50L)
                .sort(2)
                .name("NIGHT")
                .startTime("16:00")
                .endTime("23:59")
                .build();
        scheduleOrderProcess.setRouteProcessId(routeProcess.getId());
        scheduleOrderProcess.setNightShiftEnabled(Boolean.TRUE);
        MesProCapacityPlanDO nextDayCapacityPlan = MesProCapacityPlanDO.builder()
                .id(71L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0))
                .shiftId(11L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();

        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift, nightShift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any()))
                .thenReturn(List.of(capacityPlan, nextDayCapacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("5"))));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertTrue(preview.getIssues().isEmpty());
        var generatedTasks = preview.getTasks().stream()
                .filter(task -> readRequiredStringField(task, "id").contains("_preview_"))
                .toList();
        assertEquals(2, generatedTasks.size());
        assertTrue(generatedTasks.stream()
                .anyMatch(task -> LocalDateTime.of(2026, 5, 14, 16, 0).equals(
                        readRequiredField(task, "startDate", LocalDateTime.class))
                        && readRequiredField(task, "endDate", LocalDateTime.class)
                        .isAfter(LocalDateTime.of(2026, 5, 14, 16, 0))));
    }

    @Test
    void replanPreview_shouldUseRemainingSnapshotProcessWhenRouteProcessChangedProcessId() {
        workOrder.setQuantity(new BigDecimal("2"));
        MesProRouteProcessDO changedRouteProcess = MesProRouteProcessDO.builder()
                .id(3L)
                .routeId(20L)
                .processId(301L)
                .sort(1)
                .prepareTime(0)
                .waitTime(0)
                .build();
        scheduleOrderProcess.setRouteProcessId(3L);
        scheduleOrderProcess.setProcessId(300L);
        scheduleOrderProcess.setProcessName("吹球囊成型");
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(new BigDecimal("0.01"));
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(changedRouteProcess));
        when(processService.getProcessMap(any())).thenReturn(Map.of(
                300L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(300L).name("吹球囊成型").build(),
                301L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(301L).name("吹球囊成型-新主数据").build()));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        var generatedTask = preview.getTasks().stream()
                .filter(task -> readRequiredStringField(task, "id").contains("_preview_"))
                .findFirst()
                .orElseThrow();
        assertEquals("WS-1", readRequiredField(generatedTask, "workstation", String.class));
        assertEquals("吹球囊成型", readRequiredField(generatedTask, "process", String.class));
    }

    @Test
    void replanPreview_shouldUseLiveBoundWorkstationWhenSnapshotProcessIdDrifted() {
        workOrder.setQuantity(new BigDecimal("2"));
        MesProRouteProcessDO changedRouteProcess = MesProRouteProcessDO.builder()
                .id(3L)
                .routeId(20L)
                .processId(301L)
                .workstationId(30L)
                .sort(1)
                .prepareTime(0)
                .waitTime(0)
                .build();
        scheduleOrderProcess.setRouteProcessId(3L);
        scheduleOrderProcess.setProcessId(300L);
        scheduleOrderProcess.setProcessName("吹球囊成型");
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(new BigDecimal("0.01"));
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        workstation.setProcessId(301L);
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(changedRouteProcess));
        when(workstationMapper.selectByIds(any())).thenReturn(List.of(workstation));
        when(processService.getProcessMap(any())).thenReturn(Map.of(
                300L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(300L).name("吹球囊成型").build(),
                301L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(301L).name("吹球囊成型-新主数据").build()));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        var generatedTask = preview.getTasks().stream()
                .filter(task -> readRequiredStringField(task, "id").contains("_preview_"))
                .findFirst()
                .orElseThrow();
        assertEquals("WS-1", readRequiredField(generatedTask, "workstation", String.class));
        assertEquals("吹球囊成型", readRequiredField(generatedTask, "process", String.class));
    }

    @Test
    void replanPreview_shouldAllowExplicitBoundWorkstationFromDifferentProcess() {
        workOrder.setQuantity(new BigDecimal("2"));
        MesProRouteProcessDO changedRouteProcess = MesProRouteProcessDO.builder()
                .id(3L)
                .routeId(20L)
                .processId(301L)
                .workstationId(30L)
                .sort(1)
                .prepareTime(0)
                .waitTime(0)
                .build();
        scheduleOrderProcess.setRouteProcessId(3L);
        scheduleOrderProcess.setProcessId(301L);
        scheduleOrderProcess.setProcessName("吹球囊成型-复用工作站");
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(new BigDecimal("0.01"));
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        workstation.setProcessId(302L);
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(changedRouteProcess));
        when(workstationMapper.selectByIds(any())).thenReturn(List.of(workstation));
        when(processService.getProcessMap(any())).thenReturn(Map.of(
                300L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(300L).name("吹球囊成型").build(),
                301L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(301L).name("吹球囊成型-复用工作站").build(),
                302L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(302L).name("原工作站工序").build()));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        var generatedTask = preview.getTasks().stream()
                .filter(task -> readRequiredStringField(task, "id").contains("_preview_"))
                .findFirst()
                .orElseThrow();
        assertEquals("WS-1", readRequiredField(generatedTask, "workstation", String.class));
        assertEquals("吹球囊成型-复用工作站", readRequiredField(generatedTask, "process", String.class));
    }

    @Test
    void replanApply_shouldUseStableRouteProcessIdWhenSnapshotProcessIdDrifted() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        workOrder.setQuantity(new BigDecimal("2"));
        routeProduct.setQuantity(2);
        MesProRouteProcessDO frozenRouteProcess = MesProRouteProcessDO.builder()
                .id(3L)
                .routeId(20L)
                .processId(300L)
                .sort(1)
                .prepareTime(0)
                .waitTime(0)
                .build();
        scheduleOrderProcess.setRouteProcessId(3L);
        scheduleOrderProcess.setProcessId(301L);
        scheduleOrderProcess.setProcessName("吹球囊成型");
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(BigDecimal.ONE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("2"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        workstation.setProcessId(301L);
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(frozenRouteProcess));
        lenient().doThrow(new IllegalStateException("无法解析当前工艺路线工序，routeId=20，sourceProcessId=301，routeProcessId=3，processCode=null"))
                .when(routeProcessService).resolveFrozenRouteProcess(3L, 20L, 301L);
        when(processService.getProcessMap(any())).thenReturn(Map.of(
                300L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(300L).name("吹球囊成型-路线当前").build(),
                301L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                        .id(301L).name("吹球囊成型").build()));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(autoCodeRecordService.generateAutoCode(anyString())).thenReturn("PT-DRIFT-001");
        doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            task.setId(960L + storedTasks.size());
            storedTasks.add(task);
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));

        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());
        var response = autoScheduleService.replanApply(reqVO);

        assertTrue(response.getApplied());
        assertEquals(List.of(960L), response.getCreatedTaskIds());
        assertEquals(301L, storedTasks.get(0).getProcessId());
        verify(routeProcessService, never()).resolveFrozenRouteProcess(3L, 20L, 301L);
    }

    @Test
    void resolveEffectiveHourlyCapacity_shouldUseManualOverrideWithoutResourcePool() {
        MesProScheduleOrderProcessDO manualOverrideProcess = MesProScheduleOrderProcessDO.builder()
                .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                .hourlyCapacityTotal(new BigDecimal("7.5"))
                .build();

        BigDecimal effectiveCapacity = ReflectionTestUtils.invokeMethod(autoScheduleService,
                "resolveEffectiveHourlyCapacity", manualOverrideProcess, null);

        assertEquals(0, new BigDecimal("7.5").compareTo(effectiveCapacity));
    }

    @Test
    void replanPreview_shouldExposeInvalidTopologySnapshotAsBlockingIssue() {
        scheduleOrderProcess.setRootProcessFlag(Boolean.FALSE);
        scheduleOrderProcess.setPredecessorRouteProcessId(null);

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertTrue(preview.getIssues().stream().anyMatch(issue ->
                "ROUTE".equals(issue.getIssueType())
                        && "BLOCKING".equals(issue.getSeverity())
                        && "排产工序拓扑快照无效，scheduleOrderId=501".equals(issue.getMessage())));
        assertEquals(0, preview.getSummary().getGeneratedTaskCount());
    }

    @Test
    void replanPreview_shouldAcceptHistoricalTopologySnapshotWhenCurrentRouteProcessIdsChanged() {
        workOrder.setQuantity(new BigDecimal("2"));
        routeProduct.setQuantity(2);
        MesProRouteProcessDO currentRoot = MesProRouteProcessDO.builder()
                .id(13L)
                .routeId(20L)
                .processId(300L)
                .sort(1)
                .prepareTime(0)
                .waitTime(0)
                .build();
        MesProRouteProcessDO currentSecond = MesProRouteProcessDO.builder()
                .id(14L)
                .routeId(20L)
                .processId(301L)
                .sort(2)
                .prepareTime(0)
                .waitTime(0)
                .build();
        MesProScheduleOrderProcessDO historicalRoot = MesProScheduleOrderProcessDO.builder()
                .id(601L)
                .scheduleOrderId(501L)
                .routeVersionId(700L)
                .routeProcessId(3L)
                .predecessorRouteProcessId(null)
                .rootProcessFlag(Boolean.TRUE)
                .processId(0L)
                .processCode("Z2630")
                .processName("吹球囊成型")
                .sort(1)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("2"))
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("2"))
                .build();
        MesProScheduleOrderProcessDO historicalSecond = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(501L)
                .routeVersionId(700L)
                .routeProcessId(4L)
                .predecessorRouteProcessId(3L)
                .rootProcessFlag(Boolean.FALSE)
                .processId(301L)
                .processCode("Z3710")
                .processName("球囊裁剪")
                .sort(2)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("2"))
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("2"))
                .build();
        MesMdWorkstationDO secondWorkstation = MesMdWorkstationDO.builder()
                .id(31L)
                .name("WS-2")
                .processId(301L)
                .productionLineId(40L)
                .status(0)
                .build();
        when(routeProcessService.getRouteProcessListByRouteId(20L))
                .thenReturn(List.of(currentRoot, currentSecond));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(anyLong()))
                .thenReturn(List.of(historicalRoot, historicalSecond));
        doReturn(currentRoot).when(routeProcessService).resolveCurrentRouteProcess(eq(3L), eq(20L), isNull());
        doReturn(currentSecond).when(routeProcessService).resolveCurrentRouteProcess(4L, 20L, 301L);
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation, secondWorkstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any())).thenReturn(Map.of(
                30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE),
                31L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertFalse(preview.getIssues().stream().anyMatch(issue ->
                "排产工序拓扑快照无效，scheduleOrderId=501".equals(issue.getMessage())));
        List<String> generatedTaskIds = preview.getTasks().stream()
                .map(task -> readRequiredStringField(task, "id"))
                .filter(id -> id.contains("_preview_"))
                .toList();
        assertTrue(generatedTaskIds.stream().anyMatch(id -> id.contains("_300_")));
        assertTrue(generatedTaskIds.stream().anyMatch(id -> id.contains("_301_")));
    }

    @Test
    void replanPreview_shouldScheduleRemainingProcessesWhenSnapshotPredecessorIsReportedFull() {
        workOrder.setQuantity(new BigDecimal("2"));
        routeProduct.setQuantity(2);
        MesProRouteProcessDO secondRouteProcess = MesProRouteProcessDO.builder()
                .id(4L)
                .routeId(20L)
                .processId(301L)
                .sort(2)
                .prepareTime(0)
                .waitTime(0)
                .colorCode("#13c2c2")
                .build();
        MesMdWorkstationDO secondWorkstation = MesMdWorkstationDO.builder()
                .id(31L)
                .name("WS-2")
                .processId(301L)
                .productionLineId(40L)
                .status(0)
                .build();
        MesProScheduleOrderProcessDO fullPredecessor = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(501L)
                .routeVersionId(700L)
                .routeProcessId(3L)
                .predecessorRouteProcessId(null)
                .rootProcessFlag(Boolean.TRUE)
                .processId(300L)
                .sort(1)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("2"))
                .reportedQuantity(new BigDecimal("2"))
                .remainingQuantity(BigDecimal.ZERO)
                .build();
        MesProScheduleOrderProcessDO remainingSuccessor = MesProScheduleOrderProcessDO.builder()
                .id(603L)
                .scheduleOrderId(501L)
                .routeVersionId(700L)
                .routeProcessId(4L)
                .predecessorRouteProcessId(3L)
                .rootProcessFlag(Boolean.FALSE)
                .processId(301L)
                .sort(2)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("2"))
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal("2"))
                .build();
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(routeProcess, secondRouteProcess));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L))
                .thenReturn(List.of(fullPredecessor, remainingSuccessor));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation, secondWorkstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(
                        30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE),
                        31L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, BigDecimal.ONE)));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertTrue(preview.getTasks().stream().anyMatch(task ->
                readRequiredStringField(task, "id").contains("_preview_")
                        && "WS-2".equals(readRequiredField(task, "workstation", String.class))));
    }

    @Test
    void replanPreview_shouldKeepChronologicalOrderWhenNightShiftEnabled() {
        workOrder.setQuantity(new BigDecimal("12"));
        shift.setSort(1);
        MesCalPlanShiftDO middleShift = MesCalPlanShiftDO.builder()
                .id(12L)
                .planId(50L)
                .sort(2)
                .name("中班")
                .startTime("16:00")
                .endTime("20:00")
                .build();
        MesCalPlanShiftDO nightShift = MesCalPlanShiftDO.builder()
                .id(13L)
                .planId(50L)
                .sort(3)
                .name("夜班")
                .startTime("20:00")
                .endTime("08:00")
                .build();
        MesProCapacityPlanDO middleCapacityPlan = MesProCapacityPlanDO.builder()
                .id(71L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .shiftId(12L)
                .capacityMinutes(240)
                .enabled(Boolean.TRUE)
                .build();
        MesProCapacityPlanDO nightCapacityPlan = MesProCapacityPlanDO.builder()
                .id(72L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .shiftId(13L)
                .capacityMinutes(720)
                .enabled(Boolean.TRUE)
                .build();
        scheduleOrderProcess.setNightShiftEnabled(Boolean.TRUE);
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("12"));

        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift, middleShift, nightShift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any()))
                .thenReturn(List.of(capacityPlan, middleCapacityPlan, nightCapacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount());
        assertTrue(preview.getTasks().stream()
                .filter(task -> readRequiredStringField(task, "id").contains("_preview_"))
                .anyMatch(task -> LocalDateTime.of(2026, 5, 14, 20, 0).equals(
                        readRequiredField(task, "startDate", LocalDateTime.class))));
    }

    @Test
    void replanPreview_shouldRefreshNightShiftFromRouteConfigWhenScheduleOrderSnapshotIsStale() {
        workOrder.setQuantity(new BigDecimal("12"));
        routeProcess.setWorkstationId(30L);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(new BigDecimal("12"));
        shift.setSort(1);
        MesCalPlanShiftDO middleShift = MesCalPlanShiftDO.builder()
                .id(12L)
                .planId(50L)
                .sort(2)
                .name("中班")
                .startTime("16:00")
                .endTime("20:00")
                .build();
        MesCalPlanShiftDO nightShift = MesCalPlanShiftDO.builder()
                .id(13L)
                .planId(50L)
                .sort(3)
                .name("夜班")
                .startTime("20:00")
                .endTime("08:00")
                .build();
        MesProCapacityPlanDO middleCapacityPlan = MesProCapacityPlanDO.builder()
                .id(71L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .shiftId(12L)
                .capacityMinutes(240)
                .enabled(Boolean.TRUE)
                .build();
        MesProCapacityPlanDO nightCapacityPlan = MesProCapacityPlanDO.builder()
                .id(72L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .shiftId(13L)
                .capacityMinutes(720)
                .enabled(Boolean.TRUE)
                .build();
        scheduleOrder.setRouteVersionId(700L);
        scheduleOrderProcess.setRouteVersionId(null);
        scheduleOrderProcess.setNightShiftEnabled(Boolean.FALSE);
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("12"));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(700L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(9001L)
                        .routeVersionId(700L)
                        .itemId(100L)
                        .routeProcessId(3L)
                        .capacityMode(scheduleOrderProcess.getCapacityMode())
                        .hourlyCapacity(scheduleOrderProcess.getHourlyCapacityTotal())
                        .nightShiftEnabled(Boolean.TRUE)
                        .calendarRuleId(1L)
                        .remark("DAY_AND_NIGHT")
                        .build()));
        when(scheduleCalendarRuleMapper.selectById(1L)).thenReturn(MesProScheduleCalendarRuleDO.builder()
                .id(1L)
                .skipStatutoryHolidays(Boolean.FALSE)
                .weekendRestMode("NONE")
                .build());

        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift, middleShift, nightShift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any()))
                .thenReturn(List.of(capacityPlan, middleCapacityPlan, nightCapacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertTrue(scheduleOrderProcess.getNightShiftEnabled());
        assertEquals(9001L, scheduleOrderProcess.getRouteScheduleConfigId());
        assertEquals(0, preview.getSummary().getBlockingIssueCount());
        assertTrue(preview.getTasks().stream()
                .filter(task -> readRequiredStringField(task, "id").contains("_preview_"))
                .anyMatch(task -> LocalDateTime.of(2026, 5, 14, 20, 0).equals(
                        readRequiredField(task, "startDate", LocalDateTime.class))));
    }

    @Test
    void preview_shouldEnsurePlannedCapacityCoverageBeforeLoadingCapacityPlans() {
        workOrder.setQuantity(new BigDecimal("60"));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(
                capacityPlan,
                MesProCapacityPlanDO.builder()
                        .id(71L)
                        .lineId(40L)
                        .calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0))
                        .shiftId(11L)
                        .capacityMinutes(480)
                        .enabled(Boolean.TRUE)
                        .build()));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("5"))));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount());
        assertEquals(2, preview.getSummary().getGeneratedTaskCount());
        assertEquals(3, preview.getTasks().size());
        assertEquals(2L, preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK, readRequiredField(task, "type", Integer.class)))
                .count());
        InOrder inOrder = inOrder(scheduleCalendarService, capacityPlanMapper);
        inOrder.verify(scheduleCalendarService).ensureCapacityPlanCoverage(Set.of(40L),
                LocalDateTime.of(2026, 5, 14, 0, 0).toLocalDate(),
                LocalDateTime.of(2026, 5, 20, 0, 0).toLocalDate());
        inOrder.verify(capacityPlanMapper).selectListByLineIdsAndDate(any(), any());
        verify(scheduleCalendarService).ensureCapacityPlanCoverage(Set.of(40L),
                LocalDateTime.of(2026, 5, 14, 0, 0).toLocalDate(),
                LocalDateTime.of(2026, 5, 20, 0, 0).toLocalDate());
    }

    @Test
    void preview_shouldExtendPlannedLineCapacityWhenLoadedWindowsAreFull() {
        workOrder.setQuantity(new BigDecimal("120"));
        plan.setEndDate(LocalDateTime.of(2026, 5, 14, 0, 0));
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setPlannedQuantity(new BigDecimal("120"));
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("120"));
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setShiftCapacityTotal(new BigDecimal("100"));
        MesProCapacityPlanDO nextDayCapacityPlan = MesProCapacityPlanDO.builder()
                .id(71L)
                .lineId(40L)
                .calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0))
                .shiftId(11L)
                .capacityMinutes(480)
                .enabled(Boolean.TRUE)
                .build();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any()))
                .thenReturn(List.of(capacityPlan))
                .thenReturn(List.of(capacityPlan, nextDayCapacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        when(workstationCapacityService.getCapacityMetrics(any(), any()))
                .thenReturn(Map.of(30L, buildCapacityMetrics(1, 1, BigDecimal.ZERO, new BigDecimal("120"))));

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        List<GanttDataRespVO> generatedTasks = preview.getTasks().stream()
                .filter(task -> Objects.equals(MesBizTypeConstants.PRO_TASK, task.getType()))
                .toList();
        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(2, generatedTasks.size());
        assertEquals(new BigDecimal("100"), generatedTasks.get(0).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0), generatedTasks.get(0).getStartDate());
        assertEquals(new BigDecimal("20"), generatedTasks.get(1).getQuantity());
        assertEquals(LocalDateTime.of(2026, 5, 15, 8, 0), generatedTasks.get(1).getStartDate());
        verify(scheduleCalendarService).ensureCapacityPlanCoverage(Set.of(40L),
                LocalDate.of(2026, 5, 15), LocalDate.of(2026, 6, 13));
    }

    @Test
    void apply_shouldAcceptScheduleOrderIdsAndPersistTaskScheduleOrderRelation() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(autoCodeRecordService.generateAutoCode(anyString())).thenReturn("PT-003");
        doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            task.setId(901L + storedTasks.size());
            storedTasks.add(task);
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));

        MesProAutoSchedulePreviewReqVO previewReq = buildReq();
        previewReq.setWorkOrderIds(null);
        previewReq.setScheduleOrderIds(List.of(501L));
        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(previewReq);
        writeRequiredStringField(previewReq, "calendarContextToken", readRequiredStringField(preview, "calendarContextToken"));

        var response = autoScheduleService.apply(previewReq);

        assertTrue(response.getApplied());
        assertEquals(1, response.getCreatedTaskIds().size());
        ArgumentCaptor<MesProTaskScheduleExtDO> extCaptor = ArgumentCaptor.forClass(MesProTaskScheduleExtDO.class);
        verify(taskScheduleExtMapper).insert(extCaptor.capture());
        assertEquals(501L, extCaptor.getValue().getScheduleOrderId());
        assertEquals(601L, extCaptor.getValue().getScheduleOrderProcessId());
        assertEquals(List.of(1L), previewReq.getWorkOrderIds());
    }

    @Test
    void cancelNightShift_shouldDeleteNightTaskAndWriteResolvedIssue() {
        MesProTaskDO nightTask = MesProTaskDO.builder()
                .id(880L)
                .workOrderId(1L)
                .processId(300L)
                .workstationId(30L)
                .startTime(LocalDateTime.of(2026, 5, 14, 20, 0))
                .endTime(LocalDateTime.of(2026, 5, 15, 4, 0))
                .build();
        when(taskMapper.selectById(880L)).thenReturn(nightTask);
        when(scheduleIssueMapper.insert(any(MesProScheduleIssueDO.class))).thenAnswer(invocation -> {
            MesProScheduleIssueDO issue = invocation.getArgument(0);
            issue.setId(990L);
            return 1;
        });

        MesProAutoScheduleCancelNightShiftReqVO reqVO = new MesProAutoScheduleCancelNightShiftReqVO();
        reqVO.setTaskId(880L);
        reqVO.setReason("????????");

        Long issueId = autoScheduleService.cancelNightShift(reqVO);

        assertEquals(990L, issueId);
        verify(taskDependencyMapper).deleteByTaskIds(List.of(880L));
        verify(scheduleIssueMapper).deleteByTaskIds(List.of(880L));
        verify(taskScheduleExtMapper).deleteByTaskIds(List.of(880L));
        verify(taskMapper).deleteById(880L);

        ArgumentCaptor<MesProScheduleIssueDO> issueCaptor = ArgumentCaptor.forClass(MesProScheduleIssueDO.class);
        verify(scheduleIssueMapper).insert(issueCaptor.capture());
        assertEquals("MANUAL_NIGHT_SHIFT_CANCEL", issueCaptor.getValue().getIssueType());
        assertEquals("NIGHT_SHIFT_CANCEL", issueCaptor.getValue().getSourceType());
        assertEquals("RESOLVED", issueCaptor.getValue().getStatus());
        assertEquals(Boolean.TRUE, issueCaptor.getValue().getResolved());
        assertEquals("????????", issueCaptor.getValue().getResolutionReason());
    }

    @Test
    void preview_shouldFailFastWhenRequestedScheduleOrderIsFilteredOut() {
        MesProScheduleOrderDO filteredOut = MesProScheduleOrderDO.builder()
                .id(502L)
                .workOrderId(2L)
                .code("SCH-FROZEN")
                .erpWorkOrderCode("WO-FROZEN")
                .frozen(Boolean.TRUE)
                .autoSchedulable(Boolean.TRUE)
                .build();
        when(scheduleOrderMapper.selectByIds(List.of(501L, 502L))).thenReturn(List.of(scheduleOrder, filteredOut));
        when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L, 502L))).thenReturn(List.of(scheduleOrder));

        MesProAutoSchedulePreviewReqVO reqVO = buildReq();
        reqVO.setScheduleOrderIds(List.of(501L, 502L));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> autoScheduleService.preview(reqVO));
        assertTrue(ex.getMessage().contains("SCH-FROZEN") || ex.getMessage().contains("502"));
    }

    @Test
    void preview_shouldFailFastWhenRequestedScheduleOrderIsFinishedEvenIfMapperReturnsIt() {
        MesProScheduleOrderDO finishedOrder = MesProScheduleOrderDO.builder()
                .id(502L)
                .workOrderId(1L)
                .code("SCH-FINISHED")
                .erpWorkOrderCode("WO-FINISHED")
                .productId(100L)
                .routeId(20L)
                .routeVersionId(700L)
                .routeVersion("V1")
                .scheduleConfigVersion("V1")
                .frozen(Boolean.FALSE)
                .autoSchedulable(Boolean.TRUE)
                .status(MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                .build();
        when(scheduleOrderMapper.selectByIds(List.of(502L))).thenReturn(List.of(finishedOrder));
        when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(502L))).thenReturn(List.of(finishedOrder));

        MesProAutoSchedulePreviewReqVO reqVO = buildReq();
        reqVO.setScheduleOrderIds(List.of(502L));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> autoScheduleService.preview(reqVO));
        assertTrue(ex.getMessage().contains("SCH-FINISHED") || ex.getMessage().contains("已完成"));
    }

    @Test
    void preview_shouldIgnoreLegacyDisabledScheduleRouteFlow() {
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(new BigDecimal("8"));
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setPlannedQuantity(workOrder.getQuantity());
        scheduleOrderProcess.setRemainingQuantity(workOrder.getQuantity());
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(20L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(90000L)
                        .routeId(20L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.FALSE)
                        .build());
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoSchedulePreviewRespVO preview = autoScheduleService.preview(buildReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertTrue(preview.getSummary().getGeneratedTaskCount() > 0);
        assertTrue(preview.getTasks().stream()
                .anyMatch(task -> readRequiredStringField(task, "id").contains("_preview_")));
    }

    @Test
    void replanApply_shouldDeriveScheduleOrderRelationFromWorkOrderScope() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(autoCodeRecordService.generateAutoCode(anyString())).thenReturn("PT-004");
        doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            task.setId(920L + storedTasks.size());
            storedTasks.add(task);
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));

        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        autoScheduleService.replanApply(reqVO);

        ArgumentCaptor<MesProScheduleOrderPreflightReqVO> preflightCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderPreflightReqVO.class);
        verify(scheduleOrderService).preflight(preflightCaptor.capture());
        assertEquals(reqVO.getScheduleOrderIds(), preflightCaptor.getValue().getScheduleOrderIds());
        assertEquals(reqVO.getStartTime(), preflightCaptor.getValue().getStartTime());
        assertEquals(reqVO.getRuntimeCapacityBasis(), preflightCaptor.getValue().getCapacityMode());
        assertEquals(Boolean.FALSE, preflightCaptor.getValue().getIncludeAdmissionDiff());

        ArgumentCaptor<MesProTaskScheduleExtDO> extCaptor = ArgumentCaptor.forClass(MesProTaskScheduleExtDO.class);
        verify(taskScheduleExtMapper).insert(extCaptor.capture());
        assertEquals(501L, extCaptor.getValue().getScheduleOrderId());
        assertEquals(601L, extCaptor.getValue().getScheduleOrderProcessId());
    }

    @Test
    void replanPreview_shouldKeepFinishedTaskAndGenerateNewActiveTaskForRemainingQuantity() {
        MesProTaskDO finishedTask = MesProTaskDO.builder()
                .id(940L)
                .code("PT-FIN-001")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(new BigDecimal("0.5000"))
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 12, 0))
                .status(cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum.FINISHED.getStatus())
                .build();
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("0.500000"));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(finishedTask));
        when(taskScheduleExtMapper.selectListByTaskIds(any())).thenReturn(List.of(MesProTaskScheduleExtDO.builder()
                .taskId(940L)
                .scheduleOrderId(501L)
                .scheduleOrderProcessId(601L)
                .scheduleSource("AUTO")
                .locked(Boolean.FALSE)
                .riskStatus("NONE")
                .build()));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount());
        assertEquals(1, preview.getSummary().getPreservedTaskCount());
        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
        assertTrue(preview.getIssues().isEmpty());
        assertEquals(3, preview.getTasks().size());
        assertEquals(1, preview.getProtectedTasks().size());
        assertEquals(940L, preview.getProtectedTasks().get(0).getTaskId());
        assertEquals("FINISHED", preview.getProtectedTasks().get(0).getProtectionReason());
        long generatedTaskCount = preview.getTasks().stream()
                .filter(task -> task.getId().contains("_preview_"))
                .count();
        assertEquals(1L, generatedTaskCount);
        var generatedStep = preview.getTasks().stream()
                .filter(task -> task.getId().contains("_preview_"))
                .findFirst()
                .orElseThrow();
        assertEquals(0, BigDecimal.ONE.compareTo(generatedStep.getQuantity()));
    }

    @Test
    void replanPreview_shouldNotGenerateTaskWhenScheduleOrderProcessHasNoRemainingQuantity() {
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setCapacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode());
        scheduleOrderProcess.setHourlyCapacityTotal(BigDecimal.ONE);
        scheduleOrderProcess.setShiftHours(BigDecimal.ONE);
        scheduleOrderProcess.setPlannedQuantity(BigDecimal.ONE);
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ONE);
        scheduleOrderProcess.setRemainingQuantity(BigDecimal.ZERO);
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(0, preview.getSummary().getGeneratedTaskCount());
        assertTrue(preview.getTasks().stream()
                .noneMatch(task -> readRequiredStringField(task, "id").contains("_preview_")));
        assertTrue(preview.getIssues().isEmpty());
    }

    @Test
    void replanPreview_shouldLoadProtectedTaskLiveWorkstationLineWhenRouteSnapshotHasNoWorkstation() {
        routeProcess.setWorkstationId(null);
        scheduleOrderProcess.setEnabled(Boolean.TRUE);
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("0.500000"));
        scheduleOrderProcess.setPlannedQuantity(BigDecimal.ONE);
        scheduleOrderProcess.setReportedQuantity(new BigDecimal("0.500000"));
        MesProTaskDO feedbackTask = MesProTaskDO.builder()
                .id(960L)
                .code("PT-FEEDBACK-001")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(new BigDecimal("0.5000"))
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 10, 0))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();

        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectByIds(any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(feedbackTask));
        when(feedbackMapper.selectListByTaskIds(any())).thenReturn(List.of(MesProFeedbackDO.builder()
                .id(961L)
                .taskId(960L)
                .status(1)
                .build()));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount());
        assertFalse(preview.getIssues().stream()
                .anyMatch(issue -> "受保护任务未绑定产线".equals(issue.getMessage())));
        assertEquals(1, preview.getSummary().getPreservedTaskCount());
        assertEquals(1, preview.getSummary().getGeneratedTaskCount());
    }

    @Test
    void replanPreview_shouldResolveProtectedTaskDeletedWorkstationToLiveBoundWorkstation() {
        routeProcess.setWorkstationId(30L);
        MesProTaskDO protectedTask = MesProTaskDO.builder()
                .id(970L)
                .code("PT-PROTECTED-DELETED-WS")
                .workOrderId(1L)
                .workstationId(900140L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 10, 0))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();

        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(Collections.emptyList());
        when(workstationMapper.selectByIds(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return ids.contains(30L) ? List.of(workstation) : Collections.emptyList();
        });
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(protectedTask));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertEquals(0, preview.getSummary().getBlockingIssueCount(), preview.getIssues().toString());
        assertEquals(1, preview.getSummary().getPreservedTaskCount());
        assertEquals(1, preview.getProtectedTasks().size());
        assertEquals(30L, preview.getProtectedTasks().get(0).getWorkstationId());
        assertEquals("WS-1", preview.getProtectedTasks().get(0).getWorkstationName());
        assertFalse(preview.getIssues().stream()
                .anyMatch(issue -> "受保护任务工作站不存在或已删除".equals(issue.getMessage())));
    }

    @Test
    void replanPreview_shouldKeepBlockingWhenProtectedTaskWorkstationCannotBeResolved() {
        routeProcess.setWorkstationId(null);
        MesProTaskDO protectedTask = MesProTaskDO.builder()
                .id(971L)
                .code("PT-PROTECTED-MISSING-WS")
                .workOrderId(1L)
                .workstationId(900999L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 10, 0))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();

        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(Collections.emptyList());
        when(workstationMapper.selectByIds(any())).thenReturn(Collections.emptyList());
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(protectedTask));

        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(buildReplanReq());

        assertTrue(preview.getSummary().getBlockingIssueCount() > 0);
        assertTrue(preview.getIssues().stream()
                .anyMatch(issue -> "受保护任务工作站不存在或已删除".equals(issue.getMessage())));
    }

    @Test
    void replanApply_shouldKeepFinishedTaskAndCreateNewActiveTaskForRemainingQuantity() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        MesProTaskDO finishedTask = MesProTaskDO.builder()
                .id(941L)
                .code("PT-FIN-002")
                .workOrderId(1L)
                .workstationId(30L)
                .routeId(20L)
                .processId(300L)
                .itemId(100L)
                .quantity(new BigDecimal("0.5000"))
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 12, 0))
                .status(cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum.FINISHED.getStatus())
                .build();
        scheduleOrderProcess.setRemainingQuantity(new BigDecimal("0.500000"));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> {
            List<MesProTaskDO> allTasks = new ArrayList<>();
            allTasks.add(finishedTask);
            allTasks.addAll(storedTasks);
            return allTasks;
        });
        when(taskScheduleExtMapper.selectListByTaskIds(any())).thenReturn(List.of(MesProTaskScheduleExtDO.builder()
                .taskId(941L)
                .scheduleOrderId(501L)
                .scheduleOrderProcessId(601L)
                .scheduleSource("AUTO")
                .locked(Boolean.FALSE)
                .riskStatus("NONE")
                .build()));
        when(autoCodeRecordService.generateAutoCode(anyString())).thenReturn("PT-REM-001");
        doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            task.setId(950L + storedTasks.size());
            storedTasks.add(task);
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));

        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        var response = autoScheduleService.replanApply(reqVO);

        assertTrue(response.getApplied());
        assertEquals(List.of(950L), response.getCreatedTaskIds());
        assertEquals(List.of(941L), response.getPreservedTaskIds());
        assertTrue(response.getDeletedTaskIds().isEmpty());

        ArgumentCaptor<MesProTaskDO> taskCaptor = ArgumentCaptor.forClass(MesProTaskDO.class);
        verify(taskMapper).insert(taskCaptor.capture());
        assertEquals(0, BigDecimal.ONE.compareTo(taskCaptor.getValue().getQuantity()));
        assertEquals(MesProTaskStatusEnum.PREPARE.getStatus(), taskCaptor.getValue().getStatus());

        ArgumentCaptor<MesProTaskScheduleExtDO> extCaptor = ArgumentCaptor.forClass(MesProTaskScheduleExtDO.class);
        verify(taskScheduleExtMapper).insert(extCaptor.capture());
        assertEquals(501L, extCaptor.getValue().getScheduleOrderId());
        assertEquals(601L, extCaptor.getValue().getScheduleOrderProcessId());
    }

    @Test
    void replanPreview_shouldKeepLatestStartRiskOrderSchedulableDuringReplan() {
        MesProWorkOrderDO deferredWorkOrder = MesProWorkOrderDO.builder()
                .id(2L)
                .code("WO-002")
                .productId(100L)
                .batchCode("BATCH-SCHEDULE-002")
                .quantity(BigDecimal.ONE)
                .clientId(10L)
                .build();
        MesProScheduleOrderDO deferredScheduleOrder = MesProScheduleOrderDO.builder()
                .id(502L)
                .workOrderId(2L)
                .code("SCH-002")
                .erpWorkOrderCode("WO-002")
                .productId(100L)
                .routeId(20L)
                .routeVersionId(700L)
                .promiseDate(LocalDateTime.of(2026, 5, 13, 0, 0).toLocalDate())
                .build();
        MesProScheduleOrderProcessDO deferredProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(502L)
                .routeVersionId(700L)
                .routeProcessId(3L)
                .predecessorRouteProcessId(null)
                .rootProcessFlag(Boolean.TRUE)
                .processId(300L)
                .sort(1)
                .remainingQuantity(BigDecimal.ONE)
                .plannedQuantity(BigDecimal.ONE)
                .reportedQuantity(BigDecimal.ZERO)
                .build();
        scheduleOrder.setPromiseDate(LocalDateTime.of(2026, 5, 15, 0, 0).toLocalDate());
        scheduleOrderProcess.setRemainingQuantity(BigDecimal.ONE);
        scheduleOrderProcess.setPlannedQuantity(BigDecimal.ONE);
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        when(scheduleOrderMapper.selectByIds(List.of(501L, 502L))).thenReturn(List.of(scheduleOrder, deferredScheduleOrder));
        when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L, 502L))).thenReturn(List.of(scheduleOrder, deferredScheduleOrder));
        when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(workOrder, deferredWorkOrder));
        when(workOrderService.getWorkOrderMap(any())).thenReturn(Map.of(
                1L, workOrder,
                2L, deferredWorkOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(scheduleOrderProcess));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(502L)).thenReturn(List.of(deferredProcess));
        when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                buildProductionMaterialListRow(1L, 200L, BigDecimal.ONE),
                buildProductionMaterialListRow(2L, 200L, BigDecimal.ONE)));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenReturn(Collections.emptyList());
        Map<Long, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO> processMap = new LinkedHashMap<>();
        processMap.put(300L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                .id(300L)
                .name("工序A")
                .build());
        when(processService.getProcessMap(any())).thenReturn(processMap);

        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setScheduleOrderIds(List.of(501L, 502L));
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);

        assertEquals(0, preview.getSummary().getBlockingIssueCount());
        assertEquals(2, preview.getSummary().getGeneratedTaskCount());
        assertEquals(4, preview.getTasks().size());
        assertTrue(preview.getIssues().stream()
                .anyMatch(issue -> "WO-002".equals(issue.getWorkOrderCode())
                        && "LATEST_START".equals(issue.getIssueType())
                        && "WARNING".equals(issue.getSeverity())));
        assertFalse(preview.getIssues().stream()
                .anyMatch(issue -> "WO-002".equals(issue.getWorkOrderCode())
                        && "ACTIVE_TASK".equals(issue.getIssueType())));
        assertEquals(1L, preview.getTasks().stream()
                .filter(task -> "301_1".equals(task.getParent()))
                .count());
        assertEquals(1L, preview.getTasks().stream()
                .filter(task -> "301_2".equals(task.getParent()))
                .count());
    }

    @Test
    void replanApply_shouldKeepLatestStartRiskOrderAsActiveTaskDuringReplan() {
        List<MesProTaskDO> storedTasks = new ArrayList<>();
        List<MesProTaskDO> insertedTasks = new ArrayList<>();
        MesProWorkOrderDO deferredWorkOrder = MesProWorkOrderDO.builder()
                .id(2L)
                .code("WO-002")
                .productId(100L)
                .batchCode("BATCH-SCHEDULE-002")
                .quantity(BigDecimal.ONE)
                .clientId(10L)
                .build();
        MesProScheduleOrderDO deferredScheduleOrder = MesProScheduleOrderDO.builder()
                .id(502L)
                .workOrderId(2L)
                .code("SCH-002")
                .erpWorkOrderCode("WO-002")
                .productId(100L)
                .routeId(20L)
                .routeVersionId(700L)
                .promiseDate(LocalDateTime.of(2026, 5, 13, 0, 0).toLocalDate())
                .build();
        MesProScheduleOrderProcessDO deferredProcess = MesProScheduleOrderProcessDO.builder()
                .id(602L)
                .scheduleOrderId(502L)
                .routeVersionId(700L)
                .routeProcessId(3L)
                .predecessorRouteProcessId(null)
                .rootProcessFlag(Boolean.TRUE)
                .processId(300L)
                .sort(1)
                .remainingQuantity(BigDecimal.ONE)
                .plannedQuantity(BigDecimal.ONE)
                .reportedQuantity(BigDecimal.ZERO)
                .build();
        scheduleOrder.setPromiseDate(LocalDateTime.of(2026, 5, 15, 0, 0).toLocalDate());
        scheduleOrderProcess.setRemainingQuantity(BigDecimal.ONE);
        scheduleOrderProcess.setPlannedQuantity(BigDecimal.ONE);
        scheduleOrderProcess.setReportedQuantity(BigDecimal.ZERO);
        when(scheduleOrderMapper.selectByIds(List.of(501L, 502L))).thenReturn(List.of(scheduleOrder, deferredScheduleOrder));
        when(scheduleOrderMapper.selectAutoSchedulableByIds(List.of(501L, 502L))).thenReturn(List.of(scheduleOrder, deferredScheduleOrder));
        when(workOrderService.getWorkOrderList(any())).thenReturn(List.of(workOrder, deferredWorkOrder));
        when(workOrderService.getWorkOrderMap(any())).thenReturn(Map.of(
                1L, workOrder,
                2L, deferredWorkOrder));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(scheduleOrderProcess));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(502L)).thenReturn(List.of(deferredProcess));
        when(productionMaterialListMapper.selectListByWorkOrderIds(any())).thenReturn(List.of(
                buildProductionMaterialListRow(1L, 200L, BigDecimal.ONE),
                buildProductionMaterialListRow(2L, 200L, BigDecimal.ONE)));
        when(materialStockMapper.selectListByItemIds(any())).thenReturn(List.of(stock));
        when(workstationMapper.selectListByProcessIds(any(), any())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(any())).thenReturn(Map.of(40L, productionLine));
        when(planService.getPlanMap(any())).thenReturn(Map.of(50L, plan));
        when(planShiftService.getPlanShiftListByPlanId(50L)).thenReturn(List.of(shift));
        when(capacityPlanMapper.selectListByLineIdsAndDate(any(), any())).thenReturn(List.of(capacityPlan));
        when(taskMapper.selectListByWorkOrderIds(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(autoCodeRecordService.generateAutoCode(anyString()))
                .thenReturn("PT-001", "PT-002");
        doAnswer(invocation -> {
            MesProTaskDO task = invocation.getArgument(0);
            task.setId(950L + insertedTasks.size());
            insertedTasks.add(task);
            storedTasks.add(task);
            return 1;
        }).when(taskMapper).insert(any(MesProTaskDO.class));
        Map<Long, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO> processMap = new LinkedHashMap<>();
        processMap.put(300L, cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder()
                .id(300L)
                .name("工序A")
                .build());
        when(processService.getProcessMap(any())).thenReturn(processMap);
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setScheduleOrderIds(List.of(501L, 502L));
        MesProAutoScheduleReplanPreviewRespVO preview = autoScheduleService.replanPreview(reqVO);
        reqVO.setCalendarContextToken(preview.getCalendarContextToken());

        var response = autoScheduleService.replanApply(reqVO);

        assertTrue(response.getApplied());
        assertEquals(List.of(950L, 951L), response.getCreatedTaskIds());
        assertEquals(2, insertedTasks.size());
        assertEquals(Set.of(1L, 2L), insertedTasks.stream()
                .map(MesProTaskDO::getWorkOrderId)
                .collect(Collectors.toSet()));

        ArgumentCaptor<MesProTaskScheduleExtDO> extCaptor = ArgumentCaptor.forClass(MesProTaskScheduleExtDO.class);
        verify(taskScheduleExtMapper, times(2)).insert(extCaptor.capture());
        assertEquals(Set.of(501L, 502L), extCaptor.getAllValues().stream()
                .map(MesProTaskScheduleExtDO::getScheduleOrderId)
                .collect(Collectors.toSet()));

        verify(scheduleIssueMapper).deleteByWorkOrderIds(any());
        verify(scheduleIssueMapper).insertBatch(any());
    }

    private MesProAutoSchedulePreviewReqVO buildReq() {
        MesProAutoSchedulePreviewReqVO reqVO = new MesProAutoSchedulePreviewReqVO();
        reqVO.setScheduleOrderIds(List.of(501L));
        reqVO.setStartTime(LocalDateTime.of(2026, 5, 14, 8, 0));
        reqVO.setRuntimeCapacityBasis("PLANNED");
        reqVO.setPreserveManualLockedTasks(true);
        reqVO.setReason("业务原因：发布排产");
        return reqVO;
    }

    private MesProAutoScheduleReplanReqVO buildReplanReq() {
        MesProAutoScheduleReplanReqVO reqVO = new MesProAutoScheduleReplanReqVO();
        reqVO.setScheduleOrderIds(List.of(501L));
        reqVO.setStartTime(LocalDateTime.of(2026, 5, 14, 8, 0));
        reqVO.setRuntimeCapacityBasis("PLANNED");
        reqVO.setPreserveManualLockedTasks(true);
        reqVO.setCalendarContextToken("token-1");
        reqVO.setReason("业务原因：手动重排");
        return reqVO;
    }

    private MesProScheduleCalendarRulesRespVO buildCalendarRules(String simulationCurrentDate, Map<String, String> dateShiftModeByDate) {
        MesProScheduleCalendarRulesRespVO rulesRespVO = new MesProScheduleCalendarRulesRespVO();
        rulesRespVO.setSkipStatutoryHolidays(false);
        rulesRespVO.setWeekendRestMode("NONE");
        rulesRespVO.setDateShiftModeByDate(new LinkedHashMap<>(dateShiftModeByDate));
        rulesRespVO.setSimulationCurrentDate(simulationCurrentDate);
        return rulesRespVO;
    }

    private MesProScheduleOrderPreflightRespVO passPreflightResp() {
        MesProScheduleOrderPreflightRespVO respVO = new MesProScheduleOrderPreflightRespVO();
        respVO.setResult("PASS");
        respVO.setSummary(new MesProScheduleOrderPreflightSummaryRespVO());
        return respVO;
    }

    private List<MesKingdeeProductionMaterialListDO> buildProductionMaterialListRows(BigDecimal quantity) {
        return List.of(buildProductionMaterialListRow(1L, 200L, quantity));
    }

    private MesKingdeeProductionMaterialListDO buildProductionMaterialListRow(Long workOrderId,
                                                                              Long childMaterialId,
                                                                              BigDecimal quantity) {
        return MesKingdeeProductionMaterialListDO.builder()
                .workOrderId(workOrderId)
                .workOrderCode("WO-" + String.format("%03d", workOrderId))
                .childMaterialId(childMaterialId)
                .childMaterialCode("ITEM-BOM")
                .childMaterialName("物料B")
                .requiredQuantity(quantity)
                .build();
    }

    private MesMdWorkstationCapacityMetrics buildCapacityMetrics(int configuredWorkerCount,
                                                                 int currentWorkerCount,
                                                                 BigDecimal machineryStandardHourlyCapacity,
                                                                 BigDecimal effectiveHourlyCapacity) {
        return MesMdWorkstationCapacityMetrics.builder()
                .configuredWorkerCount(configuredWorkerCount)
                .currentWorkerCount(currentWorkerCount)
                .machineryStandardHourlyCapacity(machineryStandardHourlyCapacity)
                .todayCapacity(effectiveHourlyCapacity)
                .build();
    }

    private String readRequiredStringField(Object target, String fieldName) {
        return readRequiredField(target, fieldName, String.class);
    }

    private <T> T readRequiredField(Object target, String fieldName, Class<T> type) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            assertNotNull(value, "Expected field '" + fieldName + "' to be populated");
            return type.cast(value);
        } catch (NoSuchFieldException e) {
            fail("Expected field '" + fieldName + "' on " + target.getClass().getSimpleName());
            return null;
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private void writeRequiredStringField(Object target, String fieldName, String value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException e) {
            fail("Expected field '" + fieldName + "' on " + target.getClass().getSimpleName());
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

}
