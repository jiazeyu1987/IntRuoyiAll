package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserRespVO;
import cn.iocoder.yudao.module.system.convert.user.UserConvert;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.*;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.dept.*;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 只补充已经过权限过滤的用户结果，不扩大用户查询范围。 */
@Service
public class UserDisplayService {
    @Resource private DeptMapper deptMapper;
    @Resource private PostMapper postMapper;
    @Resource private RoleMapper roleMapper;
    @Resource private UserPostMapper userPostMapper;
    @Resource private UserRoleMapper userRoleMapper;
    @Resource private cn.iocoder.yudao.module.system.service.tenant.TenantService tenantService;

    @DataPermission(enable = false)
    public List<UserRespVO> describe(List<AdminUserDO> users) {
        if (users.isEmpty()) return List.of();
        Set<Long> ids = users.stream().map(AdminUserDO::getId).collect(Collectors.toSet());
        Set<Long> deptIds = users.stream().map(AdminUserDO::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, DeptDO> departments = deptIds.isEmpty() ? new HashMap<>() : deptMapper.selectByIds(deptIds).stream()
                .collect(Collectors.toMap(DeptDO::getId, Function.identity()));
        List<UserPostDO> postLinks = userPostMapper.selectList(UserPostDO::getUserId, ids);
        List<UserRoleDO> roleLinks = userRoleMapper.selectList(UserRoleDO::getUserId, ids);
        Set<Long> postIds = postLinks.stream().map(UserPostDO::getPostId).collect(Collectors.toSet());
        Set<Long> roleIds = roleLinks.stream().map(UserRoleDO::getRoleId).collect(Collectors.toSet());
        Map<Long, PostDO> posts = postIds.isEmpty() ? Map.of() : postMapper.selectByIds(postIds).stream()
                .collect(Collectors.toMap(PostDO::getId, Function.identity()));
        Map<Long, RoleDO> roles = roleIds.isEmpty() ? Map.of() : roleMapper.selectByIds(roleIds).stream()
                .collect(Collectors.toMap(RoleDO::getId, Function.identity()));
        Map<Long, List<UserPostDO>> postsByUser = postLinks.stream().collect(Collectors.groupingBy(UserPostDO::getUserId));
        Map<Long, List<UserRoleDO>> rolesByUser = roleLinks.stream().collect(Collectors.groupingBy(UserRoleDO::getUserId));
        List<UserRespVO> result = UserConvert.INSTANCE.convertList(users, departments);
        var yintaiTenant = YintaiAdminAccountPolicy.TENANT_ID.equals(
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getTenantId())
                ? tenantService.getTenant(YintaiAdminAccountPolicy.TENANT_ID) : null;
        Map<Long, AdminUserDO> usersById = users.stream()
                .collect(Collectors.toMap(AdminUserDO::getId, Function.identity()));
        for (UserRespVO user : result) {
            user.setYintaiAdminUsernameAllowed(YintaiAdminAccountPolicy.isAdministrator(
                    usersById.get(user.getId()), yintaiTenant));
            List<Long> assignedPosts = postsByUser.getOrDefault(user.getId(), List.of()).stream()
                    .map(UserPostDO::getPostId).distinct().sorted().toList();
            user.setPostIds(new LinkedHashSet<>(assignedPosts));
            user.setPostNames(assignedPosts.stream().map(id -> {
                PostDO post = posts.get(id);
                return post == null ? "已删除岗位（" + id + "）" : post.getName()
                        + (Integer.valueOf(1).equals(post.getStatus()) ? "（已停用）" : "");
            }).toList());
            user.setRoleNames(rolesByUser.getOrDefault(user.getId(), List.of()).stream()
                    .map(UserRoleDO::getRoleId).distinct().sorted().map(id -> {
                        RoleDO role = roles.get(id);
                        return role == null ? "已删除角色（" + id + "）" : role.getName()
                                + (Integer.valueOf(1).equals(role.getStatus()) ? "（已停用）" : "");
                    }).toList());
        }
        return result;
    }
}
