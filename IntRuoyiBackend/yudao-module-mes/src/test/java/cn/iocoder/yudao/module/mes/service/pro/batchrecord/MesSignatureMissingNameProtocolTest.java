package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService.VerifiedSignatureFact;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
import cn.iocoder.yudao.module.system.service.gxpaudit.SignatureCreationVerificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SYNTHETIC_PROTOCOL_TEST_ONLY: explicitly authorized first-persisted V1 fixtures.
 * The absent displayName exists before the FIRST INSERT. No signature writer runs:
 * its V1 SERVICE_METHOD/ElectronicSignatureServiceImpl.sign source marking is a
 * permitted TEST SIMULATION, never evidence of historical or production signing.
 * Real MES adapter -> initial synthetic record -> real GxP append -> real creation
 * verifier -> private server issuer -> real parser. Only actor/policy/ledger/storage
 * ports are substituted. No token mock/reflection, no post-sign digest/audit repair,
 * no physical DB, AOP transaction/lock, datetime(3), directory or permission proof.
 */
class MesSignatureMissingNameProtocolTest {
    private static final String MODULE = "MES";
    private static final String TYPE = "MES_BATCH_RECORD";
    private static final String ACTION = "PRODUCTION_SUBMIT";
    private static final String REASON = "生产提交";
    private static final String MARKER = "SYNTHETIC_PROTOCOL_TEST_ONLY";
    private static final String SOURCE_NAME =
            "{\"activeOrderId\":700,\"routeProcessId\":701,\"processId\":702,"
                    + "\"submissionIdempotencyKey\":\"SYNTHETIC_PROTOCOL_TEST_ONLY\"}";
    private static final LocalDateTime SIGNED_AT = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
    private static final ObjectMapper ORACLE_READER = new ObjectMapper();
    private static final Set<String> BUSINESS_KEYS = Set.of(
            "adapter", "executionId", "actionType", "processInstanceId", "bpmTaskId",
            "bpmTaskDefinitionKey", "bpmTaskName", "signatureCellKey", "signatureRowIndex",
            "signatureColumnIndex", "reviewSourceType", "reviewSourceId", "reviewSourceName",
            "approvalResult", "reason", "fieldAuditRevision", "fieldAuditHeadHash",
            "cellValuesHash", "signatureChallengeHash");
    private static final Set<String> AFTER_STATE_KEYS = Set.of(
            "signatureId", "moduleCode", "actionCode", "subjectType", "subjectId", "subjectVersion",
            "actorId", "meaningCode", "meaningLabel", "reason", "signedAt", "authenticationMethod",
            "contentHash", "beforeContentHash", "afterContentHash", "evidenceHash", "policyVersion",
            "verificationStatus", "processInstanceId", "taskId", "nodeCode", "nodeOrder");

