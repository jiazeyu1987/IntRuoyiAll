package cn.iocoder.yudao.module.erp.service.stock.sync;

import java.time.LocalDateTime;

public interface ErpKingdeeStockSyncService {

    ErpKingdeeStockSyncResult syncStocks();

    ErpKingdeeStockSyncResult syncStocksFullSkipExisting();

    ErpKingdeeStockSyncResult syncStocksFullSkipExisting(LocalDateTime windowStart, LocalDateTime windowEnd);

    ErpKingdeeStockSyncResult syncStocksModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd);


    int syncByCode(String code);

}
