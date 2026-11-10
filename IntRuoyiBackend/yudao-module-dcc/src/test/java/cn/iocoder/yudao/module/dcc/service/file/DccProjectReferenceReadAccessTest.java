package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccProjectReferenceReadAccessTest {
    final DccProjectReferenceReadAccess access = new DccProjectReferenceReadAccess();
    final DccProjectAccessService projects = mock(DccProjectAccessService.class);
    JdbcTemplate jdbc;
    DccControlledFileDO file;

    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        var login=new cn.iocoder.yudao.framework.security.core.LoginUser();
        login.setId(99L);login.setTenantId(1L);login.setUserType(2);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login,null,List.of()));
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:reference_editor;MODE=MYSQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE dcc_project_file_reference(tenant_id BIGINT,project_id BIGINT,folder_id BIGINT,master_id BIGINT,selected_controlled_file_id BIGINT)");
        jdbc.execute("CREATE TABLE dcc_project_folder(id BIGINT,project_code_id BIGINT,tenant_id BIGINT,deleted INT,active INT)");
        jdbc.execute("CREATE TABLE dcc_project_code(id BIGINT,tenant_id BIGINT,deleted INT,status VARCHAR(20))");
        jdbc.update("INSERT INTO dcc_project_code VALUES(20,1,0,'ENABLE')");
        jdbc.update("INSERT INTO dcc_project_folder VALUES(21,20,1,0,1)");
        jdbc.update("INSERT INTO dcc_project_file_reference VALUES(1,20,21,700,910)");
        ReflectionTestUtils.setField(access,"jdbc",jdbc);
        ReflectionTestUtils.setField(access,"projectAccessService",projects);
        when(projects.listReadableProjectIds(99L)).thenReturn(List.of(20L));
        when(projects.hasProjectEditorOrOwner(99L,20L)).thenReturn(true);
        file=DccControlledFileDO.builder().id(910L).masterId(700L).dccProjectCodeId(10L).tenantId(1L)
                .status("ACTIVE").versionNo("A/1").publishedFileId(100L)
                .controlledTime(LocalDateTime.now()).activatedTime(LocalDateTime.now()).build();
    }
    @AfterEach void clear() {
        TenantContextHolder.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test void editorReadsOnlyPersistedExactFileAndRevocationTakesEffect() {
        assertTrue(access.canRead(99L,file));
        file.setId(911L); assertFalse(access.canRead(99L,file));
        file.setId(910L); file.setMasterId(701L); assertFalse(access.canRead(99L,file));
        file.setMasterId(700L); file.setTenantId(2L); assertFalse(access.canRead(99L,file));
        file.setTenantId(1L); jdbc.update("DELETE FROM dcc_project_file_reference");
        assertFalse(access.canRead(99L,file));
    }
    @Test void viewerAndOutsideAssignedTargetScopeCannotUseEditorGrant() {
        when(projects.hasProjectEditorOrOwner(99L,20L)).thenReturn(false);
        assertFalse(access.canRead(99L,file));
        when(projects.hasProjectEditorOrOwner(99L,20L)).thenReturn(true);
        when(projects.listReadableProjectIds(99L)).thenReturn(List.of());
        assertFalse(access.canRead(99L,file));
    }
    @Test void invalidTargetFolderAndUncontrolledOrArchivedFilesStayDenied() {
        jdbc.update("UPDATE dcc_project_folder SET active=0");assertFalse(access.canRead(99L,file));
        jdbc.update("UPDATE dcc_project_folder SET active=1,project_code_id=30");assertFalse(access.canRead(99L,file));
        jdbc.update("UPDATE dcc_project_folder SET project_code_id=20");
        for (String status:List.of("WORKING","CONTROLLED_PENDING_EFFECTIVE","SUPERSEDED","OBSOLETE")) {
            file.setStatus(status);assertFalse(access.canRead(99L,file));
        }
        file.setStatus("ACTIVE");file.setPublishedFileId(null);assertFalse(access.canRead(99L,file));
    }
    @Test void realDetailAndPreviewGuardsAllowReferenceButDoNotGrantDownloadOrAuthoring() {
        var guard=new DccControlledFileDetailAuthorizationGuard();
        ReflectionTestUtils.setField(guard,"projectReferenceReadAccess",access);
        var users=mock(AdminUserApi.class);
        when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        ReflectionTestUtils.setField(guard,"adminUserApi",users);
        ReflectionTestUtils.setField(guard,"permissionSupport",mock(DccControlledFileCategoryPermissionSupport.class));
        ReflectionTestUtils.setField(guard,"assignmentScopeService",mock(DccControlledFileAssignmentScopeService.class));
        var masters=mock(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper.class);
        when(masters.selectById(700L)).thenReturn(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO.builder()
                .id(700L).tenantId(1L).dccProjectCodeId(10L).build());
        ReflectionTestUtils.setField(guard,"masterMapper",masters);
        assertTrue(guard.isAllowed(99L,file,false,null));
        guard.requireBodyRead(99L,file,new DccControlledFileBodyReadContext(
                DccControlledFileBodyReadContext.Intent.VERSION_HISTORY,"910","A/1",null,null,null));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->guard.requireBodyRead(99L,file,
                new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.AUTHORING,"910","A/1",null,null,null)));
        var query=new DccControlledFileQueryServiceImpl();
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",guard);
        ReflectionTestUtils.setField(query,"assignmentScopeService",mock(DccControlledFileAssignmentScopeService.class));
        ReflectionTestUtils.setField(query,"permissionSupport",mock(DccControlledFileCategoryPermissionSupport.class));
        ReflectionTestUtils.setField(query,"directoryAccessPermissionService",mock(DccDirectoryAccessPermissionService.class));
        assertTrue(query.canViewFileName(99L,file));
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(query,"canReadBinary",99L,file,DccAccessTypeEnum.PREVIEW)));
        assertTrue(Boolean.FALSE.equals(ReflectionTestUtils.invokeMethod(query,"canReadBinary",99L,file,DccAccessTypeEnum.DOWNLOAD)));
        cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyDecision attachmentDecision =
                ReflectionTestUtils.invokeMethod(query,"decideAttachmentDownload",99L,file);
        assertFalse(attachmentDecision.allowed());
        file.setDccProjectCodeId(11L);
        assertFalse(guard.canReadProjectReference(99L,file));
        file.setDccProjectCodeId(10L);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        assertFalse(guard.canReadProjectReference(99L,file));
    }
}