    private final MesBatchRecordSignatureSubjectAdapter adapter = new MesBatchRecordSignatureSubjectAdapter();
    private ElectronicSignatureRecordMapper records;
    private GxpAuditEventMapper audits;
    private GxpAuditLedgerSequenceMapper ledgers;
    private GxpAuditPolicyActivationMapper activations;
    private GxpAuditPolicyOperationMapper policies;
    private GxpAuditEventRelationMapper relations;
    private GxpAuditServiceImpl auditWriter;
    private ElectronicSignatureVerifiedFactService issuer;
    private MockedStatic<SecurityFrameworkUtils> security;
    private ElectronicSignatureRecordDO insertedRecord;
    private ElectronicSignatureRecordDO storedRecord;
    private ElectronicSignatureRecordDO selectedRecord;
    private GxpAuditEventDO insertedAudit;
    private GxpAuditEventDO storedAudit;
    private List<GxpAuditEventDO> selectedAudits;
    private boolean duplicateAudit;
    private int parserCalls;
    private VerifiedSignatureFact issuedToken;
    private String firstRecordImage;
    private String firstAuditImage;
    private String firstAfterState;
    private String firstCanonicalContent;
    private String firstContentHash;
    private String firstEvidenceHash;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var login = new LoginUser();
        login.setId(7L); login.setTenantId(1L); login.setUserType(2);
        login.setInfo(Map.of("username", "synthetic-operator",
                LoginUser.INFO_KEY_NICKNAME, "测试操作员"));
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
        security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(login);
        records = mock(ElectronicSignatureRecordMapper.class);
        audits = mock(GxpAuditEventMapper.class);
        ledgers = mock(GxpAuditLedgerSequenceMapper.class);
        activations = mock(GxpAuditPolicyActivationMapper.class);
        policies = mock(GxpAuditPolicyOperationMapper.class);
        relations = mock(GxpAuditEventRelationMapper.class);
        when(records.insert(any(ElectronicSignatureRecordDO.class))).thenAnswer(call -> {
            insertedRecord = call.getArgument(0);
            assertEquals(10L, insertedRecord.getId()); // Already final before publication.
            assertEquals(firstRecordImage, image(insertedRecord));
            assertFalse(tree(insertedRecord.getCanonicalContentJson())
                    .get("signatureIdentity").has("displayName"));
            storedRecord = copy(insertedRecord, new ElectronicSignatureRecordDO());
            return 1;
        });
        when(records.selectById(10L)).thenAnswer(call -> {
            selectedRecord = copy(storedRecord, new ElectronicSignatureRecordDO());
            return selectedRecord;
        });
        when(audits.insert(any(GxpAuditEventDO.class))).thenAnswer(call -> {
            insertedAudit = call.getArgument(0);
            insertedAudit.setId(20L); // Ordinary storage-assigned audit ID; never repair hashes.
            storedAudit = copy(insertedAudit, new GxpAuditEventDO());
            return 1;
        });
        when(audits.selectSignatureCreationEvents(1L, "10")).thenAnswer(call -> {
            GxpAuditEventDO first = copy(storedAudit, new GxpAuditEventDO());
            selectedAudits = duplicateAudit
                    ? List.of(first, copy(storedAudit, new GxpAuditEventDO())) : List.of(first);
            return selectedAudits;
        });
        var ledger = new GxpAuditLedgerSequenceDO();
        ledger.setTenantId(1L); ledger.setNextLedgerSequence(1L);
        when(ledgers.selectByTenantIdForUpdate(1L)).thenReturn(ledger);
        when(ledgers.updateById(any(GxpAuditLedgerSequenceDO.class))).thenReturn(1);
        var activation = new GxpAuditPolicyActivationDO();
        activation.setPolicyVersion("audit-p1");
        when(activations.selectLatestForUpdate(1L)).thenReturn(activation);
        var policy = new GxpAuditPolicyOperationDO();
        policy.setActive(true); policy.setApplicability("GXP"); policy.setPolicyVersion("audit-p1");
        policy.setDomain("SIGNATURE"); policy.setSubjectType("SIGNATURE_RECORD");
        policy.setActionType("CREATE"); policy.setReasonPolicy("USER_REQUIRED");
        policy.setSignaturePolicy("REQUIRED");
        when(policies.selectByPolicyVersionForUpdate(1L, "audit-p1", "signature.record.create"))
                .thenReturn(policy);
        auditWriter = new GxpAuditServiceImpl();
        ReflectionTestUtils.setField(auditWriter, "auditEventMapper", audits);
        ReflectionTestUtils.setField(auditWriter, "ledgerSequenceMapper", ledgers);
        ReflectionTestUtils.setField(auditWriter, "policyActivationMapper", activations);
        ReflectionTestUtils.setField(auditWriter, "policyOperationMapper", policies);
        ReflectionTestUtils.setField(auditWriter, "eventRelationMapper", relations);
        var verifier = new SignatureCreationVerificationService();
        ReflectionTestUtils.setField(verifier, "auditEventMapper", audits);
        issuer = new ElectronicSignatureVerifiedFactService(records, verifier);
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
    @ValueSource(strings = {"SYSTEM_USER", "MES_EMPLOYEE_PROFILE"})
    void syntheticFirstPersistedMissingNameIssuesAndParsesBothDomains(String domain) {
        // Given a first-persisted, explicitly synthetic selected identity with no name.
        publishInitialFixture(domain, matchingAuth(domain));
        // When the real issuer verifies it, then the real parser retains missing-name semantics.
        var content = issueAndParse();
        VerifiedSignatureFact fact = issuedToken;
        assertSame(fact, content.signatureFact());
        assertEquals(1, parserCalls);
        assertEquals("V1", content.formatVersion());
        assertEquals("VALID", fact.verification().verificationStatus());
        assertEquals("CONSISTENT", fact.verification().digestStatus());
        assertEquals("CONSISTENT", fact.verification().timeConsistencyStatus());
        assertEquals("CONSISTENT", fact.verification().auditBindingStatus());
        assertEquals(List.of("IDENTITY_SNAPSHOT_MISSING"), fact.verification().reasonCodes());
        assertEquals(20L, fact.auditEventId());
        assertEquals(10L, fact.id());
        assertEquals(SIGNED_AT, fact.signedAt());
        assertEquals("SERVER_CLOCK:" + SIGNED_AT, fact.timeEvidenceId());
        var identity = content.identity();
        assertNotNull(identity);
        assertEquals(domain, identity.domain());
        assertEquals(1L, identity.tenantId());
        assertEquals(9L, identity.signerId());
        assertEquals(7L, identity.operatorId());
        assertNull(identity.displayName());
        assertEquals(matchingAuth(domain), fact.authenticationMethod());
        var rawIdentity = tree(fact.canonicalContentJson()).get("signatureIdentity");
        assertFalse(rawIdentity.has("displayName"));
        assertEquals(4, rawIdentity.size());
        for (String key : List.of("tenantId", "signerId", "operatorId")) {
            assertTrue(rawIdentity.get(key).isIntegralNumber());
        }
        assertBusiness(content.businessContentJson());
        assertEquals(firstCanonicalContent, fact.canonicalContentJson());
        assertEquals(firstContentHash, fact.contentHash());
        assertEquals(firstEvidenceHash, fact.evidenceHash());
        assertEquals(firstContentHash, fact.verification().calculatedContentHash());
        assertEquals(firstEvidenceHash, fact.verification().calculatedEvidenceHash());
        assertEquals(insertedRecord.getSubjectId(), content.signatureFact().subjectId());
        assertEquals(insertedRecord.getSubjectVersion(), fact.subjectVersion());
        assertEquals(sha(fact.subjectId()), fact.subjectVersion());
        assertEquals(firstAfterState, storedAudit.getAfterStateJson());
        assertEquals(firstRecordImage, image(insertedRecord));
        assertEquals(firstRecordImage, image(storedRecord));
        assertEquals(firstRecordImage, image(selectedRecord));
        assertEquals(firstAuditImage, image(storedAudit));
        assertEquals(firstAuditImage, image(selectedAudits.get(0)));
        assertNotSame(insertedRecord, storedRecord);
        assertNotSame(storedRecord, selectedRecord);
        assertNotSame(insertedAudit, storedAudit);
        assertNotSame(storedAudit, selectedAudits.get(0));
        assertEquals(7L, storedAudit.getActorId()); // Operator, not selected signer 9.
        assertEquals("SERVICE_METHOD", storedAudit.getSourceType());
        assertEquals("ElectronicSignatureServiceImpl.sign", storedAudit.getSourceLocator());
        assertEquals(firstAfterState, insertedAudit.getAfterStateJson());
        assertEquals(sha(storedAudit.getCanonicalEventJson()), storedAudit.getEventHash());
        assertEquals(sha("{}" + "\u001f" + firstAfterState), storedAudit.getStatePayloadHash());
        verify(records, times(1)).selectById(10L);
        verify(audits, times(1)).selectSignatureCreationEvents(1L, "10");
        verifyNoInteractions(ledgers, activations, policies, relations);
        // No directory is wired into issuer/parser. This is not a current-name lookup test double.
    }

