package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
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
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
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
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private MesProProcessPoolEventMapper processPoolEventMapper;
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private MesQaInspectionRegulationService regulationService;
    private MesMdItemService itemService;
    private MesFrontlinePqcContextService service;

    @BeforeEach
    void setUp() {
        activeOrderMapper = mock(MesProcessPoolActiveOrderMapper.class);
        processPoolEventMapper = mock(MesProProcessPoolEventMapper.class);
        processSnapshotMapper = mock(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
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
        regulationService = mock(MesQaInspectionRegulationService.class);
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
                processSnapshotMapper,
                workOrderMapper, routeMapper, routeVersionMapper, dccProjectCodeMapper,
                regulationMapper, versionMapper, regulationProcessMapper, regulationItemMapper,
                equipmentMapper, regulationService, pqcTaskMapper, pieceDetailMapper, itemService, scopeMapper,
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

    @Test
    void listProcessesByActiveOrderIdUsesLockedQaSnapshotAndDedicatedProjection() {
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(ACTIVE_ORDER_ID, WORK_ORDER_ID,
                LocalDateTime.of(2026, 8, 12, 8, 0));
        activeOrder.setDccProjectCodeId(DCC_PROJECT_ID);
        activeOrder.setQaRegulationId(REGULATION_ID);
        activeOrder.setQaRegulationVersionId(REGULATION_VERSION_ID);
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder);
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID));
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route());
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess firstProcess =
                qaPublishedProcess(QA_PROCESS_ID, "ID-QA-001", "清洗", 1,
                        qaPublishedItem("ID-001", List.of("FIRST", "PATROL", "FINAL")));
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess secondProcess =
                qaPublishedProcess(9002L, "ID-QA-002", "精洗", 2,
                        qaPublishedItem("ID-002", List.of("PATROL")));
        when(regulationService.getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, REGULATION_VERSION_ID))
                .thenReturn(lockedQaAggregate("RETIRED", firstProcess, secondProcess));
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder().id(9101L).activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                        .inspectionRuleKey("PATROL_PM").inspectionType("PATROL")
                        .businessDate(LocalDate.of(2026, 8, 12)).shiftCode("PM").roundNo(1)
                        .plannedInspectionQuantity(5).taskStatus("CONFIRMED").build(),
                MesPqcInspectionTaskDO.builder().id(9102L).activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                        .inspectionRuleKey("FIRST").inspectionType("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 12)).shiftCode("FIRST").roundNo(1)
                        .plannedInspectionQuantity(5).taskStatus("PENDING").build(),
                MesPqcInspectionTaskDO.builder().id(9103L).activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                        .inspectionRuleKey("PATROL_AM").inspectionType("PATROL")
                        .businessDate(LocalDate.of(2026, 8, 12)).shiftCode("AM").roundNo(1)
                        .plannedInspectionQuantity(5).taskStatus("SUBMITTED").build(),
                MesPqcInspectionTaskDO.builder().id(9104L).activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                        .inspectionRuleKey("FINAL").inspectionType("FINAL")
                        .businessDate(LocalDate.of(2026, 8, 12)).shiftCode("FINAL").roundNo(1)
                        .plannedInspectionQuantity(3).taskStatus("CANCELLED").build()));
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshot(30001L, 40001L),
                processSnapshot(30003L, 40003L)));
        when(processPoolEventMapper.selectProductionSubmitsByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(List.of(
                        productionSubmitEvent(9201L, 30001L, 40001L),
                        productionSubmitEvent(9202L, 30002L, 40002L),
                        productionSubmitEvent(9203L, 30003L, 40003L)));

        List<MesFrontlinePqcProcessRespVO> result = service.listProcessesByActiveOrder(ACTIVE_ORDER_ID);

        assertEquals(2, result.size());
        assertEquals(List.of("清洗", "精洗"),
                result.stream().map(MesFrontlinePqcProcessRespVO::getQaProcessName).toList());
        assertEquals(ACTIVE_ORDER_ID, result.get(0).getActiveOrderId());
        assertEquals(REGULATION_VERSION_ID, result.get(0).getRegulationVersionId());
        assertEquals(List.of("FIRST", "PATROL_AM", "PATROL_PM", "FINAL"),
                result.get(0).getInspectionTypeRules().stream()
                        .map(MesFrontlinePqcProcessRespVO.QaInspectionTypeRule::getKey).toList());
        assertEquals("上午巡检", result.get(0).getInspectionTypeRules().get(1).getLabel());
        assertEquals("上午第1轮", result.get(0).getInspectionTypeRules().get(1).getRoundLabel());
        assertEquals("BY_SHIFT", result.get(0).getInspectionTypeRules().get(1).getTaskRule());
        assertEquals("REQUIRED", result.get(0).getInspectionTypeRules().get(1).getReleaseGate());
        MesFrontlinePqcProcessRespVO.PqcInspectionItem item = result.get(0).getInspectionItems().get(0);
        assertEquals(1, item.getItemSort());
        assertEquals("ID-001", item.getItemCode());
        assertEquals(List.of("FIRST", "PATROL", "FINAL"), item.getApplicableInspectionTypes());
        assertEquals(5, item.getFirstInspectionQuantity());
        assertEquals(new BigDecimal("25.00"), item.getPatrolInspectionRatio());
        assertEquals(true, item.getCritical());
        assertEquals("任一件不合格即失败", item.getFailureRule());
        assertEquals("QA 发布来源", item.getSourceNote());
        assertEquals(12, item.getSourceOriginalPage());
        assertEquals("原始项目", item.getSourceOriginalItem());
        assertEquals("原始摘录", item.getSourceOriginalExcerpt());
        assertEquals("原始方法", item.getSourceOriginalMethod());
        assertEquals(9102L, result.get(0).getPqcTaskId());
        assertEquals("FIRST", result.get(0).getInspectionRuleKey());
        assertEquals("PENDING", result.get(0).getTaskStatus());
        assertEquals("MIXED", result.get(0).getTaskSummary().getState());
        assertEquals(4, result.get(0).getTaskSummary().getTotalCount());
        assertEquals(1, result.get(0).getTaskSummary().getPendingCount());
        assertEquals(1, result.get(0).getTaskSummary().getSubmittedCount());
        assertEquals(1, result.get(0).getTaskSummary().getConfirmedCount());
        assertEquals(1, result.get(0).getTaskSummary().getCancelledCount());
        assertEquals(List.of("FIRST", "PATROL_AM", "PATROL_PM", "FINAL"),
                result.get(0).getPqcTaskOptions().stream()
                        .map(MesFrontlinePqcProcessRespVO.PqcTaskOption::getInspectionRuleKey).toList());
        assertEquals("FIRST", result.get(0).getPqcTaskOptions().get(0).getInspectionRuleKey());
        assertEquals(List.of("PENDING", "SUBMITTED", "CONFIRMED", "CANCELLED"),
                result.get(0).getPqcTaskOptions().stream()
                        .map(MesFrontlinePqcProcessRespVO.PqcTaskOption::getTaskStatus).toList());
        assertEquals(List.of(10, 20, 30, 40), result.get(0).getPqcTaskOptions().stream()
                .map(MesFrontlinePqcProcessRespVO.PqcTaskOption::getRuleSort).toList());
        assertEquals("PATROL_AM",
                result.get(0).getPqcTaskOptions().get(1).getInspectionTypeRule().getKey());
        assertEquals("上午巡检",
                result.get(0).getPqcTaskOptions().get(1).getInspectionTypeRule().getLabel());
        assertEquals(List.of("FIRST", "PATROL", "FINAL"), result.get(0).getPqcTaskOptions().get(1)
                .getInspectionItems().get(0).getApplicableInspectionTypes());
        assertNull(result.get(1).getTaskStatus());
        assertNull(result.get(1).getInspectionRuleKey());
        assertEquals("NOT_CREATED", result.get(1).getTaskSummary().getState());
        assertEquals(0, result.get(1).getTaskSummary().getTotalCount());
        assertEquals(0, result.get(1).getTaskSummary().getPendingCount());
        assertEquals(0, result.get(1).getTaskSummary().getSubmittedCount());
        assertEquals(0, result.get(1).getTaskSummary().getConfirmedCount());
        assertEquals(0, result.get(1).getTaskSummary().getCancelledCount());
        assertEquals(List.of(), result.get(1).getPqcTaskOptions());
        assertEquals(List.of(9203L, 9201L), result.get(0).getProductionSubmitCandidates().stream()
                .map(MesFrontlinePqcProcessRespVO.ProductionSubmitCandidate::getEventId).toList());
        assertEquals(ACTIVE_ORDER_ID, result.get(0).getProductionSubmitCandidates().get(0).getActiveOrderId());
        assertEquals(30003L, result.get(0).getProductionSubmitCandidates().get(0).getRouteProcessId());
        assertEquals(40003L, result.get(0).getProductionSubmitCandidates().get(0).getProcessId());
        assertEquals(30001L, result.get(0).getProductionSubmitCandidates().get(1).getRouteProcessId());
        assertEquals(40001L, result.get(0).getProductionSubmitCandidates().get(1).getProcessId());
        assertEquals(result.get(0).getProductionSubmitCandidates(), result.get(1).getProductionSubmitCandidates());
        verify(processSnapshotMapper).selectListByActiveOrderId(ACTIVE_ORDER_ID);
        verify(processPoolEventMapper).selectProductionSubmitsByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID);
        verify(regulationService).getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, REGULATION_VERSION_ID);
        verify(regulationMapper, never()).selectById(REGULATION_ID);
        verify(versionMapper, never()).selectById(REGULATION_VERSION_ID);
        verify(regulationProcessMapper, never()).selectListByVersionId(REGULATION_VERSION_ID);
        verify(regulationItemMapper, never()).selectListByVersionId(REGULATION_VERSION_ID);
    }

    @Test
    void listProcessesByActiveOrderIdRejectsNullTaskRecordWithServiceException() {
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(ACTIVE_ORDER_ID, WORK_ORDER_ID,
                LocalDateTime.of(2026, 8, 12, 8, 0));
        activeOrder.setDccProjectCodeId(DCC_PROJECT_ID);
        activeOrder.setQaRegulationId(REGULATION_ID);
        activeOrder.setQaRegulationVersionId(REGULATION_VERSION_ID);
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder);
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID));
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route());
        when(regulationService.getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, REGULATION_VERSION_ID))
                .thenReturn(lockedQaAggregate("PUBLISHED",
                        qaPublishedProcess(QA_PROCESS_ID, "ID-QA-001", "清洗", 1,
                                qaPublishedItem("ID-001", List.of("FIRST")))));
        List<MesPqcInspectionTaskDO> invalidTasks = new java.util.ArrayList<>();
        invalidTasks.add(null);
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(invalidTasks);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.listProcessesByActiveOrder(ACTIVE_ORDER_ID));

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

    private static MesQaInspectionRegulationPublishedVersionRespVO lockedQaAggregate(
            String lifecycleStatus,
            MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess... processes) {
        return MesQaInspectionRegulationPublishedVersionRespVO.builder()
                .dccProjectCodeId(DCC_PROJECT_ID)
                .regulationId(REGULATION_ID)
                .publishedVersionId(REGULATION_VERSION_ID)
                .versionNo("G/0")
                .lifecycleStatus(lifecycleStatus)
                .finalInspectionApplicable(true)
                .inspectionTypeRules(List.of(
                        qaInspectionTypeRule("FIRST", "FIRST", "首检", "首检第1轮", 10),
                        qaInspectionTypeRule("PATROL_AM", "PATROL", "上午巡检", "上午第1轮", 20),
                        qaInspectionTypeRule("PATROL_PM", "PATROL", "下午巡检", "下午第1轮", 30),
                        qaInspectionTypeRule("FINAL", "FINAL", "末检", "末检第1轮", 40)))
                .processes(List.of(processes))
                .build();
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule qaInspectionTypeRule(
            String key, String inspectionType, String label, String roundLabel, int fixedQuantity) {
        return MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule.builder()
                .key(key)
                .inspectionType(inspectionType)
                .label(label)
                .roundLabel(roundLabel)
                .required(true)
                .fixedQuantity(fixedQuantity)
                .taskRule("PATROL".equals(inspectionType) ? "BY_SHIFT" : "ONCE")
                .releaseGate("REQUIRED")
                .build();
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess qaPublishedProcess(
            long id,
            String code,
            String name,
            int sort,
            MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem... items) {
        return MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess.builder()
                .qaProcessId(id)
                .processCode(code)
                .processName(name)
                .sort(sort)
                .items(List.of(items))
                .build();
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem qaPublishedItem(
            String code,
            List<String> applicableInspectionTypes) {
        return MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem.builder()
                .itemSort(1)
                .itemCode(code)
                .itemName("外观")
                .inspectionMethod("目测")
                .standardText("应符合要求")
                .inspectionTool("目测")
                .samplingPlanText("按规程抽样")
                .resultType("BOOLEAN")
                .equipmentRequired(false)
                .equipmentOptions(List.of())
                .applicableInspectionTypes(applicableInspectionTypes)
                .firstInspectionQuantity(applicableInspectionTypes.contains("FIRST") ? 5 : null)
                .patrolInspectionRatio(applicableInspectionTypes.contains("PATROL") ? new BigDecimal("25.00") : null)
                .critical(true)
                .failureRule("任一件不合格即失败")
                .sourceNote("QA 发布来源")
                .sourceOriginalPage(12)
                .sourceOriginalItem("原始项目")
                .sourceOriginalExcerpt("原始摘录")
                .sourceOriginalMethod("原始方法")
                .build();
    }

    private static MesQaInspectionRegulationItemDO qaItem(long qaProcessId, String code) {
        return qaItem(qaProcessId, code, "PATROL");
    }

    private static MesQaInspectionRegulationItemDO qaItem(long qaProcessId, String code, String inspectionType) {
        return MesQaInspectionRegulationItemDO.builder().regulationVersionId(REGULATION_VERSION_ID)
                .qaProcessId(qaProcessId).itemSort(1).inspectionType(inspectionType)
                .itemCode(code).itemName("外观")
                .inspectionMethod("目测").standardText("应符合要求").inspectionTool("目测")
                .samplingPlanText("按规程抽样").resultType("BOOLEAN").equipmentRequired(false)
                .firstInspectionQuantity("FIRST".equals(inspectionType) ? 5 : null)
                .patrolInspectionRatio("PATROL".equals(inspectionType) ? new BigDecimal("25.00") : null)
                .critical(true).failureRule("任一件不合格即失败").sourceNote("QA 发布来源")
                .sourceOriginalPage(12).sourceOriginalItem("原始项目")
                .sourceOriginalExcerpt("原始摘录").sourceOriginalMethod("原始方法").build();
    }

    private static String inspectionTypeRulesJson() {
        return """
                [
                  {"key":"FIRST","inspectionType":"FIRST","label":"首检","roundLabel":"首检第1轮","required":true,"fixedQuantity":5,"taskRule":"ON_START","releaseGate":"REQUIRED"},
                  {"key":"PATROL_AM","inspectionType":"PATROL","label":"上午巡检","roundLabel":"上午第1轮","required":true,"taskRule":"BY_SHIFT","releaseGate":"REQUIRED"},
                  {"key":"PATROL_PM","inspectionType":"PATROL","label":"下午巡检","roundLabel":"下午第1轮","required":true,"taskRule":"BY_SHIFT","releaseGate":"REQUIRED"},
                  {"key":"FINAL","inspectionType":"FINAL","label":"末检","roundLabel":"末检第1轮","required":true,"fixedQuantity":3,"taskRule":"ON_COMPLETE","releaseGate":"REQUIRED"}
                ]
                """;
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot(long routeProcessId, long processId) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(ACTIVE_ORDER_ID).workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID).routeProcessId(routeProcessId).processId(processId).build();
    }

    private static MesProProcessPoolEventDO productionSubmitEvent(long id, long routeProcessId, long processId) {
        return MesProProcessPoolEventDO.builder().id(id)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID)
                .routeProcessId(routeProcessId).processId(processId)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 12, 10, 0).plusMinutes(id)).build();
    }
}
