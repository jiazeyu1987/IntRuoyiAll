package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

public record DccWorkingApplicationStorageRecoveryPreviewVO(
        String controlledFileId, String masterId, String fileNumber, String projectId, String projectFolderId,
        String categoryId, String taxonomyId, String currentTypePath, String originalProofId,
        String originalProofSha256, String observedSha256, String sourceSha256, String matrixDigest,
        String oldDirectoryId, String targetBaseDirectoryId, String targetDirectoryId,
        boolean recovered, String recoveryEvidenceId) {}
