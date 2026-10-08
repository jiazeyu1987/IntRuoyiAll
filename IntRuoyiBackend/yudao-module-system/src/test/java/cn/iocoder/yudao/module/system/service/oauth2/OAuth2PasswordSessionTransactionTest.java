package cn.iocoder.yudao.module.system.service.oauth2;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnoreAspect;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserPasswordHistoryDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserPasswordHistoryMapper;
import cn.iocoder.yudao.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.user.AdminUserServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_PASSWORD_REUSE_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

/**
 * UM-05 联合事务回归：真实外部 Spring 代理、正式用户/历史/令牌/关联 Mapper、H2 数据库。
 * 不在测试方法上开启事务，成功提交和失败回滚均由生产服务负责。
 * Redis DAO 是具有可观察删除副作用的替身；第 N 次删除失败不会恢复已经删除的缓存。
 * 客户端配置和外部服务仅为注入边界替身，不将本测试视为 Redis/MySQL/E2E 运行验证。
 * 不调用共同 getAccessToken 算法，MEMBER 仅用于证明 ADMIN 撤销不会删除其正式集合。
 */
@Import({AdminUserServiceImpl.class, OAuth2TokenServiceImpl.class,
        TransactionAutoConfiguration.class, OAuth2PasswordSessionTransactionTest.TransactionTestConfiguration.class})
public class OAuth2PasswordSessionTransactionTest extends BaseDbUnitTest {

    private static final long TENANT = 1L;
    private static final long OTHER_TENANT = 2L;
    private static final long USER = 1001L;
    private static final long OTHER_USER = 1002L;
    private static final String CURRENT_PASSWORD = "Current@2026";
    private static final String HISTORY_PASSWORD = "Earlier@2026";
    private static final String NEW_PASSWORD = "Changed@2026";
    private static final LocalDateTime OLD_TIME = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
    private static final List<String> SNAPSHOT_TABLES = List.of("system_users", "system_user_password_history",
            "system_oauth2_access_token", "system_oauth2_refresh_token", "system_user_post", "system_user_role");

    @MockitoSpyBean(name = "adminUserService") private AdminUserService userService;
    @Resource private OAuth2TokenService tokenService;
    @Resource private AdminUserMapper userMapper;
    @Resource private AdminUserPasswordHistoryMapper historyMapper;
    @Resource private OAuth2AccessTokenMapper accessMapper;
    @Resource private OAuth2RefreshTokenMapper refreshMapper;
    @Resource private UserPostMapper userPostMapper;
    @Resource private UserRoleMapper userRoleMapper;
    @Resource private PasswordEncoder passwordEncoder;
    @Resource private DataSource dataSource;
    @Resource private MybatisPlusInterceptor mybatisPlusInterceptor;
    @Resource private PlatformTransactionManager transactionManager;

    @MockitoBean private DeptService deptService;
    @MockitoBean private PostService postService;
    @MockitoBean private PermissionService permissionService;
    @MockitoBean private TenantService tenantService;
    @MockitoBean private ConfigApi configApi;
    @MockitoBean private OAuth2ClientService clientService;
    @MockitoBean private OAuth2AccessTokenRedisDAO redisDAO;

    private JdbcTemplate jdbc;
    private final Set<String> cachedTokens = ConcurrentHashMap.newKeySet();
    private final List<String> deletedCacheTokens = new CopyOnWriteArrayList<>();
    private final List<Object> deletionConnections = new CopyOnWriteArrayList<>();
    private int deleteAttempts;
    private int failAtDelete;
    private final IllegalStateException redisFailure = new IllegalStateException("UM-05 injected Redis delete failure");

    @BeforeEach
    void prepareBoundaries() {
        jdbc = new JdbcTemplate(dataSource);
        TenantContextHolder.setTenantId(TENANT);
        TenantContextHolder.setIgnore(false);
        cachedTokens.clear();
        deletedCacheTokens.clear();
        deletionConnections.clear();
        deleteAttempts = 0;
        failAtDelete = 0;
        doAnswer(invocation -> {
            deleteCachedToken(invocation.getArgument(0));
            return null;
        }).when(redisDAO).delete(anyString());
        doAnswer(invocation -> {
            Collection<String> tokens = invocation.getArgument(0);
            for (String token : tokens) {
                deleteCachedToken(token);
            }
            return null;
        }).when(redisDAO).deleteList(any());
        doAnswer(invocation -> {
            OAuth2AccessTokenDO token = invocation.getArgument(0);
            cachedTokens.add(token.getAccessToken());
            return null;
        }).when(redisDAO).set(any());
        when(clientService.validOAuthClientFromCache(anyString())).thenAnswer(invocation ->
                new OAuth2ClientDO().setClientId(invocation.getArgument(0))
                        .setAccessTokenValiditySeconds(3600).setRefreshTokenValiditySeconds(7200));
    }

    @ParameterizedTest
    @EnumSource(PasswordEntry.class)
    void passwordMutationCommitsAllClientAndOrphanRevocation(PasswordEntry entry) {
        seedUserAndRelations();
        seedAllSessions();
        DatabaseSnapshot before = snapshot();
        String oldHash = userMapper.selectById(USER).getPassword();
        assertProductionBoundaries();

        entry.change(userService, NEW_PASSWORD);

        assertPasswordCommitted(entry, NEW_PASSWORD, oldHash, before);
        assertTargetSessionsRevoked();
        assertProtectedSessionsUnchanged(before);
        assertSingleRealTransaction();
    }

