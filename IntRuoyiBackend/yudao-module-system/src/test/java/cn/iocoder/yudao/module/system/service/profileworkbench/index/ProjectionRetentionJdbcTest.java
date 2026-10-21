package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRetention.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionRetentionJdbcTest {
    record Call(String sql,List<Object> args) { }
    static class RecordingJdbc extends JdbcTemplate {
        final List<Call> calls=new ArrayList<>();
        final Map<String,List<Map<String,Object>>> tables=new HashMap<>();
        final Map<String,List<String>> hashes=new HashMap<>();
        boolean retired;Long retiredTenant;int rowCount=1;
        @Override public List<Map<String,Object>> queryForList(String sql,Object...args) {
            calls.add(new Call(sql,Arrays.asList(args)));
            return tables.entrySet().stream().filter(e->sql.contains("FROM "+e.getKey()+" ")).findFirst().map(Map.Entry::getValue).orElse(List.of());
        }
        @Override @SuppressWarnings("unchecked") public <T> List<T> queryForList(String sql,Class<T> type,Object...args) {
            calls.add(new Call(sql,Arrays.asList(args)));
            if(sql.contains("system_workbench_generation_retirement"))return (retired||Objects.equals(retiredTenant,args[1]))?(List<T>)List.of("RETIRED"):List.of();
            return (List<T>)hashes.entrySet().stream().filter(e->sql.contains("FROM "+e.getKey()+" ")).findFirst().map(Map.Entry::getValue).orElse(List.of());
        }
        @Override public int update(String sql,Object...args) {
            calls.add(new Call(sql,Arrays.asList(args)));return rowCount;
        }
    }
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final RecordingJdbc jdbc=new RecordingJdbc();
    JdbcProjectionRetentionRepository retention() {
        var r=new JdbcProjectionRetentionRepository(mock(DataSource.class),mapper);ReflectionTestUtils.setField(r,"jdbc",jdbc);return r;
    }
    JdbcProjectionRepository projection() {
        var r=spy(new JdbcProjectionRepository(mock(DataSource.class),mapper));ReflectionTestUtils.setField(r,"jdbc",jdbc);
        ReflectionTestUtils.setField(r,"history",mock(ProjectionHistoryRepository.class));
        doReturn(new Global(0,0,"r")).when(r).lockGlobal(false);
        doReturn(new Tenant(2,0,0,"active")).when(r).lockTenant(2,false);
        return r;
    }
    @BeforeEach void begin() { TransactionSynchronizationManager.setActualTransactionActive(true); }
    @AfterEach void end() { TransactionSynchronizationManager.clear(); }
    @Test void boundedFirstNonemptyTableUsesExactTenantGenerationAndAllVersionKeys() {
        var data=Map.<String,Object>of("generation","old","tenant_id",2L,"task_key","key","entity_version",4L,"user_id",8L);
        jdbc.tables.put("system_workbench_task_audience",List.of(data));
        var selection=retention().nextBatch(2,"old",1000,true);
        assertEquals(Stage.AUDIENCE,selection.stage());assertEquals(List.of("key",4L,8L),selection.rows().get(0).keys());
        assertEquals(1,jdbc.calls.size());assertEquals(List.of("old",2L,1000),jdbc.calls.get(0).args());
        assertTrue(jdbc.calls.get(0).sql().endsWith("LIMIT ? FOR UPDATE"));
        jdbc.calls.clear();jdbc.tables.clear();
        String payload="{\"event\":1}",hash=ProjectionCodec.hashText(payload);
        jdbc.tables.put("system_workbench_task_projection",List.of(Map.of("generation","old","tenant_id",2L,"task_key","key","entity_version",4L,"payload",payload,"content_hash",hash)));
        assertEquals(Stage.TASK,retention().nextBatch(2,"old",1,false).stage());
        assertEquals(2,jdbc.calls.size());
        jdbc.tables.clear();jdbc.calls.clear();
        assertEquals(Stage.METADATA,retention().nextBatch(2,"old",1,true).stage());
        assertEquals(4,jdbc.calls.size());for(Call call:jdbc.calls) {
            assertTrue(call.sql().contains("WHERE generation=? AND tenant_id=?"));
            assertFalse(call.sql().contains("version_to IS NULL"));assertEquals(List.of("old",2L,1),call.args());
        }
    }
    @Test void tamperedPayloadIsRejectedAndRowContentChangesInvalidatePlanDigest() {
        String payload="one",hash=ProjectionCodec.hashText(payload);
        var original=Map.<String,Object>of("task_key","key","entity_version",4L,"payload",payload,"content_hash",hash,"version_to",5L);
        jdbc.tables.put("system_workbench_task_projection",List.of(original));
        String digest=retention().nextBatch(2,"old",1,false).rows().get(0).rowHash();
        var changed=new HashMap<>(original);changed.put("version_to",6L);
        jdbc.tables.put("system_workbench_task_projection",List.of(changed));
        assertNotEquals(digest,retention().nextBatch(2,"old",1,false).rows().get(0).rowHash());
        changed.put("payload","different");
        assertThrows(IllegalStateException.class,()->retention().nextBatch(2,"old",1,false));
    }
    @Test void exactCompositeDeletionNeverTargetsAnotherTenantOrBroadGeneration() {
        var row=new Row(List.of("SRC",8L,"task","policy","grant",4L),ProjectionCodec.hashText("row"),ProjectionCodec.hashText("payload"));
        retention().deleteRows(2,"old",new Selection(Stage.AUTHORIZATION,List.of(row)));
        assertEquals(1,jdbc.calls.size());Call call=jdbc.calls.get(0);
        assertEquals("DELETE FROM system_workbench_authorization_projection WHERE generation=? AND tenant_id=? AND source_id=? AND user_id=? AND task_key=? AND policy_id=? AND grant_key=? AND entity_version=? AND content_hash=?",call.sql());
        assertEquals(List.of("old",2L,"SRC",8L,"task","policy","grant",4L,row.contentHash()),call.args());
        jdbc.rowCount=0;assertThrows(IllegalStateException.class,()->retention().deleteRows(2,"old",new Selection(Stage.AUTHORIZATION,List.of(row))));
    }
    @Test void finalizationChecksFourEmptyTablesThenPreservesRetirementAndSharedIdentity() {
        Request request=new Request(2,"old",60L,90L,"reason","approval");
        retirementFinish(new Retirement(request,State.RETIRED,1000,391000));
        assertEquals(6,jdbc.calls.size());
        assertEquals("DELETE FROM system_workbench_generation WHERE generation=? AND tenant_id=?",jdbc.calls.get(4).sql());
        assertEquals(List.of("old",2L),jdbc.calls.get(4).args());
        assertTrue(jdbc.calls.get(5).sql().startsWith("UPDATE system_workbench_generation_retirement SET state='PURGED'"));
        for(Call call:jdbc.calls) {
            assertFalse(call.sql().contains("system_workbench_generation_global"));
            assertFalse(call.sql().contains("system_workbench_outbox"));
        }
        jdbc.calls.clear();jdbc.tables.put("system_workbench_consumed_event",List.of(Map.of("event_id","event","content_hash",ProjectionCodec.hashText("event"))));
        assertThrows(IllegalStateException.class,()->retirementFinish(new Retirement(request,State.RETIRED,1000,391000)));
        assertTrue(jdbc.calls.stream().noneMatch(c->c.sql().startsWith("DELETE")||c.sql().startsWith("UPDATE")));
    }
    void retirementFinish(Retirement retirement) { retention().finish(retirement); }
    @Test void readonlyAndAbsentTransactionsNeverDelete() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        assertThrows(IllegalStateException.class,()->retention().finish(new Retirement(new Request(2,"old",1L,1L,"reason","a"),State.RETIRED,0,301000)));
        TransactionSynchronizationManager.clear();
        assertThrows(IllegalStateException.class,()->retention().nextBatch(2,"old",1,false));assertTrue(jdbc.calls.isEmpty());
    }
    @Test void everyProjectionReadWriteAndReplayPortRefusesRetiredGenerationBeforeDataAccess() {
        jdbc.retired=true;var r=projection();Checkpoint c=new Checkpoint("old",2,0,0,0,false);
        Task task=new Task(2,"SRC","SRC:1",1,true,"1",null,null,null,null,null,null,0,null,null,null);
        Authorization authorization=new Authorization(2,"SRC",8,"","source:SRC",1,false,Instant.EPOCH,null);
        Event event=new Event("e",Scope.TENANT,2,1,"batch",0,1,4,EventKind.CONTROL,null,null,"r");
        var q=new ProjectionRepository.Query("old",2,8,List.of(),Instant.EPOCH,null);
        List<Runnable> uses=List.of(
            ()->r.checkpoint("old",2),()->r.createGeneration("old",2,new Watermark(0,0),"r",Set.of()),
            ()->r.advanceCheckpoint(c,c),()->r.applyTask("old",task),()->r.applyAuthorization("old",authorization),
            ()->r.recordEvent("old",2,event),()->r.setComplete(c),()->r.publish(new Tenant(2,0,0,"active"),"old"),
            ()->r.generationSources("old",2,"r"),()->r.allTasks("old",2),()->r.allAuthorizations("old",2),
            ()->r.authorizedSources("old",2,8,Instant.EPOCH),()->r.nextAuthorizationBoundary("old",2,8,List.of(),Instant.EPOCH),
            ()->r.totals(q),()->r.page(q,0,1),()->r.eligibleTask(q,"key"));
        for(Runnable use:uses)assertThrows(IllegalStateException.class,use::run);
        assertEquals(uses.size(),jdbc.calls.size());
        for(Call call:jdbc.calls) {
            assertEquals(List.of("old",2L),call.args());
            assertTrue(call.sql().contains("generation_retirement"));assertTrue(call.sql().endsWith("LOCK IN SHARE MODE"));
        }
    }
    @Test void switchingAtoBtoAChangesEpochAndOldATokenCannotResurrect() {
        var r=projection();
        doAnswer(i->new Checkpoint(i.getArgument(0),i.getArgument(1),0,0,0,true)).when(r).checkpoint(anyString(),anyLong());
        var a=new Tenant(2,0,5,"A");r.publish(a,"B");
        Call first=jdbc.calls.get(jdbc.calls.size()-1);assertEquals(List.of("B",6L,2L,0L,5L,"A"),first.args());
        var b=new Tenant(2,0,6,"B");r.publish(b,"A");
        Call second=jdbc.calls.get(jdbc.calls.size()-1);assertEquals(List.of("A",7L,2L,0L,6L,"B"),second.args());
        var back=new Tenant(2,0,7,"A");r.publish(back,"A");
        assertEquals(List.of("A",7L,2L,0L,7L,"A"),jdbc.calls.get(jdbc.calls.size()-1).args());
        var version=new ProjectionFence.Version("A","r",List.of(),0,0,0,5,1,Instant.EPOCH,null,"hash");
        assertThrows(RuntimeException.class,()->ProjectionFence.current(version,new Global(0,0,"r"),back,1,"r",List.of(),"hash",Instant.EPOCH));
        int before=jdbc.calls.size();
        assertThrows(ArithmeticException.class,()->r.publish(new Tenant(2,0,Long.MAX_VALUE,"A"),"B"));
        assertEquals(before+1,jdbc.calls.size()); // Only the retirement read ran, no pointer UPDATE.
    }
    @Test void retainedGenerationStillRejectsConflictingEntityVersionAndEventIdentity() {
        var r=projection();Task task=new Task(2,"SRC","SRC:1",1,true,"1",null,null,null,null,null,null,0,null,null,null);
        jdbc.tables.put("system_workbench_task_projection",List.of(Map.of("entity_version",1L,"content_hash",ProjectionCodec.hashText("different"))));
        assertThrows(RuntimeException.class,()->r.applyTask("old",task));
        Authorization authorization=new Authorization(2,"SRC",8,"","source:SRC",1,false,Instant.EPOCH,null);
        jdbc.tables.put("system_workbench_authorization_projection",List.of(Map.of("entity_version",1L,"content_hash",ProjectionCodec.hashText("different"))));
        assertThrows(RuntimeException.class,()->r.applyAuthorization("old",authorization));
        Event event=new Event("e",Scope.TENANT,2,1,"batch",0,1,4,EventKind.CONTROL,null,null,"r");
        jdbc.hashes.put("system_workbench_consumed_event",List.of(ProjectionCodec.hashText("different")));
        assertThrows(RuntimeException.class,()->r.recordEvent("old",2,event));
        assertTrue(jdbc.calls.stream().noneMatch(c->c.sql().startsWith("INSERT")||c.sql().startsWith("UPDATE")));
    }
    @Test void retirementAndCleanupScopeDoNotDisableAnotherTenantWithTheSameGeneration() {
        var r=projection();jdbc.retiredTenant=2L;
        doReturn(new Tenant(3,0,0,"old")).when(r).lockTenant(3,false);
        assertThrows(IllegalStateException.class,()->r.requireRetained("old",2));
        assertDoesNotThrow(()->r.requireRetained("old",3));
        assertEquals(List.of("old",2L),jdbc.calls.get(0).args());assertEquals(List.of("old",3L),jdbc.calls.get(1).args());
        jdbc.calls.clear();
        Row row=new Row(List.of("event"),ProjectionCodec.hashText("row"),ProjectionCodec.hashText("event"));
        retention().deleteRows(2,"old",new Selection(Stage.EVENT,List.of(row)));
        assertEquals(List.of("old",2L,"event",row.contentHash()),jdbc.calls.get(0).args());
        assertFalse(jdbc.calls.get(0).args().contains(3L));
    }

}
