package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;

@Service
@Slf4j
public class DccPublicationNotificationPostCommitScheduler {

    @Resource
    private DccPublicationNotificationDispatchOrchestrator orchestrator;

    public void scheduleAfterCommit(Long tenantId, Long batchId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Publication notification scheduling requires an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    TenantUtils.execute(tenantId, () -> orchestrator.dispatchBatch(tenantId, batchId));
                } catch (RuntimeException ex) {
                    log.error("[afterCommit][publication notification dispatch failed, tenantId={}, batchId={}, failureType={}]",
                            tenantId, batchId, ex.getClass().getSimpleName());
                }
            }
        });
    }
}
