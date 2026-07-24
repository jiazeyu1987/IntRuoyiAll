package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;

public interface MesKingdeeProductionOrderSyncService {

    MesKingdeeProductionOrderSyncResult syncWorkOrders();

    MesKingdeeProductionOrderSyncResult syncWorkOrders(ErpKingdeeSyncContext context);

}
