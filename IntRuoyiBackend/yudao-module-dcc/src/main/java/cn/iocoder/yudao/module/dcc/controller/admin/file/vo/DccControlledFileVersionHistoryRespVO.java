package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DccControlledFileVersionHistoryRespVO {

    private Long id;
    private String title;
    private String fileNumber;
    private String versionNo;
    private String changeType;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime controlledTime;
    private String processInstanceId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long fileOwnerUserId;
    private String fileOwnerUsernameSnapshot;
    private String fileOwnerNicknameSnapshot;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long fileOwnerSignatureId;
    private String fileOwnerApprovalTaskId;
    private String fileOwnerProcessInstanceId;
    private LocalDateTime fileOwnerSelectedTime;
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
    private Long submitterId;
    private Long requesterId;
    private LocalDateTime submittedTime;
    /** Real row creation event, distinct from a later formal submission. */
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private LocalDateTime approvedTime;
    private LocalDateTime rejectedTime;
    private String rejectReason;
    private String finalizationError;
    private String status;
    private Boolean needTraining;
    private String currentActiveVersionNo;
    private Boolean publishedArtifactAvailable;
    private Boolean stampedArtifactAvailable;
    private LocalDate effectiveDate;
    private String effectiveMode;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveTime;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long effectiveSignatureId;
    private String effectiveProcessInstanceId;
    private java.util.List<String> selectedDistributionDepartmentIds;
    private LocalDateTime publishedTime;
    private LocalDateTime obsoletedTime;
    private Long supersededByFileId;
    private String remark;
    private Boolean canPreview;
    private String previewUnavailableReason;
    private Boolean canDownload;
    private Boolean canDownloadReadOnly;
    private Boolean canDownloadEditable;
    private Boolean canPrint;
    private DccControlledFileActionProjectionRespVO actionProjection;
    private Boolean checkedOut;
    private Long checkedOutBy;
    private String checkedOutByName;
    private LocalDateTime checkedOutTime;
    private String checkedOutReason;
}
