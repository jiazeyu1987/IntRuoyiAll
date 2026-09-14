package cn.iocoder.yudao.module.mdm.service.companyscope;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmRoleCompanyScopeDO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmUserCompanyScopeDO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import cn.iocoder.yudao.module.mdm.dal.mysql.companyscope.MdmRoleCompanyScopeMapper;
import cn.iocoder.yudao.module.mdm.dal.mysql.companyscope.MdmUserCompanyScopeMapper;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseStatusEnum;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import cn.iocoder.yudao.module.mdm.service.enterprise.MdmEnterpriseService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_CONFIG_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_FIELD_REQUIRED;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_PERMISSION_MISSING;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_RECIPIENT_NOT_FOUND;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_ROLE_COMPANY_SCOPE_DISABLED;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_ROLE_COMPANY_SCOPE_MISSING;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_DISABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmCompanyScopeRecipientResolverTest {

    private static final Long TENANT_ID = 11L;
    private static final Long COMPANY_ID = 301L;
    private static final String PERMISSION = "dcc:registration-certificate:notify";

    @Mock
    private MdmEnterpriseService enterpriseService;
    @Mock
    private MdmRoleCompanyScopeMapper roleScopeMapper;
    @Mock
    private MdmUserCompanyScopeMapper userScopeMapper;
    @Mock
    private RoleApi roleApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private AdminUserApi adminUserApi;

    private MdmCompanyScopeRecipientResolver resolver;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        resolver = new MdmCompanyScopeRecipientResolver();
        ReflectionTestUtils.setField(resolver, "enterpriseService", enterpriseService);
        ReflectionTestUtils.setField(resolver, "roleScopeMapper", roleScopeMapper);
        ReflectionTestUtils.setField(resolver, "userScopeMapper", userScopeMapper);
        ReflectionTestUtils.setField(resolver, "roleApi", roleApi);
        ReflectionTestUtils.setField(resolver, "permissionApi", permissionApi);
        ReflectionTestUtils.setField(resolver, "adminUserApi", adminUserApi);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void resolveReturnsStableDeduplicatedFullIntersection() {
        List<Long> roleIds = List.of(20L, 10L);
        stubEnabledCompany();
        when(roleScopeMapper.selectByTenantCompanyAndRoleIds(TENANT_ID, COMPANY_ID, roleIds))
                .thenReturn(List.of(roleScope(2L, 10L, true), roleScope(1L, 20L, true)));
        when(roleApi.getRoleList(roleIds)).thenReturn(List.of(enabledRole(10L), enabledRole(20L)));
        when(permissionApi.hasAnyPermissionsInRoles(Set.of(10L), PERMISSION)).thenReturn(true);
        when(permissionApi.hasAnyPermissionsInRoles(Set.of(20L), PERMISSION)).thenReturn(true);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(10L, 20L)))
                .thenReturn(new LinkedHashSet<>(List.of(300L, 100L, 200L)));
        when(userScopeMapper.selectByTenantCompany(TENANT_ID, COMPANY_ID)).thenReturn(List.of(
                userScope(1L, 100L, true),
                userScope(2L, 200L, true),
                userScope(3L, 999L, true)));
        when(adminUserApi.getUserList(Set.of(100L, 200L)))
                .thenReturn(List.of(enabledUser(200L), enabledUser(100L)));

        Set<Long> recipients = resolver.resolve(COMPANY_ID, roleIds, PERMISSION);

        assertEquals(List.of(100L, 200L), List.copyOf(recipients));
        verify(permissionApi).hasAnyPermissionsInRoles(Set.of(10L), PERMISSION);
        verify(permissionApi).hasAnyPermissionsInRoles(Set.of(20L), PERMISSION);
    }

    @Test
    void resolveChecksRoleCompanyMappingBeforeEnterpriseLookup() {
        List<Long> roleIds = List.of(10L);
        when(roleScopeMapper.selectByTenantCompanyAndRoleIds(TENANT_ID, COMPANY_ID, roleIds))
                .thenReturn(List.of());

        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_ROLE_COMPANY_SCOPE_MISSING);

        verifyNoInteractions(enterpriseService);
    }

    @Test
    void resolveNormalizesEnterpriseStateFailureAfterRoleScope() {
        List<Long> roleIds = List.of(10L);
        when(roleScopeMapper.selectByTenantCompanyAndRoleIds(TENANT_ID, COMPANY_ID, roleIds))
                .thenReturn(List.of(roleScope(1010L, 10L, true)));
        doThrow(exception(MDM_ENTERPRISE_DISABLED, COMPANY_ID))
                .when(enterpriseService).getEnabledEnterprises(List.of(COMPANY_ID),
                        Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));

        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_COMPANY_SCOPE_CONFIG_INVALID);

        verifyNoInteractions(roleApi, permissionApi, userScopeMapper, adminUserApi);
    }

    @Test
    void resolvePropagatesInfrastructureFailureUnchanged() {
        List<Long> roleIds = List.of(10L);
        when(roleScopeMapper.selectByTenantCompanyAndRoleIds(TENANT_ID, COMPANY_ID, roleIds))
                .thenReturn(List.of(roleScope(1010L, 10L, true)));
        IllegalStateException databaseFailure = new IllegalStateException("enterprise database unavailable");
        doThrow(databaseFailure).when(enterpriseService).getEnabledEnterprises(List.of(COMPANY_ID),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION));

        assertSame(databaseFailure, thrown);
        verifyNoInteractions(roleApi, permissionApi, userScopeMapper, adminUserApi);
    }

    @Test
    void resolveRejectsMissingDisabledAndAmbiguousRoleCompanyConfiguration() {
        List<Long> roleIds = List.of(10L, 20L);
        when(roleScopeMapper.selectByTenantCompanyAndRoleIds(TENANT_ID, COMPANY_ID, roleIds))
                .thenReturn(List.of(roleScope(1L, 10L, true)));
        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_ROLE_COMPANY_SCOPE_MISSING);

        when(roleScopeMapper.selectByTenantCompanyAndRoleIds(TENANT_ID, COMPANY_ID, roleIds))
                .thenReturn(List.of(roleScope(1L, 10L, true), roleScope(2L, 20L, false)));
        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_ROLE_COMPANY_SCOPE_DISABLED);

        when(roleScopeMapper.selectByTenantCompanyAndRoleIds(TENANT_ID, COMPANY_ID, roleIds))
                .thenReturn(List.of(roleScope(1L, 10L, true), roleScope(2L, 10L, true),
                        roleScope(3L, 20L, true)));
        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_COMPANY_SCOPE_CONFIG_INVALID);

        verifyNoInteractions(roleApi, permissionApi, userScopeMapper, adminUserApi);
    }

    @Test
    void resolveRejectsMissingDisabledOrDuplicateSystemRoleEvidence() {
        List<Long> roleIds = List.of(10L);
        stubEnabledRoleScope(roleIds);
        when(roleApi.getRoleList(roleIds)).thenReturn(List.of());
        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID);

        when(roleApi.getRoleList(roleIds)).thenReturn(List.of(disabledRole(10L)));
        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID);

        when(roleApi.getRoleList(roleIds)).thenReturn(List.of(enabledRole(10L), enabledRole(10L)));
        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID);
    }

    @Test
    void resolveRequiresEveryConfiguredRoleToHoldExplicitPermission() {
        List<Long> roleIds = List.of(10L, 20L);
        stubEnabledRoleScope(roleIds);
        when(roleApi.getRoleList(roleIds)).thenReturn(List.of(enabledRole(10L), enabledRole(20L)));
        when(permissionApi.hasAnyPermissionsInRoles(Set.of(10L), PERMISSION)).thenReturn(true);
        when(permissionApi.hasAnyPermissionsInRoles(Set.of(20L), PERMISSION)).thenReturn(false);

        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_COMPANY_SCOPE_PERMISSION_MISSING);

        verify(permissionApi, never()).getUserRoleIdListByRoleIds(Set.of(10L, 20L));
    }

    @Test
    void resolveRejectsEmptyIntersectionInsteadOfFallingBack() {
        List<Long> roleIds = List.of(10L);
        stubEnabledRoleScopeAndIdentity(roleIds);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(10L))).thenReturn(Set.of(100L));
        when(userScopeMapper.selectByTenantCompany(TENANT_ID, COMPANY_ID))
                .thenReturn(List.of(userScope(1L, 200L, true)));

        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_COMPANY_SCOPE_RECIPIENT_NOT_FOUND);

        verifyNoInteractions(adminUserApi);
    }

    @Test
    void resolveRejectsMissingOrDisabledSystemUsersInTheIntersection() {
        List<Long> roleIds = List.of(10L);
        stubEnabledRoleScopeAndIdentity(roleIds);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(10L))).thenReturn(Set.of(100L));
        when(userScopeMapper.selectByTenantCompany(TENANT_ID, COMPANY_ID))
                .thenReturn(List.of(userScope(1L, 100L, true)));
        when(adminUserApi.getUserList(Set.of(100L))).thenReturn(List.of());
        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID);

        when(adminUserApi.getUserList(Set.of(100L))).thenReturn(List.of(disabledUser(100L)));
        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID);
    }

    @Test
    void resolveRejectsAmbiguousOrCrossTenantUserScopeEvidence() {
        List<Long> roleIds = List.of(10L);
        stubEnabledRoleScopeAndIdentity(roleIds);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(10L))).thenReturn(Set.of(100L));
        MdmUserCompanyScopeDO crossTenant = userScope(1L, 100L, true);
        crossTenant.setTenantId(99L);
        when(userScopeMapper.selectByTenantCompany(TENANT_ID, COMPANY_ID)).thenReturn(List.of(crossTenant));
        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_COMPANY_SCOPE_CONFIG_INVALID);

        when(userScopeMapper.selectByTenantCompany(TENANT_ID, COMPANY_ID)).thenReturn(List.of(
                userScope(1L, 100L, true), userScope(2L, 100L, true)));
        assertServiceException(() -> resolver.resolve(COMPANY_ID, roleIds, PERMISSION),
                MDM_COMPANY_SCOPE_CONFIG_INVALID);
    }

    @Test
    void resolveRejectsInvalidInputsBeforeReadingConfiguration() {
        assertServiceException(() -> resolver.resolve(null, List.of(10L), PERMISSION),
                MDM_COMPANY_SCOPE_FIELD_REQUIRED);
        assertServiceException(() -> resolver.resolve(COMPANY_ID, List.of(), PERMISSION),
                MDM_COMPANY_SCOPE_FIELD_REQUIRED);
        assertServiceException(() -> resolver.resolve(COMPANY_ID, List.of(10L, 10L), PERMISSION),
                MDM_COMPANY_SCOPE_CONFIG_INVALID);
        assertServiceException(() -> resolver.resolve(COMPANY_ID, List.of(10L), " "),
                MDM_COMPANY_SCOPE_FIELD_REQUIRED);

        verifyNoInteractions(enterpriseService, roleScopeMapper, roleApi, permissionApi, userScopeMapper, adminUserApi);
    }

    private void stubEnabledRoleScope(List<Long> roleIds) {
        stubEnabledCompany();
        when(roleScopeMapper.selectByTenantCompanyAndRoleIds(TENANT_ID, COMPANY_ID, roleIds))
                .thenReturn(roleIds.stream().map(roleId -> roleScope(roleId + 1000L, roleId, true)).toList());
    }

    private void stubEnabledRoleScopeAndIdentity(List<Long> roleIds) {
        stubEnabledRoleScope(roleIds);
        when(roleApi.getRoleList(roleIds)).thenReturn(roleIds.stream().map(this::enabledRole).toList());
        roleIds.forEach(roleId -> when(permissionApi.hasAnyPermissionsInRoles(Set.of(roleId), PERMISSION))
                .thenReturn(true));
    }

    private void stubEnabledCompany() {
        when(enterpriseService.getEnabledEnterprises(List.of(COMPANY_ID),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())))
                .thenReturn(List.of(enabledCompany()));
    }

    private MdmEnterpriseDO enabledCompany() {
        MdmEnterpriseDO company = MdmEnterpriseDO.builder()
                .id(COMPANY_ID).enterpriseCode("COMP-001").name("Owned company")
                .type(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())
                .status(MdmEnterpriseStatusEnum.ENABLE.getStatus()).revision(1).build();
        company.setTenantId(TENANT_ID);
        company.setDeleted(false);
        return company;
    }

    private MdmRoleCompanyScopeDO roleScope(Long id, Long roleId, boolean enabled) {
        MdmRoleCompanyScopeDO scope = MdmRoleCompanyScopeDO.builder()
                .id(id).roleId(roleId).companyId(COMPANY_ID)
                .status(enabled ? MdmEnterpriseStatusEnum.ENABLE.getStatus()
                        : MdmEnterpriseStatusEnum.DISABLE.getStatus())
                .revision(1).build();
        scope.setTenantId(TENANT_ID);
        scope.setDeleted(false);
        return scope;
    }

    private MdmUserCompanyScopeDO userScope(Long id, Long userId, boolean enabled) {
        MdmUserCompanyScopeDO scope = MdmUserCompanyScopeDO.builder()
                .id(id).userId(userId).companyId(COMPANY_ID)
                .status(enabled ? MdmEnterpriseStatusEnum.ENABLE.getStatus()
                        : MdmEnterpriseStatusEnum.DISABLE.getStatus())
                .revision(1).build();
        scope.setTenantId(TENANT_ID);
        scope.setDeleted(false);
        return scope;
    }

    private RoleRespDTO enabledRole(Long roleId) {
        RoleRespDTO role = new RoleRespDTO();
        role.setId(roleId);
        role.setCode("role-" + roleId);
        role.setName("Role " + roleId);
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return role;
    }

    private RoleRespDTO disabledRole(Long roleId) {
        RoleRespDTO role = enabledRole(roleId);
        role.setStatus(CommonStatusEnum.DISABLE.getStatus());
        return role;
    }

    private AdminUserRespDTO enabledUser(Long userId) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(userId);
        user.setUsername("user-" + userId);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return user;
    }

    private AdminUserRespDTO disabledUser(Long userId) {
        AdminUserRespDTO user = enabledUser(userId);
        user.setStatus(CommonStatusEnum.DISABLE.getStatus());
        return user;
    }

    private void assertServiceException(Runnable invocation, ErrorCode expected) {
        ServiceException exception = assertThrows(ServiceException.class, invocation::run);
        assertEquals(expected.getCode(), exception.getCode());
    }

}
