package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real verification algorithm over stored audit structures; only the database read port is substituted. */
class SignatureCreationVerificationServiceTest {
    private SignatureCreationVerificationService verifier;
    private GxpAuditEventMapper mapper;
    private SignatureAuditExpectation expected;
    private GxpAuditEventDO audit;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        mapper = mock(GxpAuditEventMapper.class);
        verifier = new SignatureCreationVerificationService();
        ReflectionTestUtils.setField(verifier, "auditEventMapper", mapper);
        expected = new SignatureAuditExpectation(1L, 10L, "V1", "TYPE:10", "v1", 7L, "批准", "content",
                "SIGNATURE:original", Map.of("signatureId", 10L, "actorId", 7L, "signedAt", "2026-10-06T10:00:00"));
        audit = new GxpAuditEventDO();
        audit.setId(20L); audit.setTenantId(1L); audit.setLedgerSequence(1L); audit.setOperationId("signature.record.create");
        audit.setDomain("SIGNATURE"); audit.setSubjectType("SIGNATURE_RECORD"); audit.setAction("CREATE");
        audit.setSubjectId("TYPE:10"); audit.setSubjectVersion("v1"); audit.setActorId(7L); audit.setReason("批准");
        audit.setServerOccurredAt(LocalDateTime.of(2026, 10, 6, 2, 0)); audit.setBeforeState("NO_SIGNATURE_RECORD");
        audit.setBeforeObjectVersion("v1"); audit.setBeforeStateJson("{}"); audit.setAfterState("ELECTRONIC_SIGNATURE_RECORDED");
        audit.setAfterObjectVersion("v1"); audit.setAfterStateJson(JsonUtils.toJsonString(expected.afterState()));
        audit.setSignatureRecordId("10"); audit.setSignatureContentHash("content"); audit.setIdempotencyKey("SIGNATURE:original");
        audit.setPolicyVersion("audit-p1"); audit.setIdempotencyPayloadHash("a".repeat(64));
        audit.setAlgorithm("SHA-256"); audit.setEventSchemaVersion(2); audit.setCanonicalizationVersion("GXP_CANONICAL_V2");
        audit.setResultStatus("SUCCESS"); audit.setReasonCode("SIGNATURE_RECORD_CREATE"); audit.setReasonSource("USER");
        audit.setTransactionId("tx"); audit.setAuthenticatedActorJson("{\"actorId\":7,\"tenantId\":1}");
        audit.setPerformedByJson(audit.getAuthenticatedActorJson()); audit.setSourceType("SERVICE_METHOD");
        audit.setSourceLocator("ElectronicSignatureServiceImpl.sign"); audit.setRelationManifestJson("[]"); audit.setEvidenceManifestJson("[]");
        rehash(); when(mapper.selectSignatureCreationEvents(1L, "10")).thenReturn(List.of(audit));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    private void rehash() {
        audit.setStatePayloadHash(DigestUtil.sha256Hex(audit.getBeforeStateJson() + "\u001f" + audit.getAfterStateJson()));
        // Use the actual existing writer serialization, not a separate made-up audit hash protocol.
        String canonical = ReflectionTestUtils.invokeMethod(new GxpAuditServiceImpl(), "canonicalEvent", audit);
        audit.setCanonicalEventJson(canonical); audit.setEventHash(DigestUtil.sha256Hex(canonical));
    }
    @Test void realAlgorithmAcceptsFrozenCreationFact() {
        var result = verifier.verifySignatureCreation(expected);
        assertEquals("CONSISTENT", result.auditBindingStatus()); assertEquals(20L, result.auditEventId()); assertTrue(result.reasonCodes().isEmpty());
    }
    @Test void v2CreationMustUseItsOwnExactSourceAndCannotBeRelabeledAsV1() {
        var v2 = new SignatureAuditExpectation(expected.tenantId(), expected.signatureId(), "V2",
                expected.subjectId(), expected.subjectVersion(), expected.operatorId(), expected.reason(),
                expected.contentHash(), expected.idempotencyKey(), expected.afterState());
        assertEquals(List.of("AUDIT_BINDING_MISMATCH"), verifier.verifySignatureCreation(v2).reasonCodes());
        audit.setSourceLocator("ElectronicSignatureTransactionExecutor.executeAuthenticated");
        rehash();
        assertEquals("CONSISTENT", verifier.verifySignatureCreation(v2).auditBindingStatus());
        assertEquals(List.of("AUDIT_BINDING_MISMATCH"), verifier.verifySignatureCreation(expected).reasonCodes());
    }
    @Test void emptyDuplicateNullAndInfrastructureFailuresAreDistinct() {
        when(mapper.selectSignatureCreationEvents(1L, "10")).thenReturn(List.of());
        assertEquals(List.of("AUDIT_LINK_MISSING"), verifier.verifySignatureCreation(expected).reasonCodes());
        when(mapper.selectSignatureCreationEvents(1L, "10")).thenReturn(List.of(audit, audit));
        assertEquals(List.of("AUDIT_LINK_DUPLICATE"), verifier.verifySignatureCreation(expected).reasonCodes());
        when(mapper.selectSignatureCreationEvents(1L, "10")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> verifier.verifySignatureCreation(expected));
        when(mapper.selectSignatureCreationEvents(1L, "10")).thenThrow(new IllegalStateException("DB offline"));
        assertThrows(IllegalStateException.class, () -> verifier.verifySignatureCreation(expected));
    }
    @ParameterizedTest @ValueSource(strings={"tenant", "source", "sourceType", "domain", "subjectType", "action", "actor", "signature", "subject", "reason", "signedAt", "manifest", "state", "hash", "schema", "algorithm", "actorJson"})
    void rejectsTamperingEvenWhenBothAuditHashesAreSelfConsistent(String field) {
        switch (field) {
            case "tenant" -> audit.setTenantId(2L);
            case "source" -> audit.setSourceLocator("ElectronicSignatureTransactionExecutor.executeAuthenticated");
            case "sourceType" -> audit.setSourceType("CONTROLLER_METHOD");
            case "domain" -> audit.setDomain("MES");
            case "subjectType" -> audit.setSubjectType("PRODUCTION_RECORD");
            case "action" -> audit.setAction("UPDATE");
            case "actor" -> audit.setActorId(8L);
            case "signature" -> audit.setSignatureRecordId("11");
            case "subject" -> audit.setSubjectId("OTHER:10");
            case "reason" -> audit.setReason("不同原因");
            case "signedAt" -> audit.setAfterStateJson(audit.getAfterStateJson().replace("10:00:00", "10:00:01"));
            case "manifest" -> audit.setEvidenceManifestJson("[{\"type\":\"fake\"}]");
            case "state" -> audit.setAfterState("PRESENT");
            case "hash" -> audit.setSignatureContentHash("changed");
            case "schema" -> audit.setEventSchemaVersion(1);
            case "algorithm" -> audit.setAlgorithm("MD5");
            case "actorJson" -> audit.setPerformedByJson("{\"actorId\":8,\"tenantId\":1}");
            default -> throw new AssertionError(field);
        }
        rehash(); assertEquals("MISMATCH", verifier.verifySignatureCreation(expected).auditBindingStatus());
    }
    @ParameterizedTest @ValueSource(strings={"reason", "actorDisplayName", "serverOccurredAt", "relationManifest", "canonical", "statePayloadHash", "eventHash"})
    void rejectsColumnHashContradictions(String field) {
        switch (field) {
            case "reason" -> audit.setReason("changed");
            case "actorDisplayName" -> audit.setActorDisplayName("changed");
            case "serverOccurredAt" -> audit.setServerOccurredAt(audit.getServerOccurredAt().plusSeconds(1));
            case "relationManifest" -> audit.setRelationManifestJson("[{}]");
            case "canonical" -> { audit.setCanonicalEventJson("{"); audit.setEventHash(DigestUtil.sha256Hex("{")); }
            case "statePayloadHash" -> audit.setStatePayloadHash("broken");
            case "eventHash" -> audit.setEventHash("broken");
            default -> throw new AssertionError(field);
        }
        assertEquals("MISMATCH", verifier.verifySignatureCreation(expected).auditBindingStatus());
    }
    @Test void expectedTenantMustMatchReadTenant() {
        TenantContextHolder.setTenantId(2L);
        assertThrows(IllegalArgumentException.class, () -> verifier.verifySignatureCreation(expected)); verifyNoInteractions(mapper);
    }
}
