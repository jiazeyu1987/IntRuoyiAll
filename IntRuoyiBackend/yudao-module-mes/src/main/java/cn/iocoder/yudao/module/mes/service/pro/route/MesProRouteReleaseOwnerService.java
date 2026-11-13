package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Read-only route display; never grants role administration or release execution permissions. */
@Service
@RequiredArgsConstructor
public class MesProRouteReleaseOwnerService {
    private final RoleApi roles;
    private final PermissionApi permissions;
    private final AdminUserApi users;

    @Transactional(readOnly = true)
    public Owner getOwner(String code) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!MesProductionReleaseRoleCodes.isAllowed(code)) {
            throw invalid("不支持的放行角色：" + code);
        }
        cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO role;
        try {
            role = roles.getRoleByCode(code);
        } catch (TooManyResultsException ex) {
            throw invalid("默认权限角色必须唯一，请检查：" + code);
        }
        if (role == null) throw invalid("默认权限角色未配置：" + code);
        if (!Integer.valueOf(0).equals(role.getStatus())) {
            throw invalid("默认权限角色已停用：" + role.getName());
        }
        Set<Long> memberIds = permissions.getUserRoleIdListByRoleIds(Set.of(role.getId()));
        if (memberIds.isEmpty()) {
            return new Owner(new Role(role.getId(), role.getName()), List.of());
        }
        var members = users.getUserList(memberIds);
        if (!members.stream().map(user -> user.getId()).collect(Collectors.toSet()).equals(memberIds)
                || members.stream().anyMatch(user -> !Objects.equals(tenantId, user.getTenantId()))) {
            throw invalid("放行角色关联人员不存在或不属于当前租户，请检查授权配置");
        }
        return new Owner(new Role(role.getId(), role.getName()), members.stream()
                .map(user -> new Member(user.getId(), user.getUsername(), user.getNickname(), user.getStatus()))
                .sorted(Comparator.comparing(Member::id)).toList());
    }

    private static ServiceException invalid(String message) {
        return new ServiceException(400, message);
    }

    public record Owner(Role role, List<Member> users) { }
    public record Role(Long id, String name) { }
    public record Member(Long id, String username, String nickname, Integer status) { }
}
