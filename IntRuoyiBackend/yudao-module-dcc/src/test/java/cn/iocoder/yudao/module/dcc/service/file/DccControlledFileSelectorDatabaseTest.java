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
        var storage=mock(FileMapper.class); when(storage.selectById(501L)).thenReturn(FileDO.builder().id(501L).name("controlled.pdf").type("application/pdf").build());
        ReflectionTestUtils.setField(query,"fileMapper",storage);
        var settings=mock(DccControlledFileBrowserSettingsService.class);when(settings.getBlacklistedExtensionPatterns()).thenReturn(List.of());
        ReflectionTestUtils.setField(query,"browserSettingsService",settings);
    }
    @Test void selectorBlacklistUsesActualSourceExtensionBeforeAuthorizedTotalAndPage() {
        jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name='Internal.DB',file_name='Template.pdf' WHERE id=21");
        jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name='SOP.PDF',file_name='Template.db' WHERE id=22");
        var settings = (DccControlledFileBrowserSettingsService) ReflectionTestUtils.getField(query, "browserSettingsService");
        when(settings.getBlacklistedExtensionPatterns()).thenReturn(List.of("*.db"));
        var first = query.getControlledFileSelectorPage(99L, q(null, null, null, null, 1, 1));
        assertEquals(2L, first.getTotal()); assertEquals(22L, first.getList().get(0).controlledFileId());
        assertEquals("SOP.PDF", first.getList().get(0).fileName());
        var second = query.getControlledFileSelectorPage(99L, q(null, null, null, null, 2, 1));
        assertEquals(2L, second.getTotal()); assertEquals(23L, second.getList().get(0).controlledFileId());
    }
    @Test void actualBrowserSqlIncludesFutureLatestAndIgnoresWorkingBeforePagination() {
        ReflectionTestUtils.setField(query, "controlledFileMapper", controlledFileMapper);
        ReflectionTestUtils.setField(query, "controlledFileMasterMapper", masterMapper);
        ReflectionTestUtils.setField(query, "directoryMapper", directoryMapper);
        var request = new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO();
        request.setLatestVersionOnly(true); request.setPageNo(1); request.setPageSize(10);
        var result = query.getControlledFileBrowserPage(99L, request);
        assertEquals(3L, result.getTotal());
        assertEquals(Set.of(21L, 22L, 23L), result.getList().stream().map(row -> row.getId()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(result.getList().stream().noneMatch(row -> row.getId().equals(20L) || row.getId().equals(24L)));
        var workflowPage = query.getControlledFilePage(99L, request);
        assertEquals(3L, workflowPage.getTotal());
        assertEquals(Set.of(21L, 22L, 23L), workflowPage.getList().stream().map(row -> row.getId()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(20L, masterMapper.selectById(10L).getCurrentActiveControlledFileId());
        var future = result.getList().stream().filter(row -> row.getId().equals(21L)).findFirst().orElseThrow();
        assertEquals("CONTROLLED_PENDING_EFFECTIVE", future.getStatus());
        assertFalse(Boolean.TRUE.equals(future.getCanDownload()));
        assertFalse(Boolean.TRUE.equals(future.getCanPrint()));
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=NULL WHERE id=10");
        result = query.getControlledFileBrowserPage(99L, request);
        assertEquals(2L, result.getTotal());
        assertTrue(result.getList().stream().noneMatch(row -> row.getMasterId().equals(10L)));
    }
    @Test void actualBrowserDefaultSourceSearchAndExplicitWorkingFilterRemainDistinct() {
        ReflectionTestUtils.setField(query, "controlledFileMapper", controlledFileMapper);
        ReflectionTestUtils.setField(query, "controlledFileMasterMapper", masterMapper);
        ReflectionTestUtils.setField(query, "directoryMapper", directoryMapper);
        var request = new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO();
        request.setKeyword("SOURCE-21.pdf"); request.setPageNo(1); request.setPageSize(10);
        var result = query.getControlledFileBrowserPage(99L, request);
        assertEquals(1L, result.getTotal()); assertEquals(21L, result.getList().get(0).getId());
        assertNull(request.getLatestVersionOnly()); assertEquals("SOURCE-21.pdf", request.getKeyword());
        request.setKeyword(null); request.setStatus("WORKING");
        result = query.getControlledFileBrowserPage(99L, request);
        assertEquals(1L, result.getTotal()); assertEquals(24L, result.getList().get(0).getId());
        assertEquals("WORKING", result.getList().get(0).getStatus());
    }
    @Test void browserSelectorReadsFormalPlacementWithSqlTotalAndNameOnlyBodyIsolation() {
        jdbc.update("DELETE FROM dcc_project_file_placement");
        jdbc.update("INSERT INTO dcc_project_folder(id,tenant_id,project_code_id,parent_id,name,sort_order,active,source_template_id,source_node_key,deleted) VALUES(500,1,5,0,'Logical SOP',0,1,1,'c-test-500',0),(501,1,6,0,'Other logical',0,1,1,'c-test-501',0)");
        jdbc.update("INSERT INTO dcc_project_file_placement(id,tenant_id,project_code_id,project_folder_id,controlled_file_id,storage_directory_id) VALUES(1,1,5,500,21,5),(2,1,6,501,22,5)");
        var request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO();
        request.setSelectorScope("PROJECT_FOLDER");request.setDccProjectCodeId(5L);request.setProjectFolderId(500L);request.setPageNo(1);request.setPageSize(1);
        var result=query.getControlledFileBrowserPage(99L,request);
        assertEquals(1L,result.getTotal());assertEquals(21L,result.getList().get(0).getId());
        assertEquals(500L,result.getList().get(0).getProjectFolderId());assertEquals("Logical SOP",result.getList().get(0).getProjectFolderName());
        assertEquals("Source project",result.getList().get(0).getProjectName());
        assertEquals(21L,result.getList().get(0).getLatestControlledFileId());assertTrue(result.getList().get(0).getPendingEffect());
        request.setPageNo(2);result=query.getControlledFileBrowserPage(99L,request);assertEquals(1L,result.getTotal());assertTrue(result.getList().isEmpty());
        request.setSelectorScope("GLOBAL");request.setProjectFolderId(null);request.setDccProjectCodeId(null);request.setPageNo(1);request.setPageSize(20);
        result=query.getControlledFileBrowserPage(99L,request);assertEquals(3L,result.getTotal());
        var unknown=result.getList().stream().filter(r -> r.getId().equals(23L)).findFirst().orElseThrow();
        assertNull(unknown.getProjectFolderId());assertNull(unknown.getProjectFolderName());assertFalse(unknown.getCanPreview());
        assertNull(unknown.getSourceSha256());assertNull(unknown.getChangeDescription());
        request.setSelectorScope("PROJECT_FOLDER");request.setDccProjectCodeId(5L);request.setProjectFolderId(5L);
        assertEquals(0L,query.getControlledFileBrowserPage(99L,request).getTotal()); // NAS ID5 is not folder500.
        request.setSelectorScope(null);
        assertThrows(IllegalArgumentException.class,() -> query.getControlledFileBrowserPage(99L,request));
    }
    @Test void globalPageReturnsOneLatestPerMasterAndTrueAuthorizedTotal() {
        var first=query.getControlledFileSelectorPage(99L,q(null,null,null,null,1,2));
        assertEquals(3L,first.getTotal()); assertEquals(List.of(21L,22L),first.getList().stream().map(DccControlledFileSelectorRow::controlledFileId).toList());
        assertTrue(first.getList().stream().allMatch(DccControlledFileSelectorRow::pendingEffect));
        var next=query.getControlledFileSelectorPage(99L,q(null,null,null,null,2,2));
        assertEquals(3L,next.getTotal()); assertEquals(List.of(23L),next.getList().stream().map(DccControlledFileSelectorRow::controlledFileId).toList());
        assertFalse(next.getList().get(0).canPreview()); assertTrue(next.getList().get(0).controlled());
    }
    @Test void realFrozenSnapshotReadUsesFileViewAndPersistedRoundWithoutProjectDefaultsOrEditor() {
        ReflectionTestUtils.setField(query,"controlledFileMapper",controlledFileMapper);
        ReflectionTestUtils.setField(query,"applicationEvidenceMapper",evidenceMapper);
        var guard=new DccControlledFileDetailAuthorizationGuard();
        for(String field:new String[]{"assignmentScopeService","directoryAccessPermissionService","permissionSupport","viewMatrixAccessService","distributionRecipientMapper","routeSnapshotMapper","bpmTaskService"})
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
        var result=query.getApplicationEvidence(99L,23L,"REVISION","actual-history-bpm");
        assertTrue(result.recorded());assertEquals(7,result.attributeRound());
        assertEquals(List.of("CE"),result.defaultSource().targetMarkets());assertEquals(List.of("FDA"),result.actualAttributes().targetMarkets());
        var editor=(cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService)ReflectionTestUtils.getField(query,"projectAccessService");
        verifyNoInteractions(editor);
        jdbc.update("UPDATE dcc_project_application_attributes SET submitted=0 WHERE application_id=23");
        result=query.getApplicationEvidence(99L,23L,"REVISION","actual-history-bpm");
        assertFalse(result.recorded());assertEquals("NOT_FROZEN",result.unavailableReason());assertNull(result.actualAttributes());
    }
    @Test void projectionCarriesActualProjectSourceDirectoryAndIndependentPreviewPermission() {
        var result=query.getControlledFileSelectorPage(99L,q(5L,5L,null,null,1,20));
        assertEquals(2L,result.getTotal()); var row=result.getList().get(0);
        assertEquals(5L,row.projectId());assertEquals("Source project",row.projectName());
        assertEquals(5L,row.sourceDirectoryId());assertEquals("Source NAS folder",row.sourceDirectoryName());
        assertNull(row.projectFolderId()); assertNull(row.projectFolderName());
        assertEquals(row.controlledFileId(),row.latestControlledFileId()); assertEquals("SOURCE-21.pdf",row.fileName());
        assertTrue(row.canPreview());
    }

    @Test void applicationHistoryListsOnlyExactBoundRoundsAndExcludesReservedDrafts() {
        ReflectionTestUtils.setField(query,"controlledFileMapper",controlledFileMapper);
        ReflectionTestUtils.setField(query,"applicationEvidenceMapper",evidenceMapper);
        var guard=new DccControlledFileDetailAuthorizationGuard();
        for(String field:new String[]{"assignmentScopeService","directoryAccessPermissionService","permissionSupport","viewMatrixAccessService","distributionRecipientMapper","routeSnapshotMapper","bpmTaskService"})
            ReflectionTestUtils.setField(guard,field,ReflectionTestUtils.getField(query,field));
        var view=(DccControlledFileViewMatrixAccessService)ReflectionTestUtils.getField(query,"viewMatrixAccessService");
        when(view.canAccessCurrentViewMatrix(eq(99L),any(DccControlledFileDO.class))).thenReturn(true);
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",guard);
        jdbc.update("DELETE FROM dcc_application_round_link");
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,5,'UPLOAD',23,'upload-23',1),(1,5,'OBSOLETE',23,'obsolete-first',1),(1,5,'OBSOLETE',23,'obsolete-second',2),(1,5,'REVISION',23,NULL,1),(2,5,'OBSOLETE',23,'foreign-tenant',1),(1,6,'OBSOLETE',23,'foreign-project',3)");
        java.util.List<?> result=ReflectionTestUtils.invokeMethod(query,"listApplicationRounds",99L,23L);
        assertNotNull(result);assertEquals(3,result.size());
        var rows=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(result),java.util.Map.class);
        assertEquals(Set.of("upload-23","obsolete-first","obsolete-second"),rows.stream().map(row->row.get("bpmRound")).collect(java.util.stream.Collectors.toSet()));
        assertTrue(rows.stream().allMatch(row->"23".equals(row.get("controlledFileId"))));
        TenantContextHolder.setTenantId(2L);
        assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(query,"listApplicationRounds",99L,23L));
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
        assertEquals(1L,result.getTotal());assertEquals(22L,result.getList().get(0).controlledFileId());
        var empty=query.getControlledFileSelectorPage(99L,q(null,null,null,"Other project",2,1));
        assertEquals(1L,empty.getTotal());assertTrue(empty.getList().isEmpty());
    }
    @Test void missingLatestIsNeverReplacedWithExecutionOrWorkingVersion() {
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=NULL WHERE id=10");
        var result=query.getControlledFileSelectorPage(99L,q(null,null,null,null,1,20));
        assertEquals(2L,result.getTotal()); assertTrue(result.getList().stream().noneMatch(r -> r.masterId().equals(10L)));
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
        when(scope.isWithinAssignedFileScope(eq(99L),anyLong())).thenAnswer(i -> i.<Long>getArgument(1).equals(22L));
        ReflectionTestUtils.setField(query,"assignmentScopeService",scope);
        var result=query.getControlledFileSelectorPage(99L,q(null,null,null,null,1,20));
        assertEquals(1L,result.getTotal());assertEquals(List.of(22L),result.getList().stream().map(DccControlledFileSelectorRow::controlledFileId).toList());
    }
    @Test void selectorResponseKeepsLargeBusinessIdsAsStringsWithoutStorageCapabilities() throws Exception {
        Long large=9007199254740993L;
        var row=new DccControlledFileSelectorRow(1L,large,large,large,5L,"Source",6L,"NAS",null,null,
                "N1","SOP.pdf","A/1","ACTIVE",true,false,true,false);
        var json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(row);
        var tree=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(json);
        assertTrue(tree.get("controlledFileId").isTextual());assertEquals("9007199254740993",tree.get("controlledFileId").asText());
        assertTrue(tree.get("sourceDirectoryId").isTextual());
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
