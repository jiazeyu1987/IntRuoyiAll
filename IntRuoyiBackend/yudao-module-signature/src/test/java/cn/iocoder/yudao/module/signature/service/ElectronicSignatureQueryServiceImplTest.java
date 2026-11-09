package cn.iocoder.yudao.module.signature.service;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Executes existing signature writer, actual GxP writer, and both actual readers.
 * Storage/auth ports are substituted; this is NOT real DB persistence or Spring transaction verification. */
class ElectronicSignatureQueryServiceImplTest {
    private ElectronicSignatureQueryServiceImpl query;
    private ElectronicSignatureVerifiedFactService verifiedFacts;
    private ElectronicSignatureServiceImpl writer;
    private ElectronicSignatureRecordMapper records;
    private GxpAuditEventMapper audits;
    private ElectronicSignatureSubjectAdapter adapter;
    private AdminUserApi users;
    private ElectronicSignatureRecordDO record;
    private GxpAuditEventDO audit;
    private org.mockito.MockedStatic<SecurityFrameworkUtils> security;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var login = new LoginUser(); login.setId(7L); login.setTenantId(1L); login.setUserType(2);
        login.setInfo(Map.of("username", "operator", LoginUser.INFO_KEY_NICKNAME, "设备操作员"));
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
        security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(login);
        records = mock(ElectronicSignatureRecordMapper.class); audits = mock(GxpAuditEventMapper.class);
        users = mock(AdminUserApi.class); adapter = mock(ElectronicSignatureSubjectAdapter.class, CALLS_REAL_METHODS);
        when(adapter.moduleCode()).thenReturn("MES");
        when(adapter.supportedActions()).thenReturn(Set.of(new SignatureActionDefinition("MES", "APPROVE", "TYPE", "APPROVE", "批准", "p1")));
        when(adapter.loadAndAuthorize(any())).thenReturn(new SignatureSubjectSnapshot("TYPE", "10", "v1",
                "{\"value\":\"中文事实\",\"list\":[1,2]}", null, null, null, "pi", "task", "node", 1));
        when(records.insert(any(ElectronicSignatureRecordDO.class))).thenAnswer(call -> { record = call.getArgument(0); record.setId(10L); return 1; });
        when(records.selectById(10L)).thenAnswer(call -> record);
        when(audits.insert(any(GxpAuditEventDO.class))).thenAnswer(call -> { audit = call.getArgument(0); audit.setId(20L); return 1; });
        when(audits.selectSignatureCreationEvents(1L, "10")).thenAnswer(call -> audit == null ? List.of() : List.of(audit));
        var ledgerMapper = mock(GxpAuditLedgerSequenceMapper.class);
        var ledger = new GxpAuditLedgerSequenceDO(); ledger.setTenantId(1L); ledger.setNextLedgerSequence(1L);
        when(ledgerMapper.selectByTenantIdForUpdate(1L)).thenReturn(ledger);
        when(ledgerMapper.updateById(any(GxpAuditLedgerSequenceDO.class))).thenReturn(1);
        var activationMapper = mock(GxpAuditPolicyActivationMapper.class);
        var activation = new GxpAuditPolicyActivationDO(); activation.setPolicyVersion("audit-p1");
        when(activationMapper.selectLatestForUpdate(1L)).thenReturn(activation);
        var policyMapper = mock(GxpAuditPolicyOperationMapper.class);
        var policy = new GxpAuditPolicyOperationDO(); policy.setActive(true); policy.setApplicability("GXP");
        policy.setPolicyVersion("audit-p1"); policy.setDomain("SIGNATURE"); policy.setSubjectType("SIGNATURE_RECORD");
        policy.setActionType("CREATE"); policy.setReasonPolicy("APPROVAL_DECISION"); policy.setSignaturePolicy("REQUIRED");
        when(policyMapper.selectByPolicyVersionForUpdate(1L, "audit-p1", "signature.record.create")).thenReturn(policy);
        var auditWriter = new GxpAuditServiceImpl();
        ReflectionTestUtils.setField(auditWriter, "auditEventMapper", audits);
        ReflectionTestUtils.setField(auditWriter, "ledgerSequenceMapper", ledgerMapper);
        ReflectionTestUtils.setField(auditWriter, "policyActivationMapper", activationMapper);
        ReflectionTestUtils.setField(auditWriter, "policyOperationMapper", policyMapper);
        ReflectionTestUtils.setField(auditWriter, "eventRelationMapper", mock(GxpAuditEventRelationMapper.class));
        var directory = mock(cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper.class);
        var account = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO()
                .setId(7L).setStatus(0).setUsername("operator").setNickname("设备操作员");
        account.setTenantId(1L);
        when(directory.selectGxpAuditActor(1L, 7L)).thenReturn(account);
        ReflectionTestUtils.setField(auditWriter, "adminUserMapper", directory);
        writer = new ElectronicSignatureServiceImpl();
        ReflectionTestUtils.setField(writer, "signatureRecordMapper", records); ReflectionTestUtils.setField(writer, "adminUserApi", users);
        ReflectionTestUtils.setField(writer, "gxpAuditService", auditWriter); ReflectionTestUtils.setField(writer, "subjectAdapters", List.of(adapter));
        var verifier = new SignatureCreationVerificationService(); ReflectionTestUtils.setField(verifier, "auditEventMapper", audits);
        query = new ElectronicSignatureQueryServiceImpl(); ReflectionTestUtils.setField(query, "signatureRecordMapper", records);
        ReflectionTestUtils.setField(query, "signatureCreationVerificationService", verifier);
        verifiedFacts = new ElectronicSignatureVerifiedFactService(records, verifier);
    }
    @AfterEach void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            for (var synchronization : TransactionSynchronizationManager.getSynchronizations()) synchronization.afterCompletion(1);
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false); security.close(); TenantContextHolder.clear();
    }
    private void sign(String domain) {
        var command = new ElectronicSignatureCommand("MES", "APPROVE", "TYPE", "10", "v1", "password", "批准", "idem", null, null);
        if ("ordinary".equals(domain)) writer.sign(command);
        else writer.signAuthorized(command, new AuthorizedSignatureIdentity(domain, 9L, "签署时姓名"),
                "MES_EMPLOYEE_PROFILE".equals(domain) ? mock(Runnable.class) : null);
        clearInvocations(adapter, users);
    }

    private void configureDccApproval() {
        when(adapter.moduleCode()).thenReturn("DCC");
        when(adapter.supportedActions()).thenReturn(Set.of(
                new SignatureActionDefinition("DCC", "APPROVE", "DCC_CONTROLLED_FILE", "APPROVE", "批准", "p1"),
                new SignatureActionDefinition("DCC", "REJECT", "DCC_CONTROLLED_FILE", "REJECT", "驳回", "p1")));
        when(adapter.isReasonRequired("APPROVE", "DCC_CONTROLLED_FILE")).thenReturn(false);
        when(adapter.serverTimeResolution(any(), any())).thenReturn(ElectronicSignatureSubjectAdapter.ServerTimeResolution.MICROS);
        when(adapter.loadAndAuthorize(any())).thenReturn(new SignatureSubjectSnapshot("DCC_CONTROLLED_FILE", "10", "v1",
                "{\"value\":\"文件审批事实\"}", null, null, null, "pi", "task", "node", 1));
    }

    private void signEmptyDccApproval(String reason) {
        configureDccApproval();
        writer.sign(new ElectronicSignatureCommand("DCC", "APPROVE", "DCC_CONTROLLED_FILE", "10", "v1",
                "password", reason, "empty-dcc-approval", null, null));
        verify(users).reauthenticateForSignature(7L, "password");
        clearInvocations(adapter, users);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\r\n ", "\u00a0\u2003\ufeff"})
    void emptyDccApprovalPassesActualWriterAndVerifierWithSystemReasonSource(String reason) {
        signEmptyDccApproval(reason);
        assertEquals("", record.getReason());
        assertEquals("", audit.getReason());
        assertEquals("SYSTEM", audit.getReasonSource());
        assertEquals("APPROVE", JsonUtils.parseTree(audit.getAfterStateJson()).path("actionCode").asText());
        assertEquals("", JsonUtils.parseTree(audit.getAfterStateJson()).path("reason").asText());
        var verification = query.verifyEvidence(10L);
        assertEquals("VALID", verification.verificationStatus());
        assertEquals("CONSISTENT", verification.auditBindingStatus());
        assertEquals("CONSISTENT", verification.timeConsistencyStatus());
        assertEquals("SERVER_MICROS_CLOCK", verification.timeSource());
        assertTrue(record.getTimeEvidenceId().startsWith("SERVER_MICROS_CLOCK:"));
        assertEquals(0, record.getSignedAt().getNano() % 1000);
        assertEquals("", verifiedFacts.requireVerifiedFact(10L).reason());
        verifyNoInteractions(adapter, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeColumn", "crossMicrosecond", "subMicrosecond", "secondsPrefix"})
    void microsecondApprovalRejectsChangedTimeAndUnsupportedPrecision(String mutation) {
        signEmptyDccApproval("");
        var at = java.time.LocalDateTime.of(2026, 10, 27, 10, 0, 0, 123456000);
        record.setSignedAt(at);
        record.setTimeEvidenceId("SERVER_MICROS_CLOCK:" + at);
        switch (mutation) {
            case "timeColumn" -> record.setSignedAt(at.plusNanos(1000));
            case "crossMicrosecond" -> record.setTimeEvidenceId("SERVER_MICROS_CLOCK:" + at.plusNanos(1000));
            case "subMicrosecond" -> {
                record.setSignedAt(at.plusNanos(1));
                record.setTimeEvidenceId("SERVER_MICROS_CLOCK:" + record.getSignedAt());
            }
            case "secondsPrefix" -> record.setTimeEvidenceId("SERVER_CLOCK:" + at);
            default -> throw new AssertionError(mutation);
        }
        // Recompute synthetic hashes to isolate precision/value validation from digest tampering.
        rehashSignature();
        Map<String,Object> state = ReflectionTestUtils.invokeMethod(verifiedFacts, "afterState",
                (Object) ReflectionTestUtils.invokeMethod(verifiedFacts, "freeze", record));
        audit.setAfterStateJson(JsonUtils.toJsonString(state));
        rehashAudit();
        var result = query.verifyEvidence(10L);
        assertEquals("CONSISTENT", result.digestStatus());
        assertEquals("CONSISTENT", result.auditBindingStatus());
        assertEquals("MISMATCH", result.timeConsistencyStatus());
        assertTrue(result.reasonCodes().contains("TIME_VALUE_MISMATCH"));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> verifiedFacts.requireVerifiedFact(10L));
        verifyNoInteractions(adapter, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reason", "reasonSource", "rejection", "otherAction"})
    void emptyApprovalStillRejectsAlteredReasonSourceAndNonApprovalHistory(String mutation) {
        signEmptyDccApproval("");
        switch (mutation) {
            case "reason" -> { record.setReason("改写意见"); rehashSignature(); }
            case "reasonSource" -> { audit.setReasonSource("USER"); rehashAudit(); }
            case "rejection", "otherAction" -> {
                record.setActionCode("rejection".equals(mutation) ? "REJECT" : "DISTRIBUTION_ACK");
                rehashSignature();
                Map<String,Object> state = ReflectionTestUtils.invokeMethod(verifiedFacts, "afterState",
                        (Object) ReflectionTestUtils.invokeMethod(verifiedFacts, "freeze", record));
                audit.setAfterStateJson(JsonUtils.toJsonString(state));
                audit.setReasonSource("USER");
                rehashAudit();
            }
            default -> throw new AssertionError(mutation);
        }
        var result = query.verifyEvidence(10L);
        assertNotEquals("VALID", result.verificationStatus());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> verifiedFacts.requireVerifiedFact(10L));
        if (Set.of("rejection", "otherAction").contains(mutation)) {
            assertTrue(result.reasonCodes().contains("SIGNATURE_FACT_INVALID"));
        }
        verifyNoInteractions(adapter, users);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\r\n ", "\u00a0\u2003\ufeff"})
    void emptyDccRejectionRemainsRejectedBeforeSignatureOrAuditWrite(String reason) {
        configureDccApproval();
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> writer.sign(new ElectronicSignatureCommand("DCC", "REJECT", "DCC_CONTROLLED_FILE", "10", "v1",
                        "password", reason, "empty-dcc-rejection", null, null)));
        verify(records, never()).insert(any(ElectronicSignatureRecordDO.class));
        verify(audits, never()).insert(any(GxpAuditEventDO.class));
        verifyNoInteractions(users);
    }
    @ParameterizedTest @ValueSource(strings={"ordinary", "SYSTEM_USER", "MES_EMPLOYEE_PROFILE"})
    void generatedV1FactsPassActualDigestTimeAndCreationAudit(String domain) {
        sign(domain);
        var result = query.verifyEvidence(10L);
        assertEquals("VALID", result.verificationStatus()); assertEquals("CONSISTENT", result.digestStatus());
        assertEquals("CONSISTENT", result.timeConsistencyStatus()); assertEquals("CONSISTENT", result.auditBindingStatus());
        assertEquals("NOT_BOUND", result.timeTrustStatus());
        assertEquals("LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", result.verificationScope()); assertEquals("V1", result.evidenceFormatVersion());
        var evidence = query.getById(10L);
        assertEquals("ordinary".equals(domain) ? null : "签署时姓名", evidence.actorDisplayName());
        assertEquals("ordinary".equals(domain) ? "MISSING" : "SNAPSHOT", evidence.identitySnapshotStatus());
        assertEquals("ordinary".equals(domain) ? null : domain, evidence.identityDomain());
        var serialized = JsonUtils.parseTree(JsonUtils.toJsonString(evidence));
        assertEquals(evidence.identitySnapshotStatus(), serialized.get("identitySnapshotStatus").asText());
        assertEquals("V1", serialized.get("evidenceFormatVersion").asText());
        verifyNoInteractions(adapter, users);
    }
    @Test void currentAdapterAndDirectoryNeverParticipateInHistoricalVerification() {
        sign("SYSTEM_USER"); when(adapter.loadAndAuthorize(any())).thenThrow(new IllegalStateException("current business changed"));
        when(users.getUser(anyLong())).thenThrow(new IllegalStateException("disabled or renamed user")); clearInvocations(adapter, users);
        assertEquals("VALID", query.verifyEvidence(10L).verificationStatus()); assertEquals("签署时姓名", query.getById(10L).actorDisplayName());
        verifyNoInteractions(adapter, users);
    }
    @Test void databaseJsonFormattingUsesOnlyPersistedSemanticCanonicalization() {
        sign("ordinary");
        record.setCanonicalContentJson("{ \"list\" : [1, 2], \"value\" : \"中文事实\" }");
        assertEquals("VALID", query.verifyEvidence(10L).verificationStatus()); verifyNoInteractions(adapter, users);
    }
    @Test void businessKeysDoNotDeclareASignatureProtocol() {
        when(adapter.loadAndAuthorize(any())).thenReturn(new SignatureSubjectSnapshot("TYPE", "10", "v1",
                "{\"evidenceFormatVersion\":\"business-V2\",\"commandHashVersion\":{\"business\":1}}",
                null, null, null, "pi", "task", "node", 1));
        sign("ordinary");
        assertEquals("VALID", query.verifyEvidence(10L).verificationStatus());
        assertEquals("V1", query.verifyEvidence(10L).evidenceFormatVersion());
        assertEquals("V1", query.getById(10L).evidenceFormatVersion());
        verifyNoInteractions(adapter, users);
    }
    @ParameterizedTest @ValueSource(strings={"timeColumn", "crossSecond", "invalidTime", "missingTime", "missingSignedAt", "algorithm", "businessFieldTamper", "json", "jsonAndHashes", "missingBody", "unknownKeyVersion", "identityActor", "missingAudit", "duplicateAudit", "auditSource"})
    void refusesIncompleteOrContradictoryHistory(String mutation) {
        sign("SYSTEM_USER");
        switch (mutation) {
            case "timeColumn" -> record.setSignedAt(record.getSignedAt().plusSeconds(1));
            case "crossSecond" -> record.setTimeEvidenceId("SERVER_CLOCK:" + record.getSignedAt().plusSeconds(1));
            case "invalidTime" -> record.setTimeEvidenceId("SERVER_CLOCK:not-a-time");
            case "missingTime" -> record.setTimeEvidenceId(null);
            case "missingSignedAt" -> record.setSignedAt(null);
            case "algorithm" -> record.setAlgorithm("MD5");
            case "businessFieldTamper" -> record.setCanonicalContentJson(record.getCanonicalContentJson().replaceFirst("\\{", "{\"evidenceFormatVersion\":\"V2\","));
            case "json" -> record.setCanonicalContentJson("{");
            case "jsonAndHashes" -> { record.setCanonicalContentJson(record.getCanonicalContentJson().replace("中文事实", "改写事实")); rehashSignature(); }
            case "missingBody" -> record.setCanonicalContentJson(null);
            case "unknownKeyVersion" -> record.setKeyVersion("unknown");
            case "identityActor" -> { record.setActorId(8L); rehashSignature(); }
            case "missingAudit" -> when(audits.selectSignatureCreationEvents(1L, "10")).thenReturn(List.of());
            case "duplicateAudit" -> when(audits.selectSignatureCreationEvents(1L, "10")).thenReturn(List.of(audit, audit));
            case "auditSource" -> audit.setSourceLocator("other");
            default -> throw new AssertionError(mutation);
        }
        var result = query.verifyEvidence(10L);
        assertNotEquals("VALID", result.verificationStatus());
        if (Set.of("invalidTime", "missingTime", "missingSignedAt").contains(mutation))
            assertEquals("MISSING", result.timeConsistencyStatus());
        if (Set.of("timeColumn", "crossSecond").contains(mutation)) {
            assertEquals("MISMATCH", result.timeConsistencyStatus());
            assertTrue(result.reasonCodes().contains("TIME_VALUE_MISMATCH"));
        }
        verifyNoInteractions(adapter, users);
    }
    @ParameterizedTest @ValueSource(strings={"SERVER_CLOCK:not-a-time", "UNSUPPORTED:2026-10-06T10:00", ""})
    void missingOrInvalidTimeIsIncompleteWhenDigestAndAuditHaveNoIndependentContradiction(String evidence) {
        sign("ordinary"); record.setTimeEvidenceId(evidence.isEmpty() ? null : evidence); rehashSignature();
        // Synthetic stored fixture isolates time classification without claiming the writer emits bad time.
        Map<String,Object> state = ReflectionTestUtils.invokeMethod(verifiedFacts, "afterState",
                (Object) ReflectionTestUtils.invokeMethod(verifiedFacts, "freeze", record));
        audit.setAfterStateJson(JsonUtils.toJsonString(state)); rehashAudit();
        var result = query.verifyEvidence(10L);
        assertEquals("CONSISTENT", result.digestStatus()); assertEquals("CONSISTENT", result.auditBindingStatus());
        assertEquals("MISSING", result.timeConsistencyStatus()); assertEquals("INCOMPLETE", result.verificationStatus());
        assertEquals("NOT_BOUND", result.timeTrustStatus());
        assertTrue(result.reasonCodes().contains(evidence.isEmpty() ? "TIME_EVIDENCE_MISSING" : "TIME_EVIDENCE_INVALID"));
    }
    private void rehashSignature() {
        record.setContentHash(DigestUtil.sha256Hex(ElectronicSignatureJsonCanonicalizer.canonicalize(record.getCanonicalContentJson())));
        String payload = ReflectionTestUtils.invokeMethod(verifiedFacts, "evidencePayload",
                (Object) ReflectionTestUtils.invokeMethod(verifiedFacts, "freeze", record), record.getContentHash());
        record.setEvidenceHash(DigestUtil.sha256Hex(payload));
    }
    @Test void minuteAndSecondRepresentationsCompareValuesWithoutRewritingByteProtocol() {
        sign("ordinary");
        var at = java.time.LocalDateTime.of(2026, 10, 6, 10, 0, 0);
        record.setSignedAt(at); record.setTimeEvidenceId("SERVER_CLOCK:2026-10-06T10:00"); rehashSignature();
        // Original after-state is updated only in this synthetic fixture; production never repairs historical rows.
        Map<String,Object> state = ReflectionTestUtils.invokeMethod(verifiedFacts, "afterState",
                (Object) ReflectionTestUtils.invokeMethod(verifiedFacts, "freeze", record));
        audit.setAfterStateJson(JsonUtils.toJsonString(state)); rehashAudit();
        assertEquals("VALID", query.verifyEvidence(10L).verificationStatus());
        record.setTimeEvidenceId("SERVER_CLOCK:2026-10-06T10:00:00"); rehashSignature();
        state = ReflectionTestUtils.invokeMethod(verifiedFacts, "afterState",
                (Object) ReflectionTestUtils.invokeMethod(verifiedFacts, "freeze", record));
        audit.setAfterStateJson(JsonUtils.toJsonString(state)); rehashAudit();
        assertEquals("VALID", query.verifyEvidence(10L).verificationStatus());
    }
    private void rehashAudit() {
        audit.setStatePayloadHash(DigestUtil.sha256Hex(audit.getBeforeStateJson() + "\u001f" + audit.getAfterStateJson()));
        String canonical = ReflectionTestUtils.invokeMethod(new GxpAuditServiceImpl(), "canonicalEvent", audit);
        audit.setCanonicalEventJson(canonical); audit.setEventHash(DigestUtil.sha256Hex(canonical));
    }
    @Test void contentProtocolKeepsArrayOrderNullMissingAndTextWhitespace() {
        assertEquals(ElectronicSignatureJsonCanonicalizer.canonicalize("{\"a\":1,\"b\":2}"), ElectronicSignatureJsonCanonicalizer.canonicalize("{\"b\":2,\"a\":1}"));
        assertNotEquals(ElectronicSignatureJsonCanonicalizer.canonicalize("[1,2]"), ElectronicSignatureJsonCanonicalizer.canonicalize("[2,1]"));
        assertNotEquals(ElectronicSignatureJsonCanonicalizer.canonicalize("{\"a\":null}"), ElectronicSignatureJsonCanonicalizer.canonicalize("{}"));
        assertNotEquals(ElectronicSignatureJsonCanonicalizer.canonicalize("{\"a\":\"\"}"), ElectronicSignatureJsonCanonicalizer.canonicalize("{\"a\":null}"));
        assertNotEquals(ElectronicSignatureJsonCanonicalizer.canonicalize("{\"a\":\" x \"}"), ElectronicSignatureJsonCanonicalizer.canonicalize("{\"a\":\"x\"}"));
    }
    @Test void databaseReadFailuresPropagateAndCrossTenantIsRejected() {
        sign("ordinary"); when(audits.selectSignatureCreationEvents(1L, "10")).thenThrow(new IllegalStateException("audit DB offline"));
        assertThrows(IllegalStateException.class, () -> query.verifyEvidence(10L));
        when(records.selectById(10L)).thenThrow(new IllegalStateException("signature DB offline"));
        assertThrows(IllegalStateException.class, () -> query.verifyEvidence(10L));
        doReturn(record).when(records).selectById(10L); record.setTenantId(2L);
        assertNull(query.getById(10L)); assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> query.verifyEvidence(10L));
    }
}
