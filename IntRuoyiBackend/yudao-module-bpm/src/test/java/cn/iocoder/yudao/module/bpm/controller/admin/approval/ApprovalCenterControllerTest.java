package cn.iocoder.yudao.module.bpm.controller.admin.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalCenterService;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalProviderDescriptor;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQuery;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalCenterControllerTest {

    @Mock
    private ApprovalCenterService approvalCenterService;
    @InjectMocks
    private ApprovalCenterController controller;

    @Test
    void modulesEndpointDelegatesToService() {
        List<ApprovalProviderDescriptor> descriptors = List.of(new ApprovalProviderDescriptor()
                .setModuleCode(ApprovalModuleCode.SHOWROOM)
                .setModuleName("Showroom 审批")
                .setProviderCode("showroom-approval")
                .setProviderVersion("phase1")
                .setSupportedViewTypes(Set.of(ApprovalTaskViewType.TODO))
                .setCapabilities(Set.of(ApprovalTaskCapability.TIMELINE)));
        when(approvalCenterService.listProviders(100L)).thenReturn(descriptors);

        try (MockedStatic<WebFrameworkUtils> webFrameworkUtils = mockStatic(WebFrameworkUtils.class)) {
            webFrameworkUtils.when(WebFrameworkUtils::getLoginUserId).thenReturn(100L);

            assertSame(descriptors, controller.getModules().getData());
        }

        verify(approvalCenterService).listProviders(100L);
    }

    @Test
    void tasksEndpointDelegatesToService() {
        ApprovalTaskPageReqVO reqVO = new ApprovalTaskPageReqVO()
                .setViewType(ApprovalTaskViewType.TODO)
                .setModuleCode(ApprovalModuleCode.SHOWROOM);
        PageResult<ApprovalTaskSummary> page = PageResult.empty();
        when(approvalCenterService.getTaskPage(100L, reqVO.toQuery())).thenReturn(page);

        try (MockedStatic<WebFrameworkUtils> webFrameworkUtils = mockStatic(WebFrameworkUtils.class)) {
            webFrameworkUtils.when(WebFrameworkUtils::getLoginUserId).thenReturn(100L);

            assertSame(page, controller.getTaskPage(reqVO).getData());
        }

        verify(approvalCenterService).getTaskPage(100L, reqVO.toQuery());
    }

    @Test
    void controllerRoutesAndPermissionsAreStable() throws Exception {
        assertArrayEquals(new String[]{"/approval-center"},
                ApprovalCenterController.class.getAnnotation(RequestMapping.class).value());

        Method modules = ApprovalCenterController.class.getDeclaredMethod("getModules");
        assertArrayEquals(new String[]{"/modules"}, modules.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('bpm:task:query')",
                modules.getAnnotation(PreAuthorize.class).value());

        Method tasks = ApprovalCenterController.class.getDeclaredMethod("getTaskPage", ApprovalTaskPageReqVO.class);
        assertArrayEquals(new String[]{"/tasks/page"}, tasks.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('bpm:task:query')",
                tasks.getAnnotation(PreAuthorize.class).value());

        Method review = ApprovalCenterController.class.getDeclaredMethod("reviewTask", ApprovalTaskReviewReqVO.class);
        assertArrayEquals(new String[]{"/tasks/review"}, review.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('bpm:task:update')",
                review.getAnnotation(PreAuthorize.class).value());

        ApprovalTaskPageReqVO.class.getDeclaredMethod("toQuery");
        ApprovalTaskReviewReqVO.class.getDeclaredMethod("getSignaturePassword");
        ApprovalTaskQuery.class.getDeclaredMethod("getViewType");
        ApprovalTaskSummary.class.getDeclaredMethod("getDetailRoute");
    }
}
