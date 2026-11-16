package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.time.LocalDateTime;

public interface ErpKingdeePurchaseOrderSyncService {

    ErpKingdeePurchaseOrderSyncResult syncPurchaseOrders();

    ErpKingdeePurchaseOrderSyncResult syncPurchaseOrdersFullSkipExisting();

    ErpKingdeePurchaseOrderSyncResult syncPurchaseOrdersFullSkipExisting(LocalDateTime windowStart, LocalDateTime windowEnd);

    ErpKingdeePurchaseOrderSyncResult syncPurchaseOrdersModifiedBetween(LocalDateTime windowStart,
                                                                        LocalDateTime windowEnd);

}
