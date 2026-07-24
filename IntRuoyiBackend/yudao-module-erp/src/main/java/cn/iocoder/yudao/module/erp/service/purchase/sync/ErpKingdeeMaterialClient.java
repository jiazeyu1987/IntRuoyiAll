package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ErpKingdeeMaterialClient {

    List<ErpKingdeeMaterial> fetchMaterials(ErpKingdeeProperties properties);

    List<ErpKingdeeMaterial> fetchMaterialsModifiedBetween(ErpKingdeeProperties properties,
                                                           LocalDateTime windowStart,
                                                           LocalDateTime windowEnd);

    List<ErpKingdeeMaterial> fetchMaterialsByNumbers(ErpKingdeeProperties properties,
                                                     Collection<String> materialNumbers);

}
