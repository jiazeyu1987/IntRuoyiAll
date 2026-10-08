package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.*;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.PostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostServiceImpl;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Resource;
import org.apache.ibatis.executor.result.ResultMapException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Spring transaction / tenant interceptor / H2. Does not establish InnoDB RR behavior. */
public class AdminUserPostRetentionTransactionTest extends UserPostRetentionTestSupport {

    @Test
    void editReadAndContactUpdateRetainDisabledBindingIdentity() {
        AdminUserDO user = boundUser(Set.of(10L, 20L));
        UserPostDO retained = bindings(user).stream().filter(p -> p.getPostId() == 20L).findFirst().orElseThrow();
        UserEditRespVO edit = userService.getUserForUpdate(user.getId());
        assertEquals(Set.of(10L, 20L), edit.getPostIds());
        assertEquals(List.of(10L, 20L), edit.getAssignedPosts().stream().map(UserEditRespVO.PostItem::getId).toList());
        assertEquals("停用岗位", edit.getAssignedPosts().get(1).getName());
        assertEquals(1, edit.getAssignedPosts().get(1).getStatus());
        userService.updateUser(request(user, Set.of(10L, 20L)));
        assertEquals("changed@example.test", userMapper.selectById(user.getId()).getEmail());
        assertStorage(user, Set.of(10L, 20L));
        assertEquals(retained, userPostMapper.selectById(retained.getId()));
        assertProtectedFields(user);
        verifyNoInteractions(permissionService, oauth2TokenService, passwordEncoder);
    }

    @Test
    void explicitRemovalAndEmptyArrayOnlyDeleteTheDifference() {
        AdminUserDO user = boundUser(Set.of(10L, 20L));
        UserPostDO retained = bindings(user).stream().filter(p -> p.getPostId() == 10L).findFirst().orElseThrow();
        userService.updateUser(request(user, Set.of(10L)));
        assertStorage(user, Set.of(10L));
        assertEquals(retained, userPostMapper.selectById(retained.getId()));
        userService.updateUser(request(user, Set.of()));
        assertStorage(user, Set.of());
        assertTrue(userService.getUserForUpdate(user.getId()).getAssignedPosts().isEmpty());
    }

    @Test
    void enabledAdditionPersistsBothSets() {
        AdminUserDO user = boundUser(Set.of(20L));
        userService.updateUser(request(user, Set.of(20L, 30L)));
        assertStorage(user, Set.of(20L, 30L));
    }

    @Test
    void disabledNewPostCannotBeAddedAndGlobalValidationStillRejectsIt() {
        AdminUserDO user = boundUser(Set.of(10L));
        assertRejectedUnchanged(user, Set.of(10L, 20L), POST_NOT_ENABLE);
        assertCode(POST_NOT_ENABLE, () -> postService.validatePostList(Set.of(20L)));
    }

    @Test
    void latestDisabledStateIsRecheckedAfterAnEditSnapshot() {
        AdminUserDO user = boundUser(Set.of(10L));
        userService.getUserForUpdate(user.getId());
        jdbc.update("UPDATE system_post SET status=1 WHERE id=30 AND tenant_id=1");
        assertRejectedUnchanged(user, Set.of(10L, 30L), POST_NOT_ENABLE);
    }

