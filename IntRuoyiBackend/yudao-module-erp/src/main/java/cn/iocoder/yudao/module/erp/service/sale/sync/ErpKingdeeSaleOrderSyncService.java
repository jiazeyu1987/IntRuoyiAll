package cn.iocoder.yudao.module.erp.service.sale.sync;

import java.time.LocalDateTime;

public interface ErpKingdeeSaleOrderSyncService {

    ErpKingdeeSaleOrderSyncResult syncSaleOrders();

    ErpKingdeeSaleOrderSyncResult syncSaleOrdersModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd);

}
