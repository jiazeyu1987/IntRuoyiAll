package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import java.time.LocalDateTime;
import java.util.Collection;

public interface MesKingdeeProductionMaterialListSyncService {

    cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult syncAllSkipExisting(Long runId);

    cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult syncAllSkipExisting(
            Long runId, LocalDateTime windowStart, LocalDateTime windowEnd);

    MesKingdeeProductionMaterialListSyncResult syncModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd);

    MesKingdeeProductionMaterialListSyncResult syncByProductionOrderNos(Collection<String> productionOrderNos);


    int syncByCode(String code);

}
