package cn.iocoder.yudao.module.system.service.oauth2;

import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;

import java.util.Objects;

/** 后台服务内部签发结果，不扩展公共令牌协议或持久化对象。 */
public final class AdminSessionToken {

    private final OAuth2AccessTokenDO accessToken;
    private final boolean passwordChangeRequired;

    public AdminSessionToken(OAuth2AccessTokenDO accessToken, boolean passwordChangeRequired) {
        this.accessToken = Objects.requireNonNull(accessToken, "Issued access token is required");
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public OAuth2AccessTokenDO getAccessToken() { return accessToken; }
    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }
}
