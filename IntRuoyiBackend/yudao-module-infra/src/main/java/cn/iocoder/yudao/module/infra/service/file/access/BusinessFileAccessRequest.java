package cn.iocoder.yudao.module.infra.service.file.access;

public record BusinessFileAccessRequest(
        BusinessFileAccessOperation operation,
        Long fileId,
        Long tenantId,
        Long userId,
        String serviceIdentity,
        String requestId,
        BusinessFileAccessReference claim,
        String sourceIp,
        String userAgent,
        boolean tokenClaimRequired) {

    public BusinessFileAccessRequest(BusinessFileAccessOperation operation, Long fileId, Long tenantId,
                                     Long userId, String serviceIdentity, String requestId,
                                     BusinessFileAccessReference claim, String sourceIp, String userAgent) {
        this(operation, fileId, tenantId, userId, serviceIdentity, requestId, claim, sourceIp, userAgent, false);
    }

    public BusinessFileAccessRequest(BusinessFileAccessOperation operation, Long fileId, Long tenantId,
                                     Long userId, String serviceIdentity, String requestId,
                                     BusinessFileAccessReference claim) {
        this(operation, fileId, tenantId, userId, serviceIdentity, requestId, claim, null, null);
    }

    public static BusinessFileAccessRequest publicDirectLink(Long fileId, String requestId) {
        return publicDirectLink(fileId, requestId, null, null);
    }

    public static BusinessFileAccessRequest publicDirectLink(Long fileId, String requestId,
                                                             String sourceIp, String userAgent) {
        return new BusinessFileAccessRequest(BusinessFileAccessOperation.DIRECT_LINK, fileId,
                null, null, null, requestId, null, sourceIp, userAgent);
    }

    public static BusinessFileAccessRequest tokenCallback(BusinessFileAccessOperation operation, Long fileId,
                                                          Long tenantId, Long userId, String serviceIdentity,
                                                          String requestId, BusinessFileAccessReference claim,
                                                          String sourceIp, String userAgent) {
        return new BusinessFileAccessRequest(operation, fileId, tenantId, userId, serviceIdentity,
                requestId, claim, sourceIp, userAgent, true);
    }
}
