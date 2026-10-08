package cn.iocoder.yudao.module.system.service.oauth2;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.date.DateUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link OAuth2TokenServiceImpl} 的单元测试类
 *
 * @author 瑛泰源码
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class,
        org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration.class})
public class OAuth2TokenServiceImplTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;

    @Resource
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;
    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;

    @MockitoBean
    private OAuth2ClientService oauth2ClientService;
    @MockitoBean
    private AdminUserService adminUserService;

    @BeforeEach
    void setExplicitTenant() {
        TenantContextHolder.setTenantId(1L);
        TenantContextHolder.setIgnore(false);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    public void testCreateAccessToken() {
        TenantContextHolder.setTenantId(0L);
        // 准备参数
        Long userId = randomLongId();
        Integer userType = UserTypeEnum.ADMIN.getValue();
        String clientId = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        // mock 方法
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(clientId)
                .setAccessTokenValiditySeconds(30).setRefreshTokenValiditySeconds(60);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenReturn(clientDO);
        // mock 数据（用户）
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(userId)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setLoginLocked(0);
        user.setTenantId(0L);
        when(adminUserService.getUser(userId)).thenReturn(user);
        when(adminUserService.lockUserForSessionMutation(0L, userId)).thenReturn(user);

        // 调用
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.createAccessToken(userId, userType, clientId, scopes);
        // 断言访问令牌
        OAuth2AccessTokenDO dbAccessTokenDO = oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.getAccessToken());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, dbAccessTokenDO, "expiresTime", "createTime", "updateTime", "deleted");
        assertEquals(userId, accessTokenDO.getUserId());
        assertEquals(userType, accessTokenDO.getUserType());
        assertEquals(2, accessTokenDO.getUserInfo().size());
        assertEquals(user.getNickname(), accessTokenDO.getUserInfo().get("nickname"));
        assertEquals(user.getDeptId().toString(), accessTokenDO.getUserInfo().get("deptId"));
        assertEquals(clientId, accessTokenDO.getClientId());
        assertEquals(scopes, accessTokenDO.getScopes());
        assertFalse(DateUtils.isExpired(accessTokenDO.getExpiresTime()));
        // 断言访问令牌的缓存
        OAuth2AccessTokenDO redisAccessTokenDO = oauth2AccessTokenRedisDAO.get(accessTokenDO.getAccessToken());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, redisAccessTokenDO, "expiresTime", "createTime", "updateTime", "deleted");
        // 断言刷新令牌
        OAuth2RefreshTokenDO refreshTokenDO = oauth2RefreshTokenMapper.selectList().get(0);
        assertPojoEquals(accessTokenDO, refreshTokenDO, "id", "expiresTime", "createTime", "updateTime", "deleted");
        assertFalse(DateUtils.isExpired(refreshTokenDO.getExpiresTime()));
    }

    @Test
    public void testRefreshAccessToken_null() {
        // 准备参数
        String refreshToken = randomString();
        String clientId = randomString();
        // mock 方法

        // 调用，并断言
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refreshToken, clientId),
                new ErrorCode(400, "无效的刷新令牌"));
    }

    @Test
    public void testRefreshAccessToken_clientIdError() {
        // 准备参数
        String refreshToken = randomString();
        String clientId = randomString();
        // mock 方法
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(clientId);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenReturn(clientDO);
        // mock 数据（访问令牌）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class)
                .setRefreshToken(refreshToken).setClientId("error").setUserId(0L)
                .setUserType(UserTypeEnum.ADMIN.getValue());
        refreshTokenDO.setTenantId(TenantContextHolder.getRequiredTenantId());
        oauth2RefreshTokenMapper.insert(refreshTokenDO);

        // 调用，并断言
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refreshToken, clientId),
                new ErrorCode(400, "刷新令牌的客户端编号不正确"));
    }

    @Test
    public void testRefreshAccessToken_expired() {
        // 准备参数
        String refreshToken = randomString();
        String clientId = randomString();
        // mock 方法
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(clientId);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenReturn(clientDO);
        // mock 数据（访问令牌）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class)
                .setRefreshToken(refreshToken).setClientId(clientId)
                .setUserId(0L).setUserType(UserTypeEnum.ADMIN.getValue())
                .setExpiresTime(LocalDateTime.now().minusDays(1));
        refreshTokenDO.setTenantId(TenantContextHolder.getRequiredTenantId());
        oauth2RefreshTokenMapper.insert(refreshTokenDO);

        // 调用，并断言
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refreshToken, clientId),
                new ErrorCode(401, "刷新令牌已过期"));
        assertEquals(0, oauth2AccessTokenMapper.selectCount());
    }

    @Test
    public void testRefreshAccessToken_userNotExists() {
        TenantContextHolder.setTenantId(122L);
        // 准备参数
        String refreshToken = randomString();
        String clientId = randomString();
        // mock 方法
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(clientId)
                .setAccessTokenValiditySeconds(30);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenReturn(clientDO);
        // mock 数据（刷新令牌对应的用户已被删除）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class, o ->
                o.setRefreshToken(refreshToken).setClientId(clientId)
                        .setExpiresTime(LocalDateTime.now().plusDays(1))
                        .setUserType(UserTypeEnum.ADMIN.getValue())
                        .setTenantId(TenantContextHolder.getTenantId()));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class)
                .setRefreshToken(refreshToken)
                .setUserId(refreshTokenDO.getUserId())
                .setUserType(refreshTokenDO.getUserType());
        accessTokenDO.setTenantId(122L);
        oauth2AccessTokenMapper.insert(accessTokenDO);
        oauth2AccessTokenRedisDAO.set(accessTokenDO);
        when(adminUserService.lockUserForSessionMutation(122L, refreshTokenDO.getUserId()))
                .thenThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                        cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS));

        // 调用，并断言失效登录态被明确清理
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refreshToken, clientId),
                new ErrorCode(401, "刷新令牌对应的用户不存在"));
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.getAccessToken()));
        assertNull(oauth2AccessTokenRedisDAO.get(accessTokenDO.getAccessToken()));
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken));
    }

    @Test
    public void testRefreshAccessToken_success() {
        TenantContextHolder.setTenantId(0L);
        // 准备参数
        String refreshToken = randomString();
        String clientId = randomString();
        // mock 方法
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(clientId)
                .setAccessTokenValiditySeconds(30);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenReturn(clientDO);
        // mock 数据（访问令牌）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class, o ->
                o.setRefreshToken(refreshToken).setClientId(clientId)
                        .setExpiresTime(LocalDateTime.now().plusDays(1))
                        .setUserType(UserTypeEnum.ADMIN.getValue())
                        .setTenantId(TenantContextHolder.getTenantId()));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // mock 数据（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class).setRefreshToken(refreshToken)
                .setUserId(refreshTokenDO.getUserId())
                .setUserType(refreshTokenDO.getUserType());
        accessTokenDO.setTenantId(0L);
        oauth2AccessTokenMapper.insert(accessTokenDO);
        oauth2AccessTokenRedisDAO.set(accessTokenDO);
        // mock 数据（用户）
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(refreshTokenDO.getUserId())
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setLoginLocked(0);
        user.setTenantId(0L);
        when(adminUserService.getUser(refreshTokenDO.getUserId())).thenReturn(user);
        when(adminUserService.lockUserForSessionMutation(0L, refreshTokenDO.getUserId())).thenReturn(user);

        // 调用
        OAuth2AccessTokenDO newAccessTokenDO = oauth2TokenService.refreshAccessToken(refreshToken, clientId);
        // 断言，老的访问令牌被删除
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.getAccessToken()));
        assertNull(oauth2AccessTokenRedisDAO.get(accessTokenDO.getAccessToken()));
        // 断言，新的访问令牌
        OAuth2AccessTokenDO dbAccessTokenDO = oauth2AccessTokenMapper.selectByAccessToken(newAccessTokenDO.getAccessToken());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(newAccessTokenDO, dbAccessTokenDO, "expiresTime", "createTime", "updateTime", "deleted");
        assertPojoEquals(newAccessTokenDO, refreshTokenDO, "id", "expiresTime", "createTime", "updateTime", "deleted",
                "creator", "updater");
        assertFalse(DateUtils.isExpired(newAccessTokenDO.getExpiresTime()));
        // 断言，新的访问令牌的缓存
        OAuth2AccessTokenDO redisAccessTokenDO = oauth2AccessTokenRedisDAO.get(newAccessTokenDO.getAccessToken());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(newAccessTokenDO, redisAccessTokenDO, "expiresTime", "createTime", "updateTime", "deleted");
    }

    @Test
    public void testGetAccessToken() {
        // mock 数据（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class)
                .setUserType(UserTypeEnum.MEMBER.getValue()).setExpiresTime(LocalDateTime.now().plusDays(1));
        accessTokenDO.setTenantId(1L);
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // 准备参数
        String accessToken = accessTokenDO.getAccessToken();

        // 调用
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessToken);
        // 断言
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, result, "expiresTime", "createTime", "updateTime", "deleted",
                "creator", "updater");
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, oauth2AccessTokenRedisDAO.get(accessToken), "expiresTime", "createTime", "updateTime", "deleted",
                "creator", "updater");
    }

    @Test
    public void testCheckAccessToken_null() {
        // 调研，并断言
        assertServiceException(() -> oauth2TokenService.checkAccessToken(randomString()),
                new ErrorCode(401, "访问令牌不存在"));
    }

    @Test
    public void testCheckAccessToken_expired() {
        // mock 数据（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class)
                .setUserType(UserTypeEnum.MEMBER.getValue()).setExpiresTime(LocalDateTime.now().minusDays(1));
        accessTokenDO.setTenantId(1L);
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // 准备参数
        String accessToken = accessTokenDO.getAccessToken();

        // 调研，并断言
        assertServiceException(() -> oauth2TokenService.checkAccessToken(accessToken),
                new ErrorCode(401, "访问令牌已过期"));
    }

    @Test
    public void testCheckAccessToken_refreshToken() {
        // mock 数据（访问令牌）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class)
                .setUserId(0L).setUserType(UserTypeEnum.ADMIN.getValue())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        refreshTokenDO.setTenantId(1L);
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // 准备参数
        String accessToken = refreshTokenDO.getRefreshToken();

        // 调研，并断言
        OAuth2AccessTokenDO result = oauth2TokenService.checkAccessToken(accessToken);
        // 断言
        assertPojoEquals(refreshTokenDO, result, "expiresTime", "createTime", "updateTime", "deleted",
                "creator", "updater");
    }

    @Test
    public void testCheckAccessToken_success() {
        // mock 数据（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class)
                .setUserType(UserTypeEnum.MEMBER.getValue()).setExpiresTime(LocalDateTime.now().plusDays(1));
        accessTokenDO.setTenantId(1L);
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // 准备参数
        String accessToken = accessTokenDO.getAccessToken();

        // 调研，并断言
        OAuth2AccessTokenDO result = oauth2TokenService.checkAccessToken(accessToken);
        // 断言
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, result, "expiresTime", "createTime", "updateTime", "deleted",
                "creator", "updater");
    }

    @Test
    void testOrphanCacheCannotAuthorizeEitherPublicEntry() {
        OAuth2AccessTokenDO orphan = randomPojo(OAuth2AccessTokenDO.class)
                .setUserId(0L).setUserType(UserTypeEnum.ADMIN.getValue())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        orphan.setTenantId(1L);
        oauth2AccessTokenRedisDAO.set(orphan);
        assertNull(oauth2TokenService.getAccessToken(orphan.getAccessToken()));
        assertServiceException(() -> oauth2TokenService.checkAccessToken(orphan.getAccessToken()),
                new ErrorCode(401, "访问令牌不存在"));
    }

    @Test
    void testOfficialMemberIdentityAndScopesReplacePoisonedCache() {
        OAuth2AccessTokenDO official = randomPojo(OAuth2AccessTokenDO.class)
                .setUserType(UserTypeEnum.MEMBER.getValue()).setClientId("official-client")
                .setScopes(List.of("read")).setExpiresTime(LocalDateTime.now().plusDays(1));
        official.setTenantId(1L);
        oauth2AccessTokenMapper.insert(official);
        OAuth2AccessTokenDO poisoned = randomPojo(OAuth2AccessTokenDO.class)
                .setAccessToken(official.getAccessToken()).setUserId(0L)
                .setUserType(UserTypeEnum.ADMIN.getValue()).setClientId("poisoned-client")
                .setScopes(List.of("write")).setExpiresTime(LocalDateTime.now().plusDays(1));
        poisoned.setTenantId(2L);
        oauth2AccessTokenRedisDAO.set(poisoned);

        OAuth2AccessTokenDO result = oauth2TokenService.checkAccessToken(official.getAccessToken());
        assertEquals(official.getUserId(), result.getUserId());
        assertEquals(UserTypeEnum.MEMBER.getValue(), result.getUserType());
        assertEquals(1L, result.getTenantId());
        assertEquals("official-client", result.getClientId());
        assertEquals(List.of("read"), result.getScopes());
        assertEquals(result.getUserId(), oauth2AccessTokenRedisDAO.get(official.getAccessToken()).getUserId());
    }

    @Test
    void testAdminValidationUsesStoredTenantAndCurrentUserWithContextRestored() {
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(9021L)
                .setCanonicalUsername("um05-token-current-user").setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setLoginLocked(0).setNickname("Current nickname").setDeptId(12L).setSex(1);
        user.setTenantId(1L);
        adminUserMapper.insert(user);
        OAuth2AccessTokenDO token = randomPojo(OAuth2AccessTokenDO.class).setUserId(user.getId())
                .setUserType(UserTypeEnum.ADMIN.getValue()).setUserInfo(java.util.Map.of("nickname", "Old nickname"))
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        token.setTenantId(1L);
        oauth2AccessTokenMapper.insert(token);
        TenantContextHolder.setTenantId(2L);
        TenantContextHolder.setIgnore(true);

        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(token.getAccessToken());
        assertEquals("Current nickname", result.getUserInfo().get("nickname"));
        assertEquals("12", result.getUserInfo().get("deptId"));
        assertEquals(2L, TenantContextHolder.getTenantId());
        assertTrue(TenantContextHolder.isIgnore());

        adminUserMapper.updateById(new AdminUserDO().setId(user.getId()).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        assertServiceException(() -> oauth2TokenService.checkAccessToken(token.getAccessToken()),
                new ErrorCode(401, "账号已禁用，请重新登录"));
        assertEquals(2L, TenantContextHolder.getTenantId());
        assertTrue(TenantContextHolder.isIgnore());
        adminUserMapper.updateById(new AdminUserDO().setId(user.getId()).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setLoginLocked(1).setLoginLockedTime(LocalDateTime.now()));
        assertServiceException(() -> oauth2TokenService.getAccessToken(token.getAccessToken()),
                new ErrorCode(401, "账号已锁定，请重新登录"));
        assertEquals(2L, TenantContextHolder.getTenantId());
        assertTrue(TenantContextHolder.isIgnore());
    }

    @Test
    void testOfficialTokenWithUnknownTypeIsRejected() {
        OAuth2AccessTokenDO token = randomPojo(OAuth2AccessTokenDO.class).setUserType(99)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        token.setTenantId(1L);
        oauth2AccessTokenMapper.insert(token);
        assertServiceException(() -> oauth2TokenService.checkAccessToken(token.getAccessToken()),
                new ErrorCode(401, "令牌存储身份不完整或用户类型无效"));
    }

    @Test
    public void testRemoveAccessToken_null() {
        // 调用，并断言
        assertNull(oauth2TokenService.removeAccessToken(randomString()));
    }

    @Test
    public void testRemoveAccessToken_success() {
        // mock 数据（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // mock 数据（刷新令牌）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class)
                .setRefreshToken(accessTokenDO.getRefreshToken());
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // 调用
        OAuth2AccessTokenDO result = oauth2TokenService.removeAccessToken(accessTokenDO.getAccessToken());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, result, "expiresTime", "createTime", "updateTime", "deleted",
                "creator", "updater");
        // 断言数据
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.getAccessToken()));
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(accessTokenDO.getRefreshToken()));
        assertNull(oauth2AccessTokenRedisDAO.get(accessTokenDO.getAccessToken()));
    }


    @Test
    public void testGetAccessTokenPage() {
        // mock 数据
        OAuth2AccessTokenDO dbAccessToken = randomPojo(OAuth2AccessTokenDO.class, o -> { // 等会查询到
            o.setUserId(10L);
            o.setUserType(1);
            o.setClientId("test_client");
            o.setExpiresTime(LocalDateTime.now().plusDays(1));
        });
        oauth2AccessTokenMapper.insert(dbAccessToken);
        // 测试 userId 不匹配
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setUserId(20L)));
        // 测试 userType 不匹配
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setUserType(2)));
        // 测试 userType 不匹配
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setClientId("it_client")));
        // 测试 expireTime 不匹配：必须使用明确过去时间，避免与分页查询内的 now() 边界同毫秒抖动
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setExpiresTime(LocalDateTime.now().minusDays(1))));
        // 准备参数
        OAuth2AccessTokenPageReqVO reqVO = new OAuth2AccessTokenPageReqVO();
        reqVO.setUserId(10L);
        reqVO.setUserType(1);
        reqVO.setClientId("test");

        // 调用
        PageResult<OAuth2AccessTokenDO> pageResult = oauth2TokenService.getAccessTokenPage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(dbAccessToken, pageResult.getList().get(0), "expiresTime");
    }

}
