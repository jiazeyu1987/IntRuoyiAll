package cn.iocoder.yudao.module.erp.service.stock.sync;

import lombok.Getter;

@Getter
public class ErpKingdeeStockMoveSyncResult {

    private int createdCount;
    private int updatedCount;
    private int skippedCount;

    public void addCreated() {
        createdCount++;
    }

    public void addUpdated() {
        updatedCount++;
    }

    public void addSkipped() {
        skippedCount++;
    }

}
