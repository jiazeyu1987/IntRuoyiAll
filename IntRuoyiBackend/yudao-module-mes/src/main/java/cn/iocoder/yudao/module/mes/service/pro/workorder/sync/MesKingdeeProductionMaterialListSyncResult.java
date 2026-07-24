package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public class MesKingdeeProductionMaterialListSyncResult {

    private final Set<Long> createdIds = new LinkedHashSet<>();
    private final Set<Long> updatedIds = new LinkedHashSet<>();

    public void addCreated(Long id) {
        createdIds.add(id);
    }

    public void addUpdated(Long id) {
        updatedIds.add(id);
    }

    public int getCreatedCount() {
        return createdIds.size();
    }

    public int getUpdatedCount() {
        return updatedIds.size();
    }

}

