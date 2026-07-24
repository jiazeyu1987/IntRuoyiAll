package cn.iocoder.yudao.module.erp.service.product.sync;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ErpKingdeeProductSyncResult {

    private final List<String> createdProductCodes = new ArrayList<>();
    private final List<String> updatedProductCodes = new ArrayList<>();
    private final List<String> skippedProductCodes = new ArrayList<>();

    public Integer getCreatedCount() {
        return createdProductCodes.size();
    }

    public Integer getUpdatedCount() {
        return updatedProductCodes.size();
    }

    public Integer getSkippedCount() {
        return skippedProductCodes.size();
    }

    public void addCreated(String productCode) {
        createdProductCodes.add(productCode);
    }

    public void addUpdated(String productCode) {
        updatedProductCodes.add(productCode);
    }

    public void addSkipped(String productCode) {
        skippedProductCodes.add(productCode);
    }

}
