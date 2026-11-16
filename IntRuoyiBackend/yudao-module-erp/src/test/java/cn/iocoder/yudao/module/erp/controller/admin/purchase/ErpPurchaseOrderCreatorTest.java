package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ErpPurchaseOrderCreatorTest {
    @Test
    void importedOrdersWithoutCreatorDoNotBreakPageOrInventAnActor() {
        var controller = new ErpPurchaseOrderController();
        var orders = mock(ErpPurchaseOrderService.class);
        var products = mock(ErpProductService.class);
        var suppliers = mock(ErpSupplierService.class);
        var users = mock(AdminUserApi.class);
        ReflectionTestUtils.setField(controller, "purchaseOrderService", orders);
        ReflectionTestUtils.setField(controller, "productService", products);
        ReflectionTestUtils.setField(controller, "supplierService", suppliers);
        ReflectionTestUtils.setField(controller, "adminUserApi", users);
        var imported = new ErpPurchaseOrderDO();
        imported.setId(1L);
        var blank = new ErpPurchaseOrderDO();
        blank.setId(2L);
        blank.setCreator("");
        var known = new ErpPurchaseOrderDO();
        known.setId(3L);
        known.setCreator("7");
        var user = new AdminUserRespDTO();
        user.setId(7L);
        user.setNickname("Existing creator");
        when(orders.getPurchaseOrderPage(any())).thenReturn(new PageResult<>(List.of(imported, blank, known), 3L));
        when(orders.getPurchaseOrderItemListByOrderIds(any())).thenReturn(List.of());
        when(products.getProductVOMap(any())).thenReturn(Map.of());
        when(suppliers.getSupplierMap(any())).thenReturn(Map.of());
        when(users.getUserMap(Set.of(7L))).thenReturn(Map.of(7L, user));

        var result = controller.getPurchaseOrderPage(new ErpPurchaseOrderPageReqVO()).getData();
        assertEquals(3L, result.getTotal());
        assertNull(result.getList().get(0).getCreatorName());
        assertNull(result.getList().get(1).getCreatorName());
        assertEquals("Existing creator", result.getList().get(2).getCreatorName());
        verify(users).getUserMap(Set.of(7L));
    }
}
