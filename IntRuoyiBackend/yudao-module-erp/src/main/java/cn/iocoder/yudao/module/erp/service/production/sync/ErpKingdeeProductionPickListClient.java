package cn.iocoder.yudao.module.erp.service.production.sync;

import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;

import java.time.LocalDateTime;
import java.util.List;

public interface ErpKingdeeProductionPickListClient {

    List<ErpKingdeeProductionPickList> fetchProductionPickLists(
            ErpKingdeeProperties properties, LocalDateTime windowStart, LocalDateTime windowEnd);

    List<ErpKingdeeProductionPickList> fetchProductionPickListsModifiedBetween(
            ErpKingdeeProperties properties, LocalDateTime windowStart, LocalDateTime windowEnd);

}
