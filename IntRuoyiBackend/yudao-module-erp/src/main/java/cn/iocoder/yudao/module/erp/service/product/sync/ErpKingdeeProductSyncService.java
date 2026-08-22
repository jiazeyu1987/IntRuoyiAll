package cn.iocoder.yudao.module.erp.service.product.sync;

import java.time.LocalDateTime;
import java.util.Collection;

public interface ErpKingdeeProductSyncService {

    ErpKingdeeProductSyncResult syncProducts();

    ErpKingdeeProductSyncResult syncProductsFullSkipExisting();

    ErpKingdeeProductSyncResult syncProductsModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd);

    ErpKingdeeProductSyncResult syncProductsByNumbers(Collection<String> materialNumbers);

}
