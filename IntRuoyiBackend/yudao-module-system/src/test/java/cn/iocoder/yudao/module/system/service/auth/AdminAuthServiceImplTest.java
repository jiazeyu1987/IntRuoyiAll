package cn.iocoder.yudao.module.system.service.auth;

import cn.hutool.core.util.ReflectUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.api.sms.SmsCodeApi;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserBindReqDTO;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.logger.LoginLogTypeEnum;
import cn.iocoder.yudao.module.system.enums.logger.LoginResultEnum;
import cn.iocoder.yudao.module.system.enums.sms.SmsSceneEnum;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.service.logger.LoginLogService;
import cn.iocoder.yudao.module.system.service.member.MemberService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.oauth2.AdminPasswordAuthenticationSnapshot;
import cn.iocoder.yudao.module.system.service.oauth2.AdminSessionToken;
import cn.iocoder.yudao.module.system.service.social.SocialUserService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.service.CaptchaService;
import jakarta.annotation.Resource;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomString;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Import(AdminAuthServiceImpl.class)
public class AdminAuthServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AdminAuthServiceImpl authService;

    @MockitoBean
    private AdminUserService userService;
    @MockitoBean
    private CaptchaService captchaService;
    @MockitoBean
    private LoginLogService loginLogService;
    @MockitoBean
    private SocialUserService socialUserService;
    @MockitoBean
    private SmsCodeApi smsCodeApi;
    @MockitoBean
    private OAuth2TokenService oauth2TokenService;
    @MockitoBean
    private MemberService memberService;
    @MockitoBean
    private Validator validator;

    @BeforeEach
    public void setUp() {
        authService.setCaptchaEnable(true);
        // 注入一个 Validator 对象
        ReflectUtil.setFieldValue(authService, "validator",
                Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Test
    public void testAuthenticate_success() {
        // 准备参数
        String username = randomString();
        String password = randomString();
        // mock user 数据
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setUsername(username)
                .setPassword(password).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setPasswordUpdateTime(LocalDateTime.now().minusDays(30)));
        when(userService.getUserByUsername(eq(username))).thenReturn(user);
        // mock password 匹配
        when(userService.isPasswordMatch(eq(password), eq(user.getPassword()))).thenReturn(true);

        // 调用
        AdminUserDO loginUser = authService.authenticate(username, password);
        // 校验
        assertPojoEquals(user, loginUser);
    }

    @Test
    public void testAuthenticate_userNotFound() {
        // 准备参数
        String username = randomString();
        String password = randomString();

        // 调用, 并断言异常
        assertServiceException(() -> authService.authenticate(username, password),
                AUTH_LOGIN_BAD_CREDENTIALS);
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.BAD_CREDENTIALS.getResult())
                        && o.getUserId() == null)
        );
    }

    @Test
    public void testAuthenticate_badCredentials() {
        // 准备参数
        String username = randomString();
        String password = randomString();
        // mock user 数据
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setUsername(username)
                .setPassword(password).setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(userService.getUserByUsername(eq(username))).thenReturn(user);
        when(userService.isPasswordMatch(eq(password), eq(user.getPassword()))).thenReturn(false);

        // 调用, 并断言异常
        assertServiceException(() -> authService.authenticate(username, password),
                AUTH_LOGIN_BAD_CREDENTIALS);
        verify(userService).recordUserLoginFailure(eq(user.getId()));
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.BAD_CREDENTIALS.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testAuthenticate_userLocked() {
        String username = randomString();
        String password = randomString();
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setUsername(username)
                .setPassword(password).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setLoginLocked(1)
                .setLoginLockedTime(LocalDateTime.now().minusMinutes(10)));
        when(userService.getUserByUsername(eq(username))).thenReturn(user);

        assertServiceException(() -> authService.authenticate(username, password),
                AUTH_LOGIN_USER_LOCKED);
        verify(userService, never()).isPasswordMatch(anyString(), anyString());
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.USER_LOCKED.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testAuthenticate_userDisabled() {
        // 准备参数
        String username = randomString();
        String password = randomString();
        // mock user 数据
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setUsername(username)
                .setPassword(password).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        when(userService.getUserByUsername(eq(username))).thenReturn(user);
        // mock password 匹配
        when(userService.isPasswordMatch(eq(password), eq(user.getPassword()))).thenReturn(true);

        // 调用, 并断言异常
        assertServiceException(() -> authService.authenticate(username, password),
                AUTH_LOGIN_USER_DISABLED);
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.USER_DISABLED.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testAuthenticate_passwordExpired() {
        String username = randomString();
        String password = randomString();
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setUsername(username)
                .setPassword(password).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setPasswordUpdateTime(LocalDateTime.now().minusDays(90)));
        when(userService.getUserByUsername(eq(username))).thenReturn(user);
        when(userService.isPasswordMatch(eq(password), eq(user.getPassword()))).thenReturn(true);

        assertServiceException(() -> authService.authenticate(username, password),
                AUTH_LOGIN_PASSWORD_EXPIRED);
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.PASSWORD_EXPIRED.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testAuthenticate_passwordValidAt89Days() {
        String username = randomString();
        String password = randomString();
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setUsername(username)
                .setPassword(password).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setPasswordUpdateTime(LocalDateTime.now().minusDays(89)));
        when(userService.getUserByUsername(eq(username))).thenReturn(user);
        when(userService.isPasswordMatch(eq(password), eq(user.getPassword()))).thenReturn(true);

        AdminUserDO loginUser = authService.authenticate(username, password);

        assertPojoEquals(user, loginUser);
    }

    @Test
    public void testAuthenticate_passwordChangeRequired() {
        String username = randomString();
        String password = randomString();
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setUsername(username)
                .setPassword(password).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setPasswordCredentialStatus("RESET_REQUIRED")
                .setPasswordUpdateTime(LocalDateTime.now()));
        when(userService.getUserByUsername(eq(username))).thenReturn(user);
        when(userService.isPasswordMatch(eq(password), eq(user.getPassword()))).thenReturn(true);

        AdminUserDO loginUser = authService.authenticate(username, password);
        assertPojoEquals(user, loginUser);
    }

    @Test
    public void testAuthenticate_expiredLockIsNotClearedBeforeGuardedIssuance() {
        AdminUserDO user = new AdminUserDO().setId(10L).setUsername("expired-lock")
                .setPassword("encoded-password").setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setLoginLocked(1).setLoginLockedTime(LocalDateTime.now().minusMinutes(31))
                .setLoginFailureCount(5).setPasswordCredentialStatus("ACTIVE")
                .setPasswordUpdateTime(LocalDateTime.now());
        when(userService.getUserByUsername("expired-lock")).thenReturn(user);
        when(userService.isPasswordMatch("current-password", "encoded-password")).thenReturn(true);

        AdminUserDO authenticated = authService.authenticate("expired-lock", "current-password");

        assertSame(user, authenticated);
        assertEquals(1, authenticated.getLoginLocked());
        assertEquals(5, authenticated.getLoginFailureCount());
        verify(userService, never()).resetUserLoginFailure(anyLong());
    }

    @Test
    public void testLogin_usesIssuedFlagAndCapturedCredentialVersion() {
        AuthLoginReqVO reqVO = new AuthLoginReqVO().setUsername("snapshot-user").setPassword("current-password");
        AdminUserDO user = new AdminUserDO().setId(11L).setUsername("snapshot-user")
                .setPassword("encoded-before").setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setPasswordCredentialStatus("ACTIVE").setPasswordUpdateTime(LocalDateTime.now());
        user.setTenantId(1L);
        when(userService.getUserByUsername("snapshot-user")).thenReturn(user);
        when(userService.isPasswordMatch("current-password", "encoded-before")).thenReturn(true);
        OAuth2AccessTokenDO token = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(11L));
        when(oauth2TokenService.createPasswordAccessToken(any(), eq("default"), isNull()))
                .thenReturn(new AdminSessionToken(token, true));

        AuthLoginRespVO response = authService.login(reqVO);

        assertEquals(Boolean.TRUE, response.getPasswordChangeRequired());
        var order = inOrder(oauth2TokenService, loginLogService, userService);
        order.verify(oauth2TokenService).createPasswordAccessToken(any(), eq("default"), isNull());
        order.verify(loginLogService).createLoginLog(argThat(log ->
                log.getResult().equals(LoginResultEnum.SUCCESS.getResult())));
        order.verify(userService).updateUserLogin(eq(11L), any());
        verify(userService, never()).resetUserLoginFailure(anyLong());
        verify(oauth2TokenService, never()).createAccessToken(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    public void testLogin_guardedIssuanceFailureDoesNotRecordSuccessOrClearFailures() {
        AdminUserDO user = new AdminUserDO().setId(12L).setUsername("changed-user")
                .setPassword("encoded-before").setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setPasswordCredentialStatus("ACTIVE").setPasswordUpdateTime(LocalDateTime.now());
        user.setTenantId(1L);
        when(userService.getUserByUsername("changed-user")).thenReturn(user);
        when(userService.isPasswordMatch("current-password", "encoded-before")).thenReturn(true);
        when(oauth2TokenService.createPasswordAccessToken(any(), eq("default"), isNull()))
                .thenThrow(new IllegalStateException("Credential version changed"));

        assertThrows(IllegalStateException.class, () -> authService.login(new AuthLoginReqVO()
                .setUsername("changed-user").setPassword("current-password")));

        verifyNoInteractions(loginLogService);
        verify(userService, never()).updateUserLogin(anyLong(), any());
        verify(userService, never()).resetUserLoginFailure(anyLong());
    }

    @Test
    public void testSmsLogin_guardedFailureDoesNotPretendSmsCodeWasRestored() {
        AdminUserDO user = new AdminUserDO().setId(13L);
        when(userService.getUserByMobile("task-mobile")).thenReturn(user);
        when(oauth2TokenService.createMobileAccessToken(13L, "task-mobile", "default", null))
                .thenThrow(new IllegalStateException("Authenticated mobile binding changed"));

        assertThrows(IllegalStateException.class,
                () -> authService.smsLogin(new AuthSmsLoginReqVO("task-mobile", "one-time-code")));

        var order = inOrder(smsCodeApi, oauth2TokenService);
        order.verify(smsCodeApi).useSmsCode(any());
        order.verify(oauth2TokenService).createMobileAccessToken(13L, "task-mobile", "default", null);
        verify(oauth2TokenService, never()).createAdminAccessToken(anyLong(), anyString(), any());
        verifyNoInteractions(loginLogService);
        verify(userService, never()).resetUserLoginFailure(anyLong());
    }

    @Test
    public void testRegister_returnsGuardedCredentialFlag() {
        authService.setCaptchaEnable(false);
        AuthRegisterReqVO request = new AuthRegisterReqVO();
        request.setUsername("registered-user");
        request.setPassword("Current@2026");
        when(userService.registerUser(request)).thenReturn(14L);
        OAuth2AccessTokenDO token = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(14L));
        when(oauth2TokenService.createAdminAccessToken(14L, "default", null))
                .thenReturn(new AdminSessionToken(token, false));

        AuthLoginRespVO response = authService.register(request);

        assertEquals(Boolean.FALSE, response.getPasswordChangeRequired());
        assertEquals(14L, response.getUserId());
        verify(userService, never()).resetUserLoginFailure(anyLong());
    }

    @Test
    public void testLogin_success() {
        // 准备参数
        AuthLoginReqVO reqVO = randomPojo(AuthLoginReqVO.class, o ->
                o.setUsername("test_username").setPassword("test_password")
                        .setSocialType(randomEle(SocialTypeEnum.values()).getType()));

        // mock 验证码正确
        authService.setCaptchaEnable(false);
        // mock user 数据
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(1L).setUsername("test_username")
                .setPassword("test_password").setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setPasswordUpdateTime(LocalDateTime.now().minusDays(30)));
        user.setTenantId(1L);
        when(userService.getUserByUsername(eq("test_username"))).thenReturn(user);
        // mock password 匹配
        when(userService.isPasswordMatch(eq("test_password"), eq(user.getPassword()))).thenReturn(true);
        // mock 缓存登录用户到 Redis
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.createPasswordAccessToken(any(AdminPasswordAuthenticationSnapshot.class), eq("default"), isNull()))
                .thenReturn(new AdminSessionToken(accessTokenDO, false));

        // 调用，并校验
        AuthLoginRespVO loginRespVO = authService.login(reqVO);
        assertPojoEquals(accessTokenDO, loginRespVO);
        // 校验调用参数
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.SUCCESS.getResult())
                        && o.getUserId().equals(user.getId()))
        );
        verify(userService, never()).resetUserLoginFailure(anyLong());
        ArgumentCaptor<AdminPasswordAuthenticationSnapshot> snapshot = ArgumentCaptor.forClass(AdminPasswordAuthenticationSnapshot.class);
        verify(oauth2TokenService).createPasswordAccessToken(snapshot.capture(), eq("default"), isNull());
        assertTrue(snapshotMatches(user, snapshot.getValue()), "Authenticated credential version must reach guarded issuance");
        assertEquals(Boolean.FALSE, loginRespVO.getPasswordChangeRequired());
        verify(socialUserService).bindSocialUser(eq(new SocialUserBindReqDTO(
                user.getId(), UserTypeEnum.ADMIN.getValue(),
                reqVO.getSocialType(), reqVO.getSocialCode(), reqVO.getSocialState())));
    }

    @Test
    public void testLogin_passwordChangeRequired_returnsTokenAndFlag() {
        AuthLoginReqVO reqVO = randomPojo(AuthLoginReqVO.class, o ->
                o.setUsername("reset_user").setPassword("reset_password"));
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(2L).setUsername("reset_user")
                .setPassword("reset_password").setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setPasswordCredentialStatus("RESET_REQUIRED").setPasswordUpdateTime(LocalDateTime.now()));
        user.setTenantId(1L);
        when(userService.getUserByUsername(eq("reset_user"))).thenReturn(user);
        when(userService.isPasswordMatch(eq("reset_password"), eq(user.getPassword()))).thenReturn(true);
        OAuth2AccessTokenDO token = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(2L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.createPasswordAccessToken(any(AdminPasswordAuthenticationSnapshot.class), eq("default"), isNull()))
                .thenReturn(new AdminSessionToken(token, true));

        AuthLoginRespVO response = authService.login(reqVO);

        assertPojoEquals(token, response);
        assertEquals(Boolean.TRUE, response.getPasswordChangeRequired());
    }

    @Test
    public void testLogin_successWithoutCaptchaVerificationWhenCaptchaEnabled() {
        // 准备参数
        AuthLoginReqVO reqVO = new AuthLoginReqVO();
        reqVO.setUsername("test_username");
        reqVO.setPassword("test_password");
        reqVO.setCaptchaVerification(null);

        // mock user 数据
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(1L).setUsername("test_username")
                .setPassword("test_password").setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setPasswordUpdateTime(LocalDateTime.now().minusDays(30)));
        user.setTenantId(1L);
        when(userService.getUserByUsername(eq("test_username"))).thenReturn(user);
        // mock password 匹配
        when(userService.isPasswordMatch(eq("test_password"), eq(user.getPassword()))).thenReturn(true);
        // mock 缓存登录用户到 Redis
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.createPasswordAccessToken(any(AdminPasswordAuthenticationSnapshot.class), eq("default"), isNull()))
                .thenReturn(new AdminSessionToken(accessTokenDO, false));

        // 调用，并校验
        AuthLoginRespVO loginRespVO = authService.login(reqVO);
        assertPojoEquals(accessTokenDO, loginRespVO);
        verifyNoInteractions(captchaService);
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.SUCCESS.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testSendSmsCode() {
        // 准备参数
        String mobile = randomString();
        Integer scene = SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene();
        AuthSmsSendReqVO reqVO = new AuthSmsSendReqVO(mobile, scene);
        // mock 方法（用户信息）
        AdminUserDO user = randomPojo(AdminUserDO.class);
        when(userService.getUserByMobile(eq(mobile))).thenReturn(user);

        // 调用
        authService.sendSmsCode(reqVO);
        // 断言
        verify(smsCodeApi).sendSmsCode(argThat(sendReqDTO -> {
            assertEquals(mobile, sendReqDTO.getMobile());
            assertEquals(scene, sendReqDTO.getScene());
            return true;
        }));
    }

    @Test
    public void testSmsLogin_success() {
        // 准备参数
        String mobile = randomString();
        String code = randomString();
        AuthSmsLoginReqVO reqVO = new AuthSmsLoginReqVO(mobile, code);
        // mock 方法（验证码）
        doNothing().when(smsCodeApi).useSmsCode((argThat(smsCodeUseReqDTO -> {
            assertEquals(mobile, smsCodeUseReqDTO.getMobile());
            assertEquals(code, smsCodeUseReqDTO.getCode());
            assertEquals(SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene(), smsCodeUseReqDTO.getScene());
            return true;
        })));
        // mock 方法（用户信息）
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(1L));
        when(userService.getUserByMobile(eq(mobile))).thenReturn(user);
        // mock 缓存登录用户到 Redis
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.createMobileAccessToken(eq(1L), eq(mobile), eq("default"), isNull()))
                .thenReturn(new AdminSessionToken(accessTokenDO, true));

        // 调用，并断言
        AuthLoginRespVO loginRespVO = authService.smsLogin(reqVO);
        assertPojoEquals(accessTokenDO, loginRespVO);
        assertEquals(Boolean.TRUE, loginRespVO.getPasswordChangeRequired());
        verify(userService, never()).resetUserLoginFailure(anyLong());
        verify(oauth2TokenService).createMobileAccessToken(1L, mobile, "default", null);
        verify(oauth2TokenService, never()).createAdminAccessToken(anyLong(), anyString(), any());
        // 断言调用
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_MOBILE.getType())
                        && o.getResult().equals(LoginResultEnum.SUCCESS.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testSocialLogin_success() {
        // 准备参数
        AuthSocialLoginReqVO reqVO = randomPojo(AuthSocialLoginReqVO.class);
        // mock 方法（绑定的用户编号）
        Long userId = 1L;
        when(socialUserService.getSocialUserByCode(eq(UserTypeEnum.ADMIN.getValue()), eq(reqVO.getType()),
                eq(reqVO.getCode()), eq(reqVO.getState()))).thenReturn(new SocialUserRespDTO(randomString(), randomString(), randomString(), userId));
        // mock（用户）
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(userId));
        when(userService.getUser(eq(userId))).thenReturn(user);
        // mock 缓存登录用户到 Redis
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.createAdminAccessToken(eq(1L), eq("default"), isNull()))
                .thenReturn(new AdminSessionToken(accessTokenDO, true));

        // 调用，并断言
        AuthLoginRespVO loginRespVO = authService.socialLogin(reqVO);
        assertPojoEquals(accessTokenDO, loginRespVO);
        assertEquals(Boolean.TRUE, loginRespVO.getPasswordChangeRequired());
        verify(userService, never()).resetUserLoginFailure(anyLong());
        // 断言调用
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_SOCIAL.getType())
                        && o.getResult().equals(LoginResultEnum.SUCCESS.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testValidateCaptcha_successWithEnable() {
        // 准备参数
        AuthLoginReqVO reqVO = randomPojo(AuthLoginReqVO.class);

        // mock 验证通过
        when(captchaService.verification(argThat(captchaVO -> {
            assertEquals(reqVO.getCaptchaVerification(), captchaVO.getCaptchaVerification());
            return true;
        }))).thenReturn(ResponseModel.success());

        // 调用，无需断言
        authService.validateCaptcha(reqVO);
    }

    @Test
    public void testValidateCaptcha_successWithDisable() {
        // 准备参数
        AuthLoginReqVO reqVO = randomPojo(AuthLoginReqVO.class);

        // mock 验证码关闭
        authService.setCaptchaEnable(false);

        // 调用，无需断言
        authService.validateCaptcha(reqVO);
    }

    @Test
    public void testCaptcha_fail() {
        // 准备参数
        AuthLoginReqVO reqVO = randomPojo(AuthLoginReqVO.class);

        // mock 验证通过
        when(captchaService.verification(argThat(captchaVO -> {
            assertEquals(reqVO.getCaptchaVerification(), captchaVO.getCaptchaVerification());
            return true;
        }))).thenReturn(ResponseModel.errorMsg("就是不对"));

        // 调用, 并断言异常
        assertServiceException(() -> authService.validateCaptcha(reqVO), AUTH_LOGIN_CAPTCHA_CODE_ERROR, "就是不对");
        // 校验调用参数
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.CAPTCHA_CODE_ERROR.getResult()))
        );
    }

    @Test
    public void testRefreshToken() {
        // 准备参数
        String refreshToken = randomString();
        // mock 方法
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.refreshAdminAccessToken(eq(refreshToken), eq("default")))
                .thenReturn(new AdminSessionToken(accessTokenDO, true));

        // 调用
        AuthLoginRespVO loginRespVO = authService.refreshToken(refreshToken);
        // 断言
        assertPojoEquals(accessTokenDO, loginRespVO);
        assertEquals(Boolean.TRUE, loginRespVO.getPasswordChangeRequired());
    }

    private static boolean snapshotMatches(AdminUserDO user, AdminPasswordAuthenticationSnapshot snapshot) {
        return java.util.Objects.equals(user.getTenantId(), snapshot.getTenantId())
                && java.util.Objects.equals(user.getId(), snapshot.getUserId())
                && java.util.Objects.equals(user.getPassword(), snapshot.getPasswordHash())
                && java.util.Objects.equals(user.getPasswordUpdateTime(), snapshot.getPasswordUpdateTime())
                && java.util.Objects.equals(user.getPasswordCredentialStatus(), snapshot.getPasswordCredentialStatus());
    }

    @Test
    public void testLogout_success() {
        // 准备参数
        String token = randomString();
        // mock
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.removeAccessToken(eq(token))).thenReturn(accessTokenDO);

        // 调用
        authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        // 校验调用参数
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGOUT_SELF.getType())
                        && o.getResult().equals(LoginResultEnum.SUCCESS.getResult()))
        );
        // 调用，并校验

    }

    @Test
    public void testLogout_fail() {
        // 准备参数
        String token = randomString();

        // 调用
        authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        // 校验调用参数
        verify(loginLogService, never()).createLoginLog(any());
    }

}
