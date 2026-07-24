package cn.iocoder.yudao.module.mes.controller.admin.md.workstation;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesMdWorkshopControllerChargeUserListTest {

    @Mock
    private cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService workshopService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PostService postService;
    @InjectMocks
    private MesMdWorkshopController controller;

    @Test
    void getWorkshopChargeUserList_returnsOnlyEnabledWorkshopDirectorUsers() {
        PostDO workshopDirectorPost = new PostDO();
        workshopDirectorPost.setId(99L);
        workshopDirectorPost.setCode("WORKSHOP_DIRECTOR");
        workshopDirectorPost.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(postService.getPostList(isNull(), eq(Collections.singleton(CommonStatusEnum.ENABLE.getStatus()))))
                .thenReturn(List.of(workshopDirectorPost));

        AdminUserRespDTO disabledUser = new AdminUserRespDTO();
        disabledUser.setId(1L);
        disabledUser.setNickname("芋道源码");
        disabledUser.setDeptId(103L);
        disabledUser.setStatus(CommonStatusEnum.DISABLE.getStatus());
        AdminUserRespDTO enabledUser = new AdminUserRespDTO();
        enabledUser.setId(539L);
        enabledUser.setNickname("吴孝磊");
        enabledUser.setDeptId(133L);
        enabledUser.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(adminUserApi.getUserListByPostIds(eq(Set.of(99L))))
                .thenReturn(List.of(disabledUser, enabledUser));

        CommonResult<List<UserSimpleRespVO>> response = controller.getWorkshopChargeUserList();

        assertEquals(0, response.getCode());
        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals(539L, response.getData().get(0).getId());
        assertEquals("吴孝磊", response.getData().get(0).getNickname());
        verify(adminUserApi).getUserListByPostIds(eq(Set.of(99L)));
    }

    @Test
    void getWorkshopChargeUserList_throwsWhenWorkshopDirectorPostMissing() {
        when(postService.getPostList(isNull(), eq(Collections.singleton(CommonStatusEnum.ENABLE.getStatus()))))
                .thenReturn(List.of());

        assertServiceException(() -> controller.getWorkshopChargeUserList(),
                ErrorCodeConstants.MD_WORKSHOP_CHARGE_POST_NOT_READY, "WORKSHOP_DIRECTOR");
    }
}
