package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApprovalQualificationWriterTest {
    private final ApprovalAuthorityApi authority = mock(ApprovalAuthorityApi.class);
    private final RoleMapper mapper = mock(RoleMapper.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final RoleCategoryService categories = mock(RoleCategoryService.class);
    private final RoleServiceImpl roles = new RoleServiceImpl();
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(7L);
        ReflectionTestUtils.setField(roles, "approvalAuthority", authority);
        ReflectionTestUtils.setField(roles, "roleMapper", mapper);
        ReflectionTestUtils.setField(roles, "permissionService", permissions);
        ReflectionTestUtils.setField(roles, "roleCategoryService", categories);
        when(authority.lockTenantApprovalAuthority(7L)).thenReturn(new ApprovalAuthorityRevisionDTO(7L, 3L));
        RoleDO role = new RoleDO(); role.setId(12L); role.setTenantId(7L); role.setType(RoleTypeEnum.CUSTOM.getType());
        when(mapper.selectById(12L)).thenReturn(role);
        when(mapper.updateById(any(RoleDO.class))).thenReturn(1);
        when(mapper.deleteById(12L)).thenReturn(1);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void roleDataScopeMutationHoldsAuthorityBeforeSubjectRead() {
        roles.updateRoleDataScope(12L, 2, Set.of(5L));
        var order = inOrder(authority, mapper);
        order.verify(authority).lockTenantApprovalAuthority(7L);
        order.verify(mapper).selectById(12L);
        order.verify(mapper).updateById(argThat((RoleDO role) -> role.getId() == 12L && role.getDataScope() == 2));
        order.verify(authority).incrementLockedAuthorityRevision(7L, 3L);
    }
    @Test void deletionAndMembershipRevocationShareAuthorityRevision() {
        roles.deleteRole(12L);
        var order = inOrder(authority, mapper, permissions);
        order.verify(authority).lockTenantApprovalAuthority(7L);
        order.verify(mapper).selectById(12L);
        order.verify(mapper).deleteById(12L);
        order.verify(permissions).processRoleDeleted(12L);
        order.verify(authority).incrementLockedAuthorityRevision(7L, 3L);
    }
    @Test void missingAuthorityPreventsRoleChanges() {
        when(authority.lockTenantApprovalAuthority(7L)).thenThrow(new IllegalStateException("not initialized"));
        assertThrows(IllegalStateException.class, () -> roles.updateRoleDataScope(12L, 2, Set.of()));
        verifyNoInteractions(mapper, permissions);
    }
    @Test void failedRoleUpdateCannotClaimCompletedAuthorityRevision() {
        when(mapper.updateById(any(RoleDO.class))).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> roles.updateRoleDataScope(12L, 2, Set.of()));
        verify(authority, never()).incrementLockedAuthorityRevision(anyLong(), anyLong());
    }
    @Test void roleUpdatePreservesBusinessValidationAfterAuthorityLock() {
        RoleSaveReqVO request = new RoleSaveReqVO(); request.setId(12L); request.setName("approver");
        request.setCode("approver"); request.setStatus(1);
        roles.updateRole(request);
        verify(mapper).updateById(argThat((RoleDO role) -> role.getStatus() == 1 && "approver".equals(role.getCode())));
        verify(authority).incrementLockedAuthorityRevision(7L, 3L);
    }
}
