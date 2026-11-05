package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccArchiveAccessServiceTest extends BaseMockitoUnitTest {
    @Mock private PermissionApi permissionApi;
    @Mock private AdminUserApi adminUserApi;
    @InjectMocks private DccArchiveAccessService service;
    @BeforeEach void tenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    private DccControlledFileDO archived() {
        var file=DccControlledFileDO.builder().id(10L).status("OBSOLETE").versionNo("A.0").build();
        file.setTenantId(1L);return file;
    }
    private void actor() {
        when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
    }
    private void documentControl() {
        actor();
        when(permissionApi.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(99L,"dcc:controlled-file:archive:query")).thenReturn(true);
    }
    @Test void requesterAndEditorDoNotGrantArchiveRead() {
        actor();assertFalse(service.canRead(99L,archived()));
    }
    @Test void archivePermissionAloneDoesNotGrantDocumentControlAccess() {
        actor();lenient().when(permissionApi.hasAnyPermissions(99L,"dcc:controlled-file:archive:query")).thenReturn(true);
        assertFalse(service.canQuery(99L));assertFalse(service.canDownload(99L,archived(),null));
    }
    @Test void documentControlRoleStillRequiresArchivePermission() {
        actor();when(permissionApi.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        assertFalse(service.canQuery(99L));assertFalse(service.canRead(99L,archived()));
    }
    @Test void documentControlReadsAndDownloadsBodyAndAttachmentsAcrossProjectScope() {
        documentControl();var file=archived();
        assertTrue(service.canRead(99L,file));assertTrue(service.canDownload(99L,file,null));
        file.setStatus("SUPERSEDED");assertTrue(service.canDownload(99L,file,77L));
    }
    @Test void ordinaryUserCannotDownloadArchivedBodyOrAttachment() {
        actor();
        assertFalse(service.canDownload(99L,archived(),null));assertFalse(service.canDownload(99L,archived(),77L));
    }
    @Test void wrongTenantDeletedAndActiveFilesCannotUseArchiveAccess() {
        var file=archived();file.setTenantId(2L);assertFalse(service.canDownload(99L,file,null));
        file.setTenantId(1L);file.setDeleted(true);assertFalse(service.canRead(99L,file));
        file.setDeleted(false);file.setStatus("ACTIVE");assertFalse(service.canDownload(99L,file,77L));
        verifyNoInteractions(permissionApi,adminUserApi);
    }
    @Test void disabledForeignOrMissingActorCannotReadOrDownload() {
        when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(1));
        assertFalse(service.canRead(99L,archived()));
        when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(2L).setStatus(0));
        assertFalse(service.canDownload(99L,archived(),null));assertFalse(service.canQuery(null));
        verifyNoInteractions(permissionApi);
    }
}
