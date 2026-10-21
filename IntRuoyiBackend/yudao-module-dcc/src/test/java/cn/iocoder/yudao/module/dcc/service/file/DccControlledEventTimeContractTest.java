package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccLatestControlledFileResolverImpl;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.ControlledEvent;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRemediationService;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStore;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.module.dcc.service.file.DccG02ControlledSignatureTimeTransactionTest.Fixture.wire;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Reuses actual G02 Flowable/HMAC/Mapper fixture and D relation DB fixture. No runtime database. */
class DccControlledEventTimeContractTest {
    private DccG02ControlledSignatureTimeTransactionTest.Fixture lifecycleFixture;
    private DccRelationRemediationTransactionTest relationFixture;
    private DccControlledFileDO source;

    @AfterEach
    void cleanup() {
        if (lifecycleFixture != null) lifecycleFixture.close();
        if (relationFixture != null) relationFixture.cleanup();
    }

    @Test
    void delayedProducerMustPublishAndPersistOriginalMicrosecondSignedTimeInsteadOfProcessingNow() throws Exception {
        lifecycleFixture = new DccG02ControlledSignatureTimeTransactionTest.Fixture();
        var f = lifecycleFixture;
        f.now.set(f.now.get().withNano(987654321));
        f.confirmCommitted();
        f.artifacts();
        var observed = captureControlEvent(f);

        f.control();

        var file = f.files.selectById(20L);
        assertNotEquals(f.signedAt, f.now.get());
        assertEquals(f.signedAt, file.getControlledTime());
        assertEquals(f.signedAt, f.signatures.selectById(f.confirmSignatureId).getSignedAt(),
                "source electronic signature must never be rounded or replaced");
        assertNotNull(observed.get());
        assertAll("CONTROLLED event must carry the exact committed signature control fact",
                () -> assertEquals(f.signedAt, observed.get().occurredAt()),
                () -> assertEquals(f.signedAt, persistedControlledTime(f)),
                () -> assertEquals(file.getControlledTime(), observed.get().occurredAt()));
    }

    @Test
    void immediateProducerMustPreserveMicrosecondsAndRemoveOnlyUnrepresentableSubmicroseconds() throws Exception {
        lifecycleFixture = new DccG02ControlledSignatureTimeTransactionTest.Fixture(
                DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE);
        var f = lifecycleFixture;
        f.now.set(f.now.get().withNano(654321987));
        var expected = f.now.get().truncatedTo(ChronoUnit.MICROS);
        var originalSignatures = f.jdbc.queryForList("SELECT id,signed_at,evidence_hash FROM dcc_controlled_file_signature ORDER BY id");
        f.artifacts();
        var observed = captureControlEvent(f);

        f.control();

        assertNotEquals(0, expected.getNano());
        assertNotNull(observed.get());
        assertAll("new immediate control uses a single representable microsecond fact",
                () -> assertEquals(expected, f.files.selectById(20L).getControlledTime()),
                () -> assertEquals(expected, observed.get().occurredAt()),
                () -> assertEquals(expected, persistedControlledTime(f)),
                () -> assertEquals(originalSignatures,
                        f.jdbc.queryForList("SELECT id,signed_at,evidence_hash FROM dcc_controlled_file_signature ORDER BY id")));
    }

