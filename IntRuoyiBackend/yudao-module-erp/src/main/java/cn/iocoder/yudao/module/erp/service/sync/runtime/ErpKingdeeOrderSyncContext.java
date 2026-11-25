package cn.iocoder.yudao.module.erp.service.sync.runtime;

public final class ErpKingdeeOrderSyncContext {
    private static final ThreadLocal<Long> RUN_ID = new ThreadLocal<>();
    private ErpKingdeeOrderSyncContext() {}
    public static void setRunId(Long runId) { RUN_ID.set(runId); }
    public static Long getRunId() { return RUN_ID.get(); }
    public static void clear() { RUN_ID.remove(); }
}
