package cn.iocoder.yudao.module.erp.service.stock.sync;

import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;

import java.time.LocalDateTime;
import java.util.List;

public interface ErpKingdeeStockMoveClient {

    List<ErpKingdeeStockMove> fetchStockMoves(ErpKingdeeProperties properties);

    List<ErpKingdeeStockMove> fetchStockMovesModifiedBetween(ErpKingdeeProperties properties,
                                                             LocalDateTime windowStart,
                                                             LocalDateTime windowEnd);

}