    @Test
    void microsecondConsumerAcceptsExactRegisteredFactAndReplaysWithoutChangingSavedTime() throws Exception {
        var r = relationFixture();
        r.save(new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement(
                20L, 8L, r.time.plusDays(3)));

        assertDoesNotThrow(r::record, "a precisely registered microsecond event must be accepted");
        var first = r.jdbc.queryForList("SELECT * FROM dcc_relation_controlled_event ORDER BY id");
        r.record();

        assertEquals(first, r.jdbc.queryForList("SELECT * FROM dcc_relation_controlled_event ORDER BY id"));
        assertEquals(r.event.controlledAt(), r.jdbc.queryForObject(
                "SELECT controlled_at FROM dcc_relation_controlled_event", LocalDateTime.class));
        assertEquals(1, r.count("dcc_relation_controlled_event"));
        assertEquals(1, r.count("dcc_relation_remediation_task"));
        assertEquals(1, r.count("dcc_relation_notification_outbox"));
        assertEquals(r.time.plusDays(3), r.jdbc.queryForObject(
                "SELECT due_at FROM dcc_relation_remediation_task", LocalDateTime.class));
        r.event = new ControlledEvent(1L, 10L, 100L, "round-1", "controlled-100-round-1",
                r.event.controlledAt().plusNanos(1000));
        var conflict = assertThrows(DccRelationFailure.class, r::record);
        assertEquals("DCC_CONTROLLED_EVENT_REPLAY_CONFLICT", conflict.getMessage());
        assertEquals(first, r.jdbc.queryForList("SELECT * FROM dcc_relation_controlled_event ORDER BY id"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FILE_TIME", "WORKFLOW_EVENT_TIME"})
    void wrongMicrosecondControlFactStillFailsExactIdentityOrRegistration(String mismatch) throws Exception {
        var r = relationFixture();
        String expectedFailure;
        if ("FILE_TIME".equals(mismatch)) {
            source.setControlledTime(source.getControlledTime().plusNanos(1000));
            expectedFailure = "DCC_CONTROLLED_EVENT_IDENTITY_INVALID";
        } else {
            r.jdbc.update("UPDATE dcc_workflow_lifecycle_event SET occurred_at=?", r.event.controlledAt().plusNanos(1000));
            expectedFailure = "DCC_CONTROLLED_EVENT_NOT_REGISTERED";
        }

        var failure = assertThrows(DccRelationFailure.class, r::record);

        assertEquals(expectedFailure, failure.getMessage());
        assertEquals(0, r.count("dcc_relation_controlled_event"));
        assertEquals(0, r.count("dcc_relation_remediation_task"));
        verifyNoInteractions(r.scheduler);
    }

    @Test
    void submicrosecondConsumerEventIsRejectedBeforeAnyRelationWrite() throws Exception {
        var r = relationFixture();
        r.event = new ControlledEvent(1L, 10L, 100L, "round-1", "controlled-100-round-1",
                r.event.controlledAt().plusNanos(1));

        var failure = assertThrows(DccRelationFailure.class, r::record);

        assertEquals("DCC_CONTROLLED_EVENT_INVALID", failure.getMessage());
        assertEquals(0, r.count("dcc_relation_controlled_event"));
        verifyNoInteractions(r.scheduler);
    }

    private AtomicReference<DccControlledFileLifecycleEvent> captureControlEvent(
            DccG02ControlledSignatureTimeTransactionTest.Fixture f) {
        var observed = new AtomicReference<DccControlledFileLifecycleEvent>();
        wire(f.lifecycle, "eventPublisher", (ApplicationEventPublisher) event -> {
            if (event instanceof DccControlledFileLifecycleEvent fact && "CONTROLLED".equals(fact.eventType()))
                observed.set(fact);
        });
        return observed;
    }

    private static LocalDateTime persistedControlledTime(DccG02ControlledSignatureTimeTransactionTest.Fixture f) {
        return f.jdbc.queryForObject("SELECT occurred_at FROM dcc_workflow_lifecycle_event WHERE event_type='CONTROLLED'",
                LocalDateTime.class);
    }

    private DccRelationRemediationTransactionTest relationFixture() throws Exception {
        var r = new DccRelationRemediationTransactionTest();
        relationFixture = r;
        r.setup();
        // Private H2 projection of the two microsecond columns owned by Root's separate migration.
        r.jdbc.execute("ALTER TABLE dcc_relation_controlled_event ALTER COLUMN controlled_at TIMESTAMP(6) NOT NULL");
        r.jdbc.execute("CREATE TABLE dcc_workflow_lifecycle_event(tenant_id BIGINT,event_key VARCHAR(128),"
                + "event_type VARCHAR(32),master_id BIGINT,controlled_file_id BIGINT,"
                + "approval_process_instance_id VARCHAR(64),occurred_at TIMESTAMP(6))");
        var time = r.time.withNano(123456000);
        r.event = new ControlledEvent(1L, 10L, 100L, "round-1", "controlled-100-round-1", time);
        source = DccControlledFileDO.builder().id(100L).tenantId(1L).masterId(10L).dccProjectCodeId(9L)
                .fileNumber("N").fileName("源.pdf").versionNo("A/1").status("ACTIVE")
                .controlledTime(time).activatedTime(time).processInstanceId("round-1").build();
        var files = mock(DccControlledFileMapper.class);
        when(files.selectById(100L)).thenReturn(source);
        when(files.selectByIdAndTenantForUpdate(1L, 100L)).thenReturn(source);
        var masters = mock(DccControlledFileMasterMapper.class);
        when(masters.selectByIdForUpdate(10L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(10L).tenantId(1L).latestControlledFileId(100L).build());
        var exact = new DccLatestControlledFileResolverImpl();
        wire(exact, "fileMapper", files, "masterMapper", masters, "jdbc", r.jdbc);
        r.service = new DccRelationRemediationService(
                new DccRelationStore(r.jdbc, mock(GxpAuditService.class)), exact, r.access, r.scheduler);
        r.jdbc.update("INSERT INTO dcc_workflow_lifecycle_event VALUES(?,?,?,?,?,?,?)",
                1L, r.event.eventKey(), "CONTROLLED", 10L, 100L, "round-1", time);
        return r;
    }
}
