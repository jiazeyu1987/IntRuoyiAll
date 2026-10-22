package cn.iocoder.yudao.module.system.service.oauth2;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.auth.AdminAuthService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link OAuth2GrantServiceImpl} 的单元测试
 *
 * @author 瑛泰源码
 */
public class OAuth2GrantServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private OAuth2GrantServiceImpl oauth2GrantService;

    @Mock
    private OAuth2TokenService oauth2TokenService;
    @Mock
    private OAuth2CodeService oauth2CodeService;
    @Mock
    private AdminAuthService adminAuthService;
    @Mock
    private AdminUserService userService;

    @Test
    public void testGrantImplicit() {
        // 准备参数
        Long userId = randomLongId();
        Integer userType = randomEle(UserTypeEnum.values()).getValue();
        String clientId = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        // mock 方法
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.createAccessToken(eq(userId), eq(userType),
                eq(clientId), eq(scopes))).thenReturn(accessTokenDO);

        // 调用，并断言
        assertPojoEquals(accessTokenDO, oauth2GrantService.grantImplicit(
                userId, userType, clientId, scopes));
    }

    @Test
    public void testGrantAuthorizationCodeForCode() {
        // 准备参数
        Long userId = randomLongId();
        Integer userType = randomEle(UserTypeEnum.values()).getValue();
        String clientId = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        String redirectUri = randomString();
        String state = randomString();
        // mock 方法
        OAuth2CodeDO codeDO = randomPojo(OAuth2CodeDO.class);
        when(oauth2CodeService.createAuthorizationCode(eq(userId), eq(userType),
                eq(clientId), eq(scopes), eq(redirectUri), eq(state))).thenReturn(codeDO);

        // 调用，并断言
        assertEquals(codeDO.getCode(), oauth2GrantService.grantAuthorizationCodeForCode(userId, userType,
                clientId, scopes, redirectUri, state));
    }

    @Test
    public void testGrantAuthorizationCodeForAccessToken() {
        // 准备参数
        String clientId = randomString();
        String code = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        String redirectUri = randomString();
        String state = randomString();
        // mock 方法（code）
        OAuth2CodeDO codeDO = randomPojo(OAuth2CodeDO.class, o -> {
            o.setClientId(clientId);
            o.setRedirectUri(redirectUri);
            o.setState(state);
            o.setScopes(scopes);
        });
        when(oauth2CodeService.consumeAuthorizationCode(eq(code))).thenReturn(codeDO);
        // mock 方法（创建令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.createAccessToken(eq(codeDO.getUserId()), eq(codeDO.getUserType()),
                eq(codeDO.getClientId()), eq(codeDO.getScopes()))).thenReturn(accessTokenDO);

        // 调用，并断言
        assertPojoEquals(accessTokenDO, oauth2GrantService.grantAuthorizationCodeForAccessToken(
                clientId, code, redirectUri, state));
    }

    @Test
    public void testGrantPassword() {
        // 准备参数
        String username = randomString();
        String password = randomString();
        String clientId = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        // mock 方法(认证)
        AdminUserDO user = randomPojo(AdminUserDO.class);
        user.setTenantId(1L);
        when(adminAuthService.authenticate(eq(username), eq(password))).thenReturn(user);
        // mock 方法（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.createPasswordAccessToken(any(AdminPasswordAuthenticationSnapshot.class),
                eq(clientId), eq(scopes))).thenReturn(new AdminSessionToken(accessTokenDO, true));

        // 调用，并断言
        assertPojoEquals(accessTokenDO, oauth2GrantService.grantPassword(
                username, password, clientId, scopes));
        verify(userService, never()).resetUserLoginFailure(anyLong());
        var snapshot = org.mockito.ArgumentCaptor.forClass(AdminPasswordAuthenticationSnapshot.class);
        verify(oauth2TokenService).createPasswordAccessToken(snapshot.capture(), eq(clientId), eq(scopes));
        assertTrue(java.util.Objects.equals(user.getTenantId(), snapshot.getValue().getTenantId())
                && java.util.Objects.equals(user.getId(), snapshot.getValue().getUserId())
                && java.util.Objects.equals(user.getPassword(), snapshot.getValue().getPasswordHash())
                && java.util.Objects.equals(user.getPasswordUpdateTime(), snapshot.getValue().getPasswordUpdateTime())
                && java.util.Objects.equals(user.getPasswordCredentialStatus(), snapshot.getValue().getPasswordCredentialStatus()),
                "Authenticated credential version must reach guarded issuance");
        verify(oauth2TokenService, never()).createAccessToken(anyLong(), anyInt(), anyString(), anyList());
    }

    @Test
    public void testGrantPassword_guardedFailureDoesNotClearLoginFailures() {
        AdminUserDO user = new AdminUserDO().setId(21L).setPassword("encoded-before")
                .setPasswordCredentialStatus("ACTIVE").setPasswordUpdateTime(java.time.LocalDateTime.now());
        user.setTenantId(1L);
        when(adminAuthService.authenticate("task-user", "current-password")).thenReturn(user);
        when(oauth2TokenService.createPasswordAccessToken(any(), eq("task-client"), eq(List.of("read"))))
                .thenThrow(new IllegalStateException("Credential version changed"));

        assertThrows(IllegalStateException.class,
                () -> oauth2GrantService.grantPassword("task-user", "current-password", "task-client", List.of("read")));

        verifyNoInteractions(userService);
        verify(oauth2TokenService, never()).createAccessToken(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    public void testGrantClientCredentials_keepsMachineProtocol() {
        OAuth2AccessTokenDO token = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.createAccessToken(0L, UserTypeEnum.ADMIN.getValue(), "task-client", List.of("read")))
                .thenReturn(token);

        assertSame(token, oauth2GrantService.grantClientCredentials("task-client", List.of("read")));

        verifyNoInteractions(userService, adminAuthService);
        verify(oauth2TokenService, never()).createPasswordAccessToken(any(), anyString(), any());
        verify(oauth2TokenService, never()).createAdminAccessToken(anyLong(), anyString(), any());
    }

    @Test
    public void testAuthenticationSnapshot_isImmutableAndDoesNotExposeSecretsInToString() {
        AdminUserDO user = new AdminUserDO().setId(22L).setPassword("private-hash-value")
                .setPasswordCredentialStatus("ACTIVE").setPasswordUpdateTime(java.time.LocalDateTime.now());
        user.setTenantId(1L);
        AdminPasswordAuthenticationSnapshot snapshot = AdminPasswordAuthenticationSnapshot.from(user);
        user.setPassword("updated-private-hash-value");
        user.setPasswordCredentialStatus("RESET_REQUIRED");

        assertTrue("private-hash-value".equals(snapshot.getPasswordHash()), "Snapshot must retain authenticated version");
        assertEquals("ACTIVE", snapshot.getPasswordCredentialStatus());
        assertFalse(snapshot.toString().contains("private-hash-value"));
        assertFalse(snapshot.toString().contains("passwordHash"));
        AdminSessionToken issued = new AdminSessionToken(new OAuth2AccessTokenDO().setAccessToken("private-token-value"), true);
        assertFalse(issued.toString().contains("private-token-value"));
        assertThrows(NullPointerException.class, () -> AdminPasswordAuthenticationSnapshot.from(null));
        user.setTenantId(null);
        assertThrows(NullPointerException.class, () -> AdminPasswordAuthenticationSnapshot.from(user));
    }

    @Test
    public void testGrantRefreshToken() {
        // 准备参数
        String refreshToken = randomString();
        String clientId = randomString();
        // mock 方法
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.refreshAccessToken(eq(refreshToken), eq(clientId)))
                .thenReturn(accessTokenDO);

        // 调用，并断言
        assertPojoEquals(accessTokenDO, oauth2GrantService.grantRefreshToken(
                refreshToken, clientId));
    }

    @Test
    public void testRevokeToken_clientIdError() {
        // 准备参数
        String clientId = randomString();
        String accessToken = randomString();
        // mock 方法
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.getAccessToken(eq(accessToken))).thenReturn(accessTokenDO);

        // 调用，并断言
        assertFalse(oauth2GrantService.revokeToken(clientId, accessToken));
    }

    @Test
    public void testRevokeToken_success() {
        // 准备参数
        String clientId = randomString();
        String accessToken = randomString();
        // mock 方法（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class).setClientId(clientId);
        when(oauth2TokenService.getAccessToken(eq(accessToken))).thenReturn(accessTokenDO);
        // mock 方法（移除）
        when(oauth2TokenService.removeAccessToken(eq(accessToken))).thenReturn(accessTokenDO);

        // 调用，并断言
        assertTrue(oauth2GrantService.revokeToken(clientId, accessToken));
    }

}
