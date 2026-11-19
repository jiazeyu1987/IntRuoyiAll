package cn.iocoder.yudao.module.mes.service.md.item.sync;

import jakarta.validation.Valid;

import java.util.Collection;

public interface MesKingdeeItemSyncService {

    @Valid
    MesKingdeeItemSyncResult syncItems();

    /** Refresh every current ERP material without disabling other MES materials. */
    @Valid
    MesKingdeeItemSyncResult syncCurrentErpProducts();

    @Valid
    MesKingdeeItemSyncResult syncItemsByProductCodes(Collection<String> productCodes);

}
