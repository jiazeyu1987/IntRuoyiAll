package cn.iocoder.yudao.module.erp.service.production.sync;

import lombok.Getter;

@Getter
public class ErpKingdeeProductionPickListSyncResult {

    private int createdCount;
    private int updatedCount;
    private int skippedCount;

    public void addCreated() {
        createdCount++;
    }

    public void addUpdated() {
        updatedCount++;
    }

    public void addSkipped(String sourceFid) {
        skippedCount++;
    }

}
