package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.ApprovalAuthorityRevisionDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.ApprovalAuthorityRevisionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real transaction-resource boundaries, with mapper boundaries supplied explicitly. */
class ApprovalAuthorityServiceTest {
    private final ApprovalAuthorityRevisionMapper mapper = mock(ApprovalAuthorityRevisionMapper.class);
    private final ApprovalAuthorityService service = new ApprovalAuthorityService(mapper,
            Clock.fixed(Instant.parse("2026-10-10T02:00:00Z"), ZoneOffset.UTC));
    private final DataSourceTransactionManager manager = new DataSourceTransactionManager(new DriverManagerDataSource(
            "jdbc:h2:mem:authority_context_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    @BeforeEach void tenant() {
        TenantContextHolder.setTenantId(7L);
        ApprovalAuthorityRevisionDO row = new ApprovalAuthorityRevisionDO(); row.setTenantId(7L); row.setRevision(3L);
        when(mapper.selectForUpdate(7L)).thenReturn(row);
        when(mapper.increment(anyLong(), anyLong(), any())).thenReturn(1);
    }
    @AfterEach void noLeak() {
        TenantContextHolder.clear(); assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
    }
    private TransactionTemplate transaction(int propagation) {
        var template = new TransactionTemplate(manager); template.setPropagationBehavior(propagation); return template;
    }
    @Test void missingInitializationIsNotCreatedByRequest() {
        when(mapper.selectForUpdate(7L)).thenReturn(null);
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status ->
                assertThrows(IllegalStateException.class, () -> service.lockTenantApprovalAuthority(7L)));
        verify(mapper, never()).increment(anyLong(), anyLong(), any());
    }
    @Test void incrementRequiresCurrentTransactionLockAndExpectedRevision() {
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            assertThrows(IllegalStateException.class, () -> service.incrementLockedAuthorityRevision(7L, 3L));
            var locked = service.lockTenantApprovalAuthority(7L); assertEquals(3L, locked.revision());
            assertThrows(IllegalStateException.class, () -> service.incrementLockedAuthorityRevision(7L, 2L));
            service.incrementLockedAuthorityRevision(7L, 3L);
            assertThrows(IllegalStateException.class, () -> service.incrementLockedAuthorityRevision(7L, 3L));
            service.incrementLockedAuthorityRevision(7L, 4L);
        });
    }
    @Test void independentTransactionCannotBorrowConfigurationLock() {
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            service.lockTenantApprovalAuthority(7L);
            transaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW).executeWithoutResult(inner ->
                    assertThrows(IllegalStateException.class, () -> service.incrementLockedAuthorityRevision(7L, 3L)));
            service.incrementLockedAuthorityRevision(7L, 3L);
        });
    }
    @Test void tenantAndPhysicalTransactionAreRequired() {
        assertThrows(IllegalStateException.class, () -> service.lockTenantApprovalAuthority(7L));
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status ->
                assertThrows(IllegalArgumentException.class, () -> service.lockTenantApprovalAuthority(8L)));
        verifyNoInteractions(mapper);
    }
    @Test void unexpectedAffectedCountFailsInsteadOfClaimingNewRevision() {
        when(mapper.increment(anyLong(), anyLong(), any())).thenReturn(0);
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            service.lockTenantApprovalAuthority(7L);
            assertThrows(IllegalStateException.class, () -> service.incrementLockedAuthorityRevision(7L, 3L));
        });
    }
}
