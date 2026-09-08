package cn.iocoder.yudao.module.bpm.approval.service.signature;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalSignatureRecordServiceImplTest {

    @Mock
    private ElectronicSignatureService electronicSignatureService;
    @Mock
    private ApprovalSignatureImageSnapshotProvider signatureImageSnapshotProvider;

    @Test
    void recordReviewSignatureDelegatesToUnifiedKernelForNativeBpmReviewSignature() {
        ApprovalSignatureRecordServiceImpl service = newService();
        when(signatureImageSnapshotProvider.requireActiveSnapshot(100L)).thenReturn(signatureImageSnapshot());
        when(electronicSignatureService.sign(org.mockito.ArgumentMatchers.any())).thenReturn(signatureResult(810001L));

        ApprovalSignatureRecordResult result = service.recordReviewSignature(ApprovalTaskReviewContext.of(100L,
                ApprovalModuleCode.BPM, "BPM_TASK_TODO", "task-100", "pi-100", "pi-100",
                ApprovalTaskReviewResult.APPROVE, null, "secret", false));

        ElectronicSignatureCommand command = captureSignatureCommand();
        assertEquals("BPM", command.moduleCode());
        assertEquals("APPROVE", command.actionCode());
        assertEquals("BPM_APPROVAL_TASK", command.subjectType());
        assertEquals(command.expectedSubjectVersion(),
                BpmApprovalSignatureSubjectAdapter.subjectVersion(command.subjectId()));
        assertEquals("secret", command.credential());
        assertEquals("APPROVE", command.reason());
        assertTrue(command.idempotencyKey().startsWith("BPM|BPM|BPM_TASK_TODO|task-100|pi-100|pi-100|100|APPROVE|"));
        assertEquals(810001L, result.getRecordId());
        assertEquals(810001L, result.getUnifiedSignatureId());
        assertEquals(9101L, result.getSignatureImageId());
        assertEquals("/admin-api/infra/file/28/get/dcc/signature-images/signature.png",
                result.getSignatureImageFileUrl());
        verify(signatureImageSnapshotProvider).markReferenced(9101L);
    }

    @Test
    void recordReviewSignatureDelegatesToUnifiedKernelForMesFeedbackReviewSignature() {
        ApprovalSignatureRecordServiceImpl service = newService();
        when(signatureImageSnapshotProvider.requireActiveSnapshot(101L)).thenReturn(signatureImageSnapshot());
        when(electronicSignatureService.sign(org.mockito.ArgumentMatchers.any())).thenReturn(signatureResult(810002L));

        ApprovalSignatureRecordResult result = service.recordReviewSignature(ApprovalTaskReviewContext.of(101L,
                ApprovalModuleCode.MES_FEEDBACK, "MES_PRO_FEEDBACK", "9001", "9001", "pi-9001",
                ApprovalTaskReviewResult.REJECT, "quality data missing", "secret", false));

        ElectronicSignatureCommand command = captureSignatureCommand();
        assertEquals("BPM", command.moduleCode());
        assertEquals("REJECT", command.actionCode());
        assertEquals("BPM_APPROVAL_TASK", command.subjectType());
        assertEquals("quality data missing", command.reason());
        assertTrue(command.idempotencyKey().startsWith("BPM|MES_FEEDBACK|MES_PRO_FEEDBACK|9001|9001|pi-9001|101|REJECT|"));
        assertEquals(810002L, result.getUnifiedSignatureId());
        verify(signatureImageSnapshotProvider).markReferenced(9101L);
    }

    @Test
    void recordReviewSignatureRejectsMissingProcessInstanceForOrderedApprovalEvidence() {
        ApprovalSignatureRecordServiceImpl service = newService();

        assertThrows(NullPointerException.class, () -> service.recordReviewSignature(ApprovalTaskReviewContext.of(101L,
                ApprovalModuleCode.BPM, "BPM_TASK_TODO", "task-100", "pi-100", null,
                ApprovalTaskReviewResult.APPROVE, "approved", "secret", false)));

        verify(signatureImageSnapshotProvider, never()).requireActiveSnapshot(101L);
        verify(electronicSignatureService, never()).sign(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordReviewSignatureAllowsSignatureRequiredDirectBusinessApprovalWithoutProcessInstance() {
        ApprovalSignatureRecordServiceImpl service = newService();
        when(signatureImageSnapshotProvider.requireActiveSnapshot(102L)).thenReturn(signatureImageSnapshot());
        when(electronicSignatureService.sign(org.mockito.ArgumentMatchers.any())).thenReturn(signatureResult(810003L));

        ApprovalSignatureRecordResult result = service.recordReviewSignature(ApprovalTaskReviewContext.of(102L,
                ApprovalModuleCode.BPM, "BUSINESS_APPROVAL_POLICY_SWITCH", "policy-100", "MES:POLICY",
                null, ApprovalTaskReviewResult.APPROVE, "SIGNATURE_REQUIRED", "secret", false));

        ElectronicSignatureCommand command = captureSignatureCommand();
        assertEquals(810003L, result.getUnifiedSignatureId());
        assertTrue(command.idempotencyKey().startsWith(
                "BPM|BPM|BUSINESS_APPROVAL_POLICY_SWITCH|policy-100|MES:POLICY|null|102|APPROVE|"));
        verify(signatureImageSnapshotProvider).markReferenced(9101L);
    }

    @Test
    void adapterBuildsDeterministicContentSnapshotFromServerSideReviewContext() {
        ApprovalTaskReviewContext context = ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.BPM,
                "BPM_TASK_TODO", "task-100", "pi-100", "pi-100",
                ApprovalTaskReviewResult.APPROVE, "approved", "secret", false);
        String subjectId = BpmApprovalSignatureSubjectAdapter.encodeSubjectId(context, signatureImageSnapshot());
        BpmApprovalSignatureSubjectAdapter adapter = new BpmApprovalSignatureSubjectAdapter();

        SignatureSubjectSnapshot snapshot = adapter.loadAndAuthorize(new SignatureSubjectCommand(100L,
                "BPM", "APPROVE", "BPM_APPROVAL_TASK", subjectId,
                BpmApprovalSignatureSubjectAdapter.subjectVersion(subjectId), "approved"));

        assertEquals("BPM_APPROVAL_TASK", snapshot.subjectType());
        assertEquals(subjectId, snapshot.subjectId());
        assertEquals(BpmApprovalSignatureSubjectAdapter.subjectVersion(subjectId), snapshot.subjectVersion());
        assertTrue(snapshot.canonicalContentJson().contains("\"moduleCode\":\"BPM\""));
        assertTrue(snapshot.canonicalContentJson().contains("\"sourceTaskType\":\"BPM_TASK_TODO\""));
        assertTrue(snapshot.canonicalContentJson().contains("\"sourceTaskId\":\"task-100\""));
        assertTrue(snapshot.canonicalContentJson().contains("\"reviewResult\":\"APPROVE\""));
        assertTrue(snapshot.canonicalContentJson().contains("\"signatureImagePayloadHash\":\""));
        assertEquals("pi-100", snapshot.processInstanceId());
        assertEquals("task-100", snapshot.taskId());
        assertEquals("BPM_TASK_TODO", snapshot.nodeCode());
        assertFalse(snapshot.canonicalContentJson().contains("secret"));
    }

    private ApprovalSignatureRecordServiceImpl newService() {
        return new ApprovalSignatureRecordServiceImpl(electronicSignatureService, signatureImageSnapshotProvider);
    }

    private ElectronicSignatureCommand captureSignatureCommand() {
        ArgumentCaptor<ElectronicSignatureCommand> captor =
                ArgumentCaptor.forClass(ElectronicSignatureCommand.class);
        verify(electronicSignatureService).sign(captor.capture());
        return captor.getValue();
    }

    private static ElectronicSignatureResult signatureResult(Long id) {
        return new ElectronicSignatureResult(id, "VALID", LocalDateTime.now(), "SERVER_CLOCK:now",
                "version", "contentHash", "evidenceHash", "SHA-256", "system-local-v1");
    }

    private static ApprovalSignatureImageSnapshot signatureImageSnapshot() {
        return ApprovalSignatureImageSnapshot.builder()
                .imageId(9101L)
                .versionNo(2)
                .fileId(8101L)
                .fileUrl("/admin-api/infra/file/28/get/dcc/signature-images/signature.png")
                .sha256("87b335f7e9429e37ff0df4c0c966681a86932139eade14bf1957d1fda2a19430")
                .contentType("image/png")
                .fileSize(2048L)
                .imageStatus("ACTIVE")
                .verifiedStatus("VALID")
                .build();
    }

}
