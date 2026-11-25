package cn.iocoder.yudao.module.erp.service.sync.runtime;

/** An identified order failed inside an all-or-nothing source import. */
public final class ErpKingdeeOrderSyncFailure extends IllegalStateException {
    public ErpKingdeeOrderSyncFailure(String type, String fid, String billNo, String step, int fetchedCount, RuntimeException cause) {
        super("ERP同步失败：type=" + type + ", FID=" + fid + ", billNo=" + billNo
                + ", step=" + step + ", fetched=" + fetchedCount + ", failed=1，当前订单事务回滚；原因："
                + (cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage()), cause);
    }
}
