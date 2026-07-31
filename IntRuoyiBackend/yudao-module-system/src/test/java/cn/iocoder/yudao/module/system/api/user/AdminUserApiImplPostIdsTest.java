package cn.iocoder.yudao.module.system.api.user;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUserApiImplPostIdsTest {

    @InjectMocks
    private AdminUserApiImpl adminUserApi;
    @Mock
    private AdminUserService userService;
    @Mock
    private DeptService deptService;
    @Mock
    private UserPostMapper userPostMapper;

    @Test
    void getUser_shouldExposeFormalPostRelationIds() {
        Long userId = 1L;
        AdminUserDO user = AdminUserDO.builder()
                .id(userId)
                .username("device-account-1")
                .nickname("设备账号 1")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .postIds(Set.of(999L))
                .build();
        when(userService.getUser(userId)).thenReturn(user);
        when(userPostMapper.selectListByUserId(userId)).thenReturn(List.of(
                new UserPostDO().setUserId(userId).setPostId(701L),
                new UserPostDO().setUserId(userId).setPostId(702L)));

        AdminUserRespDTO respDTO = adminUserApi.getUser(userId);

        assertEquals(Set.of(701L, 702L), respDTO.getPostIds());
    }

}
