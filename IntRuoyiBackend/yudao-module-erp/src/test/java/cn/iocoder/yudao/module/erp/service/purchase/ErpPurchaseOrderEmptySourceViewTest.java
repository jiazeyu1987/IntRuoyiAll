package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.ErpPurchaseOrderController;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ErpPurchaseOrderEmptySourceViewTest {
    @Test void sourceDraftWithoutSupplierOrMaterialRemainsVisible() {
        var orderService = mock(ErpPurchaseOrderService.class);
        var supplierMapper = mock(ErpSupplierMapper.class);
        var supplierService = new ErpSupplierServiceImpl();
        ReflectionTestUtils.setField(supplierService, "supplierMapper", supplierMapper);
        var controller = new ErpPurchaseOrderController();
        ReflectionTestUtils.setField(controller, "purchaseOrderService", orderService);
        ReflectionTestUtils.setField(controller, "supplierService", supplierService);
        ReflectionTestUtils.setField(controller, "productService", mock(ErpProductService.class));
        ReflectionTestUtils.setField(controller, "adminUserApi", mock(AdminUserApi.class));
        when(orderService.getPurchaseOrderPage(any())).thenReturn(new PageResult<>(
                List.of(new ErpPurchaseOrderDO().setId(1L).setNo("source-draft").setStatus(10)), 1L));
        when(orderService.getPurchaseOrderItemListByOrderIds(Set.of(1L))).thenReturn(List.of(
                new ErpPurchaseOrderItemDO().setId(2L).setOrderId(1L).setCount(BigDecimal.ZERO)));

        var response = controller.getPurchaseOrderPage(new ErpPurchaseOrderPageReqVO()).getData();
        assertEquals(1L, response.getTotal());
        var draft = response.getList().get(0);
        assertEquals("source-draft", draft.getNo());
        assertNull(draft.getSupplierId());
        assertNull(draft.getSupplierName());
        assertEquals(1, draft.getItems().size());
        assertNull(draft.getItems().get(0).getProductId());
        assertNull(draft.getItems().get(0).getProductUnitId());
        verifyNoInteractions(supplierMapper);
    }
}
