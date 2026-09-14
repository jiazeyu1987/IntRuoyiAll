package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ErpKingdeeProductionMaterialListClient {

    List<ErpKingdeeProductionMaterialList> fetchProductionMaterialLists(
            ErpKingdeeProperties properties);

    List<ErpKingdeeProductionMaterialList> fetchProductionMaterialListsModifiedBetween(
            ErpKingdeeProperties properties, LocalDateTime windowStart, LocalDateTime windowEnd);

    List<ErpKingdeeProductionMaterialList> fetchProductionMaterialListsByProductionOrderNos(
            ErpKingdeeProperties properties, Collection<String> productionOrderNos);

}
