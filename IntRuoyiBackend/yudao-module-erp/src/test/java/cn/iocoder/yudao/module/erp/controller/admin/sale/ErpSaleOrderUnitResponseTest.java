package cn.iocoder.yudao.module.erp.controller.admin.sale;
import cn.iocoder.yudao.module.erp.service.sale.*;
import cn.iocoder.yudao.module.erp.service.product.*;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
class ErpSaleOrderUnitResponseTest {
 @Test void detailAndPageUseTransactionUnitAndDoNotInventMissingUnit() {
  var controller = new ErpSaleOrderController();
  var orders = mock(ErpSaleOrderService.class); var products = mock(ErpProductService.class);
  var units = mock(ErpProductUnitService.class);
  ReflectionTestUtils.setField(controller,"saleOrderService",orders);
  ReflectionTestUtils.setField(controller,"productService",products);
  ReflectionTestUtils.setField(controller,"productUnitService",units);
  ReflectionTestUtils.setField(controller,"stockService",mock(ErpStockService.class));
  ReflectionTestUtils.setField(controller,"customerService",mock(ErpCustomerService.class));
  ReflectionTestUtils.setField(controller,"adminUserApi",mock(AdminUserApi.class));
  var order = new ErpSaleOrderDO().setId(100L);
  var line = new ErpSaleOrderItemDO().setId(10L).setOrderId(100L).setProductId(1L).setProductUnitId(9L);
  when(orders.getSaleOrder(100L)).thenReturn(order);
  when(orders.getSaleOrderItemListByOrderId(100L)).thenReturn(List.of(line));
  when(orders.getSaleOrderItemListByOrderIds(any())).thenReturn(List.of(line));
  when(products.getProductVOMap(any())).thenReturn(Map.of(1L,new ErpProductRespVO().setName("product").setUnitName("default unit")));
  when(units.getProductUnitMap(any())).thenReturn(Map.of(9L,new ErpProductUnitDO().setId(9L).setName("transaction unit")));
  assertEquals("transaction unit",controller.getSaleOrder(100L).getData().getItems().get(0).getProductUnitName());
  PageResult<ErpSaleOrderRespVO> page = ReflectionTestUtils.invokeMethod(controller,"buildSaleOrderVOPageResult",new PageResult<>(List.of(order),1L));
  assertEquals("transaction unit",page.getList().get(0).getItems().get(0).getProductUnitName());
  line.setProductUnitId(null);
  assertNull(controller.getSaleOrder(100L).getData().getItems().get(0).getProductUnitName());
 }
}
