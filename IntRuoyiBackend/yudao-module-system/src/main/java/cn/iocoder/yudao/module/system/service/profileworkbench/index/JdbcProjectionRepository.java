package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import cn.iocoder.yudao.module.system.api.workbench.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Repository @DS("master")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class JdbcProjectionRepository implements ProjectionRepository {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final ProjectionCodec codec;
    private final ProjectionHistoryRepository history;
    public JdbcProjectionRepository(DataSource dataSource,ObjectMapper mapper) {
        jdbc=new JdbcTemplate(dataSource); jdbc.setQueryTimeout(10); named=new NamedParameterJdbcTemplate(jdbc); codec=new ProjectionCodec(mapper);
        history=new JdbcProjectionHistoryRepository(dataSource,mapper);
    }
    private static void transaction(boolean write) {
        if(!TransactionSynchronizationManager.isActualTransactionActive()
            ||(write&&TransactionSynchronizationManager.isCurrentTransactionReadOnly()))
            throw new IllegalStateException("Projection repository requires the owning master transaction");
    }
    private static void one(int count) { if(count!=1) throw PROJECTION_BATCH_INVALID.exception(); }
    @Override public void initializeControls(long tenant,String revision) {
        transaction(true);if(tenant<0) throw INVALID_QUERY.exception();text(revision,128);
        List<String> global=jdbc.queryForList("SELECT registry_revision FROM system_workbench_global_control WHERE id=1 FOR UPDATE",String.class);
        if(global.isEmpty()) {
            one(jdbc.update("INSERT INTO system_workbench_global_control(id,global_head,security_epoch,registry_revision) VALUES(1,0,0,?)",revision));
            history.initializeFloor(Scope.GLOBAL,0);
        }
        else if(global.size()!=1||!global.get(0).equals(revision)) throw SNAPSHOT_MISMATCH.exception();
        List<Long> tenants=jdbc.queryForList("SELECT tenant_id FROM system_workbench_tenant_control WHERE tenant_id=? FOR UPDATE",Long.class,tenant);
        if(tenants.isEmpty()) {
            one(jdbc.update("INSERT INTO system_workbench_tenant_control(tenant_id,tenant_head,security_epoch,active_generation) VALUES(?,0,0,NULL)",tenant));
            history.initializeFloor(Scope.TENANT,tenant);
        }
    }
    @Override public Global lockGlobal(boolean exclusive) {
        transaction(exclusive);
        List<Global> rows=jdbc.query("SELECT global_head,security_epoch,registry_revision FROM system_workbench_global_control WHERE id=1 "+(exclusive?"FOR UPDATE":"LOCK IN SHARE MODE"),
            (r,n)->new Global(r.getLong(1),r.getLong(2),r.getString(3)));
        if(rows.size()!=1) throw PROJECTION_CONTEXT_MISSING.exception(); return rows.get(0);
    }
    @Override public Tenant lockTenant(long tenant,boolean exclusive) {
        transaction(exclusive);
        List<Tenant> rows=jdbc.query("SELECT tenant_id,tenant_head,security_epoch,active_generation FROM system_workbench_tenant_control WHERE tenant_id=? "+(exclusive?"FOR UPDATE":"LOCK IN SHARE MODE"),
            (r,n)->new Tenant(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4)),tenant);
        if(rows.size()!=1) throw PROJECTION_CONTEXT_MISSING.exception(); return rows.get(0);
    }
    @Override public void advanceGlobal(Global expected,Global next) {
        transaction(true); one(jdbc.update("UPDATE system_workbench_global_control SET global_head=?,security_epoch=?,registry_revision=? WHERE id=1 AND global_head=? AND security_epoch=? AND registry_revision=?",
            next.head(),next.securityEpoch(),next.registryRevision(),expected.head(),expected.securityEpoch(),expected.registryRevision()));
    }
    @Override public void advanceTenant(Tenant expected,Tenant next) {
        transaction(true); one(jdbc.update("UPDATE system_workbench_tenant_control SET tenant_head=?,security_epoch=? WHERE tenant_id=? AND tenant_head=? AND security_epoch=?",
            next.head(),next.securityEpoch(),expected.tenantId(),expected.head(),expected.securityEpoch()));
    }
    @Override public void append(Batch batch) {
        transaction(true);
        Global global=lockGlobal(batch.scope()==Scope.GLOBAL);
        long head=batch.scope()==Scope.GLOBAL?global.head():lockTenant(batch.tenantId(),true).head();
        if(batch.sequence()!=Math.addExact(head,1))throw PROJECTION_BATCH_INVALID.exception();
        history.appendIdentity(batch);
        for(Event e:batch.events()) one(jdbc.update("INSERT INTO system_workbench_outbox(event_id,scope,tenant_id,commit_sequence,batch_key,event_ordinal,expected_count,schema_version,payload,content_hash) VALUES(?,?,?,?,?,?,?,?,?,?)",
            e.eventId(),e.scope().name(),e.tenantId(),e.sequence(),e.batchKey(),e.ordinal(),e.expectedCount(),e.schemaVersion(),codec.encode(e),codec.hash(e)));
    }
    @Override public List<Event> events(Scope scope,long tenant,long sequence) {
        transaction(false);
        Global global=lockGlobal(false);
        long head=scope==Scope.GLOBAL?global.head():lockTenant(tenant,false).head();
        if(sequence<=history.floor(scope,tenant,false)||sequence>head)throw PROJECTION_BATCH_INVALID.exception();
        List<Event> events=jdbc.query("SELECT payload,content_hash FROM system_workbench_outbox WHERE scope=? AND tenant_id=? AND commit_sequence=? ORDER BY event_ordinal",
            (r,n)->{ String payload=r.getString(1); Event event=codec.decode(payload,Event.class);
                if(!codec.hash(event).equals(r.getString(2))) throw PROJECTION_BATCH_INVALID.exception(); return event; },scope.name(),tenant,sequence);
        history.verifyBatch(ProjectionBatchConsumer.complete(scope,tenant,sequence,events));
        return events;
    }
    @Override public void requireRetained(String generation,long tenant) {
        transaction(false);ProjectionRetention.scope(tenant,generation);
        // Current locked reads see retirement even with an older RR snapshot.
        // The global -> tenant fences block retirement during direct repository use.
        lockGlobal(false);lockTenant(tenant,false);
        if(!jdbc.queryForList("SELECT state FROM system_workbench_generation_retirement WHERE generation=? AND tenant_id=? LOCK IN SHARE MODE",
            String.class,generation,tenant).isEmpty())
            throw new IllegalStateException("Retired generation cannot be reused");
    }
    @Override public Checkpoint checkpoint(String generation,long tenant) {
        transaction(false);requireRetained(generation,tenant);
        List<Checkpoint> rows=jdbc.query("SELECT g.global_checkpoint,c.tenant_global_checkpoint,c.tenant_checkpoint,c.complete FROM system_workbench_generation_global g JOIN system_workbench_generation c ON c.generation=g.generation WHERE c.generation=? AND c.tenant_id=?",
            (r,n)->new Checkpoint(generation,tenant,r.getLong(1),r.getLong(2),r.getLong(3),r.getBoolean(4)),generation,tenant);
        if(rows.size()!=1) throw PROJECTION_CONTEXT_MISSING.exception();
        validateCheckpoint(rows.get(0));return rows.get(0);
    }
    @Override public void createGeneration(String generation,long tenant,Watermark initial,String revision,Set<String> sources) {
        transaction(true);requireRetained(generation,tenant); text(generation,64); text(revision,128);
        validateCheckpoint(new Checkpoint(generation,tenant,initial.globalHead(),initial.globalHead(),initial.tenantHead(),false));
        List<Long> global=jdbc.queryForList("SELECT global_checkpoint FROM system_workbench_generation_global WHERE generation=? FOR UPDATE",Long.class,generation);
        if(global.isEmpty()) one(jdbc.update("INSERT INTO system_workbench_generation_global(generation,global_checkpoint) VALUES(?,?)",generation,initial.globalHead()));
        else if(global.size()!=1||global.get(0)!=initial.globalHead()) throw PROJECTION_BATCH_INVALID.exception();
        one(jdbc.update("INSERT INTO system_workbench_generation(generation,tenant_id,tenant_global_checkpoint,tenant_checkpoint,complete,registry_revision,sources) VALUES(?,?,?,?,0,?,?)",
            generation,tenant,initial.globalHead(),initial.tenantHead(),revision,codec.encode(sources.stream().sorted().toList())));
    }
    private void validateCheckpoint(Checkpoint checkpoint) {
        Global global=lockGlobal(false);Tenant tenant=lockTenant(checkpoint.tenantId(),false);
        long globalFloor=history.floor(Scope.GLOBAL,0,false),tenantFloor=history.floor(Scope.TENANT,checkpoint.tenantId(),false);
        if(globalFloor>checkpoint.tenantGlobalCheckpoint()||checkpoint.tenantGlobalCheckpoint()>checkpoint.globalCheckpoint()
            ||checkpoint.globalCheckpoint()>global.head()||tenantFloor>checkpoint.tenantCheckpoint()||checkpoint.tenantCheckpoint()>tenant.head())
            throw new IllegalStateException("Generation checkpoint is outside retained payload range");
    }
    @Override public void advanceCheckpoint(Checkpoint expected,Checkpoint next) {
        transaction(true);requireRetained(expected.generation(),expected.tenantId());
        validateCheckpoint(expected);validateCheckpoint(next);
        if(!expected.generation().equals(next.generation())||expected.tenantId()!=next.tenantId()
            ||next.globalCheckpoint()<expected.globalCheckpoint()||next.tenantGlobalCheckpoint()<expected.tenantGlobalCheckpoint()
            ||next.tenantCheckpoint()<expected.tenantCheckpoint()) throw PROJECTION_BATCH_INVALID.exception();
        if(next.globalCheckpoint()!=expected.globalCheckpoint()) {
            Long current=jdbc.queryForObject("SELECT global_checkpoint FROM system_workbench_generation_global WHERE generation=? FOR UPDATE",Long.class,expected.generation());
            if(current==null||current<expected.globalCheckpoint()||current<next.globalCheckpoint()-1) throw PROJECTION_BATCH_INVALID.exception();
            if(current<next.globalCheckpoint()) one(jdbc.update("UPDATE system_workbench_generation_global SET global_checkpoint=? WHERE generation=? AND global_checkpoint=?",next.globalCheckpoint(),expected.generation(),current));
        }
        one(jdbc.update("UPDATE system_workbench_generation SET tenant_global_checkpoint=?,tenant_checkpoint=? WHERE generation=? AND tenant_id=? AND tenant_global_checkpoint=? AND tenant_checkpoint=?",
            next.tenantGlobalCheckpoint(),next.tenantCheckpoint(),expected.generation(),expected.tenantId(),expected.tenantGlobalCheckpoint(),expected.tenantCheckpoint()));
    }
    @Override public void applyTask(String generation,Task task) {
        transaction(true);requireRetained(generation,task.tenantId()); String hash=codec.hash(task);
        List<Map<String,Object>> existing=jdbc.queryForList("SELECT entity_version,content_hash FROM system_workbench_task_projection WHERE generation=? AND tenant_id=? AND task_key=? ORDER BY entity_version DESC FOR UPDATE",generation,task.tenantId(),task.taskKey());
        for(Map<String,Object> row:existing) if(((Number)row.get("entity_version")).longValue()==task.entityVersion()) {
            if(!hash.equals(row.get("content_hash"))) throw PROJECTION_BATCH_INVALID.exception();
            if(!history.rememberTask(generation,task))throw new IllegalStateException("Explicit historical version registration required");
            return;
        }
        if(history.rememberTask(generation,task))return;
        if(!existing.isEmpty()&&((Number)existing.get(0).get("entity_version")).longValue()>task.entityVersion()) return;
        jdbc.update("UPDATE system_workbench_task_projection SET version_to=? WHERE generation=? AND tenant_id=? AND task_key=? AND version_to IS NULL",task.entityVersion(),generation,task.tenantId(),task.taskKey());
        Audience audience=task.audience();
        one(jdbc.update("INSERT INTO system_workbench_task_projection(generation,tenant_id,source_id,task_key,entity_version,version_from,version_to,tombstone,task_type,source_label,detail,status_label,created_at,due_at,source_rank,row_order_key,audience_kind,policy_id,payload,content_hash) VALUES(?,?,?,?,?,?,NULL,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            generation,task.tenantId(),task.sourceId(),task.taskKey(),task.entityVersion(),task.entityVersion(),task.tombstone(),task.taskType(),task.source(),task.detail(),task.statusLabel(),task.createdAt(),task.dueAt(),task.sourceRank(),task.rowOrderKey(),audience==null?null:audience.kind().name(),audience==null?null:audience.policyId(),codec.encode(task),hash));
        if(!task.tombstone() && audience.kind()!=AudienceKind.TENANT_PERMISSION_QUEUE)
            for(Long user:audience.members()) one(jdbc.update("INSERT INTO system_workbench_task_audience(generation,tenant_id,task_key,entity_version,user_id) VALUES(?,?,?,?,?)",generation,task.tenantId(),task.taskKey(),task.entityVersion(),user));
    }
    @Override public void applyAuthorization(String generation,Authorization a) {
        transaction(true);requireRetained(generation,a.tenantId()); String hash=codec.hash(a);
        List<Map<String,Object>> existing=jdbc.queryForList("SELECT entity_version,content_hash FROM system_workbench_authorization_projection WHERE generation=? AND tenant_id=? AND source_id=? AND user_id=? AND task_key=? AND policy_id=? AND grant_key=? ORDER BY entity_version DESC FOR UPDATE",
            generation,a.tenantId(),a.sourceId(),a.userId(),a.taskKey(),a.policyId(),a.grantKey());
        for(Map<String,Object> row:existing) if(((Number)row.get("entity_version")).longValue()==a.entityVersion()) {
            if(!hash.equals(row.get("content_hash"))) throw PROJECTION_BATCH_INVALID.exception();
            if(!history.rememberAuthorization(generation,a))throw new IllegalStateException("Explicit historical version registration required");
            return;
        }
        if(history.rememberAuthorization(generation,a))return;
        if(!existing.isEmpty()&&((Number)existing.get(0).get("entity_version")).longValue()>a.entityVersion()) return;
        jdbc.update("UPDATE system_workbench_authorization_projection SET version_to=? WHERE generation=? AND tenant_id=? AND source_id=? AND user_id=? AND task_key=? AND policy_id=? AND grant_key=? AND version_to IS NULL",
            a.entityVersion(),generation,a.tenantId(),a.sourceId(),a.userId(),a.taskKey(),a.policyId(),a.grantKey());
        one(jdbc.update("INSERT INTO system_workbench_authorization_projection(generation,tenant_id,source_id,user_id,task_key,policy_id,grant_key,entity_version,version_to,revoked,valid_from,valid_until,payload,content_hash) VALUES(?,?,?,?,?,?,?,?,NULL,?,?,?,?,?)",
            generation,a.tenantId(),a.sourceId(),a.userId(),a.taskKey(),a.policyId(),a.grantKey(),a.entityVersion(),a.revoked(),Timestamp.from(a.validFrom()),a.validUntil()==null?null:Timestamp.from(a.validUntil()),codec.encode(a),hash));
    }
    @Override public boolean recordEvent(String generation,long tenant,Event event) {
        transaction(true);requireRetained(generation,tenant);
        List<String> hashes=jdbc.queryForList("SELECT content_hash FROM system_workbench_consumed_event WHERE generation=? AND tenant_id=? AND event_id=? FOR UPDATE",String.class,generation,tenant,event.eventId());
        String hash=codec.hash(event);
        if(!hashes.isEmpty()) { if(hashes.size()!=1||!hash.equals(hashes.get(0))) throw PROJECTION_BATCH_INVALID.exception(); return false; }
        one(jdbc.update("INSERT INTO system_workbench_consumed_event(generation,tenant_id,event_id,content_hash) VALUES(?,?,?,?)",generation,tenant,event.eventId(),hash)); return true;
    }
    @Override public void setComplete(Checkpoint expected) {
        transaction(true);requireRetained(expected.generation(),expected.tenantId()); one(jdbc.update("UPDATE system_workbench_generation SET complete=1 WHERE generation=? AND tenant_id=? AND complete=0 AND tenant_global_checkpoint=? AND tenant_checkpoint=?",
            expected.generation(),expected.tenantId(),expected.tenantGlobalCheckpoint(),expected.tenantCheckpoint()));
    }
    @Override public void publish(Tenant expected,String generation) {
        transaction(true);requireRetained(generation,expected.tenantId());
        long nextEpoch=Objects.equals(expected.activeGeneration(),generation)?expected.securityEpoch():Math.addExact(expected.securityEpoch(),1);
        validateCheckpoint(checkpoint(generation,expected.tenantId()));
        one(jdbc.update("UPDATE system_workbench_tenant_control SET active_generation=?,security_epoch=? WHERE tenant_id=? AND tenant_head=? AND security_epoch=? AND active_generation <=> ?",
            generation,nextEpoch,expected.tenantId(),expected.head(),expected.securityEpoch(),expected.activeGeneration()));
    }
    @Override public Set<String> generationSources(String generation,long tenant,String revision) {
        transaction(false);requireRetained(generation,tenant); List<Map<String,Object>> rows=jdbc.queryForList("SELECT registry_revision,sources FROM system_workbench_generation WHERE generation=? AND tenant_id=?",generation,tenant);
        if(rows.size()!=1||!revision.equals(rows.get(0).get("registry_revision"))) throw SNAPSHOT_MISMATCH.exception();
        String[] sources=codec.decode((String)rows.get(0).get("sources"),String[].class); return Set.of(sources);
    }
    @Override public List<Task> allTasks(String generation,long tenant) {
        transaction(false);requireRetained(generation,tenant); return jdbc.query("SELECT payload FROM system_workbench_task_projection WHERE generation=? AND tenant_id=? AND version_to IS NULL ORDER BY task_key",(r,n)->codec.decode(r.getString(1),Task.class),generation,tenant);
    }
    @Override public List<Authorization> allAuthorizations(String generation,long tenant) {
        transaction(false);requireRetained(generation,tenant); return jdbc.query("SELECT payload FROM system_workbench_authorization_projection WHERE generation=? AND tenant_id=? AND version_to IS NULL ORDER BY source_id,user_id,task_key,policy_id",(r,n)->codec.decode(r.getString(1),Authorization.class),generation,tenant);
    }
    @Override public List<String> authorizedSources(String generation,long tenant,long user,Instant at) {
        transaction(false);requireRetained(generation,tenant);
        return jdbc.queryForList("SELECT DISTINCT source_id FROM system_workbench_authorization_projection WHERE generation=? AND tenant_id=? AND user_id=? AND task_key='' AND policy_id=CONCAT('source:',source_id) AND version_to IS NULL AND revoked=0 AND valid_from<=? AND (valid_until IS NULL OR valid_until>?) ORDER BY source_id",String.class,generation,tenant,user,Timestamp.from(at),Timestamp.from(at));
    }
    @Override public Instant nextAuthorizationBoundary(String generation,long tenant,long user,List<String> sources,Instant at) {
        transaction(false);requireRetained(generation,tenant); if(sources.isEmpty()) return null;
        Map<String,Object> p=new HashMap<>(); p.put("generation",generation);p.put("tenant",tenant);p.put("user",user);p.put("sources",sources);p.put("at",Timestamp.from(at));
        List<Timestamp> values=named.query("SELECT MIN(boundary) FROM (SELECT valid_from boundary FROM system_workbench_authorization_projection WHERE generation=:generation AND tenant_id=:tenant AND user_id=:user AND source_id IN (:sources) AND version_to IS NULL AND revoked=0 AND valid_from>:at UNION ALL SELECT valid_until FROM system_workbench_authorization_projection WHERE generation=:generation AND tenant_id=:tenant AND user_id=:user AND source_id IN (:sources) AND version_to IS NULL AND revoked=0 AND valid_until>:at) b",p,(r,n)->r.getTimestamp(1));
        return values.isEmpty()||values.get(0)==null?null:values.get(0).toInstant();
    }
    private static final String ELIGIBLE=" FROM system_workbench_task_projection t WHERE t.generation=:generation AND t.tenant_id=:tenant AND t.source_id IN (:sources) AND t.version_to IS NULL AND t.tombstone=0"
        +" AND EXISTS(SELECT 1 FROM system_workbench_authorization_projection s WHERE s.generation=t.generation AND s.tenant_id=t.tenant_id AND s.source_id=t.source_id AND s.user_id=:user AND s.task_key='' AND s.policy_id=CONCAT('source:',t.source_id) AND s.version_to IS NULL AND s.revoked=0 AND s.valid_from<=:at AND (s.valid_until IS NULL OR s.valid_until>:at))"
        +" AND (t.audience_kind='TENANT_PERMISSION_QUEUE' OR EXISTS(SELECT 1 FROM system_workbench_task_audience m WHERE m.generation=t.generation AND m.tenant_id=t.tenant_id AND m.task_key=t.task_key AND m.entity_version=t.entity_version AND m.user_id=:user))"
        +" AND EXISTS(SELECT 1 FROM system_workbench_authorization_projection a WHERE a.generation=t.generation AND a.tenant_id=t.tenant_id AND a.source_id=t.source_id AND a.user_id=:user AND (a.task_key=t.task_key OR (a.task_key='' AND t.audience_kind='TENANT_PERMISSION_QUEUE')) AND a.policy_id=t.policy_id AND a.version_to IS NULL AND a.revoked=0 AND a.valid_from<=:at AND (a.valid_until IS NULL OR a.valid_until>:at))";
    private static final String HIDDEN="EXISTS(SELECT 1 FROM system_profile_workbench_task_visibility h WHERE h.tenant_id=t.tenant_id AND h.user_id=:user AND CAST(h.task_key AS BINARY)=CAST(t.task_key AS BINARY) AND h.deleted=0)";
    private Map<String,Object> parameters(Query q) {
        q.spec().validate(); Map<String,Object> p=new HashMap<>();p.put("generation",q.generation());p.put("tenant",q.tenant());p.put("user",q.user());p.put("sources",q.sources());p.put("at",Timestamp.from(q.authorizationAt()));
        p.put("taskType",q.spec().getTaskType()); p.put("filter",q.spec().getFilterValue()); return p;
    }
    private String filtered(Query q) {
        QueryContext s=q.spec(); String sql=" AND "+("hidden".equals(s.getVisibility())?"":"NOT ")+HIDDEN;
        if(s.getTaskType()!=null) sql+=" AND CAST(t.task_type AS BINARY)=CAST(:taskType AS BINARY)";
        if(s.getFilterField()!=null&&!s.getFilterValue().isEmpty()) {
            String field=column(s.getFilterField());
            sql+="eq".equals(s.getFilterOperator())?" AND CAST(COALESCE(t."+field+",'') AS BINARY)=CAST(:filter AS BINARY)":" AND LOCATE(CAST(:filter AS BINARY),CAST(COALESCE(t."+field+",'') AS BINARY))>0";
        }
        return sql;
    }
    private static String column(String key) {
        return switch(key) { case "taskType"->"task_type"; case "source"->"source_label";case "detail"->"detail";case "statusLabel"->"status_label";default->throw INVALID_QUERY.exception(); };
    }
    private String order(Query q) {
        QueryContext s=q.spec(); String custom=s.getSortKey()==null?"":"CAST(COALESCE(t."+column(s.getSortKey())+",'') AS BINARY) "+("desc".equals(s.getSortOrder())?"DESC":"ASC")+",";
        return " ORDER BY "+custom+"(t.due_at IS NULL) ASC,t.due_at ASC,CASE WHEN t.due_at IS NULL THEN COALESCE(t.created_at,'1970-01-01 00:00:00') ELSE NULL END DESC,t.source_rank ASC,CAST(t.row_order_key AS BINARY) DESC";
    }
    @Override public Totals totals(Query q) {
        transaction(false);requireRetained(q.generation(),q.tenant()); if(q.sources().isEmpty()) return new Totals(0,0,0);
        Map<String,Object> p=parameters(q); Long all=named.queryForObject("SELECT COUNT(*)"+ELIGIBLE,p,Long.class);
        Long hidden=named.queryForObject("SELECT COUNT(*)"+ELIGIBLE+" AND "+HIDDEN,p,Long.class);
        Long filtered=named.queryForObject("SELECT COUNT(*)"+ELIGIBLE+filtered(q),p,Long.class);
        if(all==null||hidden==null||filtered==null||all<0||hidden<0||hidden>all||filtered<0||filtered>all||all>=9_007_199_254_740_991L) throw SOURCE_CONTRACT_INVALID.exception();
        return new Totals(all,hidden,filtered);
    }
    @Override public List<TaskSummary> page(Query q,long offset,int limit) {
        transaction(false);requireRetained(q.generation(),q.tenant()); if(offset<0||limit<1||limit>100) throw INVALID_QUERY.exception(); if(q.sources().isEmpty()) return List.of();
        Map<String,Object> p=parameters(q);p.put("offset",offset);p.put("limit",limit);
        return named.query("SELECT t.payload"+ELIGIBLE+filtered(q)+order(q)+" LIMIT :limit OFFSET :offset",p,(r,n)->codec.decode(r.getString(1),Task.class).summary());
    }
    @Override public TaskSummary eligibleTask(Query q,String key) {
        transaction(false);requireRetained(q.generation(),q.tenant()); if(q.sources().isEmpty()) throw TASK_NOT_ELIGIBLE.exception(); Map<String,Object> p=parameters(q);p.put("key",key);
        List<TaskSummary> rows=named.query("SELECT t.payload"+ELIGIBLE+" AND t.task_key=:key",p,(r,n)->codec.decode(r.getString(1),Task.class).summary());
        if(rows.size()!=1) throw TASK_NOT_ELIGIBLE.exception(); return rows.get(0);
    }
}
