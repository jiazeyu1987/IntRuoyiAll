package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ErpKingdeeProductionMaterialListClient {

    /** Consume and commit each page before requesting the next one; never retain the full dataset. */
    void forEachProductionMaterialListPage(ErpKingdeeProperties properties,
            java.util.function.Consumer<List<ErpKingdeeProductionMaterialList>> pageConsumer);

    void forEachProductionMaterialListPage(ErpKingdeeProperties properties,
            LocalDateTime windowStart, LocalDateTime windowEnd,
            java.util.function.Consumer<List<ErpKingdeeProductionMaterialList>> pageConsumer);

    List<ErpKingdeeProductionMaterialList> fetchProductionMaterialListsModifiedBetween(
            ErpKingdeeProperties properties, LocalDateTime windowStart, LocalDateTime windowEnd);

    List<ErpKingdeeProductionMaterialList> fetchProductionMaterialListsByProductionOrderNos(
            ErpKingdeeProperties properties, Collection<String> productionOrderNos);


    List<ErpKingdeeProductionMaterialList> fetchProductionMaterialListsByCode(ErpKingdeeProperties properties, String code);
}
