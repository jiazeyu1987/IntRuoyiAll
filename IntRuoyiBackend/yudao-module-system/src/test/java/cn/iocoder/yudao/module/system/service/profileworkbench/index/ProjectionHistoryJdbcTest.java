package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.sql.DataSource;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionHistory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionHistoryJdbcTest {
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final ProjectionRetentionJdbcTest.RecordingJdbc jdbc=new ProjectionRetentionJdbcTest.RecordingJdbc();
    final JdbcProjectionHistoryRepository repository=new JdbcProjectionHistoryRepository(mock(DataSource.class),mapper);
    final Request request=ProjectionHistoryTest.request(Target.OUTBOX,Scope.TENANT,2,null);
    final Global global=new Global(1,0,"r");final Tenant tenant=new Tenant(2,1,0,"active");
    final Task task=new Task(2,"SRC","SRC:1",1,true,"1",null,null,null,null,null,null,0,null,null,null);
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(repository,"jdbc",jdbc);TransactionSynchronizationManager.setActualTransactionActive(true);
        jdbc.hashes.put("system_workbench_scope_floor",(List)List.of(0L));
        jdbc.tables.put("system_workbench_generation",List.of(generation(1)));
        jdbc.tables.put("system_workbench_outbox",List.of(Map.of("event_count",1L,"payload_bytes",1L)));
    }
    Map<String,Object> generation(long cp) {return Map.of("tenant_id",2L,"generation","active","global_checkpoint",1L,"tenant_global_checkpoint",1L,"tenant_checkpoint",cp,"complete",true,"registry_revision","r","sources","[]");}
    @AfterEach void end() {TransactionSynchronizationManager.clear();}
    @Test void absentFloorNeverBecomesZeroAndInitializationIsExplicit() {
        jdbc.hashes.clear();assertThrows(IllegalStateException.class,()->repository.floor(Scope.TENANT,2,false));
        repository.initializeFloor(Scope.TENANT,2);
        assertEquals(List.of("TENANT",2L),jdbc.calls.get(jdbc.calls.size()-1).args());
        assertTrue(jdbc.calls.get(jdbc.calls.size()-1).sql().contains("VALUES(?,?,0)"));
    }
    @Test void disabledFormalTenantsRemainInGlobalInventoryAndUnregisteredTenantFails() {
        jdbc.hashes.put("system_workbench_tenant_control",(List)List.of(2L,3L));jdbc.hashes.put("system_tenant",(List)List.of(2L,3L));
        assertEquals(List.of(2L,3L),repository.controlTenants(100));
        assertTrue(jdbc.calls.get(1).sql().contains("WHERE deleted=0"));assertFalse(jdbc.calls.get(1).sql().contains("status"));
        jdbc.hashes.put("system_tenant",(List)List.of(2L,3L,4L));assertThrows(IllegalStateException.class,()->repository.controlTenants(100));
    }
    @Test void everyRetainedConsumerIncludingIncompleteRollbackCanBlockNextBatch() {
        var slow=new HashMap<>(generation(0));slow.put("complete",false);slow.put("generation","rollback");
        jdbc.tables.put("system_workbench_generation",List.of(generation(1),slow));
        assertThrows(IllegalStateException.class,()->repository.snapshot(request,"preview",global,List.of(tenant),true));
        assertTrue(jdbc.calls.stream().noneMatch(c->c.sql().contains("SELECT * FROM system_workbench_outbox")));
    }
    @Test void checkpointBelowFloorAndOrphanPayloadAreRejectedBeforeCleanup() {
        jdbc.hashes.put("system_workbench_scope_floor",(List)List.of(1L));jdbc.tables.put("system_workbench_generation",List.of(generation(0)));
        assertThrows(IllegalStateException.class,()->repository.snapshot(request,"preview",global,List.of(tenant),true));
        jdbc.hashes.put("system_workbench_scope_floor",(List)List.of(0L));jdbc.tables.put("system_workbench_generation",List.of(generation(1)));
        jdbc.hashes.put("system_workbench_task_projection",(List)List.of(2L));
        assertThrows(IllegalStateException.class,()->repository.snapshot(request,"preview",global,List.of(tenant),true));
        assertTrue(jdbc.calls.stream().noneMatch(c->c.sql().startsWith("DELETE")));
    }
    @Test void overBudgetPayloadIsRejectedBeforeLoadingLargeText() {
        jdbc.tables.put("system_workbench_outbox",List.of(Map.of("event_count",1L,"payload_bytes",100001L)));
        assertThrows(IllegalStateException.class,()->repository.snapshot(request,"preview",global,List.of(tenant),true));
        assertTrue(jdbc.calls.stream().noneMatch(c->c.sql().startsWith("SELECT * FROM system_workbench_outbox")));
    }
    @Test void eventIdentityIsWrittenBeforePayloadAndCannotReusePurgedSequence() {
        Event event=new Event("e",Scope.TENANT,2,1,"batch",0,1,4,EventKind.CONTROL,null,null,"r");Batch batch=new Batch(Scope.TENANT,2,1,"batch",List.of(event));
        repository.appendIdentity(batch);
        assertEquals(3,jdbc.calls.size());assertTrue(jdbc.calls.get(1).sql().startsWith("INSERT INTO system_workbench_batch_identity"));
        assertTrue(jdbc.calls.get(2).sql().startsWith("INSERT INTO system_workbench_event_identity"));
        jdbc.calls.clear();jdbc.hashes.put("system_workbench_scope_floor",(List)List.of(1L));
        assertThrows(IllegalStateException.class,()->repository.appendIdentity(batch));assertEquals(1,jdbc.calls.size());
    }
    @Test void permanentVersionRejectsConflictAfterPayloadWasCompressed() {
        String hash=new ProjectionCodec(mapper).hash(task);jdbc.hashes.put("system_workbench_task_version_identity",List.of(hash));
        assertTrue(repository.rememberTask("active",task));
        jdbc.hashes.put("system_workbench_task_version_identity",List.of(ProjectionCodec.hashText("other")));
        assertThrows(IllegalStateException.class,()->repository.rememberTask("active",task));
        assertTrue(jdbc.calls.stream().noneMatch(c->c.sql().startsWith("INSERT")));
    }
    @Test void obsoleteUnseenVersionIsRecordedBeforeIgnoringAndSeenConflictStillFails() {
        var p=spy(new JdbcProjectionRepository(mock(DataSource.class),mapper));ReflectionTestUtils.setField(p,"jdbc",jdbc);ReflectionTestUtils.setField(p,"history",repository);
        doReturn(global).when(p).lockGlobal(false);doReturn(tenant).when(p).lockTenant(2,false);
        jdbc.tables.put("system_workbench_task_projection",List.of(Map.of("entity_version",2L,"content_hash",ProjectionCodec.hashText("new"))));
        p.applyTask("active",task);assertTrue(jdbc.calls.stream().anyMatch(c->c.sql().startsWith("INSERT INTO system_workbench_task_version_identity")));
        assertTrue(jdbc.calls.stream().noneMatch(c->c.sql().startsWith("UPDATE system_workbench_task_projection")));
        jdbc.hashes.put("system_workbench_task_version_identity",List.of(ProjectionCodec.hashText("conflict")));
        assertThrows(IllegalStateException.class,()->p.applyTask("active",task));
    }
    Snapshot snapshot(List<Item> items,long seq) {return new Snapshot(global,List.of(tenant),0,0,List.of(),seq,"batch",ProjectionCodec.hashText("batch"),ProjectionHistory.observed(request,1000),items);}
    @Test void completeBatchDeletesExactKeysThenAdvancesContiguousFloorAndKeepsLedger() {
        Item item=new Item("OUTBOX",List.of("e","TENANT",2L,1L,"batch",0),ProjectionCodec.hashText("event"),ProjectionCodec.hashText("row"),1,null,List.of());
        repository.purge(request,snapshot(List.of(item),1));assertEquals(2,jdbc.calls.size());
        assertEquals(List.of("e","TENANT",2L,1L,"batch",0,item.contentHash()),jdbc.calls.get(0).args());
        assertEquals(List.of(1L,"TENANT",2L,0L),jdbc.calls.get(1).args());
        assertTrue(jdbc.calls.get(1).sql().contains("AND payload_floor=?"));assertTrue(jdbc.calls.stream().noneMatch(c->c.sql().startsWith("DELETE FROM system_workbench_event_identity")));
    }
    @Test void closedTaskAudienceAndPayloadAreDeletedTogetherCurrentAndHiddenUntouched() {
        Request r=ProjectionHistoryTest.request(Target.VERSION,Scope.TENANT,2,"active");
        Item item=new Item("TASK",List.of("SRC:1",1L),ProjectionCodec.hashText("task"),ProjectionCodec.hashText("row"),1,2L,List.of(8L,9L));
        repository.purge(r,snapshot(List.of(item),0));assertEquals(3,jdbc.calls.size());
        assertEquals(List.of("active",2L,"SRC:1",1L,8L),jdbc.calls.get(0).args());
        assertEquals(List.of("active",2L,"SRC:1",1L,2L,item.contentHash()),jdbc.calls.get(2).args());
        assertTrue(jdbc.calls.get(2).sql().contains("AND version_to=? AND content_hash=?"));
        for(var c:jdbc.calls)assertFalse(c.sql().contains("visibility")||c.sql().contains("consumed_event")||c.sql().contains("version_identity"));
    }
    @Test void failedAudienceDeleteStopsBeforePayloadAndReadOnlyStopsBeforeAnySql() {
        Request r=ProjectionHistoryTest.request(Target.VERSION,Scope.TENANT,2,"active");
        Item item=new Item("TASK",List.of("SRC:1",1L),ProjectionCodec.hashText("task"),ProjectionCodec.hashText("row"),1,2L,List.of(8L));
        jdbc.rowCount=0;assertThrows(IllegalStateException.class,()->repository.purge(r,snapshot(List.of(item),0)));assertEquals(1,jdbc.calls.size());
        jdbc.calls.clear();TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        assertThrows(IllegalStateException.class,()->repository.purge(r,snapshot(List.of(item),0)));assertTrue(jdbc.calls.isEmpty());
    }
    static class VersionJdbc extends ProjectionRetentionJdbcTest.RecordingJdbc {
        List<Long> current=List.of(2L);List<Long> members=List.of();
        @Override @SuppressWarnings("unchecked") public <T> List<T> queryForList(String sql,Class<T> type,Object...args) {
            if(sql.startsWith("SELECT p.tenant_id")) {calls.add(new ProjectionRetentionJdbcTest.Call(sql,Arrays.asList(args)));return List.of();}
            if(sql.startsWith("SELECT entity_version")) {calls.add(new ProjectionRetentionJdbcTest.Call(sql,Arrays.asList(args)));return (List<T>)current;}
            if(sql.startsWith("SELECT user_id")) {calls.add(new ProjectionRetentionJdbcTest.Call(sql,Arrays.asList(args)));return (List<T>)members;}
            return super.queryForList(sql,type,args);
        }
    }
    Map<String,Object> observedIdentity(String hash) {
        Observation o=ProjectionHistory.observed(request,1000);var row=new HashMap<String,Object>();
        row.put("content_hash",hash);row.put("closed_version_to",2L);row.put("observed_at_millis",o.observedAtMillis());row.put("purge_not_before_millis",o.purgeNotBeforeMillis());
        row.put("retention_seconds",o.retentionSeconds());row.put("rollback_seconds",o.rollbackSeconds());row.put("clock_skew_seconds",o.clockSkewSeconds());row.put("approval_reference",o.approvalReference());return row;
    }
    VersionJdbc versionJdbc(String kind,Map<String,Object> row) {
        var v=new VersionJdbc();v.hashes.put("system_workbench_scope_floor",(List)List.of(0L));v.tables.put("system_workbench_generation",List.of(generation(1)));
        v.tables.put("TASK".equals(kind)?"system_workbench_task_projection":"system_workbench_authorization_projection",List.of(row));
        v.tables.put("TASK".equals(kind)?"system_workbench_task_version_identity":"system_workbench_auth_version_identity",List.of(observedIdentity((String)row.get("content_hash"))));
        ReflectionTestUtils.setField(repository,"jdbc",v);return v;
    }
    @Test void actualVersionSelectionRejectsChangedClosureMissingSuccessorAndCurrentVersion() {
        String payload=new ProjectionCodec(mapper).encode(task);var row=new HashMap<String,Object>();
        row.put("tenant_id",2L);row.put("source_id","SRC");row.put("task_key","SRC:1");row.put("entity_version",1L);row.put("version_to",2L);row.put("payload",payload);row.put("content_hash",ProjectionCodec.hashText(payload));row.put("payload_bytes",(long)payload.length());
        var v=versionJdbc("TASK",row);Request r=ProjectionHistoryTest.request(Target.VERSION,Scope.TENANT,2,"active");
        Snapshot result=repository.snapshot(r,"preview",global,List.of(tenant),true);assertEquals(List.of("SRC:1",1L),result.items().get(0).keys());
        assertEquals(2L,result.items().get(0).closedVersionTo());assertTrue(v.calls.stream().anyMatch(c->c.sql().contains("p.version_to IS NOT NULL")));
        v.current=List.of();assertThrows(IllegalStateException.class,()->repository.snapshot(r,"preview",global,List.of(tenant),true));
        v.current=List.of(2L);row.put("version_to",3L);assertThrows(IllegalStateException.class,()->repository.snapshot(r,"preview",global,List.of(tenant),true));
        row.put("version_to",null);assertThrows(IllegalStateException.class,()->repository.snapshot(r,"observe",global,List.of(tenant),true));
    }
    @Test void authorizationSelectionPreservesAllEightKeysAndValidCurrentRevocation() {
        Authorization a=new Authorization(2,"SRC",8,"","source:SRC",1,false,java.time.Instant.EPOCH,null);String payload=new ProjectionCodec(mapper).encode(a);
        var row=new HashMap<String,Object>();row.put("tenant_id",2L);row.put("source_id",a.sourceId());row.put("user_id",a.userId());row.put("task_key",a.taskKey());row.put("policy_id",a.policyId());row.put("grant_key",a.grantKey());row.put("entity_version",1L);row.put("version_to",2L);row.put("payload",payload);row.put("content_hash",ProjectionCodec.hashText(payload));row.put("payload_bytes",(long)payload.length());
        var v=versionJdbc("AUTH",row);Request r=ProjectionHistoryTest.request(Target.VERSION,Scope.TENANT,2,"active");
        Snapshot snapshot=repository.snapshot(r,"preview",global,List.of(tenant),true);repository.purge(r,snapshot);
        var call=v.calls.get(v.calls.size()-1);assertEquals(List.of("active",2L,"SRC",8L,"","source:SRC",a.grantKey(),1L,2L,ProjectionCodec.hashText(payload)),call.args());
        assertTrue(call.sql().contains("policy_id=? AND grant_key=? AND entity_version=? AND version_to=?"));
    }
    @Test void repeatedObservationDoesNotChangeOriginalClosureOrSkipToNextVersion() {
        Observation obs=ProjectionHistory.observed(request,1000);Snapshot s=snapshot(List.of(),1);
        repository.observe(request,s,obs);assertTrue(jdbc.calls.isEmpty());
    }
    class OutboxJdbc extends VersionJdbc {
        List<Event> events;boolean registered;String ledgerHash;
        OutboxJdbc(List<Event> events) {this.events=events;hashes.put("system_workbench_scope_floor",(List)List.of(0L));tables.put("system_workbench_generation",List.of(generation(1)));}
        @Override public List<Map<String,Object>> queryForList(String sql,Object...args) {
            if(sql.startsWith("SELECT COUNT(*) event_count")) {calls.add(new ProjectionRetentionJdbcTest.Call(sql,Arrays.asList(args)));return List.of(Map.of("event_count",(long)events.size(),"payload_bytes",10000L));}
            if(sql.startsWith("SELECT * FROM system_workbench_outbox")) {
                calls.add(new ProjectionRetentionJdbcTest.Call(sql,Arrays.asList(args)));
                return events.stream().map(e->{var row=new HashMap<String,Object>();row.put("event_id",e.eventId());row.put("scope",e.scope().name());row.put("tenant_id",e.tenantId());row.put("commit_sequence",e.sequence());row.put("batch_key",e.batchKey());row.put("event_ordinal",e.ordinal());row.put("expected_count",e.expectedCount());row.put("schema_version",e.schemaVersion());row.put("payload",new ProjectionCodec(mapper).encode(e));row.put("content_hash",new ProjectionCodec(mapper).hash(e));return (Map<String,Object>)row;}).toList();
            }
            if(sql.startsWith("SELECT * FROM system_workbench_batch_identity")) {
                calls.add(new ProjectionRetentionJdbcTest.Call(sql,Arrays.asList(args)));if(!registered)return List.of();
                Batch b=new Batch(Scope.TENANT,2,events.get(0).sequence(),"batch",events);
                String digest=ReflectionTestUtils.invokeMethod(repository,"digest",b);
                return List.of(Map.of("batch_key","batch","expected_count",events.size(),"schema_version",4,"batch_hash",digest));
            }
            if(sql.startsWith("SELECT * FROM system_workbench_event_identity")) {
                calls.add(new ProjectionRetentionJdbcTest.Call(sql,Arrays.asList(args)));
                return events.stream().map(e->Map.<String,Object>of("event_id",e.eventId(),"scope",e.scope().name(),"tenant_id",e.tenantId(),"commit_sequence",e.sequence(),"batch_key",e.batchKey(),"event_ordinal",e.ordinal(),"expected_count",e.expectedCount(),"schema_version",e.schemaVersion(),"content_hash",ledgerHash==null?new ProjectionCodec(mapper).hash(e):ledgerHash)).toList();
            }
            return super.queryForList(sql,args);
        }
    }
    Event event(int ordinal,int count,long seq) {return new Event("e"+ordinal,Scope.TENANT,2,seq,"batch",ordinal,count,4,EventKind.CONTROL,null,null,"r");}
    @Test void historicalUnconsumedBatchCanRegisterBeforeConsumptionButCannotPurge() {
        var v=new OutboxJdbc(List.of(event(0,1,1)));v.tables.put("system_workbench_generation",List.of(generation(0)));ReflectionTestUtils.setField(repository,"jdbc",v);
        Snapshot s=repository.snapshot(request,"register",global,List.of(tenant),true);assertEquals(1,s.sequence());assertNull(s.observation());
        assertThrows(IllegalStateException.class,()->repository.snapshot(request,"preview",global,List.of(tenant),true));
    }
    @Test void explicitHistoricalLaterBatchCanObserveButCannotCreateHoleInPurgedPrefix() {
        var v=new OutboxJdbc(List.of(event(0,1,2)));ReflectionTestUtils.setField(repository,"jdbc",v);
        Request r=new Request(Target.OUTBOX,Scope.TENANT,2,null,2L,60L,90L,5L,"history cleanup","approval",request.budget());
        assertEquals(2,repository.snapshot(r,"observe",global,List.of(new Tenant(2,2,0,"active")),true).sequence());
        assertThrows(IllegalStateException.class,()->repository.snapshot(r,"preview",global,List.of(new Tenant(2,2,0,"active")),true));
    }
    @Test void incompleteBatchAndTamperedPermanentEventFailWithoutDeletion() {
        var v=new OutboxJdbc(List.of(event(0,2,1)));ReflectionTestUtils.setField(repository,"jdbc",v);
        assertThrows(RuntimeException.class,()->repository.snapshot(request,"observe",global,List.of(tenant),true));
        v.events=List.of(event(0,1,1));v.registered=true;v.ledgerHash=ProjectionCodec.hashText("different");
        assertThrows(IllegalStateException.class,()->repository.snapshot(request,"observe",global,List.of(tenant),true));
        assertTrue(v.calls.stream().noneMatch(c->c.sql().startsWith("DELETE")));
    }
}
