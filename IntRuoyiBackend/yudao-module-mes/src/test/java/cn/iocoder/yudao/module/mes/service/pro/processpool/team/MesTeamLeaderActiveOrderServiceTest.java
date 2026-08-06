package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderServiceTest {

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesActiveOrderTransferTraceService transferTraceService;
    @Mock
    private MesQaInspectionRegulationMapper inspectionRegulationMapper;
    @Mock
    private MesQaInspectionRegulationVersionMapper inspectionRegulationVersionMapper;
    @Mock
    private MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;

    private MesTeamLeaderActiveOrderService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderServiceImpl(activeOrderMapper, workOrderService, workOrderMapper,
                auditMapper, scheduleOrderMapper, scheduleOrderProcessMapper, processSnapshotMapper,
                transferTraceService, inspectionRegulationMapper, inspectionRegulationVersionMapper,
                inspectionRegulationItemMapper, pqcInspectionTaskMapper);
        lenient().when(inspectionRegulationMapper.selectPublishedByRouteProcess(any(), any(), any(), any(), any()))
                .thenReturn(publishedRegulation(9902L));
        lenient().when(inspectionRegulationVersionMapper.selectById(9902L))
                .thenReturn(publishedRegulationVersion(true, null));
        lenient().when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(defaultPqcItems());
        lenient().when(pqcInspectionTaskMapper.selectByIdentity(any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        lenient().when(pqcInspectionTaskMapper.insert(any(MesPqcInspectionTaskDO.class))).thenReturn(1);
    }

    @Test
    void shouldSearchConfirmedWorkOrderCandidatesByCode() {
        when(workOrderMapper.selectConfirmedCandidatesByCode("WO-9", 20))
                .thenReturn(List.of(confirmedWorkOrder()));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertEquals(9001L, candidates.get(0).getWorkOrderId());
        assertEquals("WO-9001", candidates.get(0).getWorkOrderCode());
        verify(workOrderMapper).selectConfirmedCandidatesByCode("WO-9", 20);
    }

    @Test
    void shouldAddWorkOrderToLeaderActivePoolWithServerResolvedRoute() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L));
        stubSuccessfulInsertAndProcesses(List.of(
                scheduleProcess(928609L, 6001L, "3.000000", "600.000000"),
                scheduleProcess(928610L, 6002L, "2.000000", "400.000000")));

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .build());

        assertEquals(8101L, activeOrderId);
        verify(workOrderService).validateWorkOrderConfirmed(9001L);
        verify(scheduleOrderMapper).selectEffectiveListByWorkOrderIds(List.of(9001L));
        ArgumentCaptor<MesProcessPoolActiveOrderDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderDO.class);
        verify(activeOrderMapper).insert(captor.capture());
        MesProcessPoolActiveOrderDO activeOrder = captor.getValue();
        assertEquals(3001L, activeOrder.getLeaderUserId());
        assertEquals(9001L, activeOrder.getWorkOrderId());
        assertEquals(922119L, activeOrder.getRouteId());
        assertEquals(448L, activeOrder.getRouteVersionId());
        assertEquals(new BigDecimal("200"), activeOrder.getErpFixedQuantitySnapshot());
        assertEquals("ACTIVE", activeOrder.getActiveStatus());
        assertEquals("ACTIVE", activeOrder.getBusinessStatus());
        assertEquals(0, activeOrder.getVersion());
        assertNotNull(activeOrder.getJoinedAt());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesProcessPoolActiveOrderProcessSnapshotDO>> snapshotCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(processSnapshotMapper).insertBatch(snapshotCaptor.capture());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = List.copyOf(snapshotCaptor.getValue());
        assertEquals(2, snapshots.size());
        assertSnapshot(snapshots.get(0), 8101L, 9001L, 922119L, 448L, 928609L, 6001L,
                "200", "3.000000", "600.000000");
        assertSnapshot(snapshots.get(1), 8101L, 9001L, 922119L, 448L, 928610L, 6002L,
                "200", "2.000000", "400.000000");
        verify(transferTraceService, never()).recordTransferTracesForActiveOrder(any(), any());
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldReturnExistingActiveOrderWhenSameWorkOrderRouteVersionAlreadyActive() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(existingActiveOrder(8101L, "ACTIVE", 0));

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .build());

        assertEquals(8101L, activeOrderId);
        verify(activeOrderMapper).selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L);
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(transferTraceService, never()).recordTransferTracesForActiveOrder(any(), any());
    }

    @Test
    void shouldReturnExistingActiveOrderWhenConcurrentInsertHitsUniqueKey() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(null, existingActiveOrder(8102L, "ACTIVE", 0));
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class)))
                .thenThrow(new DuplicateKeyException("uk_mes_pp_active_order"));

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .build());

        assertEquals(8102L, activeOrderId);
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(transferTraceService, never()).recordTransferTracesForActiveOrder(any(), any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldReactivateRemovedActiveOrderWhenSameWorkOrderRouteVersionIsJoinedAgain() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L));
        when(activeOrderMapper.selectRemovedByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(existingActiveOrder(8101L, "REMOVED", 7));
        when(activeOrderMapper.reactivateRemovedActiveOrder(any(), any(), any(), any())).thenReturn(1);

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .build());

        assertEquals(8101L, activeOrderId);
        verify(activeOrderMapper).reactivateRemovedActiveOrder(eq(8101L), eq(3001L), eq(7), any(LocalDateTime.class));
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(transferTraceService, never()).recordTransferTracesForActiveOrder(any(), any());
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldRejectUnconfirmedWorkOrderBeforeAddingActiveOrder() {
        when(workOrderService.validateWorkOrderConfirmed(9001L))
                .thenThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil
                        .exception(ErrorCodeConstants.PRO_WORK_ORDER_NOT_CONFIRMED));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_WORK_ORDER_NOT_CONFIRMED.getCode(), ex.getCode());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectWhenEffectiveScheduleMissing() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules();

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_EFFECTIVE_SCHEDULE_UNIQUE_REQUIRED.getCode(),
                ex.getCode());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectWhenMultipleEffectiveSchedulesExist() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L),
                effectiveSchedule(7702L, 922119L, 448L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_EFFECTIVE_SCHEDULE_UNIQUE_REQUIRED.getCode(),
                ex.getCode());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectWhenEffectiveScheduleRouteMissing() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, null, 448L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED.getCode(), ex.getCode());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldGenerateFormalPqcTasksFromPublishedRegulationWhenAddingActiveOrder() {
        stubConfirmedWorkOrder(new BigDecimal("301"));
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L));
        stubSuccessfulInsertAndProcesses(List.of(scheduleProcess(928609L, 6001L, "1.000000", "301.000000")));

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .build());

        assertEquals(8101L, activeOrderId);
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor = ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        List<MesPqcInspectionTaskDO> tasks = taskCaptor.getAllValues();
        assertPqcTask(tasks.get(0), "FIRST", "FIRST", 5);
        assertPqcTask(tasks.get(1), "PATROL", "AM", 16);
        assertPqcTask(tasks.get(2), "PATROL", "PM", 16);
        assertPqcTask(tasks.get(3), "FINAL", "FINAL", 3);
    }

    @Test
    void shouldListActiveOrdersWithSingleActiveOrderQueryForDailyClosePerformance() {
        List<MesProcessPoolActiveOrderDO> expected = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(expected);

        List<MesProcessPoolActiveOrderDO> activeOrders = service.listActiveOrders(3001L);

        assertEquals(expected, activeOrders);
        verify(activeOrderMapper).selectActiveListByLeader(3001L);
        verify(activeOrderMapper, never()).selectActiveList();
        verify(scheduleOrderProcessMapper, never()).selectListByScheduleOrderId(any());
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
    }

    private void stubConfirmedWorkOrder() {
        stubConfirmedWorkOrder(new BigDecimal("200"));
    }

    private void stubConfirmedWorkOrder(BigDecimal quantity) {
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(confirmedWorkOrder(quantity));
    }

    private static MesProWorkOrderDO confirmedWorkOrder() {
        return confirmedWorkOrder(new BigDecimal("200"));
    }

    private static MesProWorkOrderDO confirmedWorkOrder(BigDecimal quantity) {
        return MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(quantity)
                .build();
    }

    private void stubEffectiveSchedules(MesProScheduleOrderDO... schedules) {
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(9001L))).thenReturn(List.of(schedules));
    }

    private void stubSuccessfulInsertAndProcesses(List<MesProScheduleOrderProcessDO> processes) {
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(processes);
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);
    }

    private static MesProScheduleOrderDO effectiveSchedule(Long id, Long routeId, Long routeVersionId) {
        return MesProScheduleOrderDO.builder()
                .id(id)
                .workOrderId(9001L)
                .productId(1001L)
                .routeId(routeId)
                .routeVersionId(routeVersionId)
                .build();
    }

    private static MesProcessPoolActiveOrderDO existingActiveOrder(Long id, String status, Integer version) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(id)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus(status)
                .businessStatus(status)
                .version(version)
                .removedAt("REMOVED".equals(status) ? LocalDateTime.of(2026, 8, 4, 10, 30) : null)
                .build();
    }

    private void verifyNoActiveOrderWrites() {
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(activeOrderMapper, never()).reactivateRemovedActiveOrder(any(), any(), any(), any());
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(pqcInspectionTaskMapper, never()).insert(any(MesPqcInspectionTaskDO.class));
        verify(transferTraceService, never()).recordTransferTracesForActiveOrder(any(), any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(Long routeProcessId, Long processId, String factor,
                                                                String plannedQuantity) {
        return MesProScheduleOrderProcessDO.builder()
                .routeProcessId(routeProcessId)
                .processId(processId)
                .enabled(Boolean.TRUE)
                .planDate(LocalDate.of(2026, 8, 5))
                .productionQuantityFactor(new BigDecimal(factor))
                .plannedQuantity(new BigDecimal(plannedQuantity))
                .build();
    }

    private static MesQaInspectionRegulationDO publishedRegulation(Long versionId) {
        return MesQaInspectionRegulationDO.builder()
                .id(9901L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(928609L)
                .processId(6001L)
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(versionId)
                .build();
    }

    private static MesQaInspectionRegulationVersionDO publishedRegulationVersion(
            Boolean finalInspectionApplicable, String reason) {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(9902L)
                .regulationId(9901L)
                .versionNo("V21-QA-1")
                .lifecycleStatus("PUBLISHED")
                .finalInspectionApplicable(finalInspectionApplicable)
                .finalInspectionNotApplicableReason(reason)
                .snapshotJson("{}")
                .build();
    }

    private static List<MesQaInspectionRegulationItemDO> defaultPqcItems() {
        return List.of(
                pqcItem("FIRST", 5, null),
                pqcItem("PATROL", null, new BigDecimal("0.050000")),
                pqcItem("FINAL", 3, null));
    }

    private static MesQaInspectionRegulationItemDO pqcItem(String inspectionType, Integer fixedQuantity,
                                                           BigDecimal patrolRatio) {
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(9902L)
                .inspectionType(inspectionType)
                .itemCode(inspectionType + "-001")
                .itemName(inspectionType + " 检验项目")
                .inspectionMethod("目视")
                .standardText("符合规程")
                .resultType("BOOLEAN")
                .firstInspectionQuantity(fixedQuantity)
                .patrolInspectionRatio(patrolRatio)
                .build();
    }

    private static void assertPqcTask(MesPqcInspectionTaskDO task, String inspectionType, String shiftCode,
                                      Integer plannedInspectionQuantity) {
        assertEquals(8101L, task.getActiveOrderId());
        assertEquals(9001L, task.getWorkOrderId());
        assertEquals(922119L, task.getRouteId());
        assertEquals(448L, task.getRouteVersionId());
        assertEquals(928609L, task.getRouteProcessId());
        assertEquals(6001L, task.getProcessId());
        assertEquals(9902L, task.getRegulationVersionId());
        assertEquals(inspectionType, task.getInspectionType());
        assertEquals(LocalDate.of(2026, 8, 5), task.getBusinessDate());
        assertEquals(shiftCode, task.getShiftCode());
        assertEquals(1, task.getRoundNo());
        assertEquals(plannedInspectionQuantity, task.getPlannedInspectionQuantity());
        assertEquals(0, task.getActualInspectionQuantity());
        assertEquals("PENDING", task.getTaskStatus());
    }

    private static void assertSnapshot(MesProcessPoolActiveOrderProcessSnapshotDO snapshot, Long activeOrderId,
                                       Long workOrderId, Long routeId, Long routeVersionId, Long routeProcessId,
                                       Long processId, String erpQuantity, String factor, String plannedQuantity) {
        assertEquals(activeOrderId, snapshot.getActiveOrderId());
        assertEquals(workOrderId, snapshot.getWorkOrderId());
        assertEquals(routeId, snapshot.getRouteId());
        assertEquals(routeVersionId, snapshot.getRouteVersionId());
        assertEquals(routeProcessId, snapshot.getRouteProcessId());
        assertEquals(processId, snapshot.getProcessId());
        assertAmount(erpQuantity, snapshot.getErpFixedQuantitySnapshot());
        assertAmount(factor, snapshot.getProductionQuantityFactorSnapshot());
        assertAmount(plannedQuantity, snapshot.getPlannedQuantitySnapshot());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
