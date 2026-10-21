package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual reader/hash algorithm. Only database query ports are substituted in this class. */
class GxpCreationAuditVerificationServiceTest {
    private GxpCreationAuditVerificationService service;
    private GxpAuditEventMapper events;
    private GxpAuditEventRelationMapper relations;
    private GxpAuditEventDO audit;

    @BeforeAll static void recordProcess() {
        System.out.println("AC02_PROCESS class=" + GxpCreationAuditVerificationServiceTest.class.getName()
                + " pid=" + ProcessHandle.current().pid()
                + " jvmStartTime=" + java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime());
    }
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        events = mock(GxpAuditEventMapper.class);
        relations = mock(GxpAuditEventRelationMapper.class);
        service = new GxpCreationAuditVerificationService();
        ReflectionTestUtils.setField(service, "auditEventMapper", events);
        ReflectionTestUtils.setField(service, "eventRelationMapper", relations);
        audit = fixture();
        rehash();
        when(events.selectByTenantIdAndId(1L, 20L)).thenReturn(audit);
        when(relations.selectListByTenantIdAndEventId(1L, 20L)).thenReturn(List.of());
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    private GxpAuditEventDO fixture() {
        GxpAuditEventDO event = new GxpAuditEventDO();
        event.setId(20L); event.setTenantId(1L); event.setLedgerSequence(1L);
        event.setOperationId("generic.audit.operation"); event.setDomain("TEST"); event.setSubjectType("SOURCE");
        event.setSubjectId(" source:1 "); event.setSubjectVersion("v1"); event.setAction("CREATE"); event.setReason("original reason");
        event.setActorId(7L); event.setActorUsername("creator-a"); event.setActorDisplayName("Creator A");
        event.setServerOccurredAt(LocalDateTime.of(2026, 10, 6, 2, 0, 0, 123000000));
        event.setBeforeState("ABSENT"); event.setBeforeStateJson(" { opaque before "); event.setAfterState("PRESENT");
        event.setAfterStateJson(" {\"quantity\":12.340000} \n"); event.setAfterObjectVersion("v1");
        event.setPolicyVersion("audit-test-policy"); event.setIdempotencyKey("original:key");
        event.setIdempotencyPayloadHash("a".repeat(64)); event.setRequestId("request");
        event.setAlgorithm("SHA-256"); event.setEventSchemaVersion(2); event.setCanonicalizationVersion("GXP_CANONICAL_V2");
        event.setResultStatus("SUCCESS"); event.setReasonCode("SOURCE_CREATE"); event.setReasonSource("USER");
        event.setTransactionId("tx"); event.setAuthenticatedActorJson(actor(7L, 1L)); event.setPerformedByJson(actor(7L, 1L));
        event.setSourceType("SERVICE_METHOD"); event.setSourceLocator("generic#append");
        event.setRelationManifestJson("[]"); event.setEvidenceManifestJson("[]");
        return event;
    }
    private String actor(Long actorId, Long tenantId) {
        return JsonUtils.toJsonString(Map.of("actorId", actorId, "tenantId", tenantId, "otherOriginalField", "preserved"));
    }
    private void rehash() {
        audit.setStatePayloadHash(DigestUtil.sha256Hex(audit.getBeforeStateJson() + "\u001f" + audit.getAfterStateJson()));
        String canonical = ReflectionTestUtils.invokeMethod(new GxpAuditServiceImpl(), "canonicalEvent", audit);
        setCanonical(canonical);
    }
    private void setCanonical(String canonical) {
        audit.setCanonicalEventJson(canonical); audit.setEventHash(DigestUtil.sha256Hex(canonical));
    }
    private ObjectNode canonical() { return (ObjectNode) GxpCreationAuditVerificationService.originalTree(audit.getCanonicalEventJson()); }
    private GxpCreationAuditVerificationService.VerifiedAuditEventFact read() { return service.requireVerifiedEvent(1L, 20L); }
    private void rejected(String category) { assertEquals(category, assertThrows(IllegalStateException.class, this::read).getMessage()); }
    private GxpAuditEventRelationDO row(long id, GxpAuditRelation relation) {
        GxpAuditEventRelationDO row = new GxpAuditEventRelationDO();
        row.setId(id); row.setTenantId(1L); row.setEventId(20L); row.setRelationType(relation.relationType());
        row.setTargetType(relation.objectType()); row.setTargetId(relation.objectId());
        row.setTargetVersion(relation.objectVersion()); row.setTargetHash(relation.objectHash()); return row;
    }

