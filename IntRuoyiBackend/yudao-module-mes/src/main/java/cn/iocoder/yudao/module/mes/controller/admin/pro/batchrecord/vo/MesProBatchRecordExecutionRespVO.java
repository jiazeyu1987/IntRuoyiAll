package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MesProBatchRecordExecutionRespVO {

    private Long id;
    private Long executionId;

    private String executionCode;

    private Long templateId;

    private String templateCode;

    private String templateName;

    private Long workOrderId;

    private String workOrderCode;

    private Long batchExecutionId;

    private String instanceScope;

    private String sharedFormKey;

    private Long routeProcessId;

    private Long routeId;

    private String routeCode;

    private String routeName;

    private Long processId;

    private String processCode;

    private String processName;

    private Long taskId;

    private Long workstationId;

    private String workstationCode;

    private String workstationName;

    private String batchRecordReportId;

    private Long batchRecordDefinitionId;

    private Long batchRecordVersionId;

    private String batchRecordReportCode;

    private String batchRecordReportName;

    private String formSlotType;

    private String recordCategory;

    private String validationProfile;

    private Boolean recordbookEnabled;

    private Long permissionScopeId;

    private Long routeBindingId;

    private String routeBindingSnapshotHash;

    private String archiveVisibility;

    private String slotConfigSnapshotHash;

    private String batchCode;

    private Integer status;

    private String activeContextKey;

    private Long revisionRootExecutionId;

    private Integer revisionNo;

    private Long sourceRejectedExecutionId;

    private Long supersededByExecutionId;

    private String revisionReason;

    private String revisionParentHash;

    private Boolean activeRevisionFlag;

    private Boolean bindingResolved;

    private Boolean canOpen;

    private String processDefinitionKey;

    private String processInstanceId;

    private Long submittedBy;

    private LocalDateTime submittedAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private Long rejectedBy;

    private LocalDateTime rejectedAt;

    private String rejectReason;

    private LocalDateTime closedAt;

    private Long approvalSnapshotId;

    private String approvalSnapshotHash;

    private String approvalSnapshotStatus;

    private Boolean canApprove;

    private Boolean canReject;

    private Boolean canViewTracking;

    private Boolean canViewSignatures;

    private Boolean canGenerateArchive;

    private Boolean canDownloadArchive;

    private Boolean preReleaseEditable;

    private String preReleaseEditReason;

    private List<MesProBatchRecordExecutionSignatureRespVO> signatureSummaries;

    private List<ReviewAssigneeOption> reviewAssigneeOptions;

    private String reviewAssigneeOptionError;

    private List<AttachmentSummary> attachmentSummaries;

    private List<EdhrBatchExecutionTaskRespVO> assistSwitchTasks;

    private String sheetLayoutJson;

    private String metaJson;

    private String executionSnapshotJson;

    private List<MesProBatchRecordExecutionCellValueVO> cellValues;

    private String cellValuesHash;

    private Long fieldAuditRevision;

    private String fieldAuditHeadHash;

    private Long fieldAuditLastBatchId;

    private String remark;

    private String creator;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Data
    public static class AttachmentSummary {

        private Long id;
        private Long auditBatchId;
        private Long signatureId;
        private Long executionId;
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
        private String previousAttachmentHash;
        private String attachmentHash;
        private Long operatorId;
        private String operatorName;
        private LocalDateTime operatedAt;
        private String reasonCategory;
        private String reasonText;
    }

    @Data
    public static class ReviewAssigneeOption {

        private String signatureCellKey;

        private Integer signatureRowIndex;

        private Integer signatureColumnIndex;

        private String reviewSourceType;

        private Long reviewSourceId;

        private List<Long> reviewSourceIds;

        private String reviewSourceName;

        private List<CandidateUser> candidates;
    }

    @Data
    public static class CandidateUser {

        private Long userId;

        private String userName;
    }
}