    @ParameterizedTest
    @ValueSource(strings = {"SYSTEM_USER", "MES_EMPLOYEE_PROFILE"})
    void syntheticSelectedAuthMismatchCannotIssue(String domain) {
        // The wrong auth is part of the FIRST immutable fixture, not a repaired signed fact.
        String wrongAuth = "SYSTEM_USER".equals(domain)
                ? "SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD" : "SESSION_PLUS_SELECTED_USER_PASSWORD";
        publishInitialFixture(domain, wrongAuth);
        assertEquals(firstContentHash, sha(canonicalOracle(storedRecord.getCanonicalContentJson())));
        assertEquals(firstEvidenceHash, sha(evidenceOracle(storedRecord)));
        var diagnostic = issuer.verifyEvidence(10L);
        assertEquals("CONSISTENT", diagnostic.digestStatus());
        assertEquals("CONSISTENT", diagnostic.timeConsistencyStatus());
        assertEquals("MISMATCH", diagnostic.auditBindingStatus());
        assertTrue(diagnostic.reasonCodes().contains("IDENTITY_BINDING_MISMATCH"));
        clearReadInvocations();
        assertThrows(ServiceException.class, this::issueAndParse);
        assertEquals(0, parserCalls);
        assertNull(issuedToken);
        verify(records, times(1)).selectById(10L);
        verify(audits, never()).selectSignatureCreationEvents(anyLong(), anyString());
        assertEquals(firstRecordImage, image(storedRecord));
        assertEquals(firstAuditImage, image(storedAudit));
    }

