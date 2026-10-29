package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.post.PostSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.PostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptServiceImpl;
import cn.iocoder.yudao.module.system.service.dept.PostConfigPackageServiceImpl;
import cn.iocoder.yudao.module.system.service.dept.PostServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual writer behavior with explicit repository/authority boundaries; not database concurrency proof. */
class ApprovalGlobalConfigurationWriterTest {
    private final GlobalApprovalAuthorityCoordinator global = mock(GlobalApprovalAuthorityCoordinator.class);
    private final ApprovalAuthorityApi local = mock(ApprovalAuthorityApi.class);
    private final List<ApprovalAuthorityRevisionDTO> allTenants = List.of(
            new ApprovalAuthorityRevisionDTO(7L, 3L), new ApprovalAuthorityRevisionDTO(19L, 8L));
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(7L);
        when(global.lockGlobalRoleTenants()).thenReturn(allTenants);
        when(local.lockTenantApprovalAuthority(7L)).thenReturn(allTenants.get(0));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void postCreationReadsCurrentSequenceOnlyAfterLockingGlobalAuthority() {
        PostMapper mapper = mock(PostMapper.class);
        PostServiceImpl service = new PostServiceImpl();
        ReflectionTestUtils.setField(service, "postMapper", mapper);
        ReflectionTestUtils.setField(service, "approvalAuthority", global);
        when(mapper.selectGeneratedCodesForUpdate()).thenReturn(List.of("GW-009"));
        when(mapper.insert(any(PostDO.class))).thenReturn(1);

        service.createPost(new PostSaveReqVO().setName("生产组长").setSort(0).setStatus(0));

        var order = inOrder(global, mapper);
        order.verify(global).lockGlobalRoleTenants();
        order.verify(mapper).selectGeneratedCodesForUpdate();
        order.verify(mapper).insert(argThat((PostDO row) -> "GW-010".equals(row.getCode())));
        order.verify(global).incrementLockedTenants(allTenants);
    }

    @Test void sharedPostDisableSerializesEveryAffectedTenantBeforePostRead() {
        PostMapper mapper = mock(PostMapper.class);
        PostServiceImpl service = new PostServiceImpl();
        ReflectionTestUtils.setField(service, "postMapper", mapper);
        ReflectionTestUtils.setField(service, "approvalAuthority", global);
        PostDO existing = new PostDO(); existing.setId(12L);
        when(mapper.selectById(12L)).thenReturn(existing);
        when(mapper.updateById(any(PostDO.class))).thenReturn(1);
        PostSaveReqVO request = new PostSaveReqVO(); request.setId(12L); request.setName("approver");
        request.setStatus(1);
        service.updatePost(request);
        var order = inOrder(global, mapper);
        order.verify(global).lockGlobalRoleTenants();
        order.verify(mapper).selectById(12L);
        order.verify(mapper).updateById(argThat((PostDO row) -> row.getStatus() == 1));
        order.verify(global).incrementLockedTenants(allTenants);
    }

    @Test void missingGlobalPostScopePreventsEvenTheFirstPostRead() {
        PostMapper mapper = mock(PostMapper.class);
        PostServiceImpl service = new PostServiceImpl();
        ReflectionTestUtils.setField(service, "postMapper", mapper);
        ReflectionTestUtils.setField(service, "approvalAuthority", global);
        when(global.lockGlobalRoleTenants()).thenThrow(new IllegalStateException("missing formal scope"));
        assertThrows(IllegalStateException.class, () -> service.deletePost(12L));
        verifyNoInteractions(mapper);
    }

