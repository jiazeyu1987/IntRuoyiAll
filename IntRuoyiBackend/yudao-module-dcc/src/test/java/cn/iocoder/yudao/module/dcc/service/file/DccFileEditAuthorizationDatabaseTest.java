package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyDecision;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({DccControlledFileCategoryPermissionSupport.class, DccControlledFileEditPermissionSupport.class})
class DccFileEditAuthorizationDatabaseTest extends BaseDbUnitTest {
    @Resource DccControlledFileCategoryPermissionSupport categories;
    @Resource DccControlledFileEditPermissionSupport edit;
    @Resource DccFileCategoryPermissionRuleMapper rules;
    @MockBean AdminUserApi users;
    @MockBean PermissionApi globals;
    @MockBean DeptApi depts;
    @MockBean DccProjectAccessService projects;
    private DccControlledFileQueryServiceImpl query;
    private DccControlledFileAssignmentScopeService assignments;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        when(users.getUser(91L)).thenReturn(new AdminUserRespDTO().setId(91L).setTenantId(1L).setStatus(0));
        when(projects.listReadableProjectIds(91L)).thenReturn(List.of(321L));
        query = new DccControlledFileQueryServiceImpl();
        assignments = mock(DccControlledFileAssignmentScopeService.class);
        when(assignments.isWithinAssignedFileScope(91L, 81L)).thenReturn(true);
        ReflectionTestUtils.setField(query, "assignmentScopeService", assignments);
        ReflectionTestUtils.setField(query, "permissionSupport", categories);
        ReflectionTestUtils.setField(query, "projectAccessService", projects);
        ReflectionTestUtils.setField(query, "downloadPolicyService", new DccDownloadPolicyService());
        ReflectionTestUtils.setField(query, "directoryAccessPermissionService", mock(DccDirectoryAccessPermissionService.class));
        ReflectionTestUtils.setField(query, "viewMatrixAccessService", mock(DccControlledFileViewMatrixAccessService.class));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void editRequiresAllThreeIndependentGrantsAndEnabledTenantIdentity() {
        rule("EDIT");
        assertThrows(RuntimeException.class, () -> edit.assertEditableProjectCategory(91L,321L,101L));
        when(globals.hasAnyPermissions(91L,"dcc:controlled-file:edit")).thenReturn(true);
        assertFalse(categories.hasFilePermission(file(),91L,DccFileCategoryPermissionActionEnum.EDIT));
        when(projects.hasProjectEditorOrOwner(91L,321L)).thenReturn(true);
        assertDoesNotThrow(() -> edit.assertEditableProjectCategory(91L,321L,101L));
        assertFalse(categories.hasEditableProjectCategory(91L,322L,101L));
        assertFalse(categories.hasEditableProjectCategory(91L,321L,102L));
        assertFalse(categories.hasFilePermission(file().setTenantId(2L),91L,DccFileCategoryPermissionActionEnum.EDIT));
        when(users.getUser(91L)).thenReturn(new AdminUserRespDTO().setId(91L).setTenantId(2L).setStatus(0));
        assertFalse(categories.hasEditableProjectCategory(91L,321L,101L));
        when(users.getUser(91L)).thenReturn(new AdminUserRespDTO().setId(91L).setTenantId(1L).setStatus(1));
        assertFalse(categories.hasEditableProjectCategory(91L,321L,101L));
    }

    @Test void oldActionsAndAdministratorProjectBypassNeverInventTypeEdit() {
        rule("UPLOAD"); rule("DOWNLOAD"); rule("PRINT");
        when(globals.hasAnyPermissions(91L,"dcc:controlled-file:edit")).thenReturn(true);
        when(projects.hasProjectEditorOrOwner(91L,321L)).thenReturn(true);
        assertFalse(categories.hasFilePermission(file(),91L,DccFileCategoryPermissionActionEnum.UPLOAD));
        assertFalse(download(file()));
        assertFalse(categories.hasFilePermission(file(),91L,DccFileCategoryPermissionActionEnum.PRINT));
    }

