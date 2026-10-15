package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real candidate/count/LIMIT queries, exact QueryService authorization; no business DB or HTTP server. */
class DccControlledFileSelectorDatabaseTest extends BaseDbUnitTest {
    @Resource private DccControlledFileSelectorMapper selectorMapper;
    @Resource private DccControlledFileMapper controlledFileMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper projectCodeMapper;
    @Resource private DccControlledFileApplicationEvidenceMapper evidenceMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper directoryMapper;
    @Resource private DataSource dataSource;
    private DccControlledFileQueryServiceImpl query;
    private JdbcTemplate jdbc;

    @BeforeEach void setup() throws Exception {
        try(var c=dataSource.getConnection()){assertTrue(c.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        TenantContextHolder.setTenantId(1L); jdbc=new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_application_round_link(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT NOT NULL,project_id BIGINT NOT NULL,application_type VARCHAR(16) NOT NULL,application_id BIGINT NOT NULL,bpm_round VARCHAR(64),attribute_round INT NOT NULL,UNIQUE(tenant_id,application_type,application_id,bpm_round),UNIQUE(tenant_id,application_type,application_id,attribute_round))");
        jdbc.update("DELETE FROM dcc_application_round_link");
        jdbc.update("DELETE FROM dcc_project_file_placement");
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,project_code,status) VALUES(5,1,'Source project','P5','ACTIVE'),(6,1,'Other project','P6','ACTIVE')");
        jdbc.update("INSERT INTO dcc_file_directory(id,tenant_id,code,name) VALUES(5,1,'NAS5','Source NAS folder'),(6,1,'NAS6','Restricted NAS folder')");
        master(10L,5L,20L,21L); file(20L,10L,5L,5L,"A/1","ACTIVE",99L);
        file(21L,10L,5L,5L,"A/2","CONTROLLED_PENDING_EFFECTIVE",99L);
        file(24L,10L,5L,5L,"A/2-1","WORKING",99L);
        master(11L,6L,null,22L); file(22L,11L,6L,5L,"B/1","CONTROLLED_PENDING_EFFECTIVE",99L);
        master(12L,5L,23L,23L); file(23L,12L,5L,5L,"A/1","ACTIVE",88L); // name only
        master(13L,6L,25L,25L); file(25L,13L,6L,6L,"A/1","ACTIVE",88L); // hidden
        query=new DccControlledFileQueryServiceImpl();
        for(var f:DccControlledFileQueryServiceImpl.class.getDeclaredFields()){
            if(!Modifier.isStatic(f.getModifiers()) && f.getAnnotation(Resource.class)!=null)
                ReflectionTestUtils.setField(query,f.getName(),mock(f.getType()));
        }
        ReflectionTestUtils.setField(query,"selectorMapper",selectorMapper);
        ReflectionTestUtils.setField(query,"controlledFileMasterMapper",masterMapper);ReflectionTestUtils.setField(query,"projectCodeMapper",projectCodeMapper);
        ReflectionTestUtils.setField(query,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy());
        var permissions=mock(DccDirectoryAccessPermissionService.class);
        when(permissions.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.QUERY)).thenReturn(Set.of(5L));
        when(permissions.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of());
        ReflectionTestUtils.setField(query,"directoryAccessPermissionService",permissions);
        var scope=mock(DccControlledFileAssignmentScopeService.class);
        when(scope.isWithinAssignedFileScope(eq(99L),anyLong())).thenReturn(true);
        ReflectionTestUtils.setField(query,"assignmentScopeService",scope);
        var projects=mock(cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService.class);
        when(projects.listReadableProjectIds(99L)).thenReturn(List.of(5L,6L));
        ReflectionTestUtils.setField(query,"projectAccessService",projects);
        var storage=mock(FileMapper.class); when(storage.selectById(501L)).thenReturn(FileDO.builder().id(501L).name("controlled.pdf").type("application/pdf").build());
        ReflectionTestUtils.setField(query,"fileMapper",storage);
        var settings=mock(DccControlledFileBrowserSettingsService.class);when(settings.getBlacklistedExtensionPatterns()).thenReturn(List.of());
        ReflectionTestUtils.setField(query,"browserSettingsService",settings);
    }
    @Test void selectorBlacklistUsesActualSourceExtensionBeforeAuthorizedTotalAndPage() {
        jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name='Internal.DB',file_name='Template.pdf' WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name='SOP.PDF',file_name='Template.db' WHERE id=23");
        var settings = (DccControlledFileBrowserSettingsService) ReflectionTestUtils.getField(query, "browserSettingsService");
        when(settings.getBlacklistedExtensionPatterns()).thenReturn(List.of("*.db"));
        var first = query.getControlledFileSelectorPage(99L, q(null, null, null, null, 1, 1));
        assertEquals(1L, first.getTotal()); assertEquals(23L, first.getList().get(0).controlledFileId());
        assertEquals("SOP.PDF", first.getList().get(0).fileName());
        var second = query.getControlledFileSelectorPage(99L, q(null, null, null, null, 2, 1));
        assertEquals(1L, second.getTotal()); assertTrue(second.getList().isEmpty());
    }
    @Test void actualBrowserSqlKeepsEffectiveCurrentAndIgnoresPendingLatestBeforePagination() {
        ReflectionTestUtils.setField(query, "controlledFileMapper", controlledFileMapper);
        ReflectionTestUtils.setField(query, "controlledFileMasterMapper", masterMapper);
        ReflectionTestUtils.setField(query, "directoryMapper", directoryMapper);
        var request = new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO();
        request.setLatestVersionOnly(true); request.setPageNo(1); request.setPageSize(10);
        var result = query.getControlledFileBrowserPage(99L, request);
        assertEquals(2L, result.getTotal());
        assertEquals(Set.of(20L, 23L), result.getList().stream().map(row -> row.getId()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(result.getList().stream().noneMatch(row -> row.getId().equals(21L) || row.getId().equals(22L) || row.getId().equals(24L)));
        var workflowPage = query.getControlledFilePage(99L, request);
        assertEquals(2L, workflowPage.getTotal());
        assertEquals(Set.of(20L, 23L), workflowPage.getList().stream().map(row -> row.getId()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(20L, masterMapper.selectById(10L).getCurrentActiveControlledFileId());
        var current = result.getList().stream().filter(row -> row.getId().equals(20L)).findFirst().orElseThrow();
        assertEquals("ACTIVE", current.getStatus());
        assertFalse(Boolean.TRUE.equals(current.getCanDownload()));
        assertFalse(Boolean.TRUE.equals(current.getCanPrint()));
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=NULL WHERE id=10");
        result = query.getControlledFileBrowserPage(99L, request);
        assertEquals(2L, result.getTotal());
        assertTrue(result.getList().stream().anyMatch(row -> row.getId().equals(20L)));
    }
    @Test void actualBrowserDefaultSourceSearchAndExplicitWorkingFilterRemainDistinct() {
        ReflectionTestUtils.setField(query, "controlledFileMapper", controlledFileMapper);
        ReflectionTestUtils.setField(query, "controlledFileMasterMapper", masterMapper);
        ReflectionTestUtils.setField(query, "directoryMapper", directoryMapper);
        var request = new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO();
        request.setKeyword("SOURCE-21.pdf"); request.setPageNo(1); request.setPageSize(10);
        var result = query.getControlledFileBrowserPage(99L, request);
        assertEquals(0L, result.getTotal());
        assertNull(request.getLatestVersionOnly()); assertEquals("SOURCE-21.pdf", request.getKeyword());
        request.setKeyword("SOURCE-20.pdf"); result=query.getControlledFileBrowserPage(99L,request);
        assertEquals(1L,result.getTotal());assertEquals(20L,result.getList().get(0).getId());
        request.setKeyword(null); request.setStatus("WORKING");
        result = query.getControlledFileBrowserPage(99L, request);
        assertEquals(1L, result.getTotal()); assertEquals(24L, result.getList().get(0).getId());
        assertEquals("WORKING", result.getList().get(0).getStatus());
    }
    @Test void browserSelectorReadsFormalPlacementWithSqlTotalAndNameOnlyBodyIsolation() {
        jdbc.update("DELETE FROM dcc_project_file_placement");
        jdbc.update("INSERT INTO dcc_project_folder(id,tenant_id,project_code_id,parent_id,name,sort_order,active,source_template_id,source_node_key,deleted) VALUES(500,1,5,0,'Logical SOP',0,1,1,'c-test-500',0),(501,1,6,0,'Other logical',0,1,1,'c-test-501',0)");
        jdbc.update("INSERT INTO dcc_project_file_placement(id,tenant_id,project_code_id,project_folder_id,controlled_file_id,storage_directory_id) VALUES(1,1,5,500,20,5),(2,1,6,501,22,5)");
        var request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO();
        request.setSelectorScope("PROJECT_FOLDER");request.setDccProjectCodeId(5L);request.setProjectFolderId(500L);request.setPageNo(1);request.setPageSize(1);
        var result=query.getControlledFileBrowserPage(99L,request);
        assertEquals(1L,result.getTotal());assertEquals(20L,result.getList().get(0).getId());
        assertEquals(500L,result.getList().get(0).getProjectFolderId());assertEquals("Logical SOP",result.getList().get(0).getProjectFolderName());
        assertEquals("Source project",result.getList().get(0).getProjectName());
        assertEquals(21L,result.getList().get(0).getLatestControlledFileId());assertFalse(result.getList().get(0).getPendingEffect());
        request.setPageNo(2);result=query.getControlledFileBrowserPage(99L,request);assertEquals(1L,result.getTotal());assertTrue(result.getList().isEmpty());
        request.setSelectorScope("GLOBAL");request.setProjectFolderId(null);request.setDccProjectCodeId(null);request.setPageNo(1);request.setPageSize(20);
        result=query.getControlledFileBrowserPage(99L,request);assertEquals(2L,result.getTotal());
        var unknown=result.getList().stream().filter(r -> r.getId().equals(23L)).findFirst().orElseThrow();
        assertNull(unknown.getProjectFolderId());assertNull(unknown.getProjectFolderName());assertFalse(unknown.getCanPreview());
        assertNull(unknown.getSourceSha256());assertNull(unknown.getChangeDescription());
        request.setSelectorScope("PROJECT_FOLDER");request.setDccProjectCodeId(5L);request.setProjectFolderId(5L);
        assertEquals(0L,query.getControlledFileBrowserPage(99L,request).getTotal()); // NAS ID5 is not folder500.
        request.setSelectorScope(null);
        assertThrows(IllegalArgumentException.class,() -> query.getControlledFileBrowserPage(99L,request));
    }
    @Test void globalPageReturnsOneEffectiveCurrentPerMasterAndTrueAuthorizedTotal() {
        var first=query.getControlledFileSelectorPage(99L,q(null,null,null,null,1,2));
        assertEquals(2L,first.getTotal()); assertEquals(List.of(20L,23L),first.getList().stream().map(DccControlledFileSelectorRow::controlledFileId).toList());
        assertTrue(first.getList().stream().allMatch(row -> row.executable() && !row.pendingEffect()));
        assertEquals(21L,first.getList().get(0).latestControlledFileId());
        assertEquals(20L,first.getList().get(0).currentActiveControlledFileId());
        var next=query.getControlledFileSelectorPage(99L,q(null,null,null,null,2,2));
        assertEquals(2L,next.getTotal()); assertTrue(next.getList().isEmpty());
        assertFalse(first.getList().get(1).canPreview()); assertTrue(first.getList().get(1).controlled());
    }
    @Test void realFrozenSnapshotReadUsesFileViewAndPersistedRoundWithoutProjectDefaultsOrEditor() {
        ReflectionTestUtils.setField(query,"controlledFileMapper",controlledFileMapper);
        ReflectionTestUtils.setField(query,"applicationEvidenceMapper",evidenceMapper);
        var guard=new DccControlledFileDetailAuthorizationGuard();
        for(String field:new String[]{"assignmentScopeService","directoryAccessPermissionService","permissionSupport","viewMatrixAccessService","distributionRecipientMapper","routeSnapshotMapper","bpmTaskService","offlineTraining"})
            ReflectionTestUtils.setField(guard,field,ReflectionTestUtils.getField(query,field));
        var view=(DccControlledFileViewMatrixAccessService)ReflectionTestUtils.getField(query,"viewMatrixAccessService");
        when(view.canAccessCurrentViewMatrix(eq(99L),any(DccControlledFileDO.class))).thenReturn(true);
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",guard);
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_application_round_link(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT NOT NULL,project_id BIGINT NOT NULL,application_type VARCHAR(16) NOT NULL,application_id BIGINT NOT NULL,bpm_round VARCHAR(64),attribute_round INT NOT NULL,UNIQUE(tenant_id,application_type,application_id,bpm_round),UNIQUE(tenant_id,application_type,application_id,attribute_round))");
        jdbc.update("DELETE FROM dcc_application_round_link");
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,5,'REVISION',23,'actual-history-bpm',7)");
        String saved="{\"targetMarkets\":[\"CE\"],\"licenseHolder\":\"Y\",\"actualManufacturer\":\"N\",\"documentTransfer\":\"N\"}";
        String actual="{\"targetMarkets\":[\"FDA\"],\"licenseHolder\":\"N\",\"actualManufacturer\":\"Y\",\"documentTransfer\":\"N\"}";
        jdbc.update("INSERT INTO dcc_project_application_attributes(tenant_id,project_code_id,application_type,application_id,application_round,default_source_json,actual_attributes_json,submitted) VALUES(1,5,'REVISION',23,7,?,?,1)",saved,actual);
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",actual);
        var stages=spy(installActualRelationEvidenceReader());
        ReflectionTestUtils.setField(query,"relationStageSnapshotService",stages);
        var result=query.getApplicationEvidence(99L,23L,"REVISION","actual-history-bpm");
        assertTrue(result.recorded());assertEquals(7,result.attributeRound());
        assertEquals(List.of("CE"),result.defaultSource().targetMarkets());assertEquals(List.of("FDA"),result.actualAttributes().targetMarkets());
        assertNotNull(result.relationEvidence());assertFalse(result.relationEvidence().recorded());
        assertEquals("actual-history-bpm",result.relationEvidence().bpmRound());assertTrue(result.relationEvidence().stages().isEmpty());
        var editor=(cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService)ReflectionTestUtils.getField(query,"projectAccessService");
        verifyNoInteractions(editor);
        jdbc.update("UPDATE dcc_project_application_attributes SET submitted=0 WHERE application_id=23");
        result=query.getApplicationEvidence(99L,23L,"REVISION","actual-history-bpm");
        assertFalse(result.recorded());assertEquals("NOT_FROZEN",result.unavailableReason());assertNull(result.actualAttributes());
        assertNull(result.relationEvidence());verify(stages,times(1)).readRoundEvidence(99L,23L,"REVISION","actual-history-bpm");
        // An actually missing relation schema is a dependency error, never an empty successful payload.
        jdbc.update("UPDATE dcc_project_application_attributes SET submitted=1 WHERE application_id=23");
        var missing=new org.h2.jdbcx.JdbcDataSource();missing.setURL("jdbc:h2:mem:w08_missing_relation_"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        installActualRelationEvidenceReader(new JdbcTemplate(missing));
        var schemaFailure=assertThrows(org.springframework.jdbc.BadSqlGrammarException.class,
                ()->query.getApplicationEvidence(99L,23L,"REVISION","actual-history-bpm"));
        assertTrue(schemaFailure.getMessage().contains("dcc_controlled_file_related_file"));
    }

    @Test void applicationRelationHistoryReadsRealInvalidStageAndCompleteMembersInOneSnapshot() throws Exception {
        ReflectionTestUtils.setField(query,"controlledFileMapper",controlledFileMapper);
        ReflectionTestUtils.setField(query,"applicationEvidenceMapper",evidenceMapper);
        var guard=new DccControlledFileDetailAuthorizationGuard();
        for(String field:new String[]{"assignmentScopeService","directoryAccessPermissionService","permissionSupport","viewMatrixAccessService",
                "distributionRecipientMapper","routeSnapshotMapper","bpmTaskService","offlineTraining"})
            ReflectionTestUtils.setField(guard,field,ReflectionTestUtils.getField(query,field));
        when(((DccControlledFileViewMatrixAccessService)ReflectionTestUtils.getField(query,"viewMatrixAccessService"))
                .canAccessCurrentViewMatrix(eq(99L),any())).thenReturn(true);
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",guard);
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,5,'REVISION',23,'actual-history-bpm',7)");
        String attributes="{\"targetMarkets\":[\"CE\"],\"licenseHolder\":\"Y\",\"actualManufacturer\":\"N\",\"documentTransfer\":\"N\"}";
        jdbc.update("INSERT INTO dcc_project_application_attributes(tenant_id,project_code_id,application_type,application_id,application_round,default_source_json,actual_attributes_json,submitted) VALUES(1,5,'REVISION',23,7,?,?,1)",attributes,attributes);
        master(14L,5L,26L,26L);file(26L,14L,5L,5L,"B/1","ACTIVE",88L);
        jdbc.update("INSERT INTO dcc_controlled_file_related_file(tenant_id,controlled_file_id,related_controlled_file_id,related_master_id,project_code_id,related_file_number_snapshot,related_file_name_snapshot,related_version_no_snapshot,relation_source) VALUES(1,23,20,10,5,'N20','Original relation','A/1','UPLOAD')");
        var stages=installActualRelationEvidenceReader();
        var first=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.Selection(10L,20L,5L,"N20","Original relation","A/1","UPLOAD",false);
        var added=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.Selection(14L,26L,5L,"N26","Signed supplement","B/1","SUPPLEMENT",true);
        String complete=cn.hutool.crypto.digest.DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(List.of(first,added)));
        Long header=9007199254740993L,signature=9007199254740994L;var signedAt=LocalDateTime.of(2026,10,11,12,34,56);
        jdbc.update("INSERT INTO dcc_controlled_file_signature(id,tenant_id,controlled_file_id,revision_id,version_no,process_instance_id,task_id,actor_id,signed_at,action_type,signature_mode,evidence_status) VALUES(?,1,23,23,'A/1','actual-history-bpm','historical-task',99,?,'ASSIGN','PASSWORD','INVALID')",signature,signedAt);
        jdbc.update("INSERT INTO dcc_relation_stage_snapshot(id,tenant_id,source_file_id,source_master_id,project_id,process_instance_id,task_id,obligation_id,stage_code,node_instance_id,department_id,actor_id,signature_id,signed_at,command_key,payload_hash,expected_digest,complete_digest,action_type,reason,application_type,attribute_round) VALUES(?,1,23,12,5,'actual-history-bpm','historical-task','dept-5','MATRIX_REVIEW','actual-node',5,99,?,?,'historical-key',?,?,?,'ASSIGN','Original signed opinion','REVISION',7)",header,signature,signedAt,"a".repeat(64),"b".repeat(64),complete);
        jdbc.update("INSERT INTO dcc_relation_stage_member(tenant_id,snapshot_id,related_master_id,selected_file_id,project_id,file_number_snapshot,file_name_snapshot,version_no_snapshot,origin,supplemental) VALUES(1,?,10,20,5,'N20','Original relation','A/1','UPLOAD',0),(1,?,14,26,5,'N26','Signed supplement','B/1','SUPPLEMENT',1)",header,header);
        var manager=new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
        var transaction=new org.springframework.transaction.support.TransactionTemplate(manager);
        transaction.setReadOnly(true);transaction.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var read=transaction.execute(status->{
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            return query.getApplicationEvidence(99L,23L,"REVISION","actual-history-bpm");
        });
        assertNotNull(read);assertTrue(read.recorded());var relation=read.relationEvidence();assertNotNull(relation);
        assertEquals(7,relation.attributeRound());assertEquals(List.of(20L,26L),relation.completeSelections().stream().map(
                cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.Selection::selectedControlledFileId).toList());
        assertEquals(1,relation.stages().size());assertEquals("INVALID",relation.stages().get(0).signatureEvidenceStatus());
        assertTrue(relation.stages().get(0).signatureIdentityMatches());assertEquals(2,relation.stages().get(0).snapshot().members().size());
        assertEquals(signedAt,relation.stages().get(0).snapshot().header().signedAt());
        var json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(read));
        assertEquals("9007199254740993",json.at("/relationEvidence/stages/0/snapshot/header/id").textValue());
        assertEquals("9007199254740994",json.at("/relationEvidence/stages/0/snapshot/header/signatureId").textValue());
        assertEquals("2026-10-11 12:34:56",json.at("/relationEvidence/stages/0/snapshot/header/signedAt").textValue());
        // Moving today's execution pointer never rewrites the selected historical set or signature evidence.
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL,latest_controlled_file_id=NULL WHERE id=12");
        var again=query.getApplicationEvidence(99L,23L,"REVISION","actual-history-bpm");
        assertEquals(relation,again.relationEvidence());
        jdbc.update("DELETE FROM dcc_relation_stage_member WHERE snapshot_id=?",header);
        jdbc.update("DELETE FROM dcc_relation_stage_snapshot WHERE id=?",header);
    }

    private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageSnapshotService installActualRelationEvidenceReader() {
        return installActualRelationEvidenceReader(jdbc);
    }
    private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageSnapshotService installActualRelationEvidenceReader(JdbcTemplate relationJdbc) {
        var resolver=new cn.iocoder.yudao.module.dcc.service.file.relations.DccLatestControlledFileResolverImpl();
        ReflectionTestUtils.setField(resolver,"fileMapper",controlledFileMapper);ReflectionTestUtils.setField(resolver,"masterMapper",masterMapper);
        ReflectionTestUtils.setField(resolver,"jdbc",jdbc);
        var access=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationAccessPolicyImpl();ReflectionTestUtils.setField(access,"query",query);
        var rounds=new DccApplicationRoundService();ReflectionTestUtils.setField(rounds,"jdbc",jdbc);
        ReflectionTestUtils.setField(rounds,"files",controlledFileMapper);ReflectionTestUtils.setField(rounds,"projects",projectCodeMapper);
        var stages=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageSnapshotService(
                new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStore(relationJdbc,mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class)),
                new DccRelationStageSnapshotMapper(relationJdbc),query,resolver,access);
        ReflectionTestUtils.setField(stages,"rounds",rounds);ReflectionTestUtils.setField(query,"relationStageSnapshotService",stages);
        var history=new DccApplicationHistoryGuard();
        var processes=mock(cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService.class);
        var actual=mock(org.flowable.engine.history.HistoricProcessInstance.class);
        when(actual.getId()).thenReturn("actual-history-bpm");when(actual.getTenantId()).thenReturn("1");
        when(actual.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.REVISION);
        when(actual.getBusinessKey()).thenReturn("23");when(actual.getStartUserId()).thenReturn("88");
        when(processes.getHistoricProcessInstance("actual-history-bpm")).thenReturn(actual);
        ReflectionTestUtils.setField(history,"processes",processes);ReflectionTestUtils.setField(query,"applicationHistoryGuard",history);
        return stages;
    }
    @Test void projectionCarriesActualProjectSourceDirectoryAndIndependentPreviewPermission() {
        var result=query.getControlledFileSelectorPage(99L,q(5L,5L,null,null,1,20));
        assertEquals(2L,result.getTotal()); var row=result.getList().get(0);
        assertEquals(5L,row.projectId());assertEquals("Source project",row.projectName());
        assertEquals(5L,row.sourceDirectoryId());assertEquals("Source NAS folder",row.sourceDirectoryName());
        assertNull(row.projectFolderId()); assertNull(row.projectFolderName());
        assertEquals(20L,row.controlledFileId());assertEquals(21L,row.latestControlledFileId()); assertEquals("SOURCE-20.pdf",row.fileName());
        assertFalse(row.canPreview());
    }

