package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.time.LocalDateTime;
import java.util.List;

public interface ErpKingdeeInventoryClient {

    List<ErpKingdeeInventoryRow> fetchInventoryRows(ErpKingdeeProperties properties);

    List<ErpKingdeeInventoryRow> fetchInventoryRowsModifiedBetween(ErpKingdeeProperties properties,
                                                                   LocalDateTime windowStart,
                                                                   LocalDateTime windowEnd);

}