    @Test
    void damagedSyntheticContentCannotIssue() {
        publishInitialFixture("SYSTEM_USER", matchingAuth("SYSTEM_USER"));
        // Deliberate negative corruption affects ONLY an independent SELECT/storage copy.
        var damaged = copy(storedRecord, new ElectronicSignatureRecordDO());
        damaged.setCanonicalContentJson(damaged.getCanonicalContentJson().replace(REASON, "篡改提交"));
        assertNotEquals(firstCanonicalContent, damaged.getCanonicalContentJson());
        storedRecord = damaged; // Preserve every stored hash and the original protected audit.
        assertEquals(firstContentHash, storedRecord.getContentHash());
        assertEquals(firstEvidenceHash, storedRecord.getEvidenceHash());
        var diagnostic = issuer.verifyEvidence(10L);
        assertEquals("MISMATCH", diagnostic.digestStatus());
        assertTrue(diagnostic.reasonCodes().contains("DIGEST_MISMATCH"));
        assertEquals("CONSISTENT", diagnostic.auditBindingStatus());
        clearReadInvocations();
        assertThrows(ServiceException.class, this::issueAndParse);
        assertEquals(0, parserCalls);
        assertNull(issuedToken);
        verify(records, times(1)).selectById(10L);
        verify(audits, times(1)).selectSignatureCreationEvents(1L, "10");
        assertEquals(firstRecordImage, image(insertedRecord));
        assertEquals(firstAuditImage, image(storedAudit));
        assertEquals(firstAfterState, storedAudit.getAfterStateJson());
    }

    @Test
    void duplicateSyntheticCreationAuditCannotIssue() {
        publishInitialFixture("MES_EMPLOYEE_PROFILE", matchingAuth("MES_EMPLOYEE_PROFILE"));
        duplicateAudit = true; // Contradictory port result, not two manufactured success audits.
        var diagnostic = issuer.verifyEvidence(10L);
        assertEquals("CONSISTENT", diagnostic.digestStatus());
        assertEquals("CONSISTENT", diagnostic.timeConsistencyStatus());
        assertEquals("MISMATCH", diagnostic.auditBindingStatus());
        assertTrue(diagnostic.reasonCodes().contains("AUDIT_LINK_DUPLICATE"));
        clearReadInvocations();
        assertThrows(ServiceException.class, this::issueAndParse);
        assertEquals(0, parserCalls);
        assertNull(issuedToken);
        assertEquals(2, selectedAudits.size());
        assertNotSame(selectedAudits.get(0), selectedAudits.get(1));
        assertNotSame(storedAudit, selectedAudits.get(0));
        assertEquals(firstAuditImage, image(selectedAudits.get(0)));
        assertEquals(firstAuditImage, image(selectedAudits.get(1)));
        verify(records, times(1)).selectById(10L);
        verify(audits, times(1)).selectSignatureCreationEvents(1L, "10");
        assertEquals(firstRecordImage, image(storedRecord));
        assertEquals(firstAuditImage, image(storedAudit));
    }

