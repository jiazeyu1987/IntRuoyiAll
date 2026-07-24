package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.time.LocalDateTime;

public interface ErpKingdeePurchaseOrderSyncService {

    ErpKingdeePurchaseOrderSyncResult syncPurchaseOrders();

    ErpKingdeePurchaseOrderSyncResult syncPurchaseOrdersModifiedBetween(LocalDateTime windowStart,
                                                                        LocalDateTime windowEnd);

}
