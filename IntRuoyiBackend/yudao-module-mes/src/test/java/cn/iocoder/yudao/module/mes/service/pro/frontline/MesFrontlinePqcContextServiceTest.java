package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesFrontlinePqcContextServiceTest {

    private static final long WORK_ORDER_ID = 1001L;
    private static final long ROUTE_ID = 2001L;
    private static final long ROUTE_VERSION_ID = 3001L;
    private static final long PRODUCT_ID = 4001L;
    private static final long DCC_ITEM_ID = 4002L;
    private static final long ACTIVE_ORDER_ID = 5001L;
    private static final long DCC_PROJECT_ID = 6001L;
    private static final long REGULATION_ID = 7001L;
    private static final long REGULATION_VERSION_ID = 8001L;
    private static final long QA_PROCESS_ID = 9001L;

    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    private MesProWorkOrderMapper workOrderMapper;
    private MesProRouteMapper routeMapper;
    private MesProRouteVersionMapper routeVersionMapper;
    private DccProjectCodeMapper dccProjectCodeMapper;
    private MesQaInspectionRegulationMapper regulationMapper;
    private MesQaInspectionRegulationVersionMapper versionMapper;
    private MesQaInspectionRegulationProcessMapper regulationProcessMapper;
    private MesQaInspectionRegulationItemMapper regulationItemMapper;
    private MesPqcInspectionTaskMapper pqcTaskMapper;
    private MesMdItemService itemService;
    private MesFrontlinePqcContextService service;

    @BeforeEach
    void setUp() {
        activeOrderMapper = mock(MesProcessPoolActiveOrderMapper.class);
        MesProProcessPoolEventMapper processPoolEventMapper = mock(MesProProcessPoolEventMapper.class);
        workOrderMapper = mock(MesProWorkOrderMapper.class);
        routeMapper = mock(MesProRouteMapper.class);
        routeVersionMapper = mock(MesProRouteVersionMapper.class);
        dccProjectCodeMapper = mock(DccProjectCodeMapper.class);
        regulationMapper = mock(MesQaInspectionRegulationMapper.class);
        versionMapper = mock(MesQaInspectionRegulationVersionMapper.class);
        regulationProcessMapper = mock(MesQaInspectionRegulationProcessMapper.class);
        regulationItemMapper = mock(MesQaInspectionRegulationItemMapper.class);
        MesQaInspectionRegulationItemEquipmentMapper equipmentMapper =
                mock(MesQaInspectionRegulationItemEquipmentMapper.class);
        pqcTaskMapper = mock(MesPqcInspectionTaskMapper.class);
        MesPqcInspectionPieceDetailMapper pieceDetailMapper = mock(MesPqcInspectionPieceDetailMapper.class);
        itemService = mock(MesMdItemService.class);
        MesProcessPoolTeamLeaderScopeMapper scopeMapper = mock(MesProcessPoolTeamLeaderScopeMapper.class);
        AdminUserApi adminUserApi = mock(AdminUserApi.class);
        MesProcessPoolEventService eventService = mock(MesProcessPoolEventService.class);
        MesProProcessPoolPqcRecordMapper pqcRecordMapper = mock(MesProProcessPoolPqcRecordMapper.class);
        MesProBatchRecordExecutionSignatureService signatureService =
                mock(MesProBatchRecordExecutionSignatureService.class);
        service = new MesFrontlinePqcContextServiceImpl(activeOrderMapper, processPoolEventMapper,
                workOrderMapper, routeMapper, routeVersionMapper, dccProjectCodeMapper,
                regulationMapper, versionMapper, regulationProcessMapper, regulationItemMapper,
                equipmentMapper, pqcTaskMapper, pieceDetailMapper, itemService, scopeMapper,
                adminUserApi, eventService, pqcRecordMapper, signatureService);
    }

    @Test
    void listActiveOrdersReturnsEveryActiveOrderWithoutPendingTaskFilter() {
        List<MesProcessPoolActiveOrderDO> activeOrders = List.of(
                activeOrder(5001L, 1001L, LocalDateTime.of(2026, 8, 12, 8, 0)),
                activeOrder(5002L, 1001L, LocalDateTime.of(2026, 8, 12, 8, 1)),
                activeOrder(5003L, 1001L, LocalDateTime.of(2026, 8, 12, 8, 2)));
        when(activeOrderMapper.selectActiveList()).thenReturn(activeOrders);
        when(workOrderMapper.selectListByIds(any())).thenReturn(List.of(workOrder(1001L)));
        when(routeMapper.selectListByIdsIgnoreDeleted(any())).thenReturn(List.of(route()));
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion(PRODUCT_ID));
        when(itemService.getItemMap(Set.of(PRODUCT_ID))).thenReturn(Map.of(PRODUCT_ID, productItem()));

        List<MesFrontlineActiveOrderCandidate> result = service.listActiveOrders();

        assertEquals(3, result.size());
        assertEquals(List.of(5003L, 5002L, 5001L),
                result.stream().map(MesFrontlineActiveOrderCandidate::activeOrderId).toList());
        assertEquals(List.of(1001L, 1001L, 1001L),
                result.stream().map(MesFrontlineActiveOrderCandidate::workOrderId).toList());
        verify(pqcTaskMapper, never()).selectActiveOrderIdsByTaskStatus(any(), any());
    }

    @Test
    void listProcessesReturnsAllQaProcessesOwnedByResolvedDccProjectEvenWithoutPendingTasks() {
        givenDccOwnedQaContext();
        MesQaInspectionRegulationProcessDO secondProcess = qaProcess(9002L, "ID-QA-002", "精洗", 2);
        when(regulationProcessMapper.selectListByVersionId(REGULATION_VERSION_ID))
                .thenReturn(List.of(qaProcess(QA_PROCESS_ID, "ID-QA-001", "清洗", 1), secondProcess));
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(
                qaItem(QA_PROCESS_ID, "ID-001"), qaItem(secondProcess.getId(), "ID-002")));
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of());

        List<MesFrontlinePqcProcessCandidate> result = service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID);

        assertEquals(2, result.size());
        assertEquals(List.of("清洗", "精洗"),
                result.stream().map(MesFrontlinePqcProcessCandidate::qaProcessName).toList());
        assertEquals(DCC_PROJECT_ID, result.get(0).dccProjectCodeId());
        assertEquals(REGULATION_VERSION_ID, result.get(0).regulationVersionId());
        assertNull(result.get(0).pqcTaskId());
        verify(regulationMapper).selectByDccProjectCodeId(DCC_PROJECT_ID);
    }

    @Test
    void pendingTaskWithUnknownQaProcessFailsFast() {
        givenDccOwnedQaContext();
        when(regulationProcessMapper.selectListByVersionId(REGULATION_VERSION_ID))
                .thenReturn(List.of(qaProcess(QA_PROCESS_ID, "ID-QA-001", "清洗", 1)));
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID))
                .thenReturn(List.of(qaItem(QA_PROCESS_ID, "ID-001")));
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder().id(9101L).activeOrderId(ACTIVE_ORDER_ID)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(9999L)
                        .taskStatus("PENDING").build()));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.listProcessesByActiveOrder(WORK_ORDER_ID, ROUTE_ID));

        assertEquals(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH.getCode(), error.getCode());
    }

    private void givenDccOwnedQaContext() {
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(activeOrder(ACTIVE_ORDER_ID, WORK_ORDER_ID, LocalDateTime.of(2026, 8, 12, 8, 0)));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID));
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route());
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion(PRODUCT_ID, DCC_ITEM_ID));
        when(itemService.getItemMap(Set.of(PRODUCT_ID, DCC_ITEM_ID))).thenReturn(Map.of(
                PRODUCT_ID, productItem(),
                DCC_ITEM_ID, MesMdItemDO.builder().id(DCC_ITEM_ID).code("ID").name("DCC项目代码").build()));
        DccProjectCodeDO project = DccProjectCodeDO.builder().id(DCC_PROJECT_ID)
                .projectCode("ID").projectName("球囊扩张压力泵").productMasterId(PRODUCT_ID).build();
        when(dccProjectCodeMapper.selectEnabledList()).thenReturn(List.of(project));
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(
                MesQaInspectionRegulationDO.builder().id(REGULATION_ID).dccProjectCodeId(DCC_PROJECT_ID)
                        .ownerModule(MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                        .lifecycleStatus("PUBLISHED").currentVersionId(REGULATION_VERSION_ID).build());
        when(versionMapper.selectById(REGULATION_VERSION_ID)).thenReturn(
                MesQaInspectionRegulationVersionDO.builder().id(REGULATION_VERSION_ID)
                        .regulationId(REGULATION_ID).versionNo("G/0").lifecycleStatus("PUBLISHED")
                        .finalInspectionApplicable(true).build());
    }

    private static MesProcessPoolActiveOrderDO activeOrder(long id, long workOrderId, LocalDateTime joinedAt) {
        return MesProcessPoolActiveOrderDO.builder().id(id).workOrderId(workOrderId).routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID).activeStatus("ACTIVE").businessStatus("ACTIVE")
                .joinedAt(joinedAt).build();
    }

    private static MesProWorkOrderDO workOrder(long id) {
        return MesProWorkOrderDO.builder().id(id).code("WO-" + id).name("活跃订单")
                .productId(PRODUCT_ID).quantity(new BigDecimal("100")).build();
    }

    private static MesProRouteDO route() {
        return MesProRouteDO.builder().id(ROUTE_ID).code("RT-ID").name("球囊扩张压力泵路线").build();
    }

    private static MesProRouteVersionDO routeVersion(Long... productIds) {
        List<Map<String, Long>> products = java.util.Arrays.stream(productIds)
                .map(id -> Map.of("itemId", id)).toList();
        return MesProRouteVersionDO.builder().id(ROUTE_VERSION_ID).routeId(ROUTE_ID).active(true)
                .lifecycleStatus("ACTIVE").routeSnapshotJson(JsonUtils.toJsonString(
                        Map.of("configSnapshots", Map.of("products", products)))).build();
    }

    private static MesMdItemDO productItem() {
        return MesMdItemDO.builder().id(PRODUCT_ID).code("PUMP-001").name("球囊扩张压力泵").build();
    }

    private static MesQaInspectionRegulationProcessDO qaProcess(long id, String code, String name, int sort) {
        return MesQaInspectionRegulationProcessDO.builder().id(id).regulationVersionId(REGULATION_VERSION_ID)
                .processCode(code).processName(name).sort(sort).build();
    }

    private static MesQaInspectionRegulationItemDO qaItem(long qaProcessId, String code) {
        return MesQaInspectionRegulationItemDO.builder().regulationVersionId(REGULATION_VERSION_ID)
                .qaProcessId(qaProcessId).inspectionType("PATROL").itemCode(code).itemName("外观")
                .inspectionMethod("目测").standardText("应符合要求").inspectionTool("目测")
                .samplingPlanText("按规程抽样").resultType("BOOLEAN").equipmentRequired(false).build();
    }
}