    @Test
    void missingDeletedAndForeignTenantAdditionsRejectWithoutWrites() {
        AdminUserDO user = boundUser(Set.of(10L));
        for (Long id : List.of(40L, 50L, 999L)) {
            assertRejectedUnchanged(user, Set.of(10L, id), POST_NOT_FOUND);
        }
        assertEquals("外租户岗位", jdbc.queryForObject("SELECT name FROM system_post WHERE id=50", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM system_user_post WHERE tenant_id=2", Integer.class));
    }

    @Test
    void jsonJoinMismatchRejectsReadAndAttemptedRemovalButGenericReadStaysAvailable() {
        AdminUserDO user = seedUser(Set.of(10L));
        assertIntegrityReadAndRemoval(user);
        assertNotNull(userService.getUser(user.getId()));
    }

    @Test
    void nullJsonWithNonemptyJoinRejects() {
        AdminUserDO user = seedUser(null);
        bind(user, 10L);
        assertIntegrityReadAndRemoval(user);
    }

    @Test
    void differentJsonAndJoinSetsReject() {
        AdminUserDO user = seedUser(Set.of(10L));
        bind(user, 20L);
        assertIntegrityReadAndRemoval(user);
    }

    @Test
    void duplicateActiveBindingIsRejectedBeforeSetConversion() {
        AdminUserDO user = boundUser(Set.of(10L));
        bind(user, 10L);
        assertIntegrityReadAndRemoval(user);
    }

    @Test
    void deletedHistoricalJoinIsNotAnActiveDuplicate() {
        AdminUserDO user = boundUser(Set.of(20L));
        UserPostDO history = bind(user, 20L);
        userPostMapper.deleteById(history.getId());
        userService.updateUser(request(user, Set.of(20L)));
        assertStorage(user, Set.of(20L));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM system_user_post WHERE user_id=?", Integer.class, user.getId()));
    }

    @Test
    void deletedForeignAndMissingExistingPostRejectEvenIfRemoved() {
        for (Long postId : List.of(40L, 50L, 999L)) {
            assertIntegrityReadAndRemoval(boundUser(Set.of(postId)));
        }
    }

    @Test
    void invalidStoredNameOrStateRejectsEvenIfRemoved() {
        AdminUserDO user = boundUser(Set.of(10L));
        jdbc.update("UPDATE system_post SET name='   ' WHERE id=10");
        assertIntegrityReadAndRemoval(user);
        jdbc.update("UPDATE system_post SET name='启用岗位', status=8 WHERE id=10");
        assertIntegrityReadAndRemoval(user);
    }

    @Test
    void invalidStoredJsonAndJoinIdentitiesReject() {
        AdminUserDO badJson = seedUser(Set.of(-1L));
        assertIntegrityReadAndRemoval(badJson);
        AdminUserDO badJoin = seedUser(Set.of());
        bind(badJoin, -1L);
        assertIntegrityReadAndRemoval(badJoin);
    }

    @Test
    void rawStoredJsonRejectsCoercionDuplicateAndNoncanonicalIdentitiesWithoutWrites() {
        for (String raw : List.of("[10.5]", "[10,10]", "[\"01\"]", "[1e1]", "[\"10\",10]",
                "[null]", "[true]", "[{}]", "[[10]]", "[\"bad\"]", "[0]", "[-1]",
                "[9223372036854775808]", "[\"9223372036854775808\"]", "{}", "10", "\"10\"", "null", "[10")) {
            AdminUserDO user = boundUser(Set.of(10L));
            jdbc.update("UPDATE system_users SET post_ids=? WHERE id=? AND tenant_id=1", raw, user.getId());
            assertIntegrityReadAndRemoval(user);
            assertEquals(raw, jdbc.queryForObject("SELECT post_ids FROM system_users WHERE id=?", String.class, user.getId()));
        }
    }

    @Test
    void postIdsJacksonMappingFailureUsesOnlyTheEditIntegrityErrorAndPreservesCause() {
        AdminUserDO user = boundUser(Set.of(10L));
        jdbc.update("UPDATE system_users SET post_ids='[true]' WHERE id=? AND tenant_id=1", user.getId());
        List<Map<String, Object>> before = snapshot(user);
        ServiceException failure = assertThrows(ServiceException.class, () -> userService.getUserForUpdate(user.getId()));
        assertEquals(USER_POST_BINDING_INCONSISTENT.getCode(), failure.getCode());
        boolean postIdsColumnFailure = false;
        boolean jsonFailure = false;
        for (Throwable cause = failure.getCause(); cause != null; cause = cause.getCause()) {
            postIdsColumnFailure |= cause instanceof ResultMapException && cause.getMessage().contains("column 'post_ids'");
            jsonFailure |= cause instanceof JsonProcessingException;
        }
        assertTrue(postIdsColumnFailure);
        assertTrue(jsonFailure);
        assertEquals(before, snapshot(user));
        assertRejectedUnchanged(user, Set.of(), USER_POST_BINDING_INCONSISTENT);
    }

    @Test
    void canonicalLargeLongStoredStringAndSqlNullEmptyJoinRemainLegal() {
        long postId = 9007199254740993L;
        jdbc.update("INSERT INTO system_post(id,code,name,sort,status,deleted,tenant_id) VALUES(?,'um04-large','精确长编号岗位',0,1,FALSE,1)", postId);
        AdminUserDO user = boundUser(Set.of(postId));
        jdbc.update("UPDATE system_users SET post_ids=? WHERE id=? AND tenant_id=1", "[\"" + postId + "\"]", user.getId());
        assertEquals(Set.of(postId), userService.getUserForUpdate(user.getId()).getPostIds());
        userService.updateUser(request(user, Set.of(postId)));
        assertStorage(user, Set.of(postId));
        AdminUserDO noPosts = seedUser(null);
        assertTrue(userService.getUserForUpdate(noPosts.getId()).getPostIds().isEmpty());
        assertTrue(userService.getUserForUpdate(noPosts.getId()).getAssignedPosts().isEmpty());
        userService.updateUser(request(noPosts, Set.of()).setEmail("null-empty@example.test"));
        assertStorage(noPosts, Set.of());
    }

    @Test
    void missingDeletedAndCrossTenantUserRejectWithoutLeakage() {
        AdminUserDO deleted = seedUser(Set.of());
        userMapper.deleteById(deleted.getId());
        TenantContextHolder.setTenantId(2L);
        AdminUserDO foreign;
        try { foreign = seedUser(Set.of()); } finally { TenantContextHolder.setTenantId(1L); }
        for (Long id : List.of(deleted.getId(), foreign.getId(), 999999L)) {
            assertCode(USER_NOT_EXISTS, () -> userService.getUserForUpdate(id));
            assertCode(USER_NOT_EXISTS, () -> userService.updateUser(request(foreign, Set.of()).setId(id)));
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM system_users WHERE id=? AND tenant_id=2", Integer.class, foreign.getId()));
    }

    @Test
    void directServiceRequiresPositiveIdAndCompleteValidTarget() {
        AdminUserDO user = boundUser(Set.of(10L));
        for (Long id : Arrays.asList(null, 0L, -1L)) {
            assertCode(USER_EDIT_ID_INVALID, () -> userService.updateUser(request(user, Set.of()).setId(id)));
        }
        assertRejectedUnchanged(user, null, USER_POST_IDS_REQUIRED);
        Set<Long> nullElement = new HashSet<>(); nullElement.add(null);
        for (Set<Long> ids : List.of(Set.of(0L), Set.of(-1L), nullElement)) {
            assertRejectedUnchanged(user, ids, USER_POST_IDS_INVALID);
        }
    }

    @Test
    void createMissingNullAndEmptyPostSelectionRemainOptionalAndEditable() {
        for (Set<Long> ids : Arrays.<Set<Long>>asList(null, Set.of())) {
            UserSaveReqVO create = createRequest(ids);
            Long id = userService.createUser(create);
            assertTrue(userService.getUserForUpdate(id).getPostIds().isEmpty());
            assertTrue(userService.getUserForUpdate(id).getAssignedPosts().isEmpty());
        }
    }

    @Test
    void createDisabledPostRemainsRejectedByRealPostService() {
        assertCode(POST_NOT_ENABLE, () -> userService.createUser(createRequest(Set.of(20L))));
        assertEquals(0L, userMapper.selectCount());
    }

    @Test
    void registerWithoutPostsRemainsEditable() {
        when(configApi.getConfigValueByKey(AdminUserServiceImpl.USER_REGISTER_ENABLED_KEY)).thenReturn("true");
        AuthRegisterReqVO request = new AuthRegisterReqVO();
        request.setUsername("register04"); request.setPassword("Synthetic@2026"); request.setNickname("注册用户");
        Long id = userService.registerUser(request);
        assertTrue(userService.getUserForUpdate(id).getPostIds().isEmpty());
    }

    @Test
    void excelOverwritePreservesTheOriginalJsonAndJoinEvenWhenPostDisabled() {
        AdminUserDO user = boundUser(Set.of(20L));
        UserPostDO binding = bindings(user).get(0);
        when(configApi.getConfigValueByKey(AdminUserServiceImpl.USER_INIT_PASSWORD_KEY)).thenReturn("Synthetic@2026");
        UserImportExcelVO imported = UserImportExcelVO.builder().username(user.getUsername())
                .nickname("导入名称").email("import@example.test").mobile("13800138000").sex(1).status(0).build();
        UserImportRespVO result = userService.importUserList(List.of(imported), true);
        assertEquals(List.of(user.getUsername()), result.getUpdateUsernames());
        assertTrue(result.getFailureUsernames().isEmpty());
        assertStorage(user, Set.of(20L));
        assertEquals(binding, userPostMapper.selectById(binding.getId()));
    }

    @Test
    void realAssociationWriteExceptionRollsBackAlreadyWrittenJsonAndContact() {
        AdminUserDO user = boundUser(Set.of(10L));
        List<Map<String, Object>> before = snapshot(user);
        AtomicBoolean reached = new AtomicBoolean();
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(Set.of(10L, 30L), userMapper.selectById(user.getId()).getPostIds());
            assertEquals("changed@example.test", userMapper.selectById(user.getId()).getEmail());
            // Reach a real H2 constraint failure after the user write, not a missing fixture.
            reached.set(true);
            jdbc.update("INSERT INTO system_user_post(id,user_id,post_id,tenant_id) VALUES(?,?,?,1)", bindings(user).get(0).getId(), user.getId(), 30L);
            return invocation.callRealMethod();
        }).when(userPostMapper).insertBatch(anyCollection());
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> userService.updateUser(request(user, Set.of(10L, 30L))));
        assertTrue(reached.get());
        assertEquals(before, snapshot(user));
    }

