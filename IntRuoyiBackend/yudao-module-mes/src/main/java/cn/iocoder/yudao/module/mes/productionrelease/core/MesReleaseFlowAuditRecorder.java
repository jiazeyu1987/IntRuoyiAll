package cn.iocoder.yudao.module.mes.productionrelease.core;

@FunctionalInterface
public interface MesReleaseFlowAuditRecorder {

    /**
     * Persists the audit event synchronously in the caller's business transaction.
     * Implementations must propagate write failures and must not use REQUIRES_NEW, async dispatch, or a no-op sink.
     */
    void record(MesReleaseFlowAuditCommand command);
}
