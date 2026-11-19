package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
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

class ErpSaleOrderCreatorTest {
    @Test
    void importedOrdersWithoutCreatorDoNotBreakPageOrInventAnActor() {
        var controller = new ErpSaleOrderController();
        var orders = mock(ErpSaleOrderService.class);
        var products = mock(ErpProductService.class);
        var customers = mock(ErpCustomerService.class);
        var users = mock(AdminUserApi.class);
        ReflectionTestUtils.setField(controller, "saleOrderService", orders);
        ReflectionTestUtils.setField(controller, "productService", products);
        ReflectionTestUtils.setField(controller, "customerService", customers);
        ReflectionTestUtils.setField(controller, "adminUserApi", users);
        var imported = new ErpSaleOrderDO();
        imported.setId(1L);
        var blank = new ErpSaleOrderDO();
        blank.setId(2L);
        blank.setCreator("");
        var known = new ErpSaleOrderDO();
        known.setId(3L);
        known.setCreator("7");
        var user = new AdminUserRespDTO();
        user.setId(7L);
        user.setNickname("Existing creator");
        when(orders.getSaleOrderPage(any())).thenReturn(new PageResult<>(List.of(imported, blank, known), 3L));
        when(orders.getSaleOrderItemListByOrderIds(any())).thenReturn(List.of());
        when(products.getProductVOMap(any())).thenReturn(Map.of());
        when(customers.getCustomerMap(any())).thenReturn(Map.of());
        when(users.getUserMap(Set.of(7L))).thenReturn(Map.of(7L, user));

        var result = controller.getSaleOrderPage(new ErpSaleOrderPageReqVO()).getData();
        assertEquals(3L, result.getTotal());
        assertNull(result.getList().get(0).getCreatorName());
        assertNull(result.getList().get(1).getCreatorName());
        assertEquals("Existing creator", result.getList().get(2).getCreatorName());
        verify(users).getUserMap(Set.of(7L));
    }
}
