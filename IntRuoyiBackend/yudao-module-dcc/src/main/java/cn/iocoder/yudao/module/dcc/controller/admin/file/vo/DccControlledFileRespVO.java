package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccControlledFileRespVO {

    private Long id;
    private Long masterId;
    private Long categoryId;
    private Long directoryId;
    private String title;
    private String fileName;
    private String contentType;
    private String previewKind;
    private String fileNumber;
    private Long sourceFileId;
    private Long originalFileId;
    private Long publishedFileId;
    private Long stampedFileId;
    private Long productMasterId;
    private String productCode;
    private String productName;
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
    private String currentActiveVersionNo;
    private LocalDate effectiveDate;
    private String remark;
    private String status;
    private Long requesterId;
    private String processInstanceId;
    private String processDefinitionKey;
    private LocalDateTime submittedTime;
    private LocalDateTime approvedTime;
    private LocalDateTime publishedTime;
    private LocalDateTime rejectedTime;
    private LocalDateTime stampedTime;
    private Long obsoletedBy;
    private LocalDateTime obsoletedTime;
    private String obsoleteReason;
    private Long supersededByFileId;
    private String rejectReason;
    private String finalizationError;
    private Boolean canPreview;
    private Boolean canDownload;
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
}
