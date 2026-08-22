package cn.iocoder.yudao.module.erp.service.stock.kingdee;

import lombok.Getter;

@Getter
public class ErpKingdeeInventoryListSyncResult {

    private int createdCount;
    private int skippedCount;

    public void addCreated() {
        createdCount++;
    }

    public void addSkipped() {
        skippedCount++;
    }

}
