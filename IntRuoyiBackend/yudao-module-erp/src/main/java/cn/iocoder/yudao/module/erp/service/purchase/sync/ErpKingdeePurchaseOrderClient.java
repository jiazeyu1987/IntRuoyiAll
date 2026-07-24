package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ErpKingdeePurchaseOrderClient {

    List<ErpKingdeePurchaseOrder> fetchPurchaseOrders(ErpKingdeeProperties properties);

    List<ErpKingdeePurchaseOrder> fetchPurchaseOrdersModifiedBetween(ErpKingdeeProperties properties,
                                                                     LocalDateTime windowStart,
                                                                     LocalDateTime windowEnd);

    Map<String, ErpKingdeeMaterialDetail> fetchMaterialDetails(ErpKingdeeProperties properties,
                                                               Collection<String> materialNumbers);

}
