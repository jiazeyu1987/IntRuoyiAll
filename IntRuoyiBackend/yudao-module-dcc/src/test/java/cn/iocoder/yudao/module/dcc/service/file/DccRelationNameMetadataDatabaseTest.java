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
        var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(any(),any())).thenReturn(true);ReflectionTestUtils.setField(query,"assignmentScopeService",scope);
        var permissions=mock(DccDirectoryAccessPermissionService.class);when(permissions.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.QUERY)).thenReturn(Set.of(3L));when(permissions.getAuthorizedDirectoryIds(99L,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of());ReflectionTestUtils.setField(query,"directoryAccessPermissionService",permissions);
        var guard=new DccControlledFileDetailAuthorizationGuard();
        for(String field:new String[]{"assignmentScopeService","directoryAccessPermissionService","permissionSupport","viewMatrixAccessService","distributionRecipientMapper","routeSnapshotMapper","bpmTaskService"})ReflectionTestUtils.setField(guard,field,ReflectionTestUtils.getField(query,field));
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",guard);
    }
    @AfterEach void clear(){jdbc.update("DELETE FROM dcc_project_file_placement");TenantContextHolder.clear();}
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
