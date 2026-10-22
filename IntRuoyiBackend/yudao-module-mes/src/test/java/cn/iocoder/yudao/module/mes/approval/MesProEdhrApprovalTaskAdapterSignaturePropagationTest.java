package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalCenterServiceImpl;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewCommand;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProviderRegistry;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureImageSnapshot;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureImageSnapshotProvider;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordServiceImpl;
import cn.iocoder.yudao.module.bpm.approval.service.signature.BpmApprovalSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MesProEdhrApprovalTaskAdapterSignaturePropagationTest {
    @org.mockito.Mock private cn.iocoder.yudao.module.mes.approval.MesActiveOrderHandoffApprovalProjection handoffProjection;


    @Mock
    private MesProEdhrWorkTaskService workTaskService;
    @Mock
    private MesProEdhrReleaseService releaseService;
    @InjectMocks
    private MesProEdhrApprovalTaskAdapter adapter;

    @Test
    void reviewForwardsManagerSignaturePasswordToReleaseService() {
        Long workTaskId = 177L;
        Long releaseTransactionId = 9200L;
        String signaturePassword = "review-password-test-only";
        when(workTaskService.getReleaseApprovalTaskForReview(workTaskId, null))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(workTaskId)
                        .setBusinessScopeType("RELEASE_TRANSACTION")
                        .setBusinessScopeId(releaseTransactionId));
        when(releaseService.get(releaseTransactionId)).thenReturn(new MesProEdhrReleaseRespVO().setVersion(3));

        ApprovalTaskReviewContext context = ApprovalTaskReviewContext.of(188L, ApprovalModuleCode.EDHR,
                "EDHR_WORK_TASK", String.valueOf(workTaskId), String.valueOf(workTaskId), null,
                ApprovalTaskReviewResult.APPROVE, "符合放行要求", signaturePassword, false)
                .setSignatureSubjectId("signed-subject")
                .setSignatureEvidenceHash("a".repeat(64));
        adapter.review(context);

        ArgumentCaptor<MesProEdhrReleaseApproveReqVO> captor =
                ArgumentCaptor.forClass(MesProEdhrReleaseApproveReqVO.class);
        verify(releaseService).approve(captor.capture());
        assertEquals(signaturePassword, captor.getValue().getPassword());
        assertEquals("符合放行要求", captor.getValue().getApprovalOpinion());
    }

    @Test
    void centerSignatureBodyAndRealEdhrProviderKeepTheSameUserMultilineOpinion() {
        String rawOpinion = "\u00a0\uFEFF  evidence checked\n  user \"opinion\" \\ kept\tsecond line\r\nlast line  \u202f";
        String opinion = "evidence checked\n  user \"opinion\" \\ kept\tsecond line\r\nlast line";
        when(workTaskService.getReleaseApprovalTaskForReview(177L, null)).thenReturn(new MesProEdhrWorkTaskDO()
                .setId(177L).setBusinessScopeType("RELEASE_TRANSACTION").setBusinessScopeId(9200L));
        when(releaseService.get(9200L)).thenReturn(new MesProEdhrReleaseRespVO().setVersion(3));
        PermissionApi permissions = mock(PermissionApi.class);
        var images = mock(ApprovalSignatureImageSnapshotProvider.class);
        when(images.requireActiveSnapshot(188L)).thenReturn(ApprovalSignatureImageSnapshot.builder()
                .imageId(91L).versionNo(1).fileId(92L).fileUrl("/test/signature.png")
                .sha256("a".repeat(64)).contentType("image/png").fileSize(100L)
                .imageStatus("ACTIVE").verifiedStatus("VALID").build());
        var kernel = mock(ElectronicSignatureService.class);
        when(kernel.sign(any())).thenReturn(new ElectronicSignatureResult(81L, "VALID", LocalDateTime.now(),
                "clock", "version", "contentHash", "a".repeat(64), "SHA-256", "system-local-v1"));
        // Use the production adapter and center; only auth/image/kernel/terminal release ports are doubles.
        var center = new ApprovalCenterServiceImpl(new ApprovalTaskProviderRegistry(List.of(adapter)), permissions,
                mock(AdminUserApi.class), new ApprovalSignatureRecordServiceImpl(kernel, images));

        center.reviewTask(188L, new ApprovalTaskReviewCommand().setModuleCode(ApprovalModuleCode.EDHR)
                .setSourceTaskType("EDHR_WORK_TASK").setSourceTaskId("177").setBusinessKey("177")
                .setResult(ApprovalTaskReviewResult.APPROVE).setReason(rawOpinion).setSignaturePassword("test-only"));

        var signed = ArgumentCaptor.forClass(ElectronicSignatureCommand.class);
        var released = ArgumentCaptor.forClass(MesProEdhrReleaseApproveReqVO.class);
        var order = inOrder(releaseService, images, kernel);
        order.verify(releaseService).preflightApprovalCenterReview(188L, 177L);
        order.verify(images).requireActiveSnapshot(188L);
        order.verify(kernel).sign(signed.capture());
        order.verify(images).markReferenced(91L);
        order.verify(releaseService).approve(released.capture());
        var command = signed.getValue();
        var snapshot = new BpmApprovalSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(188L,
                command.moduleCode(), command.actionCode(), command.subjectType(), command.subjectId(),
                command.expectedSubjectVersion(), command.reason()));
        Map<?, ?> body = JsonUtils.parseObject(snapshot.canonicalContentJson(), Map.class);
        assertEquals(opinion, command.reason());
        assertEquals(opinion, body.get("reason"));
        assertEquals(opinion, released.getValue().getApprovalOpinion());
        assertEquals(command.subjectId(), released.getValue().getSignoffSubjectId());
        assertEquals("a".repeat(64), released.getValue().getSignoffEvidenceHash());
    }
}
