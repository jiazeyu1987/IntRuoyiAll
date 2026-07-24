package cn.iocoder.yudao.module.erp.service.purchase.sync;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ErpKingdeePurchaseOrderSyncResult {

    private Integer createdCount = 0;
    private Integer updatedCount = 0;
    private Integer skippedCount = 0;
    private List<Long> createdPurchaseOrderIds = new ArrayList<>();
    private List<Long> updatedPurchaseOrderIds = new ArrayList<>();
    private List<String> skippedSourceFids = new ArrayList<>();

    public void addCreated(Long purchaseOrderId) {
        createdPurchaseOrderIds.add(purchaseOrderId);
        createdCount = createdPurchaseOrderIds.size();
    }

    public void addUpdated(Long purchaseOrderId) {
        updatedPurchaseOrderIds.add(purchaseOrderId);
        updatedCount = updatedPurchaseOrderIds.size();
    }

    public void addSkipped(String sourceFid) {
        skippedSourceFids.add(sourceFid);
        skippedCount = skippedSourceFids.size();
    }

}
