package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Import(UserDisplayService.class)
class UserDisplayServiceTest extends UserPostRetentionTestSupport {
    @Resource private UserDisplayService display;
    @Resource private RoleMapper roleMapper;
    @Resource private UserRoleMapper userRoleMapper;

    @Test void formalBindingsAndDisabledLabelsAreReturnedOnlyForRequestedUsers() {
        var user = boundUser(Set.of(10L, 20L));
        // 旧 JSON 与正式关联故意不一致，读取结果必须以关联表为准。
        userMapper.updateById(new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO()
                .setId(user.getId()).setPostIds(Set.of(30L)));
        var other = boundUser(Set.of(30L));
        var role = new RoleDO().setName("审核员").setCode("supervisor-display").setSort(1).setStatus(1).setType(2);
        roleMapper.insert(role);
        userRoleMapper.insert(new UserRoleDO().setUserId(user.getId()).setRoleId(role.getId()));
        var result = display.describe(List.of(userMapper.selectById(user.getId())));
        assertEquals(1, result.size());
        assertEquals(user.getId(), result.get(0).getId());
        assertEquals(Set.of(10L, 20L), result.get(0).getPostIds());
        assertEquals(List.of("启用岗位10", "停用岗位（已停用）"), result.get(0).getPostNames());
        assertEquals(List.of("审核员（已停用）"), result.get(0).getRoleNames());
        assertNotEquals(other.getId(), result.get(0).getId());
    }

    @Test void userWithoutDepartmentOrBindingsCanBeRead() {
        var user = seedUser(Set.of());
        var result = display.describe(List.of(user)).get(0);
        assertTrue(result.getPostNames().isEmpty());
        assertTrue(result.getRoleNames().isEmpty());
        assertNull(result.getDeptName());
    }
}
