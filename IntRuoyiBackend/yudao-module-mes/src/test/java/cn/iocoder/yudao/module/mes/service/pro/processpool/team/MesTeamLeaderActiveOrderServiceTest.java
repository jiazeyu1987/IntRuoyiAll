package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
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

    private MesTeamLeaderActiveOrderService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderServiceImpl(activeOrderMapper, workOrderService, auditMapper);
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

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .build());

        assertEquals(8101L, activeOrderId);
        verify(workOrderService).validateWorkOrderExists(9001L);
        ArgumentCaptor<MesProcessPoolActiveOrderDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderDO.class);
        verify(activeOrderMapper).insert(captor.capture());
        MesProcessPoolActiveOrderDO activeOrder = captor.getValue();
        assertEquals(3001L, activeOrder.getLeaderUserId());
        assertEquals(9001L, activeOrder.getWorkOrderId());
        assertEquals("ACTIVE", activeOrder.getActiveStatus());
        assertNotNull(activeOrder.getJoinedAt());
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldRemoveActiveOrderWithoutDeletingHistory() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .activeStatus("ACTIVE")
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
        assertNotNull(update.getRemovedAt());
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldListOnlyCurrentLeaderActiveOrdersInFifoOrder() {
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
    }
}