    @ParameterizedTest
    @EnumSource(PasswordEntry.class)
    void passwordMutationRevokesWhenOnlyOrphanRefreshExists(PasswordEntry entry) {
        seedUserAndRelations();
        seedRefresh(TENANT, USER, UserTypeEnum.ADMIN.getValue(), "orphan-only", "client-orphan");
        DatabaseSnapshot before = snapshot();
        String oldHash = userMapper.selectById(USER).getPassword();
        assertProductionBoundaries();

        entry.change(userService, NEW_PASSWORD);

        assertPasswordCommitted(entry, NEW_PASSWORD, oldHash, before);
        assertEquals(0L, refreshMapper.selectCount());
        assertEquals(0L, accessMapper.selectCount());
        assertFalse(cachedTokens.contains("orphan-only"), "refresh-as-bearer cache must be removed too");
        assertSingleRealTransaction();
    }

    @ParameterizedTest
    @EnumSource(PasswordEntry.class)
    void secondRedisDeleteFailureRollsBackPasswordHistoryAndWholeTokenSet(PasswordEntry entry) {
        seedUserAndRelations();
        seedAllSessions();
        DatabaseSnapshot before = snapshot();
        Set<String> cacheBefore = Set.copyOf(cachedTokens);
        failAtDelete = 2;
        assertProductionBoundaries();

        assertSame(redisFailure, assertThrows(IllegalStateException.class,
                () -> entry.change(userService, NEW_PASSWORD)));

        assertEquals(before, snapshot(), "password/hash/time/status/history and every DB token must roll back");
        assertSingleRealTransaction();
        assertEquals(2, deleteAttempts);
        assertEquals(1, deletedCacheTokens.size());
        Set<String> expectedCache = new LinkedHashSet<>(cacheBefore);
        expectedCache.removeAll(deletedCacheTokens);
        assertEquals(expectedCache, cachedTokens, "DB rollback does not claim restoration of deleted Redis entries");
        assertProtectedCachePresent();
    }

    @Test
    void standaloneUserRevocationUsesItsOwnTransactionAndRollsBackOnRedisFailure() {
        seedUserAndRelations();
        seedAllSessions();
        DatabaseSnapshot before = snapshot();
        failAtDelete = 2;
        assertProductionBoundaries();

        assertSame(redisFailure, assertThrows(IllegalStateException.class,
                () -> tokenService.removeAccessToken(USER, UserTypeEnum.ADMIN.getValue())));

        assertEquals(before, snapshot());
        assertSingleRealTransaction();
        assertEquals(1, deletedCacheTokens.size());
        assertFalse(cachedTokens.contains(deletedCacheTokens.get(0)));
    }

    @Test
    void selfChangeRejectsCurrentAndHistoryPasswordsWithoutAnyMutation() {
        seedUserAndRelations();
        seedAllSessions();
        DatabaseSnapshot before = snapshot();
        Set<String> cacheBefore = Set.copyOf(cachedTokens);

        assertServiceException(() -> PasswordEntry.SELF_CHANGE.change(userService, CURRENT_PASSWORD),
                USER_PASSWORD_REUSE_FORBIDDEN);
        assertEquals(before, snapshot());
        assertServiceException(() -> PasswordEntry.SELF_CHANGE.change(userService, HISTORY_PASSWORD),
                USER_PASSWORD_REUSE_FORBIDDEN);
        assertEquals(before, snapshot());
        assertEquals(cacheBefore, cachedTokens);
        assertEquals(0, deleteAttempts);
    }

    @Test
    void administratorResetRetainsExistingReusePolicyAndStillRevokesSessions() {
        seedUserAndRelations();
        seedAllSessions();
        DatabaseSnapshot before = snapshot();
        String oldHash = userMapper.selectById(USER).getPassword();

        PasswordEntry.ADMIN_RESET.change(userService, HISTORY_PASSWORD);

        assertPasswordCommitted(PasswordEntry.ADMIN_RESET, HISTORY_PASSWORD, oldHash, before);
        assertTargetSessionsRevoked();
        assertProtectedSessionsUnchanged(before);
        assertSingleRealTransaction();
    }