    @Test
    void associationFalseResultRejectsAndRollsBack() {
        AdminUserDO user = boundUser(Set.of(10L));
        doReturn(false).when(userPostMapper).insertBatch(anyCollection());
        assertRejectedUnchanged(user, Set.of(10L, 30L), USER_POST_WRITE_FAILED);
        verify(userPostMapper).insertBatch(anyCollection());
    }

    @Test
    void userUpdateCountMismatchRejectsWithoutAssociationWrites() {
        AdminUserDO user = boundUser(Set.of(10L));
        doReturn(0).when(userMapper).updateById(any(AdminUserDO.class));
        assertRejectedUnchanged(user, Set.of(10L, 30L), USER_POST_WRITE_FAILED);
        verify(userPostMapper, never()).insertBatch(anyCollection());
        verify(userPostMapper, never()).deleteByUserIdAndPostId(anyLong(), anyCollection());
    }

    @Test
    void deletionCountMismatchRejectsAndRollsBackTheRealDeletion() {
        AdminUserDO user = boundUser(Set.of(10L, 20L));
        doAnswer(invocation -> { invocation.callRealMethod(); return 0; })
                .when(userPostMapper).deleteByUserIdAndPostId(eq(user.getId()), anyCollection());
        assertRejectedUnchanged(user, Set.of(10L), USER_POST_WRITE_FAILED);
        verify(userPostMapper).deleteByUserIdAndPostId(eq(user.getId()), anyCollection());
    }

