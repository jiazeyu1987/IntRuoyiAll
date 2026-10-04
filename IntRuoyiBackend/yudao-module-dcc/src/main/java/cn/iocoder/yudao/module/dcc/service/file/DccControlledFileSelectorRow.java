package cn.iocoder.yudao.module.dcc.service.file;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Name selection is not a body capability; canPreview is resolved independently by QueryService. */
public record DccControlledFileSelectorRow(
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long tenantId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long controlledFileId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long latestControlledFileId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long masterId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long projectId, String projectName,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long sourceDirectoryId, String sourceDirectoryName,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long projectFolderId, String projectFolderName,
        String fileNumber, String fileName, String versionNo, String status,
        boolean controlled, boolean pendingEffect, boolean executable, boolean canPreview) {}
