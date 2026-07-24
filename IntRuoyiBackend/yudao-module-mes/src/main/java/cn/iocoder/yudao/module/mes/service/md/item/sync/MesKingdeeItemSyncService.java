package cn.iocoder.yudao.module.mes.service.md.item.sync;

import jakarta.validation.Valid;

import java.util.Collection;

public interface MesKingdeeItemSyncService {

    @Valid
    MesKingdeeItemSyncResult syncItems();

    @Valid
    MesKingdeeItemSyncResult syncItemsByProductCodes(Collection<String> productCodes);

}
