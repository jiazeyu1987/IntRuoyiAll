package cn.iocoder.yudao.module.mes.service.pro.productionrelease.role;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReleaseRequiredCandidateResolverTest {

    private static final Long TENANT_ID = 11L;

    @Mock
    private RoleApi roleApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private AdminUserApi adminUserApi;

    private MesProductionReleaseRequiredCandidateResolver resolver;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        resolver = new MesProductionReleaseRequiredCandidateResolverImpl(roleApi, permissionApi, adminUserApi);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldResolvePqcCandidatesInStableAscendingOrderAndHash() {
        String roleCode = MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER;
        RoleRespDTO role = role(510L, roleCode, CommonStatusEnum.ENABLE.getStatus());
        Set<Long> memberIds = new LinkedHashSet<>(List.of(1003L, 1001L, 1002L));
        when(roleApi.getRoleByCode(roleCode)).thenReturn(role);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(510L))).thenReturn(memberIds);
        when(adminUserApi.getUserList(memberIds)).thenReturn(List.of(
                user(1002L, CommonStatusEnum.ENABLE.getStatus()),
                user(1001L, CommonStatusEnum.ENABLE.getStatus()),
                user(1003L, CommonStatusEnum.ENABLE.getStatus())));

        MesProductionReleaseRoleCandidates result = resolver.resolveRequiredCandidates(TENANT_ID, roleCode);

        assertEquals(510L, result.roleId());
        assertEquals(roleCode, result.roleCode());
        assertEquals(List.of(1001L, 1002L, 1003L), result.candidateUserIds());
        assertEquals(DigestUtil.sha256Hex("11|510|MES_PQC_RELEASE_OWNER|1001,1002,1003"),
                result.candidateSnapshotHash());
        assertThrows(UnsupportedOperationException.class, () -> result.candidateUserIds().add(1004L));
    }

    @Test
    void shouldResolveManagementRepresentativeWithoutComparingUsername() {
        String roleCode = MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE;
        when(roleApi.getRoleByCode(roleCode)).thenReturn(role(520L, roleCode, CommonStatusEnum.ENABLE.getStatus()));
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(520L))).thenReturn(Set.of(2001L));
        when(adminUserApi.getUserList(Set.of(2001L)))
                .thenReturn(List.of(user(2001L, CommonStatusEnum.ENABLE.getStatus())));

        MesProductionReleaseRoleCandidates result = resolver.resolveRequiredCandidates(TENANT_ID, roleCode);

        assertEquals(List.of(2001L), result.candidateUserIds());
        assertTrue(result.candidateSnapshotHash().matches("[0-9a-f]{64}"));
    }

    @Test
    void shouldRejectUnsupportedRoleCodeBeforeReadingSystemRole() {
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolveRequiredCandidates(TENANT_ID, "super_admin"));

        verify(roleApi, never()).getRoleByCode("super_admin");
    }

    @Test
    void shouldRejectCrossTenantResolutionBeforeReadingSystemRole() {
        MesProductionReleaseRoleResolutionException error = assertThrows(
                MesProductionReleaseRoleResolutionException.class,
                () -> resolver.resolveRequiredCandidates(12L, MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER));

        assertEquals(MesProductionReleaseRoleBlockerType.PQC_RELEASE_ROLE_REQUIRED, error.getBlockerType());
        assertEquals(MesProductionReleaseRoleResolutionReason.TENANT_CONTEXT_MISMATCH, error.getReason());
        verify(roleApi, never()).getRoleByCode(MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER);
    }

    @Test
    void shouldRejectMissingDisabledAndDuplicatePqcRole() {
        String roleCode = MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER;

        when(roleApi.getRoleByCode(roleCode)).thenReturn(null);
        assertResolutionFailure(roleCode, MesProductionReleaseRoleResolutionReason.ROLE_NOT_FOUND);

        when(roleApi.getRoleByCode(roleCode))
                .thenReturn(role(510L, roleCode, CommonStatusEnum.DISABLE.getStatus()));
        assertResolutionFailure(roleCode, MesProductionReleaseRoleResolutionReason.ROLE_DISABLED);

        when(roleApi.getRoleByCode(roleCode)).thenThrow(new TooManyResultsException("duplicate role code"));
        assertResolutionFailure(roleCode, MesProductionReleaseRoleResolutionReason.ROLE_NOT_UNIQUE);
    }

    @Test
    void shouldRejectEmptyRoleMembership() {
        String roleCode = MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER;
        when(roleApi.getRoleByCode(roleCode)).thenReturn(role(510L, roleCode, CommonStatusEnum.ENABLE.getStatus()));
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(510L))).thenReturn(Set.of());

        assertResolutionFailure(roleCode, MesProductionReleaseRoleResolutionReason.CANDIDATE_EMPTY);
        verify(adminUserApi, never()).getUserList(Set.of());
    }

    @Test
    void shouldRejectDisabledCandidateInsteadOfFilteringIt() {
        String roleCode = MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE;
        Set<Long> memberIds = Set.of(2001L, 2002L);
        when(roleApi.getRoleByCode(roleCode)).thenReturn(role(520L, roleCode, CommonStatusEnum.ENABLE.getStatus()));
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(520L))).thenReturn(memberIds);
        when(adminUserApi.getUserList(memberIds)).thenReturn(List.of(
                user(2001L, CommonStatusEnum.ENABLE.getStatus()),
                user(2002L, CommonStatusEnum.DISABLE.getStatus())));

        assertResolutionFailure(roleCode, MesProductionReleaseRoleResolutionReason.CANDIDATE_USER_DISABLED);
    }

    @Test
    void shouldRejectMissingCandidateReturnedFromTenantScopedUserApi() {
        String roleCode = MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER;
        Set<Long> memberIds = Set.of(1001L, 900000L);
        when(roleApi.getRoleByCode(roleCode)).thenReturn(role(510L, roleCode, CommonStatusEnum.ENABLE.getStatus()));
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(510L))).thenReturn(memberIds);
        when(adminUserApi.getUserList(memberIds))
                .thenReturn(List.of(user(1001L, CommonStatusEnum.ENABLE.getStatus())));

        assertResolutionFailure(roleCode, MesProductionReleaseRoleResolutionReason.CANDIDATE_USER_NOT_IN_TENANT);
    }

    private void assertResolutionFailure(String roleCode, MesProductionReleaseRoleResolutionReason reason) {
        MesProductionReleaseRoleResolutionException error = assertThrows(
                MesProductionReleaseRoleResolutionException.class,
                () -> resolver.resolveRequiredCandidates(TENANT_ID, roleCode));
        assertEquals(MesProductionReleaseRoleBlockerType.forRoleCode(roleCode), error.getBlockerType());
        assertEquals(reason, error.getReason());
        assertEquals(TENANT_ID, error.getTenantId());
        assertEquals(roleCode, error.getRoleCode());
    }

    private static RoleRespDTO role(Long id, String code, Integer status) {
        RoleRespDTO role = new RoleRespDTO();
        role.setId(id);
        role.setCode(code);
        role.setStatus(status);
        return role;
    }

    private static AdminUserRespDTO user(Long id, Integer status) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setStatus(status);
        return user;
    }
}
