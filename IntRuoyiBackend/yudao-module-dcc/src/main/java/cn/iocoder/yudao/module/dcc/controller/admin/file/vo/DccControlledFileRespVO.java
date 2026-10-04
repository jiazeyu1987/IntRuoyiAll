package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccControlledFileRespVO {

    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long id;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long tenantId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long latestControlledFileId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long projectFolderId;
    private String projectFolderName;
    private String projectName;
    private Boolean controlled;
    private Boolean pendingEffect;
    private Boolean executable;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long masterId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long fileOwnerUserId;
    private String fileOwnerUsernameSnapshot;
    private String fileOwnerNicknameSnapshot;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long fileOwnerSignatureId;
    private String fileOwnerApprovalTaskId;
    private String fileOwnerProcessInstanceId;
    private LocalDateTime fileOwnerSelectedTime;
    private Long categoryId;
    private String businessSourceType;
    private Long registrationCertificateId;
    private Long registrationCertificateVersionId;
    private Long registrationCertificateBusinessFileId;
    private Long directoryId;
    private String directoryPath;
    private String title;
    private String fileName;
    private String contentType;
    private String previewKind;
    private String previewUnavailableReason;
    private Boolean publishedArtifactAvailable;
    private Boolean stampedArtifactAvailable;
    private Boolean trainingRecordAvailable;
    private String trainingRecordFileName;
    private Boolean distributionCompleted;
    private String fileNumber;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productMasterId;
    private String productSource;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productCatalogId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productRelationId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productCreateRequestId;
    private String productCode;
    private String productName;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long dccProjectCodeId;
    private String projectCodeRecognitionType;
    private String projectCodeRecognitionText;
    private Long projectCodeRecognizedBy;
    private LocalDateTime projectCodeRecognizedTime;
    private Long fileTypeTaxonomyId;
    private String fileTypeLevel1;
    private String fileTypeLevel2;
    private String fileTypeLevel3;
    private String fileTypeLevel4;
    private String fileTypeLevel5;
    private Boolean needTraining;
    private String processType;
    private String versionNo;
    private String revisionChangeType;
    private Integer revisionAttemptNo;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long reworkPredecessorControlledFileId;
    private Long revisionSourceControlledFileId;
    private String revisionSourceVersionNo;
    private Long selectedIterationControlledFileId;
    private String selectedIterationVersionNo;
    private String sourceOriginalFileName;
    private String revisionCode;
    private Integer iterationNo;
    private Long predecessorControlledFileId;
    private Long revisionBaseActiveControlledFileId;
    private String sourceSha256;
    private String previousSourceSha256;
    private String changeDescription;
    private String currentActiveVersionNo;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;
    private String remark;
    private String status;
    private Long requesterId;
    private String processInstanceId;
    private String processDefinitionKey;
    private LocalDateTime submittedTime;
    private LocalDateTime approvedTime;
    private LocalDateTime publishedTime;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime controlledTime;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime activatedTime;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime distributedTime;
    private String distributionReminderStage;
    private LocalDateTime rejectedTime;
    private LocalDateTime stampedTime;
    private Long obsoletedBy;
    private LocalDateTime obsoletedTime;
    private String obsoleteReason;
    private Long supersededByFileId;
    private String rejectReason;
    private String finalizationError;
    private Boolean checkedOut;
    private Long checkedOutBy;
    private String checkedOutByName;
    private LocalDateTime checkedOutTime;
    private String checkedOutReason;
    private Boolean canPreview;
    private Boolean canDownload;
    private Boolean canPrint;
    private DccControlledFileAccessExplanationRespVO accessExplanation;
    private Boolean systemRecordDownloadOpen;
    private Boolean modifying;
    private Boolean canObsolete;
    private Boolean canPublish;
    private Boolean canManualRelease;
    private DccControlledFileActionProjectionRespVO actionProjection;
    private Boolean hasPendingTrainingAcknowledgement;
    private DccExternalFileReviewRespVO externalReview;
    private List<DccControlledFileRouteSnapshotRespVO> routeSnapshots;
    private List<DccControlledFileVersionHistoryRespVO> versionHistory;
    private List<DccControlledFileDistributionStatusRespVO> distributionStatuses;
    private List<DccControlledFileTrainingStatusRespVO> trainingStatuses;
    private List<DccControlledFileSignatureSummaryRespVO> signatureSummaries;
    private List<DccControlledFileRelatedFileRespVO> relatedFiles;
    private List<DccControlledFileAttachmentRespVO> attachments;
}
