package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.RoleApprovalConfigPackageController;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessApprovalPolicyTransferControllerTest {
    @Test
    void exposesRejectedImportReasonWithoutTurningItIntoSuccess() throws Exception {
        var service = mock(RoleApprovalConfigPackageService.class);
        var controller = new RoleApprovalConfigPackageController();
        ReflectionTestUtils.setField(controller, "transferService", service);
        var content = new byte[]{1, 2};
        String reason = "目标租户缺少已部署且启用的审批流程：test-flow，整批未导入";
        when(service.importPackage(content)).thenThrow(new BusinessApprovalException(
                BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID, reason));
        var result = controller.importConfigPackage(new MockMultipartFile("file", content));
        assertEquals(400, result.getCode());
        assertEquals(reason, result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void unexpectedDatabaseFailureStillPropagates() {
        var service = mock(RoleApprovalConfigPackageService.class);
        var controller = new RoleApprovalConfigPackageController();
        ReflectionTestUtils.setField(controller, "transferService", service);
        var content = new byte[]{1, 2};
        var failure = new IllegalStateException("database failure");
        when(service.importPackage(content)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> controller.importConfigPackage(new MockMultipartFile("file", content))));
    }
}
