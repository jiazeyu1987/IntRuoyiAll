package cn.iocoder.yudao.module.mes.service.md.item.kingdee;

import lombok.Getter;

@Getter
public class MesKingdeeBomListSyncResult {

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
