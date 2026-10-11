package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.module.bpm.approval.service.ApprovalIdentity;
import java.util.List;
import java.util.Objects;

public record PreparedApprovalContent(ApprovalIdentity identity, String contentPolicyVersion,
        String subjectVersion, String contentHash, String scopeCode, String scopeText,
        String canonicalBusinessContentJson, List<PreviewAttachment> attachments, List<String> allowedActions) {
    public PreparedApprovalContent {
        Objects.requireNonNull(identity); Objects.requireNonNull(canonicalBusinessContentJson);
        if (!Objects.equals(contentHash, ApprovalContentJson.hash(canonicalBusinessContentJson)))
            throw new IllegalArgumentException("APPROVAL_CONTENT_HASH_MISMATCH");
        attachments = List.copyOf(Objects.requireNonNull(attachments));
        allowedActions = List.copyOf(Objects.requireNonNull(allowedActions));
    }
}
