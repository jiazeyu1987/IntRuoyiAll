package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import lombok.Data;

@Data
public class MesKingdeeWorkOrderBomSyncResult {

    private Long workOrderId;
    private String erpBomVersion;
    private Integer syncedBomCount;

}
