package cn.iocoder.yudao.module.dcc.service.token;

public record DccViewerTokenExpectedContext(Long tenantId,
                                            Long userId,
                                            Long fileId,
                                            String versionId,
                                            Long accessEventId,
                                            String purpose,
                                            Long ttlSeconds,
                                            String nonce,
                                            String tokenId) {
}
