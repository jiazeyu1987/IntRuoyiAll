package cn.iocoder.yudao.module.dcc.service.file;

public record DccControlledFileArtifactReference(Long controlledFileId, Long tenantId,
                                                 DccControlledFileArtifactRole role) {
}
