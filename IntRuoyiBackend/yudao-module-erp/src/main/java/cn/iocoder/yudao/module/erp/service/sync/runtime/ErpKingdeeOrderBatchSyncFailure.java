package cn.iocoder.yudao.module.erp.service.sync.runtime;

import java.util.List;

public final class ErpKingdeeOrderBatchSyncFailure extends IllegalStateException {
    private final int failedCount;
    public ErpKingdeeOrderBatchSyncFailure(List<ErpKingdeeOrderSyncFailure> failures) {
        super("ERP订单同步部分失败，失败数量=" + failures.size() + "；成功订单已独立提交，失败订单待重试；" + failures.get(0).getMessage(), failures.get(0));
        failedCount = failures.size();
        failures.forEach(this::addSuppressed);
    }
    public int getFailedCount() { return failedCount; }
}
