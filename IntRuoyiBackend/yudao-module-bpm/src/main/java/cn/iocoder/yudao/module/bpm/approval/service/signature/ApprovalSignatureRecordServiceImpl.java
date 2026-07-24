package cn.iocoder.yudao.module.bpm.approval.service.signature;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.dal.dataobject.signature.BpmApprovalSignatureRecordDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.signature.BpmApprovalSignatureRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class ApprovalSignatureRecordServiceImpl implements ApprovalSignatureRecordService {

    private final BpmApprovalSignatureRecordMapper signatureRecordMapper;
    private final ApprovalSignatureImageSnapshotProvider signatureImageSnapshotProvider;

    public ApprovalSignatureRecordServiceImpl(BpmApprovalSignatureRecordMapper signatureRecordMapper,
                                              ApprovalSignatureImageSnapshotProvider signatureImageSnapshotProvider) {
        this.signatureRecordMapper = signatureRecordMapper;
        this.signatureImageSnapshotProvider = signatureImageSnapshotProvider;
    }

    @Override
    public ApprovalSignatureRecordResult recordReviewSignature(ApprovalTaskReviewContext context) {
        Objects.requireNonNull(context, "APPROVAL_REVIEW_CONTEXT_REQUIRED");
        Objects.requireNonNull(context.getModuleCode(), "APPROVAL_MODULE_REQUIRED");
        Objects.requireNonNull(context.getResult(), "APPROVAL_REVIEW_RESULT_REQUIRED");
        if (context.getSourceTaskType() == null || context.getSourceTaskType().isBlank()) {
            throw new NullPointerException("APPROVAL_SOURCE_TASK_TYPE_REQUIRED");
        }
        if (context.getLoginUserId() == null) {
            throw new NullPointerException("APPROVAL_LOGIN_USER_REQUIRED");
        }
        if (context.getSignaturePassword() == null || context.getSignaturePassword().isBlank()) {
            throw new IllegalArgumentException("APPROVAL_SIGNATURE_PASSWORD_REQUIRED");
        }
        ApprovalSignatureImageSnapshot imageSnapshot =
                signatureImageSnapshotProvider.requireActiveSnapshot(context.getLoginUserId());
        String signatureImageFileUrl = requireText(imageSnapshot.getFileUrl(), "APPROVAL_SIGNATURE_IMAGE_URL_REQUIRED");
        BpmApprovalSignatureRecordDO record = BpmApprovalSignatureRecordDO.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .moduleCode(context.getModuleCode().name())
                .sourceTaskType(context.getSourceTaskType().trim())
                .sourceTaskId(trimToNull(context.getSourceTaskId()))
                .businessKey(trimToNull(context.getBusinessKey()))
                .processInstanceId(trimToNull(context.getProcessInstanceId()))
                .signerUserId(context.getLoginUserId())
                .reviewResult(context.getResult().name())
                .reason(trimToNull(context.getReason()))
                .passwordVerified(Boolean.TRUE)
                .signedAt(LocalDateTime.now())
                .signatureImageId(imageSnapshot.getImageId())
                .signatureImageVersionNo(imageSnapshot.getVersionNo())
                .signatureImageFileId(imageSnapshot.getFileId())
                .signatureImageFileUrl(signatureImageFileUrl)
                .signatureImageSha256(imageSnapshot.getSha256())
                .signatureImageContentType(imageSnapshot.getContentType())
                .signatureImageFileSize(imageSnapshot.getFileSize())
                .signatureImageStatusSnapshot(imageSnapshot.getImageStatus())
                .signatureImageVerifiedStatus(imageSnapshot.getVerifiedStatus())
                .build();
        signatureRecordMapper.insert(record);
        signatureImageSnapshotProvider.markReferenced(imageSnapshot.getImageId());
        return ApprovalSignatureRecordResult.builder()
                .recordId(record.getId())
                .signatureImageId(imageSnapshot.getImageId())
                .signatureImageFileUrl(signatureImageFileUrl)
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

}
