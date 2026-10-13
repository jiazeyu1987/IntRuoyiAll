package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Real append logic with scoped server-time sampling and capture-only mapper doubles.
 * Pure new-write value contract: no JDBC, MySQL, commit/rollback or historical provenance proof.
 */
class GxpAuditWriterTimePrecisionTest {

    private final GxpAuditServiceImpl writer = new GxpAuditServiceImpl();
    private GxpAuditEventMapper events;
    private GxpAuditEventRelationMapper relations;
    private GxpAuditLedgerSequenceMapper ledger;
    private GxpAuditPolicyOperationMapper policies;
    private GxpAuditPolicyActivationMapper activations;
    private GxpAuditEventDO insertedEvent;
    private GxpAuditEventRelationDO insertedRelation;

    @BeforeEach
    void setUp() {
        LoginUser actor = new LoginUser();
        actor.setId(21L);
        actor.setTenantId(11L);
        actor.setInfo(Map.of("username", "precision-actor", LoginUser.INFO_KEY_NICKNAME, "precision-actor"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        // Fixed correlation avoids a random fixture identity; the real context still checks active transaction.
        TransactionSynchronizationManager.bindResource(GxpAuditTransactionContext.class, "precision-transaction");

        events = mock(GxpAuditEventMapper.class);
        relations = mock(GxpAuditEventRelationMapper.class);
        ledger = mock(GxpAuditLedgerSequenceMapper.class);
        policies = mock(GxpAuditPolicyOperationMapper.class);
        activations = mock(GxpAuditPolicyActivationMapper.class);
        ReflectionTestUtils.setField(writer, "auditEventMapper", events);
        ReflectionTestUtils.setField(writer, "eventRelationMapper", relations);
        ReflectionTestUtils.setField(writer, "ledgerSequenceMapper", ledger);
        ReflectionTestUtils.setField(writer, "policyOperationMapper", policies);
        ReflectionTestUtils.setField(writer, "policyActivationMapper", activations);

        GxpAuditLedgerSequenceDO sequence = new GxpAuditLedgerSequenceDO();
        sequence.setTenantId(11L);
        sequence.setNextLedgerSequence(1L);
        when(ledger.selectByTenantIdForUpdate(11L)).thenReturn(sequence);
        when(ledger.updateById(any(GxpAuditLedgerSequenceDO.class))).thenReturn(1);
        GxpAuditPolicyActivationDO activation = new GxpAuditPolicyActivationDO();
        activation.setTenantId(11L);
        activation.setPolicyVersion("precision-v1");
        when(activations.selectLatestForUpdate(11L)).thenReturn(activation);
        GxpAuditPolicyOperationDO policy = new GxpAuditPolicyOperationDO();
        policy.setTenantId(11L);
        policy.setPolicyVersion("precision-v1");
        policy.setOperationId("signature.record.create");
        policy.setDomain("SIGNATURE");
        policy.setSubjectType("SIGNATURE_RECORD");
        policy.setActionType("CREATE");
        policy.setReasonPolicy("REQUIRED_CATEGORY_AND_TEXT");
        policy.setSignaturePolicy("NOT_REQUIRED");
        policy.setActive(true);
        policy.setApplicability("GXP");
        when(policies.selectByPolicyVersionForUpdate(11L, "precision-v1", "signature.record.create"))
                .thenReturn(policy);
        when(events.insert(any(GxpAuditEventDO.class))).thenAnswer(call -> {
            GxpAuditEventDO parameter = call.getArgument(0);
            parameter.setId(101L);
            insertedEvent = new GxpAuditEventDO();
            BeanUtils.copyProperties(parameter, insertedEvent);
            return 1;
        });
        when(relations.insert(any(GxpAuditEventRelationDO.class))).thenAnswer(call -> {
            insertedRelation = new GxpAuditEventRelationDO();
            BeanUtils.copyProperties(call.getArgument(0), insertedRelation);
            return 1;
        });
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.unbindResource(GxpAuditTransactionContext.class);
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "{index}: UTC {0} becomes exactly {1}")
    @CsvSource({
            "2026-10-08T12:34:56.123456789, 2026-10-08T12:34:56.123",
            "2026-10-08T12:34:56.123499500, 2026-10-08T12:34:56.123",
            "2026-10-08T12:34:56.123999900, 2026-10-08T12:34:56.123",
            "2026-10-08T12:34:56.999499900, 2026-10-08T12:34:56.999",
            "2026-10-08T12:34:59.999999900, 2026-10-08T12:34:59.999",
            "2026-10-08T23:59:59.999999900, 2026-10-08T23:59:59.999",
            "2026-10-08T12:34:56.000000900, 2026-10-08T12:34:56",
            "2026-10-08T12:34:56.123000000, 2026-10-08T12:34:56.123",
            "2026-10-08T12:34:56.000000000, 2026-10-08T12:34:56"
    })
    void newEventUsesOneUtcMillisecondValueBeforeCanonicalHashAndRelation(String sampleText, String expectedText) {
        // Construct samples before the static scope so parsing cannot become a mocked prerequisite.
        LocalDateTime sample = LocalDateTime.parse(sampleText);
        LocalDateTime expected = LocalDateTime.parse(expectedText);
        GxpAuditCommand input = command();
        String inputBefore = JsonUtils.toJsonString(input);
        GxpAuditAppendResult result;
        try (ScopedTime time = new ScopedTime(sample, 1)) {
            result = writer.append(input);
            time.verifyExpectedSampling();
        }
        assertNotNull(insertedEvent);
        assertNotNull(insertedRelation);
        String expectedCanonical = canonicalWithTime(insertedEvent.getCanonicalEventJson(), expected);
        String expectedHash = DigestUtil.sha256Hex(expectedCanonical);
        assertAll("one exact UTC millisecond value before hashing and mapper calls",
                () -> assertEquals(expected, insertedEvent.getServerOccurredAt(), "event time must truncate to milliseconds"),
                () -> assertEquals(0, insertedEvent.getServerOccurredAt().getNano() % 1_000_000, "no sub-millisecond nanos"),
                () -> assertEquals(expected.toString(), JsonUtils.parseObject(insertedEvent.getCanonicalEventJson(),
                        "serverOccurredAt", String.class), "canonical must contain the millisecond value"),
                () -> assertEquals(expectedCanonical, insertedEvent.getCanonicalEventJson(), "canonical format stays unchanged"),
                () -> assertEquals(expectedHash, insertedEvent.getEventHash(), "hash must cover the millisecond canonical"),
                () -> assertEquals(expectedHash, result.eventHash(), "result hash must match captured event hash"),
                () -> assertEquals(expected, insertedRelation.getCreatedAtUtc(), "relation must use the same millisecond value"),
                () -> assertEquals(insertedEvent.getId(), insertedRelation.getEventId()),
                () -> assertEquals(11L, insertedRelation.getTenantId()),
                () -> assertEquals(inputBefore, JsonUtils.toJsonString(input), "caller input remains unchanged"),
                () -> assertFalse(result.replayed()));
        verify(ledger).updateById(argThat((GxpAuditLedgerSequenceDO row) ->
                row.getTenantId().equals(11L) && row.getNextLedgerSequence().equals(2L)));
        verify(events).insert(any(GxpAuditEventDO.class));
        verify(relations).insert(any(GxpAuditEventRelationDO.class));
    }

    @Test
    void replayReturnsOriginalFactsWithoutSamplingTimeOrWriting() {
        LocalDateTime initialSample = LocalDateTime.parse("2026-10-08T12:34:56.123");
        LocalDateTime originalTime = LocalDateTime.parse("2026-10-07T23:59:59.999499900");
        GxpAuditCommand input = command();
        String inputBefore = JsonUtils.toJsonString(input);
        try (ScopedTime time = new ScopedTime(initialSample, 1)) {
            writer.append(input);
            time.verifyExpectedSampling();
        }
        // A separate synthetic existing fact tests preservation, not historical storage applicability.
        GxpAuditEventDO original = new GxpAuditEventDO();
        BeanUtils.copyProperties(insertedEvent, original);
        original.setServerOccurredAt(originalTime);
        original.setCanonicalEventJson(canonicalWithTime(original.getCanonicalEventJson(), originalTime));
        original.setEventHash(DigestUtil.sha256Hex(original.getCanonicalEventJson()));
        String originalBefore = JsonUtils.toJsonString(original);
        when(events.selectByIdempotencyKeyForUpdate(11L, input.getIdempotencyKey())).thenReturn(original);
        clearInvocations(events, relations, ledger, policies, activations);

        GxpAuditAppendResult result;
        try (ScopedTime time = new ScopedTime(null, 0)) {
            result = writer.append(input);
            time.verifyExpectedSampling();
        }
        assertTrue(result.replayed());
        assertEquals(original.getId(), result.eventId());
        assertEquals(original.getLedgerSequence(), result.ledgerSequence());
        assertEquals(original.getEventHash(), result.eventHash());
        assertEquals(originalBefore, JsonUtils.toJsonString(original));
        assertEquals(inputBefore, JsonUtils.toJsonString(input));
        assertNoWrites();
        verify(events, never()).selectLatestByTenantForUpdate(anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"policy", "watermark", "transaction"})
    void missingPrerequisiteStillFailsBeforeTimeSamplingAndWrites(String missing) {
        if (missing.equals("policy")) {
            when(policies.selectByPolicyVersionForUpdate(11L, "precision-v1", "signature.record.create"))
                    .thenReturn(null);
        } else if (missing.equals("watermark")) {
            when(ledger.selectByTenantIdForUpdate(11L)).thenReturn(null);
        } else {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
        try (ScopedTime time = new ScopedTime(null, 0)) {
            if (missing.equals("transaction")) {
                IllegalStateException error = assertThrows(IllegalStateException.class, () -> writer.append(command()));
                assertEquals("GxP audit append requires an active transaction", error.getMessage());
            } else {
                ServiceException error = assertThrows(ServiceException.class, () -> writer.append(command()));
                assertEquals((missing.equals("policy") ? GXP_AUDIT_POLICY_NOT_FOUND : GXP_AUDIT_APPEND_FAILED)
                        .getCode(), error.getCode());
            }
            time.verifyExpectedSampling();
        }
        assertNoWrites();
    }

    private static final class ScopedTime implements AutoCloseable {
        private final AtomicInteger samplingAttempts = new AtomicInteger();
        private final int permittedCalls;
        private final MockedStatic<LocalDateTime> time;

        private ScopedTime(LocalDateTime sample, int permittedCalls) {
            this.permittedCalls = permittedCalls;
            time = mockStatic(LocalDateTime.class, call -> {
                // Construction/conversion is real; every clock overload is intercepted before execution.
                if (!call.getMethod().getName().equals("now")) {
                    return call.callRealMethod();
                }
                int attempted = samplingAttempts.incrementAndGet();
                Object[] arguments = call.getArguments();
                if (arguments.length != 1 || !ZoneOffset.UTC.equals(arguments[0])
                        || attempted > permittedCalls) {
                    throw new AssertionError("unexpected server time sampling");
                }
                return sample;
            });
        }

        private void verifyExpectedSampling() {
            assertEquals(permittedCalls, samplingAttempts.get(), "exact server time sampling attempts");
            time.verify(() -> LocalDateTime.now(ZoneOffset.UTC), times(permittedCalls));
        }

        @Override
        public void close() {
            time.close();
        }
    }

    private void assertNoWrites() {
        for (Object mapper : List.of(ledger, events, relations, policies, activations)) {
            assertFalse(mockingDetails(mapper).getInvocations().stream().anyMatch(call -> {
                String method = call.getMethod().getName();
                return method.startsWith("insert") || method.startsWith("update") || method.startsWith("delete");
            }), "replay and missing prerequisites must have zero mapper writes");
        }
    }

    private String canonicalWithTime(String canonical, LocalDateTime expected) {
        TreeMap<String, Object> fields = new TreeMap<>(JsonUtils.parseObject(canonical, Map.class));
        fields.put("serverOccurredAt", expected.toString());
        return JsonUtils.toJsonString(fields);
    }

    private GxpAuditCommand command() {
        return GxpAuditCommand.builder().operationId("signature.record.create").subjectId("precision-signature")
                .subjectVersion("1").reason("precision contract").idempotencyKey("precision-key")
                .requestId("precision-request").source("GxpAuditWriterTimePrecisionTest#append")
                .signatureRecordId("precision-signature").signatureContentHash("precision-content-hash")
                .beforeState(GxpAuditStateEnvelope.builder().state("ABSENT").objectVersion("0")
                        .canonicalJson("{}").build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion("1")
                        .canonicalJson("{\"id\":\"precision-signature\"}").build())
                .links(List.of(new GxpAuditRelation("SOURCE", "EDHR_EXECUTION", "precision-subject", "1", "precision-hash")))
                .build();
    }
}
