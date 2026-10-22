package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.ApprovalAuthorityTenantScopeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class GlobalApprovalAuthorityCoordinator {
    private final ApprovalAuthorityTenantScopeMapper scope;
    private final ApprovalAuthorityApi authority;
    public GlobalApprovalAuthorityCoordinator(ApprovalAuthorityTenantScopeMapper scope, ApprovalAuthorityApi authority) {
        this.scope = scope; this.authority = authority;
    }
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public List<ApprovalAuthorityRevisionDTO> lockGlobalRoleTenants() {
        List<Long> tenants = scope.selectGlobalRoleTenantIds();
        if (tenants == null || tenants.isEmpty() || tenants.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalStateException("APPROVAL_AUTHORITY_FORMAL_TENANT_SCOPE_REQUIRED");
        }
        List<Long> sorted = tenants.stream().distinct().sorted().toList();
        if (!sorted.equals(tenants)) throw new IllegalStateException("APPROVAL_AUTHORITY_TENANT_SCOPE_INVALID");
        var locked = new ArrayList<ApprovalAuthorityRevisionDTO>();
        for (Long tenant : sorted) {
            TenantUtils.execute(tenant, () -> {
                locked.add(authority.lockTenantApprovalAuthority(tenant));
            });
        }
        return List.copyOf(locked);
    }
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void incrementLockedTenants(List<ApprovalAuthorityRevisionDTO> locked) {
        if (locked == null || locked.isEmpty()) throw new IllegalStateException("APPROVAL_AUTHORITY_LOCK_SCOPE_REQUIRED");
        Long previous = null;
        for (var item : locked) {
            if (item == null || previous != null && item.tenantId() <= previous) {
                throw new IllegalArgumentException("APPROVAL_AUTHORITY_LOCK_SCOPE_INVALID");
            }
            TenantUtils.execute(item.tenantId(), () ->
                    authority.incrementLockedAuthorityRevision(item.tenantId(), item.revision()));
            previous = item.tenantId();
        }
    }
}
