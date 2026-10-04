package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.signature.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.Mockito.*;

/** Only directory, authentication and image storage are doubles; writer, adapters, hashes and SQL are real. */
public final class MesSignaturePersistenceFixture {
    public final ElectronicSignatureServiceImpl writer = new ElectronicSignatureServiceImpl();
    public final ElectronicSignatureQueryServiceImpl query = new ElectronicSignatureQueryServiceImpl();

    public MesSignaturePersistenceFixture(ElectronicSignatureRecordMapper records, AdminUserApi users,
                                          GxpAuditService audit) {
        var adapters = List.of(new MesBatchRecordSignatureSubjectAdapter(), new BpmApprovalSignatureSubjectAdapter());
        for (Object target : List.of(writer, query)) {
            ReflectionTestUtils.setField(target, "signatureRecordMapper", records);
            ReflectionTestUtils.setField(target, "adminUserApi", users);
            ReflectionTestUtils.setField(target, "subjectAdapters", adapters);
        }
        ReflectionTestUtils.setField(writer, "gxpAuditService", audit);
    }

    public ApprovalSignatureRecordResult bpm(long actor, long task, String opinion) {
        return bpm(actor, task, opinion, ApprovalModuleCode.EDHR, "EDHR_WORK_TASK");
    }

    public ApprovalSignatureRecordResult bpm(long actor, long task, String opinion,
                                            ApprovalModuleCode module, String sourceType) {
        var images = mock(ApprovalSignatureImageSnapshotProvider.class);
        when(images.requireActiveSnapshot(actor)).thenReturn(ApprovalSignatureImageSnapshot.builder()
                .imageId(91L).versionNo(1).fileId(92L).fileUrl("https://test.invalid/signature.png")
                .sha256("a".repeat(64)).contentType("image/png").fileSize(100L)
                .imageStatus("ACTIVE").verifiedStatus("VERIFIED").build());
        return new ApprovalSignatureRecordServiceImpl(writer, images).recordReviewSignature(
                ApprovalTaskReviewContext.of(actor, module, sourceType,
                        String.valueOf(task), String.valueOf(task), null, ApprovalTaskReviewResult.APPROVE,
                        opinion, "test-only", false));
    }
}
