package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionReviewTimelineRespVO {

    private Long batchExecutionId;

    private List<BatchEvent> batchEvents;

    private List<TaskEvent> taskEvents;

    private List<SignatureRecord> signatureRecords;

    private List<ApprovalRecord> approvalRecords;

    private List<FlowEvent> flowEvents;

    private List<EdhrBatchExecutionArchiveRespVO> archiveVersions;

    private List<DossierItem> dossierItems;

    private List<ExecutionReview> executionReviews;

    @Data
    @Accessors(chain = true)
    public static class BatchEvent {

        private Long batchExecutionId;

        private String batchExecutionCode;

        private Integer status;

        private String aggregateHash;

        private Long closedBy;

        private LocalDateTime closedAt;

        private Long closeSignatureId;

        private Long rejectSignatureId;

        private Long rejectedBy;

        private LocalDateTime rejectedAt;

        private String rejectReason;

        private LocalDateTime createTime;
    }

    @Data
    @Accessors(chain = true)
    public static class TaskEvent {

        private Long taskId;

        private Integer routeProcessSort;

        private String processCode;

        private String processName;

        private String batchRecordReportId;

        private String batchRecordReportName;

        private Integer batchRecordSort;

        private String executionMode;

        private Boolean available;

        private String gateMessage;

        private Long executionId;

        private Integer status;

        private String blockerCode;

        private String blockerMessage;

        private Long skippedBy;

        private LocalDateTime skippedAt;

        private String specialPayloadJson;

        private LocalDateTime openedAt;

        private LocalDateTime submittedAt;

        private LocalDateTime approvedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class SignatureRecord {

        private Long id;

        private Long executionId;

        private String executionCode;

        private Long actorId;

        private String actorName;

        private String actorNicknameSnapshot;

        private String actorUsernameSnapshot;

        private String actionType;

        private String signatureMode;

        private Boolean passwordVerified;

        private String comment;

        private String aggregateHash;

        private LocalDateTime signedAt;

        private LocalDateTime selectedSignedAt;

        private LocalDateTime signatureDisplayAt;

        private String signatureTimeMode;

        private String selectedTimeZone;

        private String selectedTimeReason;

        private String selectedTimePolicyVersion;

        private String selectedTimeAuditHash;

        private String signaturePurpose;

        private String recordHashSnapshot;
    }

    @Data
    @Accessors(chain = true)
    public static class DossierItem {

        private Long id;

        private String itemType;

        private String itemKey;

        private String itemName;

        private Boolean requiredFlag;

        private String itemStatus;

        private String sourceDocType;

        private Long sourceDocId;

        private String sourceDocCode;

        private String sourceDocStatus;

        private String sourceDocResult;

        private String sourceDocHash;

        private LocalDateTime completedAt;

        private LocalDateTime verifiedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class ApprovalRecord {

        private Long executionId;

        private String executionCode;

        private String processCode;

        private String processName;

        private String actorName;

        private String comment;

        private String bpmTaskId;

        private String bpmTaskName;

        private String approvalResult;

        private LocalDateTime signedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class FlowEvent {

        private Long id;

        private Long interventionId;

        private String taskId;

        private String nodeKey;

        private String eventType;

        private String action;

        private String fromStatus;

        private String toStatus;

        private Long actorUserId;

        private Long targetUserId;

        private String permissionCode;

        private String permissionDecision;

        private String reason;

        private String signoffEvidenceHash;

        private String integrityCheckResult;

        private String eventSnapshotJson;

        private String evidenceHash;

        private LocalDateTime occurredAt;
    }

    @Data
    @Accessors(chain = true)
    public static class ExecutionReview {

        private Long taskId;

        private Integer routeProcessSort;

        private String processCode;

        private String processName;

        private String batchRecordReportId;

        private String batchRecordReportName;

        private Long executionId;

        private String executionCode;

        private Integer status;

        private LocalDateTime submittedAt;

        private LocalDateTime approvedAt;

        private FormViewModel formViewModel;

        private FieldAuditSummary fieldAuditSummary;

        private SignatureSummary signatureSummary;

        private List<SignatureRecord> signatureRecords;

        private ApprovalSummary approvalSummary;

        private DomainTraceSummary domainTraceSummary;

        private Integer attachmentCount;

        private List<AttachmentSummary> attachmentSummaries;
    }

    @Data
    @Accessors(chain = true)
    public static class AttachmentSummary {

        private Long id;

        private Long executionId;

        private Long batchTaskId;

        private Long workTaskId;

        private Integer rowIndex;

        private Integer columnIndex;

        private String fieldKey;

        private String fieldPath;

        private String fieldLabel;

        private String attachmentType;

        private String attachmentGroupKey;

        private String attachmentAction;

        private Integer versionNo;

        private Long fileId;

        private String fileUrl;

        private Long storageConfigId;

        private String storagePath;

        private String fileName;

        private String contentType;

        private Long fileSize;

        private String sha256;

        private String storageRetentionHash;

        private Long auditBatchId;

        private Long signatureId;

        private String attachmentHash;

        private Long operatorId;

        private String operatorName;

        private LocalDateTime operatedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class FormViewModel {

        private String sheetLayoutJson;

        private String metaJson;

        private String executionSnapshotJson;

        private String cellValuesJson;

        private String remark;

        private List<SignatureCellMarker> signatureCellMarkers;
    }

    @Data
    @Accessors(chain = true)
    public static class SignatureCellMarker {

        private Integer rowIndex;

        private Integer columnIndex;

        private Boolean enabled;

        private String actionType;

        private String label;

        private String displayFormat;
    }

    @Data
    @Accessors(chain = true)
    public static class FieldAuditSummary {

        private Integer batchCount;

        private Long revision;

        private Long lastBatchId;

        private String headHash;
    }

    @Data
    @Accessors(chain = true)
    public static class SignatureSummary {

        private Integer totalCount;

        private Integer fieldChangeCount;

        private Integer formReviewCount;

        private Integer submitCount;

        private Integer approveCount;

        private LocalDateTime lastSignedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class ApprovalSummary {

        private String processInstanceId;

        private String approvalSnapshotStatus;

        private String currentBpmTaskId;

        private ApprovalRecord approvedRecord;
    }

    @Data
    @Accessors(chain = true)
    public static class DomainTraceSummary {

        private Long snapshotId;

        private String status;

        private String snapshotHash;

        private LocalDateTime verifiedAt;
    }
}
