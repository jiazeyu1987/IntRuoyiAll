package cn.iocoder.yudao.module.dcc.service.audit;

public record DccDirectLinkDeniedLogCreateCommand(Long tenantId,
                                                  Long controlledFileId,
                                                  Long infraFileId,
                                                  String artifactRole,
                                                  String actionType,
                                                  String purpose,
                                                  String result,
                                                  String failureCode,
                                                  String reason,
                                                  String sourceIp,
                                                  String requestId,
                                                  String userAgent) {
}
