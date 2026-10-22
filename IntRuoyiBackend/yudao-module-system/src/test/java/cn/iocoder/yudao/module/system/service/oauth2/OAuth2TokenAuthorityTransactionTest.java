package cn.iocoder.yudao.module.system.service.oauth2;

import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.datapermission.config.YudaoDataPermissionAutoConfiguration;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionAnnotationAdvisor;
import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionAnnotationInterceptor;
import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.datapermission.core.db.DataPermissionRuleHandler;
import cn.iocoder.yudao.framework.datapermission.core.rule.DataPermissionRuleFactory;
import cn.iocoder.yudao.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import cn.iocoder.yudao.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import cn.iocoder.yudao.module.system.service.user.AdminUserServiceImpl;
import cn.iocoder.yudao.module.system.framework.datapermission.config.DataPermissionConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import jakarta.annotation.Resource;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.framework.Advised;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * UM-05/S-01: real Spring proxies, production tenant interceptor and formal H2 Mappers.
 * Redis is an observable I/O substitute; no Mapper or business-service result is mocked.
 * SQL budgets count prepared production SELECT statements, not Mockito invocations.
 * RR cases prove suspension on this H2 runtime, not MySQL isolation or real Redis behavior.
 */
@Import({OAuth2TokenServiceImpl.class, AdminUserServiceImpl.class, TransactionAutoConfiguration.class,
        YudaoDataPermissionAutoConfiguration.class, DataPermissionConfiguration.class,
        OAuth2PasswordSessionTransactionTest.TransactionTestConfiguration.class,
        OAuth2TokenAuthorityTransactionTest.SqlObservationConfiguration.class})
public class OAuth2TokenAuthorityTransactionTest extends BaseDbUnitTest {

    private static final long TENANT = 1L;
    private static final long CALLER_TENANT = 2L;
    private static final long USER = 1101L;
    private static final int ADMIN = UserTypeEnum.ADMIN.getValue();
    private static final int MEMBER = UserTypeEnum.MEMBER.getValue();

    @Resource private OAuth2TokenService tokenService;
    @Resource private AdminUserMapper userMapper;
    @Resource private OAuth2AccessTokenMapper accessMapper;
    @Resource private OAuth2RefreshTokenMapper refreshMapper;
    @Resource private DataSource dataSource;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private MybatisPlusInterceptor mybatisPlusInterceptor;
    @Resource private AuthoritySqlRecorder sqlRecorder;
    @Resource private DataPermissionRuleHandler dataPermissionRuleHandler;
    @Resource private DataPermissionRuleFactory dataPermissionRuleFactory;
    @Resource private DataPermissionAnnotationAdvisor dataPermissionAnnotationAdvisor;
    @MockitoBean private PermissionCommonApi permissionCommonApi;
    @MockitoBean private DeptService deptService;
    @MockitoBean private PostService postService;
    @MockitoBean private PermissionService permissionService;
    @MockitoBean private TenantService tenantService;
    @MockitoBean private ConfigApi configApi;
    @MockitoBean private OAuth2ClientService clientService;
    @MockitoBean private OAuth2AccessTokenRedisDAO redisDAO;

    private final Map<String, OAuth2AccessTokenDO> cache = new ConcurrentHashMap<>();
    private JdbcTemplate jdbc;