    @Test void viewReadsContentButCannotDownloadPrintOrMutateEvenWithDirectoryRights() {
        rule("VIEW");
        var directories=mock(DccDirectoryAccessPermissionService.class);
        when(directories.getAuthorizedDirectoryIds(91L,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of(11L));
        when(directories.getAuthorizedDirectoryIds(91L,DccAccessTypeEnum.DOWNLOAD)).thenReturn(Set.of(11L));
        ReflectionTestUtils.setField(query,"directoryAccessPermissionService",directories);
        assertTrue(preview(file()));
        assertFalse(download(file()));
        for (var action:List.of(DccFileCategoryPermissionActionEnum.UPLOAD,DccFileCategoryPermissionActionEnum.PRINT,
                DccFileCategoryPermissionActionEnum.OBSOLETE,DccFileCategoryPermissionActionEnum.DISTRIBUTE))
            assertFalse(categories.hasFilePermission(file(),91L,action));
    }

    @Test void qualifiedEditorIncludesBodyAndDownloadWithoutLegacyOperationOrDirectoryGrants() {
        rule("EDIT");
        when(globals.hasAnyPermissions(91L,"dcc:controlled-file:edit")).thenReturn(true);
        when(projects.hasProjectEditorOrOwner(91L,321L)).thenReturn(true);
        assertTrue(preview(file())); assertTrue(download(file()));
        for (var action:List.of(DccFileCategoryPermissionActionEnum.UPLOAD,DccFileCategoryPermissionActionEnum.PRINT,
                DccFileCategoryPermissionActionEnum.OBSOLETE,DccFileCategoryPermissionActionEnum.DISTRIBUTE))
            assertTrue(categories.hasFilePermission(file(),91L,action));
        assertFalse(download(file().setStatus("SUPERSEDED")));
        when(assignments.isWithinAssignedFileScope(91L,81L)).thenReturn(false);
        assertFalse(preview(file())); assertFalse(download(file()));
    }

    @Test void directoryPreviewAloneDoesNotGrantBodyAndViewNeverCrossesProject() {
        var directories=mock(DccDirectoryAccessPermissionService.class);
        when(directories.getAuthorizedDirectoryIds(91L,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of(11L));
        ReflectionTestUtils.setField(query,"directoryAccessPermissionService",directories);
        assertFalse(preview(file()));
        rule("VIEW"); assertTrue(preview(file()));
        assertFalse(preview(file().setDccProjectCodeId(322L)));
    }

    @Test void reviewMatrixTaskQualificationDoesNotRequireOrdinaryEdit() {
        rule("REVIEW"); rule("APPROVE");
        assertTrue(categories.hasCategoryPermission(101L,91L,DccFileCategoryPermissionActionEnum.REVIEW));
        assertTrue(categories.hasCategoryPermission(101L,91L,DccFileCategoryPermissionActionEnum.APPROVE));
        assertFalse(categories.hasFilePermission(file(),91L,DccFileCategoryPermissionActionEnum.EDIT));
    }

    private void rule(String action) {
        rules.insert(DccFileCategoryPermissionRuleDO.builder().categoryId(101L).actionType(action)
                .subjectType("USER").subjectId(91L).scopeType("GLOBAL").active(true).build());
    }
    private DccControlledFileDO file() {
        return DccControlledFileDO.builder().id(81L).tenantId(1L).dccProjectCodeId(321L).categoryId(101L)
                .directoryId(11L).publishedFileId(51L).status("ACTIVE").build();
    }
    private boolean preview(DccControlledFileDO f) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(query,"canReadBinary",91L,f,DccAccessTypeEnum.PREVIEW,false,null));
    }
    private boolean download(DccControlledFileDO f) {
        DccDownloadPolicyDecision decision=ReflectionTestUtils.invokeMethod(query,"decideDownloadBinary",91L,f,false);
        return decision.allowed();
    }
}
