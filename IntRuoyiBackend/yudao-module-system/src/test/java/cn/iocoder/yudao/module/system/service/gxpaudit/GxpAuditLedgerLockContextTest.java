package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class GxpAuditLedgerLockContextTest {
    private final DataSourceTransactionManager manager = new DataSourceTransactionManager(new DriverManagerDataSource(
            "jdbc:h2:mem:ledger_context_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    private TransactionTemplate transaction(int propagation) {
        var template = new TransactionTemplate(manager); template.setPropagationBehavior(propagation); return template;
    }
    @AfterEach void noLeakedProof() { assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty()); }
    @Test void transactionAnnotationAloneDoesNotProveTenantLock() {
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            GxpAuditTransactionContext.requireTransactionId();
            assertThrows(IllegalStateException.class, () -> GxpAuditLedgerLockContext.requireHeld(7L));
            GxpAuditLedgerLockContext.acquired(7L); GxpAuditLedgerLockContext.requireHeld(7L);
            assertThrows(IllegalStateException.class, () -> GxpAuditLedgerLockContext.requireHeld(8L));
        });
        assertThrows(IllegalStateException.class, () -> GxpAuditLedgerLockContext.requireHeld(7L));
    }
    @Test void nestedIndependentTransactionCannotBorrowOuterLockProof() {
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            GxpAuditLedgerLockContext.acquired(7L);
            transaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW).executeWithoutResult(inner -> {
                assertThrows(IllegalStateException.class, () -> GxpAuditLedgerLockContext.requireHeld(7L));
                GxpAuditLedgerLockContext.acquired(8L); GxpAuditLedgerLockContext.requireHeld(8L);
                assertThrows(IllegalStateException.class, () -> GxpAuditLedgerLockContext.requireHeld(7L));
            });
            GxpAuditLedgerLockContext.requireHeld(7L);
            assertThrows(IllegalStateException.class, () -> GxpAuditLedgerLockContext.requireHeld(8L));
        });
    }
    @Test void reservedAuditIdentityCannotBeReusedOrRelabeled() {
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            GxpAuditLedgerLockContext.acquired(7L);
            Long id = GxpAuditLedgerLockContext.reserve(7L, "APPROVAL_CENTER_REVIEW_COMMIT", "receipt-1", "executor:receipt-1", "key-1");
            var command = GxpAuditCommand.builder().reservedEventId(id).operationId("APPROVAL_CENTER_REVIEW_COMMIT")
                    .subjectId("receipt-1").sourceLocator("executor:receipt-1").idempotencyKey("key-1").build();
            command.setSubjectId("receipt-2");
            assertThrows(IllegalStateException.class, () -> GxpAuditLedgerLockContext.consume(7L, command));
            command.setSubjectId("receipt-1");
            transaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW).executeWithoutResult(inner -> {
                GxpAuditLedgerLockContext.acquired(7L);
                assertThrows(IllegalStateException.class, () -> GxpAuditLedgerLockContext.consume(7L, command));
            });
            GxpAuditLedgerLockContext.consume(7L, command);
            assertThrows(IllegalStateException.class, () -> GxpAuditLedgerLockContext.consume(7L, command));
        });
    }
}
