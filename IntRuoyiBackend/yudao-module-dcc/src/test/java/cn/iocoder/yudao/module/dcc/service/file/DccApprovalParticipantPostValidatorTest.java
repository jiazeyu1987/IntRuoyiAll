package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_APPROVER_POST_REQUIRED;
import static org.mockito.Mockito.when;

class DccApprovalParticipantPostValidatorTest extends BaseMockitoUnitTest {

    @Mock
    private AdminUserApi adminUserApi;

    @InjectMocks
    private DccApprovalParticipantPostValidator validator;

    @Test
    void requireConfiguredPosts_userWithoutPostFailsWithDedicatedError() {
        when(adminUserApi.getUserList(List.of(101L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(101L).setNickname("无岗位审批人").setPostIds(Set.of())));

        assertServiceException(() -> validator.requireConfiguredPosts(List.of(101L)),
                CONTROLLED_FILE_APPROVER_POST_REQUIRED);
    }

    @Test
    void requireConfiguredPosts_allUsersHavePostPasses() {
        when(adminUserApi.getUserList(List.of(101L, 102L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(101L).setPostIds(Set.of(11L)),
                new AdminUserRespDTO().setId(102L).setPostIds(Set.of(12L))));

        validator.requireConfiguredPosts(List.of(101L, 102L));
    }
}
