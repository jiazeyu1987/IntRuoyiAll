package cn.iocoder.yudao.module.erp.service.sale.sync;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ErpKingdeeSaleOrderSyncResult {

    private int createdCount;
    private int updatedCount;
    private int skippedCount;
    private List<Long> createdSaleOrderIds = new ArrayList<>();
    private List<Long> updatedSaleOrderIds = new ArrayList<>();
    private List<String> skippedSourceFids = new ArrayList<>();

    public void addCreated(Long saleOrderId) {
        createdCount++;
        createdSaleOrderIds.add(saleOrderId);
    }

    public void addUpdated(Long saleOrderId) {
        updatedCount++;
        updatedSaleOrderIds.add(saleOrderId);
    }

    public void addSkipped(String sourceFid) {
        skippedCount++;
        skippedSourceFids.add(sourceFid);
    }

}
