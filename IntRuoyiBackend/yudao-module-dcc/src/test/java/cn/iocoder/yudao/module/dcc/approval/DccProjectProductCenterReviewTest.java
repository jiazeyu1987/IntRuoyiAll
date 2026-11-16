package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccProjectProductCenterReviewTest {
    private final DccProjectProductActorSupport actors = mock(DccProjectProductActorSupport.class);
    private final DccProjectProductCreateService service = mock(DccProjectProductCreateService.class);
    private final DccProjectReviewerRoleService roles = mock(DccProjectReviewerRoleService.class);
    private final DccProjectProductTaskDelegate delegate = new DccProjectProductTaskDelegate(
            mock(DccProjectProductCreateRequestMapper.class), actors, service);
    private DccProjectProductCreateRequestDO row;

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(delegate, "reviewerRoles", roles);
        row = new DccProjectProductCreateRequestDO();
        row.setId(9007199254740993L); row.setTenantId(1L); row.setStatus("PENDING_REVIEW");
        row.setConfiguredReviewerUserId(2L);
        when(service.getRequest(2L, row.getId())).thenReturn(row);
        when(actors.canQuery(2L)).thenReturn(true);
        when(actors.canUpdate(2L)).thenReturn(true);
    }

    private ApprovalTaskReviewContext context(String type, ApprovalTaskReviewResult result) {
        return ApprovalTaskReviewContext.of(2L, ApprovalModuleCode.DCC, type,
                "DCC_PROJECT_PRODUCT:1:" + row.getId() + ":" + type, row.getId().toString(),
                null, result, "真实审批意见", "signature-password", false);
    }

    @Test void reviewAndRejectUseExactRequestAndExistingBusinessService() {
        for (var result : ApprovalTaskReviewResult.values()) {
            if (result != ApprovalTaskReviewResult.APPROVE && result != ApprovalTaskReviewResult.REJECT) continue;
            delegate.review(context(DccProjectProductTaskDelegate.REVIEW, result));
            verify(service).review(2L, row.getId(), "真实审批意见", result == ApprovalTaskReviewResult.APPROVE);
        }
    }

    @Test void roleContractUsesCurrentQualifiedMemberAndHasNoSecondApproval() {
        row.setReviewContract("ROLE_SINGLE_REVIEW_V1");
        when(roles.isRoleContract(row)).thenReturn(true);
        when(roles.canReview(2L, row)).thenReturn(true);
        delegate.review(context(DccProjectProductTaskDelegate.REVIEW, ApprovalTaskReviewResult.APPROVE));
        verify(service).review(2L, row.getId(), "真实审批意见", true);
        row.setStatus("PENDING_APPROVAL");
        assertThrows(IllegalArgumentException.class, () -> delegate.review(context(DccProjectProductTaskDelegate.APPROVAL, ApprovalTaskReviewResult.APPROVE)));
        verify(service, never()).approve(any(), any(), any(), anyBoolean());
    }

    @Test void legacyApprovalRequiresActualApproverEvenInGlobalView() {
        row.setStatus("PENDING_APPROVAL");
        var command = context(DccProjectProductTaskDelegate.APPROVAL, ApprovalTaskReviewResult.REJECT);
        command.setGlobalView(true);
        assertThrows(IllegalArgumentException.class, () -> delegate.review(command));
        when(actors.isApprover(2L)).thenReturn(true);
        delegate.review(command);
        verify(service).approve(2L, row.getId(), "真实审批意见", false);
    }

    @Test void wrongTenantTaskWrongNodeStaleStateAndRevokedPermissionNeverWrite() {
        var command = context(DccProjectProductTaskDelegate.REVIEW, ApprovalTaskReviewResult.APPROVE);
        command.setSourceTaskId(command.getSourceTaskId().replace(":1:", ":9:"));
        assertThrows(IllegalArgumentException.class, () -> delegate.review(command));
        var wrongNode = context(DccProjectProductTaskDelegate.APPROVAL, ApprovalTaskReviewResult.APPROVE);
        assertThrows(IllegalArgumentException.class, () -> delegate.review(wrongNode));
        row.setStatus("COMPLETED");
        assertThrows(IllegalArgumentException.class, () -> delegate.review(context(DccProjectProductTaskDelegate.REVIEW, ApprovalTaskReviewResult.APPROVE)));
        row.setStatus("PENDING_REVIEW"); when(actors.canQuery(2L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> delegate.review(context(DccProjectProductTaskDelegate.REVIEW, ApprovalTaskReviewResult.APPROVE)));
        verify(service, never()).review(any(), any(), any(), anyBoolean());
        verify(service, never()).approve(any(), any(), any(), anyBoolean());
    }
}
