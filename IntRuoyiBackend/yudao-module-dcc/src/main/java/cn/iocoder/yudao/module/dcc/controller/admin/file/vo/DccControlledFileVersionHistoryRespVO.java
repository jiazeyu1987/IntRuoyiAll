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
    private LocalDateTime approvedTime;
    private LocalDateTime rejectedTime;
    private String rejectReason;
    private String finalizationError;
    private String status;
    private String currentActiveVersionNo;
    private Boolean publishedArtifactAvailable;
    private Boolean stampedArtifactAvailable;
    private LocalDate effectiveDate;
    private LocalDateTime publishedTime;
    private LocalDateTime obsoletedTime;
    private Long supersededByFileId;
    private String remark;
    private Boolean canPreview;
    private String previewUnavailableReason;
    private Boolean canDownload;
    private Boolean checkedOut;
    private Long checkedOutBy;
    private String checkedOutByName;
    private LocalDateTime checkedOutTime;
    private String checkedOutReason;
}
