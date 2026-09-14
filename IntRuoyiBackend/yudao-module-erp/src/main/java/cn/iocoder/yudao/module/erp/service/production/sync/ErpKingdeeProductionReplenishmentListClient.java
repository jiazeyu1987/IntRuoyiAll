package cn.iocoder.yudao.module.erp.service.production.sync;

import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;

import java.time.LocalDateTime;
import java.util.List;

public interface ErpKingdeeProductionReplenishmentListClient {

    List<ErpKingdeeProductionReplenishmentList> fetchProductionReplenishmentLists(
            ErpKingdeeProperties properties, LocalDateTime windowStart, LocalDateTime windowEnd);

    List<ErpKingdeeProductionReplenishmentList> fetchProductionReplenishmentListsModifiedBetween(
            ErpKingdeeProperties properties, LocalDateTime windowStart, LocalDateTime windowEnd);

}
