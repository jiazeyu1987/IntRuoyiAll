package cn.iocoder.yudao.module.bpm.approval.service.signature;

import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

@Service
public class ApprovalSignatureRecordServiceImpl implements ApprovalSignatureRecordService {

    static final String MODULE_CODE = "BPM";
    static final String SUBJECT_TYPE = "BPM_APPROVAL_TASK";

    private final ElectronicSignatureService electronicSignatureService;
    private final ApprovalSignatureImageSnapshotProvider signatureImageSnapshotProvider;

    public ApprovalSignatureRecordServiceImpl(ElectronicSignatureService electronicSignatureService,
                                              ApprovalSignatureImageSnapshotProvider signatureImageSnapshotProvider) {
        this.electronicSignatureService = electronicSignatureService;
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
        String subjectId = BpmApprovalSignatureSubjectAdapter.encodeSubjectId(context, imageSnapshot);
        String subjectVersion = BpmApprovalSignatureSubjectAdapter.subjectVersion(subjectId);
        ElectronicSignatureResult signature = electronicSignatureService.sign(new ElectronicSignatureCommand(
                MODULE_CODE,
                context.getResult().name(),
                SUBJECT_TYPE,
                subjectId,
                subjectVersion,
                context.getSignaturePassword().trim(),
                trimToNull(context.getReason()) == null ? context.getResult().name() : context.getReason().trim(),
                idempotencyKey(context, imageSnapshot),
                null,
                null));
        signatureImageSnapshotProvider.markReferenced(imageSnapshot.getImageId());
        return ApprovalSignatureRecordResult.builder()
                .recordId(signature.signatureId())
                .unifiedSignatureId(signature.signatureId())
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

    private static String idempotencyKey(ApprovalTaskReviewContext context, ApprovalSignatureImageSnapshot imageSnapshot) {
        return "BPM|" + context.getModuleCode().name()
                + "|" + trimToNull(context.getSourceTaskType())
                + "|" + trimToNull(context.getSourceTaskId())
                + "|" + trimToNull(context.getBusinessKey())
                + "|" + trimToNull(context.getProcessInstanceId())
                + "|" + context.getLoginUserId()
                + "|" + context.getResult().name()
                + "|" + trimToNull(context.getReason())
                + "|" + imageSnapshot.getImageId()
                + "|" + imageSnapshot.getVersionNo()
                + "|" + sha256(signatureImagePayload(imageSnapshot));
    }

    static String signatureImagePayload(ApprovalSignatureImageSnapshot imageSnapshot) {
        return String.join("|",
                String.valueOf(imageSnapshot.getImageId()),
                String.valueOf(imageSnapshot.getVersionNo()),
                String.valueOf(imageSnapshot.getFileId()),
                requireText(imageSnapshot.getFileUrl(), "APPROVAL_SIGNATURE_IMAGE_URL_REQUIRED"),
                trimToNull(imageSnapshot.getSha256()),
                trimToNull(imageSnapshot.getContentType()),
                String.valueOf(imageSnapshot.getFileSize()),
                trimToNull(imageSnapshot.getImageStatus()),
                trimToNull(imageSnapshot.getVerifiedStatus()));
    }

    static String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

}