    private MesSignatureContentProtocol.VerifiedMesSignatureContent issueAndParse() {
        // Assignment and parser entry occur ONLY after successful private server issuance.
        issuedToken = issuer.requireVerifiedFact(10L);
        parserCalls++;
        return MesSignatureContentProtocol.parseVerifiedContent(issuedToken, 1L);
    }

    private void publishInitialFixture(String domain, String authenticationMethod) {
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                0L, ACTION, null, null, null, null, null, null, null,
                MesProductionSubmitSignatureContext.SOURCE_TYPE, 700L, SOURCE_NAME,
                null, null, null, null, null);
        String[] expectedSegments = new String[17];
        Arrays.fill(expectedSegments, "");
        expectedSegments[0] = "0"; expectedSegments[1] = ACTION;
        expectedSegments[9] = "MES_PRODUCTION_SUBMISSION";
        expectedSegments[10] = "700"; expectedSegments[11] = SOURCE_NAME;
        assertEquals(String.join("\n", expectedSegments),
                new String(Base64.getUrlDecoder().decode(subject), StandardCharsets.UTF_8));
        String version = sha(subject);
        var snapshot = adapter.loadAndAuthorize(
                new SignatureSubjectCommand(9L, MODULE, ACTION, TYPE, subject, version, REASON));
        assertEquals(TYPE, snapshot.subjectType());
        assertEquals(subject, snapshot.subjectId());
        assertEquals(version, snapshot.subjectVersion());
        assertBusiness(snapshot.canonicalContentJson());
        var action = adapter.supportedActions().stream()
                .filter(a -> ACTION.equals(a.actionCode())).findFirst().orElseThrow();
        assertEquals("生产提交", action.meaningLabel());
        assertEquals("mes-batch-record-signature-v1", action.policyVersion());
        ObjectNode identity = JsonNodeFactory.instance.objectNode();
        identity.put("domain", domain);
        identity.put("tenantId", 1L); identity.put("signerId", 9L); identity.put("operatorId", 7L);
        ObjectNode initial = (ObjectNode) tree(snapshot.canonicalContentJson());
        initial.set("signatureIdentity", identity); // NO displayName from the beginning.
        var record = ElectronicSignatureRecordDO.builder()
                .id(10L).moduleCode(MODULE).actionCode(ACTION).subjectType(TYPE)
                .subjectId(subject).subjectVersion(version).actorId(9L)
                .meaningCode(ACTION).meaningLabel(action.meaningLabel()).reason(REASON)
                .signedAt(SIGNED_AT).timeEvidenceId("SERVER_CLOCK:" + SIGNED_AT)
                .authenticationMethod(authenticationMethod)
                .canonicalContentJson(canonicalOracle(initial.toString()))
                .beforeContentJson(canonicalOracle(snapshot.beforeContentJson()))
                .afterContentJson(canonicalOracle(snapshot.afterContentJson()))
                .fieldDiffJson(canonicalOracle(snapshot.fieldDiffJson()))
                .algorithm("SHA-256").keyVersion("system-local-v1").policyVersion(action.policyVersion())
                .verificationStatus("VALID").idempotencyKey(MARKER + "|" + domain)
                .processInstanceId(snapshot.processInstanceId()).taskId(snapshot.taskId())
                .nodeCode(snapshot.nodeCode()).nodeOrder(snapshot.nodeOrder()).build();
        record.setTenantId(1L);
        record.setContentHash(sha(record.getCanonicalContentJson()));
        record.setBeforeContentHash(optionalHash(record.getBeforeContentJson()));
        record.setAfterContentHash(optionalHash(record.getAfterContentJson()));
        String evidence = evidenceOracle(record);
        assertEquals(23, evidence.split("\\|", -1).length);
        record.setEvidenceHash(sha(evidence));
        record.setCommandHash(sha(commandPayloadOracle(record) + "|" + canonicalOracle(identity.toString())));
        assertFalse(tree(record.getCanonicalContentJson()).get("signatureIdentity").has("displayName"));
        firstCanonicalContent = record.getCanonicalContentJson();
        firstContentHash = record.getContentHash();
        firstEvidenceHash = record.getEvidenceHash();
        firstAfterState = afterStateOracle(record);
        // Every record value, hash and audit afterState is final BEFORE FIRST INSERT/publication.
        firstRecordImage = image(record);
        var command = GxpAuditCommand.builder()
                .operationId("signature.record.create").subjectId(TYPE + ":" + subject)
                .subjectVersion(version).reason(REASON)
                .beforeState(GxpAuditStateEnvelope.builder().state("NO_SIGNATURE_RECORD")
                        .objectVersion(version).canonicalJson("{}").build())
                .afterState(GxpAuditStateEnvelope.builder().state("ELECTRONIC_SIGNATURE_RECORDED")
                        .objectVersion(version).canonicalJson(firstAfterState).build())
                .idempotencyKey("SIGNATURE:" + sha(record.getIdempotencyKey()))
                // Explicitly authorized synthetic V1 caller-source simulation, not an invocation claim.
                .source("ElectronicSignatureServiceImpl.sign")
                .signatureRecordId("10").signatureContentHash(firstContentHash).build();
        String commandImage = image(command);
        assertEquals(1, records.insert(record));
        var appended = auditWriter.append(command); // Real canonical event/state hashes; no fabricated audit DTO.
        assertEquals(20L, appended.eventId());
        assertEquals(commandImage, image(command)); // append must normalize its own copy.
        assertEquals(firstAfterState, storedAudit.getAfterStateJson());
        assertEquals(firstContentHash, storedAudit.getSignatureContentHash());
        assertEquals(sha(storedAudit.getCanonicalEventJson()), storedAudit.getEventHash());
        assertEquals(sha("{}" + "\u001f" + firstAfterState), storedAudit.getStatePayloadHash());
        firstAuditImage = image(storedAudit);
        assertEquals(firstRecordImage, image(storedRecord));
        verify(records, times(1)).insert(any(ElectronicSignatureRecordDO.class));
        verify(audits, times(1)).insert(any(GxpAuditEventDO.class));
        clearInvocations(records, audits, ledgers, activations, policies, relations);
    }

    private void clearReadInvocations() { clearInvocations(records, audits); }

    private static String matchingAuth(String domain) {
        return switch (domain) {
            case "SYSTEM_USER" -> "SESSION_PLUS_SELECTED_USER_PASSWORD";
            case "MES_EMPLOYEE_PROFILE" -> "SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD";
            default -> throw new AssertionError("Unknown fixture domain: " + domain);
        };
    }

    private static void assertBusiness(String json) {
        JsonNode actual = tree(json);
        assertEquals(expectedBusinessOracle(), actual);
        assertEquals(BUSINESS_KEYS, keys(actual));
        assertEquals(19, actual.size());
        actual.forEach(value -> assertTrue(value.isTextual()));
    }

    private static ObjectNode expectedBusinessOracle() {
        // Fixed V1 business-field oracle; never calls the adapter/parser to generate expected content.
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("adapter", "MES_BATCH_RECORD"); body.put("executionId", "0");
        body.put("actionType", "PRODUCTION_SUBMIT"); body.put("processInstanceId", "");
        body.put("bpmTaskId", ""); body.put("bpmTaskDefinitionKey", ""); body.put("bpmTaskName", "");
        body.put("signatureCellKey", ""); body.put("signatureRowIndex", ""); body.put("signatureColumnIndex", "");
        body.put("reviewSourceType", "MES_PRODUCTION_SUBMISSION"); body.put("reviewSourceId", "700");
        body.put("reviewSourceName", SOURCE_NAME); body.put("approvalResult", ""); body.put("reason", "生产提交");
        body.put("fieldAuditRevision", ""); body.put("fieldAuditHeadHash", "");
        body.put("cellValuesHash", ""); body.put("signatureChallengeHash", "");
        return body;
    }

    private static String evidenceOracle(ElectronicSignatureRecordDO record) {
        // Original 23-position pipe, not the future V2 14/28-key envelopes.
        return String.join("|", "1", "9", "MES", "PRODUCTION_SUBMIT", "MES_BATCH_RECORD",
                record.getSubjectId(), record.getSubjectVersion(), "PRODUCTION_SUBMIT", "生产提交", "生产提交",
                SIGNED_AT.toString(), "SERVER_CLOCK:" + SIGNED_AT, record.getAuthenticationMethod(),
                record.getContentHash(), empty(record.getBeforeContentHash()), empty(record.getAfterContentHash()),
                empty(canonicalOracle(record.getBeforeContentJson())), empty(canonicalOracle(record.getAfterContentJson())),
                empty(canonicalOracle(record.getFieldDiffJson())), "SHA-256", "system-local-v1",
                "mes-batch-record-signature-v1", "VALID");
    }

    private static String commandPayloadOracle(ElectronicSignatureRecordDO record) {
        // Original 11 request positions plus the first selected identity JSON.
        // Issuer does not check request replay; a passed token must not imply that additional claim.
        return String.join("|", "1", "9", "MES", "PRODUCTION_SUBMIT", "MES_BATCH_RECORD",
                record.getSubjectId(), record.getSubjectVersion(), REASON, record.getIdempotencyKey(), "", "");
    }

    private static String afterStateOracle(ElectronicSignatureRecordDO record) {
        // Original explicit 22-field CREATE afterState contract; no core reflection/expected DTO mock.
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("signatureId", 10L); state.put("moduleCode", "MES"); state.put("actionCode", "PRODUCTION_SUBMIT");
        state.put("subjectType", "MES_BATCH_RECORD"); state.put("subjectId", record.getSubjectId());
        state.put("subjectVersion", record.getSubjectVersion()); state.put("actorId", 9L);
        state.put("meaningCode", "PRODUCTION_SUBMIT"); state.put("meaningLabel", "生产提交");
        state.put("reason", REASON); state.put("signedAt", SIGNED_AT);
        state.put("authenticationMethod", record.getAuthenticationMethod());
        state.put("contentHash", record.getContentHash()); state.put("beforeContentHash", record.getBeforeContentHash());
        state.put("afterContentHash", record.getAfterContentHash()); state.put("evidenceHash", record.getEvidenceHash());
        state.put("policyVersion", "mes-batch-record-signature-v1"); state.put("verificationStatus", "VALID");
        state.put("processInstanceId", record.getProcessInstanceId()); state.put("taskId", record.getTaskId());
        state.put("nodeCode", record.getNodeCode()); state.put("nodeOrder", record.getNodeOrder());
        assertEquals(AFTER_STATE_KEYS, state.keySet());
        assertEquals(22, state.size());
        // Preserve the current JsonUtils nullable-map serialization; do not invent explicit-null keys.
        return JsonUtils.toJsonString(state);
    }

    private static Set<String> keys(JsonNode node) {
        Set<String> result = new HashSet<>();
        node.fieldNames().forEachRemaining(result::add);
        return result;
    }

    private static String image(Object value) { return JsonUtils.toJsonString(value); }

    private static <T> T copy(T source, T target) {
        // Both DOs contain immutable scalar values, never shared mutable JSON trees/maps.
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private static JsonNode tree(String json) {
        try { return ORACLE_READER.readTree(json); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new AssertionError(ex); }
    }

    private static String canonicalOracle(String json) {
        if (json == null || json.isBlank()) return null;
        return JsonUtils.toJsonString(sortedOracle(tree(json)));
    }

    private static JsonNode sortedOracle(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            for (String name : names) sorted.set(name, sortedOracle(node.get(name)));
            return sorted;
        }
        if (node.isArray()) {
            var sorted = JsonNodeFactory.instance.arrayNode();
            node.forEach(value -> sorted.add(sortedOracle(value)));
            return sorted;
        }
        return node;
    }

    private static String optionalHash(String json) { return json == null ? null : sha(json); }
    private static String empty(String value) { return value == null ? "" : value; }

    private static String sha(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new AssertionError(ex); }
    }
}
