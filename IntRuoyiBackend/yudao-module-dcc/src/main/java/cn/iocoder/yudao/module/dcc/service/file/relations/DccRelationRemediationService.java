package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.time.LocalDateTime;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class DccRelationRemediationService {
    private final DccRelationStore store;
    private final DccLatestControlledFileResolver files;
    private final DccRelationAccessPolicy access;
    private final DccRelationNotificationPostCommitScheduler notificationScheduler;
    public DccRelationRemediationService(DccRelationStore store, DccLatestControlledFileResolver files,
                                         DccRelationAccessPolicy access,DccRelationNotificationPostCommitScheduler notificationScheduler) {
        this.store = store; this.files = files; this.access = access;this.notificationScheduler=notificationScheduler;
    }
    public List<RemediationTask> listAssignedTasks(Long actorId) {
        if(actorId==null || actorId<=0) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationInputFailure("DCC_REMEDIATION_ACTOR_REQUIRED");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        return store.jdbc().query("SELECT task.id,event.controlled_file_id,task.related_master_id,task.assignee_user_id,task.due_at,task.status "
                + "FROM dcc_relation_remediation_task task INNER JOIN dcc_relation_controlled_event event ON event.tenant_id=task.tenant_id AND event.id=task.event_id "
                + "WHERE task.tenant_id=? AND task.assignee_user_id=? ORDER BY task.due_at,task.id",
                (rs,n)->new RemediationTask(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getObject(5,LocalDateTime.class),rs.getString(6)),tenant,actorId);
    }
    @Transactional(readOnly=true)
    public List<Arrangement> listArrangements(Long actorId,Long sourceFileId,String applicationRound){
        if(actorId==null || actorId<=0 || sourceFileId==null || sourceFileId<=0 || applicationRound==null
                || applicationRound.isBlank() || applicationRound.length()>64 || !applicationRound.equals(applicationRound.trim()))
            throw new DccRelationInputFailure("DCC_ARRANGEMENT_INPUT_REQUIRED");
        Long tenant=TenantContextHolder.getRequiredTenantId();var source=files.resolveSelected(sourceFileId);
        if(source==null || !Objects.equals(tenant,source.tenantId()) || !Objects.equals(sourceFileId,source.controlledFileId()))
            throw new DccRelationFailure("DCC_ARRANGEMENT_SOURCE_INVALID");
        access.assertCanReadArrangements(actorId,sourceFileId,applicationRound);
        return store.jdbc().query("SELECT related_master_id,assignee_user_id,due_at FROM dcc_relation_arrangement WHERE tenant_id=? AND source_file_id=? AND application_round=? ORDER BY related_master_id",
                (rs,n)->new Arrangement(rs.getLong(1),rs.getLong(2),rs.getObject(3,LocalDateTime.class)),tenant,sourceFileId,applicationRound);
    }
    @Transactional(propagation=Propagation.MANDATORY, rollbackFor=Exception.class)
    public void saveArrangements(Long actorId, Long sourceFileId, String round, List<Arrangement> arrangements, String reason) {
        if(actorId==null || actorId<=0 || round==null || round.isBlank() || round.length()>64 || arrangements==null
                || reason==null || reason.isBlank()) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationInputFailure("DCC_ARRANGEMENT_INPUT_REQUIRED");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var source=files.resolveSelected(sourceFileId);
        if(source==null || !Objects.equals(tenant,source.tenantId()) || !Objects.equals(sourceFileId,source.controlledFileId()))
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_ARRANGEMENT_SOURCE_INVALID");
        store.lockMaster(tenant,source.masterId());
        access.assertCanArrange(actorId,sourceFileId,round);
        if(!store.jdbc().queryForList("SELECT id FROM dcc_relation_controlled_event WHERE tenant_id=? AND controlled_file_id=? AND application_round=? FOR UPDATE",
                Long.class,tenant,sourceFileId,round).isEmpty()) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_ARRANGEMENT_ALREADY_FROZEN");
        var unique=new LinkedHashSet<Long>();
        for(var row:arrangements) {
            if(row==null || row.relatedMasterId()==null || row.relatedMasterId()<=0 || row.assigneeUserId()==null
                    || row.assigneeUserId()<=0 || row.dueAt()==null || row.dueAt().getNano()!=0 || !unique.add(row.relatedMasterId()))
                throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationInputFailure("DCC_ARRANGEMENT_INVALID");
            Long relationCount=store.jdbc().queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_related_file WHERE tenant_id=? "
                    + "AND controlled_file_id=? AND related_master_id=? AND deleted=0",Long.class,tenant,sourceFileId,row.relatedMasterId());
            if(relationCount!=1) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_ARRANGEMENT_RELATION_NOT_FROZEN");
            access.assertAssigneeAvailable(row.assigneeUserId(),row.relatedMasterId());
            var existing=store.jdbc().query("SELECT related_master_id,assignee_user_id,due_at FROM dcc_relation_arrangement "
                    + "WHERE tenant_id=? AND source_file_id=? AND application_round=? AND related_master_id=? FOR UPDATE",
                    (rs,n)->new Arrangement(rs.getLong(1),rs.getLong(2),rs.getObject(3,LocalDateTime.class)),tenant,sourceFileId,round,row.relatedMasterId());
            if(!existing.isEmpty()) {
                if(!existing.get(0).equals(row)) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_ARRANGEMENT_CONFLICT_REQUIRES_COORDINATION");
                continue;
            }
            store.jdbc().update("INSERT INTO dcc_relation_arrangement (tenant_id,source_file_id,application_round,related_master_id,assignee_user_id,due_at,arranged_by) VALUES (?,?,?,?,?,?,?)",
                    tenant,sourceFileId,round,row.relatedMasterId(),row.assigneeUserId(),row.dueAt(),actorId);
            store.audit("dcc.relation.arrange","ARRANGEMENT:"+sourceFileId+":"+round+":"+row.relatedMasterId(),reason,null,row);
        }
    }
    @Transactional(propagation=Propagation.MANDATORY, rollbackFor=Exception.class)
    public void recordControlled(ControlledEvent event) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(event==null || !Objects.equals(tenant,event.tenantId()) || event.masterId()==null || event.controlledFileId()==null
                || event.applicationRound()==null || event.applicationRound().isBlank() || event.applicationRound().length()>64
                || event.eventKey()==null || event.eventKey().isBlank() || event.eventKey().length()>128
                || event.controlledAt()==null || event.controlledAt().getNano()!=0) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_CONTROLLED_EVENT_INVALID");
        store.lockMaster(tenant,event.masterId());
        var previous=store.jdbc().query("SELECT master_id,controlled_file_id,application_round,event_key,controlled_at FROM dcc_relation_controlled_event "
                + "WHERE tenant_id=? AND (event_key=? OR (controlled_file_id=? AND application_round=?)) FOR UPDATE",
                (rs,n)->new ControlledEvent(tenant,rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getObject(5,LocalDateTime.class)),
                tenant,event.eventKey(),event.controlledFileId(),event.applicationRound());
        if(!previous.isEmpty()) {
            if(previous.size()!=1 || !previous.get(0).equals(event)) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_CONTROLLED_EVENT_REPLAY_CONFLICT");
            Long existingId=store.jdbc().queryForObject("SELECT id FROM dcc_relation_controlled_event WHERE tenant_id=? AND event_key=?",Long.class,tenant,event.eventKey());
            notificationScheduler.scheduleAfterCommit(tenant,existingId);
            return;
        }
        files.assertControlledEvent(event);
        var source=files.resolveSelected(event.controlledFileId());
        if(source==null || !Objects.equals(source.tenantId(),tenant) || !Objects.equals(source.masterId(),event.masterId())
                || !Objects.equals(source.controlledFileId(),event.controlledFileId()) || source.fileNumber()==null || source.fileNumber().isBlank()
                || source.versionNo()==null || source.versionNo().isBlank()) throw new DccRelationFailure("DCC_CONTROLLED_EVENT_SOURCE_IDENTITY_INVALID");
        var latest=files.resolveLatestForUpdate(event.masterId());
        if(latest==null || !Objects.equals(tenant,latest.tenantId()) || !Objects.equals(event.masterId(),latest.masterId())
                || latest.controlledFileId()==null) throw new DccRelationFailure("DCC_CONTROLLED_EVENT_LATEST_IDENTITY_INVALID");
        store.jdbc().update("INSERT INTO dcc_relation_controlled_event (tenant_id,master_id,controlled_file_id,application_round,event_key,controlled_at) VALUES (?,?,?,?,?,?)",
                tenant,event.masterId(),event.controlledFileId(),event.applicationRound(),event.eventKey(),event.controlledAt());
        Long eventId=store.jdbc().queryForObject("SELECT id FROM dcc_relation_controlled_event WHERE tenant_id=? AND event_key=?",Long.class,tenant,event.eventKey());
        var rows=store.jdbc().query("SELECT related_master_id,assignee_user_id,due_at FROM dcc_relation_arrangement WHERE tenant_id=? AND source_file_id=? AND application_round=? ORDER BY related_master_id FOR UPDATE",
                (rs,n)->new Arrangement(rs.getLong(1),rs.getLong(2),rs.getObject(3,LocalDateTime.class)),tenant,event.controlledFileId(),event.applicationRound());
        // Validate the complete frozen audience before any task or outbox is created.
        rows.forEach(row->access.assertAssigneeAvailable(row.assigneeUserId(),row.relatedMasterId()));
        // The source submission's existing relation rows are immutable approval evidence.
        // Controlled completion promotes their stable identities into the separate mutable current view.
        var masters=store.jdbc().queryForList("SELECT related_master_id FROM dcc_controlled_file_related_file WHERE tenant_id=? AND controlled_file_id=? AND deleted=0 ORDER BY related_master_id FOR UPDATE",
                Long.class,tenant,event.controlledFileId());
        if(masters.stream().anyMatch(master->master==null || master.equals(event.masterId())) || masters.stream().distinct().count()!=masters.size())
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_CONTROLLED_RELATION_IDENTITY_INVALID");
        if(rows.stream().anyMatch(row->!masters.contains(row.relatedMasterId())))
            throw new DccRelationFailure("DCC_ARRANGEMENT_RELATION_NO_LONGER_FROZEN");
        // An authentic historical event may be consumed for the first time after a newer control.
        // Its immutable tasks remain due at the frozen time; it must never regress the mutable current view.
        if(latest.controlled() && Objects.equals(latest.controlledFileId(),event.controlledFileId())) {
            store.jdbc().update("DELETE FROM dcc_current_file_relation WHERE tenant_id=? AND source_master_id=?",tenant,event.masterId());
            masters.forEach(master->store.jdbc().update("INSERT INTO dcc_current_file_relation VALUES (?,?,?)",tenant,event.masterId(),master));
            var setIds=store.jdbc().queryForList("SELECT controlled_file_id FROM dcc_current_file_relation_set WHERE tenant_id=? AND source_master_id=? FOR UPDATE",Long.class,tenant,event.masterId());
            if(setIds.isEmpty()) store.jdbc().update("INSERT INTO dcc_current_file_relation_set (tenant_id,source_master_id,controlled_file_id) VALUES (?,?,?)",tenant,event.masterId(),event.controlledFileId());
            else if(store.jdbc().update("UPDATE dcc_current_file_relation_set SET controlled_file_id=?,row_version=row_version+1 WHERE tenant_id=? AND source_master_id=?",event.controlledFileId(),tenant,event.masterId())!=1)
                throw new DccRelationFailure("DCC_RELATION_CURRENT_SET_WRITE_FAILED");
        }
        for(var row:rows) {
            store.jdbc().update("INSERT INTO dcc_relation_remediation_task (tenant_id,event_id,related_master_id,assignee_user_id,due_at,status) VALUES (?,?,?,?,?,'PENDING')",
                    tenant,eventId,row.relatedMasterId(),row.assigneeUserId(),row.dueAt());
            String key="dcc-remediation:"+tenant+":"+event.controlledFileId()+":"+event.applicationRound()+":"+row.relatedMasterId();
            store.jdbc().update("INSERT INTO dcc_relation_notification_outbox (tenant_id,event_id,related_master_id,recipient_user_id,source_file_id,due_at,business_key,status,file_number_snapshot,version_no_snapshot) VALUES (?,?,?,?,?,?,?,'PENDING',?,?)",
                    tenant,eventId,row.relatedMasterId(),row.assigneeUserId(),event.controlledFileId(),row.dueAt(),key,source.fileNumber(),source.versionNo());
        }
        store.audit("dcc.relation.controlled","CONTROLLED_EVENT:"+eventId,"成功受控事件 "+event.eventKey(),null,event);
        notificationScheduler.scheduleAfterCommit(tenant,eventId);
    }
}
