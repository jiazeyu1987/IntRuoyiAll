package cn.iocoder.yudao.module.erp.service.sale.sync;

import java.time.LocalDateTime;

public interface ErpKingdeeSaleOrderSyncService {

    ErpKingdeeSaleOrderSyncResult syncSaleOrders();

    ErpKingdeeSaleOrderSyncResult syncSaleOrdersFullSkipExisting();

    ErpKingdeeSaleOrderSyncResult syncSaleOrdersFullSkipExisting(LocalDateTime windowStart, LocalDateTime windowEnd);

    ErpKingdeeSaleOrderSyncResult syncSaleOrdersModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd);

}
