package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrDeviationHandlingRespVO {

    private Long id;
    private Long deviationId;
    private LocalDateTime investigationStartedAt;
    private LocalDateTime plannedCompletedAt;
    private LocalDateTime completedAt;
    private String investigationMembersJson;
    private String rootCauseAnalysis;
    private String impactScope;
    private String riskAssessment;
    private String productDisposition;
    private String nonconformanceReviewCode;
    private Long correctiveOwnerId;
    private String correctiveOwnerName;
    private LocalDateTime correctiveDueAt;
    private Boolean capaRequired;
    private String capaCode;
    private String capaAttachmentsJson;
    private String handlingConclusion;
    private String verificationResult;
    private String verificationContent;
    private Integer contentVersion;
    private String contentHash;
    private Map<String, Long> effectiveSignatureIds;
    private List<SignatureEvidence> signatureEvidence;
    private List<SignatureEvidence> signatureHistory;
    private List<RevisionHistory> revisionHistory;

    @Data
    @Accessors(chain = true)
    public static class SignatureEvidence {
        private Long id;
        private String node;
        private Long actorId;
        private String actorDisplayName;
        private String meaningLabel;
        private LocalDateTime signedAt;
        private String timeZone;
        private String timeEvidenceId;
        private String verificationStatus;
        private String contentHash;
        private String evidenceHash;
        private String policyVersion;
        private String subjectVersion;
        private String validityStatus;
    }

    @Data
    @Accessors(chain = true)
    public static class RevisionHistory {
        private Long auditId;
        private Long handlingId;
        private String revisionReason;
        private String beforeContent;
        private String afterContent;
        private Long actorUserId;
        private String actorUsername;
        private LocalDateTime occurredAt;
        private String beforeSummaryHash;
        private String afterSummaryHash;
        private String previousAuditHash;
        private String auditHash;
        private Integer beforeContentVersion;
        private Integer afterContentVersion;
        private String beforeContentHash;
        private String afterContentHash;
    }
}
