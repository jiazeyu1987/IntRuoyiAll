package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import java.lang.reflect.Modifier;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
class DccRelationNameMetadataDatabaseTest extends BaseDbUnitTest {
    @Resource javax.sql.DataSource source;
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource DccProjectCodeMapper projects;
    @Resource DccProjectFolderMapper folders;
    @Resource DccProjectFilePlacementMapper placements;
    @Resource DccProjectFolderStorageMappingMapper storageMappings;
    @Resource DccControlledFileRelatedFileMapper historicalRelations;
    DccControlledFileQueryServiceImpl query;JdbcTemplate jdbc;
    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(source);
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,project_code,status) VALUES(5,1,'真实来源项目','P5','ENABLE')");
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,file_name,file_number,dcc_project_code_id,status) VALUES(10,1,2,'SOP','N-1',5,'ACTIVE_CHAIN')");
        files.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder().id(9007199254740993L).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L).sourceFileId(100L).originalFileId(100L).fileName("SOP").title("SOP").sourceOriginalFileName("Source.pdf").fileNumber("N-1").versionNo("A/1").status("ACTIVE").dccProjectCodeId(5L).requesterId(88L).submitterId(88L).publishedFileId(200L).controlledTime(java.time.LocalDateTime.now()).build());
        jdbc.update("INSERT INTO dcc_project_folder(id,tenant_id,project_code_id,parent_id,name,sort_order,active,source_template_id,source_node_key,deleted) VALUES(500,1,5,0,'正式逻辑目录',0,1,1,'root',0)");
        jdbc.update("INSERT INTO dcc_project_file_placement(id,tenant_id,project_code_id,project_folder_id,controlled_file_id,storage_directory_id) VALUES(1,1,5,500,9007199254740993,3)");
        query=new DccControlledFileQueryServiceImpl();for(var f:query.getClass().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers()) && f.getAnnotation(Resource.class)!=null)ReflectionTestUtils.setField(query,f.getName(),mock(f.getType()));
        ReflectionTestUtils.setField(query,"controlledFileMapper",files);ReflectionTestUtils.setField(query,"controlledFileMasterMapper",masters);ReflectionTestUtils.setField(query,"projectCodeMapper",projects);
        ReflectionTestUtils.setField(query,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy());
        if(Arrays.stream(query.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals("projectFilePlacementMapper")))ReflectionTestUtils.setField(query,"projectFilePlacementMapper",placements);
        if(Arrays.stream(query.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals("projectFolderMapper")))ReflectionTestUtils.setField(query,"projectFolderMapper",folders);
        ReflectionTestUtils.setField(query,"projectStorageMappingMapper",storageMappings);
        var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(any(),any())).thenReturn(true);ReflectionTestUtils.setField(query,"assignmentScopeService",scope);
        var permissions=mock(DccDirectoryAccessPermissionService.class);when(permissions.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.QUERY)).thenReturn(Set.of(3L));when(permissions.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of());ReflectionTestUtils.setField(query,"directoryAccessPermissionService",permissions);
        var guard=new DccControlledFileDetailAuthorizationGuard();
        for(String field:new String[]{"assignmentScopeService","directoryAccessPermissionService","permissionSupport","viewMatrixAccessService","distributionRecipientMapper","routeSnapshotMapper","bpmTaskService"})ReflectionTestUtils.setField(guard,field,ReflectionTestUtils.getField(query,field));
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",guard);
    }
    @AfterEach void clear(){jdbc.update("DELETE FROM dcc_project_file_placement");jdbc.update("DELETE FROM dcc_project_folder_storage_mapping");TenantContextHolder.clear();}

    @Test void authorizedMainDetailDoesNotEagerlyReadAnUnauthorizedHistoricalRelationAndExplicitHistoryStaysStrict() throws Exception {
        jdbc.update("UPDATE dcc_controlled_file SET version_no='A/2' WHERE id=9007199254740993");
        var directories=(DccDirectoryAccessPermissionService)ReflectionTestUtils.getField(query,"directoryAccessPermissionService");
        when(directories.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of(3L));
        ReflectionTestUtils.setField(query,"downloadPolicyService",new cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService());
        assertTrue(query.canViewFileName(99L,files.selectById(9007199254740993L)));
        var target=new cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO();
        org.springframework.beans.BeanUtils.copyProperties(files.selectById(9007199254740993L),target);
        target.setId(300L);target.setDirectoryId(4L);target.setStatus("OBSOLETE");target.setVersionNo("A/1");
        target.setObsoletedTime(java.time.LocalDateTime.of(2026,10,1,12,0));files.insert(target);
        jdbc.update("INSERT INTO dcc_controlled_file_related_file(controlled_file_id,related_controlled_file_id,project_code_id,related_master_id,related_file_number_snapshot,related_file_name_snapshot,related_version_no_snapshot,relation_source,tenant_id) VALUES(9007199254740993,300,5,10,'N-1','Old historical.pdf','A/1','UPLOAD',1)");
        var resolver=new cn.iocoder.yudao.module.dcc.service.file.relations.DccLatestControlledFileResolverImpl();
        ReflectionTestUtils.setField(resolver,"fileMapper",files);ReflectionTestUtils.setField(resolver,"masterMapper",masters);ReflectionTestUtils.setField(resolver,"jdbc",jdbc);
        var policy=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationAccessPolicyImpl();
        ReflectionTestUtils.setField(policy,"query",query);
        var relations=new DccControlledFileRelatedFileServiceImpl();
        ReflectionTestUtils.setField(relations,"controlledFileMapper",files);ReflectionTestUtils.setField(relations,"controlledFileMasterMapper",masters);
        ReflectionTestUtils.setField(relations,"relatedFileMapper",historicalRelations);ReflectionTestUtils.setField(relations,"latestFileResolver",resolver);
        ReflectionTestUtils.setField(relations,"relationAccessPolicy",policy);ReflectionTestUtils.setField(query,"relatedFileService",relations);
        var fileRows=normalizeOriginalRows(jdbc.queryForList("SELECT * FROM dcc_controlled_file ORDER BY id"));
        var relationRows=normalizeOriginalRows(jdbc.queryForList("SELECT * FROM dcc_controlled_file_related_file ORDER BY id"));
        var controller=new DccControlledFileController();ReflectionTestUtils.setField(controller,"queryService",query);
        try(var login=mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var response=query.getControlledFile(99L,9007199254740993L);
            assertNull(response.getRelatedFiles(),"the unused legacy eager projection must not become a fake empty list");
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/9007199254740993"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(0));
            var denied=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    ()->relations.listHistoricalRelatedFiles(99L,9007199254740993L));
            assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED.getCode(),denied.getCode());
            var scope=(DccControlledFileAssignmentScopeService)ReflectionTestUtils.getField(query,"assignmentScopeService");
            when(scope.isWithinAssignedFileScope(99L,9007199254740993L)).thenReturn(false);
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.getControlledFile(99L,9007199254740993L));
        }
        assertEquals(fileRows,normalizeOriginalRows(jdbc.queryForList("SELECT * FROM dcc_controlled_file ORDER BY id")));
        assertEquals(relationRows,normalizeOriginalRows(jdbc.queryForList("SELECT * FROM dcc_controlled_file_related_file ORDER BY id")));
    }
    private List<Map<String,Object>> normalizeOriginalRows(List<Map<String,Object>> rows) {
        return rows.stream().map(row->{Map<String,Object> normalized=new TreeMap<>();
            row.forEach((key,value)->normalized.put(key,value instanceof byte[] bytes
                    ? "BINARY:"+java.util.HexFormat.of().formatHex(bytes):value));return normalized;}).toList();
    }

    @Test void publicDetailExposesObsoleteForARealControlledPendingVersion() throws Exception {
        prepareFormalControlledPending();
        var before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=9007199254740993"));
        var controller=new DccControlledFileController();ReflectionTestUtils.setField(controller,"queryService",query);
        try(var login=mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var response=MockMvcBuilders.standaloneSetup(controller).build()
                    .perform(get("/dcc/controlled-files/9007199254740993")).andReturn();
            var data=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),com.fasterxml.jackson.databind.JsonNode.class).get("data");
            assertTrue(data.get("canObsolete").booleanValue(),"A genuine controlled future version must expose the authorized obsolete action");
            assertTrue(data.get("actionProjection").get("allowedActions").toString().contains("OBSOLETE"));
            assertEquals("9007199254740993",data.get("id").textValue());
        }
        var file=files.selectById(9007199254740993L);
        var browser=(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO)
                ReflectionTestUtils.invokeMethod(query,"toBrowserRespVO",99L,file,false,List.of(),null,Map.of());
        assertTrue(browser.getCanObsolete());assertTrue(browser.getActionProjection().getAllowedActions().contains("OBSOLETE"));
        var history=browser.getVersionHistory().stream().filter(row->row.getId().equals(file.getId())).findFirst().orElseThrow();
        assertTrue(history.getActionProjection().getAllowedActions().contains("OBSOLETE"));
        assertEquals(before,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=9007199254740993")));
    }

    private void prepareFormalControlledPending() {
        jdbc.update("UPDATE dcc_controlled_file SET status='CONTROLLED_PENDING_EFFECTIVE',published_file_id=200,stamped_file_id=200,"
                +"published_time=controlled_time,approved_time=controlled_time,activated_time=NULL,effective_date=?,"
                +"process_instance_id='formal-upload-round',process_definition_key='dcc-controlled-file-upload',file_type_taxonomy_id=6 WHERE id=9007199254740993",java.time.LocalDate.now().plusDays(1));
        jdbc.update("UPDATE dcc_controlled_file_master SET file_type_taxonomy_leaf_id=6,normalized_file_number='N-1',latest_controlled_file_id=9007199254740993 WHERE id=10");
        var detail=mock(DccControlledFileDetailAuthorizationGuard.class);
        when(detail.isAllowed(any(),any(),anyBoolean(),any())).thenReturn(true);
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",detail);
        ReflectionTestUtils.setField(query,"downloadPolicyService",new cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService());
        var permission=(DccControlledFileCategoryPermissionSupport)ReflectionTestUtils.getField(query,"permissionSupport");
        when(permission.hasCategoryPermission(2L,99L,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.OBSOLETE)).thenReturn(true);
    }

    @ParameterizedTest @ValueSource(strings={"CONTROL_TIME","PUBLISHED","STAMPED","WORKING_VERSION","MASTER_TYPE"})
    void pendingWithoutActualFormalControlledFactsNeverReceivesTheObsoleteAction(String missing) {
        prepareFormalControlledPending();
        switch(missing) {
            case "CONTROL_TIME"->jdbc.update("UPDATE dcc_controlled_file SET controlled_time=NULL WHERE id=9007199254740993");
            case "PUBLISHED"->jdbc.update("UPDATE dcc_controlled_file SET published_file_id=NULL WHERE id=9007199254740993");
            case "STAMPED"->jdbc.update("UPDATE dcc_controlled_file SET stamped_file_id=NULL WHERE id=9007199254740993");
            case "WORKING_VERSION"->jdbc.update("UPDATE dcc_controlled_file SET version_no='A/1-1' WHERE id=9007199254740993");
            case "MASTER_TYPE"->jdbc.update("UPDATE dcc_controlled_file_master SET file_type_taxonomy_leaf_id=777 WHERE id=10");
            default->fail("Unknown explicit field");
        }
        var response=query.getControlledFile(99L,9007199254740993L);
        assertFalse(response.getCanObsolete());assertFalse(response.getActionProjection().getAllowedActions().contains("OBSOLETE"));
    }

    @Test void pendingFormLockBlocksAnotherObsoleteRequestAndPreservesExistingApplicantWithdrawal() {
        prepareFormalControlledPending();
        var form=new cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO();
        form.setId(9007199254740997L);form.setApplicantUserId(99L);form.setTenantId(1L);form.setObjectId("9007199254740993");
        form.setObjectVersion("A/1");form.setStatus("IN_APPROVAL");form.setActionCode("OBSOLETE");
        var pending=(DccControlledFileFormActionPendingService)ReflectionTestUtils.getField(query,"formActionPendingService");
        when(pending.findOpenObsoleteAction(9007199254740993L)).thenReturn(form);
        var response=query.getControlledFile(99L,9007199254740993L);
        assertTrue(response.getCanObsolete());assertFalse(response.getActionProjection().getAllowedActions().contains("OBSOLETE"));
        assertEquals("OBSOLETE_APPROVAL_PENDING",response.getActionProjection().getActionLockReason());
        assertEquals(9007199254740997L,response.getActionProjection().getPendingRequestId());assertTrue(response.getActionProjection().getCanWithdraw());
    }

    @Test void pendingPermissionAndMetadataOnlyReadsDoNotGrantObsoletePrintOrMajorRevision() {
        prepareFormalControlledPending();
        var permission=(DccControlledFileCategoryPermissionSupport)ReflectionTestUtils.getField(query,"permissionSupport");
        when(permission.hasCategoryPermission(2L,99L,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.OBSOLETE)).thenReturn(false);
        var response=query.getControlledFile(99L,9007199254740993L);assertFalse(response.getCanObsolete());
        assertFalse(response.getActionProjection().getAllowedActions().contains("OBSOLETE"));
        assertFalse(response.getActionProjection().getAllowedActions().contains("PRINT"));
        assertFalse(response.getActionProjection().getAllowedActions().contains("MAJOR_REVISION"));
        assertFalse(response.getActionProjection().getAllowedActions().contains("MANUAL_RELEASE"));
        when(permission.hasCategoryPermission(2L,99L,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.OBSOLETE)).thenReturn(true);
        var readOnly=ReflectionTestUtils.invokeMethod(query,"toNameOnlyResponseWithHistory",99L,files.selectById(9007199254740993L),
                false,List.of(),null);
        var nameOnly=(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO)readOnly;
        assertFalse(nameOnly.getCanObsolete());assertFalse(nameOnly.getActionProjection().getAllowedActions().contains("OBSOLETE"));
    }
    @Test void authorizedDetailProjectsCurrentRegisteredFolderFromItsOwnFileLocation() {
        var before=jdbc.queryForMap("SELECT * FROM dcc_project_file_placement WHERE id=1");
        var response=authorizedDetail();
        assertEquals(500L,response.getProjectFolderId(),"Authorized detail must expose the actual current file placement");
        assertEquals("正式逻辑目录",response.getProjectFolderName());
        var json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(response),com.fasterxml.jackson.databind.JsonNode.class);
        assertTrue(json.get("hasProjectStorageMapping").booleanValue());
        assertEquals("500",json.get("projectFolderId").textValue());
        assertEquals(before,jdbc.queryForMap("SELECT * FROM dcc_project_file_placement WHERE id=1"));
    }
    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO authorizedDetail() {
        var detail=mock(DccControlledFileDetailAuthorizationGuard.class);
        when(detail.isAllowed(any(),any(),anyBoolean(),any())).thenReturn(true);
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",detail);
        ReflectionTestUtils.setField(query,"downloadPolicyService",new cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService());
        return query.getControlledFile(99L,9007199254740993L);
    }
    @Test void historicalProjectWithoutOwnPlacementDoesNotGuessAFolderAndIsNormallyPhysical() {
        jdbc.update("DELETE FROM dcc_project_file_placement WHERE id=1");
        var response=authorizedDetail();
        assertFalse(response.isHasProjectStorageMapping());assertNull(response.getProjectFolderId());assertNull(response.getProjectFolderName());
        assertEquals(5L,response.getDccProjectCodeId());
    }
    @Test void exactMappedDirectoryWithMissingOwnPlacementIsAnErrorRatherThanLegacyFallback() {
        jdbc.update("INSERT INTO dcc_project_folder_storage_mapping(tenant_id,project_code_id,project_folder_id,category_id,base_directory_id,storage_directory_id) VALUES(1,5,500,2,2,3)");
        jdbc.update("DELETE FROM dcc_project_file_placement WHERE id=1");
        assertEquals("DCC_FILE_PROJECT_LOCATION_MISSING",assertThrows(IllegalStateException.class,this::authorizedDetail).getMessage());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_placement",Integer.class));
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"FOREIGN_PLACEMENT","WRONG_PROJECT","WRONG_STORAGE","FOREIGN_FOLDER","INACTIVE_FOLDER","DELETED_FOLDER"})
    void brokenCurrentPlacementCannotBorrowAnotherFolder(String invalid) {
        switch(invalid) {
            case "FOREIGN_PLACEMENT"->{
                jdbc.update("INSERT INTO dcc_project_folder_storage_mapping(tenant_id,project_code_id,project_folder_id,category_id,base_directory_id,storage_directory_id) VALUES(1,5,500,2,2,3)");
                jdbc.update("UPDATE dcc_project_file_placement SET tenant_id=2 WHERE id=1");
            }
            case "WRONG_PROJECT"->jdbc.update("UPDATE dcc_project_file_placement SET project_code_id=777 WHERE id=1");
            case "WRONG_STORAGE"->jdbc.update("UPDATE dcc_project_file_placement SET storage_directory_id=777 WHERE id=1");
            case "FOREIGN_FOLDER"->jdbc.update("UPDATE dcc_project_folder SET tenant_id=2 WHERE id=500");
            case "INACTIVE_FOLDER"->jdbc.update("UPDATE dcc_project_folder SET active=0 WHERE id=500");
            case "DELETED_FOLDER"->jdbc.update("UPDATE dcc_project_folder SET deleted=1 WHERE id=500");
            default->fail("Unknown explicit scenario");
        }
        assertEquals("FOREIGN_PLACEMENT".equals(invalid)?"DCC_FILE_PROJECT_LOCATION_MISSING":"DCC_FILE_PROJECT_LOCATION_INVALID",
                assertThrows(IllegalStateException.class,this::authorizedDetail).getMessage());
    }
    @Test void existingDerivedLocationMustMatchTheFilesExactCategoryAndFolder() {
        jdbc.update("INSERT INTO dcc_project_folder_storage_mapping(tenant_id,project_code_id,project_folder_id,category_id,base_directory_id,storage_directory_id) VALUES(1,5,500,99,2,3)");
        assertEquals("DCC_FILE_PROJECT_STORAGE_MAPPING_INVALID",assertThrows(IllegalStateException.class,this::authorizedDetail).getMessage());
    }
    @Test void nameOnlyHistoricalTargetMetadataDoesNotRequireDetailOrGrantBodyAndCarriesExactParents() throws Exception {
        assertThrows(RuntimeException.class,()->query.getControlledFile(99L,9007199254740993L));
        var result=query.getRelationPermissions(99L,9007199254740993L);assertFalse(result.canEdit());assertFalse(result.canPreview());
        assertEquals(1L,(Object)ReflectionTestUtils.invokeMethod(result,"tenantId"));assertEquals(10L,(Object)ReflectionTestUtils.invokeMethod(result,"masterId"));assertEquals(5L,(Object)ReflectionTestUtils.invokeMethod(result,"projectId"));
        assertEquals("真实来源项目",ReflectionTestUtils.invokeMethod(result,"projectName"));assertEquals(500L,(Object)ReflectionTestUtils.invokeMethod(result,"projectFolderId"));assertEquals("正式逻辑目录",ReflectionTestUtils.invokeMethod(result,"projectFolderName"));
        var controller=new DccControlledFileController();ReflectionTestUtils.setField(controller,"queryService",query);
        try(var login=mockStatic(SecurityFrameworkUtils.class)){login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var response=MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/9007199254740993/relation-permissions")).andReturn();
            var json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),com.fasterxml.jackson.databind.JsonNode.class);
            assertEquals("9007199254740993",json.get("data").get("controlledFileId").textValue());assertEquals("10",json.get("data").get("masterId").textValue());assertEquals("500",json.get("data").get("projectFolderId").textValue());
            assertTrue(json.get("data").get("hasCurrentControlledSource").isBoolean());
            assertFalse(json.get("data").get("hasCurrentControlledSource").booleanValue());
        }
    }
    @Test void workingSelectionWithAnActualLatestControlledSourceMustExposeTheServerSourceFact() {
        jdbc.update("UPDATE dcc_controlled_file SET status='WORKING',controlled_time=NULL,version_no='A/1-1' WHERE id=9007199254740993");
        files.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder().id(20L).tenantId(1L)
                .masterId(10L).categoryId(2L).directoryId(3L).sourceFileId(100L).originalFileId(100L).fileName("CurrentSource.pdf").title("Current")
                .fileNumber("N-1").versionNo("A/1").status("ACTIVE").dccProjectCodeId(5L).requesterId(88L).submitterId(88L)
                .controlledTime(java.time.LocalDateTime.of(2026,10,5,10,0)).build());
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20 WHERE id=10");
        var result=query.getRelationPermissions(99L,9007199254740993L);
        var json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(result),Map.class);
        assertEquals(Boolean.TRUE,json.get("hasCurrentControlledSource"));
        assertFalse(result.controlled());assertFalse(result.canEdit());assertFalse(result.canPreview());
    }

    @Test void nullLatestPointerIsExplicitNoSourceAndDoesNotChangeSelectedHistoricalFacts() {
        var before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(files.selectById(9007199254740993L));
        var result=query.getRelationPermissions(99L,9007199254740993L);
        assertFalse(result.hasCurrentControlledSource());assertTrue(result.controlled());
        assertFalse(result.canEdit());assertFalse(result.canPreview());
        assertEquals(before,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(files.selectById(9007199254740993L)));
        assertNull(masters.selectById(10L).getLatestControlledFileId());
    }

    @ParameterizedTest @ValueSource(strings={"ACTIVE","CONTROLLED_PENDING_EFFECTIVE","OBSOLETE"})
    void exactFormalLatestSourceUsesItsOwnStateAndObsoleteIsACompleteTerminalHistory(String status) {
        files.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder().id(20L).tenantId(1L)
                .masterId(10L).categoryId(2L).directoryId(3L).sourceFileId(100L).originalFileId(100L).fileName("Head.pdf").title("Head")
                .fileNumber("N-1").versionNo("A/2").status(status).dccProjectCodeId(5L).requesterId(88L).submitterId(88L)
                .controlledTime(java.time.LocalDateTime.of(2026,10,5,10,0))
                .obsoletedTime("OBSOLETE".equals(status)?java.time.LocalDateTime.of(2026,10,5,11,0):null).build());
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20 WHERE id=10");
        String before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(files.selectById(9007199254740993L));
        var result=query.getRelationPermissions(99L,9007199254740993L);
        assertEquals(!"OBSOLETE".equals(status),result.hasCurrentControlledSource());
        assertFalse(result.canEdit());assertFalse(result.canPreview());
        assertEquals(before,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(files.selectById(9007199254740993L)));
        assertEquals(20L,masters.selectById(10L).getLatestControlledFileId());
    }

    @ParameterizedTest @ValueSource(strings={"missing","foreignTenant","otherMaster","otherProject","notControlled","workingVersion","invalidVersion","noncurrentStatus","obsoleteMissingTime","deleted"})
    void invalidLatestFactsCannotBeReportedAsFalseOrBorrowCurrentExecution(String invalid) {
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20,current_active_controlled_file_id=9007199254740993 WHERE id=10");
        files.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder().id(20L).tenantId(1L)
                .masterId(10L).categoryId(2L).directoryId(3L).sourceFileId(100L).originalFileId(100L).fileName("Head.pdf").title("Head")
                .fileNumber("N-1").versionNo("A/2").status("ACTIVE").dccProjectCodeId(5L).requesterId(88L).submitterId(88L)
                .controlledTime(java.time.LocalDateTime.of(2026,10,5,10,0)).build());
        switch(invalid) {
            case "missing" -> jdbc.update("DELETE FROM dcc_controlled_file WHERE id=20");
            case "foreignTenant" -> jdbc.update("UPDATE dcc_controlled_file SET tenant_id=122 WHERE id=20");
            case "otherMaster" -> jdbc.update("UPDATE dcc_controlled_file SET master_id=11 WHERE id=20");
            case "otherProject" -> jdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=6 WHERE id=20");
            case "notControlled" -> jdbc.update("UPDATE dcc_controlled_file SET controlled_time=NULL WHERE id=20");
            case "workingVersion" -> jdbc.update("UPDATE dcc_controlled_file SET version_no='A/2-1' WHERE id=20");
            case "invalidVersion" -> jdbc.update("UPDATE dcc_controlled_file SET version_no='invalid' WHERE id=20");
            case "noncurrentStatus" -> jdbc.update("UPDATE dcc_controlled_file SET status='SUPERSEDED' WHERE id=20");
            case "obsoleteMissingTime" -> jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE',obsoleted_time=NULL WHERE id=20");
            case "deleted" -> jdbc.update("UPDATE dcc_controlled_file SET deleted=1 WHERE id=20");
        }
        assertThrows(IllegalStateException.class,()->query.getRelationPermissions(99L,9007199254740993L));
        assertEquals(20L,masters.selectById(10L).getLatestControlledFileId());
        assertEquals(9007199254740993L,masters.selectById(10L).getCurrentActiveControlledFileId());
    }

    @ParameterizedTest @ValueSource(strings={"MASTER","PROJECT","PLACEMENT_PROJECT","PLACEMENT_NAS","FOLDER"})
    void missingOrMismatchedFormalMetadataCannotBecomeEmptySuccess(String invalid){
        switch(invalid){case "MASTER"->jdbc.update("UPDATE dcc_controlled_file_master SET dcc_project_code_id=9 WHERE id=10");case "PROJECT"->jdbc.update("DELETE FROM dcc_project_code WHERE id=5");case "PLACEMENT_PROJECT"->jdbc.update("UPDATE dcc_project_file_placement SET project_code_id=9 WHERE id=1");case "PLACEMENT_NAS"->jdbc.update("UPDATE dcc_project_file_placement SET storage_directory_id=999 WHERE id=1");case "FOLDER"->jdbc.update("UPDATE dcc_project_folder SET active=0 WHERE id=500");}
        assertThrows(IllegalStateException.class,()->query.getRelationPermissions(99L,9007199254740993L));
    }
}