    @Test void applicationHistoryListsOnlyExactBoundRoundsAndExcludesReservedDrafts() {
        ReflectionTestUtils.setField(query,"controlledFileMapper",controlledFileMapper);
        ReflectionTestUtils.setField(query,"applicationEvidenceMapper",evidenceMapper);
        var guard=new DccControlledFileDetailAuthorizationGuard();
        for(String field:new String[]{"assignmentScopeService","directoryAccessPermissionService","permissionSupport","viewMatrixAccessService","distributionRecipientMapper","routeSnapshotMapper","bpmTaskService","offlineTraining"})
            ReflectionTestUtils.setField(guard,field,ReflectionTestUtils.getField(query,field));
        var view=(DccControlledFileViewMatrixAccessService)ReflectionTestUtils.getField(query,"viewMatrixAccessService");
        when(view.canAccessCurrentViewMatrix(eq(99L),any(DccControlledFileDO.class))).thenReturn(true);
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",guard);
        jdbc.update("DELETE FROM dcc_application_round_link");
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,5,'UPLOAD',23,'upload-23',1),(1,5,'OBSOLETE',23,'obsolete-first',1),(1,5,'OBSOLETE',23,'obsolete-second',2),(1,5,'REVISION',23,NULL,1),(2,5,'OBSOLETE',23,'foreign-tenant',1),(1,6,'OBSOLETE',23,'foreign-project',3)");
        installDeclaredHistoricalRoundFlowPorts();
        java.util.List<?> result=ReflectionTestUtils.invokeMethod(query,"listApplicationRounds",99L,23L);
        assertNotNull(result);assertEquals(3,result.size());
        var rows=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(result),java.util.Map.class);
        assertEquals(Set.of("upload-23","obsolete-first","obsolete-second"),rows.stream().map(row->row.get("bpmRound")).collect(java.util.stream.Collectors.toSet()));
        assertTrue(rows.stream().allMatch(row->"23".equals(row.get("controlledFileId"))));
        for(Object item:result){var contract=((DccApplicationRoundSummary)item).getFlowContract();assertNotNull(contract);
            assertEquals(DccBoundWorkflowContractResolver.RoundContractStatus.NOT_RECORDED,contract.status());assertNull(contract.flowRule());}
        TenantContextHolder.setTenantId(2L);
        assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(query,"listApplicationRounds",99L,23L));
    }

    /** Original mapping fixture has no later attributes. Actual leaf/guard consume explicitly isolated
     * historical BPM/definition/form read ports, never a mock READY result or invented snapshot row. */
    private void installDeclaredHistoricalRoundFlowPorts() {
        var rounds=new DccApplicationRoundService();ReflectionTestUtils.setField(rounds,"jdbc",jdbc);
        ReflectionTestUtils.setField(rounds,"files",controlledFileMapper);ReflectionTestUtils.setField(rounds,"projects",projectCodeMapper);
        var attributes=mock(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper.class);
        var models=new java.util.HashMap<>(DccW08PreparedUnitFixture.loadLegacyModels());
        try{java.nio.file.Path root=java.nio.file.Path.of("").toAbsolutePath();while(root!=null&&!java.nio.file.Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
            byte[] xml=java.nio.file.Files.readAllBytes(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/obsolete.bpmn20.xml"));
            models.put(DccControlledFileProcessDefinitionKeys.OBSOLETE,new org.flowable.bpmn.converter.BpmnXMLConverter()
                    .convertToBpmnModel((org.flowable.common.engine.api.io.InputStreamProvider)()->new java.io.ByteArrayInputStream(xml),false,false));}
        catch(java.io.IOException failure){throw new IllegalStateException(failure);}
        var definitions=mock(cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService.class);
        var processes=mock(cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService.class);
        var forms=mock(cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper.class);
        var histories=new java.util.HashMap<String,org.flowable.engine.history.HistoricProcessInstance>();
        var formFacts=new java.util.HashMap<String,cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO>();
        for(String bpm:List.of("upload-23","obsolete-first","obsolete-second")){
            boolean obsolete=bpm.startsWith("obsolete");String key=obsolete?DccControlledFileProcessDefinitionKeys.OBSOLETE:DccControlledFileProcessDefinitionKeys.UPLOAD;
            String definitionId=key+":declared-historical-read";var definition=mock(org.flowable.engine.repository.ProcessDefinition.class);
            when(definition.getId()).thenReturn(definitionId);when(definition.getKey()).thenReturn(key);when(definition.getTenantId()).thenReturn("1");when(definition.getDeploymentId()).thenReturn("declared-old-deployment");
            when(definitions.getProcessDefinition(definitionId)).thenReturn(definition);when(definitions.getProcessDefinitionBpmnModel(definitionId)).thenReturn(models.get(key));
            var history=mock(org.flowable.engine.history.HistoricProcessInstance.class);when(history.getId()).thenReturn(bpm);when(history.getTenantId()).thenReturn("1");
            when(history.getStartUserId()).thenReturn("88");when(history.getProcessDefinitionKey()).thenReturn(key);when(history.getProcessDefinitionId()).thenReturn(definitionId);
            when(history.getEndTime()).thenReturn(java.sql.Timestamp.valueOf("2026-10-01 12:00:00"));
            if(obsolete){long id=bpm.equals("obsolete-first")?801L:802L;String code="FORM-"+bpm;
                when(history.getBusinessKey()).thenReturn("FORM_ACTION:"+code);
                when(history.getProcessVariables()).thenReturn(java.util.Map.of("systemCode","DCC","objectType","CONTROLLED_FILE","actionCode","OBSOLETE",
                        "objectId","23","objectVersion","A/1","controlledFileId",23L,"tenantId",1L,"formInstanceId",id));
                var form=new cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO();form.setId(id);form.setTenantId(1L);form.setApplicantUserId(88L);
                form.setInstanceCode(code);form.setSystemCode("DCC");form.setObjectType("CONTROLLED_FILE");form.setActionCode("OBSOLETE");form.setObjectId("23");form.setObjectVersion("A/1");
                form.setBpmProcessInstanceId(bpm);form.setFormDataJson("{}");form.setDeleted(false);formFacts.put(bpm,form);
            }else{when(history.getBusinessKey()).thenReturn("23");when(history.getProcessVariables()).thenReturn(java.util.Map.of("controlledFileId",23L,"needTraining",false));}
            histories.put(bpm,history);
        }
        when(processes.getHistoricProcessInstance(anyString())).thenAnswer(call->histories.get(call.getArgument(0)));
        when(forms.selectList(any())).thenAnswer(call->{var wrapper=call.<com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>>getArgument(0);
            wrapper.getSqlSegment(); // Materialize the exact query parameters before matching the declared read-port facts.
            var matches=formFacts.entrySet().stream().filter(entry->wrapper.getParamNameValuePairs().containsValue(entry.getKey())).map(java.util.Map.Entry::getValue).toList();return matches;});
        var historyGuard=new DccApplicationHistoryGuard();ReflectionTestUtils.setField(historyGuard,"processes",processes);
        var leaf=new DccBoundWorkflowContractResolver();ReflectionTestUtils.setField(leaf,"definitions",definitions);ReflectionTestUtils.setField(leaf,"processes",processes);
        ReflectionTestUtils.setField(leaf,"rounds",rounds);ReflectionTestUtils.setField(leaf,"attributes",attributes);ReflectionTestUtils.setField(leaf,"forms",forms);
        ReflectionTestUtils.setField(query,"roundFlowContracts",leaf);ReflectionTestUtils.setField(query,"applicationHistoryGuard",historyGuard);
    }

    @Test void relationCapabilitiesUseActualProjectAndBodyPermissionsWithoutSwallowingInfrastructureFailure() {
        ReflectionTestUtils.setField(query,"controlledFileMapper",controlledFileMapper);
        var permission=(DccControlledFileCategoryPermissionSupport)ReflectionTestUtils.getField(query,"permissionSupport");
        Object initial=ReflectionTestUtils.invokeMethod(query,"getRelationPermissions",99L,23L);
        assertEquals(false,ReflectionTestUtils.invokeMethod(initial,"canEdit"));
        assertEquals(false,ReflectionTestUtils.invokeMethod(initial,"canPreview"));
        when(permission.hasCategoryPermission(controlledFileMapper.selectById(23L).getCategoryId(),99L,
                cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.UPLOAD)).thenReturn(true);
        Object editable=ReflectionTestUtils.invokeMethod(query,"getRelationPermissions",99L,23L);
        assertEquals(true,ReflectionTestUtils.invokeMethod(editable,"canEdit"));
        assertEquals(false,ReflectionTestUtils.invokeMethod(editable,"canPreview"));
        var projects=(cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService)ReflectionTestUtils.getField(query,"projectAccessService");
        doThrow(new IllegalStateException("permission directory unavailable")).when(projects).assertProjectEditorOrOwner(99L,5L);
        var failure=assertThrows(IllegalStateException.class,()->ReflectionTestUtils.invokeMethod(query,"getRelationPermissions",99L,23L));
        assertEquals("permission directory unavailable",failure.getMessage());
    }
    @Test void keywordCountAndPageUseSameServerFilters() {
        var result=query.getControlledFileSelectorPage(99L,q(null,null,null,"Other project",1,1));
        assertEquals(0L,result.getTotal());assertTrue(result.getList().isEmpty());
        var empty=query.getControlledFileSelectorPage(99L,q(null,null,null,"Other project",2,1));
        assertEquals(0L,empty.getTotal());assertTrue(empty.getList().isEmpty());
    }
    @Test void missingCurrentIsNeverReplacedWithHistoricalActiveOrWorkingVersion() {
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL WHERE id=10");
        var result=query.getControlledFileSelectorPage(99L,q(null,null,null,null,1,20));
        assertEquals(1L,result.getTotal()); assertTrue(result.getList().stream().noneMatch(r -> r.masterId().equals(10L)));
    }
    @Test void projectFolderEqualToNasDirectoryMustNotBeInferredAsMapping() {
        assertEquals(0L,query.getControlledFileSelectorPage(99L,q(5L,null,5L,null,1,20)).getTotal());
    }
    @Test void tenantAndPageBoundariesRemainServerControlled() {
        TenantContextHolder.setTenantId(2L); assertEquals(0L,query.getControlledFileSelectorPage(99L,q(null,null,null,null,1,20)).getTotal());
        assertThrows(IllegalArgumentException.class,() -> query.getControlledFileSelectorPage(99L,q(null,null,null,null,1,-1)));
    }
    @Test void hardAssignedFileScopeFiltersNamesBeforeSqlTotalAndPagination() {
        var scope=mock(DccControlledFileAssignmentScopeService.class);
        when(scope.isWithinAssignedFileScope(eq(99L),anyLong())).thenAnswer(i -> i.<Long>getArgument(1).equals(23L));
        ReflectionTestUtils.setField(query,"assignmentScopeService",scope);
        var result=query.getControlledFileSelectorPage(99L,q(null,null,null,null,1,20));
        assertEquals(1L,result.getTotal());assertEquals(List.of(23L),result.getList().stream().map(DccControlledFileSelectorRow::controlledFileId).toList());
    }
    @Test void selectorResponseKeepsLargeBusinessIdsAsStringsWithoutStorageCapabilities() throws Exception {
        Long large=9007199254740993L;
        var row=new DccControlledFileSelectorRow(1L,large,large,large,large,5L,"Source",6L,"NAS",null,null,
                "N1","SOP.pdf","A/1","ACTIVE",true,false,true,false);
        var json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(row);
        var tree=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(json);
        assertTrue(tree.get("controlledFileId").isTextual());assertEquals("9007199254740993",tree.get("controlledFileId").asText());
        assertTrue(tree.get("sourceDirectoryId").isTextual());
        assertTrue(tree.get("currentActiveControlledFileId").isTextual());
        assertFalse(tree.has("publishedFileId"));assertFalse(tree.has("sourceFileId"));assertFalse(tree.has("fileUrl"));
    }
    @AfterEach void clearRoundFixture() {
        jdbc.update("DELETE FROM dcc_application_round_link");
        TenantContextHolder.clear();
    }
    private DccControlledFileSelectorQuery q(Long p,Long d,Long f,String k,int n,int s){return new DccControlledFileSelectorQuery(p,d,f,k,n,s);}
    private void master(Long id,Long project,Long execution,Long latest){
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,status,current_active_controlled_file_id,latest_controlled_file_id) VALUES(?,1,2,5,?,?,?,'ACTIVE_CHAIN',?,?)",id,"M"+id,"N"+id,project,execution,latest);
    }
    private void file(Long id,Long master,Long project,Long directory,String version,String status,Long requester){
        controlledFileMapper.insert(DccControlledFileDO.builder().id(id).tenantId(1L).masterId(master).categoryId(2L)
                .directoryId(directory).dccProjectCodeId(project).sourceFileId(100L).originalFileId(100L).publishedFileId(501L)
                .title("Title"+id).fileName("Template"+id).sourceOriginalFileName("SOURCE-"+id+".pdf").fileNumber("N"+id)
                .versionNo(version).status(status).requesterId(requester).submitterId(requester)
                .controlledTime("WORKING".equals(status)?null:LocalDateTime.of(2026,10,1,12,0))
                .activatedTime("ACTIVE".equals(status)?LocalDateTime.of(2026,10,1,12,1):null).build());
    }
}
