package cn.iocoder.yudao.module.erp.service.production.sync;

import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

public interface ErpKingdeeProductionPickListClient {

    List<ErpKingdeeProductionPickList> fetchProductionPickLists(
            ErpKingdeeProperties properties, LocalDateTime windowStart, LocalDateTime windowEnd);

    void forEachProductionPickListBatch(ErpKingdeeProperties properties,
            LocalDateTime windowStart, LocalDateTime windowEnd,
            Consumer<List<ErpKingdeeProductionPickList>> consumer);

    List<ErpKingdeeProductionPickList> fetchProductionPickListsModifiedBetween(
            ErpKingdeeProperties properties, LocalDateTime windowStart, LocalDateTime windowEnd);


    List<ErpKingdeeProductionPickList> fetchByCode(ErpKingdeeProperties properties, String code);
}
