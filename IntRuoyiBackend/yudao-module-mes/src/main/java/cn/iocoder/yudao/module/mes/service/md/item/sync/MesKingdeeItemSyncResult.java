package cn.iocoder.yudao.module.mes.service.md.item.sync;

import lombok.Data;

@Data
public class MesKingdeeItemSyncResult {

    private int createdCount;
    private int updatedCount;
    private int disabledCount;
    private int skippedCount;

    public void addCreated() {
        createdCount++;
    }

    public void addUpdated() {
        updatedCount++;
    }

    public void addDisabled() {
        disabledCount++;
    }

    public void addSkipped() {
        skippedCount++;
    }

}
