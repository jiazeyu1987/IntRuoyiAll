package cn.iocoder.yudao.module.erp.service.stock.sync;

import lombok.Data;

@Data
public class ErpKingdeeStockSyncResult {

    private int syncedCount;
    private int skippedCount;
    private int createdCount;
    private int updatedCount;

    public void addCreated() { createdCount++; }
    public void addUpdated() { updatedCount++; }

    public void addSkipped() {
        skippedCount++;
    }

}
