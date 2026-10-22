package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import java.util.List;

@Service
public class DccRelationNotificationRecoveryService {
    private final DccRelationStore store;
    private final PermissionApi permission;
    private final DccRelationAccessPolicy access;
    private final DccRelationNotificationPostCommitScheduler scheduler;
    public DccRelationNotificationRecoveryService(DccRelationStore store,PermissionApi permission,
                                                   DccRelationAccessPolicy access,DccRelationNotificationPostCommitScheduler scheduler){
        this.store=store;this.permission=permission;this.access=access;this.scheduler=scheduler;
    }
    @Transactional(rollbackFor=Exception.class)
    public boolean requestRetry(Long actorId,Long eventId,String reason){
        if(reason==null || reason.isBlank()) throw new DccRelationInputFailure("DCC_RELATION_REASON_REQUIRED");
        if(actorId==null || actorId<=0 || eventId==null || eventId<=0) throw new DccRelationInputFailure("DCC_NOTIFICATION_RECOVERY_ID_REQUIRED");
        assertManager(actorId);
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var files=store.jdbc().queryForList("SELECT controlled_file_id FROM dcc_relation_controlled_event WHERE tenant_id=? AND id=? FOR UPDATE",Long.class,tenant,eventId);
        if(files.size()!=1) throw new DccRelationFailure("DCC_NOTIFICATION_EVENT_NOT_FOUND");
        access.assertNameVisible(actorId,files.get(0));
        var pending=store.jdbc().query("SELECT id,status FROM dcc_relation_notification_outbox WHERE tenant_id=? AND event_id=? AND status IN ('PENDING','FAILED') ORDER BY id FOR UPDATE",
                (rs,n)->new NotificationStatus(rs.getLong(1),rs.getString(2)),tenant,eventId);
        if(pending.isEmpty()) return false;
        store.audit("dcc.relation.notification.retry","CONTROLLED_EVENT:"+eventId,reason,pending,pending);
        scheduler.scheduleAfterCommit(tenant,eventId);
        return true;
    }
    public List<NotificationStatus> listStatus(Long actorId,Long eventId){
        if(actorId==null || actorId<=0 || eventId==null || eventId<=0) throw new DccRelationInputFailure("DCC_NOTIFICATION_RECOVERY_ID_REQUIRED");
        assertManager(actorId);Long tenant=TenantContextHolder.getRequiredTenantId();
        var files=store.jdbc().queryForList("SELECT controlled_file_id FROM dcc_relation_controlled_event WHERE tenant_id=? AND id=?",Long.class,tenant,eventId);
        if(files.size()!=1) throw new DccRelationFailure("DCC_NOTIFICATION_EVENT_NOT_FOUND");
        access.assertNameVisible(actorId,files.get(0));
        return store.jdbc().query("SELECT id,status FROM dcc_relation_notification_outbox WHERE tenant_id=? AND event_id=? ORDER BY id",
                (rs,n)->new NotificationStatus(rs.getLong(1),rs.getString(2)),tenant,eventId);
    }
    private void assertManager(Long actorId){
        if(!permission.hasAnyRoles(actorId,"doc_control") || !permission.hasAnyPermissions(actorId,"dcc:controlled-file:approve")
                || !permission.hasAnyPermissions(actorId,"dcc:controlled-file:publication-followup:manage"))
            throw new DccRelationFailure("DCC_NOTIFICATION_MANAGE_DENIED");
    }
    public record NotificationStatus(@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long deliveryId,String status){}
}
