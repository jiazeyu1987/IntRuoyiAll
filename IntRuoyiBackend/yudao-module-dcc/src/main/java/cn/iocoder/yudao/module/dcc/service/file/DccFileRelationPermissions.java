package cn.iocoder.yudao.module.dcc.service.file;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Permission facts only; the actual command and binary endpoints validate again. */
public record DccFileRelationPermissions(
        @JsonFormat(shape = JsonFormat.Shape.STRING) Long controlledFileId,
        boolean canEdit,
        boolean canPreview,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long tenantId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long masterId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long projectId,
        String projectName,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long projectFolderId,
        String projectFolderName,String fileNumber,String fileName,String versionNo,String status,
        boolean controlled,boolean pendingEffect,boolean executable,boolean hasCurrentControlledSource) {}
