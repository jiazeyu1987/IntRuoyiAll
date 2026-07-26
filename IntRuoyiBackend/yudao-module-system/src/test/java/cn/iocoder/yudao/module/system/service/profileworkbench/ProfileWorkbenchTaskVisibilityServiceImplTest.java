package cn.iocoder.yudao.module.system.service.profileworkbench;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.profileworkbench.vo.ProfileWorkbenchTaskVisibilitySaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.profileworkbench.ProfileWorkbenchTaskVisibilityDO;
import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.ProfileWorkbenchTaskVisibilityMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.PROFILE_WORKBENCH_TASK_VISIBILITY_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

@Import(ProfileWorkbenchTaskVisibilityServiceImpl.class)
class ProfileWorkbenchTaskVisibilityServiceImplTest extends BaseDbUnitTest {

    @Resource
    private ProfileWorkbenchTaskVisibilityService profileWorkbenchTaskVisibilityService;
    @Resource
    private ProfileWorkbenchTaskVisibilityMapper profileWorkbenchTaskVisibilityMapper;

    @Test
    void hideAndListHiddenKeys_currentUserOnly() {
        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);

            profileWorkbenchTaskVisibilityService.hideTask(saveReq("eDHR工作任务:9001", "批记录",
                    "eDHR工作任务", "9001", "批记录任务 9001"));

            List<String> hiddenKeys = profileWorkbenchTaskVisibilityService.getHiddenTaskKeys();
            ProfileWorkbenchTaskVisibilityDO stored =
                    profileWorkbenchTaskVisibilityMapper.selectByUserAndTaskKey(101L, "eDHR工作任务:9001");

            assertEquals(List.of("eDHR工作任务:9001"), hiddenKeys);
            assertNotNull(stored);
            assertEquals(11L, stored.getTenantId());
            assertEquals("批记录", stored.getTaskType());
            assertEquals("eDHR工作任务", stored.getSource());

            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(202L);
            assertTrue(profileWorkbenchTaskVisibilityService.getHiddenTaskKeys().isEmpty());
        }
    }

    @Test
    void restoreTask_removesCurrentUserOnly() {
        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            profileWorkbenchTaskVisibilityService.hideTask(saveReq("文控培训:3001", "文控",
                    "文控培训", "3001", "培训任务"));

            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(202L);
            profileWorkbenchTaskVisibilityService.hideTask(saveReq("文控培训:3001", "文控",
                    "文控培训", "3001", "培训任务"));

            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            profileWorkbenchTaskVisibilityService.restoreTask("文控培训:3001");

            assertFalse(profileWorkbenchTaskVisibilityService.getHiddenTaskKeys().contains("文控培训:3001"));
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(202L);
            assertEquals(List.of("文控培训:3001"), profileWorkbenchTaskVisibilityService.getHiddenTaskKeys());
        }
    }

    @Test
    void invalidRequest_failFast() {
        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);

            assertServiceException(() -> profileWorkbenchTaskVisibilityService.hideTask(saveReq("", "文控",
                    "文控培训", "3001", "培训任务")), PROFILE_WORKBENCH_TASK_VISIBILITY_INVALID);
            assertServiceException(() -> profileWorkbenchTaskVisibilityService.restoreTask(""),
                    PROFILE_WORKBENCH_TASK_VISIBILITY_INVALID);
        }
    }

    private static ProfileWorkbenchTaskVisibilitySaveReqVO saveReq(String taskKey, String taskType,
                                                                    String source, String businessId,
                                                                    String detail) {
        return new ProfileWorkbenchTaskVisibilitySaveReqVO()
                .setTaskKey(taskKey)
                .setTaskType(taskType)
                .setSource(source)
                .setBusinessId(businessId)
                .setDetail(detail);
    }
}
