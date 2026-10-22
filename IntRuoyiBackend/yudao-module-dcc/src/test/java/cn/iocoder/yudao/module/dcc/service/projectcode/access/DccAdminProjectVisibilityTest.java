package cn.iocoder.yudao.module.dcc.service.projectcode.access;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectAccessRuleMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccAdminProjectVisibilityTest {
    @Test void adminReadsAllEnabledTenantProjectsButDoesNotGainWriteAccess() {
        TenantContextHolder.setTenantId(1L);
        try {
            var service = new DccProjectAccessServiceImpl();
            var users = mock(AdminUserApi.class);
            var permissions = mock(PermissionApi.class);
            var projects = mock(DccProjectCodeMapper.class);
            var rules = mock(DccProjectAccessRuleMapper.class);
            ReflectionTestUtils.setField(service,"adminUserApi",users);
            ReflectionTestUtils.setField(service,"permissionApi",permissions);
            ReflectionTestUtils.setField(service,"projectCodeMapper",projects);
            ReflectionTestUtils.setField(service,"accessRuleMapper",rules);
            var user = new AdminUserRespDTO();
            user.setId(99L); user.setTenantId(1L); user.setStatus(0);
            when(users.getUser(99L)).thenReturn(user);
            when(permissions.hasAnyRoles(99L,"super_admin")).thenReturn(true);
            when(permissions.getUserRoleIdListByUserId(99L)).thenReturn(Set.of());
            var own = project(10L,1L,"ENABLE");
            var foreign = project(11L,2L,"ENABLE");
            var disabled = project(12L,1L,"DISABLE");
            when(projects.selectEnabledList()).thenReturn(List.of(own,foreign,disabled));
            when(projects.selectById(10L)).thenReturn(own);
            when(projects.selectById(11L)).thenReturn(foreign);
            when(projects.selectById(12L)).thenReturn(disabled);
            assertEquals(List.of(10L),service.listReadableProjectIds(99L));
            assertDoesNotThrow(()->service.assertProjectViewerOrAbove(99L,10L));
            assertThrows(RuntimeException.class,()->service.assertProjectViewerOrAbove(99L,11L));
            assertThrows(RuntimeException.class,()->service.assertProjectViewerOrAbove(99L,12L));
            assertFalse(service.hasProjectEditorOrOwner(99L,10L));
            assertFalse(service.hasProjectOwner(99L,10L));
            when(permissions.hasAnyRoles(99L,"super_admin")).thenReturn(false);
            assertEquals(List.of(),service.listReadableProjectIds(99L));
            assertThrows(RuntimeException.class,()->service.assertProjectViewerOrAbove(99L,10L));
            when(permissions.hasAnyRoles(99L,"super_admin")).thenReturn(true);
            user.setStatus(1);
            assertThrows(RuntimeException.class,()->service.listReadableProjectIds(99L));
            assertThrows(RuntimeException.class,()->service.assertProjectViewerOrAbove(99L,10L));
            user.setStatus(0); user.setTenantId(2L);
            assertThrows(RuntimeException.class,()->service.listReadableProjectIds(99L));
            assertThrows(RuntimeException.class,()->service.assertProjectViewerOrAbove(99L,10L));
        } finally { TenantContextHolder.clear(); }
    }
    private static DccProjectCodeDO project(Long id,Long tenant,String status) {
        var value=new DccProjectCodeDO(); value.setId(id); value.setTenantId(tenant); value.setStatus(status); return value;
    }
}
