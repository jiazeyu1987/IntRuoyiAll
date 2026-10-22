package cn.iocoder.yudao.module.bpm.approval.service.signature;

import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Arrays;

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
        String reason = context.getReason() == null ? null
                : context.getReason().replaceAll(
                        "^[\\p{javaWhitespace}\\p{javaSpaceChar}\\x00-\\x20\\uFEFF]+|[\\p{javaWhitespace}\\p{javaSpaceChar}\\x00-\\x20\\uFEFF]+$", "");
        if (reason == null || reason.isEmpty()) {
            throw new IllegalArgumentException("APPROVAL_REVIEW_REASON_REQUIRED");
        }
        if (context.getSourceTaskType() == null || context.getSourceTaskType().isBlank()) {
            throw new NullPointerException("APPROVAL_SOURCE_TASK_TYPE_REQUIRED");
        }
        if (context.getSourceTaskId() == null || context.getSourceTaskId().isBlank()) {
            throw new NullPointerException("APPROVAL_SOURCE_TASK_ID_REQUIRED");
        }
        if (isOrderedBpmApproval(context)
                && (context.getProcessInstanceId() == null || context.getProcessInstanceId().isBlank())) {
            throw new NullPointerException("APPROVAL_PROCESS_INSTANCE_REQUIRED");
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
                reason,
                idempotencyKey(context, imageSnapshot, reason),
                null,
                null));
        signatureImageSnapshotProvider.markReferenced(imageSnapshot.getImageId());
        return ApprovalSignatureRecordResult.builder()
                .recordId(signature.signatureId())
                .unifiedSignatureId(signature.signatureId())
                .signatureImageId(imageSnapshot.getImageId())
                .signatureImageFileUrl(signatureImageFileUrl)
                .subjectId(subjectId)
                .evidenceHash(signature.evidenceHash())
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean isOrderedBpmApproval(ApprovalTaskReviewContext context) {
        return MODULE_CODE.equals(context.getModuleCode().name())
                && "BPM_TASK_TODO".equals(trimToNull(context.getSourceTaskType()));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String idempotencyKey(ApprovalTaskReviewContext context, ApprovalSignatureImageSnapshot imageSnapshot,
                                         String reason) {
        // JSON array boundaries distinguish null, literal "null" and delimiters in user input.
        // Store the full reason in the signature; only its idempotency identity is digested.
        String material = JsonUtils.toJsonString(Arrays.asList(
                context.getModuleCode().name(), trimToNull(context.getSourceTaskType()),
                trimToNull(context.getSourceTaskId()), trimToNull(context.getBusinessKey()),
                trimToNull(context.getProcessInstanceId()), context.getLoginUserId(),
                context.getResult().name(), reason, imageSnapshot.getImageId(), imageSnapshot.getVersionNo(),
                imageSnapshot.getFileId(), requireText(imageSnapshot.getFileUrl(), "APPROVAL_SIGNATURE_IMAGE_URL_REQUIRED"),
                trimToNull(imageSnapshot.getSha256()), trimToNull(imageSnapshot.getContentType()),
                imageSnapshot.getFileSize(), trimToNull(imageSnapshot.getImageStatus()),
                trimToNull(imageSnapshot.getVerifiedStatus())));
        return "BPM_APPROVAL_REVIEW_V1|" + sha256(material);
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
