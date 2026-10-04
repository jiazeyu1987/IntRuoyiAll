package cn.iocoder.yudao.module.dcc.service.file.relations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Notification;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class DccRelationNotificationDispatcher {
    private final DccRelationStore store;
    private final DccRelationNotificationSender sender;
    private final DccRelationAccessPolicy access;
    private final TransactionTemplate tx;
    public DccRelationNotificationDispatcher(DccRelationStore store, DccRelationNotificationSender sender,
                                              DccRelationAccessPolicy access, PlatformTransactionManager manager) {
        this.store=store;this.sender=sender;this.access=access;tx=new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    /** Call after A's committed transaction. Throws a visible retryable error after persisting FAILED. */
    public void dispatch(Long deliveryId) {
        if(TransactionSynchronizationManager.isActualTransactionActive()) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_NOTIFICATION_REQUIRES_COMMITTED_CONTROL");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        Boolean ok=tx.execute(s->{
            var rows=store.jdbc().query("SELECT business_key,recipient_user_id,source_file_id,related_master_id,due_at,status,file_number_snapshot,version_no_snapshot FROM dcc_relation_notification_outbox WHERE tenant_id=? AND id=? FOR UPDATE",
                    (rs,n)->new Delivery(new Notification(rs.getString(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getObject(5,LocalDateTime.class),rs.getString(7),rs.getString(8)),rs.getString(6)),tenant,deliveryId);
            if(rows.size()!=1) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_NOTIFICATION_NOT_FOUND");
            var delivery=rows.get(0); if("SENT".equals(delivery.status())) return true;
            if(!"PENDING".equals(delivery.status()) && !"FAILED".equals(delivery.status())) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_NOTIFICATION_STATUS_INVALID");
            Long messageId;
            try {
                access.assertAssigneeAvailable(delivery.notification().recipientUserId(),delivery.notification().relatedMasterId());
                messageId=sender.send(delivery.notification());
                if(messageId==null || messageId<=0) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_PLATFORM_MESSAGE_ID_MISSING");
            } catch(RuntimeException error) {
                // No exception details persisted: sender may include credentials or personal content.
                if(store.jdbc().update("UPDATE dcc_relation_notification_outbox SET status='FAILED',failure_code='DELIVERY_FAILED' WHERE tenant_id=? AND id=? AND status<>'SENT'",tenant,deliveryId)!=1)
                    throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_NOTIFICATION_FAILED_STATUS_WRITE",error);
                return false;
            }
            if(store.jdbc().update("UPDATE dcc_relation_notification_outbox SET status='SENT',platform_message_id=?,failure_code=NULL WHERE tenant_id=? AND id=? AND status<>'SENT'",messageId,tenant,deliveryId)!=1)
                throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_NOTIFICATION_SENT_STATUS_WRITE_FAILED");
            return true;
        });
        if(!Objects.equals(ok,true)) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_NOTIFICATION_DELIVERY_FAILED_RETRYABLE");
    }
    private record Delivery(Notification notification,String status) {}
}
