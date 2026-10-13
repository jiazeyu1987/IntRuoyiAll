package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionHistory.*;

@Repository @DS("master")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class JdbcProjectionHistoryRepository implements ProjectionHistoryRepository {
    private final JdbcTemplate jdbc;
    private final ProjectionCodec codec;
    private static final String BATCH="system_workbench_batch_identity";
    private static final String EVENT="system_workbench_event_identity";
    private static final String TASK="system_workbench_task_version_identity";
    private static final String AUTH="system_workbench_auth_version_identity";
    private static final String OBS="observed_at_millis,purge_not_before_millis,retention_seconds,rollback_seconds,clock_skew_seconds,approval_reference";
    public JdbcProjectionHistoryRepository(DataSource source,ObjectMapper mapper) {jdbc=new JdbcTemplate(source);jdbc.setQueryTimeout(10);codec=new ProjectionCodec(mapper);}
    private static void transaction(boolean write) {
        if(!TransactionSynchronizationManager.isActualTransactionActive()||(write&&TransactionSynchronizationManager.isCurrentTransactionReadOnly()))
            throw new IllegalStateException("History repository requires the owning master transaction");
    }
    private static void one(int count) {if(count!=1)throw new IllegalStateException("History write row count mismatch");}
    private static long number(Map<String,Object> row,String key) {Object v=row.get(key);if(!(v instanceof Number))throw new IllegalStateException("History numeric fact missing: "+key);return ((Number)v).longValue();}
    private static String string(Map<String,Object> row,String key) {Object v=row.get(key);if(!(v instanceof String))throw new IllegalStateException("History text fact missing: "+key);return (String)v;}
    private static String lock(boolean exclusive) {return exclusive?" FOR UPDATE":" LOCK IN SHARE MODE";}
    @Override public void initializeFloor(Scope scope,long tenant) {
        transaction(true);List<Long> rows=jdbc.queryForList("SELECT payload_floor FROM system_workbench_scope_floor WHERE scope=? AND tenant_id=? FOR UPDATE",Long.class,scope.name(),tenant);
        if(rows.isEmpty())one(jdbc.update("INSERT INTO system_workbench_scope_floor(scope,tenant_id,payload_floor) VALUES(?,?,0)",scope.name(),tenant));
        else if(rows.size()!=1||rows.get(0)<0)throw new IllegalStateException("Invalid registered scope floor");
    }
    @Override public long floor(Scope scope,long tenant,boolean exclusive) {
        transaction(false);List<Long> rows=jdbc.queryForList("SELECT payload_floor FROM system_workbench_scope_floor WHERE scope=? AND tenant_id=?"+lock(exclusive),Long.class,scope.name(),tenant);
        if(rows.size()!=1||rows.get(0)<0)throw new IllegalStateException("Explicit scope floor registration required");return rows.get(0);
    }
    private record EventIdentity(String eventId,Scope scope,long tenant,long sequence,String batchKey,int ordinal,int expected,int schema,String hash) { }
    private List<EventIdentity> identities(Batch batch) {
        return batch.events().stream().map(e->new EventIdentity(e.eventId(),e.scope(),e.tenantId(),e.sequence(),e.batchKey(),e.ordinal(),e.expectedCount(),e.schemaVersion(),codec.hash(e))).toList();
    }
    private String digest(Batch batch) {return codec.hash(identities(batch));}
    @Override public void appendIdentity(Batch batch) {
        transaction(true);long floor=floor(batch.scope(),batch.tenantId(),false);
        if(batch.sequence()<=floor)throw new IllegalStateException("Purged batch sequence cannot be reused");
        one(jdbc.update("INSERT INTO "+BATCH+"(scope,tenant_id,commit_sequence,batch_key,expected_count,schema_version,batch_hash) VALUES(?,?,?,?,?,4,?)",
            batch.scope().name(),batch.tenantId(),batch.sequence(),batch.batchKey(),batch.events().size(),digest(batch)));
        for(EventIdentity e:identities(batch))one(jdbc.update("INSERT INTO "+EVENT+"(event_id,scope,tenant_id,commit_sequence,batch_key,event_ordinal,expected_count,schema_version,content_hash) VALUES(?,?,?,?,?,?,?,?,?)",
            e.eventId,e.scope.name(),e.tenant,e.sequence,e.batchKey,e.ordinal,e.expected,e.schema,e.hash));
    }
    private List<Map<String,Object>> batchIdentity(Batch batch,boolean exclusive) {
        return jdbc.queryForList("SELECT * FROM "+BATCH+" WHERE scope=? AND tenant_id=? AND commit_sequence=?"+lock(exclusive),batch.scope().name(),batch.tenantId(),batch.sequence());
    }
    @Override public void verifyBatch(Batch batch) {
        transaction(false);List<Map<String,Object>> rows=batchIdentity(batch,false);
        if(rows.size()!=1)throw new IllegalStateException("Historical batch identity registration required");Map<String,Object> row=rows.get(0);
        if(!string(row,"batch_key").equals(batch.batchKey())||number(row,"expected_count")!=batch.events().size()||number(row,"schema_version")!=4||!string(row,"batch_hash").equals(digest(batch)))
            throw new IllegalStateException("Permanent batch identity conflict");
        List<Map<String,Object>> events=jdbc.queryForList("SELECT * FROM "+EVENT+" WHERE scope=? AND tenant_id=? AND commit_sequence=? ORDER BY event_ordinal LOCK IN SHARE MODE",batch.scope().name(),batch.tenantId(),batch.sequence());
        if(events.size()!=batch.events().size())throw new IllegalStateException("Incomplete permanent batch identity");
        List<EventIdentity> vector=identities(batch);
        for(int i=0;i<events.size();i++) {
            Map<String,Object> e=events.get(i);EventIdentity expected=vector.get(i);
            if(!string(e,"event_id").equals(expected.eventId)||!string(e,"scope").equals(expected.scope.name())||number(e,"tenant_id")!=expected.tenant||number(e,"commit_sequence")!=expected.sequence
                ||!string(e,"batch_key").equals(expected.batchKey)||number(e,"event_ordinal")!=expected.ordinal||number(e,"expected_count")!=expected.expected||number(e,"schema_version")!=expected.schema||!string(e,"content_hash").equals(expected.hash))
                throw new IllegalStateException("Permanent event identity conflict");
        }
    }
    @Override public boolean rememberTask(String generation,Task task) {
        transaction(true);List<String> hashes=jdbc.queryForList("SELECT content_hash FROM "+TASK+" WHERE generation=? AND tenant_id=? AND task_key=? AND entity_version=? FOR UPDATE",String.class,generation,task.tenantId(),task.taskKey(),task.entityVersion());
        if(!hashes.isEmpty()) {same(hashes,codec.hash(task));return true;}
        one(jdbc.update("INSERT INTO "+TASK+"(generation,tenant_id,task_key,entity_version,source_id,content_hash) VALUES(?,?,?,?,?,?)",generation,task.tenantId(),task.taskKey(),task.entityVersion(),task.sourceId(),codec.hash(task)));return false;
    }
    @Override public boolean rememberAuthorization(String generation,Authorization a) {
        transaction(true);List<String> hashes=jdbc.queryForList("SELECT content_hash FROM "+AUTH+" WHERE generation=? AND tenant_id=? AND source_id=? AND user_id=? AND task_key=? AND policy_id=? AND grant_key=? AND entity_version=? FOR UPDATE",
            String.class,generation,a.tenantId(),a.sourceId(),a.userId(),a.taskKey(),a.policyId(),a.grantKey(),a.entityVersion());
        if(!hashes.isEmpty()) {same(hashes,codec.hash(a));return true;}
        one(jdbc.update("INSERT INTO "+AUTH+"(generation,tenant_id,source_id,user_id,task_key,policy_id,grant_key,entity_version,content_hash) VALUES(?,?,?,?,?,?,?,?,?)",
            generation,a.tenantId(),a.sourceId(),a.userId(),a.taskKey(),a.policyId(),a.grantKey(),a.entityVersion(),codec.hash(a)));return false;
    }
    private static void same(List<String> hashes,String hash) {if(hashes.size()!=1||!hash.equals(hashes.get(0)))throw new IllegalStateException("Permanent entity version conflict");}
    @Override public List<Long> controlTenants(int maximum) {
        transaction(false);List<Long> ids=jdbc.queryForList("SELECT tenant_id FROM system_workbench_tenant_control ORDER BY tenant_id LIMIT ? LOCK IN SHARE MODE",Long.class,Math.addExact(maximum,1));
        if(ids.isEmpty()||ids.size()>maximum)throw new IllegalStateException("Complete consumer inventory exceeds budget or is empty");
        List<Long> formal=jdbc.queryForList("SELECT id FROM system_tenant WHERE deleted=0 ORDER BY id LIMIT ? LOCK IN SHARE MODE",Long.class,Math.addExact(maximum,1));
        if(formal.size()>maximum||!new HashSet<>(ids).containsAll(formal))throw new IllegalStateException("Formal tenant lacks protocol control registration");return List.copyOf(ids);
    }
    private List<Consumer> consumers(Request request,Global global,List<Tenant> tenants,long globalFloor,long tenantFloor,boolean exclusive) {
        List<Consumer> result=new ArrayList<>();Set<Long> ids=new HashSet<>();tenants.forEach(t->ids.add(t.tenantId()));
        Map<Long,Tenant> owners=new HashMap<>();Map<Long,Long> floors=new HashMap<>();
        for(Tenant tenant:tenants) {owners.put(tenant.tenantId(),tenant);floors.put(tenant.tenantId(),request.scope()==Scope.GLOBAL?floor(Scope.TENANT,tenant.tenantId(),false):tenantFloor);}
        // An orphan from any tenant prevents global cleanup. Tenant operations restrict this check to their own scope.
        String tenantWhere=request.scope()==Scope.GLOBAL?"":" AND c.tenant_id=?";
        Object[] args=request.scope()==Scope.GLOBAL?new Object[]{request.budget().maxConsumers()+1}:new Object[]{request.tenant(),request.budget().maxConsumers()+1};
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT c.*,g.global_checkpoint FROM system_workbench_generation c LEFT JOIN system_workbench_generation_global g ON g.generation=c.generation LEFT JOIN system_workbench_generation_retirement r ON r.generation=c.generation AND r.tenant_id=c.tenant_id WHERE r.generation IS NULL"+tenantWhere+" ORDER BY c.tenant_id,c.generation LIMIT ?"+lock(exclusive),args);
        if(rows.size()>request.budget().maxConsumers())throw new IllegalStateException("Complete generation inventory exceeds budget");
        for(Map<String,Object> row:rows) {
            long tenant=number(row,"tenant_id");if(!ids.contains(tenant))throw new IllegalStateException("Generation lacks tenant control");
            Tenant t=owners.get(tenant);
            long gc=number(row,"global_checkpoint"),tg=number(row,"tenant_global_checkpoint"),tc=number(row,"tenant_checkpoint");
            long tf=floors.get(tenant);
            if(globalFloor>tg||tg>gc||gc>global.head()||tf>tc||tc>t.head())throw new IllegalStateException("Generation checkpoint is outside retained payload range");
            Object complete=row.get("complete");
            if(!(complete instanceof Boolean)&&(!(complete instanceof Number)||(((Number)complete).intValue()!=0&&((Number)complete).intValue()!=1)))throw new IllegalStateException("Invalid generation completeness fact");
            boolean completed=complete instanceof Boolean?(Boolean)complete:((Number)complete).intValue()==1;
            result.add(new Consumer(tenant,string(row,"generation"),gc,tg,tc,completed,t.activeGeneration(),t.head(),t.securityEpoch(),string(row,"registry_revision"),ProjectionCodec.hashText(string(row,"sources")),tf));
        }
        for(Tenant t:tenants) {
            List<Consumer> owned=result.stream().filter(c->c.tenant()==t.tenantId()).toList();
            if(owned.isEmpty()||(t.activeGeneration()!=null&&owned.stream().noneMatch(c->c.generation().equals(t.activeGeneration()))))
                throw new IllegalStateException("Control tenant lacks a retained generation or active registration");
        }
        for(String table:List.of("system_workbench_task_projection","system_workbench_task_audience","system_workbench_authorization_projection","system_workbench_consumed_event")) {
            List<Long> orphan=jdbc.queryForList("SELECT p.tenant_id FROM "+table+" p LEFT JOIN system_workbench_generation c ON c.generation=p.generation AND c.tenant_id=p.tenant_id WHERE c.generation IS NULL"+(request.scope()==Scope.GLOBAL?"":" AND p.tenant_id=?")+" LIMIT 1"+lock(exclusive),Long.class,request.scope()==Scope.GLOBAL?new Object[]{}:new Object[]{request.tenant()});
            if(!orphan.isEmpty())throw new IllegalStateException("Orphan projection payload prevents cleanup");
        }
        return List.copyOf(result);
    }
    @Override public Snapshot snapshot(Request request,String operation,Global global,List<Tenant> tenants,boolean exclusive) {
        transaction(false);long gf=floor(Scope.GLOBAL,0,exclusive&&request.scope()==Scope.GLOBAL),tf=request.scope()==Scope.GLOBAL?0:floor(Scope.TENANT,request.tenant(),exclusive);
        if(gf>global.head()||(request.scope()==Scope.TENANT&&tf>tenants.get(0).head()))throw new IllegalStateException("Scope floor exceeds committed head");
        List<Consumer> consumers=consumers(request,global,tenants,gf,tf,exclusive);
        if(request.target()==Target.OUTBOX)return outbox(request,operation,global,tenants,gf,tf,consumers,exclusive);
        if(consumers.stream().noneMatch(c->c.generation().equals(request.generation())))throw new IllegalStateException("Retained target generation registration missing");
        return version(request,operation,global,tenants,gf,tf,consumers,exclusive);
    }
    private Snapshot outbox(Request r,String operation,Global global,List<Tenant> tenants,long gf,long tf,List<Consumer> consumers,boolean exclusive) {
        if(r.sequence()==null)throw new IllegalArgumentException("Explicit batch sequence required");
        long floor=r.scope()==Scope.GLOBAL?gf:tf,next=r.sequence(),head=r.scope()==Scope.GLOBAL?global.head():tenants.get(0).head();
        if(floor>head||next<=floor||next>head)throw new IllegalStateException("Explicit batch is outside retained committed range");
        if("preview".equals(operation)&&next!=Math.addExact(floor,1))throw new IllegalStateException("Only the next contiguous batch can be purged");
        for(Consumer c:consumers)if("preview".equals(operation)&&next>(r.scope()==Scope.GLOBAL?Math.min(c.globalCheckpoint(),c.tenantGlobalCheckpoint()):c.tenantCheckpoint()))
            throw new IllegalStateException("A retained consumer has not consumed the next cleanup batch");
        List<Map<String,Object>> sizes=jdbc.queryForList("SELECT COUNT(*) event_count,COALESCE(SUM(OCTET_LENGTH(payload)),0) payload_bytes FROM system_workbench_outbox WHERE scope=? AND tenant_id=? AND commit_sequence=?",r.scope().name(),r.tenant(),next);
        if(sizes.size()!=1||number(sizes.get(0),"event_count")<1||number(sizes.get(0),"event_count")>r.budget().maxEvents())throw new IllegalStateException("Complete batch exceeds budget or is missing");
        r.budget().rows(number(sizes.get(0),"event_count"));r.budget().bytes(number(sizes.get(0),"payload_bytes"));
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM system_workbench_outbox WHERE scope=? AND tenant_id=? AND commit_sequence=? ORDER BY event_ordinal LIMIT ?"+lock(exclusive),r.scope().name(),r.tenant(),next,r.budget().maxEvents()+1);
        if(rows.size()>r.budget().maxEvents())throw new IllegalStateException("Complete batch exceeds event budget");
        List<Event> events=new ArrayList<>();List<Item> items=new ArrayList<>();long bytes=0;
        for(Map<String,Object> row:rows) {
            String payload=string(row,"payload"),hash=string(row,"content_hash");Event e=codec.decode(payload,Event.class);
            if(!codec.hash(e).equals(hash)||!e.eventId().equals(string(row,"event_id"))||!e.scope().name().equals(string(row,"scope"))||e.tenantId()!=number(row,"tenant_id")||e.sequence()!=number(row,"commit_sequence")
                ||!e.batchKey().equals(string(row,"batch_key"))||e.ordinal()!=number(row,"event_ordinal")||e.expectedCount()!=number(row,"expected_count")||e.schemaVersion()!=number(row,"schema_version"))throw new IllegalStateException("Outbox payload differs from persisted batch identity");
            events.add(e);long length=payload.getBytes(StandardCharsets.UTF_8).length;bytes=Math.addExact(bytes,length);
            items.add(new Item("OUTBOX",List.of(e.eventId(),e.scope().name(),e.tenantId(),e.sequence(),e.batchKey(),e.ordinal()),hash,rowHash(row),length,null,List.of()));
        }
        Batch batch=ProjectionBatchConsumer.complete(r.scope(),r.tenant(),next,events);r.budget().rows(rows.size());r.budget().bytes(bytes);
        List<Map<String,Object>> identities=batchIdentity(batch,exclusive);Observation observation=null;
        if(!identities.isEmpty()) {verifyBatch(batch);observation=observation(identities.get(0));}
        else if(!"observe".equals(operation)&&!"register".equals(operation))throw new IllegalStateException("Explicit historical batch registration required");
        return new Snapshot(global,tenants,gf,tf,consumers,next,batch.batchKey(),digest(batch),observation,items);
    }
    private static Observation observation(Map<String,Object> row) {
        if(row.get("observed_at_millis")==null) {
            for(String field:OBS.split(","))if(row.get(field)!=null)throw new IllegalStateException("Partial observation metadata");return null;
        }
        return new Observation(number(row,"observed_at_millis"),number(row,"purge_not_before_millis"),number(row,"retention_seconds"),number(row,"rollback_seconds"),number(row,"clock_skew_seconds"),string(row,"approval_reference"));
    }
    private String rowHash(Map<String,Object> row) {
        TreeMap<String,String> values=new TreeMap<>();row.forEach((k,v)->values.put(k,v==null?null:v instanceof byte[]?HexFormat.of().formatHex((byte[])v):v.toString()));return codec.hash(values);
    }
    private static String table(String kind) {return "TASK".equals(kind)?"system_workbench_task_projection":"system_workbench_authorization_projection";}
    private static String ledger(String kind) {return "TASK".equals(kind)?TASK:AUTH;}
    private static List<String> keyFields(String kind) {return "TASK".equals(kind)?List.of("task_key","entity_version"):List.of("source_id","user_id","task_key","policy_id","grant_key","entity_version");}
    private static String predicate(String kind,String prefix) {return String.join(" AND ",keyFields(kind).stream().map(k->prefix+k+"=?").toList());}
    private static List<Object> arguments(Request r,Item item) {List<Object> a=new ArrayList<>(List.of(r.generation(),r.tenant()));a.addAll(item.keys());return a;}
    private Snapshot version(Request r,String operation,Global global,List<Tenant> tenants,long gf,long tf,List<Consumer> consumers,boolean exclusive) {
        for(String kind:List.of("TASK","AUTH")) {
            String join="h.generation=p.generation AND h.tenant_id=p.tenant_id AND "+String.join(" AND ",keyFields(kind).stream().map(k->"h."+k+"=p."+k).toList());
            String filter="register".equals(operation)?" AND h.content_hash IS NULL":" AND p.version_to IS NOT NULL";
            List<Map<String,Object>> sizes=jdbc.queryForList("SELECT "+String.join(",",keyFields(kind).stream().map(k->"p."+k).toList())+",OCTET_LENGTH(p.payload) payload_bytes FROM "+table(kind)+" p LEFT JOIN "+ledger(kind)+" h ON "+join+" WHERE p.generation=? AND p.tenant_id=?"+filter+" ORDER BY "+String.join(",",keyFields(kind).stream().map(k->"p."+k).toList())+" LIMIT 1"+lock(exclusive),r.generation(),r.tenant());
            if(sizes.isEmpty())continue;
            r.budget().bytes(number(sizes.get(0),"payload_bytes"));
            List<Object> rowArgs=new ArrayList<>(List.of(r.generation(),r.tenant()));
            for(String field:keyFields(kind))rowArgs.add(sizes.get(0).get(field));
            List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM "+table(kind)+" WHERE generation=? AND tenant_id=? AND "+predicate(kind,"")+lock(exclusive),rowArgs.toArray());
            if(rows.size()!=1)throw new IllegalStateException("Selected version disappeared");
            Map<String,Object> row=rows.get(0);String payload=string(row,"payload"),hash=string(row,"content_hash");
            List<Object> keys=keyFields(kind).stream().map(row::get).toList();
            if(keys.stream().anyMatch(Objects::isNull)||!ProjectionCodec.hashText(payload).equals(hash))throw new IllegalStateException("Version payload or composite identity is corrupt");
            Long closed=row.get("version_to")==null?null:number(row,"version_to");long bytes=payload.getBytes(StandardCharsets.UTF_8).length;
            List<Long> members=new ArrayList<>();
            if("TASK".equals(kind)) {
                Task t=codec.decode(payload,Task.class);
                if(t.tenantId()!=r.tenant()||!t.taskKey().equals(keys.get(0))||t.entityVersion()!=((Number)keys.get(1)).longValue()||!t.sourceId().equals(string(row,"source_id")))throw new IllegalStateException("Task identity differs from payload");
                List<Long> audience=jdbc.queryForList("SELECT user_id FROM system_workbench_task_audience WHERE generation=? AND tenant_id=? AND task_key=? AND entity_version=? ORDER BY user_id LIMIT ?"+lock(exclusive),Long.class,r.generation(),r.tenant(),t.taskKey(),t.entityVersion(),r.budget().maxRows()+1);
                Set<Long> expected=t.tombstone()?Set.of():t.audience().members();
                if(audience.size()>r.budget().maxRows()||audience.size()!=new HashSet<>(audience).size()||!new HashSet<>(audience).equals(expected))throw new IllegalStateException("Complete task audience differs or exceeds budget");members.addAll(audience);
            } else {
                Authorization a=codec.decode(payload,Authorization.class);
                if(a.tenantId()!=r.tenant()||!List.of(a.sourceId(),a.userId(),a.taskKey(),a.policyId(),a.grantKey(),a.entityVersion()).equals(keys))throw new IllegalStateException("Authorization identity differs from payload");
            }
            Item item=new Item(kind,keys,hash,rowHash(row),bytes,closed,members);List<Object> args=arguments(r,item);
            List<Map<String,Object>> identity=jdbc.queryForList("SELECT * FROM "+ledger(kind)+" WHERE generation=? AND tenant_id=? AND "+predicate(kind,"")+lock(exclusive),args.toArray());
            Observation obs=null;
            if(!identity.isEmpty()) {
                if(identity.size()!=1||!hash.equals(string(identity.get(0),"content_hash")))throw new IllegalStateException("Permanent version hash differs");
                obs=observation(identity.get(0));
                if(obs==null&&identity.get(0).get("closed_version_to")!=null)throw new IllegalStateException("Partial closure observation metadata");
                if(obs!=null&&(!Objects.equals(closed,identity.get(0).get("closed_version_to"))))throw new IllegalStateException("Observed closure boundary changed");
            } else if("preview".equals(operation))throw new IllegalStateException("Explicit historical version registration required");
            if(!"register".equals(operation)) {
                if(closed==null)throw new IllegalStateException("Only closed versions can be observed or compressed");
                List<Object> base=args.subList(0,args.size()-1);
                String basePredicate=predicate(kind,"").substring(0,predicate(kind,"").lastIndexOf(" AND entity_version=?"));
                List<Long> current=jdbc.queryForList("SELECT entity_version FROM "+table(kind)+" WHERE generation=? AND tenant_id=? AND "+basePredicate+" AND version_to IS NULL"+lock(exclusive),Long.class,base.toArray());
                if(current.size()!=1||current.get(0)<closed||closed<=((Number)keys.get(keys.size()-1)).longValue())throw new IllegalStateException("Closed version has no valid unique current successor");
            }
            r.budget().rows(Math.addExact(1,members.size()));r.budget().bytes(bytes);
            return new Snapshot(global,tenants,gf,tf,consumers,0,null,hash,obs,List.of(item));
        }
        throw new IllegalStateException("No payload matches the explicit history operation");
    }
    @Override public void register(Request r,Snapshot snapshot) {
        transaction(true);
        if(r.target()==Target.OUTBOX) {registerBatch(r,snapshot);return;}
        Item item=snapshot.items().get(0);List<Object> args=arguments(r,item);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT payload FROM "+table(item.kind())+" WHERE generation=? AND tenant_id=? AND "+predicate(item.kind(),"")+" FOR UPDATE",args.toArray());
        if(rows.size()!=1)throw new IllegalStateException("Selected version registration row missing");Map<String,Object> row=rows.get(0);
        String payload=string(row,"payload");
        if(!ProjectionCodec.hashText(payload).equals(item.contentHash()))throw new IllegalStateException("Version payload changed before registration");
        if("TASK".equals(item.kind()))rememberTask(r.generation(),codec.decode(payload,Task.class));
        else rememberAuthorization(r.generation(),codec.decode(payload,Authorization.class));
    }
    private Batch registerBatch(Request r,Snapshot snapshot) {
        List<Event> events=jdbc.query("SELECT payload FROM system_workbench_outbox WHERE scope=? AND tenant_id=? AND commit_sequence=? ORDER BY event_ordinal FOR UPDATE",(row,n)->codec.decode(row.getString(1),Event.class),r.scope().name(),r.tenant(),snapshot.sequence());
        Batch batch=ProjectionBatchConsumer.complete(r.scope(),r.tenant(),snapshot.sequence(),events);
        if(!digest(batch).equals(snapshot.identityHash()))throw new IllegalStateException("Batch changed before registration");
        if(batchIdentity(batch,true).isEmpty())appendIdentity(batch);else verifyBatch(batch);return batch;
    }
    @Override public void observe(Request r,Snapshot snapshot,Observation obs) {
        transaction(true);
        if(snapshot.observation()!=null)return;
        String target,where;List<Object> key=new ArrayList<>();
        if(r.target()==Target.OUTBOX) {
            registerBatch(r,snapshot);
            target=BATCH;where="scope=? AND tenant_id=? AND commit_sequence=?";key.addAll(List.of(r.scope().name(),r.tenant(),snapshot.sequence()));
        } else {
            register(r,snapshot);Item item=snapshot.items().get(0);target=ledger(item.kind());where="generation=? AND tenant_id=? AND "+predicate(item.kind(),"");key.addAll(arguments(r,item));
        }
        List<Object> args=new ArrayList<>(List.of(obs.observedAtMillis(),obs.purgeNotBeforeMillis(),obs.retentionSeconds(),obs.rollbackSeconds(),obs.clockSkewSeconds(),obs.approvalReference()));
        String closure="";
        if(r.target()==Target.VERSION) {closure=",closed_version_to=?";args.add(snapshot.items().get(0).closedVersionTo());}
        args.addAll(key);
        one(jdbc.update("UPDATE "+target+" SET observed_at_millis=?,purge_not_before_millis=?,retention_seconds=?,rollback_seconds=?,clock_skew_seconds=?,approval_reference=?"+closure+" WHERE "+where+" AND observed_at_millis IS NULL",args.toArray()));
    }
    @Override public void purge(Request r,Snapshot snapshot) {
        transaction(true);
        if(r.target()==Target.OUTBOX) {
            for(Item item:snapshot.items())one(jdbc.update("DELETE FROM system_workbench_outbox WHERE event_id=? AND scope=? AND tenant_id=? AND commit_sequence=? AND batch_key=? AND event_ordinal=? AND content_hash=?",append(item.keys(),item.contentHash())));
            one(jdbc.update("UPDATE system_workbench_scope_floor SET payload_floor=? WHERE scope=? AND tenant_id=? AND payload_floor=?",snapshot.sequence(),r.scope().name(),r.tenant(),snapshot.sequence()-1));
        } else {
            Item item=snapshot.items().get(0);List<Object> args=arguments(r,item);
            if("TASK".equals(item.kind()))for(long user:item.members())one(jdbc.update("DELETE FROM system_workbench_task_audience WHERE generation=? AND tenant_id=? AND task_key=? AND entity_version=? AND user_id=?",append(args,user)));
            args.add(item.closedVersionTo());args.add(item.contentHash());
            one(jdbc.update("DELETE FROM "+table(item.kind())+" WHERE generation=? AND tenant_id=? AND "+predicate(item.kind(),"")+" AND version_to=? AND content_hash=?",args.toArray()));
        }
    }
    private static Object[] append(List<Object> original,Object last) {List<Object> values=new ArrayList<>(original);values.add(last);return values.toArray();}
}
