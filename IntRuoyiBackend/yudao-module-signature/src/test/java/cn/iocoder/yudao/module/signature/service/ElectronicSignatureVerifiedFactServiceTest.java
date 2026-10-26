package cn.iocoder.yudao.module.signature.service;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Real signature/GxP writers and creation verifier; only persistence, authentication,
 * policy/ledger and adapter ports are mocked. Captured INSERT objects are not a DB round-trip.
 * Synthetic fixtures below isolate original V1 classifications, never repair historical rows.
 * No global mapper replacement, MES parser, V2 writer or physical-transaction assertion.
 */
class ElectronicSignatureVerifiedFactServiceTest {
    private static final List<String> FACT_FIELDS = List.of(
            "tenantId", "id", "moduleCode", "actionCode", "subjectType", "subjectId", "subjectVersion",
            "actorId", "meaningCode", "meaningLabel", "reason", "signedAt", "timeEvidenceId",
            "authenticationMethod", "contentHash", "beforeContentHash", "afterContentHash",
            "canonicalContentJson", "beforeContentJson", "afterContentJson", "fieldDiffJson",
            "evidenceHash", "algorithm", "keyVersion", "policyVersion", "verificationStatus",
            "idempotencyKey", "commandHash", "processInstanceId", "taskId", "nodeCode", "nodeOrder");
    private ElectronicSignatureVerifiedFactService service;
    private ElectronicSignatureServiceImpl writer;
    private ElectronicSignatureRecordMapper records;
    private GxpAuditEventMapper audits;
    private ElectronicSignatureSubjectAdapter adapter;
    private AdminUserApi users;
    private ElectronicSignatureRecordDO record;
    private GxpAuditEventDO audit;
    private org.mockito.MockedStatic<SecurityFrameworkUtils> security;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var login = new LoginUser();
        login.setId(7L); login.setTenantId(1L); login.setUserType(2);
        login.setInfo(Map.of("username", "operator", LoginUser.INFO_KEY_NICKNAME, "设备操作员"));
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
        security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(login);
        records = mock(ElectronicSignatureRecordMapper.class);
        audits = mock(GxpAuditEventMapper.class);
        users = mock(AdminUserApi.class);
        adapter = mock(ElectronicSignatureSubjectAdapter.class);
        when(adapter.moduleCode()).thenReturn("MES");
        when(adapter.serverTimeResolution(any(), any())).thenCallRealMethod();
        when(adapter.bindServerSignedAt(any(), any(), any())).thenCallRealMethod();
        when(adapter.supportedActions()).thenReturn(Set.of(
                new SignatureActionDefinition("MES", "APPROVE", "TYPE", "APPROVE", "批准", "p1")));
        snapshot("{\"value\":\"中文事实\",\"list\":[1,2]}", null, null, null);
        when(records.insert(any(ElectronicSignatureRecordDO.class))).thenAnswer(call -> {
            record = call.getArgument(0); record.setId(10L); return 1;
        });
        when(records.selectById(10L)).thenAnswer(call -> record);
        when(audits.insert(any(GxpAuditEventDO.class))).thenAnswer(call -> {
            audit = call.getArgument(0); audit.setId(20L); return 1;
        });
        when(audits.selectSignatureCreationEvents(1L, "10"))
                .thenAnswer(call -> audit == null ? List.of() : List.of(audit));

        var ledgerMapper = mock(GxpAuditLedgerSequenceMapper.class);
        var ledger = new GxpAuditLedgerSequenceDO();
        ledger.setTenantId(1L); ledger.setNextLedgerSequence(1L);
        when(ledgerMapper.selectByTenantIdForUpdate(1L)).thenReturn(ledger);
        when(ledgerMapper.updateById(any(GxpAuditLedgerSequenceDO.class))).thenReturn(1);
        var activationMapper = mock(GxpAuditPolicyActivationMapper.class);
        var activation = new GxpAuditPolicyActivationDO(); activation.setPolicyVersion("audit-p1");
        when(activationMapper.selectLatestForUpdate(1L)).thenReturn(activation);
        var policyMapper = mock(GxpAuditPolicyOperationMapper.class);
        var policy = new GxpAuditPolicyOperationDO();
        policy.setActive(true); policy.setApplicability("GXP"); policy.setPolicyVersion("audit-p1");
        policy.setDomain("SIGNATURE"); policy.setSubjectType("SIGNATURE_RECORD"); policy.setActionType("CREATE");
        policy.setReasonPolicy("USER_REQUIRED"); policy.setSignaturePolicy("REQUIRED");
        when(policyMapper.selectByPolicyVersionForUpdate(1L, "audit-p1", "signature.record.create"))
                .thenReturn(policy);

