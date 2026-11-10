package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessServiceImpl;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real authorization services; only persistent rules and account boundaries are mocked. */
final class DccLibraryUploadPermissionFixture {
    final DccControlledFileCategoryPermissionSupport categories = new DccControlledFileCategoryPermissionSupport();
    final DccProjectAccessServiceImpl access = new DccProjectAccessServiceImpl();
    final DccProjectAccessRuleDO rule = new DccProjectAccessRuleDO();
    final DccProjectCodeDO project;
    final PermissionApi permissions = mock(PermissionApi.class);

    DccLibraryUploadPermissionFixture(DccProjectCodeMapper projects, Long projectId, Long actor, String scope, String level) {
        Long tenant=cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        project=DccProjectCodeDO.builder().id(projectId).status("ENABLE")
                .projectCode("LIB-"+scope).projectName(scope).libraryScope(scope).build();
        project.setTenantId(tenant);
        lenient().when(projects.selectById(projectId)).thenReturn(project);
        var users=mock(AdminUserApi.class);var departments=mock(DeptApi.class);
        lenient().when(users.getUser(actor)).thenReturn(new AdminUserRespDTO().setId(actor).setTenantId(tenant).setStatus(0));
        lenient().when(permissions.getUserRoleIdListByUserId(actor)).thenReturn(Set.of());
        lenient().when(permissions.hasAnyPermissions(eq(actor),any(String[].class))).thenAnswer(call ->
                java.util.Arrays.stream((String[])call.getRawArguments()[1]).anyMatch(permission -> {
                    if (!permission.startsWith("dcc:library:")) return true;
                    String prefix="dcc:library:"+scope.toLowerCase(java.util.Locale.ROOT)+":";
                    return Boolean.TRUE.equals(rule.getActive()) && permission.startsWith(prefix)
                            && (permission.equals(prefix+"view") && Set.of("VIEW","EDIT").contains(rule.getAccessLevel())
                                || permission.equals(prefix+"edit") && "EDIT".equals(rule.getAccessLevel()));
                }));
        var rules=mock(DccProjectAccessRuleMapper.class);
        rule.setTenantId(tenant);rule.setDccProjectCodeId(projectId);rule.setSubjectType("USER");
        rule.setSubjectId(actor);rule.setAccessLevel(level);rule.setActive(true);
        lenient().when(rules.selectActiveRules(eq(projectId),any(LocalDateTime.class))).thenAnswer(call->List.of(rule));
        wire(access,"projectCodeMapper",projects,"accessRuleMapper",rules,"adminUserApi",users,
                "permissionApi",permissions,"deptApi",departments);
        wire(categories,"projectMapper",projects,"projectAccessService",access,"adminUserApi",users,
                "permissionApi",permissions,"deptApi",departments,
                "permissionRuleMapper",mock(DccFileCategoryPermissionRuleMapper.class));
    }
    static void wire(Object target,Object...fields) {
        for(int i=0;i<fields.length;i+=2) ReflectionTestUtils.setField(target,(String)fields[i],fields[i+1]);
    }
}
