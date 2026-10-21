package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccDistributionBatchDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccDistributionBatchMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.service.file.DccG02ControlledSignatureTimeTransactionTest.Fixture.wire;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** Actual G02 producer, actual Batch accept/requireFile and MyBatis rereads; no Batch service mock. */
class DccActivationEventTimeContractTest {
    private DccG02ControlledSignatureTimeTransactionTest.Fixture f;
    private DccDistributionBatchService batches;
    private DccDistributionBatchMapper batchRows;
    private GxpAuditService audit;

    @AfterEach
    void cleanup() {
        if (f != null) f.close();
    }

    @Test
    void immediateNonSecondClockMustPersistAcceptAndReplayOneMicrosecondActivationNotBeforeControl() throws Exception {
        setup(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE, 6);
        f.now.set(f.now.get().withNano(654321987));
        var expected = f.now.get().truncatedTo(ChronoUnit.MICROS);
        var signaturesBefore = signatureFacts();
        f.artifacts();

        assertDoesNotThrow(f::control, "actual Batch requireFile must accept the persisted activation fact");

        var file = f.files.selectById(20L);
        var event = activationEvent();
        assertAll("immediate activation must preserve the same representable instant end to end",
                () -> assertEquals(expected, file.getActivatedTime()),
                () -> assertEquals(expected, event.occurredAt()),
                () -> assertFalse(file.getActivatedTime().isBefore(file.getControlledTime())),
                () -> assertEquals(signaturesBefore, signatureFacts()),
                () -> assertEquals(1, f.events("ACTIVATED")));
        var batch = onlyBatch();
        assertEquals(expected, batch.getSourceOccurredAt());
        assertEquals("ROSTER_SOURCE_MISSING", batch.getErrorCode(),
                "real post-commit processing records the deliberately absent roster; no distribution PASS is simulated");
        assertEquals(batch.getId(), f.tx.execute(status -> batches.acceptActivatedEvent(event)));
        f.control();
        assertEquals(1, batchRows.selectList().size());
        assertEquals(1, f.events("ACTIVATED"));
        assertEquals(expected, f.files.selectById(20L).getActivatedTime());
    }

    @Test
    void delayedDueActivationMustNotPrecedeSelectedTimeAndMustPreserveOriginalSignedControl() throws Exception {
        setup(DccWorkflowDatePolicy.DELAYED, 6);
        f.confirmCommitted();
        f.artifacts();
        f.control();
        f.now.set(f.selected.minusNanos(1));
        assertEquals(Boolean.FALSE, f.tx.execute(status -> f.lifecycle.activateDue(20L)));
        assertTrue(batchRows.selectList().isEmpty());
        f.now.set(f.selected.plusNanos(123456789));
        var expected = f.now.get().truncatedTo(ChronoUnit.MICROS);
        var signaturesBefore = signatureFacts();

        assertEquals(Boolean.TRUE, assertDoesNotThrow(
                () -> f.tx.execute(status -> f.lifecycle.activateDue(20L))));

        var file = f.files.selectById(20L);
        assertAll("due activation must neither precede E nor replace original signed S",
                () -> assertEquals(expected, file.getActivatedTime()),
                () -> assertEquals(expected, activationEvent().occurredAt()),
                () -> assertEquals(expected, onlyBatch().getSourceOccurredAt()),
                () -> assertFalse(file.getActivatedTime().isBefore(f.selected)),
                () -> assertFalse(file.getActivatedTime().isBefore(file.getControlledTime())),
                () -> assertEquals(f.signedAt, file.getControlledTime()),
                () -> assertEquals(signaturesBefore, signatureFacts()));
        assertEquals(Boolean.FALSE, f.tx.execute(status -> f.lifecycle.activateDue(20L)));
        assertEquals(1, batchRows.selectList().size());
    }

