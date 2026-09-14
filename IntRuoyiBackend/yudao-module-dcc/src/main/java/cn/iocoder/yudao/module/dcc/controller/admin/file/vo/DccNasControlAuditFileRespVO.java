package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Admin - DCC NAS control audit file Response VO")
@Data
public class DccNasControlAuditFileRespVO {

    @Schema(description = "Audit file id")
    private Long auditFileId;

    @Schema(description = "Audit task id")
    private Long taskId;

    @Schema(description = "NAS share name")
    private String nasShareName;

    @Schema(description = "Scan root path")
    private String rootPath;

    @Schema(description = "Normalized NAS relative path")
    private String normalizedRelativePath;

    @Schema(description = "Tenant-scoped NAS path hash")
    private String pathHash;

    @Schema(description = "File name")
    private String fileName;

    @Schema(description = "File size")
    private Long fileSize;

    @Schema(description = "Modified time in UTC")
    private LocalDateTime modifiedAt;

    @Schema(description = "Source signature snapshot")
    private String sourceSignature;

    @Schema(description = "Control status")
    private String controlStatus;

    @Schema(description = "Classification status")
    private String classificationStatus;

    @Schema(description = "Matched DCC project code item id")
    private Long matchedProjectCodeId;

    @Schema(description = "Matched file type taxonomy id")
    private Long matchedFileTypeTaxonomyId;

    @Schema(description = "Matched file type level 1")
    private String matchedFileTypeLevel1;

    @Schema(description = "Matched file type level 2")
    private String matchedFileTypeLevel2;

    @Schema(description = "Matched file type level 3")
    private String matchedFileTypeLevel3;

    @Schema(description = "Matched file type level 4")
    private String matchedFileTypeLevel4;

    @Schema(description = "Matched file type level 5")
    private String matchedFileTypeLevel5;

    @Schema(description = "Classification reason")
    private String classificationReason;

    @Schema(description = "Recognition candidate summary JSON")
    private String classificationCandidatesJson;

    @Schema(description = "Backend-generated expected local relative path")
    private String expectedLocalRelativePath;

    @Schema(description = "Download status")
    private String downloadStatus;

    @Schema(description = "Archive status")
    private String archiveStatus;

    @Schema(description = "Local relative path")
    private String localRelativePath;

    @Schema(description = "Local write error code")
    private String localWriteErrorCode;

    @Schema(description = "Local write error")
    private String localWriteError;

    @Schema(description = "Archive error code")
    private String archiveErrorCode;

    @Schema(description = "Archive error")
    private String archiveError;

    @Schema(description = "Controlled file id after archive")
    private Long controlledFileId;

    @Schema(description = "NAS original-path sync status")
    private String originalPathSyncStatus;

    @Schema(description = "NAS original-path sync record id")
    private Long originalPathSyncFileId;

    @Schema(description = "NAS original-path sync task id")
    private Long originalPathSyncTaskId;

    @Schema(description = "NAS original-path sync task item id")
    private Long originalPathSyncTaskItemId;

    @Schema(description = "NAS original-path sync failure code")
    private String originalPathSyncErrorCode;

    @Schema(description = "NAS original-path sync failure detail")
    private String originalPathSyncError;
}