    @Test void postImportCannotAdvanceRevisionAfterIncompleteRepositoryWrite() {
        PostMapper mapper = mock(PostMapper.class);
        PostConfigPackageServiceImpl service = new PostConfigPackageServiceImpl();
        ReflectionTestUtils.setField(service, "postMapper", mapper);
        ReflectionTestUtils.setField(service, "approvalAuthority", global);
        byte[] payload = "{\"packageVersion\":\"1\",\"posts\":[{\"code\":\"approver\",\"name\":\"approver\",\"sort\":1,\"status\":0}]}"
                .getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalStateException.class, () -> service.importPackage(payload));
        var order = inOrder(global, mapper);
        order.verify(global).lockGlobalRoleTenants();
        order.verify(mapper).selectByCode("approver");
        order.verify(mapper).insert(any(PostDO.class));
        verify(global, never()).incrementLockedTenants(anyList());
    }

    @Test void globalMenuRemovalKeepsMembershipRevocationInsideSameAuthorityScope() {
        MenuMapper mapper = mock(MenuMapper.class);
        PermissionService permissions = mock(PermissionService.class);
        MenuServiceImpl service = new MenuServiceImpl();
        ReflectionTestUtils.setField(service, "menuMapper", mapper);
        ReflectionTestUtils.setField(service, "permissionService", permissions);
        ReflectionTestUtils.setField(service, "approvalAuthority", global);
        MenuDO menu = new MenuDO(); menu.setId(12L);
        when(mapper.selectCountByParentId(12L)).thenReturn(0L);
        when(mapper.selectById(12L)).thenReturn(menu);
        when(mapper.deleteById(12L)).thenReturn(1);
        service.deleteMenu(12L);
        var order = inOrder(global, mapper, permissions);
        order.verify(global).lockGlobalRoleTenants();
        order.verify(mapper).deleteById(12L);
        order.verify(permissions).processMenuDeleted(12L);
        order.verify(global).incrementLockedTenants(allTenants);
    }

    @Test void failedMenuDeletionCannotRevokeRolesOrClaimNewAuthority() {
        MenuMapper mapper = mock(MenuMapper.class);
        PermissionService permissions = mock(PermissionService.class);
        MenuServiceImpl service = new MenuServiceImpl();
        ReflectionTestUtils.setField(service, "menuMapper", mapper);
        ReflectionTestUtils.setField(service, "permissionService", permissions);
        ReflectionTestUtils.setField(service, "approvalAuthority", global);
        when(mapper.selectCountByParentId(12L)).thenReturn(0L);
        when(mapper.selectById(12L)).thenReturn(new MenuDO());
        assertThrows(IllegalStateException.class, () -> service.deleteMenu(12L));
        verifyNoInteractions(permissions);
        verify(global, never()).incrementLockedTenants(anyList());
    }

    @Test void departmentLeaderChangeUsesFormalLocalAuthorityBeforeBusinessRead() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(
                        new com.baomidou.mybatisplus.core.MybatisConfiguration(), "global-writer-test"), DeptDO.class);
        DeptMapper mapper = mock(DeptMapper.class);
        DeptServiceImpl service = new DeptServiceImpl();
        ReflectionTestUtils.setField(service, "deptMapper", mapper);
        ReflectionTestUtils.setField(service, "approvalAuthority", local);
        ReflectionTestUtils.setField(service, "supervisorService",
                mock(cn.iocoder.yudao.module.system.service.dept.DeptSupervisorService.class));
        DeptDO existing = new DeptDO(); existing.setId(12L);
        when(mapper.selectById(12L)).thenReturn(existing);
        when(mapper.updateById(any(DeptDO.class))).thenReturn(1);
        when(mapper.update(isNull(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(1);
        DeptSaveReqVO request = new DeptSaveReqVO(); request.setId(12L); request.setName("production");
        request.setLeaderUserId(29L); request.setStatus(0);
        service.updateDept(request);
        var order = inOrder(local, mapper);
        order.verify(local).lockTenantApprovalAuthority(7L);
        order.verify(mapper).selectById(12L);
        order.verify(mapper).updateById(argThat((DeptDO row) -> row.getLeaderUserId() == null));
        order.verify(mapper).update(isNull(), argThat(update ->
                update.getSqlSet().contains("leader_user_id")
                        && ((com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?>) update)
                        .getParamNameValuePairs().containsValue(29L)));
        order.verify(local).incrementLockedAuthorityRevision(7L, 3L);
        verifyNoInteractions(global);
    }
}
