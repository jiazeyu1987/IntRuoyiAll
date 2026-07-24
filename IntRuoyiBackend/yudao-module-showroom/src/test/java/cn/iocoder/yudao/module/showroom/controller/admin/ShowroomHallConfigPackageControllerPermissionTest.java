package cn.iocoder.yudao.module.showroom.controller.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.showroom.configpackage.ShowroomHallConfigPackageService;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.hall.ShowroomHallConfigPackageImportRespVO;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomApprovalActorResolver;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowFacade;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomHallConfigPackageControllerPermissionTest extends BaseMockitoUnitTest {

    @Mock
    private ShowroomApiRuntime runtime;
    @Mock
    private ShowroomWorkflowFacade workflowFacade;
    @Mock
    private ShowroomAssignmentService assignmentService;
    @Mock
    private ShowroomApprovalActorResolver approvalActorResolver;
    @Mock
    private ShowroomProductCommentService commentService;
    @Mock
    private SecurityFrameworkService securityFrameworkService;
    @Mock
    private FileService fileService;
    @Mock
    private ShowroomHallConfigPackageService hallConfigPackageService;

    @InjectMocks
    private ShowroomAdminController controller;

    @Test
    void exportShouldRejectNonPublicityUser() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(801L);
            when(securityFrameworkService.hasRole(ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE)).thenReturn(false);
            when(securityFrameworkService.hasRole(eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> controller.exportHallConfigPackage(new MockHttpServletResponse()));

            assertEquals(403, exception.getCode());
            verify(hallConfigPackageService, never()).exportPackage();
        }
    }

    @Test
    void exportShouldAllowShowroomPublicityUser() throws Exception {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(802L);
            when(securityFrameworkService.hasRole(ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE)).thenReturn(true);
            byte[] expected = "zip".getBytes();
            when(hallConfigPackageService.exportPackage()).thenReturn(expected);
            HttpServletResponse response = new MockHttpServletResponse();

            controller.exportHallConfigPackage(response);

            assertArrayEquals(expected, ((MockHttpServletResponse) response).getContentAsByteArray());
            verify(hallConfigPackageService).exportPackage();
        }
    }

    @Test
    void importShouldRejectNonPublicityUser() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(803L);
            when(securityFrameworkService.hasRole(ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE)).thenReturn(false);
            when(securityFrameworkService.hasRole(eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
            MockMultipartFile file = new MockMultipartFile("file", "package.zip", "application/zip", "zip".getBytes());

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> controller.importHallConfigPackage(file));

            assertEquals(403, exception.getCode());
            verify(hallConfigPackageService, never()).importPackage(any());
        }
    }

    @Test
    void importShouldAllowSuperAdmin() throws Exception {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(securityFrameworkService.hasRole(ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE)).thenReturn(false);
            when(securityFrameworkService.hasRole(eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(true);
            MockMultipartFile file = new MockMultipartFile("file", "package.zip", "application/zip", "zip".getBytes());
            ShowroomHallConfigPackageImportRespVO respVO =
                    new ShowroomHallConfigPackageImportRespVO(1, 2, 1, 2, 1, 0, 0, 1, 1);
            when(hallConfigPackageService.importPackage(file.getBytes())).thenReturn(respVO);

            ShowroomHallConfigPackageImportRespVO result = controller.importHallConfigPackage(file).getCheckedData();

            assertEquals(2, result.keywordCount());
            verify(hallConfigPackageService).importPackage(file.getBytes());
        }
    }

}
