package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import cn.iocoder.yudao.module.system.service.tenant.handler.TenantMenuHandler;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EdhrPermissionTransferServiceTest {
    private EdhrPermissionTransferService service;
    private RoleMapper roles;
    private MenuMapper menus;
    private AdminUserMapper users;
    private PermissionService permissions;
    private RoleService roleService;
    private RoleCategoryService categories;
    private TenantService tenants;
    private MockedStatic<SecurityFrameworkUtils> security;
    private static final String PERMISSION = "mes:pro-edhr-batch-execution:upload";
    private final String code = EdhrPermissionTransferService.targetCode("dossier_officer");
    private RoleDO existing;

    @BeforeEach void setup() throws Exception {
        service = new EdhrPermissionTransferService();
        roles = inject("roleMapper", RoleMapper.class);
        menus = inject("menuMapper", MenuMapper.class);
        users = inject("userMapper", AdminUserMapper.class);
        permissions = inject("permissionService", PermissionService.class);
        roleService = inject("roleService", RoleService.class);
        categories = inject("categoryService", RoleCategoryService.class);
        tenants = inject("tenantService", TenantService.class);
        inject("approvalAuthority", ApprovalAuthorityApi.class);
        inject("gxpAuditService", GxpAuditService.class);
        TenantContextHolder.setTenantId(2L);
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(new LoginUser().setId(10L).setTenantId(2L));
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);
        var tenant = new TenantDO(); tenant.setId(2L); tenant.setName("瑛泰集团");
        when(tenants.getTenant(2L)).thenReturn(tenant);
        doAnswer(call -> { call.<TenantMenuHandler>getArgument(0).handle(Set.of(100L, 101L)); return null; })
                .when(tenants).handleTenantMenu(any());
        MenuDO parent = new MenuDO(); parent.setId(100L); parent.setParentId(0L); parent.setName("eDHR"); parent.setStatus(0);
        MenuDO button = new MenuDO(); button.setId(101L); button.setParentId(100L); button.setName("上传资料"); button.setPermission(PERMISSION); button.setStatus(0);
        when(menus.selectList()).thenReturn(List.of(parent, button));
        existing = new RoleDO(); existing.setId(30L); existing.setCode("unrelated"); existing.setName("原有岗位");
        when(roles.selectList()).thenReturn(List.of(existing));
        when(categories.getRoleCategoryList()).thenReturn(List.of());
        when(categories.createRoleCategory(any())).thenReturn(80L);
        var user = new AdminUserDO(); user.setId(20L); user.setUsername("target_operator"); user.setNickname("目标人员"); user.setStatus(0);
        when(users.selectByUsername("target_operator")).thenReturn(user);
        when(users.selectPermissionSubjectForUpdate(2L, 20L)).thenReturn(user);
        when(permissions.getUserRoleIdListByUserId(20L)).thenReturn(Set.of(30L));
        when(roleService.createRole(any(), any())).thenReturn(40L);
    }
    private <T> T inject(String field, Class<T> type) {
        T mock = mock(type); ReflectionTestUtils.setField(service, field, mock); return mock;
    }
    @AfterEach void cleanup() { security.close(); TenantContextHolder.clear(); }

    private byte[] workbook() throws Exception {
        return EdhrPermissionWorkbook.write(
                List.of(List.of("source_operator", "来源人员", "启用", "target_operator", "dossier_officer", code)),
                List.of(List.of("dossier_officer", code, "资料人员迁移角色", "仅本人", PERMISSION, "eDHR / 上传资料", "上传活跃订单资料")),
                List.of(List.of(PERMISSION, "上传活跃订单资料", "MesActiveOrderDossierFileController")), "2026-10-31");
    }

    @Test void workbookPreservesExplicitMappingAndReadablePurpose() throws Exception {
        byte[] bytes = workbook();
        var content = EdhrPermissionWorkbook.read(bytes);
        assertEquals("target_operator", content.accounts().get(0).targetUsername());
        assertEquals(PERMISSION, content.roles().get(0).permission());
        try (var book = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            assertEquals(4, book.getNumberOfSheets());
            assertEquals("上传活跃订单资料", book.getSheet("角色权限").getRow(1).getCell(6).getStringCellValue());
            assertEquals("仅本人", book.getSheet("角色权限").getRow(1).getCell(3).getStringCellValue());
        }
    }

    @Test void departmentScopesExportWithoutBlockingOtherRolesButCannotBeImported() throws Exception {
        var sourceTenant = new TenantDO(); sourceTenant.setId(2L); sourceTenant.setName("芋道源码");
        when(tenants.getTenant(2L)).thenReturn(sourceTenant);
        List<RoleDO> sourceRoles = new ArrayList<>();
        for (int scope : List.of(1, 2, 3, 4, 5)) {
            var role = new RoleDO(); role.setId(50L + scope); role.setCode("source_scope_" + scope);
            role.setName("来源范围" + scope); role.setType(2); role.setStatus(0); role.setDataScope(scope);
            sourceRoles.add(role);
            when(permissions.getRoleMenuListByRoleId(role.getId())).thenReturn(Set.of(101L));
        }
        when(roles.selectList()).thenReturn(sourceRoles);
        when(users.selectList()).thenReturn(List.of());
        byte[] bytes = service.exportWorkbook();
        var content = EdhrPermissionWorkbook.read(bytes);
        assertEquals(5, content.roles().size());
        assertEquals(3, content.roles().stream().filter(r -> r.dataScope().contains("禁止导入")).count());
        var targetTenant = new TenantDO(); targetTenant.setId(2L); targetTenant.setName("瑛泰集团");
        when(tenants.getTenant(2L)).thenReturn(targetTenant);
        when(roles.selectList()).thenReturn(List.of(existing));
        var preview = service.preview(bytes, null);
        assertEquals(3, preview.errors().size());
        assertTrue(preview.token().isEmpty());
        assertThrows(RuntimeException.class, () -> service.confirm(bytes, null, "", "部门范围不得扩大"));
        verifyNoInteractions(roleService);
        verify(permissions, never()).assignRoleMenu(any(), any(), any(), any());
    }

    @Test void explicitlyGrantedParentPermissionSurvivesExportAndCanBePreviewed() throws Exception {
        String parentPermission = "mes:pro-edhr-batch-processing:query";
        var parent = new MenuDO(); parent.setId(100L); parent.setParentId(0L); parent.setName("eDHR批记录");
        parent.setStatus(0); parent.setPermission(parentPermission);
        var button = new MenuDO(); button.setId(101L); button.setParentId(100L); button.setName("上传资料");
        button.setStatus(0); button.setPermission(PERMISSION);
        when(menus.selectList()).thenReturn(List.of(parent, button));
        var sourceRole = new RoleDO(); sourceRole.setId(50L); sourceRole.setCode("dossier_officer");
        sourceRole.setName("资料人员"); sourceRole.setType(2); sourceRole.setStatus(0); sourceRole.setDataScope(1);
        when(roles.selectList()).thenReturn(List.of(sourceRole));
        when(users.selectList()).thenReturn(List.of());
        when(permissions.getRoleMenuListByRoleId(50L)).thenReturn(Set.of(100L, 101L));
        var sourceTenant = new TenantDO(); sourceTenant.setId(2L); sourceTenant.setName("芋道源码");
        when(tenants.getTenant(2L)).thenReturn(sourceTenant);
        byte[] bytes = service.exportWorkbook();
        assertEquals(Set.of(parentPermission, PERMISSION), EdhrPermissionWorkbook.read(bytes).roles().stream()
                .map(EdhrPermissionWorkbook.RoleRow::permission).collect(java.util.stream.Collectors.toSet()));
        var targetTenant = new TenantDO(); targetTenant.setId(2L); targetTenant.setName("瑛泰集团");
        when(tenants.getTenant(2L)).thenReturn(targetTenant);
        when(roles.selectList()).thenReturn(List.of(existing));
        var preview = service.preview(bytes, null);
        assertTrue(preview.errors().isEmpty(), preview.errors().toString());
        assertEquals(Set.of(100L, 101L), preview.roles().get(0).menuIds());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"MES_MANAGEMENT_REPRESENTATIVE", "MES_PQC_RELEASE_OWNER"})
    void missingBusinessIdentityRoleIsCreatedWithExactCodeAndQueryOnly(String businessCode) throws Exception {
        String query = "mes:pro-edhr-work-task:query";
        var parent = new MenuDO(); parent.setId(100L); parent.setParentId(0L); parent.setName("eDHR"); parent.setStatus(0);
        var menu = new MenuDO(); menu.setId(101L); menu.setParentId(100L); menu.setName("工作任务");
        menu.setPermission(query); menu.setStatus(0);
        when(menus.selectList()).thenReturn(List.of(parent, menu));
        byte[] bytes = EdhrPermissionWorkbook.write(
                List.of(List.of("source_operator", "来源人员", "启用", "target_operator", businessCode, businessCode)),
                List.of(List.of(businessCode, businessCode, "精确业务身份角色", "全部数据", query, "eDHR / 工作任务", "查询工作任务")),
                List.of(List.of(query, "查询工作任务", "MesProEdhrWorkTaskController")), "2026-10-31");
        var preview = service.preview(bytes, null);
        assertTrue(preview.errors().isEmpty(), preview.errors().toString());
        assertNull(preview.roles().get(0).roleId());
        assertEquals(businessCode, preview.roles().get(0).code());
        assertEquals(1, service.confirm(bytes, null, preview.token(), "验证缺失业务身份只读授权").createdRoles());
        verify(roleService).createRole(argThat(request -> businessCode.equals(request.getCode())), eq(2));
        verify(permissions).assignRoleMenu(eq(40L), eq(Set.of(100L, 101L)), anyString(), anyString());
        verify(permissions).assignUserRole(eq(20L), eq(Set.of(30L, 40L)), anyString(), anyString());
    }

    @Test void previewIsReadOnlyAndConfirmationAddsRolesWithoutRemovingExistingOnes() throws Exception {
        byte[] bytes = workbook();
        var preview = service.preview(bytes, null);
        assertTrue(preview.errors().isEmpty(), preview.errors().toString());
        assertEquals(Set.of(100L, 101L), preview.roles().get(0).menuIds());
        assertEquals("必要父级导航；无额外权限标识", preview.roles().get(0).menus().get(0).purpose());
        verifyNoInteractions(roleService);
        verify(permissions, never()).assignUserRole(any(), any(), any(), any());
        var result = service.confirm(bytes, null, preview.token(), "验证资料岗位授权迁移");
        assertEquals(1, result.createdRoles()); assertEquals(1, result.changedAccounts());
        verify(permissions).assignUserRole(eq(20L), eq(Set.of(30L, 40L)), anyString(), anyString());
        verify(permissions).assignRoleDataScope(eq(40L), eq(5), eq(Set.of()), anyString(), anyString());
        verify(permissions).assignRoleMenu(eq(40L), eq(Set.of(100L, 101L)), anyString(), anyString());
    }

    @Test void samePermissionAtAnotherMenuPathIsNotImplicitlyGrantedOrRequired() throws Exception {
        var parent = new MenuDO(); parent.setId(100L); parent.setParentId(0L); parent.setName("eDHR"); parent.setStatus(0);
        var requested = new MenuDO(); requested.setId(101L); requested.setParentId(100L); requested.setName("上传资料");
        requested.setPermission(PERMISSION); requested.setStatus(0);
        var other = new MenuDO(); other.setId(102L); other.setParentId(100L); other.setName("另一个未授权入口");
        other.setPermission(PERMISSION); other.setStatus(0);
        when(menus.selectList()).thenReturn(List.of(parent, requested, other));
        byte[] bytes = workbook();
        var preview = service.preview(bytes, null);
        assertTrue(preview.errors().isEmpty(), preview.errors().toString());
        assertEquals(Set.of(100L, 101L), preview.roles().get(0).menuIds());
        service.confirm(bytes, null, preview.token(), "同权限不同路径不得扩展菜单");
        verify(permissions).assignRoleMenu(eq(40L), eq(Set.of(100L, 101L)), anyString(), anyString());
    }

    @Test void explicitPageMappingCanResolveDifferentTargetUsername() throws Exception {
        byte[] bytes = workbook();
        var preview = service.preview(bytes, "[{\"sourceUsername\":\"source_operator\",\"targetUsername\":\"target_operator\"}]");
        assertTrue(preview.errors().isEmpty());
        assertEquals("目标人员", preview.accounts().get(0).targetName());
    }

    @Test void unchangedImportDoesNotIssuePermissionCommands() throws Exception {
        RoleDO role = new RoleDO(); role.setId(40L); role.setName("资料人员迁移角色"); role.setCode(code);
        role.setType(2); role.setStatus(0); role.setDataScope(5); role.setDataScopeDeptIds(Set.of());
        when(roles.selectList()).thenReturn(List.of(existing, role));
        when(roles.selectPermissionSubjectForUpdate(2L, 40L)).thenReturn(role);
        when(permissions.getRoleMenuListByRoleId(40L)).thenReturn(Set.of(100L, 101L));
        when(permissions.getUserRoleIdListByUserId(20L)).thenReturn(Set.of(30L, 40L));
        byte[] bytes = workbook();
        var preview = service.preview(bytes, null);
        assertTrue(preview.errors().isEmpty(), preview.errors().toString());
        assertTrue(service.confirm(bytes, null, preview.token(), "重复导入检查已有授权").unchanged());
        verifyNoInteractions(roleService);
        verify(permissions, never()).assignRoleMenu(any(), any(), any(), any());
        verify(permissions, never()).assignUserRole(any(), any(), any(), any());
    }

    @Test void missingTargetBlocksWholeBatchBeforeAnyAuthorizationWrite() throws Exception {
        when(users.selectByUsername("target_operator")).thenReturn(null);
        byte[] bytes = workbook();
        var preview = service.preview(bytes, null);
        assertTrue(preview.errors().get(0).contains("目标账号不存在"));
        assertEquals("", preview.token());
        assertThrows(RuntimeException.class, () -> service.confirm(bytes, null, "", "检查目标账号不存在"));
        verifyNoInteractions(roleService);
        verify(permissions, never()).assignRoleMenu(any(), any(), any(), any());
    }

    @Test void changedAccountStateRequiresFreshPreview() throws Exception {
        byte[] bytes = workbook();
        var preview = service.preview(bytes, null);
        when(permissions.getUserRoleIdListByUserId(20L)).thenReturn(Set.of());
        var ex = assertThrows(RuntimeException.class, () -> service.confirm(bytes, null, preview.token(), "检查预览后账号状态变化"));
        assertTrue(ex.getMessage().contains("预览后已变化"));
        verifyNoInteractions(roleService);
    }
}
