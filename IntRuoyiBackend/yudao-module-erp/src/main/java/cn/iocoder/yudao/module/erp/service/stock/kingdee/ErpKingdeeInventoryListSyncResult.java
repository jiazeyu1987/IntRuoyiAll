package cn.iocoder.yudao.module.erp.service.stock.kingdee;

import lombok.Getter;

@Getter
public class ErpKingdeeInventoryListSyncResult {

    private int createdCount;
    private int skippedCount;
    private int updatedCount;

    public void addUpdated() { updatedCount++; }

    public void addCreated() {
        createdCount++;
    }

    public void addSkipped() {
        skippedCount++;
    }

}