    @ParameterizedTest
    @CsvSource({
            "2026-10-11T12:50:17.252235200,2026-10-11T12:50:17.252",
            "2026-10-11T12:50:59.330517200,2026-10-11T12:50:59.331",
            "2026-10-11T23:59:59.999500,2026-10-12T00:00",
            "2026-10-11T12:50:17.252,2026-10-11T12:50:17.252"
    })
    void verifiesImmutableClockAgainstMysqlMillisecondRoundTrip(String original, String persisted) {
        audit.setServerOccurredAt(LocalDateTime.parse(original));
        rehash();
        String canonical = audit.getCanonicalEventJson(), hash = audit.getEventHash();
        audit.setServerOccurredAt(LocalDateTime.parse(persisted));
        assertEquals(20L, read().eventId());
        assertEquals(canonical, audit.getCanonicalEventJson());
        assertEquals(hash, audit.getEventHash());
        audit.setServerOccurredAt(audit.getServerOccurredAt().plusNanos(1_000_000));
        rejected("AUDIT_INTEGRITY_FAILED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "null", "number", "hashTampered", "submillisecondColumn"})
    void millisecondProjectionStillRejectsInvalidAndTamperedTimeEvidence(String mutation) {
        if ("hashTampered".equals(mutation)) {
            audit.setCanonicalEventJson(audit.getCanonicalEventJson().replace(".123", ".123400"));
        } else if ("submillisecondColumn".equals(mutation)) {
            audit.setServerOccurredAt(audit.getServerOccurredAt().plusNanos(1));
        } else {
            ObjectNode changed = canonical();
            if ("invalid".equals(mutation)) changed.put("serverOccurredAt", "invalid");
            else if ("null".equals(mutation)) changed.putNull("serverOccurredAt");
            else changed.put("serverOccurredAt", 1);
            setCanonical(JsonUtils.toJsonString(changed));
        }
        rejected("AUDIT_INTEGRITY_FAILED");
    }

    @Test void candidateReadsKeepAllRolesAndEvents() {
        ArrayList<Long> input = new ArrayList<>(List.of(20L, 21L));
        when(relations.selectEventIdsByTargets(1L, List.of(" TARGET "), " id:1 ")).thenReturn(input);
        List<Long> result = service.readCandidateEventIdsByTarget(1L, " TARGET ", " id:1 ");
        assertEquals(List.of(20L, 21L), result); input.clear(); assertEquals(2, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.add(22L));
        when(relations.selectEventIdsByTargets(1L, List.of(" TARGET "), " id:1 ")).thenReturn(List.of());
        assertEquals(List.of(), service.readCandidateEventIdsByTarget(1L, " TARGET ", " id:1 "));
        verify(relations, times(2)).selectEventIdsByTargets(1L, List.of(" TARGET "), " id:1 "); verifyNoInteractions(events);
    }
    @ParameterizedTest(name = "{0}") @ValueSource(strings = {"nullResult", "nullElement", "zero", "negative", "duplicate"})
    void candidateNullInvalidOrDuplicateIdsFailFast(String variant) {
        List<Long> ids = switch (variant) {
            case "nullResult" -> null;
            case "nullElement" -> Arrays.asList(20L, null);
            case "zero" -> List.of(0L);
            case "negative" -> List.of(-1L);
            case "duplicate" -> List.of(20L, 20L);
            default -> throw new AssertionError(variant);
        };
        when(relations.selectEventIdsByTargets(1L, List.of("TARGET"), "id")).thenReturn(ids);
        assertThrows(IllegalStateException.class, () -> service.readCandidateEventIdsByTarget(1L, "TARGET", "id"));
    }
    @Test void readArgumentsAndMissingTenantFailBeforeQueries() {
        for (Long tenant : Arrays.asList(null, 0L, -1L, 2L)) {
            assertThrows(IllegalArgumentException.class, () -> service.requireVerifiedEvent(tenant, 20L));
            assertThrows(IllegalArgumentException.class, () -> service.readCandidateEventIdsByTarget(tenant, "T", "id"));
        }
        for (Long id : Arrays.asList(null, 0L, -1L)) assertThrows(IllegalArgumentException.class, () -> service.requireVerifiedEvent(1L, id));
        for (String text : Arrays.asList(null, "", " \t")) {
            assertThrows(IllegalArgumentException.class, () -> service.readCandidateEventIdsByTarget(1L, text, "id"));
            assertThrows(IllegalArgumentException.class, () -> service.readCandidateEventIdsByTarget(1L, "T", text));
        }
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, this::read);
        assertThrows(RuntimeException.class, () -> service.readCandidateEventIdsByTarget(1L, "T", "id"));
        verifyNoInteractions(events, relations);
    }
    @ParameterizedTest(name = "{0}") @ValueSource(strings = {"exact", "candidate", "physical"})
    void infrastructureExceptionsPropagateUnchanged(String port) {
        IllegalStateException failure = new IllegalStateException("query infrastructure failed");
        if (port.equals("exact")) when(events.selectByTenantIdAndId(1L, 20L)).thenThrow(failure);
        else if (port.equals("candidate")) when(relations.selectEventIdsByTargets(1L, List.of("T"), "id")).thenThrow(failure);
        else when(relations.selectListByTenantIdAndEventId(1L, 20L)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> {
            if (port.equals("candidate")) service.readCandidateEventIdsByTarget(1L, "T", "id"); else read();
        }));
    }
    @ParameterizedTest(name = "{0}") @ValueSource(strings = {"missing", "wrongId", "wrongTenant"})
    void exactEventMustMatchRequestedIdentityAndTenant(String variant) {
        if (variant.equals("missing")) when(events.selectByTenantIdAndId(1L, 20L)).thenReturn(null);
        else if (variant.equals("wrongId")) audit.setId(21L); else audit.setTenantId(2L);
        rejected(variant.equals("missing") ? "AUDIT_EVENT_MISSING" : "AUDIT_INTEGRITY_FAILED");
        verify(events).selectByTenantIdAndId(1L, 20L); verifyNoInteractions(relations);
    }
    @ParameterizedTest(name = "{0}") @ValueSource(strings = {
            "tenantId", "ledgerSequence", "operationId", "domain", "subjectType", "subjectId", "subjectVersion", "action", "reason",
            "actorId", "actorUsername", "actorDisplayName", "serverOccurredAt", "beforeState", "beforeObjectVersion", "beforeStateJson",
            "afterState", "afterObjectVersion", "afterStateJson", "policyVersion", "idempotencyKey", "idempotencyPayloadHash", "requestId",
            "signatureRecordId", "signatureContentHash", "previousEventHash", "algorithm", "eventSchemaVersion", "canonicalizationVersion",
            "resultStatus", "reasonCode", "reasonSource", "transactionId", "authenticatedActorJson", "performedByJson", "sourceType",
            "sourceLocator", "traceId", "errorCode", "attemptedOperationId", "relationManifestJson", "evidenceManifestJson", "statePayloadHash"})
    void everyProtectedColumnContradictionIsRejected(String field) {
        BeanWrapperImpl bean = new BeanWrapperImpl(audit);
        Class<?> type = bean.getPropertyType(field); Object original = bean.getPropertyValue(field);
        Object changed;
        if (type == Long.class) changed = original == null ? 11L : (Long) original + 1L;
        else if (type == Integer.class) changed = Integer.valueOf(3);
        else if (type == LocalDateTime.class) changed = audit.getServerOccurredAt().plusNanos(1);
        else changed = "changed-column";
        bean.setPropertyValue(field, changed); rejected("AUDIT_INTEGRITY_FAILED");
    }
    @ParameterizedTest(name = "{0}") @ValueSource(strings = {"missingNull", "explicitNull", "missingNonnull", "wrongScalar"})
    void originalNullableMissingAndExplicitNullGrammarIsPreserved(String variant) {
        ObjectNode tree = canonical();
        if (variant.equals("missingNull")) tree.remove("signatureRecordId");
        else if (variant.equals("explicitNull")) tree.putNull("signatureRecordId");
        else if (variant.equals("missingNonnull")) tree.remove("subjectId");
        else tree.put("tenantId", "1");
        setCanonical(tree.toString());
        if (variant.endsWith("Null")) assertNull(read().columns().signatureRecordId()); else rejected("AUDIT_INTEGRITY_FAILED");
    }
    @ParameterizedTest(name = "{0}") @ValueSource(strings = {"eventHash", "stateHash", "schema", "algorithm", "version", "status", "eventId",
            "ledger", "time", "transaction", "policy", "payloadHash", "key", "beforeRaw", "afterRaw", "canonical"})
    void rawHashesSchemaAndLifecycleMustBeValid(String variant) {
        switch (variant) {
            case "eventHash" -> audit.setEventHash("bad"); case "stateHash" -> audit.setStatePayloadHash("bad");
            case "schema" -> audit.setEventSchemaVersion(1); case "algorithm" -> audit.setAlgorithm("MD5");
            case "version" -> audit.setCanonicalizationVersion("OTHER"); case "status" -> audit.setResultStatus("FAILED");
            case "eventId" -> audit.setId(0L); case "ledger" -> audit.setLedgerSequence(0L); case "time" -> audit.setServerOccurredAt(null);
            case "transaction" -> audit.setTransactionId(" "); case "policy" -> audit.setPolicyVersion(" ");
            case "payloadHash" -> audit.setIdempotencyPayloadHash(" "); case "key" -> audit.setIdempotencyKey(" ");
            case "beforeRaw" -> audit.setBeforeStateJson(" "); case "afterRaw" -> audit.setAfterStateJson(" ");
            case "canonical" -> { setCanonical("{"); rejected("AUDIT_INTEGRITY_FAILED"); return; }
            default -> throw new AssertionError(variant);
        }
        // Self-consistent hashes must not make invalid lifecycle prerequisites valid.
        if (!List.of("eventHash", "stateHash", "time").contains(variant)) rehash();
        rejected("AUDIT_INTEGRITY_FAILED");
    }
    @ParameterizedTest(name = "{0}:{1}") @CsvSource({"canonical,duplicate", "canonical,unknown", "canonical,trailing", "canonical,malformed",
            "actor,duplicate", "actor,trailing", "actor,malformed", "relations,duplicate", "relations,unknown", "relations,trailing", "relations,malformed",
            "evidence,duplicate", "evidence,unknown", "evidence,trailing", "evidence,malformed"})
    void newStrictReaderRejectsDuplicateUnknownAndTrailingValues(String location, String variant) {
        String category;
        if (location.equals("canonical")) {
            String raw = audit.getCanonicalEventJson();
            String changed = variant.equals("duplicate") ? "{\"tenantId\":1," + raw.substring(1)
                    : variant.equals("unknown") ? "{\"unknown\":null," + raw.substring(1)
                    : variant.equals("trailing") ? raw + " {}" : "{";
            setCanonical(changed); category = "AUDIT_INTEGRITY_FAILED";
        } else if (location.equals("actor")) {
            audit.setPerformedByJson(variant.equals("duplicate") ? "{\"actorId\":7,\"actorId\":7,\"tenantId\":1}"
                    : variant.equals("trailing") ? actor(7L, 1L) + " {}" : "{");
            rehash(); category = "AUDIT_ACTOR_MISMATCH";
        } else {
            String changed = variant.equals("duplicate") ? "[{\"role\":null,\"role\":null}]"
                    : variant.equals("unknown") ? "[{\"unknown\":null}]"
                    : variant.equals("trailing") ? "[] {}" : "[";
            if (location.equals("relations")) audit.setRelationManifestJson(changed); else audit.setEvidenceManifestJson(changed);
            rehash(); category = "AUDIT_MANIFEST_INVALID";
        }
        rejected(category);
    }
    @ParameterizedTest(name = "{0}") @ValueSource(strings = {"unknown", "number", "duplicate", "trailing", "malformed", "notArray"})
    void evidenceManifestMustUseOriginalNullableStringGrammar(String variant) {
        audit.setEvidenceManifestJson(switch (variant) {
            case "unknown" -> "[{\"other\":null}]";
            case "number" -> "[{\"sourceId\":7}]";
            case "duplicate" -> "[{\"sourceId\":null,\"sourceId\":null}]";
            case "trailing" -> "[] {}";
            case "malformed" -> "[";
            case "notArray" -> "{}";
            default -> throw new AssertionError(variant);
        });
        rehash(); rejected("AUDIT_MANIFEST_INVALID");
    }
    @ParameterizedTest(name = "{0}") @ValueSource(strings = {"valid", "otherAuthenticated", "otherPerformed", "otherTenant", "missingActor", "quotedActor", "nonpositive", "employee"})
    void actorUsesOriginalEventIdentityAndCurrentScalarGrammar(String variant) {
        switch (variant) {
            case "valid" -> { /* The current tenant has no creator identity argument: creator A is historical. */ }
            case "otherAuthenticated" -> audit.setAuthenticatedActorJson(actor(8L, 1L));
            case "otherPerformed" -> audit.setPerformedByJson(actor(8L, 1L));
            case "otherTenant" -> audit.setPerformedByJson(actor(7L, 2L));
            case "missingActor" -> audit.setPerformedByJson("{\"tenantId\":1}");
            case "quotedActor" -> audit.setPerformedByJson("{\"actorId\":\"7\",\"tenantId\":1}");
            case "nonpositive" -> { audit.setActorId(-1L); audit.setAuthenticatedActorJson(actor(-1L, 1L)); audit.setPerformedByJson(actor(-1L, 1L)); }
            case "employee" -> audit.setPerformedByJson("{\"actorId\":\"employee-7\"}");
            default -> throw new AssertionError(variant);
        }
        rehash(); if (variant.equals("valid")) assertEquals(7L, read().columns().actorId()); else rejected("AUDIT_ACTOR_MISMATCH");
    }
    @ParameterizedTest(name = "{0}") @ValueSource(strings = {"reordered", "missing", "extra", "nullResult", "nullRow", "duplicateKey", "duplicatePk",
            "invalidPk", "wrongTenant", "wrongEvent", "role", "type", "id", "version", "hash", "duplicateManifest", "manifestNumeric", "nullable"})
    void physicalRelationSetMustEqualProtectedManifest(String variant) {
        GxpAuditRelation first = new GxpAuditRelation("SOURCE", "ORDER", "order-1", "v1", "hash1");
        GxpAuditRelation second = new GxpAuditRelation("SUBJECT", "ORDER", "order-2", null, null);
        audit.setRelationManifestJson(JsonUtils.toJsonString(List.of(first, second)));
        GxpAuditEventRelationDO one = row(1L, first), two = row(2L, second);
        List<GxpAuditEventRelationDO> rows = List.of(two, one);
        switch (variant) {
            case "reordered", "nullable" -> { }
            case "missing" -> rows = List.of(one);
            case "extra" -> rows = List.of(one, two, row(3L, new GxpAuditRelation("OTHER", "ORDER", "extra", null, null)));
            case "nullResult" -> rows = null; case "nullRow" -> rows = Arrays.asList(one, null);
            case "duplicateKey" -> rows = List.of(one, row(3L, first), two);
            case "duplicatePk" -> two.setId(1L); case "invalidPk" -> one.setId(0L);
            case "wrongTenant" -> one.setTenantId(2L); case "wrongEvent" -> one.setEventId(21L);
            case "role" -> one.setRelationType("SUBJECT"); case "type" -> one.setTargetType("OTHER"); case "id" -> one.setTargetId("other");
            case "version" -> one.setTargetVersion("v2"); case "hash" -> one.setTargetHash("hash2");
            case "duplicateManifest" -> audit.setRelationManifestJson(JsonUtils.toJsonString(List.of(first, first)));
            case "manifestNumeric" -> audit.setRelationManifestJson("[{\"relationType\":\"SOURCE\",\"objectType\":\"ORDER\",\"objectId\":1}]");
            default -> throw new AssertionError(variant);
        }
        rehash(); when(relations.selectListByTenantIdAndEventId(1L, 20L)).thenReturn(rows);
        if (List.of("reordered", "nullable").contains(variant)) assertEquals(List.of(first, second), read().relations());
        else rejected(variant.startsWith("manifest") || variant.equals("duplicateManifest") ? "AUDIT_MANIFEST_INVALID" : "AUDIT_RELATION_MISMATCH");
    }
    @Test void evidenceAndFactRemainImmutableAndOpaque() {
        audit.setEvidenceManifestJson("[{}, {\"evidenceType\":null,\"sourceId\":\"\",\"sourceVersion\":null,\"sha256\":\"raw hash\",\"role\":null}]");
        rehash(); var fact = read();
        assertEquals(audit.getBeforeStateJson(), fact.columns().beforeStateJson());
        assertEquals(audit.getAfterStateJson(), fact.columns().afterStateJson());
        assertEquals(new GxpAuditEvidence(null, null, null, null, null), fact.evidences().get(0));
        assertEquals("", fact.evidences().get(1).sourceId());
        assertThrows(UnsupportedOperationException.class, () -> fact.evidences().clear());
        assertThrows(UnsupportedOperationException.class, () -> fact.relations().add(new GxpAuditRelation("S", "T", "i", null, null)));
        ArrayList<GxpAuditEvidence> input = new ArrayList<>(fact.evidences());
        var copy = new GxpCreationAuditVerificationService.VerifiedAuditEventFact(20L, fact.eventHash(), fact.canonicalEventJson(), fact.columns(), List.of(), input);
        input.clear(); assertEquals(2, copy.evidences().size()); audit.setSubjectId("later mutable DO"); assertEquals(" source:1 ", fact.columns().subjectId());
        assertThrows(NullPointerException.class, () -> new GxpCreationAuditVerificationService.VerifiedAuditEventFact(20L, "hash", "raw", fact.columns(), null, List.of()));
        assertThrows(NullPointerException.class, () -> new GxpCreationAuditVerificationService.VerifiedAuditEventFact(20L, "hash", "raw", fact.columns(), List.of(), Arrays.asList((GxpAuditEvidence) null)));
        audit.setEvidenceManifestJson("[{\"sourceId\":5}]"); rehash(); rejected("AUDIT_MANIFEST_INVALID");
    }
    @Test void legacyV1ContractRemainsUnchanged() {
        SignatureCreationVerificationService legacy = new SignatureCreationVerificationService();
        ReflectionTestUtils.setField(legacy, "auditEventMapper", events);
        var expected = new SignatureAuditExpectation(1L, 10L, "V1", "TYPE:10", "v1", 7L, "original reason", "content", "original:key", Map.of("signatureId", 10L));
        audit.setOperationId("signature.record.create"); audit.setDomain("SIGNATURE"); audit.setSubjectType("SIGNATURE_RECORD");
        audit.setSubjectId("TYPE:10"); audit.setBeforeState("NO_SIGNATURE_RECORD"); audit.setBeforeObjectVersion("v1"); audit.setBeforeStateJson("{}");
        audit.setAfterState("ELECTRONIC_SIGNATURE_RECORDED"); audit.setAfterStateJson(JsonUtils.toJsonString(expected.afterState()));
        audit.setSignatureRecordId("10"); audit.setSignatureContentHash("content"); audit.setReasonCode("SIGNATURE_RECORD_CREATE");
        audit.setSourceLocator("ElectronicSignatureServiceImpl.sign"); rehash();
        ObjectNode tree = canonical(); tree.put("historicalExtra", "allowed by original V1"); tree.remove("previousEventHash"); setCanonical(tree.toString());
        when(events.selectSignatureCreationEvents(1L, "10")).thenReturn(List.of(audit));
        assertEquals("CONSISTENT", legacy.verifySignatureCreation(expected).auditBindingStatus());
        assertTrue(GxpCreationAuditVerificationService.originalIntegrity(audit, 1L));
        verifyNoInteractions(relations); rejected("AUDIT_INTEGRITY_FAILED");
        assertNull(GxpCreationAuditVerificationService.originalTree("{"));
        assertEquals(GxpCreationAuditVerificationService.originalTree("{}"), GxpCreationAuditVerificationService.originalTree("{} {}"));
    }
    @ParameterizedTest(name = "{0}") @ValueSource(strings = {"V3", "UNKNOWN"})
    void registeredProtocolsStillRejectUnknownVersions(String protocol) {
        assertThrows(IllegalArgumentException.class, () -> SignatureCreationProtocolRegistry.require(protocol)); verifyNoInteractions(events, relations);
    }
    @Test void registeredV2KeepsItsDistinctCreationSource() {
        assertEquals("ElectronicSignatureServiceImpl.sign", SignatureCreationProtocolRegistry.require("V1").sourceLocator());
        assertEquals("ElectronicSignatureTransactionExecutor.executeAuthenticated", SignatureCreationProtocolRegistry.require("V2").sourceLocator());
        verifyNoInteractions(events, relations);
    }
    @Test void selfConsistentBusinessChangeIsNotCreationCertification() {
        audit.setOperationId("another.domain.update"); audit.setSubjectId("another-source"); audit.setAfterStateJson("uninterpreted domain payload"); rehash();
        var fact = read(); assertEquals("another.domain.update", fact.columns().operationId()); assertEquals("another-source", fact.columns().subjectId());
        assertEquals(43, GxpCreationAuditVerificationService.ProtectedColumns.class.getRecordComponents().length);
        for (var component : GxpCreationAuditVerificationService.ProtectedColumns.class.getRecordComponents()) {
            Object stored = new BeanWrapperImpl(audit).getPropertyValue(component.getName());
            try { assertEquals(stored, component.getAccessor().invoke(fact.columns()), component.getName()); }
            catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
        }
        verify(events).selectByTenantIdAndId(1L, 20L); verify(relations).selectListByTenantIdAndEventId(1L, 20L);
        verifyNoMoreInteractions(events, relations);
    }
}
