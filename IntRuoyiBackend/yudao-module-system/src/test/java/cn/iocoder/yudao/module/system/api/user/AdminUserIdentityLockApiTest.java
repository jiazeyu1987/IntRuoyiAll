package cn.iocoder.yudao.module.system.api.user;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import cn.iocoder.yudao.module.system.service.signature.SignatureIdentityLockContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Verifies the API's real transaction boundary; repository locking remains a mapper boundary. */
class AdminUserIdentityLockApiTest {
    private final AdminUserMapper users = mock(AdminUserMapper.class);
    private final UserPostMapper posts = mock(UserPostMapper.class);
    private final AdminUserApiImpl api = new AdminUserApiImpl();
    private final TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(
            new DriverManagerDataSource("jdbc:h2:mem:identity_api_" + UUID.randomUUID()
                    + ";DB_CLOSE_DELAY=-1", "sa", "")));
    private AdminUserDO user;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(7L);
        ReflectionTestUtils.setField(api, "userMapper", users);
        ReflectionTestUtils.setField(api, "userPostMapper", posts);
        user = new AdminUserDO();
        user.setId(12L); user.setTenantId(7L); user.setStatus(1); user.setNickname("人员甲");
        when(users.selectPermissionSubjectForUpdate(7L, 12L)).thenReturn(user);
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void noTransactionCannotPerformLockQuery() {
        assertThrows(IllegalStateException.class, () -> api.lockSignatureIdentity(12L));
        verifyNoInteractions(users, posts);
    }

    @Test void disabledIdentityStillLocksForQualificationChangeWithoutRelationshipReads() {
        transaction.executeWithoutResult(status -> {
            assertThrows(IllegalStateException.class,
                    () -> SignatureIdentityLockContext.requireSystemIdentityLock(12L));
            var locked = api.lockSignatureIdentity(12L);
            SignatureIdentityLockContext.requireSystemIdentityLock(12L);
            assertEquals(12L, locked.getId());
            assertEquals(1, locked.getStatus());
            assertEquals("人员甲", locked.getNickname());
        });
        verify(users).selectPermissionSubjectForUpdate(7L, 12L);
        verifyNoInteractions(posts);
    }

    @Test void separateAuthenticationTransactionCannotBorrowOuterIdentityProof() {
        var inner = new TransactionTemplate(transaction.getTransactionManager());
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.executeWithoutResult(status -> {
            api.lockSignatureIdentity(12L);
            inner.executeWithoutResult(innerStatus -> assertThrows(IllegalStateException.class,
                    () -> SignatureIdentityLockContext.requireSystemIdentityLock(12L)));
            SignatureIdentityLockContext.requireSystemIdentityLock(12L);
        });
        transaction.executeWithoutResult(status -> assertThrows(IllegalStateException.class,
                () -> SignatureIdentityLockContext.requireSystemIdentityLock(12L)));
    }

    @Test void differentTenantOrDeletedIdentityCannotBeReturned() {
        transaction.executeWithoutResult(status -> {
            user.setTenantId(8L);
            assertThrows(IllegalStateException.class, () -> api.lockSignatureIdentity(12L));
            user.setTenantId(7L); user.setDeleted(true);
            assertThrows(IllegalStateException.class, () -> api.lockSignatureIdentity(12L));
        });
        verifyNoInteractions(posts);
    }
}
