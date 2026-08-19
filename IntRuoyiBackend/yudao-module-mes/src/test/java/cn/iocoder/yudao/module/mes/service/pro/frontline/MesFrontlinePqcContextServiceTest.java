package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
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
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesFrontlinePqcContextServiceTest {

    @Test
    void processResponseKeepsTaskIdentityOnlyInTaskOptions() {
        Set<String> forbiddenProcessFields = Set.of(
                "pqcTaskId", "inspectionRuleKey", "taskStatus", "inspectionType",
                "businessDate", "shiftCode", "roundNo", "plannedInspectionQuantity");

        List<String> duplicatedTaskFields = Arrays.stream(MesFrontlinePqcProcessRespVO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .filter(forbiddenProcessFields::contains)
                .toList();

        assertEquals(List.of(), duplicatedTaskFields);
    }

    private static final long WORK_ORDER_ID = 1001L;
    private static final long ROUTE_ID = 2001L;
    private static final long ROUTE_VERSION_ID = 3001L;
    private static final long PRODUCT_ID = 4001L;
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
    private MesQaInspectionRegulationItemEquipmentMapper equipmentMapper;
    private MesPqcInspectionPieceDetailMapper pieceDetailMapper;
    private MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    private AdminUserApi adminUserApi;
    private MesProcessPoolEventService eventService;
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private MesProBatchRecordExecutionSignatureService signatureService;
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
        equipmentMapper = mock(MesQaInspectionRegulationItemEquipmentMapper.class);
        regulationService = mock(MesQaInspectionRegulationService.class);
        pqcTaskMapper = mock(MesPqcInspectionTaskMapper.class);
        pieceDetailMapper = mock(MesPqcInspectionPieceDetailMapper.class);
        itemService = mock(MesMdItemService.class);
        scopeMapper = mock(MesProcessPoolTeamLeaderScopeMapper.class);
        adminUserApi = mock(AdminUserApi.class);
        eventService = mock(MesProcessPoolEventService.class);
        pqcRecordMapper = mock(MesProProcessPoolPqcRecordMapper.class);
        signatureService = mock(MesProBatchRecordExecutionSignatureService.class);
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
    void lockedQaProjectionRejectsTaskFromUnknownQaProcess() {
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(ACTIVE_ORDER_ID, WORK_ORDER_ID,
                LocalDateTime.of(2026, 8, 12, 8, 0));
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder);
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID));
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route());
        when(regulationService.getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, REGULATION_VERSION_ID))
                .thenReturn(lockedQaAggregate("PUBLISHED",
                        qaPublishedProcess(QA_PROCESS_ID, "ID-QA-001", "清洗", 1,
                                qaPublishedItem("ID-001", List.of("FIRST")))));
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshot(30001L, 40001L)));
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder().id(9101L).activeOrderId(ACTIVE_ORDER_ID)
                        .routeProcessId(30001L).processId(40001L)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(9999L)
                        .inspectionRuleKey("FIRST").inspectionType("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 12)).shiftCode("FIRST").roundNo(1)
                        .plannedInspectionQuantity(5).taskStatus("PENDING").build()));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.listProcessesByActiveOrder(ACTIVE_ORDER_ID));

        assertEquals(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH.getCode(), error.getCode());
        verify(regulationService).getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, REGULATION_VERSION_ID);
        verify(dccProjectCodeMapper, never()).selectEnabledList();
    }

    @Test
    void submitPqcInspectionAutoBindsUniqueProductionSubmitFromSameActiveOrderProcess() {
        long loginUserId = 3001L;
        long actualEmployeeId = 3002L;
        long pqcTaskId = 9101L;
        long productionSubmitEventId = 9202L;
        long pqcEventId = 9401L;
        long signatureId = 9301L;
        LocalDateTime submitTime = LocalDateTime.of(2026, 8, 19, 15, 30);
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(ACTIVE_ORDER_ID, WORK_ORDER_ID, submitTime);
        when(pqcTaskMapper.selectByIdForUpdate(pqcTaskId)).thenReturn(pendingTask(pqcTaskId));
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder);
        when(processSnapshotMapper.selectByActiveOrderAndProcess(ACTIVE_ORDER_ID, 30001L, 40001L))
                .thenReturn(processSnapshot(30001L, 40001L));
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshot(30001L, 40001L),
                processSnapshot(30002L, 40002L)));
        when(processPoolEventMapper.selectProductionSubmitsByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(List.of(
                        productionSubmitEvent(9201L, 30002L, 40002L),
                        productionSubmitEvent(productionSubmitEventId, 30001L, 40001L)));
        when(scopeMapper.selectActiveScopesByLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC))
                .thenReturn(List.of(pqcEmployeeScope(loginUserId, actualEmployeeId)));
        when(adminUserApi.getUserList(any())).thenReturn(List.of(enabledUser(loginUserId), enabledUser(actualEmployeeId)));
        when(regulationProcessMapper.selectById(QA_PROCESS_ID)).thenReturn(MesQaInspectionRegulationProcessDO.builder()
                .id(QA_PROCESS_ID).regulationVersionId(REGULATION_VERSION_ID).build());
        when(versionMapper.selectById(REGULATION_VERSION_ID)).thenReturn(MesQaInspectionRegulationVersionDO.builder()
                .id(REGULATION_VERSION_ID).regulationId(REGULATION_ID).lifecycleStatus("PUBLISHED").build());
        when(regulationMapper.selectById(REGULATION_ID)).thenReturn(MesQaInspectionRegulationDO.builder()
                .id(REGULATION_ID).dccProjectCodeId(DCC_PROJECT_ID).build());
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(DccProjectCodeDO.builder()
                .id(DCC_PROJECT_ID).build());
        when(regulationItemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(publishedItem()));
        when(equipmentMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of());
        when(pqcTaskMapper.updateSubmittedIfPending(pqcTaskId, 1, null, "PENDING", "SUBMITTED"))
                .thenReturn(1);
        when(pqcTaskMapper.updateSubmittedIfPending(anyLong(), any(), anyString(), anyString(), anyString()))
                .thenReturn(1);
        when(signatureService.recordPqcSubmitSignature(actualEmployeeId, "sign-123", "PQC任务" + pqcTaskId + "正式提交"))
                .thenReturn(signatureId);
        when(eventService.createPqcInspectionEvent(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(pqcEventId);
        when(pqcTaskMapper.updateSubmittedEventId(pqcTaskId, pqcEventId)).thenReturn(1);
        when(processPoolEventMapper.selectById(pqcEventId)).thenReturn(MesProProcessPoolEventDO.builder()
                .id(pqcEventId)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .feedbackSourceType("MES_PQC_INSPECTION_TASK")
                .feedbackSourceId(pqcTaskId)
                .signatureId(signatureId)
                .serverSubmitTime(submitTime)
                .build());
        when(pqcRecordMapper.selectByEventId(pqcEventId)).thenReturn(MesProProcessPoolPqcRecordDO.builder()
                .id(9501L)
                .eventId(pqcEventId)
                .productionSubmitEventId(productionSubmitEventId)
                .signatureId(signatureId)
                .inspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .serverSubmitTime(submitTime)
                .build());

        MesFrontlinePqcSubmitCommand command = MesFrontlinePqcSubmitCommand.builder()
                .activeOrderId(ACTIVE_ORDER_ID)
                .pqcTaskId(pqcTaskId)
                .regulationVersionId(REGULATION_VERSION_ID)
                .qaProcessId(QA_PROCESS_ID)
                .actualEmployeeId(actualEmployeeId)
                .actualInspectionQuantity(1)
                .scrapQuantity(0)
                .signaturePassword("sign-123")
                .itemResults(List.of(MesFrontlinePqcSubmitCommand.ItemResult.builder()
                        .itemCode("ID-001")
                        .sampleValues(List.of("合格"))
                        .build()))
                .rawPayload(Map.of())
                .clientSubmitTime(submitTime)
                .build();

        MesFrontlinePqcSubmitResult result = service.submitPqcInspection(loginUserId, command);

        assertEquals(pqcEventId, result.pqcEventId());
        assertEquals(productionSubmitEventId, command.getProductionSubmitEventId());
        ArgumentCaptor<MesProcessPoolCreatePqcInspectionReqDTO> requestCaptor =
                ArgumentCaptor.forClass(MesProcessPoolCreatePqcInspectionReqDTO.class);
        verify(eventService).createPqcInspectionEvent(requestCaptor.capture());
        MesProcessPoolCreatePqcInspectionReqDTO request = requestCaptor.getValue();
        assertEquals(productionSubmitEventId, request.getProductionSubmitEventId());
        assertNotNull(request.getRawPayload());
        assertEquals(productionSubmitEventId,
                JsonUtils.parseObject(request.getRawPayload(), Map.class).get("productionSubmitEventId"));
    }

    @Test
    void lockedQaProjectionIgnoresCancelledLegacyTaskOutsideCurrentQaSnapshot() {
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(ACTIVE_ORDER_ID, WORK_ORDER_ID,
                LocalDateTime.of(2026, 8, 12, 8, 0));
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder);
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID));
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route());
        when(regulationService.getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, REGULATION_VERSION_ID))
                .thenReturn(lockedQaAggregate("PUBLISHED",
                        qaPublishedProcess(QA_PROCESS_ID, "ID-QA-001", "清洗", 1,
                                qaPublishedItem("ID-001", List.of("FIRST")))));
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder().id(9001L).activeOrderId(ACTIVE_ORDER_ID)
                        .regulationVersionId(39L).qaProcessId(null)
                        .inspectionType("FINAL").businessDate(LocalDate.of(2026, 8, 8))
                        .shiftCode("FINAL").roundNo(1).plannedInspectionQuantity(3)
                        .taskStatus("CANCELLED").build(),
                MesPqcInspectionTaskDO.builder().id(9101L).activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                        .routeProcessId(30001L).processId(40001L)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                        .qaItemCode("ID-001")
                        .inspectionRuleKey("FIRST").inspectionType("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 12)).shiftCode("FIRST").roundNo(1)
                        .plannedInspectionQuantity(5).taskStatus("PENDING").build()));
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshot(30001L, 40001L)));
        when(processPoolEventMapper.selectProductionSubmitsByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(List.of());

        List<MesFrontlinePqcProcessRespVO> result = service.listProcessesByActiveOrder(ACTIVE_ORDER_ID);

        assertEquals(1, result.size());
        assertEquals(List.of(9101L), result.get(0).getPqcTaskOptions().stream()
                .map(MesFrontlinePqcProcessRespVO.PqcTaskOption::getPqcTaskId).toList());
        assertEquals("ID-001", result.get(0).getPqcTaskOptions().get(0).getQaItemCode());
        assertEquals(1, result.get(0).getTaskSummary().getTotalCount());
        assertEquals(0, result.get(0).getTaskSummary().getCancelledCount());
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
                        .routeProcessId(30001L).processId(40001L)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                        .qaItemCode("ID-001")
                        .inspectionRuleKey("PATROL_PM").inspectionType("PATROL")
                        .businessDate(LocalDate.of(2026, 8, 12)).shiftCode("PM").roundNo(1)
                        .plannedInspectionQuantity(5).taskStatus("CONFIRMED").build(),
                MesPqcInspectionTaskDO.builder().id(9102L).activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                        .routeProcessId(30001L).processId(40001L)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                        .qaItemCode("ID-001")
                        .inspectionRuleKey("FIRST").inspectionType("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 12)).shiftCode("FIRST").roundNo(1)
                        .plannedInspectionQuantity(5).taskStatus("PENDING").build(),
                MesPqcInspectionTaskDO.builder().id(9103L).activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                        .routeProcessId(30001L).processId(40001L)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                        .qaItemCode("ID-001")
                        .inspectionRuleKey("PATROL_AM").inspectionType("PATROL")
                        .businessDate(LocalDate.of(2026, 8, 12)).shiftCode("AM").roundNo(1)
                        .plannedInspectionQuantity(5).taskStatus("SUBMITTED").build(),
                MesPqcInspectionTaskDO.builder().id(9104L).activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                        .routeProcessId(30003L).processId(40003L)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                        .qaItemCode("ID-001")
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
        MesFrontlinePqcProcessRespVO.PqcTaskOption pendingTask = result.get(0).getPqcTaskOptions().get(0);
        assertEquals(9102L, pendingTask.getPqcTaskId());
        assertEquals("FIRST", pendingTask.getInspectionRuleKey());
        assertEquals("PENDING", pendingTask.getTaskStatus());
        assertEquals("FIRST", pendingTask.getInspectionType());
        assertEquals(LocalDate.of(2026, 8, 12), pendingTask.getBusinessDate());
        assertEquals("FIRST", pendingTask.getShiftCode());
        assertEquals(1, pendingTask.getRoundNo());
        assertEquals(5, pendingTask.getPlannedInspectionQuantity());
        assertEquals("MIXED", result.get(0).getTaskSummary().getState());
        assertEquals(4, result.get(0).getTaskSummary().getTotalCount());
        assertEquals(1, result.get(0).getTaskSummary().getPendingCount());
        assertEquals(1, result.get(0).getTaskSummary().getSubmittedCount());
        assertEquals(1, result.get(0).getTaskSummary().getConfirmedCount());
        assertEquals(1, result.get(0).getTaskSummary().getCancelledCount());
        assertEquals(List.of("FIRST", "PATROL_AM", "PATROL_PM", "FINAL"),
                result.get(0).getPqcTaskOptions().stream()
                        .map(MesFrontlinePqcProcessRespVO.PqcTaskOption::getInspectionRuleKey).toList());
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
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshot(30001L, 40001L)));
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(invalidTasks);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.listProcessesByActiveOrder(ACTIVE_ORDER_ID));

        assertEquals(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH.getCode(), error.getCode());
    }

    @Test
    void lockedQaProjectionRejectsTaskFromCurrentRouteNodeNotFrozenByOrder() {
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(ACTIVE_ORDER_ID, WORK_ORDER_ID,
                LocalDateTime.of(2026, 8, 12, 8, 0));
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder);
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder(WORK_ORDER_ID));
        when(routeMapper.selectByIdIgnoreDeleted(ROUTE_ID)).thenReturn(route());
        when(regulationService.getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, REGULATION_VERSION_ID))
                .thenReturn(lockedQaAggregate("PUBLISHED",
                        qaPublishedProcess(QA_PROCESS_ID, "ID-QA-001", "清洗", 1,
                                qaPublishedItem("ID-001", List.of("FIRST")))));
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshot(980645L, 922985L)));
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder().id(9105L).activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                        .routeProcessId(9908090160L).processId(922985L)
                        .regulationVersionId(REGULATION_VERSION_ID).qaProcessId(QA_PROCESS_ID)
                        .qaItemCode("ID-001").inspectionRuleKey("FIRST").inspectionType("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 12)).shiftCode("FIRST").roundNo(1)
                        .plannedInspectionQuantity(5).taskStatus("PENDING").build()));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.listProcessesByActiveOrder(ACTIVE_ORDER_ID));

        assertEquals(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH.getCode(), error.getCode());
    }

    private static MesProcessPoolActiveOrderDO activeOrder(long id, long workOrderId, LocalDateTime joinedAt) {
        return MesProcessPoolActiveOrderDO.builder().id(id).workOrderId(workOrderId).routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID).activeStatus("ACTIVE").businessStatus("ACTIVE")
                .dccProjectCodeId(DCC_PROJECT_ID).qaRegulationId(REGULATION_ID)
                .qaRegulationVersionId(REGULATION_VERSION_ID)
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
