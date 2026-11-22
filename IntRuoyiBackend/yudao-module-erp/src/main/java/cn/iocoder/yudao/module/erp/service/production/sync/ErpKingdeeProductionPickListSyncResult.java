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

    public void add(ErpKingdeeProductionPickListSyncResult batch) {
        createdCount = Math.addExact(createdCount, batch.createdCount);
        updatedCount = Math.addExact(updatedCount, batch.updatedCount);
        skippedCount = Math.addExact(skippedCount, batch.skippedCount);
    }

    public void addUpdated() {
        updatedCount++;
    }

    public void addSkipped(String sourceFid) {
        skippedCount++;
    }

}
