package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class MesActiveOrderWorkOrderServiceTest {
    private final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
    private final MesProWorkOrderMapper workOrders = mock(MesProWorkOrderMapper.class);
    private final MesMdItemMapper items = mock(MesMdItemMapper.class);
    private final MesProcessPoolTeamMaintenanceAuditMapper audits = mock(MesProcessPoolTeamMaintenanceAuditMapper.class);
    private final MesActiveOrderWorkOrderService service = new MesActiveOrderWorkOrderService(orders, workOrders, items, audits);

    @BeforeEach
    void setup() {
        when(orders.selectByIdForUpdate(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(1L).workOrderId(20L).activeStatus("ACTIVE").build());
    }

    @Test
    void fillsOnlyBatchCodeAndTrimsInput() {
        var workOrder = new MesProWorkOrderDO();
        workOrder.setId(20L);
        when(workOrders.selectByIdForUpdate(20L)).thenReturn(workOrder);
        when(workOrders.updateById(any(MesProWorkOrderDO.class))).thenReturn(1);
        service.fillBatchCode(1L, 10L, " B20261120 ");
        var saved = ArgumentCaptor.forClass(MesProWorkOrderDO.class);
        verify(workOrders).updateById(saved.capture());
        assertEquals(20L, saved.getValue().getId());
        assertEquals("B20261120", saved.getValue().getBatchCode());
        assertNull(saved.getValue().getQuantity());
        verify(audits).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void rejectsExistingBatchIncludingRepeatSubmission() {
        var workOrder = new MesProWorkOrderDO();
        workOrder.setId(20L);
        workOrder.setBatchCode("EXISTING");
        when(workOrders.selectByIdForUpdate(20L)).thenReturn(workOrder);
        assertThrows(ServiceException.class, () -> service.fillBatchCode(1L, 10L, "NEW"));
        verify(workOrders, never()).updateById(any(MesProWorkOrderDO.class));
        verifyNoInteractions(audits);
    }

    @Test
    void rejectsAnotherLeaderBeforeAccessingWorkOrder() {
        assertThrows(ServiceException.class, () -> service.fillBatchCode(2L, 10L, "NEW"));
        verifyNoInteractions(workOrders, audits);
    }

    @Test
    void rejectsEmptyAndOversizedBatch() {
        assertThrows(ServiceException.class, () -> service.fillBatchCode(1L, 10L, "  "));
        assertThrows(ServiceException.class, () -> service.fillBatchCode(1L, 10L, "X".repeat(65)));
        verifyNoInteractions(workOrders, audits);
    }

    @Test
    void rejectsReadOutsideOwnership() {
        when(orders.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(2L).activeStatus("ACTIVE").build());
        assertThrows(ServiceException.class, () -> service.detail(1L, 10L));
        verifyNoInteractions(workOrders, items);
    }

    @Test
    void rejectsMissingWorkOrder() {
        assertThrows(ServiceException.class, () -> service.fillBatchCode(1L, 10L, "NEW"));
        verify(workOrders, never()).updateById(any(MesProWorkOrderDO.class));
    }

    @Test
    void rejectsFailedWriteWithoutSuccessAudit() {
        var workOrder = new MesProWorkOrderDO();
        workOrder.setId(20L);
        when(workOrders.selectByIdForUpdate(20L)).thenReturn(workOrder);
        assertThrows(ServiceException.class, () -> service.fillBatchCode(1L, 10L, "NEW"));
        verifyNoInteractions(audits);
    }
}
