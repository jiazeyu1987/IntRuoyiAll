package cn.iocoder.yudao.module.dcc.service.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccViewerTokenPayload {

    private String tokenId;
    private String nonce;
    private Long tenantId;
    private Long userId;
    private Long fileId;
    private String versionId;
    private Long accessEventId;
    private String purpose;
    private Long ttlSeconds;
    private Long issuedAtEpochSecond;
    private Long expiresAtEpochSecond;

}
