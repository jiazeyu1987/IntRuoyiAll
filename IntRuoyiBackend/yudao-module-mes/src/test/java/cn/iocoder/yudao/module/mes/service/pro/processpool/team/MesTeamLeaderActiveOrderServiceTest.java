package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
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
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
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
                auditMapper, scheduleOrderMapper, scheduleOrderProcessMapper, routeProductMapper, routeVersionMapper,
                processSnapshotMapper,
                inspectionRegulationMapper, inspectionRegulationVersionMapper,
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
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(List.of(7701L)))
                .thenReturn(List.of(scheduleProcess(928609L, 6001L, "1.000000", "200.000000")));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertEquals(9001L, candidates.get(0).getWorkOrderId());
        assertEquals("WO-9001", candidates.get(0).getWorkOrderCode());
        assertTrue(candidates.get(0).isEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verify(workOrderMapper).selectConfirmedCandidatesByCode("WO-9", 20);
    }

    @Test
    void shouldSearchConfirmedWorkOrderWithoutScheduleFromActiveRouteSnapshot() {
        when(workOrderMapper.selectConfirmedCandidatesByCode("WO-9", 20))
                .thenReturn(List.of(confirmedWorkOrderWithPlannedStart()));
        stubEffectiveSchedules();
        stubUnscheduledActiveRoute();
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verify(routeProductMapper).selectListByItemIds(List.of(1001L));
        verify(routeVersionMapper).selectListByRouteIds(List.of(922119L));
    }

    @Test
    void shouldMarkUnscheduledCandidateIneligibleWhenProductRouteBindingMissing() {
        when(workOrderMapper.selectConfirmedCandidatesByCode("WO-9", 20))
                .thenReturn(List.of(confirmedWorkOrderWithPlannedStart()));
        stubEffectiveSchedules();
        when(routeProductMapper.selectListByItemIds(List.of(1001L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertEquals("缺少产品正式工艺路线绑定", candidates.get(0).getIneligibleReason());
        verify(routeVersionMapper, never()).selectListByRouteIds(any());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldMarkUnscheduledCandidateIneligibleWhenActiveRouteVersionMissing() {
        when(workOrderMapper.selectConfirmedCandidatesByCode("WO-9", 20))
                .thenReturn(List.of(confirmedWorkOrderWithPlannedStart()));
        stubEffectiveSchedules();
        when(routeProductMapper.selectListByItemIds(List.of(1001L))).thenReturn(List.of(
                MesProRouteProductDO.builder().id(7001L).itemId(1001L).routeId(922119L).build()));
        when(routeVersionMapper.selectListByRouteIds(List.of(922119L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertFalse(candidates.get(0).isEligible());
        assertEquals("产品工艺路线缺少当前ACTIVE版本", candidates.get(0).getIneligibleReason());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldMarkUnscheduledCandidateIneligibleWhenActiveRouteSnapshotIncomplete() {
        when(workOrderMapper.selectConfirmedCandidatesByCode("WO-9", 20))
                .thenReturn(List.of(confirmedWorkOrderWithPlannedStart()));
        stubEffectiveSchedules();
        when(routeProductMapper.selectListByItemIds(List.of(1001L))).thenReturn(List.of(
                MesProRouteProductDO.builder().id(7001L).itemId(1001L).routeId(922119L).build()));
        when(routeVersionMapper.selectListByRouteIds(List.of(922119L))).thenReturn(List.of(
                activeRouteVersion("{}")));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertFalse(candidates.get(0).isEligible());
        assertEquals("产品工艺路线ACTIVE版本快照缺少配置快照", candidates.get(0).getIneligibleReason());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldSortEligibleActiveOrderCandidatesBeforeBlockedCandidates() {
        when(workOrderMapper.selectConfirmedCandidatesByCode("WO", 20)).thenReturn(List.of(
                confirmedWorkOrder(9002L, "WO-9002", new BigDecimal("200"), 1001L),
                confirmedWorkOrder(9001L, "WO-9001", new BigDecimal("200"), 1001L)));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(any())).thenReturn(List.of(
                effectiveSchedule(7702L, 9002L, 922119L, 448L),
                effectiveSchedule(7701L, 9001L, 922119L, 448L)));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(any())).thenReturn(List.of(
                scheduleProcess(7702L, 928610L, 6002L, "1.000000", "200.000000"),
                scheduleProcess(7701L, 928609L, 6001L, "1.000000", "200.000000")));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L, 928609L, 6001L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO");

        assertEquals(List.of(9001L, 9002L), candidates.stream()
                .map(MesTeamLeaderActiveOrderCandidateBO::getWorkOrderId)
                .toList());
        assertTrue(candidates.get(0).isEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        assertFalse(candidates.get(1).isEligible());
        assertEquals("缺少已发布QA规程", candidates.get(1).getIneligibleReason());
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(pqcInspectionTaskMapper, never()).insert(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void shouldBatchLoadCandidateDependenciesSoRemoteDropdownDoesNotStayLoading() {
        when(workOrderMapper.selectConfirmedCandidatesByCode("88", 20)).thenReturn(List.of(
                confirmedWorkOrder(9001L, "881MO093613", new BigDecimal("200"), 1001L),
                confirmedWorkOrder(9002L, "881MO093615", new BigDecimal("200"), 1001L)));
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(any())).thenReturn(List.of(
                effectiveSchedule(7701L, 9001L, 922119L, 448L),
                effectiveSchedule(7702L, 9002L, 922119L, 448L)));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderIds(any())).thenReturn(List.of(
                scheduleProcess(7701L, 928609L, 6001L, "1.000000", "200.000000"),
                scheduleProcess(7702L, 928610L, 6002L, "1.000000", "200.000000")));
        stubCandidatePqcPrerequisites(
                publishedRegulation(9902L, 928609L, 6001L),
                publishedRegulation(9903L, 928610L, 6002L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("88");

        assertEquals(List.of(9001L, 9002L), candidates.stream()
                .map(MesTeamLeaderActiveOrderCandidateBO::getWorkOrderId)
                .toList());
        assertTrue(candidates.stream().allMatch(MesTeamLeaderActiveOrderCandidateBO::isEligible));
        verify(scheduleOrderMapper).selectEffectiveListByWorkOrderIds(argThat(ids ->
                Set.copyOf(ids).equals(Set.of(9001L, 9002L))));
        verify(scheduleOrderProcessMapper).selectListByScheduleOrderIds(argThat(ids ->
                Set.copyOf(ids).equals(Set.of(7701L, 7702L))));
        verify(scheduleOrderProcessMapper, never()).selectListByScheduleOrderId(any());
        verify(inspectionRegulationMapper).selectListByProductIds(argThat(ids ->
                Set.copyOf(ids).equals(Set.of(1001L))));
        verify(inspectionRegulationMapper, never()).selectPublishedByRouteProcess(any(), any(), any(), any(), any());
        verify(inspectionRegulationVersionMapper).selectBatchIds(argThat(ids ->
                Set.copyOf(ids).equals(Set.of(9902L, 9903L))));
        verify(inspectionRegulationVersionMapper, never()).selectById(any());
        verify(inspectionRegulationItemMapper).selectListByVersionIds(argThat(ids ->
                Set.copyOf(ids).equals(Set.of(9902L, 9903L))));
        verify(inspectionRegulationItemMapper, never()).selectListByVersionId(any());
    }

    @Test
    void shouldAddWorkOrderToLeaderActivePoolWithServerResolvedRoute() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L));
        stubSuccessfulInsertAndProcesses(List.of(
                scheduleProcess(928609L, 6001L, "3.000000", "600.000000"),
                scheduleProcess(928610L, 6002L, "2.000000", "400.000000")));

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

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
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldAddWorkOrderWithoutScheduleFromActiveRouteSnapshot() {
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(confirmedWorkOrderWithPlannedStart());
        stubEffectiveSchedules();
        stubUnscheduledActiveRoute();
        stubSuccessfulActiveOrderInsert();

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, activeOrderId);
        ArgumentCaptor<MesProcessPoolActiveOrderDO> activeOrderCaptor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderDO.class);
        verify(activeOrderMapper).insert(activeOrderCaptor.capture());
        assertEquals(922119L, activeOrderCaptor.getValue().getRouteId());
        assertEquals(448L, activeOrderCaptor.getValue().getRouteVersionId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesProcessPoolActiveOrderProcessSnapshotDO>> snapshotCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(processSnapshotMapper).insertBatch(snapshotCaptor.capture());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = List.copyOf(snapshotCaptor.getValue());
        assertEquals(1, snapshots.size());
        assertSnapshot(snapshots.get(0), 8101L, 9001L, 922119L, 448L, 928609L, 6001L,
                "200", "1.000000", "200.000000");
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor = ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        assertTrue(taskCaptor.getAllValues().stream()
                .allMatch(task -> LocalDate.of(2026, 8, 5).equals(task.getBusinessDate())));
    }

    @Test
    void shouldRejectAddWithoutScheduleWhenProductRouteBindingMissing() {
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(confirmedWorkOrderWithPlannedStart());
        stubEffectiveSchedules();
        when(routeProductMapper.selectListByItemIds(List.of(1001L))).thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED.getCode(), ex.getCode());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectAddWithoutScheduleWhenErpPlannedStartMissing() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules();
        stubUnscheduledActiveRoute();

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("ERP计划开工时间缺失"));
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldReturnExistingActiveOrderWhenSameWorkOrderRouteVersionAlreadyActive() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(existingActiveOrder(8101L, "ACTIVE", 0));

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, activeOrderId);
        verify(activeOrderMapper).selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L);
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
    }

    @Test
    void shouldReturnExistingActiveOrderWhenConcurrentInsertHitsUniqueKey() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(null, existingActiveOrder(8102L, "ACTIVE", 0));
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class)))
                .thenThrow(new DuplicateKeyException("uk_mes_pp_active_order"));

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

        assertEquals(8102L, activeOrderId);
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldReactivateRemovedActiveOrderWhenSameWorkOrderRouteVersionIsJoinedAgain() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L));
        when(activeOrderMapper.selectRemovedByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(existingActiveOrder(8101L, "REMOVED", 7));
        when(activeOrderMapper.reactivateRemovedActiveOrder(any(), any(), any(), any())).thenReturn(1);

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, activeOrderId);
        verify(activeOrderMapper).reactivateRemovedActiveOrder(eq(8101L), eq(3001L), eq(7), any(LocalDateTime.class));
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldRejectUnconfirmedWorkOrderBeforeAddingActiveOrder() {
        when(workOrderService.validateWorkOrderConfirmed(9001L))
                .thenThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil
                        .exception(ErrorCodeConstants.PRO_WORK_ORDER_NOT_CONFIRMED));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_WORK_ORDER_NOT_CONFIRMED.getCode(), ex.getCode());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectWhenScheduleAndProductRouteAreMissing() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules();

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED.getCode(), ex.getCode());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectWhenMultipleEffectiveSchedulesExist() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L),
                effectiveSchedule(7702L, 922119L, 448L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_EFFECTIVE_SCHEDULE_UNIQUE_REQUIRED.getCode(),
                ex.getCode());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectWhenEffectiveScheduleRouteMissing() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, null, 448L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED.getCode(), ex.getCode());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectWhenEffectiveScheduleRouteVersionMissing() {
        stubConfirmedWorkOrder();
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, null));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED.getCode(), ex.getCode());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldGenerateFormalPqcTasksFromPublishedRegulationWhenAddingActiveOrder() {
        stubConfirmedWorkOrder(new BigDecimal("301"));
        stubEffectiveSchedules(effectiveSchedule(7701L, 922119L, 448L));
        stubSuccessfulInsertAndProcesses(List.of(scheduleProcess(928609L, 6001L, "1.000000", "301.000000")));

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

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

    private static MesTeamLeaderActiveOrderAddReqBO activeOrderReq() {
        return MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .build();
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
        return confirmedWorkOrder(9001L, "WO-9001", quantity, 1001L);
    }

    private static MesProWorkOrderDO confirmedWorkOrderWithPlannedStart() {
        return MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("200"))
                .plannedStartTime(LocalDateTime.of(2026, 8, 5, 8, 0))
                .build();
    }

    private static MesProWorkOrderDO confirmedWorkOrder(Long id, String code, BigDecimal quantity, Long productId) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code(code)
                .productId(productId)
                .quantity(quantity)
                .build();
    }

    private void stubEffectiveSchedules(MesProScheduleOrderDO... schedules) {
        stubEffectiveSchedulesFor(9001L, schedules);
    }

    private void stubEffectiveSchedulesFor(Long workOrderId, MesProScheduleOrderDO... schedules) {
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(workOrderId))).thenReturn(List.of(schedules));
    }

    private void stubSuccessfulInsertAndProcesses(List<MesProScheduleOrderProcessDO> processes) {
        stubSuccessfulActiveOrderInsert();
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(processes);
    }

    private void stubSuccessfulActiveOrderInsert() {
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);
    }

    private void stubUnscheduledActiveRoute() {
        when(routeProductMapper.selectListByItemIds(List.of(1001L))).thenReturn(List.of(
                MesProRouteProductDO.builder().id(7001L).itemId(1001L).routeId(922119L).build()));
        when(routeVersionMapper.selectListByRouteIds(List.of(922119L))).thenReturn(List.of(
                activeRouteVersion(activeRouteSnapshotJson())));
    }

    private static MesProRouteVersionDO activeRouteVersion(String routeSnapshotJson) {
        return MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .routeSnapshotJson(routeSnapshotJson)
                .build();
    }

    private static String activeRouteSnapshotJson() {
        return """
                {
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [
                        {"routeProcessId": 928609, "processId": 6001, "sort": 10}
                      ]
                    },
                    "scheduleUseConfigs": [
                      {
                        "routeId": 922119,
                        "routeProcessId": 928609,
                        "useType": "SCHEDULE",
                        "enabled": true,
                        "productionQuantityFactor": 1.000000
                      }
                    ]
                  }
                }
                """;
    }

    private static MesProScheduleOrderDO effectiveSchedule(Long id, Long routeId, Long routeVersionId) {
        return effectiveSchedule(id, 9001L, routeId, routeVersionId);
    }

    private static MesProScheduleOrderDO effectiveSchedule(Long id, Long workOrderId, Long routeId,
                                                           Long routeVersionId) {
        return MesProScheduleOrderDO.builder()
                .id(id)
                .workOrderId(workOrderId)
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
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(Long routeProcessId, Long processId, String factor,
                                                                String plannedQuantity) {
        return scheduleProcess(7701L, routeProcessId, processId, factor, plannedQuantity);
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(Long scheduleOrderId, Long routeProcessId,
                                                                Long processId, String factor,
                                                                String plannedQuantity) {
        return MesProScheduleOrderProcessDO.builder()
                .scheduleOrderId(scheduleOrderId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .enabled(Boolean.TRUE)
                .planDate(LocalDate.of(2026, 8, 5))
                .productionQuantityFactor(new BigDecimal(factor))
                .plannedQuantity(new BigDecimal(plannedQuantity))
                .build();
    }

    private static MesQaInspectionRegulationDO publishedRegulation(Long versionId) {
        return publishedRegulation(versionId, 928609L, 6001L);
    }

    private static MesQaInspectionRegulationDO publishedRegulation(Long versionId, Long routeProcessId,
                                                                   Long processId) {
        return MesQaInspectionRegulationDO.builder()
                .id(9901L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(versionId)
                .build();
    }

    private void stubCandidatePqcPrerequisites(MesQaInspectionRegulationDO... regulations) {
        List<MesQaInspectionRegulationDO> regulationList = List.of(regulations);
        when(inspectionRegulationMapper.selectListByProductIds(any())).thenReturn(regulationList);
        when(inspectionRegulationVersionMapper.selectBatchIds(any())).thenAnswer(invocation ->
                regulationList.stream()
                        .map(MesQaInspectionRegulationDO::getCurrentVersionId)
                        .map(versionId -> publishedRegulationVersion(versionId, true, null))
                        .toList());
        when(inspectionRegulationItemMapper.selectListByVersionIds(any())).thenAnswer(invocation ->
                regulationList.stream()
                        .map(MesQaInspectionRegulationDO::getCurrentVersionId)
                        .flatMap(versionId -> defaultPqcItems(versionId).stream())
                        .toList());
    }

    private static MesQaInspectionRegulationVersionDO publishedRegulationVersion(
            Boolean finalInspectionApplicable, String reason) {
        return publishedRegulationVersion(9902L, finalInspectionApplicable, reason);
    }

    private static MesQaInspectionRegulationVersionDO publishedRegulationVersion(
            Long id, Boolean finalInspectionApplicable, String reason) {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(id)
                .regulationId(9901L)
                .versionNo("V21-QA-1")
                .lifecycleStatus("PUBLISHED")
                .finalInspectionApplicable(finalInspectionApplicable)
                .finalInspectionNotApplicableReason(reason)
                .snapshotJson("{}")
                .build();
    }

    private static List<MesQaInspectionRegulationItemDO> defaultPqcItems() {
        return defaultPqcItems(9902L);
    }

    private static List<MesQaInspectionRegulationItemDO> defaultPqcItems(Long regulationVersionId) {
        return List.of(
                pqcItem(regulationVersionId, "FIRST", 5, null),
                pqcItem(regulationVersionId, "PATROL", null, new BigDecimal("0.050000")),
                pqcItem(regulationVersionId, "FINAL", 3, null));
    }

    private static MesQaInspectionRegulationItemDO pqcItem(String inspectionType, Integer fixedQuantity,
                                                           BigDecimal patrolRatio) {
        return pqcItem(9902L, inspectionType, fixedQuantity, patrolRatio);
    }

    private static MesQaInspectionRegulationItemDO pqcItem(Long regulationVersionId, String inspectionType,
                                                           Integer fixedQuantity, BigDecimal patrolRatio) {
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(regulationVersionId)
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
