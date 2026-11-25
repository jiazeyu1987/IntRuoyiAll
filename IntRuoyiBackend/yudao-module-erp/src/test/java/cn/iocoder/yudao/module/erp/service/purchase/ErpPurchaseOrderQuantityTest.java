package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpPurchaseOrderQuantityTest {

    @Mock private ErpPurchaseOrderMapper orderMapper;
    @Mock private ErpPurchaseInMapper inMapper;
    @Mock private ErpPurchaseReturnMapper returnMapper;
    @InjectMocks private ErpPurchaseOrderServiceImpl service;

    private ErpPurchaseOrderDO order;

    @BeforeEach
    void setUp() {
        order = new ErpPurchaseOrderDO().setId(501L).setNo("PO-test")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(orderMapper.selectById(501L)).thenReturn(order);
    }

    @ParameterizedTest
    @CsvSource({",", "0,", ",0", "0,0"})
    void reverseApprovalChecksDocumentsForLegacyNullTotals(BigDecimal inCount, BigDecimal returnCount) {
        order.setInCount(inCount).setReturnCount(returnCount);
        when(orderMapper.updateByIdAndStatus(eq(501L), eq(20), any())).thenReturn(1);

        service.updatePurchaseOrderStatus(501L, ErpAuditStatus.PROCESS.getStatus());

        verify(inMapper).selectListByOrderId(501L);
        verify(returnMapper).selectListByOrderId(501L);
        var update = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(orderMapper).updateByIdAndStatus(eq(501L), eq(20), update.capture());
        assertEquals(10, update.getValue().getStatus());
        assertNull(update.getValue().getInCount());
        assertNull(update.getValue().getReturnCount());
    }

    @Test
    void actualInboundBlocksReversalEvenWhenHeaderIsNull() {
        when(inMapper.selectListByOrderId(501L)).thenReturn(List.of(
                new ErpPurchaseInDO().setNo("IN-1").setTotalCount(new BigDecimal("2"))));

        assertServiceException(() -> service.updatePurchaseOrderStatus(501L, 10),
                PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN);
        verify(orderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void actualReturnBlocksReversalEvenWhenHeaderIsNull() {
        when(returnMapper.selectListByOrderId(501L)).thenReturn(List.of(
                new ErpPurchaseReturnDO().setNo("RETURN-1").setTotalCount(BigDecimal.ONE)));

        assertServiceException(() -> service.updatePurchaseOrderStatus(501L, 10),
                PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN);
        verify(orderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void existingInboundTotalStillBlocksReversal() {
        order.setInCount(BigDecimal.ONE);
        assertServiceException(() -> service.updatePurchaseOrderStatus(501L, 10),
                PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN);
        verify(orderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void existingReturnTotalStillBlocksReversal() {
        order.setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ONE);
        assertServiceException(() -> service.updatePurchaseOrderStatus(501L, 10),
                PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN);
        verify(orderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void unknownInboundQuantityFailsExplicitlyInsteadOfTreatingItAsZero() {
        when(inMapper.selectListByOrderId(501L)).thenReturn(List.of(new ErpPurchaseInDO().setNo("IN-unknown")));
        var failure = assertThrows(IllegalStateException.class,
                () -> service.updatePurchaseOrderStatus(501L, 10));
        assertTrue(failure.getMessage().contains("IN-unknown"));
        verify(orderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void unknownReturnQuantityFailsExplicitlyInsteadOfTreatingItAsZero() {
        when(returnMapper.selectListByOrderId(501L)).thenReturn(List.of(new ErpPurchaseReturnDO().setNo("RETURN-unknown")));
        var failure = assertThrows(IllegalStateException.class,
                () -> service.updatePurchaseOrderStatus(501L, 10));
        assertTrue(failure.getMessage().contains("RETURN-unknown"));
        verify(orderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void approvingOrderDoesNotResetQuantities() {
        order.setStatus(10).setInCount(BigDecimal.TEN).setReturnCount(BigDecimal.ONE);
        when(orderMapper.updateByIdAndStatus(eq(501L), eq(10), any())).thenReturn(1);

        service.updatePurchaseOrderStatus(501L, 20);

        var update = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(orderMapper).updateByIdAndStatus(eq(501L), eq(10), update.capture());
        assertNull(update.getValue().getInCount());
        assertNull(update.getValue().getReturnCount());
        verifyNoInteractions(inMapper, returnMapper);
    }

    @Test
    void concurrentStatusChangeStillFails() {
        assertServiceException(() -> service.updatePurchaseOrderStatus(501L, 10), PURCHASE_ORDER_PROCESS_FAIL);
    }

}
