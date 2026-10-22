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
                                      String requestId,
                                      String processInstanceId) {
    /** Ordinary previews have no approval-round proof. */
    public DccPreviewAccessRequest(Long tenantId,Long userId,Long fileId,String versionId,String fileNumber,
            String accessType,String purpose,Long ttlSeconds,String userIdentifier,String userDisplayName,
            Long deptId,String deptName,String tenantName,String privacyMode,String sourceIp,String userAgent,String requestId) {
        this(tenantId,userId,fileId,versionId,fileNumber,accessType,purpose,ttlSeconds,userIdentifier,userDisplayName,
                deptId,deptName,tenantName,privacyMode,sourceIp,userAgent,requestId,null);
    }
}
