package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GxpAuditTransactionContextBoundaryTest {
    private final DataSourceTransactionManager manager = new DataSourceTransactionManager(
            new DriverManagerDataSource("jdbc:h2:mem:context_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));

    @AfterEach
    void noTransactionLeak() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
        assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
    }

    private TransactionTemplate transaction(int propagation) {
        var template = new TransactionTemplate(manager);
        template.setPropagationBehavior(propagation);
        return template;
    }

    @Test
    void nestedNewTransactionHasOwnIdentityAndRestoresOuterIdentity() {
        var ids = new ArrayList<String>();
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            ids.add(GxpAuditTransactionContext.requireTransactionId());
            transaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW).executeWithoutResult(inner -> {
                ids.add(GxpAuditTransactionContext.requireTransactionId());
                assertEquals(ids.get(1), GxpAuditTransactionContext.requireTransactionId());
            });
            ids.add(GxpAuditTransactionContext.requireTransactionId());
        });
        assertNotEquals(ids.get(0), ids.get(1), "different physical transactions need different audit identity");
        assertEquals(ids.get(0), ids.get(2), "resuming outer transaction must restore its audit identity");
    }

    @Test
    void rolledBackInnerTransactionDoesNotEraseOuterIdentity() {
        var outer = new AtomicReference<String>();
        var inner = new AtomicReference<String>();
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            outer.set(GxpAuditTransactionContext.requireTransactionId());
            assertThrows(IllegalStateException.class, () ->
                    transaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW).executeWithoutResult(nested -> {
                        inner.set(GxpAuditTransactionContext.requireTransactionId());
                        throw new IllegalStateException("owned test rollback");
                    }));
            assertEquals(outer.get(), GxpAuditTransactionContext.requireTransactionId());
        });
        assertNotEquals(outer.get(), inner.get());
    }

    @Test
    void afterCommitNewTransactionsEachGetOwnIdentity() {
        List<String> ids = new ArrayList<>();
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            ids.add(GxpAuditTransactionContext.requireTransactionId());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (int i = 0; i < 2; i++) {
                        transaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW).executeWithoutResult(nested ->
                                ids.add(GxpAuditTransactionContext.requireTransactionId()));
                    }
                    assertEquals(ids.get(0), GxpAuditTransactionContext.requireTransactionId());
                }
            });
        });
        assertEquals(3, ids.stream().distinct().count(), "parent and afterCommit physical transactions must differ");
    }

    @Test
    void resumeConflictDoesNotReplaceTheForeignResource() {
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            String ownedId = GxpAuditTransactionContext.requireTransactionId();
            var synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);
            synchronization.suspend();
            TransactionSynchronizationManager.bindResource(GxpAuditTransactionContext.class, "foreign-id");
            try {
                assertThrows(IllegalStateException.class, synchronization::resume);
                assertEquals("foreign-id", TransactionSynchronizationManager.getResource(GxpAuditTransactionContext.class));
            } finally {
                TransactionSynchronizationManager.unbindResourceIfPossible(GxpAuditTransactionContext.class);
                TransactionSynchronizationManager.bindResource(GxpAuditTransactionContext.class, ownedId);
            }
        });
    }

    @Test
    void completionDoesNotRemoveAnotherTransactionsResource() {
        transaction(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            String ownedId = GxpAuditTransactionContext.requireTransactionId();
            var synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);
            synchronization.suspend();
            TransactionSynchronizationManager.bindResource(GxpAuditTransactionContext.class, "foreign-id");
            try {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
                assertEquals("foreign-id", TransactionSynchronizationManager.getResource(GxpAuditTransactionContext.class));
            } finally {
                TransactionSynchronizationManager.unbindResourceIfPossible(GxpAuditTransactionContext.class);
                TransactionSynchronizationManager.bindResource(GxpAuditTransactionContext.class, ownedId);
            }
        });
    }

    @Test
    void requiredTransactionSharesIdentityAndNextOuterTransactionDoesNot() {
        String first = transaction(TransactionDefinition.PROPAGATION_REQUIRED).execute(status -> {
            String id = GxpAuditTransactionContext.requireTransactionId();
            String nested = transaction(TransactionDefinition.PROPAGATION_REQUIRED).execute(inner ->
                    GxpAuditTransactionContext.requireTransactionId());
            assertEquals(id, nested);
            return id;
        });
        String second = transaction(TransactionDefinition.PROPAGATION_REQUIRED).execute(status ->
                GxpAuditTransactionContext.requireTransactionId());
        assertNotEquals(first, second);
        assertThrows(IllegalStateException.class, GxpAuditTransactionContext::requireTransactionId);
    }
}
