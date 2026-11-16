package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderProductionMaterialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchProductionMaterialControllerTest {
    @Mock MesProEdhrBatchActiveOrderDetailService batchActiveOrderDetailService;
    @Mock MesTeamLeaderActiveOrderProductionMaterialService productionMaterialService;
    @InjectMocks MesProEdhrBatchExecutionController controller;

    @Test
    void activeOrderUsesFormalBatchScopeRatherThanCurrentLeaderOwnership() {
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(409L).setWorkOrderId(520L);
        var rows = List.of(new MesKingdeeProductionMaterialListRespVO());
        when(batchActiveOrderDetailService.getDetailByActiveOrderId(409L)).thenReturn(detail);
        when(productionMaterialService.getListForValidatedDetail(detail)).thenReturn(rows);
        assertSame(rows, controller.getActiveOrderProductionMaterialLists(null, 409L).getData());
        var order = inOrder(batchActiveOrderDetailService, productionMaterialService);
        order.verify(batchActiveOrderDetailService).getDetailByActiveOrderId(409L);
        order.verify(productionMaterialService).getListForValidatedDetail(detail);
        verifyNoMoreInteractions(batchActiveOrderDetailService, productionMaterialService);
    }

    @Test
    void batchIdentityUsesValidatedBatchSource() {
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(409L);
        when(batchActiveOrderDetailService.getDetail(700L)).thenReturn(detail);
        when(productionMaterialService.getListForValidatedDetail(detail)).thenReturn(List.of());
        assertTrue(controller.getActiveOrderProductionMaterialLists(700L, null).getData().isEmpty());
        verify(batchActiveOrderDetailService).getDetail(700L);
        verify(productionMaterialService).getListForValidatedDetail(detail);
    }

    @Test
    void invalidSourceStopsMaterialQuery() {
        var failure = new IllegalStateException("EDHR_BATCH_ACTIVE_ORDER_SOURCE_INVALID");
        when(batchActiveOrderDetailService.getDetailByActiveOrderId(409L)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> controller.getActiveOrderProductionMaterialLists(null, 409L)));
        verifyNoInteractions(productionMaterialService);
    }

    @Test
    void materialFailureIsNotConvertedToEmptySuccess() {
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(409L);
        var failure = new IllegalStateException("material query failed");
        when(batchActiveOrderDetailService.getDetail(700L)).thenReturn(detail);
        when(productionMaterialService.getListForValidatedDetail(detail)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> controller.getActiveOrderProductionMaterialLists(700L, null)));
    }

    @Test
    void endpointRequiresExistingBatchQueryPermission() throws Exception {
        var method = MesProEdhrBatchExecutionController.class.getMethod(
                "getActiveOrderProductionMaterialLists", Long.class, Long.class);
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:query')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
