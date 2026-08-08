package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_PROCESS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlinePqcContextServiceTest {

    private static final Long LOGIN_USER_ID = 8001L;
    private static final Long WORK_ORDER_ID = 1001L;
    private static final Long ROUTE_ID = 2001L;
    private static final Long PRODUCT_ID = 3001L;
    private static final Long ROUTE_PROCESS_ID = 4001L;
    private static final Long PROCESS_ID = 5001L;
    private static final Long ACTIVE_ORDER_ID = 6001L;
    private static final Long PQC_TASK_ID = 7001L;
    private static final Long PRODUCTION_SUBMIT_EVENT_ID = 9101L;
    private static final Long REGULATION_VERSION_ID = 8001L;
    private static final Long DEVICE_ACCOUNT_ID = 9201L;
    private static final Long DEVICE_ID = 9301L;
    private static final Long WORKSTATION_ID = 6001L;
    private static final String PQC_SOURCE_TYPE = "MES_PQC_INSPECTION_TASK";
    private static final String PQC_IDEMPOTENCY_KEY = "P0-PQC-20260803-0001";

    @Mock
    private MesProProcessPoolMapper processPoolMapper;
    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper activeOrderProcessSnapshotMapper;
    @Mock
    private MesProProcessPoolEventMapper processPoolEventMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesQaInspectionRegulationMapper regulationMapper;
    @Mock
    private MesQaInspectionRegulationVersionMapper versionMapper;
    @Mock
    private MesQaInspectionRegulationItemMapper regulationItemMapper;
    @Mock
    private MesQaInspectionRegulationItemEquipmentMapper regulationItemEquipmentMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock
    private MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private MesFrontlineTemplateResolver templateResolver;
    @Mock
    private MesProcessPoolEventService processPoolEventService;
    @Mock
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    @Mock
    private MesProBatchRecordExecutionSignatureService signatureService;

    private MesFrontlinePqcContextService service;
    private final List<MesPqcInspectionTaskDO> pqcTaskContextFixtures = new ArrayList<>();
    private final List<MesProcessPoolActiveOrderProcessSnapshotDO> processSnapshotFixtures = new ArrayList<>();
    private final List<MesQaInspectionRegulationDO> publishedRegulationFixtures = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new MesFrontlinePqcContextServiceImpl(activeOrderMapper, activeOrderProcessSnapshotMapper,
                processPoolMapper, processPoolEventMapper, workOrderMapper, routeMapper, routeProductMapper, routeProcessMapper,
                regulationMapper, versionMapper, regulationItemMapper, regulationItemEquipmentMapper, pqcTaskMapper,
                pqcPieceDetailMapper,
                processService, itemService, scopeMapper, adminUserApi, templateResolver, processPoolEventService,
                pqcRecordMapper, signatureService);
        lenient().when(pqcTaskMapper.selectActiveOrderIdsByTaskStatus(anyCollection(), eq("PENDING")))
                .thenReturn(Set.of(ACTIVE_ORDER_ID));
        processSnapshotFixtures.add(processSnapshot(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID));
        lenient().when(activeOrderProcessSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID))
                .thenReturn(processSnapshotFixtures);
        lenient().when(regulationMapper.selectPublishedListByProductRouteVersion(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Long productId = invocation.getArgument(0);
                    Long routeId = invocation.getArgument(1);
                    Long routeVersionId = invocation.getArgument(2);
                    return publishedRegulationFixtures.stream()
                            .filter(regulation -> Objects.equals(productId, regulation.getProductId()))
                            .filter(regulation -> Objects.equals(routeId, regulation.getRouteId()))
                            .filter(regulation -> Objects.equals(routeVersionId, regulation.getRouteVersionId()))
                            .toList();
                });
        lenient().when(processPoolEventMapper.selectByIdForUpdate(PRODUCTION_SUBMIT_EVENT_ID))
                .thenReturn(productionSubmitEvent(PRODUCTION_SUBMIT_EVENT_ID, ROUTE_PROCESS_ID, PROCESS_ID));
    }

    @Test
    void shouldListActiveOrdersFromUnifiedActiveOrderAuthority() {
        when(activeOrderMapper.selectActiveList()).thenReturn(List.of(
                activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)),
                activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 9, 0))));
        givenWorkOrderProductAndRoute();

        List<MesFrontlineActiveOrderCandidate> orders = service.listActiveOrders();

        verify(processPoolMapper, never()).selectActiveList();
        assertEquals(1, orders.size());
        assertEquals(WORK_ORDER_ID, orders.get(0).workOrderId());
        assertEquals("WO-PQC-001", orders.get(0).workOrderCode());
        assertEquals(PRODUCT_ID, orders.get(0).productId());
        assertEquals("PQC 产品", orders.get(0).productName());
        assertEquals(new BigDecimal("125.500"), orders.get(0).quantity());
        assertEquals(ROUTE_ID, orders.get(0).routeId());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), orders.get(0).latestSubmitTime());
    }

    @Test
    void shouldRejectActiveOrderWithoutProductionQuantity() {
        when(activeOrderMapper.selectActiveList()).thenReturn(List.of(
                activeOrder(WORK_ORDER_ID, ROUTE_ID, LocalDateTime.of(2026, 8, 1, 8, 0))));
        MesProWorkOrderDO workOrder = workOrder(WORK_ORDER_ID, PRODUCT_ID);
        workOrder.setQuantity(null);
        givenWorkOrderProductAndRoute(workOrder, "PQC 产品");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.listActiveOrders());

        assertEquals(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("quantity"));
    }

    @Test
    void shouldExcludeActiveOrderWithoutPendingPqcTask() {
        when(activeOrderMapper.selectActiveList()).thenReturn(List.of(
                activeOrder(WORK_ORDER_ID, ROUTE_ID, LocalDateTime.of(2026, 8, 1, 8, 0))));
        when(pqcTaskMapper.selectActiveOrderIdsByTaskStatus(Set.of(ACTIVE_ORDER_ID), "PENDING"))
                .thenReturn(Set.of());
        lenient().when(workOrderMapper.selectListByIds(Set.of(WORK_ORDER_ID)))
                .thenReturn(List.of(workOrder(WORK_ORDER_ID, PRODUCT_ID)));
        lenient().when(itemService.getItemMap(Set.of(PRODUCT_ID))).thenReturn(Map.of(PRODUCT_ID,
                MesMdItemDO.builder().id(PRODUCT_ID).code("ITEM-PQC").name("PQC 产品").build()));
        lenient().when(routeMapper.selectListByIdsIgnoreDeleted(Set.of(ROUTE_ID)))
                .thenReturn(List.of(route(ROUTE_ID)));
        lenient().when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());

        List<MesFrontlineActiveOrderCandidate> orders = service.listActiveOrders();

        assertTrue(orders.isEmpty());
        verify(workOrderMapper, never()).selectListByIds(anyCollection());
        verify(routeMapper, never()).selectListByIdsIgnoreDeleted(anyCollection());
    }

    @Test
    void shouldReturnEmptyActiveOrderListWhenNoActiveOrderExists() {
        when(activeOrderMapper.selectActiveList()).thenReturn(List.of());

        List<MesFrontlineActiveOrderCandidate> orders = service.listActiveOrders();

        assertTrue(orders.isEmpty());
        verify(pqcTaskMapper, never()).selectActiveOrderIdsByTaskStatus(anyCollection(), eq("PENDING"));
    }

    @Test
    void shouldRejectActiveOrderWithNonPositiveProductionQuantity() {
        when(activeOrderMapper.selectActiveList()).thenReturn(List.of(
                activeOrder(WORK_ORDER_ID, ROUTE_ID, LocalDateTime.of(2026, 8, 1, 8, 0))));
        MesProWorkOrderDO workOrder = workOrder(WORK_ORDER_ID, PRODUCT_ID);
        workOrder.setQuantity(BigDecimal.ZERO);
        givenWorkOrderProductAndRoute(workOrder, "PQC 产品");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.listActiveOrders());

        assertEquals(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("quantity"));
    }

    @Test
    void shouldRejectActiveOrderWithoutProductName() {
        when(activeOrderMapper.selectActiveList()).thenReturn(List.of(
                activeOrder(WORK_ORDER_ID, ROUTE_ID, LocalDateTime.of(2026, 8, 1, 8, 0))));
        givenWorkOrderProductAndRoute(workOrder(WORK_ORDER_ID, PRODUCT_ID), " ");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.listActiveOrders());

        assertEquals(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("productName"));
    }

    @Test
    void shouldLoadProcessesFromSelectedActiveOrderProductRoute() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10),
                routeProcess(4002L, ROUTE_ID, 5002L, 20)));
        when(processService.getProcessMap(Set.of(PROCESS_ID, 5002L))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序"),
                5002L, process(5002L, "P-2", "末工序")));
        givenPqcTaskContext(ROUTE_PROCESS_ID, PROCESS_ID, PQC_TASK_ID, REGULATION_VERSION_ID);
        givenPqcTaskContext(4002L, 5002L, 7002L, 8002L);

        List<MesFrontlineRouteProcessCandidate> processes =
                service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID);

        assertEquals(List.of(ROUTE_PROCESS_ID, 4002L),
                processes.stream().map(MesFrontlineRouteProcessCandidate::routeProcessId).toList());
        assertEquals(List.of("首工序", "末工序"),
                processes.stream().map(MesFrontlineRouteProcessCandidate::processName).toList());
    }

    @Test
    void shouldDisplayOnlyQaInspectionItemProcessesWhenRouteHasExtraProcesses() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10),
                routeProcess(4002L, ROUTE_ID, 5002L, 20)));
        when(processService.getProcessMap(any())).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序"),
                5002L, process(5002L, "P-2", "末工序")));
        includeProcessSnapshot(4002L, ROUTE_ID, 5002L);
        givenPqcTaskContext(ROUTE_PROCESS_ID, PROCESS_ID, PQC_TASK_ID, REGULATION_VERSION_ID);

        List<MesFrontlineRouteProcessCandidate> processes =
                service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID);

        assertEquals(List.of(ROUTE_PROCESS_ID),
                processes.stream().map(MesFrontlineRouteProcessCandidate::routeProcessId).toList());
        assertEquals(PQC_TASK_ID, processes.get(0).pqcTaskId());
        assertEquals(Boolean.TRUE, processes.get(0).finalInspectionApplicable());
        verify(regulationMapper, never()).selectPublishedByRouteProcess(PRODUCT_ID, ROUTE_ID, 448L,
                4002L, 5002L);
    }

    @Test
    void shouldExposeFirstAndPatrolTaskOptionsForSameProcess() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10)));
        when(processService.getProcessMap(Set.of(PROCESS_ID))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序")));
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID,
                        "FIRST", "FIRST", 1, 5),
                pqcTask(7002L, ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID,
                        "PATROL", "AM", 2, 40)));
        when(regulationMapper.selectPublishedByRouteProcess(PRODUCT_ID, ROUTE_ID, 448L,
                ROUTE_PROCESS_ID, PROCESS_ID)).thenReturn(regulation(REGULATION_VERSION_ID));
        includePublishedRegulation(ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID);
        when(versionMapper.selectById(REGULATION_VERSION_ID)).thenReturn(regulationVersion(REGULATION_VERSION_ID, true));
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(
                regulationItem(REGULATION_VERSION_ID, "first-size", "首检尺寸", "NUMBER", "卡尺", "FIRST"),
                regulationItem(REGULATION_VERSION_ID, "patrol-appearance", "巡检外观", "CHOICE", "目视",
                        "PATROL")));
        when(regulationItemEquipmentMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of());

        List<MesFrontlineRouteProcessCandidate> processes =
                service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID);

        assertEquals(1, processes.size());
        MesFrontlineRouteProcessCandidate process = processes.get(0);
        assertEquals(PQC_TASK_ID, process.pqcTaskId());
        assertEquals("FIRST", process.inspectionType());
        assertEquals(5, process.plannedInspectionQuantity());
        assertEquals(List.of("FIRST", "PATROL"), process.pqcTaskOptions().stream()
                .map(MesFrontlinePqcTaskOption::inspectionType).toList());
        assertEquals(List.of(5, 40), process.pqcTaskOptions().stream()
                .map(MesFrontlinePqcTaskOption::plannedInspectionQuantity).toList());
        assertEquals(List.of("首检尺寸"), process.pqcTaskOptions().get(0).inspectionItems().stream()
                .map(MesFrontlinePqcInspectionItem::itemName).toList());
        assertEquals(List.of("巡检外观"), process.pqcTaskOptions().get(1).inspectionItems().stream()
                .map(MesFrontlinePqcInspectionItem::itemName).toList());
    }

    @Test
    void shouldHideRouteProcessesWithoutQaInspectionItemsAndAttachPqcTaskToPendingProcess() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10),
                routeProcess(4002L, ROUTE_ID, 5002L, 20)));
        when(processService.getProcessMap(any())).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "粗洗工序"),
                5002L, process(5002L, "P-2", "精洗工序")));
        includeProcessSnapshot(4002L, ROUTE_ID, 5002L);
        givenPqcTaskContext(ROUTE_PROCESS_ID, PROCESS_ID, PQC_TASK_ID, REGULATION_VERSION_ID);

        List<MesFrontlineRouteProcessCandidate> processes =
                service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID);

        assertEquals(List.of(ROUTE_PROCESS_ID),
                processes.stream().map(MesFrontlineRouteProcessCandidate::routeProcessId).toList());
        assertEquals(PQC_TASK_ID, processes.get(0).pqcTaskId());
        verify(regulationMapper, never()).selectPublishedByRouteProcess(PRODUCT_ID, ROUTE_ID, 448L,
                4002L, 5002L);
    }

    @Test
    void shouldAttachPqcTaskFromQaRegulationProcessWhenCurrentRouteProcessIdsDrift() {
        Long qaRouteProcessId = 980645L;
        Long currentRouteProcessId = 980661L;
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcess(currentRouteProcessId, ROUTE_ID, PROCESS_ID, 10)));
        when(processService.getProcessMap(Set.of(PROCESS_ID))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "粗洗工序")));
        givenPqcTaskContext(qaRouteProcessId, PROCESS_ID, PQC_TASK_ID, REGULATION_VERSION_ID);

        List<MesFrontlineRouteProcessCandidate> processes =
                service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID);

        assertEquals(List.of(qaRouteProcessId),
                processes.stream().map(MesFrontlineRouteProcessCandidate::routeProcessId).toList());
        assertEquals(PQC_TASK_ID, processes.get(0).pqcTaskId());
        assertEquals(PROCESS_ID, processes.get(0).processId());
        assertEquals(2, processes.get(0).inspectionItems().size());
        verify(regulationMapper).selectPublishedByRouteProcess(PRODUCT_ID, ROUTE_ID, 448L,
                qaRouteProcessId, PROCESS_ID);
    }

    @Test
    void shouldExposeMatchingProductionSubmitCandidatesForPqcTask() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(WORK_ORDER_ID, ROUTE_ID, LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcessWithoutWorkstation(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10)));
        when(processService.getProcessMap(Set.of(PROCESS_ID))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序")));
        givenPqcTaskContext(ROUTE_PROCESS_ID, PROCESS_ID, PQC_TASK_ID, REGULATION_VERSION_ID);
        when(processPoolEventMapper.selectProductionSubmitsByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(List.of(
                        productionSubmitEvent(9102L, ROUTE_PROCESS_ID, PROCESS_ID)
                                .setServerSubmitTime(LocalDateTime.of(2026, 8, 1, 10, 0)),
                        productionSubmitEvent(9101L, ROUTE_PROCESS_ID, PROCESS_ID)
                                .setServerSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 0))));

        MesFrontlineRouteProcessCandidate process =
                service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID).get(0);

        assertEquals(List.of(9102L, 9101L), process.productionSubmitCandidates().stream()
                .map(MesFrontlineProductionSubmitCandidate::eventId)
                .toList());
    }

    @Test
    void shouldGenerateSignatureAndReturnFormalPqcReceipt() {
        givenSuccessfulPqcSubmissionContext();

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                successfulPqcSubmitCommand()
                        .signaturePassword("formal-password")
                        .scrapQuantity(0)
                        .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        assertEquals(9902L, result.pqcEventId());
        assertEquals(10002L, result.pqcRecordId());
        assertEquals(8802L, result.signatureId());
        assertEquals("SUCCESS", result.inspectionResult());
        verify(signatureService).recordPqcSubmitSignature("formal-password", "PQC任务7001正式提交");
    }

    @Test
    void shouldSubmitPqcInspectionWithOnlySignatureAndPositiveQuantity() {
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID));
        when(pqcTaskMapper.updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED")).thenReturn(1);
        when(processPoolEventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(9910L);
        givenPqcSignatureAndReceipt(9910L, 8810L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                MesFrontlinePqcSubmitCommand.builder()
                        .pqcTaskId(PQC_TASK_ID)
                        .actualInspectionQuantity(2)
                        .signaturePassword("formal-password")
                        .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        assertEquals(9910L, result.pqcEventId());
        ArgumentCaptor<MesProcessPoolCreatePqcInspectionReqDTO> eventCaptor =
                ArgumentCaptor.forClass(MesProcessPoolCreatePqcInspectionReqDTO.class);
        verify(processPoolEventService).createPqcInspectionEvent(eventCaptor.capture());
        MesProcessPoolCreatePqcInspectionReqDTO eventRequest = eventCaptor.getValue();
        assertNull(eventRequest.getProductionSubmitEventId());
        assertEquals(WORK_ORDER_ID, eventRequest.getWorkOrderId());
        assertEquals(ROUTE_ID, eventRequest.getRouteId());
        assertEquals(ROUTE_PROCESS_ID, eventRequest.getRouteProcessId());
        assertEquals(PROCESS_ID, eventRequest.getProcessId());
        assertEquals(LOGIN_USER_ID, eventRequest.getActualEmployeeId());
        assertTrue(eventRequest.getRawPayload().contains("\"actualInspectionQuantity\":2"));
        verify(activeOrderMapper, never()).selectActiveByWorkOrderAndRoute(any(), any());
        verify(scopeMapper, never()).selectActiveScopesByLeaderType(any());
        verify(regulationMapper, never()).selectPublishedByRouteProcess(any(), any(), any(), any(), any());
    }

    @Test
    void shouldPreparePqcPieceDetailContextWithBulkQueriesOnly() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10),
                routeProcess(4002L, ROUTE_ID, 5002L, 20)));
        when(processService.getProcessMap(Set.of(PROCESS_ID, 5002L))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序"),
                5002L, process(5002L, "P-2", "末工序")));
        includeProcessSnapshot(4002L, ROUTE_ID, 5002L);
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID),
                pqcTask(7002L, 4002L, 5002L, 8002L)));
        when(regulationMapper.selectPublishedByRouteProcess(PRODUCT_ID, ROUTE_ID, 448L,
                ROUTE_PROCESS_ID, PROCESS_ID)).thenReturn(regulation(REGULATION_VERSION_ID));
        when(regulationMapper.selectPublishedByRouteProcess(PRODUCT_ID, ROUTE_ID, 448L,
                4002L, 5002L)).thenReturn(regulation(8002L, 4002L, 5002L));
        includePublishedRegulation(ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID);
        includePublishedRegulation(4002L, 5002L, 8002L);
        when(versionMapper.selectById(REGULATION_VERSION_ID))
                .thenReturn(regulationVersion(REGULATION_VERSION_ID, true));
        when(versionMapper.selectById(8002L)).thenReturn(regulationVersion(8002L, true));
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(
                regulationItem(REGULATION_VERSION_ID, "pressure", "压力", "NUMBER", "测压")));
        when(regulationItemMapper.selectListByVersionId(8002L)).thenReturn(List.of(
                regulationItem(8002L, "appearance", "外观", "CHOICE", "目视")));
        when(regulationItemEquipmentMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(
                regulationItemEquipment(REGULATION_VERSION_ID, "pressure", 9101L,
                        "MCH-PQC-001", "压力检验仪", "EQ-P-001")));
        when(regulationItemEquipmentMapper.selectListByVersionId(8002L)).thenReturn(List.of(
                regulationItemEquipment(8002L, "appearance", 9102L,
                        "MCH-PQC-002", "外观灯箱", "EQ-V-002")));

        List<MesFrontlineRouteProcessCandidate> processes =
                service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID);

        assertEquals(List.of(ROUTE_PROCESS_ID, 4002L),
                processes.stream().map(MesFrontlineRouteProcessCandidate::routeProcessId).toList());
        verify(routeProcessMapper).selectListByRouteId(ROUTE_ID);
        verify(pqcTaskMapper).selectListByActiveOrderId(ACTIVE_ORDER_ID);
        verify(regulationItemMapper).selectListByVersionId(REGULATION_VERSION_ID);
        verify(regulationItemMapper).selectListByVersionId(8002L);
        verify(pqcTaskMapper, never()).selectPendingByActiveOrderProcess(any(), any(), any());
        verify(pqcTaskMapper, never()).selectById(any());
    }

    @Test
    void shouldDisplaySubmittedPqcTaskProcessWithoutTaskContextAndKeepPendingProcess() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10),
                routeProcess(4002L, ROUTE_ID, 5002L, 20)));
        when(processService.getProcessMap(Set.of(PROCESS_ID, 5002L))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序"),
                5002L, process(5002L, "P-2", "末工序")));
        pqcTaskContextFixtures.add(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID)
                .setTaskStatus("SUBMITTED"));
        includePublishedRegulation(ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID);
        givenPqcTaskContext(4002L, 5002L, 7002L, 8002L);

        List<MesFrontlineRouteProcessCandidate> processes =
                service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID);

        assertEquals(List.of(ROUTE_PROCESS_ID, 4002L), processes.stream()
                .map(MesFrontlineRouteProcessCandidate::routeProcessId).toList());
        assertEquals(List.of("首工序", "末工序"), processes.stream()
                .map(MesFrontlineRouteProcessCandidate::processName).toList());
        assertNull(processes.get(0).pqcTaskId());
        assertEquals(7002L, processes.get(1).pqcTaskId());
    }

    @Test
    void shouldFailFastWhenPendingPqcTaskMissingFormalProcessIdentity() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10)));
        when(processService.getProcessMap(Set.of(PROCESS_ID))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序")));
        includePublishedRegulation(ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID);
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                pqcTask(PQC_TASK_ID, null, null, REGULATION_VERSION_ID)));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID));

        assertEquals(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("taskId=7001"));
        assertTrue(exception.getMessage().contains("activeOrderId=6001"));
        assertTrue(exception.getMessage().contains("routeProcessId=null"));
        assertTrue(exception.getMessage().contains("processId=null"));
    }

    @Test
    void shouldListOnlyCurrentPqcLoginEmployee() {
        when(scopeMapper.selectActiveScopesByLeaderType(LEADER_TYPE_PQC)).thenReturn(List.of(
                scope(7001L, SCOPE_TYPE_EMPLOYEE, 8001L),
                scope(7002L, SCOPE_TYPE_PROCESS, null),
                scope(7001L, SCOPE_TYPE_EMPLOYEE, 8002L)));
        when(adminUserApi.getUserList(Set.of(7001L, 7002L, 8001L, 8002L))).thenReturn(List.of(
                enabledUser(7001L, "pqc-leader-a", "PQC组长A"),
                enabledUser(7002L, "pqc-leader-b", "PQC组长B"),
                enabledUser(8001L, "pqc-employee-a", "PQC员工A"),
                enabledUser(8002L, "pqc-employee-b", "PQC员工B")));

        List<MesFrontlineEmployeeCandidate> employees = service.listPqcEmployeeCandidates(LOGIN_USER_ID);

        assertEquals(List.of(LOGIN_USER_ID),
                employees.stream().map(MesFrontlineEmployeeCandidate::userId).toList());
        assertEquals("PQC员工A", employees.get(0).nickname());
    }

    @Test
    void shouldFailFastWhenSelectedOrderIsNotActive() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID)).thenReturn(null);

        assertThrows(ServiceException.class, () -> service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID));
    }

    @Test
    void shouldSwitchPqcEmployeeOnlyAfterActiveOrderProcessAndPersonnelValidation() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcessWithoutWorkstation(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10)));
        when(processService.getProcessMap(Set.of(PROCESS_ID))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序")));
        givenPqcTaskContext(ROUTE_PROCESS_ID, PROCESS_ID, PQC_TASK_ID, REGULATION_VERSION_ID);
        when(scopeMapper.selectActiveScopesByLeaderType(LEADER_TYPE_PQC)).thenReturn(List.of(
                scope(7001L, SCOPE_TYPE_EMPLOYEE, 8001L)));
        when(adminUserApi.getUserList(Set.of(7001L, 8001L))).thenReturn(List.of(
                enabledUser(7001L, "pqc-leader-a", "PQC组长A"),
                enabledUser(8001L, "pqc-employee-a", "PQC员工A")));
        MesFrontlineEmployeeSwitchResult result = service.switchPqcActualEmployee(LOGIN_USER_ID, WORK_ORDER_ID,
                ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID, 8001L);

        assertEquals(LOGIN_USER_ID, result.loginUserId());
        assertEquals(8001L, result.actualEmployeeId());
        assertEquals("PQC_SIMPLIFIED", result.template().templateNo());
    }

    @Test
    void shouldRejectPqcEmployeeSwitchWhenActualEmployeeIsNotLoginUser() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.switchPqcActualEmployee(LOGIN_USER_ID, WORK_ORDER_ID,
                        ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID, 8002L));

        assertEquals(PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND.getCode(), exception.getCode());
        verify(activeOrderMapper, never()).selectActiveByWorkOrderAndRoute(any(), any());
    }

    @Test
    void shouldReturnPqcTemplateForPqcSwitchEvenWhenRouteProcessUsesProductionTemplate() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(WORK_ORDER_ID, ROUTE_ID,
                        LocalDateTime.of(2026, 8, 1, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID, PRODUCT_ID));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route(ROUTE_ID));
        when(routeProcessMapper.selectListByRouteId(ROUTE_ID)).thenReturn(List.of(
                routeProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10)));
        when(processService.getProcessMap(Set.of(PROCESS_ID))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序")));
        givenPqcTaskContext(ROUTE_PROCESS_ID, PROCESS_ID, PQC_TASK_ID, REGULATION_VERSION_ID);
        when(scopeMapper.selectActiveScopesByLeaderType(LEADER_TYPE_PQC)).thenReturn(List.of(
                scope(7001L, SCOPE_TYPE_EMPLOYEE, 8001L)));
        when(adminUserApi.getUserList(Set.of(7001L, 8001L))).thenReturn(List.of(
                enabledUser(7001L, "pqc-leader-a", "PQC组长A"),
                enabledUser(8001L, "pqc-employee-a", "PQC员工A")));

        MesFrontlineEmployeeSwitchResult result = service.switchPqcActualEmployee(LOGIN_USER_ID, WORK_ORDER_ID,
                ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID, 8001L);

        assertEquals("PQC_SIMPLIFIED", result.template().templateNo());
        assertEquals("PQC", result.template().templateType());
        assertEquals(ROUTE_PROCESS_ID, result.template().routeProcessId());
        assertEquals(PROCESS_ID, result.template().processId());
        assertEquals(8001L, result.template().actualEmployeeId());
    }

    @Test
    void shouldSubmitPqcInspectionFromQaRegulationTaskSource() {
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID));
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(
                regulationItem(REGULATION_VERSION_ID, "pressure", "压力", "NUMBER", "测压"),
                regulationItem(REGULATION_VERSION_ID, "appearance", "外观", "BOOLEAN", "目检")));
        when(pqcTaskMapper.updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED")).thenReturn(1);
        when(processPoolEventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(9901L);
        givenPqcSignatureAndReceipt(9901L, 8801L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                MesFrontlinePqcSubmitCommand.builder()
                .activeOrderId(ACTIVE_ORDER_ID)
                .pqcTaskId(PQC_TASK_ID)
                .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                .regulationVersionId(REGULATION_VERSION_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .inspectionType("PATROL")
                .businessDate(LocalDate.of(2026, 8, 1))
                .shiftCode("DAY")
                .roundNo(1)
                .actualInspectionQuantity(2)
                .actualEmployeeId(8001L)
                .deviceAccountId(DEVICE_ACCOUNT_ID)
                .deviceId(DEVICE_ID)
                .workstationId(WORKSTATION_ID)
                .pqcSubmissionIdempotencyKey(PQC_IDEMPOTENCY_KEY)
                .signaturePassword("formal-password")
                .templateType("PQC_SIMPLIFIED")
                .scrapQuantity(0)
                .itemResults(List.of(
                        MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                .itemCode("pressure")
                                .selectedEquipmentId(9101L)
                                .selectedEquipmentNumber("EQ-P-001")
                                .sampleValues(List.of("0.8", "0.9"))
                                .build(),
                        MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                .itemCode("appearance")
                                .selectedEquipmentId(9102L)
                                .selectedEquipmentNumber("EQ-V-002")
                                .sampleValues(List.of("合格", "合格"))
                                .build()))
                .rawPayload(Map.of(
                        "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 30)))
                .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        ArgumentCaptor<MesProcessPoolCreatePqcInspectionReqDTO> eventCaptor =
                ArgumentCaptor.forClass(MesProcessPoolCreatePqcInspectionReqDTO.class);
        verify(processPoolEventService).createPqcInspectionEvent(eventCaptor.capture());
        MesProcessPoolCreatePqcInspectionReqDTO eventRequest = eventCaptor.getValue();
        assertEquals(WORK_ORDER_ID, eventRequest.getWorkOrderId());
        assertEquals(PRODUCTION_SUBMIT_EVENT_ID, eventRequest.getProductionSubmitEventId());
        assertEquals(ROUTE_ID, eventRequest.getRouteId());
        assertEquals(ROUTE_PROCESS_ID, eventRequest.getRouteProcessId());
        assertEquals(PROCESS_ID, eventRequest.getProcessId());
        assertEquals(8001L, eventRequest.getActualEmployeeId());
        assertEquals(DEVICE_ACCOUNT_ID, eventRequest.getDeviceAccountId());
        assertEquals(DEVICE_ID, eventRequest.getDeviceId());
        assertEquals(WORKSTATION_ID, eventRequest.getWorkstationId());
        assertEquals("PQC_SIMPLIFIED", eventRequest.getTemplateType());
        assertEquals(PQC_SOURCE_TYPE, eventRequest.getFeedbackSourceType());
        assertEquals(PQC_TASK_ID, eventRequest.getFeedbackSourceId());
        assertEquals(PQC_SOURCE_TYPE, eventRequest.getRecordbookSourceType());
        assertEquals(PQC_TASK_ID, eventRequest.getRecordbookSourceId());
        assertEquals(PQC_IDEMPOTENCY_KEY, eventRequest.getPqcSubmissionIdempotencyKey());
        assertEquals("SUCCESS", eventRequest.getInspectionResult());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), eventRequest.getClientSubmitTime());
        assertEquals(8801L, eventRequest.getSignatureId());
        assertEquals(8001L, eventRequest.getSignatureUserId());
        assertTrue(eventRequest.getSignatureSnapshot().contains("\"actionType\":\"PQC_SUBMIT\""));
        assertTrue(eventRequest.getRawPayload().contains("\"pqcTaskId\":7001"));
        assertTrue(eventRequest.getRawPayload().contains("\"regulationVersionId\":8001"));
        verify(pqcTaskMapper).updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesPqcInspectionPieceDetailDO>> detailCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(pqcPieceDetailMapper).insertBatch(detailCaptor.capture());
        List<MesPqcInspectionPieceDetailDO> details = detailCaptor.getValue().stream().toList();
        assertEquals(4, details.size());
        assertEquals(PQC_TASK_ID, details.get(0).getTaskId());
        assertEquals("pressure", details.get(0).getItemCode());
        assertEquals(1, details.get(0).getSampleNo());
        assertEquals("测压", details.get(0).getInspectionMethod());
        assertEquals(9101L, details.get(0).getSelectedEquipmentId());
        assertNull(details.get(0).getSelectedEquipmentCode());
        assertNull(details.get(0).getSelectedEquipmentName());
        assertEquals("EQ-P-001", details.get(0).getSelectedEquipmentNumber());
        assertEquals(0, new BigDecimal("0.700000").compareTo(details.get(0).getStandardLowerLimit()));
        assertEquals(0, new BigDecimal("1.000000").compareTo(details.get(0).getStandardUpperLimit()));
        assertEquals("MPa", details.get(0).getStandardUnit());
        assertEquals(2, details.get(0).getStandardPrecision());
        assertTrue(eventRequest.getRawPayload().contains("\"pqcItemDetails\""));
        assertTrue(eventRequest.getRawPayload().contains("\"selectedEquipmentNumber\":\"EQ-P-001\""));
    }

    @Test
    void shouldSubmitAlreadySubmittedPqcInspectionTaskWhenSignatureAndQuantityAreValid() {
        MesPqcInspectionTaskDO submittedTask = pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID);
        submittedTask.setTaskStatus("SUBMITTED");
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(submittedTask);
        when(processPoolEventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(9911L);
        givenPqcSignatureAndReceipt(9911L, 8811L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                MesFrontlinePqcSubmitCommand.builder()
                        .activeOrderId(ACTIVE_ORDER_ID)
                        .pqcTaskId(PQC_TASK_ID)
                        .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                        .regulationVersionId(REGULATION_VERSION_ID)
                        .workOrderId(WORK_ORDER_ID)
                        .routeId(ROUTE_ID)
                        .routeProcessId(ROUTE_PROCESS_ID)
                        .processId(PROCESS_ID)
                        .inspectionType("PATROL")
                        .businessDate(LocalDate.of(2026, 8, 1))
                        .shiftCode("DAY")
                        .roundNo(1)
                        .actualInspectionQuantity(2)
                        .actualEmployeeId(8001L)
                        .deviceAccountId(DEVICE_ACCOUNT_ID)
                        .deviceId(DEVICE_ID)
                        .workstationId(WORKSTATION_ID)
                        .pqcSubmissionIdempotencyKey(PQC_IDEMPOTENCY_KEY)
                        .signaturePassword("formal-password")
                        .templateType("PQC_SIMPLIFIED")
                        .scrapQuantity(0)
                        .rawPayload(Map.of(
                                "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 2),
                                "pqcPieceValues", Map.of(
                                        "pressure", List.of("0.8", "0.9"),
                                        "appearance", List.of("合格", "合格"))))
                        .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 45))
                        .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        verify(pqcTaskMapper).updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED");
        verify(processPoolEventService).createPqcInspectionEvent(any());
    }

    @Test
    void shouldSubmitPqcInspectionWithoutEquipmentForOptionalQaItems() {
        givenValidPqcSubmissionValidationContextWithoutEquipment();
        when(pqcTaskMapper.updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED")).thenReturn(1);
        when(processPoolEventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(9904L);
        givenPqcSignatureAndReceipt(9904L, 8804L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                successfulPqcSubmitCommand()
                        .itemResults(List.of(
                                MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                        .itemCode("pressure")
                                        .sampleValues(List.of("0.8", "0.9"))
                                        .build(),
                                MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                        .itemCode("appearance")
                                        .sampleValues(List.of("合格", "合格"))
                                        .build()))
                        .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesPqcInspectionPieceDetailDO>> detailCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(pqcPieceDetailMapper).insertBatch(detailCaptor.capture());
        List<MesPqcInspectionPieceDetailDO> details = detailCaptor.getValue().stream().toList();
        assertEquals(4, details.size());
        for (MesPqcInspectionPieceDetailDO detail : details) {
            assertNull(detail.getSelectedEquipmentId());
            assertNull(detail.getSelectedEquipmentCode());
            assertNull(detail.getSelectedEquipmentName());
            assertNull(detail.getSelectedEquipmentNumber());
        }
    }

    @Test
    void shouldSubmitPqcInspectionWhenRequiredQaItemEquipmentIsMissing() {
        givenValidPqcSubmissionValidationContext();
        when(pqcTaskMapper.updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED")).thenReturn(1);
        when(processPoolEventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(9912L);
        givenPqcSignatureAndReceipt(9912L, 8812L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                successfulPqcSubmitCommand()
                                .itemResults(List.of(
                                        MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                                .itemCode("pressure")
                                                .sampleValues(List.of("0.8", "0.9"))
                                                .build(),
                                        MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                                .itemCode("appearance")
                                                .selectedEquipmentId(9102L)
                                                .selectedEquipmentNumber("EQ-V-002")
                                                .sampleValues(List.of("合格", "合格"))
                                                .build()))
                                .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesPqcInspectionPieceDetailDO>> detailCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(pqcPieceDetailMapper).insertBatch(detailCaptor.capture());
        assertTrue(detailCaptor.getValue().stream()
                .filter(detail -> "pressure".equals(detail.getItemCode()))
                .allMatch(detail -> detail.getSelectedEquipmentId() == null));
    }

    @Test
    void shouldSubmitPqcInspectionWhenActualQuantityDiffersFromPlannedTaskQuantity() {
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID));
        when(pqcTaskMapper.updateSubmittedIfPending(PQC_TASK_ID, 1, "PENDING", "SUBMITTED")).thenReturn(1);
        when(processPoolEventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(9913L);
        givenPqcSignatureAndReceipt(9913L, 8813L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                MesFrontlinePqcSubmitCommand.builder()
                        .activeOrderId(ACTIVE_ORDER_ID)
                        .pqcTaskId(PQC_TASK_ID)
                        .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                        .regulationVersionId(REGULATION_VERSION_ID)
                        .workOrderId(WORK_ORDER_ID)
                        .routeId(ROUTE_ID)
                        .routeProcessId(ROUTE_PROCESS_ID)
                        .processId(PROCESS_ID)
                        .inspectionType("PATROL")
                        .businessDate(LocalDate.of(2026, 8, 1))
                        .shiftCode("DAY")
                        .roundNo(1)
                        .actualInspectionQuantity(1)
                        .actualEmployeeId(8001L)
                        .deviceAccountId(DEVICE_ACCOUNT_ID)
                        .deviceId(DEVICE_ID)
                        .workstationId(WORKSTATION_ID)
                        .pqcSubmissionIdempotencyKey(PQC_IDEMPOTENCY_KEY)
                        .signaturePassword("formal-password")
                        .templateType("PQC_SIMPLIFIED")
                        .scrapQuantity(0)
                        .itemResults(List.of(
                                MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                        .itemCode("pressure")
                                        .selectedEquipmentId(9101L)
                                        .selectedEquipmentNumber("EQ-P-001")
                                        .sampleValues(List.of("0.8"))
                                        .build(),
                                MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                        .itemCode("appearance")
                                        .selectedEquipmentId(9102L)
                                        .selectedEquipmentNumber("EQ-V-002")
                                        .sampleValues(List.of("合格"))
                                        .build()))
                        .rawPayload(Map.of(
                                "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 1)))
                        .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 55))
                        .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        verify(pqcTaskMapper).updateSubmittedIfPending(PQC_TASK_ID, 1, "PENDING", "SUBMITTED");
        verify(processPoolEventService).createPqcInspectionEvent(any());
    }

    @Test
    void shouldLimitExtraItemSampleValuesToActualInspectionQuantity() {
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID));
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(
                regulationItem(REGULATION_VERSION_ID, "pressure", "压力", "NUMBER", "测压"),
                regulationItem(REGULATION_VERSION_ID, "appearance", "外观", "BOOLEAN", "目检")));
        when(pqcTaskMapper.updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED")).thenReturn(1);
        when(processPoolEventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(9914L);
        givenPqcSignatureAndReceipt(9914L, 8814L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                MesFrontlinePqcSubmitCommand.builder()
                        .activeOrderId(ACTIVE_ORDER_ID)
                        .pqcTaskId(PQC_TASK_ID)
                        .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                        .regulationVersionId(REGULATION_VERSION_ID)
                        .workOrderId(WORK_ORDER_ID)
                        .routeId(ROUTE_ID)
                        .routeProcessId(ROUTE_PROCESS_ID)
                        .processId(PROCESS_ID)
                        .inspectionType("PATROL")
                        .businessDate(LocalDate.of(2026, 8, 1))
                        .shiftCode("DAY")
                        .roundNo(1)
                        .actualInspectionQuantity(2)
                        .actualEmployeeId(8001L)
                        .deviceAccountId(DEVICE_ACCOUNT_ID)
                        .deviceId(DEVICE_ID)
                        .workstationId(WORKSTATION_ID)
                        .pqcSubmissionIdempotencyKey(PQC_IDEMPOTENCY_KEY)
                        .signaturePassword("formal-password")
                        .templateType("PQC_SIMPLIFIED")
                        .scrapQuantity(0)
                        .itemResults(List.of(
                                MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                        .itemCode("pressure")
                                        .selectedEquipmentId(9101L)
                                        .selectedEquipmentNumber("EQ-P-001")
                                        .sampleValues(List.of("0.8", "0.9", "1.0"))
                                        .build(),
                                MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                        .itemCode("appearance")
                                        .selectedEquipmentId(9102L)
                                        .selectedEquipmentNumber("EQ-V-002")
                                        .sampleValues(List.of("合格", "合格"))
                                        .build()))
                        .rawPayload(Map.of(
                                "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 2)))
                        .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 56))
                        .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesPqcInspectionPieceDetailDO>> detailCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(pqcPieceDetailMapper).insertBatch(detailCaptor.capture());
        assertEquals(2, detailCaptor.getValue().stream()
                .filter(detail -> "pressure".equals(detail.getItemCode()))
                .count());
    }

    @Test
    void shouldSubmitPqcInspectionWhenPendingTaskUpdateAffectsNoRows() {
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID));
        when(pqcTaskMapper.updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED")).thenReturn(0);
        when(processPoolEventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(9915L);
        givenPqcSignatureAndReceipt(9915L, 8815L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                MesFrontlinePqcSubmitCommand.builder()
                        .activeOrderId(ACTIVE_ORDER_ID)
                        .pqcTaskId(PQC_TASK_ID)
                        .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                        .regulationVersionId(REGULATION_VERSION_ID)
                        .workOrderId(WORK_ORDER_ID)
                        .routeId(ROUTE_ID)
                        .routeProcessId(ROUTE_PROCESS_ID)
                        .processId(PROCESS_ID)
                        .inspectionType("PATROL")
                        .businessDate(LocalDate.of(2026, 8, 1))
                        .shiftCode("DAY")
                        .roundNo(1)
                        .actualInspectionQuantity(2)
                        .actualEmployeeId(8001L)
                        .deviceAccountId(DEVICE_ACCOUNT_ID)
                        .deviceId(DEVICE_ID)
                        .workstationId(WORKSTATION_ID)
                        .pqcSubmissionIdempotencyKey(PQC_IDEMPOTENCY_KEY)
                        .signaturePassword("formal-password")
                        .templateType("PQC_SIMPLIFIED")
                        .scrapQuantity(0)
                        .itemResults(List.of(
                                MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                        .itemCode("pressure")
                                        .selectedEquipmentId(9101L)
                                        .selectedEquipmentNumber("EQ-P-001")
                                        .sampleValues(List.of("0.8", "0.9"))
                                        .build(),
                                MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                        .itemCode("appearance")
                                        .selectedEquipmentId(9102L)
                                        .selectedEquipmentNumber("EQ-V-002")
                                        .sampleValues(List.of("合格", "合格"))
                                        .build()))
                        .rawPayload(Map.of(
                                "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 2)))
                        .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 50))
                        .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        verify(pqcTaskMapper).updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED");
        verify(processPoolEventService).createPqcInspectionEvent(any());
    }

    @Test
    void shouldSubmitPqcInspectionWithProductionSourceAndEquipmentSnapshot() {
        givenSuccessfulPqcSubmissionContext();
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(
                regulationItem(REGULATION_VERSION_ID, "pressure", "压力", "NUMBER", "测压"),
                regulationItem(REGULATION_VERSION_ID, "appearance", "外观", "BOOLEAN", "目检")));

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                MesFrontlinePqcSubmitCommand.builder()
                .activeOrderId(ACTIVE_ORDER_ID)
                .pqcTaskId(PQC_TASK_ID)
                .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                .regulationVersionId(REGULATION_VERSION_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .inspectionType("PATROL")
                .businessDate(LocalDate.of(2026, 8, 1))
                .shiftCode("DAY")
                .roundNo(1)
                .actualInspectionQuantity(2)
                .actualEmployeeId(8001L)
                .deviceAccountId(DEVICE_ACCOUNT_ID)
                .deviceId(DEVICE_ID)
                .workstationId(WORKSTATION_ID)
                .pqcSubmissionIdempotencyKey(PQC_IDEMPOTENCY_KEY)
                .signaturePassword("formal-password")
                .templateType("PQC_SIMPLIFIED")
                .scrapQuantity(0)
                .itemResults(List.of(
                        MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                .itemCode("pressure")
                                .selectedEquipmentId(9101L)
                                .selectedEquipmentNumber("EQ-P-001")
                                .sampleValues(List.of("0.8", "0.9"))
                                .build(),
                        MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                .itemCode("appearance")
                                .selectedEquipmentId(9102L)
                                .selectedEquipmentNumber("EQ-V-002")
                                .sampleValues(List.of("合格", "合格"))
                                .build()))
                .rawPayload(Map.of(
                        "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 2)))
                .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 30))
                .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        verify(pqcTaskMapper).updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED");
        verify(processPoolEventMapper, never()).selectByIdForUpdate(any());
        ArgumentCaptor<MesProcessPoolCreatePqcInspectionReqDTO> eventCaptor =
                ArgumentCaptor.forClass(MesProcessPoolCreatePqcInspectionReqDTO.class);
        verify(processPoolEventService).createPqcInspectionEvent(eventCaptor.capture());
        MesProcessPoolCreatePqcInspectionReqDTO eventRequest = eventCaptor.getValue();
        assertEquals(PRODUCTION_SUBMIT_EVENT_ID, eventRequest.getProductionSubmitEventId());
        assertEquals(DEVICE_ACCOUNT_ID, eventRequest.getDeviceAccountId());
        assertEquals(DEVICE_ID, eventRequest.getDeviceId());
        assertEquals(WORKSTATION_ID, eventRequest.getWorkstationId());
        assertEquals(PQC_IDEMPOTENCY_KEY, eventRequest.getPqcSubmissionIdempotencyKey());
        assertEquals(PQC_SOURCE_TYPE, eventRequest.getFeedbackSourceType());
        assertEquals(PQC_TASK_ID, eventRequest.getFeedbackSourceId());
        assertEquals(PQC_SOURCE_TYPE, eventRequest.getRecordbookSourceType());
        assertEquals(PQC_TASK_ID, eventRequest.getRecordbookSourceId());
        assertTrue(eventRequest.getRawPayload().contains("\"pqcTaskId\":7001"));
        assertTrue(eventRequest.getRawPayload().contains("\"pieceDetailCount\":4"));
    }

    @Test
    void shouldPreserveSubmittedPqcDeviceContextWithoutProductionEventValidation() {
        givenSuccessfulPqcSubmissionContext();

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                successfulPqcSubmitCommand()
                .deviceAccountId(99901L)
                .deviceId(99902L)
                .workstationId(99903L)
                .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        ArgumentCaptor<MesProcessPoolCreatePqcInspectionReqDTO> eventCaptor =
                ArgumentCaptor.forClass(MesProcessPoolCreatePqcInspectionReqDTO.class);
        verify(processPoolEventService).createPqcInspectionEvent(eventCaptor.capture());
        MesProcessPoolCreatePqcInspectionReqDTO eventRequest = eventCaptor.getValue();
        assertEquals(99901L, eventRequest.getDeviceAccountId());
        assertEquals(99902L, eventRequest.getDeviceId());
        assertEquals(99903L, eventRequest.getWorkstationId());
        verify(processPoolEventMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void shouldSubmitPqcInspectionWithoutValidatingProductionSubmitEventIdentity() {
        givenSuccessfulPqcSubmissionContext();

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                successfulPqcSubmitCommand().build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        verify(processPoolEventMapper, never()).selectByIdForUpdate(any());
        verify(processPoolEventService).createPqcInspectionEvent(any());
    }

    @Test
    void shouldKeepManualNonconformanceDescriptionInFailedPqcRawPayload() {
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(
                regulationItem(REGULATION_VERSION_ID, "pressure", "压力", "NUMBER", "测压"),
                regulationItem(REGULATION_VERSION_ID, "appearance", "外观", "BOOLEAN", "目检")));
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID));
        when(pqcTaskMapper.updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED")).thenReturn(1);
        when(processPoolEventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(9903L);
        givenPqcSignatureAndReceipt(9903L, 8803L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE);

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                MesFrontlinePqcSubmitCommand.builder()
                .activeOrderId(ACTIVE_ORDER_ID)
                .pqcTaskId(PQC_TASK_ID)
                .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                .regulationVersionId(REGULATION_VERSION_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .inspectionType("PATROL")
                .businessDate(LocalDate.of(2026, 8, 1))
                .shiftCode("DAY")
                .roundNo(1)
                .actualInspectionQuantity(2)
                .actualEmployeeId(8001L)
                .deviceAccountId(DEVICE_ACCOUNT_ID)
                .deviceId(DEVICE_ID)
                .workstationId(WORKSTATION_ID)
                .pqcSubmissionIdempotencyKey(PQC_IDEMPOTENCY_KEY)
                .signaturePassword("formal-password")
                .templateType("PQC_SIMPLIFIED")
                .scrapQuantity(1)
                .nonconformanceDescription("外观第2件有黑点")
                .itemResults(List.of(
                        MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                .itemCode("pressure")
                                .selectedEquipmentId(9101L)
                                .selectedEquipmentNumber("EQ-P-001")
                                .sampleValues(List.of("0.8", "0.9"))
                                .build(),
                        MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                .itemCode("appearance")
                                .selectedEquipmentId(9102L)
                                .selectedEquipmentNumber("EQ-V-002")
                                .sampleValues(List.of("合格", "不合格"))
                                .build()))
                .rawPayload(Map.of(
                        "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 2)))
                .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 40))
                .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        ArgumentCaptor<MesProcessPoolCreatePqcInspectionReqDTO> eventCaptor =
                ArgumentCaptor.forClass(MesProcessPoolCreatePqcInspectionReqDTO.class);
        verify(processPoolEventService).createPqcInspectionEvent(eventCaptor.capture());
        MesProcessPoolCreatePqcInspectionReqDTO eventRequest = eventCaptor.getValue();
        assertEquals("FAILURE", eventRequest.getInspectionResult());
        assertTrue(eventRequest.getRawPayload().contains("\"nonconformanceDescription\":\"外观第2件有黑点\""));
        assertTrue(eventRequest.getRawPayload().contains("\"workOrderId\":1001"));
        assertTrue(eventRequest.getRawPayload().contains("\"routeProcessId\":4001"));
        assertTrue(eventRequest.getRawPayload().contains("\"pqcTaskId\":7001"));
    }

    @Test
    void shouldSubmitFailedPqcInspectionWithoutManualNonconformanceDescription() {
        givenValidPqcSubmissionValidationContext();
        when(pqcTaskMapper.updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED")).thenReturn(1);
        when(processPoolEventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(9916L);
        givenPqcSignatureAndReceipt(9916L, 8816L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE);

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID,
                MesFrontlinePqcSubmitCommand.builder()
                        .activeOrderId(ACTIVE_ORDER_ID)
                        .pqcTaskId(PQC_TASK_ID)
                        .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                        .regulationVersionId(REGULATION_VERSION_ID)
                        .workOrderId(WORK_ORDER_ID)
                        .routeId(ROUTE_ID)
                        .routeProcessId(ROUTE_PROCESS_ID)
                        .processId(PROCESS_ID)
                        .inspectionType("PATROL")
                        .businessDate(LocalDate.of(2026, 8, 1))
                        .shiftCode("DAY")
                        .roundNo(1)
                        .actualInspectionQuantity(2)
                        .actualEmployeeId(8001L)
                        .deviceAccountId(DEVICE_ACCOUNT_ID)
                        .deviceId(DEVICE_ID)
                        .workstationId(WORKSTATION_ID)
                        .pqcSubmissionIdempotencyKey(PQC_IDEMPOTENCY_KEY)
                        .signaturePassword("formal-password")
                        .templateType("PQC_SIMPLIFIED")
                        .scrapQuantity(1)
                        .itemResults(List.of(
                                MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                        .itemCode("pressure")
                                        .selectedEquipmentId(9101L)
                                        .selectedEquipmentNumber("EQ-P-001")
                                        .sampleValues(List.of("0.8", "0.9"))
                                        .build(),
                                MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                        .itemCode("appearance")
                                        .selectedEquipmentId(9102L)
                                        .selectedEquipmentNumber("EQ-V-002")
                                        .sampleValues(List.of("合格", "不合格"))
                                        .build()))
                        .rawPayload(Map.of(
                                "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 2)))
                        .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 45))
                        .build());

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        ArgumentCaptor<MesProcessPoolCreatePqcInspectionReqDTO> eventCaptor =
                ArgumentCaptor.forClass(MesProcessPoolCreatePqcInspectionReqDTO.class);
        verify(processPoolEventService).createPqcInspectionEvent(eventCaptor.capture());
        assertEquals(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE,
                eventCaptor.getValue().getInspectionResult());
        assertFalse(eventCaptor.getValue().getRawPayload().contains("nonconformanceDescription"));
    }

    @Test
    void shouldReturnExistingPqcTaskBeforeWritingDuplicateSubmit() {
        MesFrontlinePqcSubmitCommand command = MesFrontlinePqcSubmitCommand.builder()
                .activeOrderId(ACTIVE_ORDER_ID)
                .pqcTaskId(PQC_TASK_ID)
                .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                .regulationVersionId(REGULATION_VERSION_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .inspectionType("PATROL")
                .businessDate(LocalDate.of(2026, 8, 1))
                .shiftCode("DAY")
                .roundNo(1)
                .actualInspectionQuantity(2)
                .actualEmployeeId(8001L)
                .deviceAccountId(DEVICE_ACCOUNT_ID)
                .deviceId(DEVICE_ID)
                .workstationId(WORKSTATION_ID)
                .pqcSubmissionIdempotencyKey(PQC_IDEMPOTENCY_KEY)
                .signaturePassword("formal-password")
                .templateType("PQC_SIMPLIFIED")
                .scrapQuantity(0)
                .rawPayload(Map.of(
                        "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 30),
                        "pqcPieceValues", Map.of(
                                "pressure", List.of("0.8", "0.9"),
                                "appearance", List.of("合格", "合格"))))
                .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build();
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID));
        when(processPoolEventService.findExistingPqcInspectionEventId(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(Optional.of(9904L));
        givenPqcReceipt(9904L, 8801L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(LOGIN_USER_ID, command);

        assertEquals(PQC_TASK_ID, result.pqcTaskId());
        verify(activeOrderMapper, never()).selectActiveByWorkOrderAndRoute(any(), any());
        verify(pqcTaskMapper, never()).updateById(any(MesPqcInspectionTaskDO.class));
        verify(pqcPieceDetailMapper, never()).insertBatch(anyCollection());
        verify(processPoolEventService, never()).createPqcInspectionEvent(any());
    }

    @Test
    void shouldReturnSubmittedPqcReceiptByTaskIdForReadOnlyRecovery() {
        when(processPoolEventMapper.selectLatestPqcByTaskId(PQC_SOURCE_TYPE, PQC_TASK_ID))
                .thenReturn(MesProProcessPoolEventDO.builder()
                        .id(9905L)
                        .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .feedbackSourceType(PQC_SOURCE_TYPE)
                        .feedbackSourceId(PQC_TASK_ID)
                        .actualEmployeeId(LOGIN_USER_ID)
                        .signatureId(8805L)
                        .signatureUserId(LOGIN_USER_ID)
                        .serverSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 40))
                        .build());
        when(processPoolEventMapper.selectById(9905L)).thenReturn(MesProProcessPoolEventDO.builder()
                .id(9905L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .feedbackSourceType(PQC_SOURCE_TYPE)
                .feedbackSourceId(PQC_TASK_ID)
                .actualEmployeeId(LOGIN_USER_ID)
                .signatureId(8805L)
                .signatureUserId(LOGIN_USER_ID)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 40))
                .build());
        when(pqcRecordMapper.selectByEventId(9905L)).thenReturn(MesProProcessPoolPqcRecordDO.builder()
                .id(9906L)
                .eventId(9905L)
                .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                .inspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .signatureId(8805L)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 40))
                .build());

        Optional<MesFrontlinePqcSubmitResult> result =
                service.getSubmittedPqcInspection(LOGIN_USER_ID, PQC_TASK_ID);

        assertTrue(result.isPresent());
        assertEquals(PQC_TASK_ID, result.get().pqcTaskId());
        assertEquals(9905L, result.get().pqcEventId());
        assertEquals(9906L, result.get().pqcRecordId());
        assertEquals(8805L, result.get().signatureId());
        verify(pqcTaskMapper, never()).updateSubmittedIfPending(any(), any(), any(), any());
        verify(processPoolEventService, never()).createPqcInspectionEvent(any());
    }

    private void givenWorkOrderProductAndRoute() {
        givenWorkOrderProductAndRoute(workOrder(WORK_ORDER_ID, PRODUCT_ID), "PQC 产品");
    }

    private void givenWorkOrderProductAndRoute(MesProWorkOrderDO workOrder, String productName) {
        when(workOrderMapper.selectListByIds(Set.of(WORK_ORDER_ID))).thenReturn(List.of(workOrder));
        when(itemService.getItemMap(Set.of(PRODUCT_ID))).thenReturn(Map.of(PRODUCT_ID,
                MesMdItemDO.builder().id(PRODUCT_ID).code("ITEM-PQC").name(productName).build()));
        when(routeMapper.selectListByIdsIgnoreDeleted(Set.of(ROUTE_ID))).thenReturn(List.of(route(ROUTE_ID)));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
    }

    private void givenSuccessfulPqcSubmissionContext() {
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID));
        when(pqcTaskMapper.updateSubmittedIfPending(PQC_TASK_ID, 2, "PENDING", "SUBMITTED")).thenReturn(1);
        when(processPoolEventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(9902L);
        givenPqcSignatureAndReceipt(9902L, 8802L,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);
    }

    private void givenValidPqcSubmissionValidationContext() {
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID));
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(
                regulationItem(REGULATION_VERSION_ID, "pressure", "压力", "NUMBER", "测压"),
                regulationItem(REGULATION_VERSION_ID, "appearance", "外观", "BOOLEAN", "目检")));
    }

    private void givenValidPqcSubmissionValidationContextWithoutEquipment() {
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID));
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(
                regulationItemWithoutEquipment(REGULATION_VERSION_ID, "pressure", "压力", "NUMBER", "测压"),
                regulationItemWithoutEquipment(REGULATION_VERSION_ID, "appearance", "外观", "BOOLEAN", "目检")));
    }

    private void givenPqcSignatureAndReceipt(Long eventId, Long signatureId, String inspectionResult) {
        when(signatureService.recordPqcSubmitSignature("formal-password", "PQC任务7001正式提交"))
                .thenReturn(signatureId);
        givenPqcReceipt(eventId, signatureId, inspectionResult);
    }

    private void givenPqcReceipt(Long eventId, Long signatureId, String inspectionResult) {
        LocalDateTime serverSubmitTime = LocalDateTime.of(2026, 8, 1, 9, 31);
        when(processPoolEventMapper.selectById(eventId)).thenReturn(MesProProcessPoolEventDO.builder()
                .id(eventId)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .feedbackSourceType(PQC_SOURCE_TYPE)
                .feedbackSourceId(PQC_TASK_ID)
                .signatureId(signatureId)
                .signatureUserId(LOGIN_USER_ID)
                .serverSubmitTime(serverSubmitTime)
                .build());
        when(pqcRecordMapper.selectByEventId(eventId)).thenReturn(MesProProcessPoolPqcRecordDO.builder()
                .id(eventId + 100L)
                .eventId(eventId)
                .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                .inspectionResult(inspectionResult)
                .signatureId(signatureId)
                .serverSubmitTime(serverSubmitTime)
                .build());
    }

    private MesFrontlinePqcSubmitCommand.MesFrontlinePqcSubmitCommandBuilder successfulPqcSubmitCommand() {
        return MesFrontlinePqcSubmitCommand.builder()
                .activeOrderId(ACTIVE_ORDER_ID)
                .pqcTaskId(PQC_TASK_ID)
                .productionSubmitEventId(PRODUCTION_SUBMIT_EVENT_ID)
                .regulationVersionId(REGULATION_VERSION_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .inspectionType("PATROL")
                .businessDate(LocalDate.of(2026, 8, 1))
                .shiftCode("DAY")
                .roundNo(1)
                .actualInspectionQuantity(2)
                .actualEmployeeId(8001L)
                .deviceAccountId(DEVICE_ACCOUNT_ID)
                .deviceId(DEVICE_ID)
                .workstationId(WORKSTATION_ID)
                .pqcSubmissionIdempotencyKey(PQC_IDEMPOTENCY_KEY)
                .signaturePassword("formal-password")
                .templateType("PQC_SIMPLIFIED")
                .scrapQuantity(0)
                .itemResults(List.of(
                        MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                .itemCode("pressure")
                                .selectedEquipmentId(9101L)
                                .selectedEquipmentNumber("EQ-P-001")
                                .sampleValues(List.of("0.8", "0.9"))
                                .build(),
                        MesFrontlinePqcSubmitCommand.ItemResult.builder()
                                .itemCode("appearance")
                                .selectedEquipmentId(9102L)
                                .selectedEquipmentNumber("EQ-V-002")
                                .sampleValues(List.of("合格", "合格"))
                                .build()))
                .rawPayload(Map.of(
                        "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 2)))
                .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 30));
    }

    private static MesProProcessPoolEventDO productionSubmitEvent(Long eventId, Long routeProcessId,
                                                                  Long processId) {
        return MesProProcessPoolEventDO.builder()
                .id(eventId)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .deviceAccountId(DEVICE_ACCOUNT_ID)
                .deviceId(DEVICE_ID)
                .workstationId(WORKSTATION_ID)
                .build();
    }

    private void givenPqcTaskContext(Long routeProcessId, Long processId, Long taskId, Long regulationVersionId) {
        when(regulationMapper.selectPublishedByRouteProcess(PRODUCT_ID, ROUTE_ID, 448L, routeProcessId, processId))
                .thenReturn(regulation(regulationVersionId, routeProcessId, processId));
        when(versionMapper.selectById(regulationVersionId)).thenReturn(regulationVersion(regulationVersionId, true));
        pqcTaskContextFixtures.add(pqcTask(taskId, routeProcessId, processId, regulationVersionId));
        includePublishedRegulation(routeProcessId, processId, regulationVersionId);
        includeProcessSnapshot(routeProcessId, ROUTE_ID, processId);
        lenient().when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(pqcTaskContextFixtures);
        when(regulationItemMapper.selectListByVersionId(regulationVersionId)).thenReturn(List.of(
                regulationItem(regulationVersionId, "pressure", "压力", "NUMBER", "测压"),
                regulationItem(regulationVersionId, "appearance", "外观", "CHOICE", "目视")));
        when(regulationItemEquipmentMapper.selectListByVersionId(regulationVersionId)).thenReturn(List.of(
                regulationItemEquipment(regulationVersionId, "pressure", 9101L,
                        "MCH-PQC-001", "压力检验仪", "EQ-P-001"),
                regulationItemEquipment(regulationVersionId, "appearance", 9102L,
                        "MCH-PQC-002", "外观灯箱", "EQ-V-002")));
    }

    private void givenPqcTaskContextWithoutEquipment(Long routeProcessId, Long processId, Long taskId,
                                                     Long regulationVersionId) {
        when(regulationMapper.selectPublishedByRouteProcess(PRODUCT_ID, ROUTE_ID, 448L, routeProcessId, processId))
                .thenReturn(regulation(regulationVersionId, routeProcessId, processId));
        when(versionMapper.selectById(regulationVersionId)).thenReturn(regulationVersion(regulationVersionId, true));
        pqcTaskContextFixtures.add(pqcTask(taskId, routeProcessId, processId, regulationVersionId));
        includePublishedRegulation(routeProcessId, processId, regulationVersionId);
        includeProcessSnapshot(routeProcessId, ROUTE_ID, processId);
        lenient().when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(pqcTaskContextFixtures);
        when(regulationItemMapper.selectListByVersionId(regulationVersionId)).thenReturn(List.of(
                regulationItemWithoutEquipment(regulationVersionId, "pressure", "压力", "NUMBER", "测压"),
                regulationItemWithoutEquipment(regulationVersionId, "appearance", "外观", "CHOICE", "目视")));
        when(regulationItemEquipmentMapper.selectListByVersionId(regulationVersionId)).thenReturn(List.of());
    }

    private static MesProcessPoolActiveOrderDO activeOrder(Long workOrderId, Long routeId,
                                                           LocalDateTime joinedAt) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(ACTIVE_ORDER_ID)
                .workOrderId(workOrderId)
                .routeId(routeId)
                .routeVersionId(448L)
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .joinedAt(joinedAt)
                .build();
    }

    private static MesQaInspectionRegulationDO regulation(Long regulationVersionId) {
        return regulation(regulationVersionId, ROUTE_PROCESS_ID, PROCESS_ID);
    }

    private static MesQaInspectionRegulationDO regulation(Long regulationVersionId, Long routeProcessId,
                                                          Long processId) {
        return MesQaInspectionRegulationDO.builder()
                .id(8101L)
                .productId(PRODUCT_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(448L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(regulationVersionId)
                .build();
    }

    private static MesQaInspectionRegulationVersionDO regulationVersion(Long regulationVersionId,
                                                                        boolean finalInspectionApplicable) {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(regulationVersionId)
                .regulationId(8101L)
                .versionNo("V1")
                .lifecycleStatus("PUBLISHED")
                .finalInspectionApplicable(finalInspectionApplicable)
                .finalInspectionNotApplicableReason(finalInspectionApplicable ? null : "产品QA规程明确不适用末检")
                .build();
    }

    private static MesPqcInspectionTaskDO pqcTask(Long taskId, Long routeProcessId, Long processId,
                                                  Long regulationVersionId) {
        return pqcTask(taskId, routeProcessId, processId, regulationVersionId,
                "PATROL", "DAY", 1, 2);
    }

    private static MesPqcInspectionTaskDO pqcTask(Long taskId, Long routeProcessId, Long processId,
                                                  Long regulationVersionId, String inspectionType,
                                                  String shiftCode, Integer roundNo,
                                                  Integer plannedInspectionQuantity) {
        return MesPqcInspectionTaskDO.builder()
                .id(taskId)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(448L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .regulationVersionId(regulationVersionId)
                .inspectionType(inspectionType)
                .businessDate(LocalDate.of(2026, 8, 1))
                .shiftCode(shiftCode)
                .roundNo(roundNo)
                .plannedInspectionQuantity(plannedInspectionQuantity)
                .taskStatus("PENDING")
                .build();
    }

    private static MesQaInspectionRegulationItemDO regulationItem(Long regulationVersionId, String itemCode,
                                                                  String itemName, String resultType,
                                                                  String inspectionMethod) {
        return regulationItem(regulationVersionId, itemCode, itemName, resultType, inspectionMethod, "PATROL");
    }

    private static MesQaInspectionRegulationItemDO regulationItem(Long regulationVersionId, String itemCode,
                                                                  String itemName, String resultType,
                                                                  String inspectionMethod,
                                                                  String inspectionType) {
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(regulationVersionId)
                .inspectionType(inspectionType)
                .itemCode(itemCode)
                .itemName(itemName)
                .inspectionMethod(inspectionMethod)
                .standardText("按压力泵工艺路线过程检验记录标准")
                .resultType(resultType)
                .standardLowerLimit("NUMBER".equals(resultType) ? new BigDecimal("0.700000") : null)
                .standardUpperLimit("NUMBER".equals(resultType) ? new BigDecimal("1.000000") : null)
                .standardUnit("NUMBER".equals(resultType) ? "MPa" : null)
                .standardPrecision("NUMBER".equals(resultType) ? 2 : null)
                .equipmentRequired(true)
                .build();
    }

    private static MesQaInspectionRegulationItemDO regulationItemWithoutEquipment(Long regulationVersionId,
                                                                                  String itemCode,
                                                                                  String itemName,
                                                                                  String resultType,
                                                                                  String inspectionMethod) {
        MesQaInspectionRegulationItemDO item = regulationItem(regulationVersionId, itemCode, itemName, resultType,
                inspectionMethod);
        item.setEquipmentRequired(false);
        return item;
    }

    private static MesQaInspectionRegulationItemEquipmentDO regulationItemEquipment(Long regulationVersionId,
                                                                                   String itemCode,
                                                                                   Long equipmentId,
                                                                                   String equipmentCode,
                                                                                   String equipmentName,
                                                                                   String equipmentNumber) {
        return MesQaInspectionRegulationItemEquipmentDO.builder()
                .regulationVersionId(regulationVersionId)
                .inspectionType("PATROL")
                .itemCode(itemCode)
                .equipmentId(equipmentId)
                .equipmentCode(equipmentCode)
                .equipmentName(equipmentName)
                .equipmentNumber(equipmentNumber)
                .defaultFlag(true)
                .sort(1)
                .build();
    }

    private static MesProWorkOrderDO workOrder(Long id, Long productId) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code("WO-PQC-001")
                .name("PQC 活跃订单")
                .productId(productId)
                .quantity(new BigDecimal("125.500"))
                .build();
    }

    private static MesProRouteDO route(Long id) {
        return MesProRouteDO.builder()
                .id(id)
                .code("ROUTE-PQC")
                .name("PQC 产品路线")
                .build();
    }

    private static MesProRouteProcessDO routeProcess(Long routeProcessId, Long routeId, Long processId, Integer sort) {
        return MesProRouteProcessDO.builder()
                .id(routeProcessId)
                .routeId(routeId)
                .processId(processId)
                .workstationId(6001L)
                .sort(sort)
                .build();
    }

    private static MesProRouteProcessDO routeProcessWithoutWorkstation(Long routeProcessId, Long routeId,
                                                                       Long processId, Integer sort) {
        return MesProRouteProcessDO.builder()
                .id(routeProcessId)
                .routeId(routeId)
                .processId(processId)
                .workstationId(null)
                .sort(sort)
                .build();
    }

    private void includeProcessSnapshot(Long routeProcessId, Long routeId, Long processId) {
        boolean exists = processSnapshotFixtures.stream()
                .anyMatch(snapshot -> Objects.equals(snapshot.getRouteProcessId(), routeProcessId)
                        && Objects.equals(snapshot.getProcessId(), processId));
        if (!exists) {
            processSnapshotFixtures.add(processSnapshot(routeProcessId, routeId, processId));
        }
    }

    private void includePublishedRegulation(Long routeProcessId, Long processId, Long regulationVersionId) {
        boolean exists = publishedRegulationFixtures.stream()
                .anyMatch(regulation -> Objects.equals(regulation.getRouteProcessId(), routeProcessId)
                        && Objects.equals(regulation.getProcessId(), processId)
                        && Objects.equals(regulation.getCurrentVersionId(), regulationVersionId));
        if (!exists) {
            publishedRegulationFixtures.add(regulation(regulationVersionId, routeProcessId, processId));
        }
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot(MesProRouteProcessDO routeProcess) {
        return processSnapshot(routeProcess.getId(), routeProcess.getRouteId(), routeProcess.getProcessId());
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot(Long routeProcessId, Long routeId,
                                                                              Long processId) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(routeId)
                .routeVersionId(448L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .build();
    }

    private static MesProProcessDO process(Long id, String code, String name) {
        return MesProProcessDO.builder()
                .id(id)
                .code(code)
                .name(name)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
    }

    private static MesProcessPoolTeamLeaderScopeDO scope(Long leaderUserId, String scopeType, Long employeeUserId) {
        return MesProcessPoolTeamLeaderScopeDO.builder()
                .leaderUserId(leaderUserId)
                .leaderType(LEADER_TYPE_PQC)
                .scopeType(scopeType)
                .employeeUserId(employeeUserId)
                .enabled(Boolean.TRUE)
                .build();
    }

    private static AdminUserRespDTO enabledUser(Long id, String username, String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return user;
    }

}
