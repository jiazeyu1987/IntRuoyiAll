package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccProjectReferenceTransactionTest {
    JdbcTemplate jdbc; TransactionTemplate tx; DccProjectReferenceService service;
    DccProjectReferenceAuthority authority; DccRelationAccessPolicy access;
    DccLatestControlledFileResolver files;
    cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService audit;
    @BeforeEach void setup() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:"+ UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        jdbc = new JdbcTemplate(ds); tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE dcc_controlled_file_master (id BIGINT, tenant_id BIGINT, deleted INT)");
        jdbc.update("INSERT INTO dcc_controlled_file_master VALUES (10,1,0)");
        jdbc.update("INSERT INTO dcc_controlled_file_master VALUES (20,1,0)");
        jdbc.execute("CREATE TABLE dcc_project_code(id BIGINT,tenant_id BIGINT,deleted INT)");
        jdbc.execute("CREATE TABLE dcc_project_folder(id BIGINT,project_code_id BIGINT,tenant_id BIGINT,deleted INT)");
        jdbc.update("INSERT INTO dcc_project_code VALUES(2,1,0),(3,1,0)");
        jdbc.update("INSERT INTO dcc_project_folder VALUES(21,2,1,0),(22,2,1,0),(31,3,1,0)");
        for (String sql : Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$", "").split(";")) {
            String clean = sql.replaceAll("(?m)^--.*$", "").trim();
            if (!clean.isEmpty()) jdbc.execute(clean);
        }
        authority=mock(DccProjectReferenceAuthority.class); access=mock(DccRelationAccessPolicy.class);
        files=mock(DccLatestControlledFileResolver.class);
        when(files.resolveSelectedForUpdate(any())).thenAnswer(call->files.resolveSelected((Long)call.getArgument(0)));
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N","源.pdf","A/1","ACTIVE",true,false,true));
        audit=mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
        service = new DccProjectReferenceService(new DccRelationStore(jdbc, audit),authority,access,files);
        TenantContextHolder.setTenantId(1L);
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }
    ReferenceView create(long project,long folder) { return tx.execute(s -> service.create(7L,project,folder,100L,"测试引用原因")); }
    long cancel(long project,long folder,boolean confirmed) { return tx.execute(s -> service.cancel(7L,project,folder,10L,jdbc.queryForObject("SELECT id FROM dcc_project_file_reference WHERE project_id=? AND folder_id=?",Long.class,project,folder),confirmed,"测试取消原因")); }
    @Test void countsDistinctProjectsAndLastCancellationRestoresSourceColorWithoutDeletingOtherRelations() {
        jdbc.update("INSERT INTO dcc_current_file_relation VALUES (1,20,10)");
        assertEquals(1,create(2,21).referenceProjectCount()); assertEquals(1,create(2,22).referenceProjectCount());
        assertEquals(2,create(3,31).referenceProjectCount());
        assertEquals(2,cancel(2,21,true)); assertEquals(1,cancel(2,22,true)); assertEquals(0,cancel(3,31,true));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_master WHERE id=10",Integer.class));
    }
    @Test void leaderGuardRejectsMemberOtherLeaderAndNonLeaderAdminOnBothCommands() {
        for (long actor: new long[]{8,9,1}) {
            doThrow(new IllegalStateException("NOT_PROJECT_LEADER")).when(authority).assertProjectLeader(actor,2L);
            assertThrows(IllegalStateException.class,() -> tx.execute(s -> service.create(actor,2L,21L,100L,"测试引用原因")));
            assertThrows(IllegalStateException.class,() -> tx.execute(s -> service.cancel(actor,2L,21L,10L,1L,true,"测试取消原因")));
        }
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
    }
    @Test void duplicateFolderIsIdempotentAndPreservesSelectedVersionAfterLatestChanges() {
        create(2,21); create(2,21);
        when(files.resolveLatest(10L)).thenReturn(new FileVersion(1L,101L,10L,9L,"N","源.pdf","B/1","CONTROLLED",true,true,false));
        assertEquals(100L,service.list(7L,2L,21L).get(0).selectedVersion().controlledFileId());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
        verify(files,never()).resolveLatest(any());
    }
    @Test void cancelledConfirmationAndInvalidFolderDoNotMutate() {
        create(2,21); assertThrows(IllegalArgumentException.class,()->cancel(2,21,false));
        doThrow(new IllegalStateException("FOLDER_PROJECT_MISMATCH")).when(authority).assertFolderBelongsToProject(2L,22L);
        assertThrows(IllegalStateException.class,()->create(2,22));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
    }
    @Test void wrongTenantAndNameDenialFailBeforeInsert() {
        TenantContextHolder.setTenantId(2L); assertThrows(IllegalStateException.class,()->create(2,21));
        TenantContextHolder.setTenantId(1L);
        doThrow(new IllegalStateException("NAME_DENIED")).when(access).assertNameVisible(7L,100L);
        assertThrows(IllegalStateException.class,()->create(2,21));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
    }
    @Test void obsoleteSourceRemainsTraceableAndCannotBecomeCurrentExecutableContent() {
        create(2,21);
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N","源.pdf","A/1","OBSOLETE",false,false,false));
        assertFalse(service.list(7L,2L,21L).get(0).selectedVersion().executable());
        assertThrows(IllegalStateException.class,()->create(3,31));
    }
    @Test void selectedLockedRowCannotChangeTenantVersionOrMasterIdentity(){
        for(var locked:List.of(
                new FileVersion(2L,100L,10L,9L,"N","源.pdf","A/1","ACTIVE",true,false,true),
                new FileVersion(1L,101L,10L,9L,"N","源.pdf","A/2","ACTIVE",true,false,true),
                new FileVersion(1L,100L,20L,9L,"N","源.pdf","A/1","ACTIVE",true,false,true))){
            doReturn(locked).when(files).resolveSelectedForUpdate(100L);
            assertEquals("DCC_REFERENCE_SOURCE_IDENTITY_INVALID",assertThrows(DccRelationFailure.class,()->create(2,21)).getMessage());
        }
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));verifyNoInteractions(audit);
    }
    @Test void concurrentSameMasterCreatesOnlyOneEntryPerFolderAndCountsProjects() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(3);
        try {
            var futures=java.util.stream.IntStream.range(0,3).mapToObj(i -> pool.submit(() -> {
                TenantContextHolder.setTenantId(1L);
                try { return create(i==2?3:2,i==2?31:21); } finally { TenantContextHolder.clear(); }
            })).toList();
            for(var f:futures) f.get(15,TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
        assertEquals(2,new DccRelationStore(jdbc, mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class)).referenceProjectCount(1L,10L));
    }
    @Test void auditFailureRollsBackCreateAndCancelWithRealDatabaseRows() {
        doThrow(new IllegalStateException("AUDIT_FAILED")).when(audit).append(any());
        assertThrows(IllegalStateException.class,()->create(2,21));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
        reset(audit);create(2,21);doThrow(new IllegalStateException("AUDIT_FAILED")).when(audit).append(any());
        assertThrows(IllegalStateException.class,()->cancel(2,21,true));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
    }
    @Test void successAuditHasReasonAndStableServerBeforeAfterIdentity() {
        create(2,21);cancel(2,21,true);
        var captor=org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
        verify(audit,times(2)).append(captor.capture());var created=captor.getAllValues().get(0);var cancelled=captor.getAllValues().get(1);
        assertEquals(created.getSubjectId(),cancelled.getSubjectId());assertFalse(created.getIdempotencyKey().isBlank());
        assertEquals("ABSENT",created.getBeforeState().getState());assertEquals("PRESENT",created.getAfterState().getState());
        assertEquals("PRESENT",cancelled.getBeforeState().getState());assertEquals("ABSENT",cancelled.getAfterState().getState());
        assertTrue(created.getAfterState().getCanonicalJson().contains("selectedControlledFileId"));
    }
    @Test void staleCancellationCannotDeleteARecreatedReference() {
        var original=create(2,21);
        cancel(2,21,true);
        var replacement=create(2,21);
        assertNotEquals(original.reference().id(),replacement.reference().id());
        assertThrows(IllegalStateException.class,()->tx.execute(s->service.cancel(7L,2L,21L,10L,
                original.reference().id(),true,"旧窗口取消")));
        assertEquals(replacement.reference().id(),jdbc.queryForObject("SELECT id FROM dcc_project_file_reference",Long.class));
    }
    @Test void concurrentLastReferencesCancelWithExactIdentitiesAndReturnZeroWithoutLosingHistory() throws Exception {
        var first=create(2,21);var second=create(3,31);
        jdbc.update("INSERT INTO dcc_current_file_relation VALUES (1,20,10)");
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(()->cancelOnThread(2L,21L,first.reference().id()));
            var b=pool.submit(()->cancelOnThread(3L,31L,second.reference().id()));
            var counts=java.util.List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            assertTrue(counts.contains(0L));assertTrue(counts.contains(1L));
        } finally {pool.shutdownNow();}
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation",Integer.class));
    }
    private long cancelOnThread(long project,long folder,long referenceId) {
        TenantContextHolder.setTenantId(1L);
        try {return tx.execute(s->service.cancel(7L,project,folder,10L,referenceId,true,"并发取消"));}
        finally {TenantContextHolder.clear();}
    }
    @Test void oneConfirmationPersistsSeveralReferencesInOneTransactionAndDuplicateMasterRejects(){
        when(files.resolveSelected(200L)).thenReturn(new FileVersion(1L,200L,20L,9L,"N-2","二.pdf","A/1","ACTIVE",true,false,true));
        var result=tx.execute(s->service.createBatch(7L,2L,21L,List.of(200L,100L),"一次确认多引用"));
        assertEquals(2,result.size());assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
        assertThrows(IllegalArgumentException.class,()->tx.execute(s->service.createBatch(7L,2L,22L,List.of(100L,100L),"重复")));
    }
    @Test void failureOnLaterFileRollsBackTheWholeReferenceConfirmation(){
        when(files.resolveSelected(200L)).thenReturn(new FileVersion(1L,200L,20L,9L,"N-2","二.pdf","A/1","ACTIVE",true,false,true));
        doThrow(new DccRelationFailure("DCC_NAME_DENIED")).when(access).assertNameVisible(7L,200L);
        assertThrows(IllegalStateException.class,()->tx.execute(s->service.createBatch(7L,2L,21L,List.of(100L,200L),"一次确认多引用")));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
        assertEquals(0,new DccRelationStore(jdbc,audit).referenceProjectCount(1L,10L));
    }
    @Test void sourceUsageReflectsDistinctProjectsAndFinalCancellationWithoutGrantingAnyContent(){
        create(2,21);create(2,22);create(3,31);
        assertEquals(2,service.getUsage(7L,100L).referenceProjectCount());assertTrue(service.getUsage(7L,100L).referenced());
        cancel(2,21,true);cancel(2,22,true);cancel(3,31,true);
        assertEquals(0,service.getUsage(7L,100L).referenceProjectCount());assertFalse(service.getUsage(7L,100L).referenced());
        verify(access,never()).assertContentReadable(any(),any());
        doThrow(new DccRelationFailure("DCC_NAME_DENIED")).when(access).assertNameVisible(7L,100L);
        assertThrows(IllegalStateException.class,()->service.getUsage(7L,100L));
        TenantContextHolder.setTenantId(2L);assertThrows(IllegalStateException.class,()->service.getUsage(7L,100L));
    }
}