    @Test
    void otherTenantJoinDoesNotContaminateSnapshotOrGetDeleted() {
        AdminUserDO user = boundUser(Set.of(10L));
        jdbc.update("INSERT INTO system_user_post(user_id,post_id,tenant_id) VALUES(?,?,2)", user.getId(), 50L);
        assertEquals(Set.of(10L), userService.getUserForUpdate(user.getId()).getPostIds());
        userService.updateUser(request(user, Set.of()));
        assertStorage(user, Set.of());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM system_user_post WHERE user_id=? AND tenant_id=2 AND deleted=FALSE", Integer.class, user.getId()));
    }

    @Test
    void currentReadMappersHaveDeterministicOrderingAndEmptyPostSetIsExact() {
        AdminUserDO user = boundUser(Set.of(10L, 20L));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            userService.lockUserForSessionMutation(1L, user.getId());
            assertEquals(List.of(10L, 20L), userPostMapper.selectListByUserIdForUpdate(user.getId()).stream().map(UserPostDO::getPostId).toList());
            assertEquals(List.of(10L, 20L, 30L), postMapper.selectListByIdsForUpdate(List.of(30L, 20L, 10L)).stream().map(p -> p.getId()).toList());
            assertTrue(postMapper.selectListByIdsForUpdate(Set.of()).isEmpty());
        });
    }

    @Test
    void h2OuterTransactionUsesCurrentBindingAfterControlledConcurrentCommit() throws Exception {
        // H2's default READ_COMMITTED proves the real mapper/transaction sequence only.
        // MySQL REPEATABLE_READ behavior remains a separate production evidence boundary.
        AdminUserDO user = boundUser(Set.of(10L));
        CountDownLatch readOldBinding = new CountDownLatch(1);
        CountDownLatch writerCommitted = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Set<Long>> current = executor.submit(() -> {
                TenantContextHolder.setTenantId(1L);
                try {
                    return new TransactionTemplate(transactionManager).execute(status -> {
                        assertEquals(Set.of(10L), userService.getUser(user.getId()).getPostIds());
                        assertEquals(List.of(10L), bindings(user).stream().map(UserPostDO::getPostId).toList());
                        readOldBinding.countDown();
                        try { assertTrue(writerCommitted.await(15, TimeUnit.SECONDS)); }
                        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
                        return userService.getUserForUpdate(user.getId()).getPostIds();
                    });
                } finally { TenantContextHolder.clear(); }
            });
            assertTrue(readOldBinding.await(15, TimeUnit.SECONDS));
            userService.updateUser(request(user, Set.of(10L, 30L)));
            writerCommitted.countDown();
            assertEquals(Set.of(10L, 30L), current.get(15, TimeUnit.SECONDS));
        } finally {
            writerCommitted.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
    }
}

