package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccCurrentFileRelationsTest {
    DccControlledFileRelatedFileServiceImpl service; JdbcTemplate jdbc; TransactionTemplate tx;
    DccRelationAccessPolicy access; DccLatestControlledFileResolver files; DccControlledFileRelatedFileMapper related;
    DccControlledFileMapper fileMapper; DccControlledFileMasterMapper masterMapper;
    cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService audit;
    @BeforeEach void setup() throws Exception {
        JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE dcc_controlled_file_master (id BIGINT,tenant_id BIGINT,deleted INT)");
        jdbc.update("INSERT INTO dcc_controlled_file_master VALUES (10,1,0),(20,1,0),(30,1,0)");
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";")) if(!sql.isBlank())jdbc.execute(sql);
        access=mock(DccRelationAccessPolicy.class);files=mock(DccLatestControlledFileResolver.class);related=mock(DccControlledFileRelatedFileMapper.class);
        fileMapper=mock(DccControlledFileMapper.class);masterMapper=mock(DccControlledFileMasterMapper.class);
        when(fileMapper.selectByIdAndTenantForUpdate(any(),any())).thenAnswer(call->fileMapper.selectById((Long)call.getArgument(1)));
        service=new DccControlledFileRelatedFileServiceImpl();
        ReflectionTestUtils.setField(service,"relatedFileMapper",related);ReflectionTestUtils.setField(service,"controlledFileMapper",fileMapper);ReflectionTestUtils.setField(service,"controlledFileMasterMapper",masterMapper);
        // Added once implementation introduces its formal dependencies.
        TenantContextHolder.setTenantId(1L);
        jdbc.update("INSERT INTO dcc_current_file_relation_set (tenant_id,source_master_id,controlled_file_id) VALUES (1,10,100)");
    }
    void dependencies() {
        audit=mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
        ReflectionTestUtils.setField(service,"relationStore",new DccRelationStore(jdbc,audit));
        ReflectionTestUtils.setField(service,"relationAccessPolicy",access);ReflectionTestUtils.setField(service,"latestFileResolver",files);
    }
    FileVersion file(long id,long master,long project,boolean pending){return new FileVersion(1L,id,master,project,"N","源.pdf",pending?"B/1":"A/1","ACTIVE",true,pending,!pending);}
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    @Test void currentFutureLatestAndFrozenHistoricalVersionNeverMix() {
        when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));when(files.resolveLatest(20L)).thenReturn(file(201,20,2,true));
        jdbc.update("INSERT INTO dcc_current_file_relation VALUES (1,10,20)");
        when(related.selectListByControlledFileId(100L)).thenReturn(List.of(DccControlledFileRelatedFileDO.builder().relatedControlledFileId(200L).relatedMasterId(20L).relatedVersionNoSnapshot("A/1").relatedFileNameSnapshot("历史名称").projectCodeId(1L).build()));
        dependencies();
        assertEquals(201L,service.listCurrentRelatedFiles(7L,100L).get(0).controlledFileId());assertTrue(service.listCurrentRelatedFiles(7L,100L).get(0).pendingEffect());
        assertEquals("A/1",service.listHistoricalRelatedFiles(7L,100L).get(0).getVersionNo());
        assertEquals("历史名称",service.listHistoricalRelatedFiles(7L,100L).get(0).getFileName());
    }
    @Test void crossProjectBindUsesLatestIdentityNotProjectFilterAndRejectsSelfDuplicatesWrongTenant() {
        when(fileMapper.selectById(100L)).thenReturn(DccControlledFileDO.builder().id(100L).masterId(10L).tenantId(1L).dccProjectCodeId(1L).build());
        when(files.resolveSelected(201L)).thenReturn(file(201,20,2,true));when(files.resolveLatest(20L)).thenReturn(file(201,20,2,true));dependencies();
        tx.executeWithoutResult(s->service.validateAndBindRelatedFiles(100L,1L,List.of(201L)));
        verify(related).insert(any(DccControlledFileRelatedFileDO.class));verify(fileMapper,never()).selectAssociatedFilesByProjectCodeId(any(),any());
        when(files.resolveSelected(202L)).thenReturn(file(202,10,1,false));
        assertThrows(RuntimeException.class,()->service.validateAndBindRelatedFiles(100L,1L,List.of(202L)));
        when(files.resolveSelected(203L)).thenReturn(file(203,20,2,false));
        assertThrows(RuntimeException.class,()->service.validateAndBindRelatedFiles(100L,1L,List.of(201L,203L)));
        TenantContextHolder.setTenantId(2L);assertThrows(RuntimeException.class,()->service.validateAndBindRelatedFiles(100L,1L,List.of(201L)));
    }
    @Test void editingCurrentLeavesHistoricalRelationsUntouchedAndOptimisticConflictRollsBack() {
        when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));when(files.resolveSelected(201L)).thenReturn(file(201,20,2,true));when(files.resolveLatest(20L)).thenReturn(file(201,20,2,true));dependencies();
        tx.executeWithoutResult(s->service.replaceCurrentRelations(7L,100L,List.of(201L),List.of(),0L,"change-1","关联变更原因"));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation",Integer.class));verifyNoInteractions(related);
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->service.replaceCurrentRelations(7L,100L,List.of(),List.of(),0L,"change-conflict","并发修改")));
        tx.executeWithoutResult(s->service.replaceCurrentRelations(7L,100L,List.of(),List.of(20L),1L,"change-2","取消关联原因"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation",Integer.class));
    }
    @Test void noLatestFailsExplicitlyAndContentRemainsDeniedEvenWhenNameVisible() {
        when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));jdbc.update("INSERT INTO dcc_current_file_relation VALUES (1,10,20)");dependencies();
        assertThrows(IllegalStateException.class,()->service.listCurrentRelatedFiles(7L,100L));
        doThrow(new IllegalStateException("CONTENT_DENIED")).when(access).assertContentReadable(7L,200L);
        assertThrows(IllegalStateException.class,()->service.assertRelatedContentReadable(7L,200L));
        verify(access).assertContentReadable(7L,200L);
    }
    @Test void missingCurrentRelationProvenanceIsNotReportedAsNoRelations() {
        dependencies();when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));
        jdbc.update("DELETE FROM dcc_current_file_relation_set");
        assertThrows(IllegalStateException.class,()->service.listCurrentRelatedFiles(7L,100L));
    }
    @Test void staleSourceVersionCannotSaveEvenIfItsExpectedTargetsStillMatch() {
        dependencies();when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));
        jdbc.update("UPDATE dcc_current_file_relation_set SET controlled_file_id=101");
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->service.replaceCurrentRelations(7L,100L,List.of(),List.of(),0L,"stale-source","旧版保存")));
        assertEquals(101L,jdbc.queryForObject("SELECT controlled_file_id FROM dcc_current_file_relation_set",Long.class));
    }

    @Test void currentReadCannotPresentASourceVersionFromAnotherMaster() {
        dependencies();when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));
        when(files.resolveSelected(201L)).thenReturn(file(201,20,2,true));
        jdbc.update("UPDATE dcc_current_file_relation_set SET controlled_file_id=201");
        assertThrows(IllegalStateException.class,()->tx.execute(s->service.getCurrentRelationView(7L,100L)));
    }
    @Test void obsoleteLatestIsVisibleAsUnusableWithoutFallingBackToExecutionVersion() {
        dependencies();when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));
        jdbc.update("INSERT INTO dcc_current_file_relation VALUES (1,10,20)");
        when(files.resolveLatest(20L)).thenReturn(new FileVersion(1L,201L,20L,2L,"N","源.pdf","B/1","OBSOLETE",false,false,false));
        var current=service.listCurrentRelatedFiles(7L,100L);
        assertEquals(201L,current.get(0).controlledFileId());assertEquals("OBSOLETE",current.get(0).status());assertFalse(current.get(0).executable());
        verifyNoInteractions(masterMapper,fileMapper);
    }
    void mutationSources(){
        dependencies();when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));
        when(files.resolveSelected(201L)).thenReturn(file(201,20,2,true));when(files.resolveLatest(20L)).thenReturn(file(201,20,2,true));
    }
    RelationChange change(List<Long> ids,List<Long> expected,long version,String key){
        return tx.execute(s->service.replaceCurrentRelations(7L,100L,ids,expected,version,key,"关系变更"));
    }
    @Test void exactReplayReadsCommittedResultWithoutRepeatedAuditAfterMutableTargetsHaveChanged(){
        mutationSources();var first=change(List.of(201L),List.of(),0,"stable-key");
        assertEquals(1,first.rowVersion());
        when(files.resolveLatest(20L)).thenReturn(new FileVersion(1L,202L,20L,2L,"N","源.pdf","B/2","ACTIVE",true,false,true));
        assertEquals(first,change(List.of(201L),List.of(),0,"stable-key"));
        verify(audit,times(1)).append(any());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_relation_change_command",Integer.class));
    }
    @Test void replayKeyRejectsChangedPayloadOrActorAndAbaRevisionRejectsOldWindow(){
        mutationSources();change(List.of(201L),List.of(),0,"key1");
        assertThrows(IllegalStateException.class,()->change(List.of(),List.of(),0,"key1"));
        assertThrows(IllegalStateException.class,()->tx.execute(s->service.replaceCurrentRelations(8L,100L,List.of(201L),List.of(),0L,"key1","关系变更")));
        change(List.of(),List.of(20L),1,"key2");
        assertThrows(IllegalStateException.class,()->change(List.of(201L),List.of(),0,"old-window"));
        assertEquals(2,jdbc.queryForObject("SELECT row_version FROM dcc_current_file_relation_set",Long.class));
    }
    @Test void auditFailureRollsBackRelationsVersionAndCommandFact(){
        mutationSources();doThrow(new DccRelationFailure("DCC_AUDIT_FAILED")).when(audit).append(any());
        assertThrows(IllegalStateException.class,()->change(List.of(201L),List.of(),0,"failure-key"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT row_version FROM dcc_current_file_relation_set",Long.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_relation_change_command",Integer.class));
    }
    @Test void concurrentIdenticalCommandCommitsOneVersionAndOneAudit() throws Exception {
        mutationSources();var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<RelationChange> call=()->{TenantContextHolder.setTenantId(1L);try{return change(List.of(201L),List.of(),0,"concurrent-key");}finally{TenantContextHolder.clear();}};
            var first=pool.submit(call);var second=pool.submit(call);
            assertEquals(first.get(15,java.util.concurrent.TimeUnit.SECONDS),second.get(15,java.util.concurrent.TimeUnit.SECONDS));
        }finally{pool.shutdownNow();}
        assertEquals(1,jdbc.queryForObject("SELECT row_version FROM dcc_current_file_relation_set",Long.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_relation_change_command",Integer.class));verify(audit,times(1)).append(any());
    }
    @Test void commandFactInsertFailureRollsBackVersionAndRelationBeforeAudit(){
        mutationSources();jdbc.execute("ALTER TABLE dcc_relation_change_command ADD CONSTRAINT deliberate_command_failure CHECK(actor_id<>7)");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->change(List.of(201L),List.of(),0,"command-failure"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT row_version FROM dcc_current_file_relation_set",Long.class));verifyNoInteractions(audit);
    }
}
