package cn.iocoder.yudao.module.showroom.controller.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterDetailRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterRepublishReqVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterRepublishRespVO;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionCenterService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowFacade;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomVersionCenterControllerTest extends BaseMockitoUnitTest {

    private static final String SITE_KEY = "yingtai-showroom";
    private static final String STAGE = "TEST";

    @Mock
    private ShowroomVersionCenterService versionCenterService;
    @Mock
    private ShowroomAssignmentService assignmentService;
    @Mock
    private ShowroomWorkflowFacade workflowFacade;
    @Mock
    private SecurityFrameworkService securityFrameworkService;

    @InjectMocks
    private ShowroomVersionCenterController controller;

    @Test
    void republishShouldRejectNonPublicityUser() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(801L);
            when(securityFrameworkService.hasRole(ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE)).thenReturn(false);
            when(securityFrameworkService.hasRole(eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);

            ServiceException exception = assertThrows(ServiceException.class, () -> controller.republish(
                    new ShowroomVersionCenterRepublishReqVO("PRODUCT", 1001L, 3001L, SITE_KEY, STAGE)));

            assertEquals(403, exception.getCode());
            verify(versionCenterService, never()).republish(any(), any());
        }
    }

    @Test
    void republishShouldAllowShowroomPublicityUser() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(802L);
            when(securityFrameworkService.hasRole(ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE)).thenReturn(true);
            ShowroomVersionCenterRepublishRespVO response = new ShowroomVersionCenterRepublishRespVO(
                    "PRODUCT", 1001L, 3001L, 4001L, 6, "release-1", "hash-1", "2026-05-23T14:00:00Z");
            when(versionCenterService.republish(any(), eq(802L))).thenReturn(response);

            ShowroomVersionCenterRepublishRespVO result = controller.republish(
                    new ShowroomVersionCenterRepublishReqVO("PRODUCT", 1001L, 3001L, SITE_KEY, STAGE)).getCheckedData();

            assertEquals(4001L, result.newRevisionId());
            verify(versionCenterService).republish(any(), eq(802L));
        }
    }

    @Test
    void detailShouldOverrideRepublishPermissionForNonPublicityUser() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(803L);
            when(securityFrameworkService.hasRole(ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE)).thenReturn(false);
            when(securityFrameworkService.hasRole(eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
            when(versionCenterService.getDetail("COMPANY", 1001L, 3001L, SITE_KEY, STAGE))
                    .thenReturn(detail(true, null));

            ShowroomVersionCenterDetailRespVO result = controller.getDetail("COMPANY", 1001L, 3001L,
                    SITE_KEY, STAGE).getCheckedData();

            assertFalse(result.permissions().canRepublish());
            assertEquals("SHOWROOM_VERSION_REPUBLISH_FORBIDDEN: 当前用户无权执行版本重发",
                    result.permissions().republishDisabledReason());
        }
    }

    @Test
    void detailShouldKeepServicePermissionForPublicityUser() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(804L);
            when(securityFrameworkService.hasRole(ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE)).thenReturn(true);
            when(versionCenterService.getDetail("COMPANY", 1001L, 3001L, SITE_KEY, STAGE))
                    .thenReturn(detail(true, null));

            ShowroomVersionCenterDetailRespVO result = controller.getDetail("COMPANY", 1001L, 3001L,
                    SITE_KEY, STAGE).getCheckedData();

            assertTrue(result.permissions().canRepublish());
        }
    }

    private static ShowroomVersionCenterDetailRespVO detail(boolean canRepublish, String disabledReason) {
        return new ShowroomVersionCenterDetailRespVO(
                new ShowroomVersionCenterDetailRespVO.TargetSummaryRespVO("COMPANY", 1001L,
                        "盈泰医疗", "Yingtai Medical", 3001L, 3001L),
                null,
                null,
                null,
                null,
                List.of(),
                new ShowroomVersionCenterDetailRespVO.PermissionRespVO(canRepublish, disabledReason),
                new ShowroomVersionCenterDetailRespVO.RepublishReadinessRespVO(canRepublish, List.of())
        );
    }
}
