package cn.iocoder.yudao.module.mes.service.md.item.sync;

import java.time.LocalDateTime;

public interface MesKingdeeProductBomSyncService {

    MesKingdeeProductBomSyncResult syncErpBom(Long itemId);

    MesKingdeeProductBomSyncResult syncBomLinesModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd);

}
