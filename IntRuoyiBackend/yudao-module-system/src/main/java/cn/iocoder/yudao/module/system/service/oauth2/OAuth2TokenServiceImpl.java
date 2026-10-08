package cn.iocoder.yudao.module.system.service.oauth2;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.date.DateUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
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
import cn.iocoder.yudao.module.system.service.user.AdminUserServiceImpl;
import cn.iocoder.yudao.module.system.service.user.AdminUserPasswordPolicy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * OAuth2.0 Token Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Slf4j
public class OAuth2TokenServiceImpl implements OAuth2TokenService {

    @Resource
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;
    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;

    @Resource
    private OAuth2ClientService oauth2ClientService;
    @Resource
    private PlatformTransactionManager transactionManager;
    @Resource
    @Lazy // 懒加载，避免循环依赖
    private AdminUserService adminUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO createAccessToken(Long userId, Integer userType, String clientId, List<String> scopes) {
        if (Objects.equals(userType, UserTypeEnum.ADMIN.getValue())) {
            requireAdminId(userId, true);
            if (userId > 0) {
                AdminUserDO user = lockActiveAdmin(TenantContextHolder.getRequiredTenantId(), userId);
                return issueAdminToken(user, clientId, scopes).getAccessToken();
            }
            TenantContextHolder.getRequiredTenantId();
        }
        OAuth2ClientDO clientDO = oauth2ClientService.validOAuthClientFromCache(clientId);
        // 创建刷新令牌
        OAuth2RefreshTokenDO refreshTokenDO = createOAuth2RefreshToken(userId, userType, clientDO, scopes);
        // 创建访问令牌
        return createOAuth2AccessToken(refreshTokenDO, clientDO);
    }

