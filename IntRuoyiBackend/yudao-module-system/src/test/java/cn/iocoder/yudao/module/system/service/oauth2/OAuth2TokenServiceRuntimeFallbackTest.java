package cn.iocoder.yudao.module.system.service.oauth2;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.iocoder.yudao.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuth2TokenServiceRuntimeFallbackTest extends BaseMockitoUnitTest {

    @Mock
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;
    @Mock
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;
    @Mock
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;
    @Mock
    private OAuth2ClientService oauth2ClientService;
    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private OAuth2TokenServiceImpl oauth2TokenService;

    @Test
    void getAccessToken_whenRedisLookupThrowsRuntimeException_fallsBackToDatabase() {
        String accessToken = randomString();
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class)
                .setAccessToken(accessToken)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        when(oauth2AccessTokenRedisDAO.get(eq(accessToken)))
                .thenThrow(new ClassCastException("class java.lang.String cannot be cast to class [B"));
        when(oauth2AccessTokenMapper.selectByAccessToken(eq(accessToken))).thenReturn(accessTokenDO);

        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessToken);

        assertPojoEquals(accessTokenDO, result, "createTime", "updateTime", "deleted", "expiresTime");
        verify(oauth2AccessTokenMapper).selectByAccessToken(eq(accessToken));
        verify(oauth2AccessTokenRedisDAO).set(eq(accessTokenDO));
    }
}
