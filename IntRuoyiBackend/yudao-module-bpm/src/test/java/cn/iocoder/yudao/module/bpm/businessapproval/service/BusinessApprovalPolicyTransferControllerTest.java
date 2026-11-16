package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.BusinessApprovalPolicyController;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyTransferVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessApprovalPolicyTransferControllerTest {
    @Test
    void exposesRejectedImportReasonWithoutTurningItIntoSuccess() {
        var service = mock(BusinessApprovalPolicyTransferService.class);
        var controller = new BusinessApprovalPolicyController();
        ReflectionTestUtils.setField(controller, "transferService", service);
        var transfer = new BusinessApprovalPolicyTransferVO();
        String reason = "目标租户缺少已部署且启用的审批流程：test-flow，整批未导入";
        when(service.importPolicies(transfer)).thenThrow(new BusinessApprovalException(
                BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID, reason));
        var result = controller.importPolicies(transfer);
        assertEquals(400, result.getCode());
        assertEquals(reason, result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void unexpectedDatabaseFailureStillPropagates() {
        var service = mock(BusinessApprovalPolicyTransferService.class);
        var controller = new BusinessApprovalPolicyController();
        ReflectionTestUtils.setField(controller, "transferService", service);
        var transfer = new BusinessApprovalPolicyTransferVO();
        var failure = new IllegalStateException("database failure");
        when(service.importPolicies(transfer)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> controller.importPolicies(transfer)));
    }
}
