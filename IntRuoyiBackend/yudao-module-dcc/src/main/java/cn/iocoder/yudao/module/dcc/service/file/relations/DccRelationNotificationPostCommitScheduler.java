package cn.iocoder.yudao.module.dcc.service.file.relations;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.Executor;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DccRelationNotificationPostCommitScheduler {
    private static final Logger log=LoggerFactory.getLogger(DccRelationNotificationPostCommitScheduler.class);
    private final Executor executor;
    private final DccRelationStore store;
    private final DccRelationNotificationDispatcher dispatcher;
    public DccRelationNotificationPostCommitScheduler(@Qualifier("applicationTaskExecutor") Executor executor,
                                                     DccRelationStore store,DccRelationNotificationDispatcher dispatcher){
        this.executor=executor;this.store=store;this.dispatcher=dispatcher;
    }
    public void scheduleAfterCommit(Long tenantId,Long eventId){
        if(tenantId==null || !tenantId.equals(TenantContextHolder.getRequiredTenantId()) || eventId==null || eventId<=0
                || !TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.isSynchronizationActive())
            throw new DccRelationFailure("DCC_NOTIFICATION_SCHEDULING_CONTEXT_INVALID");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCommit(){
                try {
                    executor.execute(()->TenantUtils.execute(tenantId,()->dispatchUnsentEvent(tenantId,eventId)));
                } catch(RuntimeException error){
                    // Control and outbox have committed. The durable pending rows are recoverable by replay.
                    log.error("[dcc-remediation][committed dispatch enqueue failed, tenantId={}, eventId={}, failureType={}]",
                            tenantId,eventId,error.getClass().getSimpleName());
                }
            }
        });
    }
    private void dispatchUnsentEvent(Long tenantId,Long eventId){
        try {
            var ids=store.jdbc().queryForList("SELECT id FROM dcc_relation_notification_outbox WHERE tenant_id=? AND event_id=? AND status IN ('PENDING','FAILED') ORDER BY id",
                    Long.class,tenantId,eventId);
            for(Long id:ids){
                try {dispatcher.dispatch(id);}
                catch(RuntimeException error){
                    log.error("[dcc-remediation][committed delivery failed, tenantId={}, eventId={}, deliveryId={}, failureType={}]",
                            tenantId,eventId,id,error.getClass().getSimpleName());
                }
            }
        } catch(RuntimeException error){
            log.error("[dcc-remediation][committed outbox scan failed, tenantId={}, eventId={}, failureType={}]",
                    tenantId,eventId,error.getClass().getSimpleName());
        }
    }
}