    @Test
    void batchAcceptLateFailureRollsBackActivationAndSuccessfulRetryCreatesOnlyOneBatch() throws Exception {
        setup(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE, 6);
        f.now.set(f.now.get().withNano(0));
        f.artifacts();
        var before = f.rows();
        doThrow(new IllegalStateException("ACTUAL_BATCH_ACCEPT_AUDIT_FAILURE")).when(audit)
                .append(argThat(command -> "dcc.distribution.batch.accept".equals(command.getOperationId())));

        var failure = assertThrows(IllegalStateException.class, f::control);

        assertEquals("ACTUAL_BATCH_ACCEPT_AUDIT_FAILURE", failure.getMessage());
        assertEquals(before, f.rows());
        assertTrue(batchRows.selectList().isEmpty());
        assertEquals("ACTIVE", f.files.selectById(10L).getStatus());
        reset(audit);
        f.control();
        f.control();
        assertEquals(1, f.events("ACTIVATED"));
        assertEquals(1, batchRows.selectList().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FILE_ACTIVATED_TIME", "EVENT_TIME", "PERSISTED_EVENT_TIME"})
    void wrongActivationFactStillFailsExactConsumerComparison(String mismatch) throws Exception {
        setup(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE, 6);
        f.now.set(f.now.get().withNano(0));
        f.artifacts();
        f.control();
        var event = activationEvent();
        var originalBatch = onlyBatch();
        String expectedMessage = "自动下发缺少同版本受控生效事实";
        if ("FILE_ACTIVATED_TIME".equals(mismatch)) {
            f.jdbc.update("UPDATE dcc_controlled_file SET activated_time=? WHERE id=20", event.occurredAt().plusNanos(1000));
        } else if ("EVENT_TIME".equals(mismatch)) {
            event = new DccControlledFileLifecycleEvent(event.eventKey(), event.eventType(), event.tenantId(), event.masterId(),
                    event.controlledFileId(), event.previousActiveFileId(), event.versionNo(), event.approvalProcessInstanceId(),
                    event.occurredAt().plusNanos(1000));
        } else {
            f.jdbc.update("UPDATE dcc_workflow_lifecycle_event SET occurred_at=? WHERE event_type='ACTIVATED'",
                    event.occurredAt().plusNanos(1000));
            expectedMessage = "自动下发持久事件载荷与真实版本不一致";
        }
        var wrong = event;

        var failure = assertThrows(IllegalStateException.class,
                () -> f.tx.execute(status -> batches.acceptActivatedEvent(wrong)));

        assertEquals(expectedMessage, failure.getMessage());
        assertEquals(1, batchRows.selectList().size());
        assertEquals(originalBatch.getSourceOccurredAt(), onlyBatch().getSourceOccurredAt());
        assertEquals(originalBatch.getEventPayloadHash(), onlyBatch().getEventPayloadHash());
    }

    @Test
    void oldSecondPrecisionColumnCannotRepresentTheMicrosecondActivationContract() throws Exception {
        setup(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE, 0);
        f.now.set(f.now.get().withNano(654321000));
        assertTrue(f.now.get().truncatedTo(ChronoUnit.SECONDS)
                .isBefore(f.now.get().truncatedTo(ChronoUnit.MICROS)), "flooring activation to seconds would precede control");
        f.artifacts();
        var before = f.rows();

        var failure = assertThrows(IllegalStateException.class, f::control);

        assertEquals("自动下发缺少同版本受控生效事实", failure.getMessage());
        assertEquals(before, f.rows());
        assertTrue(batchRows.selectList().isEmpty());
    }

    private void setup(String mode, int activatedPrecision) throws Exception {
        f = new DccG02ControlledSignatureTimeTransactionTest.Fixture(mode);
        // Only private H2. Precision 0 models current formal column; precision 6 models Root-owned forward migration.
        f.jdbc.execute("ALTER TABLE dcc_controlled_file ALTER COLUMN activated_time TIMESTAMP(" + activatedPrecision + ")");
        f.table(DccDistributionBatchDO.class);
        f.jdbc.execute("CREATE UNIQUE INDEX activation_batch_event ON dcc_distribution_batch(tenant_id,event_key)");
        f.jdbc.execute("CREATE UNIQUE INDEX activation_batch_version ON dcc_distribution_batch(tenant_id,controlled_file_id,approval_process_instance_id)");
        f.mapperSessions.getConfiguration().addMapper(DccDistributionBatchMapper.class);
        batchRows = f.mapperSessions.getMapper(DccDistributionBatchMapper.class);
        audit = mock(GxpAuditService.class);
        batches = new DccDistributionBatchService();
        var rosterProviders = new StaticListableBeanFactory();
        wire(batches, "controlledFileMapper", f.files, "batchMapper", batchRows, "jdbcTemplate", f.jdbc,
                "transactionManager", f.transactionManager, "gxpAuditService", audit,
                "rosterSources", rosterProviders.getBeanProvider(DccDistributionRosterSource.class));
        wire(f.lifecycle, "distributionBatchService", batches);
        f.jdbc.update("UPDATE dcc_controlled_file SET distribution_department_ids_json='[\"31\"]' WHERE id=20");
    }

    private DccDistributionBatchDO onlyBatch() {
        var rows = batchRows.selectList();
        assertEquals(1, rows.size());
        return rows.get(0);
    }

    private DccControlledFileLifecycleEvent activationEvent() {
        return f.jdbc.queryForObject("SELECT * FROM dcc_workflow_lifecycle_event WHERE event_type='ACTIVATED'", (rs, index) ->
                new DccControlledFileLifecycleEvent(rs.getString("event_key"), rs.getString("event_type"),
                        rs.getLong("tenant_id"), rs.getLong("master_id"), rs.getLong("controlled_file_id"),
                        rs.getObject("previous_active_file_id", Long.class), rs.getString("version_no"),
                        rs.getString("approval_process_instance_id"), rs.getObject("occurred_at", LocalDateTime.class)));
    }

    private List<java.util.Map<String, Object>> signatureFacts() {
        return f.jdbc.queryForList("SELECT id,signed_at,evidence_hash FROM dcc_controlled_file_signature ORDER BY id");
    }
}