        var auditUsers = mock(cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper.class);
        var auditActor = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
        auditActor.setId(7L); auditActor.setTenantId(1L); auditActor.setStatus(0);
        auditActor.setUsername("operator"); auditActor.setNickname("设备操作员");
        when(auditUsers.selectGxpAuditActor(1L, 7L)).thenReturn(auditActor);
        var auditWriter = new GxpAuditServiceImpl();
        ReflectionTestUtils.setField(auditWriter, "adminUserMapper", auditUsers);
        ReflectionTestUtils.setField(auditWriter, "auditEventMapper", audits);
        ReflectionTestUtils.setField(auditWriter, "ledgerSequenceMapper", ledgerMapper);
        ReflectionTestUtils.setField(auditWriter, "policyActivationMapper", activationMapper);
        ReflectionTestUtils.setField(auditWriter, "policyOperationMapper", policyMapper);
        ReflectionTestUtils.setField(auditWriter, "eventRelationMapper", mock(GxpAuditEventRelationMapper.class));
        writer = new ElectronicSignatureServiceImpl();
        ReflectionTestUtils.setField(writer, "signatureRecordMapper", records);
        ReflectionTestUtils.setField(writer, "adminUserApi", users);
        ReflectionTestUtils.setField(writer, "gxpAuditService", auditWriter);
        ReflectionTestUtils.setField(writer, "subjectAdapters", List.of(adapter));
        var verifier = new SignatureCreationVerificationService();
        ReflectionTestUtils.setField(verifier, "auditEventMapper", audits);
        // Candidate explicit composition API from the parent; implementation choice remains under review.
        service = new ElectronicSignatureVerifiedFactService(records, verifier);
    }

    @AfterEach
    void cleanup() {
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                for (var synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                    synchronization.afterCompletion(1);
                }
                TransactionSynchronizationManager.clearSynchronization();
            }
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            if (security != null) security.close();
            TenantContextHolder.clear();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ordinary", "SYSTEM_USER", "MES_EMPLOYEE_PROFILE"})
    void actualWritersAndVerifierCreateFullyBoundFrozenToken(String domain) {
        sign(domain, "批准");
        Map<String, Object> before = facts();
        LocalDateTime start = shanghaiNow();
        var token = service.requireVerifiedFact(10L);
        LocalDateTime end = shanghaiNow();
        assertFrozen(before, token);
        assertEquals(20L, token.auditEventId());
        assertValid(token.verification(), "ordinary".equals(domain)
                ? List.of("IDENTITY_SNAPSHOT_MISSING") : List.of());
        assertWithin(token.verification().verifiedAt(), start, end);
        assertEquals(before, facts());
        verifyReadOnceAndAuditOnce();
        verifyNoInteractions(adapter, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"diagnostic", "token"})
    void eachEntrySelectsOnceAndExecutesAuditAfterRecordRead(String entry) {
        sign("ordinary", "批准");
        run(entry);
        var order = inOrder(records, audits);
        order.verify(records).selectById(10L);
        order.verify(audits).selectSignatureCreationEvents(1L, "10");
        verifyReadOnceAndAuditOnce();
        verify(records, never()).insert(any(ElectronicSignatureRecordDO.class));
        verify(audits, never()).insert(any(GxpAuditEventDO.class));
        verifyNoInteractions(adapter, users);
    }

    @Test
    void developmentDisablesOnlyAuditBindingAndStillRejectsTamperedSignature() {
        sign("SYSTEM_USER", "批准");
        var env = new org.springframework.mock.env.MockEnvironment()
                .withProperty(GxpAuditDevelopmentMode.PROPERTY, "true")
                .withProperty("spring.datasource.dynamic.datasource.master.url", "jdbc:mysql://127.0.0.1/test");
        env.setActiveProfiles("local");
        try {
            new GxpAuditDevelopmentMode(env);
            clearInvocations(audits);
            var fact=service.requireVerifiedFact(10L);
            assertEquals("CONSISTENT", fact.verification().digestStatus());
            assertEquals("CONSISTENT", fact.verification().timeConsistencyStatus());
            assertEquals("DEVELOPMENT_DISABLED", fact.verification().auditBindingStatus());
            assertTrue(fact.auditEventId()<0);
            verifyNoInteractions(audits);
            record.setCanonicalContentJson("{\"tampered\":true}");
            assertThrows(ServiceException.class, ()->service.requireVerifiedFact(10L));
            verifyNoInteractions(audits);
        } finally { new GxpAuditDevelopmentMode(new org.springframework.mock.env.MockEnvironment()); }
    }

    @Test
    void copiesAllScalarsBeforeRealAuditQueryCanMutateSourceDo() {
        snapshot("{\"value\":\"中文事实\"}", "{\"old\":1}", "{\"new\":2}", "{\"field\":\"x\"}");
        sign("SYSTEM_USER", "批准");
        Map<String, Object> before = facts();
        when(audits.selectSignatureCreationEvents(1L, "10")).thenAnswer(call -> {
            var bean = new BeanWrapperImpl(record);
            for (String field : FACT_FIELDS) bean.setPropertyValue(field, null);
            record.setTenantId(2L); record.setId(99L);
            return List.of(audit); // Real verifier still checks the writer's original protected audit.
        });
        var token = service.requireVerifiedFact(10L);
        assertFrozen(before, token);
        assertValid(token.verification(), List.of());
        assertEquals(20L, token.auditEventId());
        assertEquals(2L, record.getTenantId());
        assertNull(record.getCanonicalContentJson());
        verifyReadOnceAndAuditOnce();
    }

    @Test
    void tokenAndReasonListRemainImmutableAfterSourceMutation() {
        sign("ordinary", "批准");
        Map<String, Object> before = facts();
        var token = service.requireVerifiedFact(10L);
        record.setReason("后来原因"); record.setCanonicalContentJson("{\"changed\":true}");
        record.setTenantId(2L); record.setSignedAt(record.getSignedAt().plusDays(1));
        assertFrozen(before, token);
        assertThrows(UnsupportedOperationException.class,
                () -> token.verification().reasonCodes().add("FORGED"));
        assertEquals(List.of("IDENTITY_SNAPSHOT_MISSING"), token.verification().reasonCodes());
    }

    @Test
    void tokenHasOnlyPrivateConstructionFinalStateAndReadOnlyScalarAccessors() {
        var type = ElectronicSignatureVerifiedFactService.VerifiedSignatureFact.class;
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isStatic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertTrue(type.getDeclaredConstructors().length > 0);
        for (var ctor : type.getDeclaredConstructors()) assertTrue(Modifier.isPrivate(ctor.getModifiers()));
        for (var field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                assertTrue(Modifier.isPrivate(field.getModifiers()), field.getName());
                assertTrue(Modifier.isFinal(field.getModifiers()), field.getName());
                assertFalse(ElectronicSignatureRecordDO.class.isAssignableFrom(field.getType()));
                assertFalse(Map.class.isAssignableFrom(field.getType()));
                assertFalse(JsonNode.class.isAssignableFrom(field.getType()));
            }
        }
        for (var method : type.getMethods()) {
            assertFalse(method.getName().startsWith("set"));
            assertFalse(method.getName().equals("builder"));
            assertFalse(Modifier.isStatic(method.getModifiers())
                    && type.isAssignableFrom(method.getReturnType()), method.getName());
            assertFalse(ElectronicSignatureRecordDO.class.isAssignableFrom(method.getReturnType()));
            assertFalse(Map.class.isAssignableFrom(method.getReturnType()));
            assertFalse(JsonNode.class.isAssignableFrom(method.getReturnType()));
        }
        for (String name : FACT_FIELDS) assertDoesNotThrow(() -> type.getMethod(name));
        assertDoesNotThrow(() -> type.getMethod("verification"));
        assertDoesNotThrow(() -> type.getMethod("auditEventId"));
        for (var method : ElectronicSignatureVerifiedFactService.class.getMethods()) {
            if (type.isAssignableFrom(method.getReturnType())) {
                assertEquals("requireVerifiedFact", method.getName());
                assertArrayEquals(new Class<?>[]{Long.class}, method.getParameterTypes());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"diagnostic", "token"})
    void rawNonBlankReasonAndTrimmedBusinessReasonRemainDifferentAndValid(String entry) {
        snapshot("{\"reason\":\"批准\"}", null, null, null);
        sign("ordinary", "  批准  ");
        Map<String, Object> before = facts();
        assertEquals("  批准  ", audit.getReason());
        assertEquals("批准", JsonUtils.parseTree(record.getCanonicalContentJson()).get("reason").textValue());
        if ("diagnostic".equals(entry)) {
            assertValid(service.verifyEvidence(10L), List.of("IDENTITY_SNAPSHOT_MISSING"));
        } else {
            var token = service.requireVerifiedFact(10L);
            assertEquals("  批准  ", token.reason());
            assertFrozen(before, token);
        }
        assertEquals(before, facts());
        verifyReadOnceAndAuditOnce();
    }

    @ParameterizedTest
    @ValueSource(strings = {"absent", "null", "missingName"})
    void originalOptionalIdentityAndMissingNameRemainLegalV1(String identity) {
        if ("null".equals(identity)) snapshot("{\"signatureIdentity\":null,\"value\":1}", null, null, null);
        sign("missingName".equals(identity) ? "SYSTEM_USER" : "ordinary", "批准");
        if ("missingName".equals(identity)) {
            ObjectNode body = (ObjectNode) JsonUtils.parseTree(record.getCanonicalContentJson());
            ((ObjectNode) body.get("signatureIdentity")).remove("displayName");
            record.setCanonicalContentJson(body.toString());
            repairSyntheticFactAndAudit();
        }
        var token = service.requireVerifiedFact(10L);
        assertValid(token.verification(), List.of("IDENTITY_SNAPSHOT_MISSING"));
        assertEquals(20L, token.auditEventId());
        verifyReadOnceAndAuditOnce();
    }

    @ParameterizedTest
    @ValueSource(strings = {"diagnostic", "token"})
    void anotherCallReadsChangedFactsRatherThanCachingOldValid(String entry) {
        sign("ordinary", "批准");
        var first = service.requireVerifiedFact(10L);
        clearInvocations(records, audits);
        record.setCanonicalContentJson("{\"value\":\"tampered\"}");
        if ("diagnostic".equals(entry)) assertEquals("MISMATCH", service.verifyEvidence(10L).verificationStatus());
        else assertThrows(ServiceException.class, () -> service.requireVerifiedFact(10L));
        assertValid(first.verification(), List.of("IDENTITY_SNAPSHOT_MISSING"));
        assertEquals("{\"list\":[1,2],\"value\":\"中文事实\"}", first.canonicalContentJson());
        verifyReadOnceAndAuditOnce();
    }

    @ParameterizedTest
    @ValueSource(strings = {"missingAudit", "duplicateAudit", "auditIntegrity", "auditId", "auditSource",
            "contentHash", "evidenceHash", "timeValue", "storedStatus", "blankReason", "unknownKeyVersion"})
    void contradictionsAndMissingAuditNeverYieldToken(String mutation) {
        sign("ordinary", "批准");
        switch (mutation) {
            case "missingAudit" -> when(audits.selectSignatureCreationEvents(1L, "10")).thenReturn(List.of());
            case "duplicateAudit" -> when(audits.selectSignatureCreationEvents(1L, "10")).thenReturn(List.of(audit, audit));
            case "auditIntegrity" -> audit.setEventHash("0".repeat(64));
            case "auditId" -> audit.setId(0L);
            case "auditSource" -> { audit.setSourceLocator("unregistered"); rehashSyntheticAudit(); }
            case "contentHash" -> record.setContentHash("0".repeat(64));
            case "evidenceHash" -> record.setEvidenceHash("0".repeat(64));
            case "timeValue" -> record.setSignedAt(record.getSignedAt().plusSeconds(1));
            case "storedStatus" -> record.setVerificationStatus("PENDING");
            case "blankReason" -> record.setReason(" ");
            case "unknownKeyVersion" -> record.setKeyVersion("unknown");
            default -> throw new AssertionError(mutation);
        }
        var diagnosis = service.verifyEvidence(10L);
        assertEquals("missingAudit".equals(mutation) ? "INCOMPLETE" : "MISMATCH", diagnosis.verificationStatus());
        clearInvocations(records, audits);
        assertThrows(ServiceException.class, () -> service.requireVerifiedFact(10L));
        verifyReadOnceAndAuditOnce();
    }

    @ParameterizedTest
    @ValueSource(strings = {"missingBody", "malformedBody", "identityMismatch"})
    void unsupportedOrUnboundContentKeepsOriginalZeroAuditBranch(String mutation) {
        sign("SYSTEM_USER", "批准");
        switch (mutation) {
            case "missingBody" -> record.setCanonicalContentJson(null);
            case "malformedBody" -> record.setCanonicalContentJson("{");
            case "identityMismatch" -> record.setActorId(8L);
            default -> throw new AssertionError(mutation);
        }
        var diagnosis = service.verifyEvidence(10L);
        assertEquals("missingBody".equals(mutation) ? "INCOMPLETE" : "MISMATCH", diagnosis.verificationStatus());
        assertEquals("missingBody".equals(mutation) ? "MISSING" : "MISMATCH", diagnosis.auditBindingStatus());
        assertEquals("identityMismatch".equals(mutation) ? "V1" : "UNSUPPORTED", diagnosis.evidenceFormatVersion());
        verify(records).selectById(10L);
        verify(audits, never()).selectSignatureCreationEvents(anyLong(), anyString());
        clearInvocations(records, audits);
        assertThrows(ServiceException.class, () -> service.requireVerifiedFact(10L));
        verify(records).selectById(10L);
        verify(audits, never()).selectSignatureCreationEvents(anyLong(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"diagnostic", "token"})
    void digestFailureDoesNotSkipOriginalAuditInfrastructureException(String entry) {
        sign("ordinary", "批准");
        record.setContentHash("0".repeat(64));
        var failure = new IllegalStateException("unit audit read unavailable");
        when(audits.selectSignatureCreationEvents(1L, "10")).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> run(entry)));
        verifyReadOnceAndAuditOnce();
    }

    @ParameterizedTest
    @ValueSource(strings = {"diagnostic", "token"})
    void signatureReadInfrastructureExceptionPrecedesAuditAndPropagates(String entry) {
        sign("ordinary", "批准");
        var failure = new IllegalStateException("unit signature read unavailable");
        when(records.selectById(10L)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> run(entry)));
        verify(records).selectById(10L);
        verify(audits, never()).selectSignatureCreationEvents(anyLong(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"diagnostic", "token"})
    void nullAuditQueryIsInfrastructureFailureNotMissingEvidence(String entry) {
        sign("ordinary", "批准");
        when(audits.selectSignatureCreationEvents(1L, "10")).thenReturn(null);
        var failure = assertThrows(IllegalStateException.class, () -> run(entry));
        assertEquals("Signature creation audit query returned null", failure.getMessage());
        verifyReadOnceAndAuditOnce();
    }

    @ParameterizedTest
    @ValueSource(strings = {"diagnostic", "token"})
    void invalidInputIdsDoNotReachPersistence(String entry) {
        for (Long id : Arrays.asList(null, 0L, -1L)) {
            assertThrows(ServiceException.class, () -> {
                if ("diagnostic".equals(entry)) service.verifyEvidence(id);
                else service.requireVerifiedFact(id);
            });
        }
        verifyNoInteractions(records, audits);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "crossTenant"})
    void missingAndCrossTenantFactRetainSameExplicitFailure(String mutation) {
        sign("ordinary", "批准");
        if ("missing".equals(mutation)) when(records.selectById(10L)).thenReturn(null);
        else record.setTenantId(2L);
        var diagnosticFailure = assertThrows(ServiceException.class, () -> service.verifyEvidence(10L));
        assertEquals("电子签名命令非法，原因：签名记录不存在", diagnosticFailure.getMessage());
        assertEquals(ESIGN_COMMAND_INVALID.getCode(), diagnosticFailure.getCode());
        clearInvocations(records, audits);
        var tokenFailure = assertThrows(ServiceException.class, () -> service.requireVerifiedFact(10L));
        assertEquals(diagnosticFailure.getCode(), tokenFailure.getCode());
        assertEquals(diagnosticFailure.getMessage(), tokenFailure.getMessage());
        verify(records).selectById(10L);
        verify(audits, never()).selectSignatureCreationEvents(anyLong(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "unsupported"})
    void independentV1OracleIsolatesIncompleteTimeWithoutChangingDigestOrAudit(String time) {
        sign("ordinary", "批准");
        record.setTimeEvidenceId(switch (time) {
            case "missing" -> null;
            case "invalid" -> "SERVER_CLOCK:not-a-time";
            case "unsupported" -> "UNSUPPORTED:2026-10-06T10:00";
            default -> throw new AssertionError(time);
        });
        repairSyntheticFactAndAudit();
        var diagnosis = service.verifyEvidence(10L);
        assertEquals("INCOMPLETE", diagnosis.verificationStatus());
        assertEquals("CONSISTENT", diagnosis.digestStatus());
        assertEquals("MISSING", diagnosis.timeConsistencyStatus());
        assertEquals("CONSISTENT", diagnosis.auditBindingStatus());
        assertEquals(List.of("missing".equals(time) ? "TIME_EVIDENCE_MISSING" : "TIME_EVIDENCE_INVALID",
                "IDENTITY_SNAPSHOT_MISSING"), diagnosis.reasonCodes());
        clearInvocations(records, audits);
        assertThrows(ServiceException.class, () -> service.requireVerifiedFact(10L));
        verifyReadOnceAndAuditOnce();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-06T10:00", "2026-10-06T10:00:00"})
    void originalMinuteAndSecondEvidenceBytesRemainDistinctAndValid(String text) {
        sign("ordinary", "批准");
        record.setSignedAt(LocalDateTime.of(2026, 10, 6, 10, 0));
        record.setTimeEvidenceId("SERVER_CLOCK:" + text);
        repairSyntheticFactAndAudit();
        String expectedHash = DigestUtil.sha256Hex(v1EvidenceOracle(record));
        var token = service.requireVerifiedFact(10L);
        assertEquals(expectedHash, token.evidenceHash());
        assertEquals("SERVER_CLOCK:" + text, token.timeEvidenceId());
        assertValid(token.verification(), List.of("IDENTITY_SNAPSHOT_MISSING"));
        assertNotEquals(DigestUtil.sha256Hex(v1EvidenceOracle(record).replace(
                "SERVER_CLOCK:" + text, "SERVER_CLOCK:" + ("2026-10-06T10:00".equals(text)
                        ? "2026-10-06T10:00:00" : "2026-10-06T10:00"))), expectedHash);
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrongId", "nullId"})
    void mismatchedMapperFactIdCannotYieldTokenWithoutChangingDiagnosticContract(String mutation) {
        sign("ordinary", "批准");
        if ("wrongId".equals(mutation)) {
            record.setId(99L);
            audit.setSignatureRecordId("99");
            repairSyntheticFactAndAudit();
            when(audits.selectSignatureCreationEvents(1L, "99")).thenReturn(List.of(audit));
            var diagnosis = service.verifyEvidence(10L);
            assertEquals(99L, diagnosis.signatureId());
            assertEquals("VALID", diagnosis.verificationStatus());
            assertEquals("CONSISTENT", diagnosis.auditBindingStatus());
            assertEquals(List.of("IDENTITY_SNAPSHOT_MISSING"), diagnosis.reasonCodes());
        } else {
            record.setId(null);
            // Original SignatureAuditExpectation rejects the fact id before the audit SELECT.
            var failure = assertThrows(IllegalArgumentException.class, () -> service.verifyEvidence(10L));
            assertEquals("Signature audit expectation requires positive tenant/signature/operator IDs and frozen state",
                    failure.getMessage());
            verify(audits, never()).selectSignatureCreationEvents(anyLong(), anyString());
        }
        clearInvocations(records, audits);
        assertThrows(ServiceException.class, () -> service.requireVerifiedFact(10L));
        verify(records, times(1)).selectById(10L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"diagnostic", "token"})
    void eachEvaluationSamplesShanghaiClockExactlyOnce(String entry) {
        sign("ordinary", "批准");
        LocalDateTime fixed = LocalDateTime.of(2026, 10, 7, 12, 34, 56);
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(zone)).thenReturn(fixed);
            clock.clearInvocations();
            var diagnosis = "diagnostic".equals(entry) ? service.verifyEvidence(10L)
                    : service.requireVerifiedFact(10L).verification();
            assertEquals(fixed, diagnosis.verifiedAt());
            clock.verify(() -> LocalDateTime.now(zone), times(1));
        }
        verifyReadOnceAndAuditOnce();
    }

    @Test
    void originalReasonOrderingDeduplicationAndShanghaiTimestampArePreserved() {
        sign("ordinary", "批准");
        record.setEvidenceHash("0".repeat(64));
        record.setTimeEvidenceId("SERVER_CLOCK:not-a-time");
        LocalDateTime start = shanghaiNow();
        var diagnosis = service.verifyEvidence(10L);
        LocalDateTime end = shanghaiNow();
        assertEquals("MISMATCH", diagnosis.verificationStatus());
        assertEquals(List.of("DIGEST_MISMATCH", "TIME_EVIDENCE_INVALID",
                "AUDIT_BINDING_MISMATCH", "IDENTITY_SNAPSHOT_MISSING"), diagnosis.reasonCodes());
        assertEquals("LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", diagnosis.verificationScope());
        assertEquals("NOT_BOUND", diagnosis.timeTrustStatus());
        assertWithin(diagnosis.verifiedAt(), start, end);
        verifyReadOnceAndAuditOnce();
    }

    @Test
    void v1TextualLargeIdentityIsRejectedWithoutFixingNumericProtocol() {
        sign("SYSTEM_USER", "批准");
        ObjectNode body = (ObjectNode) JsonUtils.parseTree(record.getCanonicalContentJson());
        ((ObjectNode) body.get("signatureIdentity")).put("signerId", "9223372036854775807");
        record.setActorId(Long.MAX_VALUE); record.setCanonicalContentJson(body.toString());
        repairSyntheticFactAndAudit();
        var diagnosis = service.verifyEvidence(10L);
        assertEquals("MISMATCH", diagnosis.verificationStatus());
        assertEquals(List.of("IDENTITY_BINDING_MISMATCH"), diagnosis.reasonCodes());
        assertEquals("CONSISTENT", diagnosis.digestStatus());
        verify(audits, never()).selectSignatureCreationEvents(anyLong(), anyString());
        assertThrows(ServiceException.class, () -> service.requireVerifiedFact(10L));
    }

    @Test
    void nestedBusinessVersionKeysNeverSelectV2InCurrentV1Storage() {
        snapshot("{\"evidenceFormatVersion\":\"business-V2\",\"commandHashVersion\":{\"x\":1}}",
                null, null, null);
        sign("ordinary", "批准");
        var token = service.requireVerifiedFact(10L);
        assertEquals("V1", token.verification().evidenceFormatVersion());
        assertTrue(token.canonicalContentJson().contains("business-V2"));
        assertValid(token.verification(), List.of("IDENTITY_SNAPSHOT_MISSING"));
    }

    @Test
    void preservedRawJsonAndAllOptionalFactFieldsUseIndependentHashOracle() {
        snapshot("{\"z\":\" x \",\"a\":[2,1],\"n\":null}",
                "{\"old\":1}", "{\"new\":2}", "{\"field\":\"x\"}");
        sign("SYSTEM_USER", "批准");
        JsonNode written = JsonUtils.parseTree(record.getCanonicalContentJson());
        assertFalse(written.has("n")); // Selected writer's Map serialization omits NON_NULL entries.
        String originalCanonical = canonicalOracle(record.getCanonicalContentJson());
        String originalContentHash = record.getContentHash();
        String originalEvidenceHash = record.getEvidenceHash();
        String formatted = " \n" + written.toPrettyString() + "\n ";
        assertNotEquals(record.getCanonicalContentJson(), formatted);
        assertEquals(written, JsonUtils.parseTree(formatted));
        assertEquals(originalCanonical, canonicalOracle(formatted));
        assertEquals(originalContentHash, DigestUtil.sha256Hex(canonicalOracle(formatted)));
        record.setCanonicalContentJson(formatted);
        assertEquals(originalEvidenceHash, DigestUtil.sha256Hex(v1EvidenceOracle(record)));
        Map<String, Object> before = facts();
        var token = service.requireVerifiedFact(10L);
        assertFrozen(before, token);
        assertEquals(DigestUtil.sha256Hex(canonicalOracle(record.getCanonicalContentJson())), token.contentHash());
        assertEquals(DigestUtil.sha256Hex(v1EvidenceOracle(record)), token.evidenceHash());
        assertEquals(before, facts());
        assertNotNull(token.beforeContentHash()); assertNotNull(token.afterContentHash());
        assertEquals("{\"field\":\"x\"}", token.fieldDiffJson());
        assertEquals(formatted, token.canonicalContentJson());
        assertEquals(originalContentHash, token.contentHash());
        assertEquals(originalEvidenceHash, token.evidenceHash());
        assertValid(token.verification(), List.of());

        // Ordinary writer preserves the explicit null without the selected identity Map conversion.
        sign("ordinary", "批准");
        JsonNode ordinary = JsonUtils.parseTree(record.getCanonicalContentJson());
        assertTrue(ordinary.has("n"));
        assertTrue(ordinary.get("n").isNull());
        ObjectNode missing = ((ObjectNode) ordinary).deepCopy();
        missing.remove("n");
        assertNotEquals(canonicalOracle(ordinary.toString()), canonicalOracle(missing.toString()));
        assertNotEquals(DigestUtil.sha256Hex(canonicalOracle(ordinary.toString())),
                DigestUtil.sha256Hex(canonicalOracle(missing.toString())));
        var ordinaryToken = service.requireVerifiedFact(10L);
        assertFrozen(facts(), ordinaryToken);
        assertEquals(record.getCanonicalContentJson(), ordinaryToken.canonicalContentJson());
        assertEquals(DigestUtil.sha256Hex(canonicalOracle(ordinary.toString())), ordinaryToken.contentHash());
        assertEquals(DigestUtil.sha256Hex(v1EvidenceOracle(record)), ordinaryToken.evidenceHash());
        assertValid(ordinaryToken.verification(), List.of("IDENTITY_SNAPSHOT_MISSING"));
    }

    private void snapshot(String content, String before, String after, String diff) {
        when(adapter.loadAndAuthorize(any())).thenReturn(new SignatureSubjectSnapshot(
                "TYPE", "10", "v1", content, before, after, diff, "pi", "task", "node", 1));
    }

    private void sign(String domain, String reason) {
        var command = new ElectronicSignatureCommand("MES", "APPROVE", "TYPE", "10", "v1",
                "unit-only-auth-port-input", reason, "idem", null, null);
        if ("ordinary".equals(domain)) writer.sign(command);
        else writer.signAuthorized(command, new AuthorizedSignatureIdentity(domain, 9L, "签署时姓名"),
                "MES_EMPLOYEE_PROFILE".equals(domain) ? mock(Runnable.class) : null);
        clearInvocations(records, audits, adapter, users);
    }

    private void run(String entry) {
        if ("diagnostic".equals(entry)) service.verifyEvidence(10L);
        else service.requireVerifiedFact(10L);
    }

    private void verifyReadOnceAndAuditOnce() {
        verify(records, times(1)).selectById(10L);
        verify(audits, times(1)).selectSignatureCreationEvents(1L, "10");
    }

    private Map<String, Object> facts() {
        Map<String, Object> values = new LinkedHashMap<>();
        var bean = new BeanWrapperImpl(record);
        for (String field : FACT_FIELDS) values.put(field, bean.getPropertyValue(field));
        return values;
    }

    private static void assertFrozen(Map<String, Object> expected,
            ElectronicSignatureVerifiedFactService.VerifiedSignatureFact token) {
        for (String field : FACT_FIELDS) {
            assertEquals(expected.get(field), ReflectionTestUtils.invokeMethod(token, field), field);
        }
    }

    private static void assertValid(ElectronicSignatureVerificationDTO result, List<String> reasons) {
        assertEquals(10L, result.signatureId());
        assertEquals("VALID", result.verificationStatus());
        assertEquals("CONSISTENT", result.digestStatus());
        assertEquals("CONSISTENT", result.timeConsistencyStatus());
        assertEquals("CONSISTENT", result.auditBindingStatus());
        assertEquals(result.storedContentHash(), result.calculatedContentHash());
        assertEquals(result.storedEvidenceHash(), result.calculatedEvidenceHash());
        assertEquals("SHA-256", result.algorithm());
        assertEquals("system-local-v1", result.keyVersion());
        assertEquals("LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", result.verificationScope());
        assertEquals("SERVER_CLOCK", result.timeSource());
        assertEquals("NOT_BOUND", result.timeTrustStatus());
        assertEquals("V1", result.evidenceFormatVersion());
        assertEquals(reasons, result.reasonCodes());
    }

    private static LocalDateTime shanghaiNow() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
    }

    private static void assertWithin(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        assertNotNull(value);
        assertFalse(value.isBefore(start));
        assertFalse(value.isAfter(end));
    }

    // Independent V1 oracle transcribed from the approved original 23-position pipe contract.
    // Does not call the candidate service, QueryImpl private helpers, or production canonicalizer.
    private static String v1EvidenceOracle(ElectronicSignatureRecordDO r) {
        String time = r.getTimeEvidenceId() != null && r.getTimeEvidenceId().startsWith("SERVER_CLOCK:")
                ? r.getTimeEvidenceId().substring("SERVER_CLOCK:".length()) : String.valueOf(r.getSignedAt());
        return String.join("|", String.valueOf(r.getTenantId()), String.valueOf(r.getActorId()),
                r.getModuleCode(), r.getActionCode(), r.getSubjectType(), r.getSubjectId(), r.getSubjectVersion(),
                r.getMeaningCode(), r.getMeaningLabel(), r.getReason(), time, r.getTimeEvidenceId(),
                r.getAuthenticationMethod(), r.getContentHash(), empty(r.getBeforeContentHash()),
                empty(r.getAfterContentHash()), empty(canonicalOracle(r.getBeforeContentJson())),
                empty(canonicalOracle(r.getAfterContentJson())), empty(canonicalOracle(r.getFieldDiffJson())),
                "SHA-256", r.getKeyVersion(), r.getPolicyVersion(), r.getVerificationStatus());
    }

    private static String empty(String value) { return value == null ? "" : value; }

    private static String canonicalOracle(String json) {
        if (json == null || json.isBlank()) return null;
        return JsonUtils.toJsonString(sortedOracle(JsonUtils.parseTree(json)));
    }

    private static JsonNode sortedOracle(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            List<String> keys = new ArrayList<>();
            node.fieldNames().forEachRemaining(keys::add);
            Collections.sort(keys);
            for (String key : keys) result.set(key, sortedOracle(node.get(key)));
            return result;
        }
        if (node.isArray()) {
            var result = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
            node.forEach(value -> result.add(sortedOracle(value)));
            return result;
        }
        return node; // Preserve explicit null, text whitespace and numeric JSON token types.
    }

    private static Map<String, Object> afterStateOracle(ElectronicSignatureRecordDO r) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("signatureId", r.getId());
        for (String field : List.of("moduleCode", "actionCode", "subjectType", "subjectId", "subjectVersion",
                "actorId", "meaningCode", "meaningLabel", "reason", "signedAt", "authenticationMethod",
                "contentHash", "beforeContentHash", "afterContentHash", "evidenceHash", "policyVersion",
                "verificationStatus", "processInstanceId", "taskId", "nodeCode", "nodeOrder")) {
            state.put(field, new BeanWrapperImpl(r).getPropertyValue(field));
        }
        return state;
    }

    private void repairSyntheticFactAndAudit() {
        record.setContentHash(DigestUtil.sha256Hex(canonicalOracle(record.getCanonicalContentJson())));
        record.setEvidenceHash(DigestUtil.sha256Hex(v1EvidenceOracle(record)));
        audit.setAfterStateJson(JsonUtils.toJsonString(afterStateOracle(record)));
        audit.setSignatureContentHash(record.getContentHash());
        rehashSyntheticAudit();
        clearInvocations(records, audits, adapter, users);
    }

    private void rehashSyntheticAudit() {
        audit.setStatePayloadHash(DigestUtil.sha256Hex(
                audit.getBeforeStateJson() + "\u001f" + audit.getAfterStateJson()));
        ObjectNode original = (ObjectNode) JsonUtils.parseTree(audit.getCanonicalEventJson());
        for (String field : List.of("afterStateJson", "statePayloadHash", "signatureContentHash", "sourceLocator",
                "signatureRecordId")) {
            original.set(field, JsonUtils.parseTree(
                    JsonUtils.toJsonString(new BeanWrapperImpl(audit).getPropertyValue(field))));
        }
        audit.setCanonicalEventJson(JsonUtils.toJsonString(sortedOracle(original)));
        audit.setEventHash(DigestUtil.sha256Hex(audit.getCanonicalEventJson()));
    }
}
