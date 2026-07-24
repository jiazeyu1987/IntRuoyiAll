package cn.iocoder.yudao.module.dcc.service.preview;

public record DccPreviewAccessRequest(Long tenantId,
                                      Long userId,
                                      Long fileId,
                                      String versionId,
                                      String fileNumber,
                                      String accessType,
                                      String purpose,
                                      Long ttlSeconds,
                                      String userIdentifier,
                                      String userDisplayName,
                                      Long deptId,
                                      String deptName,
                                      String tenantName,
                                      String privacyMode,
                                      String sourceIp,
                                      String userAgent,
                                      String requestId) {
}
