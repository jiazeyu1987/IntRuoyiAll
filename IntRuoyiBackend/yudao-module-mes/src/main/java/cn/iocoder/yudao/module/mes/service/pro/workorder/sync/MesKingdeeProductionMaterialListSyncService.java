package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import java.time.LocalDateTime;
import java.util.Collection;

public interface MesKingdeeProductionMaterialListSyncService {

    MesKingdeeProductionMaterialListSyncResult syncAllSkipExisting();

    MesKingdeeProductionMaterialListSyncResult syncModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd);

    MesKingdeeProductionMaterialListSyncResult syncByProductionOrderNos(Collection<String> productionOrderNos);

}
