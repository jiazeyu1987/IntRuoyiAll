package cn.iocoder.yudao.module.mes.service.md.item.kingdee;

import lombok.Getter;

@Getter
public class MesKingdeeBomListSyncResult {

    private int createdCount;
    private int skippedCount;

    public void addCreated() {
        createdCount++;
    }

    public void addSkipped() {
        skippedCount++;
    }

}
