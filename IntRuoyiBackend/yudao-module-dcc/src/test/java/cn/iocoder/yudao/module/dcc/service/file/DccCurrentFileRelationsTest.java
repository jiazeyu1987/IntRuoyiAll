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
    FileVersion file(long id,long master,long project,boolean pending){return new FileVersion(1L,id,master,project,"N","当前名称",pending?"B/1":"A/1",pending?"CONTROLLED_PENDING_EFFECTIVE":"ACTIVE",true,pending,!pending);}
    DccControlledFileRelatedFileDO snapshot(long source,long id,long master){
        var row=DccControlledFileRelatedFileDO.builder().controlledFileId(source).relatedControlledFileId(id)
                .relatedMasterId(master).relatedFileNumberSnapshot("N").relatedVersionNoSnapshot("A/1")
                .relatedFileNameSnapshot("冻结名称").projectCodeId(1L).relationSource("UPLOAD").build();
        row.setTenantId(1L);return row;
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    @Test void currentRelationsKeepSelectedVersionAfterNewerTargetBecomesPendingOrActive(){
        dependencies();when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));
        when(files.resolveSelected(200L)).thenReturn(file(200,20,1,false));
        when(related.selectListByControlledFileId(100L)).thenReturn(List.of(snapshot(100,200,20)));
        var result=service.listCurrentRelatedFiles(7L,100L);
        assertEquals(200L,result.get(0).controlledFileId());assertEquals("冻结名称",result.get(0).fileName());
        verify(files,never()).resolveLatest(any());
        assertEquals("A/1",service.listHistoricalRelatedFiles(7L,100L).get(0).getVersionNo());
    }
    @Test void selectedSourceDoesNotSwitchToNewlyControlledPendingSource(){
        dependencies();when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));
        when(files.resolveSelected(101L)).thenReturn(file(101,10,1,true));
        when(files.resolveSelected(200L)).thenReturn(file(200,20,1,false));
        when(related.selectListByControlledFileId(100L)).thenReturn(List.of(snapshot(100,200,20)));
        jdbc.update("UPDATE dcc_current_file_relation_set SET controlled_file_id=101");
        var result=tx.execute(s->service.getCurrentRelationView(7L,100L));
        assertEquals(100L,result.sourceControlledFileId());assertEquals(200L,result.files().get(0).controlledFileId());
    }
    @Test void obsoletePinnedTargetRemainsVisibleWithoutSwitchingVersionOrGrantingBody(){
        dependencies();when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));
        when(files.resolveSelected(200L)).thenReturn(new FileVersion(1L,200L,20L,1L,"N","改名","A/1","OBSOLETE",false,false,false));
        when(related.selectListByControlledFileId(100L)).thenReturn(List.of(snapshot(100,200,20)));
        var row=service.listCurrentRelatedFiles(7L,100L).get(0);
        assertEquals(200L,row.controlledFileId());assertEquals("冻结名称",row.fileName());assertEquals("OBSOLETE",row.status());assertFalse(row.executable());
        doThrow(new DccRelationFailure("CONTENT_DENIED")).when(access).assertContentReadable(7L,200L);
        assertThrows(DccRelationFailure.class,()->service.assertRelatedContentReadable(7L,200L));
        verify(files,never()).resolveLatest(any());
    }
    @Test void freeEditingIsRejectedWithoutWritesEvenWithOldIdempotencyKey(){
        dependencies();
        var error=assertThrows(DccRelationFailure.class,()->tx.execute(s->service.replaceCurrentRelations(7L,100L,List.of(201L),List.of(),0L,"old-key","编辑原因")));
        assertEquals("DCC_RELATION_CHANGES_REQUIRE_APPLICATION",error.getMessage());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_relation_change_command",Integer.class));
        verifyNoInteractions(files,access,related,audit);
    }
    @Test void crossProjectAndPendingSelectionsAreRejectedBeforeAnyRelationInsert(){
        dependencies();when(fileMapper.selectById(100L)).thenReturn(DccControlledFileDO.builder().id(100L).masterId(10L).tenantId(1L).dccProjectCodeId(1L).build());
        for(var invalid:List.of(file(201,20,2,false),file(201,20,1,true))){
            when(files.resolveSelected(201L)).thenReturn(invalid);when(files.resolveLatest(20L)).thenReturn(invalid);
            var failure=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    ()->tx.executeWithoutResult(s->service.validateAndBindRelatedFiles(100L,1L,List.of(201L))));
            assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_RELATED_FILE_INVALID.getCode(),failure.getCode());
        }
        verify(files,times(2)).resolveSelected(201L);
        verify(files,times(2)).resolveLatest(20L);
        verify(related,never()).insert(any(DccControlledFileRelatedFileDO.class));
    }
    @Test void sameProjectActiveSelectionPersistsExactVersion() {
        dependencies();
        when(fileMapper.selectById(100L)).thenReturn(DccControlledFileDO.builder()
                .id(100L).masterId(10L).tenantId(1L).dccProjectCodeId(1L).status("WORKING").build());
        when(files.resolveSelected(201L)).thenReturn(file(201,20,1,false));
        when(files.resolveLatest(20L)).thenReturn(file(201,20,1,false));
        tx.executeWithoutResult(s->service.validateAndBindRelatedFiles(100L,1L,List.of(201L)));
        var captured=org.mockito.ArgumentCaptor.forClass(DccControlledFileRelatedFileDO.class);
        verify(related).insert(captured.capture());
        assertEquals(201L,captured.getValue().getRelatedControlledFileId());
        assertEquals(1L,captured.getValue().getProjectCodeId());
        assertEquals("A/1",captured.getValue().getRelatedVersionNoSnapshot());
    }
    @Test void missingCurrentProvenanceCannotBecomeAnEmptySuccess(){
        dependencies();when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));
        jdbc.update("DELETE FROM dcc_current_file_relation_set");
        assertThrows(DccRelationFailure.class,()->service.listCurrentRelatedFiles(7L,100L));
    }
    @Test void foreignMasterProvenanceIsRejected(){
        dependencies();when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));
        when(files.resolveSelected(201L)).thenReturn(file(201,20,1,false));
        jdbc.update("UPDATE dcc_current_file_relation_set SET controlled_file_id=201");
        assertThrows(DccRelationFailure.class,()->tx.execute(s->service.getCurrentRelationView(7L,100L)));
    }
    @Test void brokenSelectedSnapshotFailsWithoutTryingLatestOrFiltering(){
        dependencies();when(files.resolveSelected(100L)).thenReturn(file(100,10,1,false));
        when(files.resolveSelected(200L)).thenReturn(file(200,30,1,false));
        when(related.selectListByControlledFileId(100L)).thenReturn(List.of(snapshot(100,200,20)));
        assertThrows(RuntimeException.class,()->service.listCurrentRelatedFiles(7L,100L));
        verify(files,never()).resolveLatest(any());
    }
}
