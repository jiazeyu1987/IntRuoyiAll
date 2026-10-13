package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.ApprovalAuthorityTenantScopeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalApprovalAuthorityCoordinatorTest {
    private final ApprovalAuthorityTenantScopeMapper scope = mock(ApprovalAuthorityTenantScopeMapper.class);
    private final ApprovalAuthorityApi authority = mock(ApprovalAuthorityApi.class);
    private final GlobalApprovalAuthorityCoordinator service = new GlobalApprovalAuthorityCoordinator(scope, authority);
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void globalConfigurationLocksEveryFormalRoleTenantInNumericOrder() {
        TenantContextHolder.setTenantId(9L); when(scope.selectGlobalRoleTenantIds()).thenReturn(List.of(2L, 9L, 17L));
        for (Long tenant : List.of(2L, 9L, 17L)) {
            when(authority.lockTenantApprovalAuthority(tenant)).thenAnswer(call -> {
                assertEquals(tenant, TenantContextHolder.getRequiredTenantId());
                return new ApprovalAuthorityRevisionDTO(tenant, 3L);
            });
        }
        var locked = service.lockGlobalRoleTenants();
        assertEquals(9L, TenantContextHolder.getRequiredTenantId());
        service.incrementLockedTenants(locked);
        assertEquals(9L, TenantContextHolder.getRequiredTenantId());
        var order = inOrder(authority);
        for (Long tenant : List.of(2L, 9L, 17L)) order.verify(authority).lockTenantApprovalAuthority(tenant);
        for (Long tenant : List.of(2L, 9L, 17L)) order.verify(authority).incrementLockedAuthorityRevision(tenant, 3L);
    }
    @Test void missingFormalTenantEvidenceNeverUsesCurrentTenantAsReplacement() {
        TenantContextHolder.setTenantId(9L); when(scope.selectGlobalRoleTenantIds()).thenReturn(List.of());
        assertThrows(IllegalStateException.class, service::lockGlobalRoleTenants);
        verifyNoInteractions(authority);
    }
    @Test void invalidEnumerationAndDuplicateTenantsAreRejected() {
        when(scope.selectGlobalRoleTenantIds()).thenReturn(List.of(9L, 2L));
        assertThrows(IllegalStateException.class, service::lockGlobalRoleTenants);
        when(scope.selectGlobalRoleTenantIds()).thenReturn(List.of(2L, 2L));
        assertThrows(IllegalStateException.class, service::lockGlobalRoleTenants);
        verifyNoInteractions(authority);
    }
    @Test void partialAcquisitionFailureRestoresTenantContextAndPropagates() {
        TenantContextHolder.setTenantId(9L); when(scope.selectGlobalRoleTenantIds()).thenReturn(List.of(2L, 9L));
        when(authority.lockTenantApprovalAuthority(2L)).thenReturn(new ApprovalAuthorityRevisionDTO(2L, 3L));
        when(authority.lockTenantApprovalAuthority(9L)).thenThrow(new IllegalStateException("missing row"));
        assertThrows(IllegalStateException.class, service::lockGlobalRoleTenants);
        assertEquals(9L, TenantContextHolder.getRequiredTenantId());
        verify(authority, never()).incrementLockedAuthorityRevision(anyLong(), anyLong());
    }
}
