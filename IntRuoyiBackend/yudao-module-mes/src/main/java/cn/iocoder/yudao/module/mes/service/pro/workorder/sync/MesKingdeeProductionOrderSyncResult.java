package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MesKingdeeProductionOrderSyncResult {

    private int createdCount;
    private int updatedCount;
    private int finishedCount;
    private int canceledCount;
    private int skippedCount;
    private List<Long> createdWorkOrderIds = new ArrayList<>();
    private List<Long> updatedWorkOrderIds = new ArrayList<>();
    private List<Long> finishedWorkOrderIds = new ArrayList<>();
    private List<Long> canceledWorkOrderIds = new ArrayList<>();
    private List<String> skippedSourceKeys = new ArrayList<>();

    public void addCreated(Long workOrderId) {
        createdCount++;
        createdWorkOrderIds.add(workOrderId);
    }

    public void addUpdated(Long workOrderId) {
        updatedCount++;
        updatedWorkOrderIds.add(workOrderId);
    }

    public void addFinished(Long workOrderId) {
        finishedCount++;
        finishedWorkOrderIds.add(workOrderId);
    }

    public void addCanceled(Long workOrderId) {
        canceledCount++;
        canceledWorkOrderIds.add(workOrderId);
    }

    public void addSkipped(String sourceKey) {
        skippedCount++;
        skippedSourceKeys.add(sourceKey);
    }

}
