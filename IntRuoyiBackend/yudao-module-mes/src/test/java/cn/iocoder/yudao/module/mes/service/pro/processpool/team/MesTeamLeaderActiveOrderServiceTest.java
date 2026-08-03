package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderServiceTest {

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;

    private MesTeamLeaderActiveOrderService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderServiceImpl(activeOrderMapper, workOrderService, auditMapper,
                scheduleOrderMapper, scheduleOrderProcessMapper, processSnapshotMapper);
    }

    @Test
    void shouldAddWorkOrderToLeaderActivePoolWithJoinTime() {
        when(workOrderService.validateWorkOrderExists(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .quantity(new BigDecimal("200"))
                .build());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(List.of(
                scheduleProcess(928609L, 6001L, "3.000000", "600.000000"),
                scheduleProcess(928610L, 6002L, "2.000000", "400.000000")));
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());

        assertEquals(8101L, activeOrderId);
        verify(workOrderService).validateWorkOrderExists(9001L);
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
        org.mockito.ArgumentCaptor<Collection<MesProcessPoolActiveOrderProcessSnapshotDO>> snapshotCaptor =
                org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(processSnapshotMapper).insertBatch(snapshotCaptor.capture());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = List.copyOf(snapshotCaptor.getValue());
        assertEquals(2, snapshots.size());
        assertSnapshot(snapshots.get(0), 8101L, 9001L, 922119L, 448L, 928609L, 6001L, "200", "3.000000", "600.000000");
        assertSnapshot(snapshots.get(1), 8101L, 9001L, 922119L, 448L, 928610L, 6002L, "200", "2.000000", "400.000000");
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldReturnExistingActiveOrderWhenSameWorkOrderRouteVersionAlreadyActive() {
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(MesProcessPoolActiveOrderDO.builder()
                        .id(8101L)
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .activeStatus("ACTIVE")
                        .businessStatus("ACTIVE")
                        .build());

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());

        assertEquals(8101L, activeOrderId);
        verify(activeOrderMapper).selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L);
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldReturnExistingActiveOrderWhenConcurrentInsertHitsUniqueKey() {
        MesProcessPoolActiveOrderDO existing = MesProcessPoolActiveOrderDO.builder()
                .id(8102L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .build();
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(null, existing);
        when(workOrderService.validateWorkOrderExists(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .quantity(new BigDecimal("200"))
                .build());
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class)))
                .thenThrow(new DuplicateKeyException("uk_mes_pp_active_order"));

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());

        assertEquals(8102L, activeOrderId);
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldRejectConflictingRouteBeforeInsertingActiveOrder() {
        when(workOrderService.validateWorkOrderExists(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .quantity(new BigDecimal("200"))
                .build());
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .routeId(922120L)
                        .routeVersionId(449L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED.getCode(), ex.getCode());
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldRemoveActiveOrderWithoutDeletingHistory() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .version(7)
                .build());

        service.removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO.builder()
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .build());

        ArgumentCaptor<MesProcessPoolActiveOrderDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderDO.class);
        verify(activeOrderMapper).updateById(captor.capture());
        MesProcessPoolActiveOrderDO update = captor.getValue();
        assertEquals(8101L, update.getId());
        assertEquals("REMOVED", update.getActiveStatus());
        assertEquals("REMOVED", update.getBusinessStatus());
        assertEquals(7, update.getVersion());
        assertNotNull(update.getRemovedAt());
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldListUnifiedActiveOrdersAcrossLeadersInFifoOrder() {
        List<MesProcessPoolActiveOrderDO> expected = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(4001L)
                .workOrderId(9001L)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveList()).thenReturn(expected);

        List<MesProcessPoolActiveOrderDO> activeOrders = service.listActiveOrders(3001L);

        assertEquals(expected, activeOrders);
        verify(activeOrderMapper).selectActiveList();
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(Long routeProcessId, Long processId, String factor,
                                                                String plannedQuantity) {
        return MesProScheduleOrderProcessDO.builder()
                .routeProcessId(routeProcessId)
                .processId(processId)
                .enabled(Boolean.TRUE)
                .productionQuantityFactor(new BigDecimal(factor))
                .plannedQuantity(new BigDecimal(plannedQuantity))
                .build();
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