@Import({AdminUserServiceImpl.class, PostServiceImpl.class, TransactionAutoConfiguration.class,
        JacksonAutoConfiguration.class, YudaoJacksonAutoConfiguration.class, UserPostRetentionTestSupport.TenantConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class UserPostRetentionTestSupport extends BaseDbUnitTest {
    @Resource protected AdminUserServiceImpl userService;
    @MockitoSpyBean protected AdminUserMapper userMapper;
    @Resource protected PostMapper postMapper;
    @Resource protected PostServiceImpl postService;
    @Resource protected DataSource dataSource;
    @Resource protected ObjectMapper objectMapper;
    @Resource protected PlatformTransactionManager transactionManager;
    @MockitoSpyBean protected UserPostMapper userPostMapper;
    @MockitoBean protected DeptService deptService;
    @MockitoBean protected PermissionService permissionService;
    @MockitoBean protected PasswordEncoder passwordEncoder;
    @MockitoBean protected TenantService tenantService;
    @MockitoBean protected ConfigApi configApi;
    @MockitoBean protected OAuth2TokenService oauth2TokenService;
    protected JdbcTemplate jdbc;
    private int sequence;

    @BeforeEach
    void prepareFixtures() {
        jdbc = new JdbcTemplate(dataSource);
        when(passwordEncoder.encode(anyString())).thenReturn("synthetic-encoded-fixture");
        for (long id : new long[]{10, 20, 30, 40, 50}) {
            jdbc.update("INSERT INTO system_post(id,code,name,sort,status,deleted,tenant_id) VALUES(?,?,?,0,?,?,?)",
                    id, "um04-" + id, id == 20 ? "停用岗位" : id == 50 ? "外租户岗位" : "启用岗位" + id,
                    id == 20 ? 1 : 0, id == 40, id == 50 ? 2 : 1);
        }
    }

    protected AdminUserDO seedUser(Set<Long> postIds) {
        String username = "um04user" + ++sequence;
        AdminUserDO user = new AdminUserDO().setUsername(username).setCanonicalUsername(username).setNickname("原资料")
                .setPassword("synthetic-unchanged-hash").setPasswordCredentialStatus("ACTIVE").setStatus(0)
                .setEmail("before" + sequence + "@example.test").setMobile("1380013" + String.format("%04d", sequence))
                .setPostIds(postIds).setPasswordUpdateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        user.setTenantId(TenantContextHolder.getRequiredTenantId());
        userMapper.insert(user);
        return userMapper.selectById(user.getId());
    }

    protected AdminUserDO boundUser(Set<Long> ids) {
        AdminUserDO user = seedUser(ids);
        ids.forEach(id -> bind(user, id));
        return user;
    }

    protected UserPostDO bind(AdminUserDO user, Long postId) {
        UserPostDO binding = new UserPostDO().setUserId(user.getId()).setPostId(postId);
        userPostMapper.insert(binding);
        return userPostMapper.selectById(binding.getId());
    }

    protected List<UserPostDO> bindings(AdminUserDO user) { return userPostMapper.selectListByUserId(user.getId()); }

    protected UserSaveReqVO request(AdminUserDO user, Set<Long> ids) {
        return new UserSaveReqVO().setId(user.getId()).setUsername(user.getUsername()).setNickname("已修改")
                .setEmail("changed@example.test").setPostIds(ids);
    }

    protected UserSaveReqVO createRequest(Set<Long> ids) {
        return new UserSaveReqVO().setUsername("um04create" + ++sequence).setNickname("新用户")
                .setPassword("Synthetic@2026").setPostIds(ids);
    }

    protected void assertStorage(AdminUserDO user, Set<Long> ids) {
        assertEquals(ids, userMapper.selectById(user.getId()).getPostIds());
        assertEquals(ids, new HashSet<>(bindings(user).stream().map(UserPostDO::getPostId).toList()));
        assertEquals(ids.size(), bindings(user).size());
    }

    protected void assertProtectedFields(AdminUserDO original) {
        AdminUserDO actual = userMapper.selectById(original.getId());
        assertEquals(original.getPassword(), actual.getPassword());
        assertEquals(original.getPasswordUpdateTime(), actual.getPasswordUpdateTime());
        assertEquals(original.getPasswordCredentialStatus(), actual.getPasswordCredentialStatus());
        assertEquals(original.getStatus(), actual.getStatus());
        assertEquals(original.getLifecycleDocumentType(), actual.getLifecycleDocumentType());
        assertEquals(original.getLifecycleEffectiveTime(), actual.getLifecycleEffectiveTime());
        assertEquals(original.getLifecycleDeactivatedTime(), actual.getLifecycleDeactivatedTime());
    }

    protected List<Map<String, Object>> snapshot(AdminUserDO user) {
        List<Map<String, Object>> result = new ArrayList<>(jdbc.queryForList("SELECT id,post_ids,nickname,email,mobile,update_time,updater FROM system_users WHERE id=?", user.getId()));
        result.addAll(jdbc.queryForList("SELECT * FROM system_user_post WHERE user_id=? ORDER BY id", user.getId()));
        return result;
    }

    protected void assertRejectedUnchanged(AdminUserDO user, Set<Long> ids, ErrorCode code) {
        List<Map<String, Object>> before = snapshot(user);
        assertCode(code, () -> userService.updateUser(request(user, ids)));
        assertEquals(before, snapshot(user));
    }

    protected void assertIntegrityReadAndRemoval(AdminUserDO user) {
        List<Map<String, Object>> before = snapshot(user);
        assertCode(USER_POST_BINDING_INCONSISTENT, () -> userService.getUserForUpdate(user.getId()));
        assertEquals(before, snapshot(user));
        assertRejectedUnchanged(user, Set.of(), USER_POST_BINDING_INCONSISTENT);
    }

    protected void assertCode(ErrorCode expected, org.junit.jupiter.api.function.Executable action) {
        ServiceException exception = assertThrows(ServiceException.class, action);
        assertEquals(expected.getCode(), exception.getCode());
    }

    @Configuration(proxyBeanMethods = false)
    static class TenantConfiguration {
        @Bean
        static BeanPostProcessor actualTenantInterceptor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String name) {
                    if (bean instanceof MybatisPlusInterceptor interceptor) {
                        List<InnerInterceptor> ordered = new ArrayList<>();
                        ordered.add(new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties())));
                        ordered.addAll(interceptor.getInterceptors());
                        interceptor.setInterceptors(ordered);
                    }
                    return bean;
                }
            };
        }
    }
}