    @Override
    public OAuth2AccessTokenDO refreshAccessToken(String refreshToken, String clientId) {
        OAuth2RefreshTokenDO located = oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken);
        if (located == null) {
            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "无效的刷新令牌");
        }
        if (Objects.equals(located.getUserType(), UserTypeEnum.ADMIN.getValue())) {
            requireAdminId(located.getUserId(), true);
            Objects.requireNonNull(located.getTenantId(), "Stored ADMIN refresh-token tenant is required");
            return refreshLocatedAdmin(located, clientId).getAccessToken();
        }
        // MEMBER 原有业务异常提交语义保持；Redis 等运行异常仍由模板回滚。
        AtomicReference<ServiceException> error = new AtomicReference<>();
        OAuth2AccessTokenDO result = new TransactionTemplate(transactionManager).execute(status -> {
            try {
                return refreshLegacyAccessToken(located, clientId);
            } catch (ServiceException ex) {
                error.set(ex);
                return null;
            }
        });
        if (error.get() != null) {
            throw error.get();
        }
        return result;
    }

    private OAuth2AccessTokenDO refreshLegacyAccessToken(OAuth2RefreshTokenDO refreshTokenDO, String clientId) {
        String refreshToken = refreshTokenDO.getRefreshToken();

        // 校验 Client 匹配
        OAuth2ClientDO clientDO = oauth2ClientService.validOAuthClientFromCache(clientId);
        if (ObjectUtil.notEqual(clientId, refreshTokenDO.getClientId())) {
            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "刷新令牌的客户端编号不正确");
        }

        // 移除相关的访问令牌
        List<OAuth2AccessTokenDO> accessTokenDOs = oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken);
        if (CollUtil.isNotEmpty(accessTokenDOs)) {
            oauth2AccessTokenMapper.deleteByIds(convertSet(accessTokenDOs, OAuth2AccessTokenDO::getId));
            oauth2AccessTokenRedisDAO.deleteList(convertSet(accessTokenDOs, OAuth2AccessTokenDO::getAccessToken));
        }

        // 已过期的情况下，删除刷新令牌
        if (DateUtils.isExpired(refreshTokenDO.getExpiresTime())) {
            oauth2RefreshTokenMapper.deleteById(refreshTokenDO.getId());
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "刷新令牌已过期");
        }

        // 加载刷新令牌对应的用户信息。用户已删除时，清理刷新令牌，终止失效登录态的重复刷新。
        Map<String, String> userInfo;
        try {
            userInfo = buildUserInfo(refreshTokenDO.getUserId(), refreshTokenDO.getUserType(),
                    "刷新令牌对应的用户不存在");
        } catch (ServiceException ex) {
            oauth2RefreshTokenMapper.deleteById(refreshTokenDO.getId());
            log.warn("[refreshAccessToken][刷新令牌编号({})对应的用户({}/{})不存在，已清理失效登录态]",
                    refreshTokenDO.getId(), refreshTokenDO.getUserType(), refreshTokenDO.getUserId());
            throw ex;
        }

        // 创建访问令牌
        return createOAuth2AccessToken(refreshTokenDO, clientDO, userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSessionToken createPasswordAccessToken(AdminPasswordAuthenticationSnapshot snapshot,
                                                       String clientId, List<String> scopes) {
        Objects.requireNonNull(snapshot, "Password authentication snapshot is required");
        if (!Objects.equals(snapshot.getTenantId(), TenantContextHolder.getRequiredTenantId())) {
            throw unauthorized("认证租户已变化，请重新登录");
        }
        AdminUserDO user = lockActiveAdmin(snapshot.getTenantId(), snapshot.getUserId());
        if (!Objects.equals(snapshot.getTenantId(), user.getTenantId())
                || !Objects.equals(snapshot.getUserId(), user.getId())
                || !Objects.equals(snapshot.getPasswordHash(), user.getPassword())
                || !Objects.equals(snapshot.getPasswordUpdateTime(), user.getPasswordUpdateTime())
                || !Objects.equals(snapshot.getPasswordCredentialStatus(), user.getPasswordCredentialStatus())) {
            throw unauthorized("认证凭据已变化，请重新登录");
        }
        if (AdminUserPasswordPolicy.isExpired(user.getPasswordUpdateTime(), LocalDateTime.now())) {
            throw unauthorized("密码已过期，请重新认证");
        }
        adminUserService.resetUserLoginFailure(user.getId());
        return issueAdminToken(user, clientId, scopes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSessionToken createMobileAccessToken(Long userId, String authenticatedMobile,
                                                     String clientId, List<String> scopes) {
        requireAdminId(userId, false);
        AdminUserDO user = lockActiveAdmin(TenantContextHolder.getRequiredTenantId(), userId);
        if (StrUtil.isBlank(authenticatedMobile) || !Objects.equals(authenticatedMobile, user.getMobile())) {
            throw unauthorized("手机号绑定已变化，请重新验证");
        }
        adminUserService.resetUserLoginFailure(userId);
        return issueAdminToken(user, clientId, scopes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSessionToken createAdminAccessToken(Long userId, String clientId, List<String> scopes) {
        requireAdminId(userId, false);
        AdminUserDO user = lockActiveAdmin(TenantContextHolder.getRequiredTenantId(), userId);
        adminUserService.resetUserLoginFailure(userId);
        return issueAdminToken(user, clientId, scopes);
    }

    @Override
    public AdminSessionToken refreshAdminAccessToken(String refreshToken, String clientId) {
        OAuth2RefreshTokenDO located = oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken);
        if (located == null || !Objects.equals(located.getUserType(), UserTypeEnum.ADMIN.getValue())) {
            throw unauthorized("无效的后台刷新令牌");
        }
        requireAdminId(located.getUserId(), false);
        return refreshLocatedAdmin(located, clientId);
    }

    private AdminSessionToken refreshLocatedAdmin(OAuth2RefreshTokenDO located, String clientId) {
        Long tenantId = Objects.requireNonNull(located.getTenantId(), "Stored ADMIN refresh-token tenant is required");
        AtomicReference<AdminSessionToken> result = new AtomicReference<>();
        AtomicReference<ServiceException> cleanupError = new AtomicReference<>();
        TenantUtils.execute(tenantId, (Runnable) () -> result.set(new TransactionTemplate(transactionManager).execute(status -> {
            requireWritableTransaction();
            boolean machine = located.getUserId() == 0;
            AdminUserDO user = null;
            if (!machine) {
                try {
                    user = adminUserService.lockUserForSessionMutation(tenantId, located.getUserId());
                } catch (ServiceException ex) {
                    if (!Objects.equals(ex.getCode(), USER_NOT_EXISTS.getCode())) {
                        throw ex;
                    }
                }
            }
            // 与撤销保持 user -> access -> refresh；锁前对象只提供身份，不提供可变令牌事实。
            // 机器没有人员锁，以完整 access 集合 ID 顺序与机器撤销统一首个锁域。
            List<OAuth2AccessTokenDO> lockedAccess = machine
                    ? oauth2AccessTokenMapper.selectListForUserRevocation(tenantId, 0L, UserTypeEnum.ADMIN.getValue())
                    : oauth2AccessTokenMapper.selectListForRefresh(tenantId, located.getUserId(),
                            UserTypeEnum.ADMIN.getValue(), located.getRefreshToken());
            List<OAuth2AccessTokenDO> accessTokens = lockedAccess.stream()
                    .filter(access -> Objects.equals(access.getRefreshToken(), located.getRefreshToken())).toList();
            if (machine) {
                oauth2RefreshTokenMapper.selectListForUserRevocation(tenantId, 0L, UserTypeEnum.ADMIN.getValue());
            }
            OAuth2RefreshTokenDO current = oauth2RefreshTokenMapper.selectCurrentForRefresh(tenantId,
                    located.getId(), located.getRefreshToken(), located.getUserId(), UserTypeEnum.ADMIN.getValue());
            if (current == null) {
                throw unauthorized("刷新令牌已撤销，请重新登录");
            }
            OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientId);
            if (!Objects.equals(current.getClientId(), clientId)) {
                throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "刷新令牌的客户端编号不正确");
            }
            boolean expired = DateUtils.isExpired(current.getExpiresTime());
            if (user != null && !expired) {
                validateActiveAdmin(user);
            }
            for (OAuth2AccessTokenDO access : accessTokens) {
                if (oauth2AccessTokenMapper.deleteById(access.getId()) != 1) {
                    throw new IllegalStateException("ADMIN refresh access deletion affected an unexpected row count");
                }
                oauth2AccessTokenRedisDAO.delete(access.getAccessToken());
            }
            if (expired || (!machine && user == null)) {
                if (oauth2RefreshTokenMapper.deleteById(current.getId()) != 1) {
                    throw new IllegalStateException("ADMIN refresh deletion affected an unexpected row count");
                }
                oauth2AccessTokenRedisDAO.delete(current.getRefreshToken());
                cleanupError.set(unauthorized(expired ? "刷新令牌已过期" : "刷新令牌对应的用户不存在"));
                return null;
            }
            return new AdminSessionToken(createOAuth2AccessToken(current, client,
                    machine ? Collections.emptyMap() : adminUserInfo(user)),
                    !machine && passwordChangeRequired(user));
        })));
        // 仅既有到期/已删除清理在成功提交之后暴露业务拒绝；其余异常在事务内部传播。
        if (cleanupError.get() != null) {
            throw cleanupError.get();
        }
        return result.get();
    }

    private AdminSessionToken issueAdminToken(AdminUserDO user, String clientId, List<String> scopes) {
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientId);
        OAuth2RefreshTokenDO refresh = new OAuth2RefreshTokenDO().setRefreshToken(generateRefreshToken())
                .setUserId(user.getId()).setUserType(UserTypeEnum.ADMIN.getValue()).setClientId(client.getClientId())
                .setScopes(scopes).setExpiresTime(LocalDateTime.now().plusSeconds(client.getRefreshTokenValiditySeconds()));
        refresh.setTenantId(Objects.requireNonNull(user.getTenantId(), "Locked ADMIN user tenant is required"));
        oauth2RefreshTokenMapper.insert(refresh);
        return new AdminSessionToken(createOAuth2AccessToken(refresh, client, adminUserInfo(user)),
                passwordChangeRequired(user));
    }

    private AdminUserDO lockActiveAdmin(Long tenantId, Long userId) {
        requireAdminId(userId, false);
        requireWritableTransaction();
        AdminUserDO user;
        try {
            user = adminUserService.lockUserForSessionMutation(tenantId, userId);
        } catch (ServiceException ex) {
            if (Objects.equals(ex.getCode(), USER_NOT_EXISTS.getCode())) {
                throw unauthorized("用户不存在");
            }
            throw ex;
        }
        validateActiveAdmin(user);
        return user;
    }

    private void validateActiveAdmin(AdminUserDO user) {
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            throw unauthorized("账号已禁用，请重新登录");
        }
        if (AdminUserServiceImpl.isLoginLockActive(user, LocalDateTime.now())) {
            throw unauthorized("账号已锁定，请重新登录");
        }
    }

    private void requireAdminId(Long userId, boolean allowMachine) {
        if (userId == null || userId < 0 || (!allowMachine && userId == 0)) {
            throw unauthorized("无效的后台用户身份");
        }
    }

    private void requireWritableTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException("ADMIN token mutation requires a writable Spring transaction");
        }
    }

    private Map<String, String> adminUserInfo(AdminUserDO user) {
        return MapUtil.builder(LoginUser.INFO_KEY_NICKNAME, user.getNickname())
                .put(LoginUser.INFO_KEY_DEPT_ID, StrUtil.toStringOrNull(user.getDeptId())).build();
    }

    private boolean passwordChangeRequired(AdminUserDO user) {
        return Objects.equals(user.getPasswordCredentialStatus(), "INITIAL")
                || Objects.equals(user.getPasswordCredentialStatus(), "RESET_REQUIRED");
    }

    private ServiceException unauthorized(String message) {
        return exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), message);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DataPermission(enable = false)
    public OAuth2AccessTokenDO getAccessToken(String accessToken) {
        return readOfficialAccessToken(accessToken);
    }

    private OAuth2AccessTokenDO readOfficialAccessToken(String accessToken) {
        OAuth2AccessTokenDO accessTokenDO = oauth2AccessTokenMapper.selectByAccessToken(accessToken);
        if (accessTokenDO == null) {
            // 保留正式 refresh-as-bearer；缓存不能建立令牌身份或授权。
            OAuth2RefreshTokenDO refreshTokenDO = oauth2RefreshTokenMapper.selectByRefreshToken(accessToken);
            if (refreshTokenDO == null) {
                return null;
            }
            accessTokenDO = BeanUtils.toBean(refreshTokenDO, OAuth2AccessTokenDO.class)
                    .setAccessToken(refreshTokenDO.getRefreshToken()).setUserInfo(Collections.emptyMap());
            validateOfficialTokenIdentity(accessTokenDO);
            if (DateUtils.isExpired(accessTokenDO.getExpiresTime())) {
                return null;
            }
        } else {
            validateOfficialTokenIdentity(accessTokenDO);
        }
        if (Objects.equals(accessTokenDO.getUserType(), UserTypeEnum.ADMIN.getValue())) {
            if (accessTokenDO.getUserId() > 0) {
                OAuth2AccessTokenDO official = accessTokenDO;
                TenantUtils.execute(official.getTenantId(), (Runnable) () -> {
                    AdminUserDO user = adminUserMapper.selectSessionSubject(official.getTenantId(), official.getUserId());
                    if (user == null) {
                        throw unauthorized("用户不存在");
                    }
                    if (!Objects.equals(user.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
                        throw unauthorized("账号已禁用，请重新登录");
                    }
                    if (AdminUserServiceImpl.isLoginLockActive(user, LocalDateTime.now())) {
                        throw unauthorized("账号已锁定，请重新登录");
                    }
                    official.setUserInfo(adminUserInfo(user));
                });
            } else {
                accessTokenDO.setUserInfo(Collections.emptyMap());
            }
        }
        if (!DateUtils.isExpired(accessTokenDO.getExpiresTime())) {
            oauth2AccessTokenRedisDAO.set(accessTokenDO);
        }
        return accessTokenDO;
    }

    private void validateOfficialTokenIdentity(OAuth2AccessTokenDO token) {
        if (token.getTenantId() == null || token.getUserId() == null || token.getUserId() < 0
                || token.getExpiresTime() == null
                || (!Objects.equals(token.getUserType(), UserTypeEnum.ADMIN.getValue())
                && !Objects.equals(token.getUserType(), UserTypeEnum.MEMBER.getValue()))) {
            throw unauthorized("令牌存储身份不完整或用户类型无效");
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DataPermission(enable = false)
    public OAuth2AccessTokenDO checkAccessToken(String accessToken) {
        OAuth2AccessTokenDO accessTokenDO = readOfficialAccessToken(accessToken);
        if (accessTokenDO == null) {
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌不存在");
        }
        if (DateUtils.isExpired(accessTokenDO.getExpiresTime())) {
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌已过期");
        }
        return accessTokenDO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO removeAccessToken(String accessToken) {
        // 删除访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2AccessTokenMapper.selectByAccessToken(accessToken);
        if (accessTokenDO == null) {
            return null;
        }
        oauth2AccessTokenMapper.deleteById(accessTokenDO.getId());
        oauth2AccessTokenRedisDAO.delete(accessToken);
        // 删除刷新令牌
        oauth2RefreshTokenMapper.deleteByRefreshToken(accessTokenDO.getRefreshToken());
        oauth2AccessTokenRedisDAO.delete(accessTokenDO.getRefreshToken());
        return accessTokenDO;
    }

    @Override
    public void removeAccessToken(Long userId, Integer userType) {
        if (Objects.equals(userType, UserTypeEnum.ADMIN.getValue())) {
            if (userId == null || userId < 0) {
                throw new IllegalArgumentException("ADMIN session revocation requires a non-negative user ID");
            }
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                if (!TransactionSynchronizationManager.isActualTransactionActive()
                        || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                    throw new IllegalStateException("ADMIN session revocation requires a writable Spring transaction");
                }
                // 人员先锁账号；0 仅属于已有机器协议，不查询假人员。
                if (userId > 0) {
                    adminUserService.lockUserForSessionMutation(tenantId, userId);
                }
                // 分别当前读全部正式令牌，孤立 refresh 不能依赖 access 被发现。
                List<OAuth2AccessTokenDO> accessTokens = oauth2AccessTokenMapper
                        .selectListForUserRevocation(tenantId, userId, userType);
                List<OAuth2RefreshTokenDO> refreshTokens = oauth2RefreshTokenMapper
                        .selectListForUserRevocation(tenantId, userId, userType);
                Set<String> cacheKeys = new LinkedHashSet<>();
                for (OAuth2AccessTokenDO token : accessTokens) {
                    cacheKeys.add(token.getAccessToken());
                    if (oauth2AccessTokenMapper.deleteById(token.getId()) != 1) {
                        throw new IllegalStateException("ADMIN access-token revocation affected an unexpected row count");
                    }
                }
                for (OAuth2RefreshTokenDO token : refreshTokens) {
                    cacheKeys.add(token.getRefreshToken());
                    if (oauth2RefreshTokenMapper.deleteById(token.getId()) != 1) {
                        throw new IllegalStateException("ADMIN refresh-token revocation affected an unexpected row count");
                    }
                }
                // Redis 失败原样传播并回滚 DB；此前已删除的缓存不恢复。
                cacheKeys.forEach(oauth2AccessTokenRedisDAO::delete);
            });
            return;
        }
        List<OAuth2AccessTokenDO> accessTokens = oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, userType);
        if (CollUtil.isEmpty(accessTokens)) {
            return;
        }
        accessTokens.forEach(accessToken -> {
            // 删除访问令牌
            oauth2AccessTokenMapper.deleteById(accessToken.getId());
            oauth2AccessTokenRedisDAO.delete(accessToken.getAccessToken());
            // 删除刷新令牌
            oauth2RefreshTokenMapper.deleteByRefreshToken(accessToken.getRefreshToken());
            oauth2AccessTokenRedisDAO.delete(accessToken.getRefreshToken());
        });
    }

    @Override
    public PageResult<OAuth2AccessTokenDO> getAccessTokenPage(OAuth2AccessTokenPageReqVO reqVO) {
        return oauth2AccessTokenMapper.selectPage(reqVO);
    }

    private OAuth2AccessTokenDO createOAuth2AccessToken(OAuth2RefreshTokenDO refreshTokenDO, OAuth2ClientDO clientDO) {
        return createOAuth2AccessToken(refreshTokenDO, clientDO,
                buildUserInfo(refreshTokenDO.getUserId(), refreshTokenDO.getUserType(), "用户不存在"));
    }

    private OAuth2AccessTokenDO createOAuth2AccessToken(OAuth2RefreshTokenDO refreshTokenDO, OAuth2ClientDO clientDO,
                                                        Map<String, String> userInfo) {
        OAuth2AccessTokenDO accessTokenDO = new OAuth2AccessTokenDO().setAccessToken(generateAccessToken())
                .setUserId(refreshTokenDO.getUserId()).setUserType(refreshTokenDO.getUserType())
                .setUserInfo(userInfo)
                .setClientId(clientDO.getClientId()).setScopes(refreshTokenDO.getScopes())
                .setRefreshToken(refreshTokenDO.getRefreshToken())
                .setExpiresTime(LocalDateTime.now().plusSeconds(clientDO.getAccessTokenValiditySeconds()));
        // 优先从 refreshToken 获取租户编号，避免 ThreadLocal 被污染时导致 tenantId 为 null
        // 可能关联的 issue：https://t.zsxq.com/JIi5G
        Long tenantId = refreshTokenDO.getTenantId();
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        accessTokenDO.setTenantId(tenantId);
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // 记录到 Redis 中
        oauth2AccessTokenRedisDAO.set(accessTokenDO);
        return accessTokenDO;
    }

    private OAuth2RefreshTokenDO createOAuth2RefreshToken(Long userId, Integer userType, OAuth2ClientDO clientDO, List<String> scopes) {
        OAuth2RefreshTokenDO refreshToken = new OAuth2RefreshTokenDO().setRefreshToken(generateRefreshToken())
                .setUserId(userId).setUserType(userType)
                .setClientId(clientDO.getClientId()).setScopes(scopes)
                .setExpiresTime(LocalDateTime.now().plusSeconds(clientDO.getRefreshTokenValiditySeconds()));
        oauth2RefreshTokenMapper.insert(refreshToken);
        return refreshToken;
    }

    /**
     * 加载用户信息，方便 {@link cn.iocoder.yudao.framework.security.core.LoginUser} 获取到昵称、部门等信息
     *
     * @param userId 用户编号
     * @param userType 用户类型
     * @return 用户信息
     */
    private Map<String, String> buildUserInfo(Long userId, Integer userType, String userNotExistsMessage) {
        if (userId == null || userId <= 0) {
            return Collections.emptyMap();
        }
        if (userType.equals(UserTypeEnum.ADMIN.getValue())) {
            AdminUserDO user = adminUserService.getUser(userId);
            if (user == null) {
                throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), userNotExistsMessage);
            }
            return MapUtil.builder(LoginUser.INFO_KEY_NICKNAME, user.getNickname())
                    .put(LoginUser.INFO_KEY_DEPT_ID, StrUtil.toStringOrNull(user.getDeptId())).build();
        } else if (userType.equals(UserTypeEnum.MEMBER.getValue())) {
            // 注意：目前 Member 暂时不读取，可以按需实现
            return Collections.emptyMap();
        }
        throw new IllegalArgumentException("未知用户类型：" + userType);
    }

    private static String generateAccessToken() {
        return IdUtil.fastSimpleUUID();
    }

    private static String generateRefreshToken() {
        return IdUtil.fastSimpleUUID();
    }

    @Override
    public Integer cleanRefreshToken(Integer exceedDay, Integer deleteLimit) {
        int count = 0;
        LocalDateTime expireDate = LocalDateTime.now().minusDays(exceedDay);
        // 循环删除，直到没有满足条件的数据
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = oauth2RefreshTokenMapper.deleteByExpiresTimeLt(expireDate, deleteLimit);
            count += deleteCount;
            // 达到删除预期条数，说明到底了
            if (deleteCount < deleteLimit) {
                break;
            }
        }
        return count;
    }

    @Override
    public Integer cleanAccessToken(Integer exceedDay, Integer deleteLimit) {
        int count = 0;
        LocalDateTime expireDate = LocalDateTime.now().minusDays(exceedDay);
        // 循环删除，直到没有满足条件的数据
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = oauth2AccessTokenMapper.deleteByExpiresTimeLt(expireDate, deleteLimit);
            count += deleteCount;
            // 达到删除预期条数，说明到底了
            if (deleteCount < deleteLimit) {
                break;
            }
        }
        return count;
    }
}
