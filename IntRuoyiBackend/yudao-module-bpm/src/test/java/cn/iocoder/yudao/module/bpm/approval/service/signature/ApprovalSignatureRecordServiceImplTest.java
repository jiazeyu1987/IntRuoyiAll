package cn.iocoder.yudao.module.bpm.approval.service.signature;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.dal.dataobject.signature.BpmApprovalSignatureRecordDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.signature.BpmApprovalSignatureRecordMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalSignatureRecordServiceImplTest {

    @Mock
    private BpmApprovalSignatureRecordMapper signatureRecordMapper;
    @Mock
    private ApprovalSignatureImageSnapshotProvider signatureImageSnapshotProvider;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void recordReviewSignaturePersistsNativeBpmReviewSignature() {
        TenantContextHolder.setTenantId(122L);
        ApprovalSignatureRecordServiceImpl service = newService();
        when(signatureImageSnapshotProvider.requireActiveSnapshot(100L)).thenReturn(signatureImageSnapshot());

        service.recordReviewSignature(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.BPM,
                "BPM_TASK_TODO", "task-100", "pi-100", "pi-100",
                ApprovalTaskReviewResult.APPROVE, null, "secret", false));

        BpmApprovalSignatureRecordDO record = captureInsertedRecord();
        assertEquals(122L, record.getTenantId());
        assertEquals("BPM", record.getModuleCode());
        assertEquals("BPM_TASK_TODO", record.getSourceTaskType());
        assertEquals("task-100", record.getSourceTaskId());
        assertEquals("pi-100", record.getBusinessKey());
        assertEquals("pi-100", record.getProcessInstanceId());
        assertEquals(100L, record.getSignerUserId());
        assertEquals("APPROVE", record.getReviewResult());
        assertTrue(record.getPasswordVerified());
        assertNotNull(record.getSignedAt());
        assertSignatureImageSnapshot(record);
        verify(signatureImageSnapshotProvider).markReferenced(9101L);
    }

    @Test
    void recordReviewSignaturePersistsMesFeedbackReviewSignature() {
        TenantContextHolder.setTenantId(122L);
        ApprovalSignatureRecordServiceImpl service = newService();
        when(signatureImageSnapshotProvider.requireActiveSnapshot(101L)).thenReturn(signatureImageSnapshot());

        service.recordReviewSignature(ApprovalTaskReviewContext.of(101L, ApprovalModuleCode.MES_FEEDBACK,
                "MES_PRO_FEEDBACK", "9001", "9001", null,
                ApprovalTaskReviewResult.REJECT, "quality data missing", "secret", false));

        BpmApprovalSignatureRecordDO record = captureInsertedRecord();
        assertEquals(122L, record.getTenantId());
        assertEquals("MES_FEEDBACK", record.getModuleCode());
        assertEquals("MES_PRO_FEEDBACK", record.getSourceTaskType());
        assertEquals("9001", record.getSourceTaskId());
        assertEquals("9001", record.getBusinessKey());
        assertEquals(101L, record.getSignerUserId());
        assertEquals("REJECT", record.getReviewResult());
        assertEquals("quality data missing", record.getReason());
        assertTrue(record.getPasswordVerified());
        assertNotNull(record.getSignedAt());
        assertSignatureImageSnapshot(record);
        verify(signatureImageSnapshotProvider).markReferenced(9101L);
    }

    private ApprovalSignatureRecordServiceImpl newService() {
        return new ApprovalSignatureRecordServiceImpl(signatureRecordMapper, signatureImageSnapshotProvider);
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

    private static void assertSignatureImageSnapshot(BpmApprovalSignatureRecordDO record) {
        assertEquals(9101L, record.getSignatureImageId());
        assertEquals(2, record.getSignatureImageVersionNo());
        assertEquals(8101L, record.getSignatureImageFileId());
        assertEquals("/admin-api/infra/file/28/get/dcc/signature-images/signature.png",
                record.getSignatureImageFileUrl());
        assertEquals("87b335f7e9429e37ff0df4c0c966681a86932139eade14bf1957d1fda2a19430",
                record.getSignatureImageSha256());
        assertEquals("image/png", record.getSignatureImageContentType());
        assertEquals(2048L, record.getSignatureImageFileSize());
        assertEquals("ACTIVE", record.getSignatureImageStatusSnapshot());
        assertEquals("VALID", record.getSignatureImageVerifiedStatus());
    }

    private BpmApprovalSignatureRecordDO captureInsertedRecord() {
        ArgumentCaptor<BpmApprovalSignatureRecordDO> captor =
                ArgumentCaptor.forClass(BpmApprovalSignatureRecordDO.class);
        verify(signatureRecordMapper).insert(captor.capture());
        return captor.getValue();
    }
}
