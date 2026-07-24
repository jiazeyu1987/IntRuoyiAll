package cn.iocoder.yudao.module.bpm.businessapproval.controller;

import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.BusinessApprovalPolicyController;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicySaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicySwitchModeReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessApprovalPolicyControllerContractTest {

    @Test
    void controllerRoutesAndPermissionsMatchFrontendContract() throws Exception {
        assertArrayEquals(new String[]{"/business-approval/policies"},
                BusinessApprovalPolicyController.class.getAnnotation(RequestMapping.class).value());

        Method page = BusinessApprovalPolicyController.class.getDeclaredMethod("getPolicyPage",
                BusinessApprovalPolicyPageReqVO.class);
        assertArrayEquals(new String[]{""}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('bpm:business-approval-policy:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method save = BusinessApprovalPolicyController.class.getDeclaredMethod("savePolicy",
                BusinessApprovalPolicySaveReqVO.class);
        assertArrayEquals(new String[]{""}, save.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('bpm:business-approval-policy:create')",
                save.getAnnotation(PreAuthorize.class).value());

        Method publish = BusinessApprovalPolicyController.class.getDeclaredMethod("publishPolicy", Long.class);
        assertArrayEquals(new String[]{"/{policyId}/publish"}, publish.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('bpm:business-approval-policy:publish')",
                publish.getAnnotation(PreAuthorize.class).value());

        Method disable = BusinessApprovalPolicyController.class.getDeclaredMethod("disablePolicy", Long.class);
        assertArrayEquals(new String[]{"/{policyId}/disable"}, disable.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('bpm:business-approval-policy:disable')",
                disable.getAnnotation(PreAuthorize.class).value());

        Method switchMode = BusinessApprovalPolicyController.class.getDeclaredMethod("switchPolicyMode",
                Long.class, BusinessApprovalPolicySwitchModeReqVO.class);
        assertArrayEquals(new String[]{"/{policyId}/switch-mode"},
                switchMode.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('bpm:business-approval-policy:publish')",
                switchMode.getAnnotation(PreAuthorize.class).value());
    }

}
