package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.module.system.controller.admin.logger.OperateLogController;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class OperationLogAccessPolicyTest {
    private RoleDO role(String code, int status) {
        return new RoleDO().setCode(code).setStatus(status);
    }

    @Test
    void navigationRequiresEnabledAdministratorOrAuditorRole() {
        MenuDO log = new MenuDO().setId(500L).setParentId(108L).setComponent("system/operatelog/index");
        MenuDO query = new MenuDO().setId(1040L).setParentId(500L).setPermission("system:operate-log:query");
        MenuDO business = new MenuDO().setId(900436L).setParentId(0L);
        List<MenuDO> menus = List.of(query, business, log);
        for (String code : List.of("super_admin", "tenant_admin", "audit_admin", "edhr_auditor")) {
            assertEquals(menus, OperationLogAccessPolicy.filterNavigation(menus, List.of(role(code, 0))));
            assertEquals(List.of(business), OperationLogAccessPolicy.filterNavigation(menus, List.of(role(code, 1))));
        }
        for (String code : List.of("production_leader_permission", "pqc_leader_permission", "qa", "dcc_project_reviewer")) {
            assertEquals(List.of(business), OperationLogAccessPolicy.filterNavigation(menus, List.of(role(code, 0))));
        }
        assertEquals(List.of(business), OperationLogAccessPolicy.filterNavigation(menus, List.of()));
        assertEquals(3, menus.size());
        assertEquals("system:operate-log:query", query.getPermission());
    }

    @Test
    void eachSystemLogEndpointRequiresBothRoleAndItsOwnPermission() {
        var parser = new SpelExpressionParser();
        var endpoints = Arrays.stream(OperateLogController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class)).toList();
        assertEquals(3, endpoints.size());
        for (var endpoint : endpoints) {
            String expression = endpoint.getAnnotation(PreAuthorize.class).value();
            String permission = endpoint.getName().equals("exportOperateLog") ? "system:operate-log:export" : "system:operate-log:query";
            for (String code : List.of("super_admin", "tenant_admin", "audit_admin", "edhr_auditor", "production_leader_permission")) {
                for (boolean granted : List.of(true, false)) {
                    var context = new StandardEvaluationContext();
                    context.setBeanResolver((evaluationContext, beanName) -> new SecurityStub(code, granted ? permission : ""));
                    boolean expected = !code.equals("production_leader_permission") && granted;
                    assertEquals(expected, parser.parseExpression(expression).getValue(context, Boolean.class), endpoint.getName()+":"+code);
                }
            }
        }
    }

    public record SecurityStub(String role, String permission) {
        public boolean hasAnyRoles(String... roles) { return Set.of(roles).contains(role); }
        public boolean hasPermission(String requested) { return requested.equals(permission); }
    }
}
