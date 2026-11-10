package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewCommand;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewPrepareDO;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureAuthenticationFacade;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApprovalCenterReviewFacadeTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\r\n", "\u2003", "\u00a0\u202f\u2007", "\uFEFF", "\000"})
    void blankRejectionIsRejectedBeforeAuthenticationOrAnyWrite(String reason) {
        var prepares = mock(ApprovalReviewPrepareService.class);
        var receipts = mock(ApprovalCenterReceiptService.class);
        var authentication = mock(ElectronicSignatureAuthenticationFacade.class);
        var executor = mock(ApprovalCenterReviewExecutor.class);
        var facade = new ApprovalCenterReviewFacade(prepares, receipts, authentication, executor);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> facade.review(4L, command(ApprovalTaskReviewResult.REJECT, reason)));

        assertEquals("APPROVAL_REVIEW_REASON_REQUIRED", ex.getMessage());
        verifyNoInteractions(prepares, receipts, authentication, executor);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\r\n", "\u2003", "\u00a0\u202f\u2007", "\uFEFF", "\000"})
    void blankApprovalStillAuthenticatesAndDispatchesNormalizedOpinion(String reason) {
        var prepares = mock(ApprovalReviewPrepareService.class);
        var receipts = mock(ApprovalCenterReceiptService.class);
        var authentication = mock(ElectronicSignatureAuthenticationFacade.class);
        var executor = mock(ApprovalCenterReviewExecutor.class);
        var facade = new ApprovalCenterReviewFacade(prepares, receipts, authentication, executor);
        var command = command(ApprovalTaskReviewResult.APPROVE, reason);
        var prepared = new BpmApprovalReviewPrepareDO();
        prepared.setTenantId(1L); prepared.setOwnerCode("EDHR"); prepared.setSourceTaskType("EDHR_WORK_TASK");
        prepared.setSourceTaskId("2"); prepared.setSubjectType("EDHR_RELEASE"); prepared.setSubjectId("3");
        when(prepares.require(4L, command.getPrepareId(), true)).thenReturn(prepared);

        TenantContextHolder.setTenantId(1L);
        try {
            facade.review(4L, command);
        } finally {
            TenantContextHolder.clear();
        }

        verify(authentication).authenticate(argThat(request -> "secret".equals(request.credential())));
        verify(executor).execute(eq(4L), same(command), isNull(), anyString());
        assertEquals("", command.getReason());
    }

    private static ApprovalTaskReviewCommand command(ApprovalTaskReviewResult result, String reason) {
        return new ApprovalTaskReviewCommand().setModuleCode(ApprovalModuleCode.EDHR)
                .setSourceTaskType("EDHR_WORK_TASK").setSourceTaskId("2").setResult(result).setReason(reason)
                .setPrepareId(UUID.randomUUID().toString()).setIdempotencyKey(UUID.randomUUID().toString())
                .setSignaturePassword("secret");
    }
}
