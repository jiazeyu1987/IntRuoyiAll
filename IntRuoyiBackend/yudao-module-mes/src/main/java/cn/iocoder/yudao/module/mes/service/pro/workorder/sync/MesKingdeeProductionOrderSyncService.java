package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;

public interface MesKingdeeProductionOrderSyncService {

    MesKingdeeProductionOrderSyncResult syncWorkOrders();

    MesKingdeeProductionOrderSyncResult syncWorkOrdersFull();

    MesKingdeeProductionOrderSyncResult syncWorkOrdersFull(java.time.LocalDateTime windowStart,
                                                        java.time.LocalDateTime windowEnd);

    MesKingdeeProductionOrderSyncResult syncWorkOrders(ErpKingdeeSyncContext context);


    int syncByCode(String code);

}
