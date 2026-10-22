package cn.iocoder.yudao.module.system.service.oauth2;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void getAccessToken_usesOfficialMemberWithoutReadingRedisAndWritesOfficialCopy() {
        String accessToken = randomString();
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class)
                .setAccessToken(accessToken).setUserId(101L).setUserType(UserTypeEnum.MEMBER.getValue())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        accessTokenDO.setTenantId(1L);
        when(oauth2AccessTokenMapper.selectByAccessToken(eq(accessToken))).thenReturn(accessTokenDO);

        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessToken);

        assertPojoEquals(accessTokenDO, result, "createTime", "updateTime", "deleted", "expiresTime");
        verify(oauth2AccessTokenMapper).selectByAccessToken(eq(accessToken));
        verify(oauth2AccessTokenRedisDAO, never()).get(anyString());
        verify(oauth2AccessTokenRedisDAO).set(eq(accessTokenDO));
    }

    @Test
    void getAccessToken_whenOfficialCopyWriteFails_propagatesOriginalFailure() {
        String accessToken = randomString();
        OAuth2AccessTokenDO official = randomPojo(OAuth2AccessTokenDO.class)
                .setAccessToken(accessToken).setUserId(101L).setUserType(UserTypeEnum.MEMBER.getValue())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        official.setTenantId(1L);
        when(oauth2AccessTokenMapper.selectByAccessToken(eq(accessToken))).thenReturn(official);
        IllegalStateException failure = new IllegalStateException("Redis official-copy write failed");
        doThrow(failure).when(oauth2AccessTokenRedisDAO).set(eq(official));

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> oauth2TokenService.getAccessToken(accessToken)));
        verify(oauth2AccessTokenMapper).selectByAccessToken(eq(accessToken));
        verify(oauth2AccessTokenRedisDAO, never()).get(anyString());
        verify(oauth2AccessTokenRedisDAO).set(eq(official));
    }
}
