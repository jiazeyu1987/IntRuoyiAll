package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_PROCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlinePqcContextServiceTest {

    private static final Long LOGIN_USER_ID = 9001L;
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
    private static final String PQC_SIGNATURE_SNAPSHOT = "{\"signature\":\"pqc-submit\"}";

    @Mock
    private MesProProcessPoolMapper processPoolMapper;
    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
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
    private MesQaInspectionRegulationItemMapper regulationItemMapper;
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

    private MesFrontlinePqcContextService service;

    @BeforeEach
    void setUp() {
        service = new MesFrontlinePqcContextServiceImpl(activeOrderMapper, workOrderMapper, routeMapper,
                routeProductMapper, routeProcessMapper, regulationMapper, regulationItemMapper, pqcTaskMapper,
                pqcPieceDetailMapper, processService, itemService, scopeMapper, adminUserApi, templateResolver,
                processPoolEventService);
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
        assertEquals(ROUTE_ID, orders.get(0).routeId());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), orders.get(0).latestSubmitTime());
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
    void shouldListAllPqcEmployeesAndPqcLeaders() {
        when(scopeMapper.selectActiveScopesByLeaderType(LEADER_TYPE_PQC)).thenReturn(List.of(
                scope(7001L, SCOPE_TYPE_EMPLOYEE, 8001L),
                scope(7002L, SCOPE_TYPE_PROCESS, null),
                scope(7001L, SCOPE_TYPE_EMPLOYEE, 8002L)));
        when(adminUserApi.getUserList(Set.of(7001L, 7002L, 8001L, 8002L))).thenReturn(List.of(
                enabledUser(7001L, "pqc-leader-a", "PQC组长A"),
                enabledUser(7002L, "pqc-leader-b", "PQC组长B"),
                enabledUser(8001L, "pqc-employee-a", "PQC员工A"),
                enabledUser(8002L, "pqc-employee-b", "PQC员工B")));

        List<MesFrontlineEmployeeCandidate> employees = service.listPqcEmployeeCandidates();

        assertEquals(Set.of(7001L, 7002L, 8001L, 8002L),
                employees.stream().map(MesFrontlineEmployeeCandidate::userId).collect(java.util.stream.Collectors.toSet()));
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
                routeProcess(ROUTE_PROCESS_ID, ROUTE_ID, PROCESS_ID, 10)));
        when(processService.getProcessMap(Set.of(PROCESS_ID))).thenReturn(Map.of(
                PROCESS_ID, process(PROCESS_ID, "P-1", "首工序")));
        givenPqcTaskContext(ROUTE_PROCESS_ID, PROCESS_ID, PQC_TASK_ID, REGULATION_VERSION_ID);
        when(scopeMapper.selectActiveScopesByLeaderType(LEADER_TYPE_PQC)).thenReturn(List.of(
                scope(7001L, SCOPE_TYPE_EMPLOYEE, 8001L)));
        when(adminUserApi.getUserList(Set.of(7001L, 8001L))).thenReturn(List.of(
                enabledUser(7001L, "pqc-leader-a", "PQC组长A"),
                enabledUser(8001L, "pqc-employee-a", "PQC员工A")));
        when(templateResolver.resolve(any(MesFrontlineTemplateRequest.class))).thenReturn(
                new MesFrontlineTemplateDescriptor("PQC_SIMPLIFIED", "PQC", ROUTE_PROCESS_ID, PROCESS_ID, 8001L));

        MesFrontlineEmployeeSwitchResult result = service.switchPqcActualEmployee(LOGIN_USER_ID, WORK_ORDER_ID,
                ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID, 8001L);

        assertEquals(LOGIN_USER_ID, result.loginUserId());
        assertEquals(8001L, result.actualEmployeeId());
        assertEquals("PQC_SIMPLIFIED", result.template().templateNo());
    }

    @Test
    void shouldSubmitPqcInspectionFromQaRegulationTaskSource() {
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
        when(pqcTaskMapper.selectById(PQC_TASK_ID)).thenReturn(pqcTask(PQC_TASK_ID, ROUTE_PROCESS_ID,
                PROCESS_ID, REGULATION_VERSION_ID));

        Long taskId = service.submitPqcInspection(LOGIN_USER_ID, MesFrontlinePqcSubmitCommand.builder()
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
                .signatureId(8801L)
                .signatureEmployeeId(8001L)
                .signatureSnapshot(PQC_SIGNATURE_SNAPSHOT)
                .templateType("PQC_SIMPLIFIED")
                .inspectionResult("DETECTION_SUCCESS")
                .rawPayload(Map.of(
                        "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 30),
                        "pqcPieceValues", Map.of(
                                "pressure", List.of("0.8", "0.9"),
                                "appearance", List.of("合格", "合格"))))
                .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build());

        assertEquals(PQC_TASK_ID, taskId);
        ArgumentCaptor<MesProcessPoolCreatePqcInspectionReqDTO> eventCaptor =
                ArgumentCaptor.forClass(MesProcessPoolCreatePqcInspectionReqDTO.class);
        verify(processPoolEventService).createPqcInspectionEvent(eventCaptor.capture());
        MesProcessPoolCreatePqcInspectionReqDTO eventReq = eventCaptor.getValue();
        assertEquals(WORK_ORDER_ID, eventReq.getWorkOrderId());
        assertEquals(PRODUCTION_SUBMIT_EVENT_ID, eventReq.getProductionSubmitEventId());
        assertEquals(ROUTE_ID, eventReq.getRouteId());
        assertEquals(ROUTE_PROCESS_ID, eventReq.getRouteProcessId());
        assertEquals(PROCESS_ID, eventReq.getProcessId());
        assertEquals(8001L, eventReq.getActualEmployeeId());
        assertEquals(DEVICE_ACCOUNT_ID, eventReq.getDeviceAccountId());
        assertEquals(DEVICE_ID, eventReq.getDeviceId());
        assertEquals(WORKSTATION_ID, eventReq.getWorkstationId());
        assertEquals("PQC_SIMPLIFIED", eventReq.getTemplateType());
        assertEquals(PQC_SOURCE_TYPE, eventReq.getFeedbackSourceType());
        assertEquals(PQC_TASK_ID, eventReq.getFeedbackSourceId());
        assertEquals(PQC_SOURCE_TYPE, eventReq.getRecordbookSourceType());
        assertEquals(PQC_TASK_ID, eventReq.getRecordbookSourceId());
        assertEquals(PQC_IDEMPOTENCY_KEY, eventReq.getPqcSubmissionIdempotencyKey());
        assertEquals("SUCCESS", eventReq.getInspectionResult());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), eventReq.getClientSubmitTime());
        assertEquals(8801L, eventReq.getSignatureId());
        assertEquals(8001L, eventReq.getSignatureUserId());
        assertEquals(PQC_SIGNATURE_SNAPSHOT, eventReq.getSignatureSnapshot());
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor = ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcTaskMapper).updateById(taskCaptor.capture());
        assertEquals("SUBMITTED", taskCaptor.getValue().getTaskStatus());
        assertEquals(2, taskCaptor.getValue().getActualInspectionQuantity());

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
                .signatureId(8801L)
                .signatureEmployeeId(8001L)
                .signatureSnapshot(PQC_SIGNATURE_SNAPSHOT)
                .templateType("PQC_SIMPLIFIED")
                .inspectionResult("DETECTION_SUCCESS")
                .rawPayload(Map.of(
                        "pqcDraft", Map.of("inspectionType", "PATROL", "inspectionQuantity", 30),
                        "pqcPieceValues", Map.of(
                                "pressure", List.of("0.8", "0.9"),
                                "appearance", List.of("合格", "合格"))))
                .clientSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build();
        when(processPoolEventService.findExistingPqcInspectionTaskId(any(MesProcessPoolCreatePqcInspectionReqDTO.class)))
                .thenReturn(Optional.of(PQC_TASK_ID));

        Long taskId = service.submitPqcInspection(LOGIN_USER_ID, command);

        assertEquals(PQC_TASK_ID, taskId);
        verify(activeOrderMapper, never()).selectActiveByWorkOrderAndRoute(any(), any());
        verify(pqcTaskMapper, never()).updateById(any(MesPqcInspectionTaskDO.class));
        verify(pqcPieceDetailMapper, never()).insertBatch(anyCollection());
        verify(processPoolEventService, never()).createPqcInspectionEvent(any());
    }

    private void givenWorkOrderProductAndRoute() {
        when(workOrderMapper.selectListByIds(Set.of(WORK_ORDER_ID))).thenReturn(List.of(workOrder(WORK_ORDER_ID, PRODUCT_ID)));
        when(itemService.getItemMap(Set.of(PRODUCT_ID))).thenReturn(Map.of(PRODUCT_ID,
                MesMdItemDO.builder().id(PRODUCT_ID).code("ITEM-PQC").name("PQC 产品").build()));
        when(routeMapper.selectListByIdsIgnoreDeleted(Set.of(ROUTE_ID))).thenReturn(List.of(route(ROUTE_ID)));
        when(routeProductMapper.selectByRouteIdAndItemId(ROUTE_ID, PRODUCT_ID))
                .thenReturn(MesProRouteProductDO.builder().routeId(ROUTE_ID).itemId(PRODUCT_ID).build());
    }

    private void givenPqcTaskContext(Long routeProcessId, Long processId, Long taskId, Long regulationVersionId) {
        when(regulationMapper.selectPublishedByRouteProcess(PRODUCT_ID, ROUTE_ID, 448L, routeProcessId, processId))
                .thenReturn(regulation(regulationVersionId));
        when(pqcTaskMapper.selectPendingByActiveOrderProcess(ACTIVE_ORDER_ID, routeProcessId, processId))
                .thenReturn(pqcTask(taskId, routeProcessId, processId, regulationVersionId));
        when(regulationItemMapper.selectListByVersionId(regulationVersionId)).thenReturn(List.of(
                regulationItem(regulationVersionId, "pressure", "压力", "NUMBER", "测压"),
                regulationItem(regulationVersionId, "appearance", "外观", "CHOICE", "目视")));
    }

    private static MesProProcessPoolDO activePool(Long workOrderId, Long routeId, Long routeProcessId, Long processId,
                                                  LocalDateTime latestSubmitTime) {
        return MesProProcessPoolDO.builder()
                .workOrderId(workOrderId)
                .routeId(routeId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .poolStatus(MesProProcessPoolDO.STATUS_ACTIVE)
                .latestSubmitTime(latestSubmitTime)
                .build();
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
        return MesQaInspectionRegulationDO.builder()
                .id(8101L)
                .productId(PRODUCT_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(448L)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(regulationVersionId)
                .build();
    }

    private static MesPqcInspectionTaskDO pqcTask(Long taskId, Long routeProcessId, Long processId,
                                                  Long regulationVersionId) {
        return MesPqcInspectionTaskDO.builder()
                .id(taskId)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(448L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .regulationVersionId(regulationVersionId)
                .inspectionType("PATROL")
                .businessDate(LocalDate.of(2026, 8, 1))
                .shiftCode("DAY")
                .roundNo(1)
                .plannedInspectionQuantity(2)
                .taskStatus("PENDING")
                .build();
    }

    private static MesQaInspectionRegulationItemDO regulationItem(Long regulationVersionId, String itemCode,
                                                                  String itemName, String resultType,
                                                                  String inspectionMethod) {
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(regulationVersionId)
                .inspectionType("PATROL")
                .itemCode(itemCode)
                .itemName(itemName)
                .inspectionMethod(inspectionMethod)
                .standardText("按压力泵工艺路线过程检验记录标准")
                .resultType(resultType)
                .build();
    }

    private static MesProProcessPoolDO activePoolWithSourceEvent() {
        return MesProProcessPoolDO.builder()
                .id(9001L)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .deviceId(9301L)
                .workstationId(6001L)
                .poolStatus(MesProProcessPoolDO.STATUS_ACTIVE)
                .latestEventId(9101L)
                .latestSubmitTime(LocalDateTime.of(2026, 8, 1, 8, 0))
                .build();
    }

    private static MesProProcessPoolEventDO sourceEvent() {
        return MesProProcessPoolEventDO.builder()
                .id(9101L)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .deviceAccountId(9201L)
                .deviceId(9301L)
                .workstationId(6001L)
                .feedbackSourceType("MES_PRO_FEEDBACK")
                .feedbackSourceId(9401L)
                .recordbookSourceType("MES_PRO_EDHR_RECORD_BOOK_EVENT")
                .recordbookSourceId(9501L)
                .build();
    }

    private static MesProWorkOrderDO workOrder(Long id, Long productId) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code("WO-PQC-001")
                .name("PQC 活跃订单")
                .productId(productId)
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
