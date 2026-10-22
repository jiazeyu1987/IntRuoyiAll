package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.provider.*;
import cn.iocoder.yudao.module.bpm.approval.service.signature.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovalCenterReviewPreflightTest {
    @Test
    void rejectedPreflightDoesNotInvokeSignatureOrDomain() {
        ApprovalTaskProvider provider = provider();
        ApprovalSignatureRecordService signatures = mock(ApprovalSignatureRecordService.class);
        doThrow(new IllegalStateException("stale task")).when((ApprovalCenterReviewPreflight) provider)
                .preflightReview(any());
        assertThrows(IllegalStateException.class, () -> center(provider, signatures).reviewTask(100L, command()));
        verifyNoInteractions(signatures);
        verify(provider, never()).review(any());
    }

    @Test
    void successfulPreflightRunsBeforeSignatureAndFormalReview() {
        ApprovalTaskProvider provider = provider();
        ApprovalSignatureRecordService signatures = mock(ApprovalSignatureRecordService.class);
        when(signatures.recordReviewSignature(any())).thenReturn(ApprovalSignatureRecordResult.builder()
                .signatureImageFileUrl("signature.png").subjectId("subject").evidenceHash("hash").build());
        center(provider, signatures).reviewTask(100L, command());
        var order = inOrder(provider, signatures);
        order.verify((ApprovalCenterReviewPreflight) provider).preflightReview(any());
        order.verify(signatures).recordReviewSignature(any());
        order.verify(provider).review(any());
    }

    @Test
    void missingEdhrPreflightFailsClosedBeforeSignature() {
        ApprovalTaskProvider provider = mock(ApprovalTaskProvider.class);
        when(provider.getModuleCode()).thenReturn(ApprovalModuleCode.EDHR);
        when(provider.getSupportedViewTypes()).thenReturn(Set.of(ApprovalTaskViewType.TODO));
        when(provider.isVisibleTo(100L)).thenReturn(true);
        ApprovalSignatureRecordService signatures = mock(ApprovalSignatureRecordService.class);
        assertThrows(IllegalStateException.class, () -> center(provider, signatures).reviewTask(100L, command()));
        verifyNoInteractions(signatures);
    }

    private ApprovalTaskProvider provider() {
        ApprovalTaskProvider provider = mock(ApprovalTaskProvider.class,
                withSettings().extraInterfaces(ApprovalCenterReviewPreflight.class));
        when(provider.getModuleCode()).thenReturn(ApprovalModuleCode.EDHR);
        when(provider.getSupportedViewTypes()).thenReturn(Set.of(ApprovalTaskViewType.TODO));
        when(provider.isVisibleTo(100L)).thenReturn(true);
        return provider;
    }
    private ApprovalCenterServiceImpl center(ApprovalTaskProvider provider, ApprovalSignatureRecordService signatures) {
        return new ApprovalCenterServiceImpl(new ApprovalTaskProviderRegistry(List.of(provider)),
                mock(PermissionApi.class), mock(AdminUserApi.class), signatures);
    }
    private ApprovalTaskReviewCommand command() {
        return new ApprovalTaskReviewCommand().setModuleCode(ApprovalModuleCode.EDHR)
                .setSourceTaskType("EDHR_WORK_TASK").setSourceTaskId("1").setBusinessKey("1")
                .setResult(ApprovalTaskReviewResult.APPROVE).setReason("reviewed").setSignaturePassword("test-only");
    }
}
