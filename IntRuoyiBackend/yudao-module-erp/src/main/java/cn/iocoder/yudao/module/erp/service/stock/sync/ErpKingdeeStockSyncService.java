package cn.iocoder.yudao.module.erp.service.stock.sync;

import java.time.LocalDateTime;

public interface ErpKingdeeStockSyncService {

    ErpKingdeeStockSyncResult syncStocks();

    ErpKingdeeStockSyncResult syncStocksModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd);

}
