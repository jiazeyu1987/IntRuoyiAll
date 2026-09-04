package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFeedbackMaterialBatchQueryService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFeedbackMaterialCreateCommand;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFeedbackMaterialService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderSimulationServiceTest {

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper reportAllocationMapper;
    @Mock
    private MesProcessPoolSubmissionReviewMapper submissionReviewMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Mock
    private MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper;
    @Mock
    private MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesProFeedbackMaterialBatchQueryService materialBatchQueryService;
    @Mock
    private MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    @Mock
    private MesProcessPoolTeamDeviceMapper deviceMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProFeedbackMaterialService feedbackMaterialService;
    @Mock
    private MesProcessPoolEventService processPoolEventService;
    @Mock
    private MesReportAllocationCommandService reportAllocationCommandService;
    @Mock
    private MesPqcProcessInspectionAggregationService pqcProcessInspectionAggregationService;
    @Mock
    private MesTeamLeaderOrderProcessCompletionService orderProcessCompletionService;

    private MesTeamLeaderActiveOrderSimulationService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderSimulationService(activeOrderMapper, processSnapshotMapper,
                routeVersionMapper, reportAllocationMapper, submissionReviewMapper, pqcInspectionTaskMapper,
                inspectionRegulationItemMapper, pqcPieceDetailMapper, feedbackMapper, itemMapper,
                materialBatchQueryService, processDeviceMapper, deviceMapper, routeProcessMapper,
                feedbackMaterialService, processPoolEventService,
                reportAllocationCommandService, pqcProcessInspectionAggregationService,
                orderProcessCompletionService);
    }

    @Test
    void simulateCompletionShouldUseFixedPqcTasksInsteadOfAllProductionProcesses() {
        MesProcessPoolActiveOrderDO activeOrder = activeOrder();
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = List.of(
                processSnapshot(5001L, 6001L),
                processSnapshot(5002L, 6002L));
        MesPqcInspectionTaskDO pendingPqcTask = pqcTask(MesPqcInspectionTaskDO.TASK_STATUS_PENDING);
        MesPqcInspectionTaskDO confirmedPqcTask = pqcTask(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                .setActualInspectionQuantity(1)
                .setSubmittedEventId(8001L);
        List<MesProcessPoolReportAllocationDO> completedAllocations = List.of(
                allocation(7001L, 5001L, 6001L),
                allocation(7002L, 5002L, 6002L));
        AtomicLong reviewId = new AtomicLong(9000L);
        AtomicLong feedbackId = new AtomicLong(5000L);

        when(activeOrderMapper.selectByIdForUpdate(8101L)).thenReturn(activeOrder);
        when(routeVersionMapper.selectById(448L)).thenReturn(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .routeSnapshotJson("{\"routeId\":922119,\"configSnapshots\":{"
                        + "\"flowGraph\":{\"nodes\":[{\"routeProcessId\":5001,\"processId\":6001},"
                        + "{\"routeProcessId\":5002,\"processId\":6002}]},\"batchUseConfigs\":["
                        + "{\"routeProcessId\":5001,\"inputMaterialIds\":[1001],\"outputMaterialIds\":[1002]},"
                        + "{\"routeProcessId\":5002,\"inputMaterialIds\":[1002],\"outputMaterialIds\":[1003]}]}}")
                .build());
        when(itemMapper.selectById(1001L)).thenReturn(material(1001L, "INPUT-001", "输入物料"));
        when(itemMapper.selectById(1002L)).thenReturn(material(1002L, "OUTPUT-001", "中间产物"));
        when(itemMapper.selectById(1003L)).thenReturn(material(1003L, "OUTPUT-002", "输出物料"));
        when(materialBatchQueryService.listBatchCodes(9001L, "INPUT-001")).thenReturn(List.of("IN-LOT-001"));
        when(materialBatchQueryService.listBatchCodes(9001L, "OUTPUT-001")).thenReturn(List.of("IN-LOT-002"));
        when(processDeviceMapper.selectList(any())).thenReturn(List.of(
                processDevice(12L, 702L), processDevice(11L, 701L)));
        when(deviceMapper.selectById(701L)).thenReturn(device(701L, "DEVICE-001", "第一台设备"));
        when(routeProcessMapper.selectByIdIgnoreDeleted(5001L)).thenReturn(routeProcess(5001L, 6001L, 801L));
        when(routeProcessMapper.selectByIdIgnoreDeleted(5002L)).thenReturn(routeProcess(5002L, 6002L, 802L));
        when(processSnapshotMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(snapshots);
        when(pqcInspectionTaskMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of(pendingPqcTask));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(List.of(), completedAllocations, completedAllocations);
        when(feedbackMapper.insert(any(MesProFeedbackDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProFeedbackDO.class).setId(feedbackId.incrementAndGet());
            return 1;
        });
        when(processPoolEventService.createEvent(any())).thenReturn(7001L, 7002L);
        when(reportAllocationMapper.selectListByEventIdForUpdate(7001L))
                .thenReturn(List.of(allocation(7001L, 5001L, 6001L)));
        when(reportAllocationMapper.selectListByEventIdForUpdate(7002L))
                .thenReturn(List.of(allocation(7002L, 5002L, 6002L)));
        when(reportAllocationMapper.updateById(any(MesProcessPoolReportAllocationDO.class))).thenReturn(1);
        when(submissionReviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(reviewId.incrementAndGet());
            return 1;
        });
        when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(List.of(inspectionItem()));
        when(pqcPieceDetailMapper.selectListByTaskId(8301L)).thenReturn(List.of());
        when(pqcPieceDetailMapper.insertBatch(any(List.class))).thenReturn(Boolean.TRUE);
        when(pqcInspectionTaskMapper.updateSubmittedIfPending(8301L, 1, "SIMULATED:8301:1",
                MesPqcInspectionTaskDO.TASK_STATUS_PENDING, MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED))
                .thenReturn(1);
        when(processPoolEventService.createPqcInspectionEvent(any())).thenReturn(8001L);
        when(pqcInspectionTaskMapper.updateSubmittedEventId(8301L, 8001L)).thenReturn(1);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(8101L))
                .thenReturn(List.of(confirmedPqcTask), List.of(confirmedPqcTask));

        MesTeamLeaderActiveOrderSimulationResult result = service.simulateActiveOrderCompletion(3001L, 8101L);

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(2, result.getProductionSubmitCount());
        assertEquals(2, result.getProductionReviewCount());
        assertEquals(1, result.getPqcSubmitCount());
        assertEquals(1, result.getPqcReviewCount());
        assertEquals(new BigDecimal("100.000000"), result.getProductionProgressPercent());
        assertEquals(new BigDecimal("100.000000"), result.getInspectionProgressPercent());
        org.mockito.Mockito.verify(reportAllocationCommandService).createInitialAllocation(
                7001L, 8101L, new BigDecimal("200.000000"));
        org.mockito.Mockito.verify(reportAllocationCommandService).createInitialAllocation(
                7002L, 8101L, new BigDecimal("200.000000"));
        org.mockito.Mockito.verify(orderProcessCompletionService, org.mockito.Mockito.times(2))
                .reconcileAffectedAllocations(any(MesProProcessPoolEventDO.class), any(Collection.class));
        ArgumentCaptor<MesProcessPoolCreateEventReqDTO> productionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolCreateEventReqDTO.class);
        org.mockito.Mockito.verify(processPoolEventService, org.mockito.Mockito.times(2))
                .createEvent(productionCaptor.capture());
        assertEquals(List.of("MES_PRO_FEEDBACK", "MES_PRO_FEEDBACK"), productionCaptor.getAllValues().stream()
                .map(MesProcessPoolCreateEventReqDTO::getFeedbackSourceType).toList());
        assertEquals(List.of(5001L, 5002L), productionCaptor.getAllValues().stream()
                .map(MesProcessPoolCreateEventReqDTO::getFeedbackSourceId).toList());
        assertTrue(productionCaptor.getAllValues().stream()
                .allMatch(req -> req.getRawPayload().contains("\"lossDetails\":[]")));
        assertTrue(productionCaptor.getAllValues().stream()
                .allMatch(req -> req.getRawPayload().contains("\"inputMaterialDetails\"")));
        assertTrue(productionCaptor.getAllValues().stream()
                .allMatch(req -> req.getRawPayload().contains("\"direction\":\"INPUT\"")));
        assertTrue(productionCaptor.getAllValues().stream()
                .allMatch(req -> req.getRawPayload().contains("\"direction\":\"OUTPUT\"")));
        assertTrue(productionCaptor.getAllValues().stream()
                .allMatch(req -> req.getRawPayload().contains("\"outputQuantity\":200.000000")));
        assertTrue(productionCaptor.getAllValues().stream()
                .anyMatch(req -> req.getRawPayload().contains("IN-LOT-001")));
        assertEquals(List.of(701L, 701L), productionCaptor.getAllValues().stream()
                .map(MesProcessPoolCreateEventReqDTO::getDeviceId).toList());
        assertEquals(List.of(801L, 802L), productionCaptor.getAllValues().stream()
                .map(MesProcessPoolCreateEventReqDTO::getWorkstationId).toList());
        assertTrue(productionCaptor.getAllValues().stream()
                .allMatch(req -> req.getRawPayload().contains("\"deviceCode\":\"DEVICE-001\"")));
        ArgumentCaptor<MesProFeedbackMaterialCreateCommand> materialCaptor =
                ArgumentCaptor.forClass(MesProFeedbackMaterialCreateCommand.class);
        org.mockito.Mockito.verify(feedbackMaterialService, org.mockito.Mockito.times(2))
                .createMaterials(materialCaptor.capture());
        assertEquals(List.of(1002L, 1003L), materialCaptor.getAllValues().stream()
                .map(command -> command.entries().get(0).materialId()).toList());

        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        org.mockito.Mockito.verify(submissionReviewMapper, org.mockito.Mockito.times(3))
                .insert(reviewCaptor.capture());
        Map<Long, MesProcessPoolSubmissionReviewDO> productionReviewByEvent = reviewCaptor.getAllValues().stream()
                .filter(review -> MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION.equals(review.getLeaderType()))
                .collect(java.util.stream.Collectors.toMap(
                        MesProcessPoolSubmissionReviewDO::getEventId,
                        review -> review));
        ArgumentCaptor<MesProcessPoolReportAllocationDO> allocationUpdateCaptor =
                ArgumentCaptor.forClass(MesProcessPoolReportAllocationDO.class);
        org.mockito.Mockito.verify(reportAllocationMapper, org.mockito.Mockito.atLeast(2))
                .updateById(allocationUpdateCaptor.capture());
        List<MesProcessPoolReportAllocationDO> linkedAllocationUpdates =
                allocationUpdateCaptor.getAllValues().stream()
                        .filter(allocation -> allocation.getReviewId() != null)
                        .toList();
        assertEquals(2, linkedAllocationUpdates.size());
        assertTrue(linkedAllocationUpdates.stream().allMatch(allocation ->
                allocation.getConfirmedAt() != null
                        && allocation.getConfirmedAt().equals(
                        productionReviewByEvent.get(allocation.getEventId()).getReviewedAt())));
    }

    @Test
    void placeholderInputMaterialCodeSkipsFormalPickListBatchLookup() {
        MesProcessPoolActiveOrderDO activeOrder = activeOrder();
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .routeSnapshotJson("{\"routeId\":922119,\"configSnapshots\":{"
                        + "\"flowGraph\":{\"nodes\":[{\"routeProcessId\":5001,\"processId\":6001}]},"
                        + "\"batchUseConfigs\":[{\"routeProcessId\":5001,\"inputMaterialIds\":[1001],"
                        + "\"outputMaterialIds\":[]}]}}")
                .build();
        when(itemMapper.selectById(1001L)).thenReturn(material(1001L, "/", "占位物料"));

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,
                "buildSimulationMaterialPayload", activeOrder, routeVersion,
                processSnapshot(5001L, 6001L), BigDecimal.ONE, null);

        assertTrue(String.valueOf(payload.get("inputMaterialDetails")).contains("placeholderMaterial=true"));
        verify(materialBatchQueryService, never()).listBatchCodes(any(), any());
    }

    private static MesProcessPoolActiveOrderDO activeOrder() {
        MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .erpFixedQuantitySnapshot(new BigDecimal("200.000000"))
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .version(1)
                .build();
        activeOrder.setTenantId(1L);
        return activeOrder;
    }

    private static MesMdItemDO material(Long id, String code, String name) {
        return MesMdItemDO.builder().id(id).code(code).name(name).specification("规格").build();
    }

    private static MesProcessPoolTeamProcessDeviceDO processDevice(Long id, Long deviceId) {
        return MesProcessPoolTeamProcessDeviceDO.builder().id(id).leaderUserId(3001L)
                .processId(6001L).deviceId(deviceId).enabled(Boolean.TRUE).build();
    }

    private static MesProcessPoolTeamDeviceDO device(Long id, String code, String name) {
        return MesProcessPoolTeamDeviceDO.builder().id(id).leaderUserId(3001L).deviceCode(code)
                .deviceName(name).deviceStatus("ENABLED").enabled(Boolean.TRUE).build();
    }

    private static MesProRouteProcessDO routeProcess(Long id, Long processId, Long workstationId) {
        return MesProRouteProcessDO.builder().id(id).routeId(922119L).processId(processId)
                .workstationId(workstationId).build();
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot(Long routeProcessId, Long processId) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .plannedQuantitySnapshot(new BigDecimal("200.000000"))
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long eventId, Long routeProcessId, Long processId) {
        return MesProcessPoolReportAllocationDO.builder()
                .id(eventId + 100L)
                .eventId(eventId)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .allocatedQuantity(new BigDecimal("200.000000"))
                .lifecycleStatus(MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .build();
    }

    private static MesPqcInspectionTaskDO pqcTask(String status) {
        MesPqcInspectionTaskDO task = MesPqcInspectionTaskDO.builder()
                .id(8301L)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(5001L)
                .processId(6001L)
                .qaProcessId(19902L)
                .qaItemCode("FIRST-001")
                .regulationVersionId(9902L)
                .inspectionType("FIRST")
                .inspectionRuleKey("FIRST")
                .businessDate(LocalDate.of(2026, 8, 20))
                .shiftCode("FIRST")
                .roundNo(1)
                .plannedInspectionQuantity(1)
                .actualInspectionQuantity(0)
                .taskStatus(status)
                .build();
        task.setTenantId(1L);
        return task;
    }

    private static MesQaInspectionRegulationItemDO inspectionItem() {
        return MesQaInspectionRegulationItemDO.builder()
                .id(990201L)
                .regulationVersionId(9902L)
                .qaProcessId(19902L)
                .itemSort(1)
                .inspectionType("FIRST")
                .itemCode("FIRST-001")
                .itemName("Appearance")
                .inspectionMethod("Visual")
                .standardText("Pass")
                .resultType("TEXT")
                .standardLowerLimit(BigDecimal.ZERO)
                .standardUpperLimit(BigDecimal.ONE)
                .standardPrecision(1)
                .build();
    }
}
