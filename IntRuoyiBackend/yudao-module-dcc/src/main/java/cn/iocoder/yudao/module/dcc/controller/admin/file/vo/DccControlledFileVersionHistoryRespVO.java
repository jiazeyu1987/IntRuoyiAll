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
}
