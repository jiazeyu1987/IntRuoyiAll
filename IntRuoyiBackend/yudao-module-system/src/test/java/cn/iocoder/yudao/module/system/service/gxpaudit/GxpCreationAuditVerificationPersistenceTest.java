package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditLedgerSequenceDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;

/** Real writer/proxy/MyBatis/H2. Must execute alone in a fresh, non-reused JVM. */
@Import({GxpAuditServiceImpl.class, GxpCreationAuditVerificationService.class,
        JacksonAutoConfiguration.class, YudaoJacksonAutoConfiguration.class})
@Transactional
class GxpCreationAuditVerificationPersistenceTest extends BaseDbUnitTest {
    @Resource private GxpAuditService writer;
    @Resource private GxpCreationAuditVerificationService reader;
    @Resource private GxpAuditEventMapper events;
    @Resource private GxpAuditEventRelationMapper relations;
    @Resource private GxpAuditLedgerSequenceMapper ledger;
    @Resource private GxpAuditPolicyActivationMapper activations;
    @Resource private GxpAuditPolicyOperationMapper policies;
    @Resource private ObjectMapper objectMapper;
    // Required injection forces the actual production initializer bean, including in lazy test contexts.
    @Resource private JsonUtils jsonUtils;

    @BeforeAll static void recordProcess() {
        System.out.println("AC02_PROCESS class=" + GxpCreationAuditVerificationPersistenceTest.class.getName()
                + " pid=" + ProcessHandle.current().pid()
                + " jvmStartTime=" + java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime());
    }
    @BeforeEach void seedFixture() {
        assertNotNull(jsonUtils); assertSame(objectMapper, JsonUtils.getObjectMapper());
        assertTrue(AopUtils.isAopProxy(writer), "MANDATORY writer must be the real Spring proxy");
        login(1001L);
        GxpAuditPolicyOperationDO policy = new GxpAuditPolicyOperationDO();
        policy.setTenantId(1L); policy.setPolicyVersion("shared-reader-test-policy");
        policy.setOperationId("edhr.execution.field.update"); policy.setSourceType("SERVICE_METHOD");
        policy.setSourceLocator("unit-test#edhr.execution.field.update"); policy.setDomain("EDHR");
        policy.setSubjectType("EDHR_FIELD"); policy.setActionType("UPDATE"); policy.setReasonPolicy("USER_REQUIRED");
        policy.setSignaturePolicy("NONE"); policy.setStatePolicy("PRESENT_TO_PRESENT"); policy.setRetentionClass("GXP_MASTER_DATA");
        policy.setTestIds("AC02-SYSTEM-H2"); policy.setOwner("test-owner"); policy.setApplicability("GXP"); policy.setActive(true);
        assertEquals(1, policies.insert(policy));
        GxpAuditLedgerSequenceDO sequence = new GxpAuditLedgerSequenceDO();
        sequence.setTenantId(1L); sequence.setNextLedgerSequence(1L); assertEquals(1, ledger.insert(sequence));
        GxpAuditPolicyActivationDO activation = new GxpAuditPolicyActivationDO();
        activation.setTenantId(1L); activation.setPolicyVersion("shared-reader-test-policy");
        activation.setPolicyHash("test-policy-hash"); activation.setRequestId("shared-reader-policy-request");
        activation.setActorId(1001L); activation.setApprovalReference("test-approval"); activation.setActivatedAtUtc(LocalDateTime.now());
        activation.setEffectiveAfterSequence(0L); activation.setCanonicalActivationJson("{\"test\":true}");
        activation.setActivationHash("test-activation-hash"); assertEquals(1, activations.insert(activation));
    }
    private void login(long actorId) {
        LoginUser user = new LoginUser(); user.setId(actorId); user.setTenantId(1L);
        user.setInfo(Map.of("username", "test.creator", LoginUser.INFO_KEY_NICKNAME, "Test Creator"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }
    @AfterEach void clearFixtureLedger() {
        SecurityContextHolder.clearContext();
        // Existing clean.sql cleans events/relations/policy but omits the fixture tenant's ledger.
        ledger.deleteById(1L);
    }
    private GxpAuditCommand command(String key) {
        return GxpAuditCommand.builder().operationId("edhr.execution.field.update").subjectId("field-1").subjectVersion("2")
                .reason("reviewed generic fixture change")
                .beforeState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion("1").canonicalJson(" {\"quantity\":12.340000} ").build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion("2").canonicalJson("{\"quantity\":13.340000}\n").build())
                .idempotencyKey(key).requestId("shared-reader-request").traceId("shared-reader-trace").source("unit-test")
                .links(List.of(new GxpAuditRelation("SOURCE", "ORDER", "source-order", "v1", "source-hash")))
                .evidences(List.of(new GxpAuditEvidence("DOCUMENT", "document-1", null, "literal-test-hash", "SOURCE"))).build();
    }
    private GxpCreationAuditVerificationService.VerifiedAuditEventFact read(GxpAuditAppendResult appended) {
        return reader.requireVerifiedEvent(1L, appended.eventId());
    }
    private void endRollback() {
        if (TestTransaction.isActive()) { TestTransaction.flagForRollback(); TestTransaction.end(); }
    }
    private void commitAndStartRead() {
        TestTransaction.flagForCommit(); TestTransaction.end(); TestTransaction.start();
    }
    private void rehashAndUpdate(GxpAuditEventDO event) {
        String canonical = ReflectionTestUtils.invokeMethod(new GxpAuditServiceImpl(), "canonicalEvent", event);
        event.setCanonicalEventJson(canonical); event.setEventHash(DigestUtil.sha256Hex(canonical));
        assertEquals(1, events.updateById(event));
    }
    private void assertEmptyAtBaseline() {
        assertEquals(0L, events.selectCount()); assertEquals(0L, relations.selectCount());
        assertEquals(1L, ledger.selectById(1L).getNextLedgerSequence());
    }

    @ParameterizedTest(name = "actor={0}") @ValueSource(longs = {9007199254740990L, 9007199254740991L, 9007199254740993L, 9223372036854775807L})
    void productionJacksonBeanAndLargeLongGrammarAreActuallyActive(long actorId) {
        assertSame(objectMapper, JsonUtils.getObjectMapper());
        assertEquals("9007199254740990", JsonUtils.toJsonString(9007199254740990L));
        assertEquals("\"9007199254740991\"", JsonUtils.toJsonString(9007199254740991L));
        assertEquals("-9007199254740990", JsonUtils.toJsonString(-9007199254740990L));
        assertEquals("\"-9007199254740991\"", JsonUtils.toJsonString(-9007199254740991L));
        assertEquals("\"9223372036854775807\"", JsonUtils.toJsonString(Long.MAX_VALUE));
        login(actorId);
        var appended = writer.append(command("large-actor-" + actorId));
        var fact = read(appended); assertEquals(actorId, fact.columns().actorId());
        JsonNode storedActor = GxpCreationAuditVerificationService.originalTree(fact.columns().authenticatedActorJson());
        assertEquals(actorId < 9007199254740991L, storedActor.get("actorId").isIntegralNumber());
        assertEquals(actorId >= 9007199254740991L, storedActor.get("actorId").isTextual());
        GxpAuditEventDO event = events.selectById(appended.eventId());
        String valid = event.getPerformedByJson();
        for (String value : List.of(actorId < 9007199254740991L ? "\"" + actorId + "\"" : Long.toString(actorId),
                "\"0" + actorId + "\"", "\"" + actorId + ".0\"", "\"9223372036854775808\"", "null")) {
            event.setPerformedByJson("{\"actorId\":" + value + ",\"tenantId\":1}"); rehashAndUpdate(event);
            assertEquals("AUDIT_ACTOR_MISMATCH", assertThrows(IllegalStateException.class, () -> read(appended)).getMessage());
        }
        event.setPerformedByJson(valid); rehashAndUpdate(event); assertEquals(actorId, read(appended).columns().actorId());
    }
    @Test void realAppendCommittedReadReturnsImmutableAuditFact() {
        GxpAuditCommand input = command("committed-fact"); var appended = writer.append(input);
        GxpAuditEventDO original = events.selectById(appended.eventId());
        commitAndStartRead();
        try {
            var fact = read(appended); assertEquals(appended.eventHash(), fact.eventHash());
            assertEquals(original.getCanonicalEventJson(), fact.canonicalEventJson());
            assertEquals(43, GxpCreationAuditVerificationService.ProtectedColumns.class.getRecordComponents().length);
            for (var component : GxpCreationAuditVerificationService.ProtectedColumns.class.getRecordComponents()) {
                Object stored = new org.springframework.beans.BeanWrapperImpl(original).getPropertyValue(component.getName());
                try { assertEquals(stored, component.getAccessor().invoke(fact.columns()), component.getName()); }
                catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
            }
            assertEquals(input.getBeforeState().getCanonicalJson(), fact.columns().beforeStateJson());
            assertEquals(input.getAfterState().getCanonicalJson(), fact.columns().afterStateJson());
            assertEquals(input.getLinks(), fact.relations()); assertEquals(input.getEvidences(), fact.evidences());
            assertEquals(List.of(appended.eventId()), reader.readCandidateEventIdsByTarget(1L, "ORDER", "source-order"));
            assertThrows(UnsupportedOperationException.class, () -> fact.relations().clear());
            assertThrows(UnsupportedOperationException.class, () -> fact.evidences().clear());
        } finally { endRollback(); }
    }
    @Test void committedCandidatesIncludeAllRolesAndEvents() {
        var first = writer.append(command("candidate-source"));
        GxpAuditCommand other = command("candidate-subject");
        other.setLinks(List.of(new GxpAuditRelation("SUBJECT", "ORDER", "source-order", null, null)));
        var second = writer.append(other); commitAndStartRead();
        try {
            assertEquals(List.of(first.eventId(), second.eventId()), reader.readCandidateEventIdsByTarget(1L, "ORDER", "source-order"));
            assertEquals("SOURCE", read(first).relations().get(0).relationType());
            assertEquals("SUBJECT", read(second).relations().get(0).relationType());
            assertEquals(List.of(), reader.readCandidateEventIdsByTarget(1L, "ORDER", "absent"));
        } finally { endRollback(); }
    }
    @ParameterizedTest(name = "{0}") @ValueSource(strings = {"column", "missing", "extra", "version", "hash"})
    void persistedTamperOrMissingRelationIsRejected(String variant) {
        var appended = writer.append(command("tamper-" + variant));
        try {
            GxpAuditEventRelationDO relation = relations.selectListByTenantIdAndEventId(1L, appended.eventId()).get(0);
            switch (variant) {
                case "column" -> { GxpAuditEventDO event = events.selectById(appended.eventId()); event.setReason("altered stored column"); assertEquals(1, events.updateById(event)); }
                case "missing" -> assertEquals(1, relations.deleteById(relation.getId()));
                case "extra" -> { relation.setId(null); relation.setTargetId("extra-target"); assertEquals(1, relations.insert(relation)); }
                case "version" -> { relation.setTargetVersion("v2"); assertEquals(1, relations.updateById(relation)); }
                case "hash" -> { relation.setTargetHash("wrong-hash"); assertEquals(1, relations.updateById(relation)); }
                default -> throw new AssertionError(variant);
            }
            assertEquals(variant.equals("column") ? "AUDIT_INTEGRITY_FAILED" : "AUDIT_RELATION_MISMATCH",
                    assertThrows(IllegalStateException.class, () -> read(appended)).getMessage());
        } finally { endRollback(); }
    }
    @Test void actualRelationUniqueFailureRollsBackAppend() {
        // Commit fixture baseline, so the transaction-outside assertions prove append rollback alone.
        TestTransaction.flagForCommit(); TestTransaction.end(); TestTransaction.start();
        GxpAuditCommand invalid = command("actual-unique-failure");
        GxpAuditRelation relation = invalid.getLinks().get(0); invalid.setLinks(List.of(relation, relation));
        DataAccessException failure = assertThrows(DataAccessException.class, () -> writer.append(invalid));
        assertNotNull(failure.getCause());
        // Before rollback, actual event/first relation/watermark writes must have occurred.
        assertEquals(1L, events.selectCount()); assertEquals(1L, relations.selectCount());
        long actualEventId = events.selectLatestByTenant(1L).getId();
        assertEquals(2L, ledger.selectById(1L).getNextLedgerSequence());
        TestTransaction.flagForCommit(); assertThrows(UnexpectedRollbackException.class, TestTransaction::end);
        assertFalse(TestTransaction.isActive()); assertEmptyAtBaseline();
        assertEquals(List.of(), reader.readCandidateEventIdsByTarget(1L, "ORDER", "source-order"));
        assertEquals("AUDIT_EVENT_MISSING", assertThrows(IllegalStateException.class, () -> reader.requireVerifiedEvent(1L, actualEventId)).getMessage());
    }
    @Test void missingPolicyDoesNotCreateFact() {
        GxpAuditCommand missing = command("missing-policy"); missing.setOperationId("unregistered.generic.operation");
        assertServiceException(() -> writer.append(missing), GXP_AUDIT_POLICY_NOT_FOUND, "unregistered.generic.operation");
        assertEmptyAtBaseline();
    }
    @Test void noOuterTransactionDoesNotCreateFact() {
        TestTransaction.flagForCommit(); TestTransaction.end();
        assertThrows(IllegalTransactionStateException.class, () -> writer.append(command("no-outer-transaction")));
        assertThrows(IllegalTransactionStateException.class, writer::acquireLedgerLock); assertEmptyAtBaseline();
    }
    @Test void sharedReadDoesNotAppendOrChangeWatermark() {
        var appended = writer.append(command("read-only-fact")); commitAndStartRead();
        try {
            GxpAuditEventDO original = events.selectById(appended.eventId());
            String eventBytes = JsonUtils.toJsonString(original), relationBytes = JsonUtils.toJsonString(relations.selectListByTenantIdAndEventId(1L, appended.eventId()));
            String ledgerBytes = JsonUtils.toJsonString(ledger.selectById(1L));
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.hasResource(GxpAuditTransactionContext.class));
            for (int count = 0; count < 3; count++) { read(appended); reader.readCandidateEventIdsByTarget(1L, "ORDER", "source-order"); }
            assertEquals(1L, events.selectCount()); assertEquals(1L, relations.selectCount());
            assertEquals(eventBytes, JsonUtils.toJsonString(events.selectById(appended.eventId())));
            assertEquals(relationBytes, JsonUtils.toJsonString(relations.selectListByTenantIdAndEventId(1L, appended.eventId())));
            assertEquals(ledgerBytes, JsonUtils.toJsonString(ledger.selectById(1L)));
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.hasResource(GxpAuditTransactionContext.class));
        } finally { endRollback(); }
    }
}