    @Test
    void currentPasswordHistoryReadKeepsLatestFiveAndExplicitOwnerWhenTenantIgnored() {
        seedUserAndRelations();
        List<Long> ids = new java.util.ArrayList<>();
        for (int index = 0; index < 6; index++) {
            ids.add(insertHistoryForCurrentRead(TENANT, USER, OLD_TIME.plusSeconds(index / 2)));
        }
        insertHistoryForCurrentRead(OTHER_TENANT, USER, OLD_TIME.plusYears(1));
        insertHistoryForCurrentRead(TENANT, OTHER_USER, OLD_TIME.plusYears(1));
        Long deleted = insertHistoryForCurrentRead(TENANT, USER, OLD_TIME.plusYears(1));
        assertEquals(1, historyMapper.deleteById(deleted));

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Boolean previousIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setIgnore(true);
            transaction.executeWithoutResult(status -> {
                userService.lockUserForSessionMutation(TENANT, USER);
                List<AdminUserPasswordHistoryDO> current = historyMapper.selectLatestListForUpdate(TENANT, USER, 5);
                assertEquals(List.of(ids.get(5), ids.get(4), ids.get(3), ids.get(2), ids.get(1)),
                        current.stream().map(AdminUserPasswordHistoryDO::getId).toList());
                assertTrue(current.stream().allMatch(row -> row.getTenantId().equals(TENANT)
                        && row.getUserId().equals(USER) && !row.getDeleted()));
            });
        } finally {
            TenantContextHolder.setIgnore(previousIgnore);
        }
    }

    @Test
    void selfChangeAfterCommittedResetRejectsNewlyRecordedHistoryWithoutFurtherMutation() throws Exception {
        seedUserAndRelations();
        seedAllSessions();
        CountDownLatch historyReadBeforeReset = new CountDownLatch(1);
        CountDownLatch resetCommitted = new CountDownLatch(1);
        ExecutorService workers = sequenceWorkers();
        try {
            Future<ServiceException> selfChange = workers.submit(() -> inTenantResult(() -> {
                // No outer transaction here: this H2 case proves commit ordering, not MySQL RR semantics.
                assertEquals(1, historyMapper.selectLatestListByUserId(USER, 5).size());
                historyReadBeforeReset.countDown();
                awaitLatch(resetCommitted);
                DatabaseSnapshot afterReset = snapshot();
                Set<String> cacheAfterReset = Set.copyOf(cachedTokens);
                TransactionTemplate outer = new TransactionTemplate(transactionManager);
                UserProfileUpdatePasswordReqVO request = new UserProfileUpdatePasswordReqVO();
                request.setOldPassword(NEW_PASSWORD);
                request.setNewPassword(CURRENT_PASSWORD);
                ServiceException error = assertThrows(ServiceException.class, () -> outer.executeWithoutResult(
                        status -> userService.updateUserPassword(USER, request)));
                assertEquals(afterReset, snapshot());
                assertEquals(cacheAfterReset, cachedTokens);
                return error;
            }));
            Future<?> reset = workers.submit(() -> inTenant(TENANT, () -> {
                awaitLatch(historyReadBeforeReset);
                PasswordEntry.ADMIN_RESET.change(userService, NEW_PASSWORD);
                resetCommitted.countDown();
            }));
            reset.get(15, TimeUnit.SECONDS);
            assertEquals(USER_PASSWORD_REUSE_FORBIDDEN.getCode(), selfChange.get(15, TimeUnit.SECONDS).getCode());
            assertTrue(passwordEncoder.matches(NEW_PASSWORD, userMapper.selectById(USER).getPassword()));
            assertEquals("RESET_REQUIRED", userMapper.selectById(USER).getPasswordCredentialStatus());
            assertNoTargetTokens();
        } finally {
            resetCommitted.countDown();
            stopWorkers(workers);
        }
    }

    private Long insertHistoryForCurrentRead(long tenantId, long userId, LocalDateTime changedAt) {
        AdminUserPasswordHistoryDO row = new AdminUserPasswordHistoryDO().setUserId(userId)
                .setPasswordHash(passwordEncoder.encode(HISTORY_PASSWORD)).setChangedAt(changedAt)
                .setSourceType("SELF_CHANGE");
        row.setTenantId(tenantId);
        inTenant(tenantId, () -> assertEquals(1, historyMapper.insert(row)));
        return row.getId();
    }

    @ParameterizedTest
    @ValueSource(strings = {"member-refresh", "other-tenant-refresh"})
    void dirtyAccessRefreshAssociationDoesNotRevokeAnotherOwnersCache(String protectedRefreshToken) {
        seedUserAndRelations();
        seedAllSessions();
        OAuth2AccessTokenDO dirtyAccess = accessMapper.selectByAccessToken("target-a");
        assertNotNull(dirtyAccess);
        dirtyAccess.setRefreshToken(protectedRefreshToken);
        accessMapper.updateById(dirtyAccess);
        DatabaseSnapshot before = snapshot();
        String oldHash = userMapper.selectById(USER).getPassword();
        assertProductionBoundaries();

        PasswordEntry.ADMIN_RESET.change(userService, NEW_PASSWORD);

        assertPasswordCommitted(PasswordEntry.ADMIN_RESET, NEW_PASSWORD, oldHash, before);
        assertTargetSessionsRevoked();
        assertProtectedSessionsUnchanged(before);
        assertTrue(cachedTokens.contains(protectedRefreshToken),
                "an access association cannot establish ownership of a different owner's refresh cache");
        assertFalse(deletedCacheTokens.contains(protectedRefreshToken));
        assertSingleRealTransaction();
    }

    @Test
    void authenticatedSnapshotCannotMintAfterConcurrentReset() throws Exception {
        seedUserAndRelations();
        setFreshPasswordTime();
        CountDownLatch authenticated = new CountDownLatch(1);
        CountDownLatch resetDone = new CountDownLatch(1);
        ExecutorService workers = sequenceWorkers();
        try {
            Future<ServiceException> login = workers.submit(() -> inTenantResult(() -> {
                AdminPasswordAuthenticationSnapshot snapshot = AdminPasswordAuthenticationSnapshot.from(userMapper.selectById(USER));
                authenticated.countDown();
                awaitLatch(resetDone);
                return assertThrows(ServiceException.class,
                        () -> tokenService.createPasswordAccessToken(snapshot, "client-a", List.of("read")));
            }));
            Future<?> reset = workers.submit(() -> inTenant(TENANT, () -> {
                awaitLatch(authenticated);
                PasswordEntry.ADMIN_RESET.change(userService, NEW_PASSWORD);
                resetDone.countDown();
            }));
            reset.get(15, TimeUnit.SECONDS);
            assertEquals(401, login.get(15, TimeUnit.SECONDS).getCode());
            assertNoTargetTokens();
            assertEquals(3, userMapper.selectById(USER).getLoginFailureCount(), "stale authentication must not clear failures");
        } finally {
            resetDone.countDown();
            stopWorkers(workers);
        }
    }

    @Test
    void resetAfterPasswordIssuanceRevokesTheNewlyCommittedSession() throws Exception {
        assertMintThenReset(false);
    }

    @Test
    void refreshLocatedBeforeResetCannotUseLockBeforeObjectAfterReset() throws Exception {
        seedUserAndRelations();
        seedSession(TENANT, USER, UserTypeEnum.ADMIN.getValue(), "old-refresh-access", "client-a");
        CountDownLatch refreshReachedUserLock = new CountDownLatch(1);
        CountDownLatch resetDone = new CountDownLatch(1);
        // Spy delegates to the actual service: only the test worker pauses before acquiring the real user row lock.
        doAnswer(invocation -> {
            if (Thread.currentThread().getName().equals("um05-first")) {
                refreshReachedUserLock.countDown();
                awaitLatch(resetDone);
            }
            return invocation.callRealMethod();
        }).when(userService).lockUserForSessionMutation(eq(TENANT), eq(USER));
        ExecutorService workers = sequenceWorkers();
        try {
            Future<ServiceException> refresh = workers.submit(() -> inTenantResult(() ->
                    assertThrows(ServiceException.class, () -> tokenService.refreshAdminAccessToken("old-refresh-access-refresh", "client-a"))));
            Future<?> reset = workers.submit(() -> inTenant(TENANT, () -> {
                awaitLatch(refreshReachedUserLock);
                PasswordEntry.ADMIN_RESET.change(userService, NEW_PASSWORD);
                resetDone.countDown();
            }));
            reset.get(15, TimeUnit.SECONDS);
            assertEquals(401, refresh.get(15, TimeUnit.SECONDS).getCode());
            assertNoTargetTokens();
            assertFalse(cachedTokens.contains("old-refresh-access"));
        } finally {
            resetDone.countDown();
            stopWorkers(workers);
        }
    }

    @Test
    void resetAfterRefreshRevokesTheNewlyCommittedSession() throws Exception {
        assertMintThenReset(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INITIAL", "RESET_REQUIRED", "ACTIVE"})
    void adminPasswordIssuanceUsesLockedCredentialFlagAndClearsFailuresOnlyOnSuccess(String credentialStatus) {
        seedUserAndRelations();
        userMapper.updateById(new AdminUserDO().setId(USER).setPasswordCredentialStatus(credentialStatus));
        setFreshPasswordTime();
        AdminPasswordAuthenticationSnapshot snapshot = AdminPasswordAuthenticationSnapshot.from(userMapper.selectById(USER));
        AdminSessionToken issued = tokenService.createPasswordAccessToken(snapshot, "client-a", List.of("read"));
        assertEquals(!"ACTIVE".equals(credentialStatus), issued.isPasswordChangeRequired());
        assertEquals(0, userMapper.selectById(USER).getLoginFailureCount());
        assertNotNull(accessMapper.selectByAccessToken(issued.getAccessToken().getAccessToken()));
        assertEquals(TENANT, issued.getAccessToken().getTenantId());
        assertEquals(List.of("read"), issued.getAccessToken().getScopes());
    }

    @Test
    void genericAdminCreationDoesNotClearLoginFailures() {
        seedUserAndRelations();
        OAuth2AccessTokenDO issued = tokenService.createAccessToken(USER, UserTypeEnum.ADMIN.getValue(), "client-a", List.of("read"));
        assertNotNull(accessMapper.selectByAccessToken(issued.getAccessToken()));
        assertEquals(3, userMapper.selectById(USER).getLoginFailureCount());
    }

    @Test
    void passwordExpiryIsRecheckedWithinUserLockWithoutClearingFailures() {
        seedUserAndRelations();
        AdminPasswordAuthenticationSnapshot snapshot = AdminPasswordAuthenticationSnapshot.from(userMapper.selectById(USER));
        assertEquals(401, assertThrows(ServiceException.class,
                () -> tokenService.createPasswordAccessToken(snapshot, "client-a", List.of("read"))).getCode());
        assertEquals(3, userMapper.selectById(USER).getLoginFailureCount());
        assertNoTargetTokens();
    }

    @Test
    void mobileIssuanceRejectsChangedBindingThenUsesCurrentCredentialFlag() {
        seedUserAndRelations();
        userMapper.updateById(new AdminUserDO().setId(USER).setMobile("13900001111"));
        assertEquals(401, assertThrows(ServiceException.class, () -> tokenService.createMobileAccessToken(
                USER, "13800001111", "client-a", List.of("read"))).getCode());
        assertEquals(3, userMapper.selectById(USER).getLoginFailureCount());
        assertNoTargetTokens();
        AdminSessionToken issued = tokenService.createMobileAccessToken(USER, "13900001111", "client-a", List.of("read"));
        assertTrue(issued.isPasswordChangeRequired());
        assertEquals(0, userMapper.selectById(USER).getLoginFailureCount());
    }

    @Test
    void machineRefreshUsesStoredTenantAndPreservesOtherMachineSession() {
        seedSession(TENANT, 0L, UserTypeEnum.ADMIN.getValue(), "machine-a", "client-a");
        seedSession(TENANT, 0L, UserTypeEnum.ADMIN.getValue(), "machine-b", "client-b");
        TenantContextHolder.setTenantId(OTHER_TENANT);
        TenantContextHolder.setIgnore(true);
        OAuth2AccessTokenDO issued = tokenService.refreshAccessToken("machine-a-refresh", "client-a");
        assertEquals(TENANT, issued.getTenantId());
        assertTrue(issued.getUserInfo().isEmpty());
        assertEquals(OTHER_TENANT, TenantContextHolder.getTenantId());
        assertTrue(TenantContextHolder.isIgnore());
        assertNotNull(accessMapper.selectByAccessToken("machine-b"));
        assertFalse(cachedTokens.contains("machine-a"));
        assertTrue(cachedTokens.contains("machine-b"));
    }

    @Test
    void lockedAdminRefreshRejectsWithoutDeletingRowsOrClearingFailures() {
        seedUserAndRelations();
        seedSession(TENANT, USER, UserTypeEnum.ADMIN.getValue(), "locked-refresh", "client-a");
        userMapper.updateById(new AdminUserDO().setId(USER).setLoginLockedTime(LocalDateTime.now()));
        DatabaseSnapshot before = snapshot();
        assertEquals(401, assertThrows(ServiceException.class,
                () -> tokenService.refreshAdminAccessToken("locked-refresh-refresh", "client-a")).getCode());
        assertEquals(before, snapshot());
        assertTrue(cachedTokens.contains("locked-refresh"));
        assertTrue(cachedTokens.contains("locked-refresh-refresh"));
        assertEquals(0, deleteAttempts);
    }

    @Test
    void adminIssuanceRejectsDisabledAndActivelyLockedUsersWithoutClearingFailuresOrMinting() {
        seedUserAndRelations();
        userMapper.updateById(new AdminUserDO().setId(USER).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        assertEquals(401, assertThrows(ServiceException.class,
                () -> tokenService.createAdminAccessToken(USER, "client-a", List.of("read"))).getCode());
        userMapper.updateById(new AdminUserDO().setId(USER).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setLoginLockedTime(LocalDateTime.now()));
        assertEquals(401, assertThrows(ServiceException.class,
                () -> tokenService.createAccessToken(USER, UserTypeEnum.ADMIN.getValue(), "client-a", List.of("read"))).getCode());
        assertEquals(3, userMapper.selectById(USER).getLoginFailureCount());
        assertNoTargetTokens();
    }

    @Test
    void expiredAdminRefreshCommitsExplicitCleanupAndRestoresTenantContext() {
        seedUserAndRelations();
        seedSession(TENANT, USER, UserTypeEnum.ADMIN.getValue(), "expired", "client-a");
        OAuth2RefreshTokenDO refresh = refreshMapper.selectByRefreshToken("expired-refresh");
        refresh.setExpiresTime(LocalDateTime.now().minusDays(1));
        refreshMapper.updateById(refresh);
        TenantContextHolder.setTenantId(OTHER_TENANT);
        TenantContextHolder.setIgnore(true);
        assertEquals(401, assertThrows(ServiceException.class,
                () -> tokenService.refreshAdminAccessToken("expired-refresh", "client-a")).getCode());
        assertEquals(OTHER_TENANT, TenantContextHolder.getTenantId());
        assertTrue(TenantContextHolder.isIgnore());
        assertNoTargetTokens();
        assertFalse(cachedTokens.contains("expired"));
        assertFalse(cachedTokens.contains("expired-refresh"));
    }

    @Test
    void deletedAdminRefreshCommitsExplicitCleanup() {
        seedUserAndRelations();
        seedSession(TENANT, USER, UserTypeEnum.ADMIN.getValue(), "deleted-user", "client-a");
        userMapper.deleteById(USER);
        assertEquals(401, assertThrows(ServiceException.class,
                () -> tokenService.refreshAdminAccessToken("deleted-user-refresh", "client-a")).getCode());
        assertNoTargetTokens();
        assertFalse(cachedTokens.contains("deleted-user"));
        assertFalse(cachedTokens.contains("deleted-user-refresh"));
    }

    @Test
    void refreshRedisFailureRollsBackOfficialRowsAndKeepsOriginalError() {
        seedUserAndRelations();
        seedSession(TENANT, USER, UserTypeEnum.ADMIN.getValue(), "refresh-failure", "client-a");
        DatabaseSnapshot before = snapshot();
        failAtDelete = 1;
        TenantContextHolder.setTenantId(OTHER_TENANT);
        TenantContextHolder.setIgnore(true);
        assertSame(redisFailure, assertThrows(IllegalStateException.class,
                () -> tokenService.refreshAdminAccessToken("refresh-failure-refresh", "client-a")));
        assertEquals(before, snapshot());
        assertEquals(OTHER_TENANT, TenantContextHolder.getTenantId());
        assertTrue(TenantContextHolder.isIgnore());
    }

    private void assertMintThenReset(boolean refresh) throws Exception {
        seedUserAndRelations();
        setFreshPasswordTime();
        if (refresh) {
            seedSession(TENANT, USER, UserTypeEnum.ADMIN.getValue(), "before-refresh", "client-a");
        }
        AdminPasswordAuthenticationSnapshot snapshot = AdminPasswordAuthenticationSnapshot.from(userMapper.selectById(USER));
        CountDownLatch mintHoldingUserLock = new CountDownLatch(1);
        CountDownLatch resetReachedUserLock = new CountDownLatch(1);
        doAnswer(invocation -> {
            if (Thread.currentThread().getName().equals("um05-second")) {
                resetReachedUserLock.countDown();
            }
            return invocation.callRealMethod();
        }).when(userService).lockUserForSessionMutation(eq(TENANT), eq(USER));
        when(clientService.validOAuthClientFromCache("client-a")).thenAnswer(invocation -> {
            if (Thread.currentThread().getName().equals("um05-first")) {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                mintHoldingUserLock.countDown();
                awaitLatch(resetReachedUserLock);
            }
            return new OAuth2ClientDO().setClientId("client-a")
                    .setAccessTokenValiditySeconds(3600).setRefreshTokenValiditySeconds(7200);
        });
        ExecutorService workers = sequenceWorkers();
        try {
            Future<AdminSessionToken> mint = workers.submit(() -> inTenantResult(() -> refresh
                    ? tokenService.refreshAdminAccessToken("before-refresh-refresh", "client-a")
                    : tokenService.createPasswordAccessToken(snapshot, "client-a", List.of("read"))));
            Future<?> reset = workers.submit(() -> inTenant(TENANT, () -> {
                awaitLatch(mintHoldingUserLock);
                PasswordEntry.ADMIN_RESET.change(userService, NEW_PASSWORD);
            }));
            AdminSessionToken issued = mint.get(15, TimeUnit.SECONDS);
            reset.get(15, TimeUnit.SECONDS);
            assertTrue(issued.isPasswordChangeRequired());
            assertNoTargetTokens();
            assertFalse(cachedTokens.contains(issued.getAccessToken().getAccessToken()));
            assertFalse(cachedTokens.contains(issued.getAccessToken().getRefreshToken()));
            assertEquals("RESET_REQUIRED", userMapper.selectById(USER).getPasswordCredentialStatus());
        } finally {
            resetReachedUserLock.countDown();
            mintHoldingUserLock.countDown();
            stopWorkers(workers);
        }
    }

    private ExecutorService sequenceWorkers() {
        AtomicInteger number = new AtomicInteger();
        return Executors.newFixedThreadPool(2, action -> new Thread(action,
                number.incrementAndGet() == 1 ? "um05-first" : "um05-second"));
    }

    private <T> T inTenantResult(java.util.concurrent.Callable<T> operation) throws Exception {
        TenantContextHolder.setTenantId(TENANT);
        TenantContextHolder.setIgnore(false);
        try {
            return operation.call();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "deterministic test ordering did not reach its boundary");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while arranging test ordering", ex);
        }
    }

    private void stopWorkers(ExecutorService workers) throws InterruptedException {
        workers.shutdownNow();
        assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS), "test workers must terminate");
    }

    private void assertNoTargetTokens() {
        for (String table : List.of("system_oauth2_access_token", "system_oauth2_refresh_token")) {
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table
                    + " WHERE tenant_id = ? AND user_id = ? AND user_type = ? AND deleted = false",
                    Integer.class, TENANT, USER, UserTypeEnum.ADMIN.getValue()));
        }
    }

    private void setFreshPasswordTime() {
        userMapper.updateById(new AdminUserDO().setId(USER).setPasswordUpdateTime(LocalDateTime.now().minusDays(1)));
    }

    private void seedUserAndRelations() {
        AdminUserDO user = new AdminUserDO().setId(USER).setUsername("um05-transaction")
                .setCanonicalUsername("um05-transaction").setNickname("UM05 transaction user")
                .setPassword(passwordEncoder.encode(CURRENT_PASSWORD)).setPasswordUpdateTime(OLD_TIME)
                .setPasswordCredentialStatus("INITIAL").setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setDeptId(501L).setPostIds(Set.of(601L, 602L)).setEmail("um05@example.invalid")
                .setLoginFailureCount(3).setLoginFailureWindowStartTime(OLD_TIME)
                .setLoginLocked(1).setLoginLockedTime(OLD_TIME)
                .setLifecycleDocumentType("TRANSFER").setLifecycleDocumentNo("UM05-TX-TRANSFER")
                .setLifecycleDocumentTime(OLD_TIME).setLifecycleEffectiveTime(LocalDateTime.now().plusYears(1));
        user.setTenantId(TENANT);
        userMapper.insert(user);
        AdminUserPasswordHistoryDO history = new AdminUserPasswordHistoryDO().setUserId(USER)
                .setPasswordHash(passwordEncoder.encode(HISTORY_PASSWORD)).setChangedAt(OLD_TIME.minusDays(1))
                .setSourceType("SELF_CHANGE");
        history.setTenantId(TENANT);
        historyMapper.insert(history);
        userPostMapper.insert(new UserPostDO().setUserId(USER).setPostId(601L));
        userPostMapper.insert(new UserPostDO().setUserId(USER).setPostId(602L));
        userRoleMapper.insert(new UserRoleDO().setTenantId(TENANT).setUserId(USER).setRoleId(701L));
        userRoleMapper.insert(new UserRoleDO().setTenantId(TENANT).setUserId(USER).setRoleId(702L));
    }

    private void seedAllSessions() {
        seedSession(TENANT, USER, UserTypeEnum.ADMIN.getValue(), "target-a", "client-a");
        seedSession(TENANT, USER, UserTypeEnum.ADMIN.getValue(), "target-b", "client-b");
        seedRefresh(TENANT, USER, UserTypeEnum.ADMIN.getValue(), "target-orphan", "client-c");
        seedSession(TENANT, OTHER_USER, UserTypeEnum.ADMIN.getValue(), "other-user", "client-a");
        seedSession(OTHER_TENANT, USER, UserTypeEnum.ADMIN.getValue(), "other-tenant", "client-a");
        seedRefresh(OTHER_TENANT, USER, UserTypeEnum.ADMIN.getValue(), "other-tenant-orphan", "client-c");
        seedSession(TENANT, USER, UserTypeEnum.MEMBER.getValue(), "member", "client-a");
        seedRefresh(TENANT, USER, UserTypeEnum.MEMBER.getValue(), "member-orphan", "client-c");
    }

    private void seedSession(long tenantId, long userId, int userType, String token, String client) {
        String refresh = token + "-refresh";
        seedRefresh(tenantId, userId, userType, refresh, client);
        inTenant(tenantId, () -> {
            OAuth2AccessTokenDO row = new OAuth2AccessTokenDO().setUserId(userId).setUserType(userType)
                    .setAccessToken(token).setRefreshToken(refresh).setClientId(client)
                    .setScopes(List.of("read", "write")).setUserInfo(Map.of("nickname", "fixture"))
                    .setExpiresTime(LocalDateTime.now().plusDays(1));
            row.setTenantId(tenantId);
            accessMapper.insert(row);
        });
        cachedTokens.add(token);
    }

    private void seedRefresh(long tenantId, long userId, int userType, String token, String client) {
        inTenant(tenantId, () -> {
            OAuth2RefreshTokenDO row = new OAuth2RefreshTokenDO().setUserId(userId).setUserType(userType)
                    .setRefreshToken(token).setClientId(client).setScopes(List.of("read", "write"))
                    .setExpiresTime(LocalDateTime.now().plusDays(1));
            row.setTenantId(tenantId);
            refreshMapper.insert(row);
        });
        // getAccessToken historically caches refresh-as-bearer under the refresh string itself.
        cachedTokens.add(token);
    }

    private void assertPasswordCommitted(PasswordEntry entry, String password, String oldHash, DatabaseSnapshot before) {
        AdminUserDO actual = userMapper.selectById(USER);
        assertTrue(passwordEncoder.matches(password, actual.getPassword()));
        assertTrue(actual.getPasswordUpdateTime().isAfter(OLD_TIME));
        assertEquals(entry.credentialStatus, actual.getPasswordCredentialStatus());
        List<AdminUserPasswordHistoryDO> history = historyMapper.selectLatestListByUserId(USER, 10);
        assertEquals(2, history.size());
        assertEquals(oldHash, history.get(0).getPasswordHash());
        assertEquals(entry.name(), history.get(0).getSourceType());
        assertEquals(TENANT, history.get(0).getTenantId());
        DatabaseSnapshot after = snapshot();
        assertEquals(withoutCredentialFields(before.rows.get("system_users").get(0)),
                withoutCredentialFields(after.rows.get("system_users").get(0)),
                "password mutation must retain account identity, department, lifecycle and login lock facts");
        assertEquals(before.rows.get("system_user_post"), after.rows.get("system_user_post"));
        assertEquals(before.rows.get("system_user_role"), after.rows.get("system_user_role"));
    }

    private void assertTargetSessionsRevoked() {
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM system_oauth2_access_token "
                + "WHERE tenant_id = ? AND user_id = ? AND user_type = ? AND deleted = false",
                Integer.class, TENANT, USER, UserTypeEnum.ADMIN.getValue()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM system_oauth2_refresh_token "
                + "WHERE tenant_id = ? AND user_id = ? AND user_type = ? AND deleted = false",
                Integer.class, TENANT, USER, UserTypeEnum.ADMIN.getValue()));
        for (String token : List.of("target-a", "target-a-refresh", "target-b", "target-b-refresh", "target-orphan")) {
            assertFalse(cachedTokens.contains(token), "must revoke cache key " + token);
        }
        assertProtectedCachePresent();
    }

    private void assertProtectedSessionsUnchanged(DatabaseSnapshot before) {
        DatabaseSnapshot after = snapshot();
        for (String table : List.of("system_oauth2_access_token", "system_oauth2_refresh_token")) {
            List<Map<String, Object>> expected = before.rows.get(table).stream().filter(row -> !isTarget(row)).toList();
            List<Map<String, Object>> actual = after.rows.get(table).stream().filter(row -> !isTarget(row)).toList();
            assertEquals(expected, actual, "other tenant/user/MEMBER rows must remain byte-for-byte unchanged: " + table);
        }
    }

    private boolean isTarget(Map<String, Object> row) {
        return ((Number) row.get("tenant_id")).longValue() == TENANT
                && ((Number) row.get("user_id")).longValue() == USER
                && ((Number) row.get("user_type")).intValue() == UserTypeEnum.ADMIN.getValue();
    }

    private void assertProtectedCachePresent() {
        assertTrue(cachedTokens.containsAll(List.of("other-user", "other-user-refresh", "other-tenant",
                "other-tenant-refresh", "other-tenant-orphan", "member", "member-refresh", "member-orphan")));
    }

    private void assertProductionBoundaries() {
        assertTrue(mybatisPlusInterceptor.getInterceptors().stream()
                .anyMatch(TenantLineInnerInterceptor.class::isInstance),
                "the production tenant SQL interceptor must be initialized");
        assertTrue(AopUtils.isAopProxy(userService), "password service must be an external Spring proxy");
        assertTrue(AopUtils.isAopProxy(tokenService), "token service must be an external Spring proxy");
        assertEquals(AdminUserServiceImpl.class, AopUtils.getTargetClass(userService));
        assertEquals(OAuth2TokenServiceImpl.class, AopUtils.getTargetClass(tokenService));
        for (Object tested : List.of(userService, tokenService, userMapper, historyMapper, accessMapper, refreshMapper)) {
            assertTrue(!mockingDetails(tested).isMock() || mockingDetails(tested).isSpy(),
                    "production service/Mapper must be real or a delegating spy, never a substituted mock");
        }
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "test must not supply a transaction");
    }

    private void deleteCachedToken(String token) {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive(), "cache deletion must run inside real transaction");
        Object resource = TransactionSynchronizationManager.getResource(dataSource);
        assertNotNull(resource, "the H2 transaction must hold a datasource connection");
        deletionConnections.add(resource);
        deleteAttempts++;
        if (deleteAttempts == failAtDelete) {
            throw redisFailure;
        }
        cachedTokens.remove(token);
        deletedCacheTokens.add(token);
    }

    private void assertSingleRealTransaction() {
        assertFalse(deletionConnections.isEmpty(), "real revocation must reach Redis boundary");
        Object connection = deletionConnections.get(0);
        deletionConnections.forEach(actual -> assertSame(connection, actual, "all deletes must join the same DB transaction"));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "transaction must finish at external service return");
    }

    private DatabaseSnapshot snapshot() {
        Map<String, List<Map<String, Object>>> rows = new LinkedHashMap<>();
        for (String table : SNAPSHOT_TABLES) {
            rows.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id"));
        }
        return new DatabaseSnapshot(rows);
    }

    private Map<String, Object> withoutCredentialFields(Map<String, Object> row) {
        Map<String, Object> fields = new LinkedHashMap<>(row);
        List.of("password", "password_update_time", "password_credential_status", "update_time", "updater")
                .forEach(fields::remove);
        return fields;
    }

    private void inTenant(long tenantId, Runnable action) {
        Long previousTenant = TenantContextHolder.getTenantId();
        Boolean previousIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setIgnore(false);
            action.run();
        } finally {
            TenantContextHolder.setTenantId(previousTenant);
            TenantContextHolder.setIgnore(previousIgnore);
        }
    }

    private record DatabaseSnapshot(Map<String, List<Map<String, Object>>> rows) { }

    enum PasswordEntry {
        ADMIN_RESET("RESET_REQUIRED"), SELF_CHANGE("ACTIVE");

        private final String credentialStatus;

        PasswordEntry(String credentialStatus) {
            this.credentialStatus = credentialStatus;
        }

        void change(AdminUserService service, String password) {
            if (this == ADMIN_RESET) {
                service.updateUserPassword(USER, password);
            } else {
                UserProfileUpdatePasswordReqVO request = new UserProfileUpdatePasswordReqVO();
                request.setOldPassword(CURRENT_PASSWORD);
                request.setNewPassword(password);
                service.updateUserPassword(USER, request);
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TransactionTestConfiguration {

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4);
        }

        @Bean
        TenantIgnoreAspect tenantIgnoreAspect() {
            return new TenantIgnoreAspect();
        }

        @Bean
        @Lazy(false)
        InitializingBean tenantInterceptor(MybatisPlusInterceptor interceptor) {
            // Reuse production tenant SQL handler without unrelated web/Redis auto-configuration.
            return () -> MyBatisUtils.addInterceptor(interceptor,
                    new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties())), 0);
        }
    }
}