    @BeforeEach
    void establishRealBoundaries() {
        TenantContextHolder.setTenantId(TENANT);
        TenantContextHolder.setIgnore(false);
        jdbc = new JdbcTemplate(dataSource);
        cache.clear();
        SecurityContextHolder.clearContext();
        DataPermissionContextHolder.clear();
        assertTrue(AopUtils.isAopProxy(tokenService));
        assertEquals(OAuth2TokenServiceImpl.class, AopUtils.getTargetClass(tokenService));
        for (Object mapper : List.of(userMapper, accessMapper, refreshMapper)) {
            assertFalse(mockingDetails(mapper).isMock(), "formal Mapper must execute production SQL");
        }
        assertTrue(mybatisPlusInterceptor.getInterceptors().stream()
                .anyMatch(TenantLineInnerInterceptor.class::isInstance));
        assertTrue(mybatisPlusInterceptor.getInterceptors().stream()
                .anyMatch(DataPermissionInterceptor.class::isInstance));
        assertNotNull(dataPermissionRuleHandler);
        assertTrue(dataPermissionRuleFactory.getDataPermissionRules().stream()
                .anyMatch(DeptDataPermissionRule.class::isInstance));
        assertInstanceOf(DataPermissionAnnotationInterceptor.class, dataPermissionAnnotationAdvisor.getAdvice());
        assertTrue(Arrays.stream(((Advised) tokenService).getAdvisors())
                .anyMatch(advisor -> advisor instanceof DataPermissionAnnotationAdvisor),
                "token entry must receive the actual production data-permission advisor");
        when(redisDAO.get(anyString())).thenAnswer(invocation -> cache.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            OAuth2AccessTokenDO row = invocation.getArgument(0);
            cache.put(row.getAccessToken(), row);
            return null;
        }).when(redisDAO).set(any());
        doAnswer(invocation -> {
            cache.remove(invocation.getArgument(0));
            return null;
        }).when(redisDAO).delete(anyString());
        doAnswer(invocation -> {
            Collection<String> keys = invocation.getArgument(0);
            keys.forEach(cache::remove);
            return null;
        }).when(redisDAO).deleteList(any());
    }

    @AfterEach
    void releaseThreadContexts() {
        sqlRecorder.stop();
        SecurityContextHolder.clearContext();
        DataPermissionContextHolder.clear();
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void orphanCacheNeverAuthorizesAdminOrMember(int type) {
        cache.put("orphan", accessValue("orphan", USER, type, CALLER_TENANT));
        assertAbsent("orphan");
    }

    @ParameterizedTest
    @EnumSource(Entry.class)
    void formalAccessIdentityWinsOverPollutedCache(Entry entry) {
        seedUser("ACTIVE");
        OAuth2AccessTokenDO formal = seedAccess("access", USER, ADMIN);
        cache.put("access", accessValue("access", USER + 99, MEMBER, CALLER_TENANT)
                .setClientId("wrong-client").setScopes(List.of("wrong-scope")));
        Trace trace = sqlRecorder.start();
        OAuth2AccessTokenDO actual;
        try {
            actual = entry.read(tokenService, "access");
        } finally {
            sqlRecorder.stop();
        }
        assertFormalIdentity(formal, actual);
        assertEquals("Authority test user", actual.getUserInfo().get("nickname"));
        trace.assertBudget(1, 0, 1);
    }

    @ParameterizedTest
    @EnumSource(Entry.class)
    void accessIsPreferredToSameStringRefreshAndCostsTwoQueries(Entry entry) {
        seedUser("ACTIVE");
        OAuth2AccessTokenDO formal = seedAccess("collision", USER, ADMIN);
        seedRefresh("collision", USER + 99, MEMBER);
        Trace trace = sqlRecorder.start();
        try {
            assertFormalIdentity(formal, entry.read(tokenService, "collision"));
        } finally {
            sqlRecorder.stop();
        }
        trace.assertBudget(1, 0, 1);
    }

    @ParameterizedTest
    @EnumSource(Entry.class)
    void formalRefreshAsBearerIsValidAndCostsThreeQueries(Entry entry) {
        seedUser("ACTIVE");
        OAuth2RefreshTokenDO formal = seedRefresh("refresh", USER, ADMIN);
        cache.put("refresh", accessValue("refresh", USER + 99, MEMBER, CALLER_TENANT));
        Trace trace = sqlRecorder.start();
        OAuth2AccessTokenDO actual;
        try {
            actual = entry.read(tokenService, "refresh");
        } finally {
            sqlRecorder.stop();
        }
        assertNotNull(actual);
        assertEquals(formal.getTenantId(), actual.getTenantId());
        assertEquals(formal.getUserId(), actual.getUserId());
        assertEquals(formal.getUserType(), actual.getUserType());
        assertEquals(formal.getClientId(), actual.getClientId());
        assertEquals(formal.getScopes(), actual.getScopes());
        assertEquals(formal.getExpiresTime(), actual.getExpiresTime());
        trace.assertBudget(1, 1, 1);
    }

    @Test
    void bothBearerFormsAreRejectedAfterRealUserRevocationEvenWhenCacheReturns() {
        seedUser("RESET_REQUIRED");
        OAuth2AccessTokenDO access = seedAccess("revoke-access", USER, ADMIN);
        OAuth2RefreshTokenDO refresh = seedRefresh(access.getRefreshToken(), USER, ADMIN);
        assertNotNull(tokenService.checkAccessToken(access.getAccessToken()));
        assertNotNull(tokenService.checkAccessToken(refresh.getRefreshToken()));
        tokenService.removeAccessToken(USER, ADMIN);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM system_oauth2_access_token WHERE deleted = false", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM system_oauth2_refresh_token WHERE deleted = false", Integer.class));
        cache.put(access.getAccessToken(), access);
        cache.put(refresh.getRefreshToken(), accessValue(refresh.getRefreshToken(), USER, ADMIN, TENANT));
        assertAbsent(access.getAccessToken());
        assertAbsent(refresh.getRefreshToken());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "deleted", "disabled", "locked", "wrong-tenant"})
    void positiveAdminRequiresCurrentEnabledUnlockedAccount(String condition) {
        if (!condition.equals("missing")) {
            seedUser("ACTIVE");
            AdminUserDO update = new AdminUserDO().setId(USER);
            if (condition.equals("deleted")) {
                assertEquals(1, userMapper.deleteById(USER));
            } else if (condition.equals("wrong-tenant")) {
                assertEquals(1, jdbc.update("UPDATE system_users SET tenant_id = ? WHERE id = ?", CALLER_TENANT, USER));
            } else if (condition.equals("disabled")) {
                update.setStatus(CommonStatusEnum.DISABLE.getStatus());
            } else {
                update.setLoginLocked(1).setLoginLockedTime(LocalDateTime.now());
            }
            if (condition.equals("disabled") || condition.equals("locked")) {
                assertEquals(1, userMapper.updateById(update));
            }
        }
        seedAccess("guarded", USER, ADMIN);
        seedRefresh("guarded-refresh", USER, ADMIN);
        for (String key : List.of("guarded", "guarded-refresh")) {
            for (Entry entry : Entry.values()) {
                Trace trace = sqlRecorder.start();
                try {
                    assertUnauthorized(() -> entry.read(tokenService, key));
                } finally {
                    sqlRecorder.stop();
                }
                trace.assertBudget(1, key.equals("guarded-refresh") ? 1 : 0, 1);
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"false,-1,2", "true,-1,2", "false,1101,99", "true,1101,99"})
    void invalidFormalOwnerOrTypeFailsBeforePersonOrCacheRead(boolean refresh, long owner, int type) {
        if (refresh) {
            seedRefresh("invalid-formal", owner, type);
        } else {
            seedAccess("invalid-formal", owner, type);
        }
        cache.put("invalid-formal", accessValue("invalid-formal", 0L, ADMIN, TENANT));
        for (Entry entry : Entry.values()) {
            Trace trace = sqlRecorder.start();
            try {
                assertUnauthorized(() -> entry.read(tokenService, "invalid-formal"));
            } finally {
                sqlRecorder.stop();
            }
            trace.assertBudget(1, refresh ? 1 : 0, 0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"INITIAL", "RESET_REQUIRED"})
    void passwordChangeRequiredCredentialsRemainUsableForValidation(String credential) {
        seedUser(credential);
        seedAccess("pending-password", USER, ADMIN);
        assertNotNull(tokenService.getAccessToken("pending-password"));
        assertNotNull(tokenService.checkAccessToken("pending-password"));
        assertEquals(credential, userMapper.selectById(USER).getPasswordCredentialStatus());
    }

    @ParameterizedTest
    @CsvSource({"GET,false", "CHECK,false", "GET,true", "CHECK,true"})
    void machineAndMemberNeverReadAdminPeopleAndKeepTheirFormalBearerProtocol(Entry entry, boolean refresh) {
        for (int type : List.of(ADMIN, MEMBER)) {
            long owner = type == ADMIN ? 0L : USER;
            String key = "kind-" + type + "-" + refresh;
            if (refresh) {
                seedRefresh(key, owner, type);
            } else {
                seedAccess(key, owner, type);
            }
            Trace trace = sqlRecorder.start();
            try {
                OAuth2AccessTokenDO actual = entry.read(tokenService, key);
                assertNotNull(actual);
                assertEquals(owner, actual.getUserId());
                assertEquals(type, actual.getUserType());
                assertEquals(TENANT, actual.getTenantId());
            } finally {
                sqlRecorder.stop();
            }
            trace.assertBudget(1, refresh ? 1 : 0, 0);
        }
    }

    @ParameterizedTest
    @CsvSource({"GET,false", "CHECK,false", "GET,true", "CHECK,true"})
    void storedTenantIsAuthoritativeAndCallerTenantIgnoreRestored(Entry entry, boolean ignore) {
        seedUser("ACTIVE");
        OAuth2AccessTokenDO formal = seedAccess("tenant-access", USER, ADMIN);
        TenantContextHolder.setTenantId(CALLER_TENANT);
        TenantContextHolder.setIgnore(ignore);
        assertFormalIdentity(formal, entry.read(tokenService, "tenant-access"));
        assertCallerContext(ignore);
        RuntimeException injected = new IllegalStateException("authority Redis write I/O failure");
        doAnswer(invocation -> { throw injected; }).when(redisDAO).set(any());
        assertSame(injected, assertThrows(RuntimeException.class, () -> entry.read(tokenService, "tenant-access")));
        assertCallerContext(ignore);
    }

    @ParameterizedTest
    @EnumSource(Entry.class)
    void formalQueryFailurePropagatesAndRestoresCallerContext(Entry entry) {
        seedUser("ACTIVE");
        seedAccess("query-fault", USER, ADMIN);
        TenantContextHolder.setTenantId(CALLER_TENANT);
        TenantContextHolder.setIgnore(true);
        Trace trace = sqlRecorder.start();
        trace.failUserRead = true;
        try {
            RuntimeException error = assertThrows(RuntimeException.class, () -> entry.read(tokenService, "query-fault"));
            boolean found = false;
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                found |= cause == trace.queryFailure;
            }
            assertTrue(found, "database boundary failure must propagate, never become successful authentication");
        } finally {
            sqlRecorder.stop();
        }
        assertCallerContext(true);
    }

    @ParameterizedTest
    @CsvSource({"GET,false", "CHECK,false", "GET,true", "CHECK,true"})
    void publicEntrySuspendsOldRepeatableReadAndResumesOuterTransaction(Entry entry, boolean refresh) throws Exception {
        seedUser("ACTIVE");
        String key = "rr-bearer";
        String table;
        String column;
        long rowId;
        if (refresh) {
            rowId = seedRefresh(key, USER, ADMIN).getId();
            table = "system_oauth2_refresh_token";
            column = "refresh_token";
        } else {
            rowId = seedAccess(key, USER, ADMIN).getId();
            table = "system_oauth2_access_token";
            column = "access_token";
        }
        cache.put(key, accessValue(key, USER, ADMIN, TENANT));
        ExecutorService worker = Executors.newSingleThreadExecutor();
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        outer.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        try {
            outer.executeWithoutResult(status -> {
                String countSql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ? AND deleted = false";
                assertEquals(1, jdbc.queryForObject(countSql, Integer.class, key));
                Object originalConnection = TransactionSynchronizationManager.getResource(dataSource);
                assertNotNull(originalConnection);
                Future<?> deletion = worker.submit(() -> withTenant(() -> {
                    new TransactionTemplate(transactionManager).executeWithoutResult(deleteStatus -> {
                        int deleted = refresh ? refreshMapper.deleteById(rowId) : accessMapper.deleteById(rowId);
                        assertEquals(1, deleted);
                    });
                }));
                awaitFuture(deletion);
                assertEquals(1, jdbc.queryForObject(countSql, Integer.class, key),
                        "ordinary JDBC read must still observe the established old RR snapshot");
                Trace trace = sqlRecorder.start();
                try {
                    if (entry == Entry.GET) {
                        assertNull(entry.read(tokenService, key));
                    } else {
                        assertUnauthorized(() -> entry.read(tokenService, key));
                    }
                } finally {
                    sqlRecorder.stop();
                }
                trace.assertBudget(1, 1, 0);
                assertTrue(trace.queries.stream().noneMatch(Query::transactionActive),
                        "both public entry proxies must suspend the outer transaction for every formal read");
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ,
                        TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
                assertSame(originalConnection, TransactionSynchronizationManager.getResource(dataSource));
                assertEquals(1, jdbc.queryForObject(countSql, Integer.class, key));
                assertEquals(TENANT, TenantContextHolder.getTenantId());
                assertFalse(TenantContextHolder.isIgnore());
            });
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE deleted = false", Integer.class));
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        } finally {
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void lateCacheFillAfterRevocationCannotAuthorizeNextRequest(boolean refresh) throws Exception {
        seedUser("ACTIVE");
        String key = refresh ? "late-refresh" : "late-access";
        if (refresh) {
            seedRefresh(key, USER, ADMIN);
        } else {
            seedAccess(key, USER, ADMIN);
        }
        CountDownLatch fillReached = new CountDownLatch(1);
        CountDownLatch allowLateFill = new CountDownLatch(1);
        doAnswer(invocation -> {
            OAuth2AccessTokenDO row = invocation.getArgument(0);
            fillReached.countDown();
            awaitLatch(allowLateFill);
            cache.put(row.getAccessToken(), row);
            return null;
        }).when(redisDAO).set(any());
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<OAuth2AccessTokenDO> alreadyValidated = worker.submit(() -> withTenantResult(() -> tokenService.checkAccessToken(key)));
            awaitLatch(fillReached);
            tokenService.removeAccessToken(USER, ADMIN);
            allowLateFill.countDown();
            assertNotNull(alreadyValidated.get(15, TimeUnit.SECONDS),
                    "a request validated before revocation is not retroactively cancelled");
            assertTrue(cache.containsKey(key), "fixture must demonstrate an actual late cache fill after deletion committed");
            assertAbsent(key);
        } finally {
            allowLateFill.countDown();
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @CsvSource({"GET,false,true", "CHECK,false,true", "GET,true,true", "CHECK,true,true",
            "GET,false,false", "CHECK,false,false", "GET,true,false", "CHECK,true,false"})
    void existingRestrictedAdminCannotMaskTokenOwnerOrAddPermissionQueries(Entry entry, boolean refresh, boolean selfOnly) {
        seedUser("ACTIVE");
        String key = "permission-bearer";
        if (refresh) {
            seedRefresh(key, USER, ADMIN);
        } else {
            seedAccess(key, USER, ADMIN);
        }
        establishRestrictiveCallerAndProveFiltering(selfOnly);
        TenantContextHolder.setTenantId(CALLER_TENANT);
        TenantContextHolder.setIgnore(true);
        DataPermission outerPermission = PermissionScopeMarker.class.getAnnotation(DataPermission.class);
        DataPermissionContextHolder.add(outerPermission);
        try {
            Trace trace = sqlRecorder.start();
            OAuth2AccessTokenDO result;
            try {
                result = entry.read(tokenService, key);
            } finally {
                sqlRecorder.stop();
            }
            assertNotNull(result);
            assertEquals(USER, result.getUserId());
            assertEquals(TENANT, result.getTenantId());
            assertEquals("Authority test user", result.getUserInfo().get("nickname"));
            trace.assertBudget(1, refresh ? 1 : 0, 1);
            assertPermissionDisabledDuringFormalReads(trace);
            verifyNoInteractions(permissionCommonApi);
            assertSame(outerPermission, DataPermissionContextHolder.get());
            assertEquals(List.of(outerPermission), DataPermissionContextHolder.getAll());
            assertCallerContext(true);
            assertEquals(USER + 1, SecurityFrameworkUtils.getLoginUserId());
        } finally {
            assertSame(outerPermission, DataPermissionContextHolder.remove());
        }
        assertNull(DataPermissionContextHolder.get());
    }

    @ParameterizedTest
    @CsvSource({"GET,false,redis", "CHECK,false,redis", "GET,true,redis", "CHECK,true,redis",
            "GET,false,query", "CHECK,false,query", "GET,true,query", "CHECK,true,query",
            "GET,false,unauthorized", "CHECK,false,unauthorized", "GET,true,unauthorized", "CHECK,true,unauthorized"})
    void permissionScopeAndCallerAndOuterTransactionRestoreAfterFailure(Entry entry, boolean refresh, String fault) {
        seedUser("ACTIVE");
        String key = "permission-fault";
        if (refresh) {
            seedRefresh(key, USER, ADMIN);
        } else {
            seedAccess(key, USER, ADMIN);
        }
        if (fault.equals("unauthorized")) {
            assertEquals(1, userMapper.updateById(new AdminUserDO().setId(USER).setStatus(CommonStatusEnum.DISABLE.getStatus())));
        }
        establishRestrictiveCallerAndProveFiltering(true);
        RuntimeException redisFailure = new IllegalStateException("permission-boundary Redis write I/O failure");
        if (fault.equals("redis")) {
            doAnswer(invocation -> { throw redisFailure; }).when(redisDAO).set(any());
        }
        TenantContextHolder.setTenantId(CALLER_TENANT);
        TenantContextHolder.setIgnore(true);
        DataPermission outerPermission = PermissionScopeMarker.class.getAnnotation(DataPermission.class);
        DataPermissionContextHolder.add(outerPermission);
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        outer.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        try {
            outer.executeWithoutResult(status -> {
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM system_users WHERE id = ? AND deleted = false", Integer.class, USER));
                Object originalConnection = TransactionSynchronizationManager.getResource(dataSource);
                assertNotNull(originalConnection);
                Trace trace = sqlRecorder.start();
                trace.failUserRead = fault.equals("query");
                RuntimeException error;
                try {
                    error = assertThrows(RuntimeException.class, () -> entry.read(tokenService, key));
                } finally {
                    sqlRecorder.stop();
                }
                if (fault.equals("redis")) {
                    assertSame(redisFailure, error);
                } else if (fault.equals("query")) {
                    boolean matched = false;
                    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                        matched |= cause == trace.queryFailure;
                    }
                    assertTrue(matched, "formal query failure must remain in the exception chain");
                } else {
                    assertEquals(401, assertInstanceOf(ServiceException.class, error).getCode());
                }
                trace.assertBudget(1, refresh ? 1 : 0, 1);
                assertPermissionDisabledDuringFormalReads(trace);
                assertTrue(trace.queries.stream().noneMatch(Query::transactionActive));
                assertSame(outerPermission, DataPermissionContextHolder.get());
                assertEquals(List.of(outerPermission), DataPermissionContextHolder.getAll());
                assertCallerContext(true);
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ,
                        TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
                assertSame(originalConnection, TransactionSynchronizationManager.getResource(dataSource));
                assertFalse(status.isRollbackOnly(), "suspended authority failure must not mark the caller transaction rollback-only");
                assertEquals(USER + 1, SecurityFrameworkUtils.getLoginUserId());
                verifyNoInteractions(permissionCommonApi);
            });
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertSame(outerPermission, DataPermissionContextHolder.get());
        } finally {
            assertSame(outerPermission, DataPermissionContextHolder.remove());
        }
        assertNull(DataPermissionContextHolder.get());
    }

    private void establishRestrictiveCallerAndProveFiltering(boolean selfOnly) {
        DeptDataPermissionRespDTO restriction = new DeptDataPermissionRespDTO().setAll(false).setSelf(selfOnly)
                .setDeptIds(selfOnly ? Set.of() : Set.of(999L));
        when(permissionCommonApi.getDeptDataPermission(anyLong())).thenReturn(restriction);
        installFreshCaller();
        assertNull(userMapper.selectSessionSubject(TENANT, USER),
                "control query must actually be hidden by the production department data-permission rule");
        verify(permissionCommonApi).getDeptDataPermission(USER + 1);
        // The production rule caches restrictions in LoginUser. Replace it so zero API calls
        // from token validation cannot be explained by that caller cache.
        installFreshCaller();
        clearInvocations(permissionCommonApi);
    }

    private void installFreshCaller() {
        LoginUser caller = new LoginUser().setId(USER + 1).setUserType(ADMIN).setTenantId(TENANT)
                .setInfo(Map.of(LoginUser.INFO_KEY_DEPT_ID, "999", LoginUser.INFO_KEY_NICKNAME, "Restricted caller"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(caller, null, List.of()));
        assertSame(caller, SecurityFrameworkUtils.getLoginUser());
    }

    private void assertPermissionDisabledDuringFormalReads(Trace trace) {
        assertFalse(trace.queries.isEmpty());
        assertTrue(trace.queries.stream().allMatch(query -> Boolean.FALSE.equals(query.dataPermissionEnabled())),
                "production annotation interceptor must disable data permissions during all authority SQL");
    }

    private void assertAbsent(String key) {
        for (Entry entry : Entry.values()) {
            Trace trace = sqlRecorder.start();
            try {
                if (entry == Entry.GET) {
                    assertNull(entry.read(tokenService, key));
                } else {
                    assertUnauthorized(() -> entry.read(tokenService, key));
                }
            } finally {
                sqlRecorder.stop();
            }
            trace.assertBudget(1, 1, 0);
        }
    }

    private void assertUnauthorized(Runnable call) {
        assertEquals(401, assertThrows(ServiceException.class, call::run).getCode());
    }

    private void assertCallerContext(boolean ignore) {
        assertEquals(CALLER_TENANT, TenantContextHolder.getTenantId());
        assertEquals(ignore, TenantContextHolder.isIgnore());
    }

    private void assertFormalIdentity(OAuth2AccessTokenDO expected, OAuth2AccessTokenDO actual) {
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getTenantId(), actual.getTenantId());
        assertEquals(expected.getUserId(), actual.getUserId());
        assertEquals(expected.getUserType(), actual.getUserType());
        assertEquals(expected.getClientId(), actual.getClientId());
        assertEquals(expected.getScopes(), actual.getScopes());
        assertEquals(expected.getExpiresTime(), actual.getExpiresTime());
    }

    private void seedUser(String credential) {
        AdminUserDO user = new AdminUserDO().setId(USER).setUsername("um05-authority")
                .setCanonicalUsername("um05-authority").setNickname("Authority test user")
                .setPassword("test-only-unused-credential").setPasswordUpdateTime(LocalDateTime.now())
                .setPasswordCredentialStatus(credential).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setDeptId(501L).setLoginLocked(0).setLoginFailureCount(0);
        user.setTenantId(TENANT);
        assertEquals(1, userMapper.insert(user));
    }

    private OAuth2AccessTokenDO seedAccess(String key, long owner, int type) {
        OAuth2AccessTokenDO row = accessValue(key, owner, type, TENANT);
        assertEquals(1, accessMapper.insert(row));
        return accessMapper.selectById(row.getId());
    }

    private OAuth2AccessTokenDO accessValue(String key, long owner, int type, long tenant) {
        OAuth2AccessTokenDO row = new OAuth2AccessTokenDO().setAccessToken(key).setRefreshToken(key + "-refresh")
                .setUserId(owner).setUserType(type).setClientId("formal-client").setScopes(List.of("read", "write"))
                .setUserInfo(Map.of("nickname", "formal owner")).setExpiresTime(LocalDateTime.now().plusHours(1).withNano(0));
        row.setTenantId(tenant);
        return row;
    }

    private OAuth2RefreshTokenDO seedRefresh(String key, long owner, int type) {
        OAuth2RefreshTokenDO row = new OAuth2RefreshTokenDO().setRefreshToken(key).setUserId(owner).setUserType(type)
                .setClientId("formal-client").setScopes(List.of("read", "write"))
                .setExpiresTime(LocalDateTime.now().plusHours(2).withNano(0));
        row.setTenantId(TENANT);
        assertEquals(1, refreshMapper.insert(row));
        return refreshMapper.selectById(row.getId());
    }

    private void withTenant(Runnable action) {
        withTenantResult(() -> { action.run(); return null; });
    }

    private <T> T withTenantResult(java.util.function.Supplier<T> action) {
        TenantContextHolder.setTenantId(TENANT);
        TenantContextHolder.setIgnore(false);
        try {
            return action.get();
        } finally {
            sqlRecorder.stop();
            TenantContextHolder.clear();
        }
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "test ordering boundary was not reached");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted authority test ordering", error);
        }
    }

    private void awaitFuture(Future<?> future) {
        try {
            future.get(15, TimeUnit.SECONDS);
        } catch (Exception error) {
            throw new IllegalStateException("Concurrent formal deletion did not commit", error);
        }
    }

    enum Entry {
        GET, CHECK;

        OAuth2AccessTokenDO read(OAuth2TokenService service, String bearer) {
            return this == GET ? service.getAccessToken(bearer) : service.checkAccessToken(bearer);
        }
    }

    @DataPermission(enable = true)
    static class PermissionScopeMarker { }

    record Query(String table, boolean transactionActive, Boolean dataPermissionEnabled) { }

    static class Trace {
        final List<Query> queries = new ArrayList<>();
        final RuntimeException queryFailure = new IllegalStateException("authority formal user SELECT I/O failure");
        boolean failUserRead;

        void assertBudget(long access, long refresh, long user) {
            assertEquals(access, count("system_oauth2_access_token"), "formal access SELECT budget");
            assertEquals(refresh, count("system_oauth2_refresh_token"), "formal refresh SELECT budget");
            assertEquals(user, count("system_users"), "formal ADMIN person SELECT budget");
            assertEquals(access + refresh + user, queries.size(), "no hidden extra formal SELECT");
        }

        long count(String table) {
            return queries.stream().filter(query -> query.table().equals(table)).count();
        }
    }

    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    static class AuthoritySqlRecorder implements Interceptor {
        private final ThreadLocal<Trace> current = new ThreadLocal<>();

        Trace start() {
            assertNull(current.get(), "SQL observation must not nest or leak between entry calls");
            Trace trace = new Trace();
            current.set(trace);
            return trace;
        }

        void stop() {
            current.remove();
        }

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            Trace trace = current.get();
            String sql = ((StatementHandler) invocation.getTarget()).getBoundSql().getSql()
                    .toLowerCase(Locale.ROOT).replace('"', ' ').replace('`', ' ').replaceAll("\\s+", " ").trim();
            if (trace != null && sql.startsWith("select ")) {
                String table = sql.contains(" from system_oauth2_access_token ") ? "system_oauth2_access_token"
                        : sql.contains(" from system_oauth2_refresh_token ") ? "system_oauth2_refresh_token"
                        : sql.contains(" from system_users ") ? "system_users" : "unexpected-formal-select";
                DataPermission permission = DataPermissionContextHolder.get();
                trace.queries.add(new Query(table, TransactionSynchronizationManager.isActualTransactionActive(),
                        permission == null ? null : permission.enable()));
                if (table.equals("system_users") && trace.failUserRead) {
                    throw trace.queryFailure;
                }
            }
            return invocation.proceed();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SqlObservationConfiguration {
        @Bean
        DeptDataPermissionRule authorityDeptDataPermissionRule(PermissionCommonApi api,
                                                               List<DeptDataPermissionRuleCustomizer> customizers,
                                                               AdminUserMapper userMapper, DeptMapper deptMapper) {
            // Instantiate both formal Mappers first: the production customizer resolves
            // column registrations through their initialized MyBatis TableInfo metadata.
            DeptDataPermissionRule rule = new DeptDataPermissionRule(api);
            customizers.forEach(customizer -> customizer.customize(rule));
            return rule;
        }

        @Bean
        @Lazy(false)
        AuthoritySqlRecorder authoritySqlRecorder() {
            // MyBatis auto-configuration installs Interceptor beans on the real SqlSessionFactory.
            return new AuthoritySqlRecorder();
        }
    }
}
