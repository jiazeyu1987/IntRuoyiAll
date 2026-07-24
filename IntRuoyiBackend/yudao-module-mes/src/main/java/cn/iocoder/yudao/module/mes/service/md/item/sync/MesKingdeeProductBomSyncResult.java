package cn.iocoder.yudao.module.mes.service.md.item.sync;

import lombok.Data;

@Data
public class MesKingdeeProductBomSyncResult {

    private Long itemId;
    private String erpBomVersion;
    private Integer syncedBomCount;
    private Integer syncedParentCount;
    private Integer recalculatedWorkOrderCount;

}
