package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ErpPurchaseInOrderIntegrityTest {
    @Test
    void rejectsForeignOrderLineAndUnconvertibleUnitButPreservesSameUnitQuantity() {
        var service = new ErpPurchaseInServiceImpl();
        var products = mock(ErpProductService.class);
        var orders = mock(ErpPurchaseOrderService.class);
        ReflectionTestUtils.setField(service, "productService", products);
        ReflectionTestUtils.setField(service, "purchaseOrderService", orders);
        when(products.validProductList(any())).thenReturn(List.of(new ErpProductDO().setId(1L).setUnitId(2L)));
        var line = new ErpPurchaseOrderItemDO().setId(10L).setProductId(1L).setProductUnitId(2L);
        when(orders.getPurchaseOrderItemListByOrderId(100L)).thenReturn(List.of(line));
        var item = new ErpPurchaseInSaveReqVO.Item();
        item.setOrderItemId(11L); item.setProductId(1L); item.setCount(new BigDecimal("3"));
        assertThrows(IllegalArgumentException.class, () -> ReflectionTestUtils.invokeMethod(service, "validatePurchaseInItems", List.of(item), 100L));
        item.setOrderItemId(10L); line.setProductUnitId(9L);
        assertThrows(IllegalArgumentException.class, () -> ReflectionTestUtils.invokeMethod(service, "validatePurchaseInItems", List.of(item), 100L));
        line.setProductUnitId(2L);
        List<cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO> result = ReflectionTestUtils.invokeMethod(service, "validatePurchaseInItems", List.of(item), 100L);
        assertEquals(new BigDecimal("3"), result.get(0).getCount());
        assertEquals(2L, result.get(0).getProductUnitId());
    }

    @Test
    void cannotMoveExistingDocumentToAnotherOrder() {
        var service = new ErpPurchaseInServiceImpl();
        var mapper = mock(cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper.class);
        ReflectionTestUtils.setField(service, "purchaseInMapper", mapper);
        when(mapper.selectById(7L)).thenReturn(new cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO().setId(7L).setOrderId(100L));
        var request = new ErpPurchaseInSaveReqVO(); request.setId(7L); request.setOrderId(200L);
        assertThrows(IllegalArgumentException.class, () -> service.updatePurchaseIn(request));
        verify(mapper, never()).updateById(any(cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO.class));
    }

    @Test
    void batchDeleteLocksParentOrdersInAscendingOrder() {
        var service = new ErpPurchaseInServiceImpl();
        var mapper = mock(cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper.class);
        var orders = mock(ErpPurchaseOrderService.class);
        ReflectionTestUtils.setField(service, "purchaseInMapper", mapper);
        ReflectionTestUtils.setField(service, "purchaseOrderService", orders);
        when(mapper.selectByIds(List.of(7L, 8L))).thenReturn(List.of(
                new cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO().setId(7L).setOrderId(200L),
                new cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO().setId(8L).setOrderId(100L)));
        service.deletePurchaseIn(List.of(7L, 8L));
        var sequence = inOrder(orders, mapper);
        sequence.verify(mapper).selectByIds(List.of(7L, 8L));
        sequence.verify(orders).lockOrder(100L);
        sequence.verify(orders).lockOrder(200L);
        sequence.verify(mapper).selectByIdForUpdate(7L);
        sequence.verify(mapper).selectByIdForUpdate(8L);
    }

    @Test
    void historicalMismatchCannotApproveButCancellationReversesPersistedQuantity() {
        var service = new ErpPurchaseInServiceImpl();
        var mapper = mock(cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper.class);
        var items = mock(cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper.class);
        var stock = mock(cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService.class);
        var products = mock(ErpProductService.class);
        var orders = mock(ErpPurchaseOrderService.class);
        ReflectionTestUtils.setField(service, "purchaseInMapper", mapper);
        ReflectionTestUtils.setField(service, "purchaseInItemMapper", items);
        ReflectionTestUtils.setField(service, "stockRecordService", stock);
        ReflectionTestUtils.setField(service, "productService", products);
        ReflectionTestUtils.setField(service, "purchaseOrderService", orders);
        int process = cn.iocoder.yudao.module.erp.enums.ErpAuditStatus.PROCESS.getStatus();
        int approve = cn.iocoder.yudao.module.erp.enums.ErpAuditStatus.APPROVE.getStatus();
        var document = new cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO()
                .setId(7L).setOrderId(100L).setNo("HISTORICAL").setStatus(process).setPaymentPrice(BigDecimal.ZERO);
        when(mapper.selectById(7L)).thenReturn(document);
        when(mapper.updateByIdAndStatus(eq(7L), any(), any())).thenReturn(1);
        when(items.selectListByInId(7L)).thenReturn(List.of(
                new cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO()
                        .setId(8L).setInId(7L).setOrderItemId(10L).setProductId(1L)
                        .setProductUnitId(9L).setWarehouseId(4L).setCount(new BigDecimal("3"))));
        when(products.validProductList(any())).thenReturn(List.of(new ErpProductDO().setId(1L).setUnitId(2L)));
        when(orders.getPurchaseOrderItemListByOrderId(100L)).thenReturn(List.of(
                new ErpPurchaseOrderItemDO().setId(10L).setProductId(1L).setProductUnitId(9L)));
        assertThrows(IllegalArgumentException.class, () -> service.updatePurchaseInStatus(7L, approve));
        verifyNoInteractions(stock);
        document.setStatus(approve);
        service.updatePurchaseInStatus(7L, process);
        var posted = org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO.class);
        verify(stock).createStockRecord(posted.capture());
        assertEquals(new BigDecimal("-3"), posted.getValue().getCount());
        assertEquals(1L, posted.getValue().getProductId());
        assertEquals(4L, posted.getValue().getWarehouseId());
    }
}
