package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.dto.AuthorizedSignatureIdentity;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureActionDefinition;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService.VerifiedSignatureFact;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import cn.iocoder.yudao.module.system.service.gxpaudit.SignatureCreationVerificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Modifier;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Real MES adapter -> signature writer -> GxP writer -> creation verifier -> token.
 * Only authentication/policy/ledger/storage ports are mocked. INSERT and SELECT use
 * separate scalar DO copies, not a physical database or datetime(3) conversion.
 * Controlled contradictory producers alter snapshots BEFORE signing; no post-sign hash repair.
 */
class MesSignatureContentProtocolTest {
    private static final String MODULE = "MES";
    private static final String TYPE = "MES_BATCH_RECORD";
    private static final ObjectMapper ORACLE_READER = new ObjectMapper();
    private static final String FULL_PAYLOAD =
            "900\nAPPROVE\npi-900\ntask-900\nnode-900\n审批节点\ncell-1\n0\n2\n"
                    + "SOURCE\n700\n正式来源\nAPPROVE\n3\nhead-hash\ncells-hash\nchallenge-hash";
    private static final List<String> KEYS = List.of(
            "adapter", "executionId", "actionType", "processInstanceId", "bpmTaskId",
            "bpmTaskDefinitionKey", "bpmTaskName", "signatureCellKey", "signatureRowIndex",
            "signatureColumnIndex", "reviewSourceType", "reviewSourceId", "reviewSourceName",
            "approvalResult", "reason", "fieldAuditRevision", "fieldAuditHeadHash",
            "cellValuesHash", "signatureChallengeHash");
    private final MesBatchRecordSignatureSubjectAdapter adapter = new MesBatchRecordSignatureSubjectAdapter();
    private ElectronicSignatureServiceImpl writer;
    private ElectronicSignatureVerifiedFactService verifiedFacts;
    private ElectronicSignatureRecordMapper records;
    private GxpAuditEventMapper audits;
    private AdminUserApi users;
    private GxpAuditLedgerSequenceMapper ledgers;
    private GxpAuditPolicyActivationMapper activations;
    private GxpAuditPolicyOperationMapper policies;
    private GxpAuditEventRelationMapper relations;
    private ElectronicSignatureRecordDO insertedRecord;
    private ElectronicSignatureRecordDO storedRecord;
    private ElectronicSignatureRecordDO selectedRecord;
    private GxpAuditEventDO insertedAudit;
    private GxpAuditEventDO storedAudit;
    private GxpAuditEventDO selectedAudit;
    private MockedStatic<SecurityFrameworkUtils> security;

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
        ledgers = mock(GxpAuditLedgerSequenceMapper.class);
        activations = mock(GxpAuditPolicyActivationMapper.class);
        policies = mock(GxpAuditPolicyOperationMapper.class);
        relations = mock(GxpAuditEventRelationMapper.class);
        when(records.insert(any(ElectronicSignatureRecordDO.class))).thenAnswer(call -> {
            insertedRecord = call.getArgument(0);
            insertedRecord.setId(10L);
            storedRecord = copy(insertedRecord, new ElectronicSignatureRecordDO());
            return 1;
        });
        when(records.selectById(10L)).thenAnswer(call -> {
            selectedRecord = storedRecord == null ? null : copy(storedRecord, new ElectronicSignatureRecordDO());
            return selectedRecord;
        });
        when(audits.insert(any(GxpAuditEventDO.class))).thenAnswer(call -> {
            insertedAudit = call.getArgument(0);
            insertedAudit.setId(20L);
            storedAudit = copy(insertedAudit, new GxpAuditEventDO());
            return 1;
        });
        when(audits.selectSignatureCreationEvents(1L, "10")).thenAnswer(call -> {
            if (storedAudit == null) return List.of();
            selectedAudit = copy(storedAudit, new GxpAuditEventDO());
            return List.of(selectedAudit);
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
        policy.setDomain("SIGNATURE"); policy.setSubjectType("SIGNATURE_RECORD"); policy.setActionType("CREATE");
        policy.setReasonPolicy("USER_REQUIRED"); policy.setSignaturePolicy("REQUIRED");
        when(policies.selectByPolicyVersionForUpdate(1L, "audit-p1", "signature.record.create")).thenReturn(policy);
        var auditWriter = new GxpAuditServiceImpl();
        // GxP resolves the authenticated actor from the current tenant directory,
        // independently of OAuth login info. Mock only that storage lookup so the
        // real audit writer and signature creation verification remain in use.
        var auditUsers = mock(AdminUserMapper.class);
        var currentActor = new AdminUserDO();
        currentActor.setId(7L);
        currentActor.setTenantId(1L);
        currentActor.setStatus(0);
        currentActor.setUsername("operator");
        currentActor.setNickname("设备操作员");
        when(auditUsers.selectGxpAuditActor(1L, 7L)).thenReturn(currentActor);
        ReflectionTestUtils.setField(auditWriter, "adminUserMapper", auditUsers);
        ReflectionTestUtils.setField(auditWriter, "auditEventMapper", audits);
        ReflectionTestUtils.setField(auditWriter, "ledgerSequenceMapper", ledgers);
        ReflectionTestUtils.setField(auditWriter, "policyActivationMapper", activations);
        ReflectionTestUtils.setField(auditWriter, "policyOperationMapper", policies);
        ReflectionTestUtils.setField(auditWriter, "eventRelationMapper", relations);
        writer = new ElectronicSignatureServiceImpl();
        ReflectionTestUtils.setField(writer, "signatureRecordMapper", records);
        ReflectionTestUtils.setField(writer, "adminUserApi", users);
        ReflectionTestUtils.setField(writer, "gxpAuditService", auditWriter);
        ReflectionTestUtils.setField(writer, "subjectAdapters", List.of(adapter));
        var verifier = new SignatureCreationVerificationService();
        ReflectionTestUtils.setField(verifier, "auditEventMapper", audits);
        verifiedFacts = new ElectronicSignatureVerifiedFactService(records, verifier);
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

    @Test
    void realProductionZeroAndEmptySlotsKeepOriginalFactsAndIndependentBody() {
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                0L, "PRODUCTION_SUBMIT", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        String[] parts = new String[17];
        Arrays.fill(parts, "");
        parts[0] = "0"; parts[1] = "PRODUCTION_SUBMIT";
        assertEquals(String.join("\n", parts), decodePayload(subject));
        var fact = sign(subject, "PRODUCTION_SUBMIT", "生产提交", "ordinary");
        var content = parse(fact, 1L);
        assertSame(fact, content.signatureFact());
        assertEquals("V1", content.formatVersion());
        assertEquals("0", content.subject().executionId());
        assertEquals("", content.subject().signatureChallengeHash());
        assertNull(content.identity());
        assertNull(content.opinionSource());
        assertBusiness(content.businessContentJson(), expectedBusiness(parts, "PRODUCTION_SUBMIT", "生产提交", null), false);
        assertOriginal(fact);
        // Parser success is not permission to consume unscoped production evidence.
    }

    @ParameterizedTest
    @ValueSource(strings = {"SYSTEM_USER", "MES_EMPLOYEE_PROFILE"})
    void realSelectedIdentityIsNumericTypedAndFrozenWithoutDirectoryLookup(String domain) {
        var fact = sign(realSubject("APPROVE", "审批节点"), "APPROVE", "批准", domain);
        var content = parse(fact, 1L);
        var identity = content.identity();
        assertNotNull(identity);
        assertEquals(domain, identity.domain());
        assertEquals(1L, identity.tenantId());
        assertEquals(9L, identity.signerId());
        assertEquals(7L, identity.operatorId());
        assertEquals("签署时姓名", identity.displayName());
        assertEquals("MES_EMPLOYEE_PROFILE".equals(domain)
                ? "SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD" : "SESSION_PLUS_SELECTED_USER_PASSWORD",
                fact.authenticationMethod());
        var rawIdentity = tree(fact.canonicalContentJson()).get("signatureIdentity");
        for (String key : List.of("tenantId", "signerId", "operatorId")) assertTrue(rawIdentity.get(key).isIntegralNumber());
        assertBusiness(content.businessContentJson(), expectedFull("APPROVE", "审批节点", "批准", null), false);
        assertOriginal(fact);
    }

    @Test
    void historicalSeventeenActionReasonDoesNotInventOpinionSource() {
        var fact = sign(realSubject("APPROVE", "审批节点"), "APPROVE", "APPROVE", "ordinary");
        var content = parse(fact, 1L);
        assertNull(content.opinionSource());
        assertNull(content.userOpinion());
        assertNull(content.subject().opinionSource());
        assertNull(content.identity());
        assertFalse(tree(content.businessContentJson()).has("opinionSource"));
        assertTrue(fact.verification().reasonCodes().contains("IDENTITY_SNAPSHOT_MISSING"));
        assertBusiness(content.businessContentJson(), expectedFull("APPROVE", "审批节点", "APPROVE", null), false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVE", "PQC_RELEASE"})
    void realUserLiteralActionAndSystemActionProtectDifferentFullVersions(String action) {
        String legacy = realSubject(action, "审批节点");
        String user = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(legacy, action);
        String system = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(legacy, null);
        var userFact = sign(user, action, action, "ordinary");
        var userContent = parse(userFact, 1L);
        assertEquals("USER", userContent.opinionSource());
        assertEquals(action, userContent.userOpinion());
        assertBusiness(userContent.businessContentJson(), expectedFull(action, "审批节点", action, "USER"), true);
        assertEquals(sha(user), userFact.subjectVersion());
        String userRaw = userFact.canonicalContentJson();
        String userHash = userFact.contentHash();
        var systemFact = sign(system, action, action, "ordinary");
        var systemContent = parse(systemFact, 1L);
        assertEquals("SYSTEM", systemContent.opinionSource());
        assertNull(systemContent.userOpinion());
        assertBusiness(systemContent.businessContentJson(), expectedFull(action, "审批节点", action, "SYSTEM"), true);
        assertEquals(sha(system), systemFact.subjectVersion());
        assertNotEquals(userFact.subjectVersion(), systemFact.subjectVersion());
        assertNotEquals(userFact.contentHash(), systemFact.contentHash());
        assertEquals(userRaw, userFact.canonicalContentJson());
        assertEquals(userHash, userFact.contentHash());
        assertNotEquals(sha(legacy), userFact.subjectVersion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"legacy", "USER"})
    void rawNonBlankOpinionKeepsWhitespaceWhileV1BusinessUsesOriginalTrim(String source) {
        String subject = realSubject("APPROVE", "审批节点");
        String raw = "  第一行\n第二行  ";
        if ("USER".equals(source)) subject = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(subject, raw);
        var fact = sign(subject, "APPROVE", raw, "ordinary");
        var content = parse(fact, 1L);
        assertEquals(raw, fact.reason());
        assertEquals("第一行\n第二行", tree(content.businessContentJson()).get("reason").textValue());
        assertEquals("USER".equals(source) ? raw : null, content.userOpinion());
        assertBusiness(content.businessContentJson(),
                expectedFull("APPROVE", "审批节点", raw.trim(), "USER".equals(source) ? "USER" : null),
                "USER".equals(source));
        assertOriginal(fact);
    }

    @Test
    void onlyTokenApiExistsAndNullTokenIsExplicitlyRejected() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> MesSignatureContentProtocol.parseVerifiedContent(null, 1L));
        assertEquals("MES_SIGNATURE_VERIFIED_FACT_REQUIRED", error.getMessage());
        var methods = Arrays.stream(MesSignatureContentProtocol.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())
                        && method.getName().equals("parseVerifiedContent")).toList();
        assertEquals(1, methods.size());
        assertArrayEquals(new Class<?>[]{VerifiedSignatureFact.class, Long.class}, methods.get(0).getParameterTypes());
        verifyNoInteractions(records, audits, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "zero", "otherTenant"})
    void expectedTenantIsExplicitAndCannotBeDefaultedFromContext(String input) {
        var fact = sign(realSubject("APPROVE", "审批节点"), "APPROVE", "批准", "ordinary");
        Long expected = switch (input) { case "null" -> null; case "zero" -> 0L; default -> 2L; };
        clearPorts();
        assertParserFailure("MES_SIGNATURE_TENANT_CONFLICT", fact, expected);
        verifyNoInteractions(records, audits, users, ledgers, activations, policies, relations);
    }

    @Test
    void genuineLegacyAdapterActionOverrideCannotPassSubjectBinding() {
        // Existing V1 adapter produces command action in the body even when frozen subject differs.
        var fact = sign(realSubject("APPROVE", "审批节点"), "REJECT", "驳回", "ordinary");
        assertEquals("REJECT", tree(fact.canonicalContentJson()).get("actionType").textValue());
        assertEquals("APPROVE", decodePayload(fact.subjectId()).split("\n", -1)[1]);
        assertParserFailure("MES_SIGNATURE_ACTION_CONFLICT", fact, 1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrongModule", "wrongType", "unregisteredAction", "wrongVersion"})
    void controlledProducerFactsStillNeedMesScopeRegistrationAndFullVersion(String mutation) {
        String module = "wrongModule".equals(mutation) ? "OTHER" : MODULE;
        String type = "wrongType".equals(mutation) ? "OTHER_TYPE" : TYPE;
        String action = "unregisteredAction".equals(mutation) ? "UNREGISTERED" : "APPROVE";
        String subject = realSubject(action, "审批节点");
        String version = "wrongVersion".equals(mutation) ? "controlled-version" : sha(subject);
        installControlledProducer(module, type, action, snapshot -> replaceSnapshot(snapshot,
                type, version, snapshot.canonicalContentJson()));
        var fact = signCommand(subject, module, type, action, version, "批准", "ordinary");
        assertEquals(subject, fact.subjectId());
        assertEquals(version, fact.subjectVersion());
        String code = switch (mutation) {
            case "wrongModule", "wrongType" -> "MES_SIGNATURE_FORMAT_UNSUPPORTED";
            case "unregisteredAction" -> "MES_SIGNATURE_ACTION_CONFLICT";
            default -> "MES_SIGNATURE_VERSION_CONFLICT";
        };
        assertParserFailure(code, fact, 1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"extra", "missing", "number", "null", "source"})
    void controlledBusinessContradictionsAreSignedBeforeWholeObjectRejection(String mutation) {
        String subject = realSubject("APPROVE", "审批节点");
        if ("source".equals(mutation)) subject = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(subject, "批准");
        installControlledProducer(MODULE, TYPE, "APPROVE", snapshot -> {
            ObjectNode body = (ObjectNode) tree(snapshot.canonicalContentJson());
            switch (mutation) {
                case "extra" -> body.put("unexpectedBusinessKey", "kept");
                case "missing" -> body.remove("reason");
                case "number" -> body.put("reason", 7);
                case "null" -> body.putNull("reason");
                case "source" -> body.put("opinionSource", "SYSTEM");
                default -> throw new AssertionError(mutation);
            }
            return replaceSnapshot(snapshot, TYPE, snapshot.subjectVersion(), body.toString());
        });
        var fact = sign(subject, "APPROVE", "批准", "ordinary");
        assertParserFailure("MES_SIGNATURE_BUSINESS_CONFLICT", fact, 1L);
        assertOriginal(fact);
    }

    @Test
    void controlledExplicitNullIdentityRemainsMissingWithoutFillingCurrentName() {
        installControlledProducer(MODULE, TYPE, "APPROVE", snapshot -> {
            ObjectNode body = (ObjectNode) tree(snapshot.canonicalContentJson());
            body.putNull("signatureIdentity");
            return replaceSnapshot(snapshot, TYPE, snapshot.subjectVersion(), body.toString());
        });
        var fact = sign(realSubject("APPROVE", "审批节点"), "APPROVE", "批准", "ordinary");
        assertTrue(tree(fact.canonicalContentJson()).get("signatureIdentity").isNull());
        var content = parse(fact, 1L);
        assertNull(content.identity());
        assertTrue(fact.verification().reasonCodes().contains("IDENTITY_SNAPSHOT_MISSING"));
        assertFalse(tree(content.businessContentJson()).has("signatureIdentity"));
        assertBusiness(content.businessContentJson(), expectedFull("APPROVE", "审批节点", "批准", null), false);
        assertOriginal(fact);
    }

    @ParameterizedTest
    @ValueSource(strings = {"textual", "outOfRange"})
    void controlledInvalidIdentityHasNoTokenAndNeverBypassesIssuer(String input) {
        // sign() ordinary preserves this controlled selected-domain snapshot; issuer rejects it.
        installControlledProducer(MODULE, TYPE, "APPROVE", snapshot -> {
            ObjectNode body = (ObjectNode) tree(snapshot.canonicalContentJson());
            var identity = body.putObject("signatureIdentity");
            identity.put("domain", "SYSTEM_USER");
            identity.put("tenantId", 1);
            identity.put("operatorId", 7);
            identity.put("displayName", "签署时姓名");
            if ("textual".equals(input)) identity.put("signerId", "7");
            else identity.put("signerId", new java.math.BigInteger("9223372036854775808"));
            return replaceSnapshot(snapshot, TYPE, snapshot.subjectVersion(), body.toString());
        });
        write(realSubject("APPROVE", "审批节点"), MODULE, TYPE, "APPROVE", null, "批准", "ordinary");
        clearPorts();
        assertThrows(ServiceException.class, () -> verifiedFacts.requireVerifiedFact(10L));
        verify(records, times(1)).selectById(10L);
        verify(audits, never()).selectSignatureCreationEvents(anyLong(), anyString());
        // No token exists: there is intentionally no parser call or manufactured VALID marker.
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\r\n"})
    void blankCommandReasonWritesNeitherSignatureNorAudit(String reason) {
        String subject = realSubject("APPROVE", "审批节点");
        var command = command(subject, MODULE, TYPE, "APPROVE", sha(subject), reason);
        assertThrows(ServiceException.class, () -> writer.sign(command));
        verify(records, never()).insert(any(ElectronicSignatureRecordDO.class));
        verify(audits, never()).insert(any(GxpAuditEventDO.class));
        assertNull(storedRecord);
        assertNull(storedAudit);
    }

    @ParameterizedTest
    @ValueSource(strings = {"digest", "time", "audit"})
    void damagedPersistedFactsNeverProduceParserInput(String damage) {
        write(realSubject("APPROVE", "审批节点"), MODULE, TYPE, "APPROVE", null, "批准", "ordinary");
        switch (damage) {
            case "digest" -> storedRecord.setCanonicalContentJson(storedRecord.getCanonicalContentJson().replace("批准", "篡改"));
            case "time" -> storedRecord.setSignedAt(storedRecord.getSignedAt().plusSeconds(1));
            case "audit" -> storedAudit = null;
            default -> throw new AssertionError(damage);
        }
        clearPorts();
        assertThrows(ServiceException.class, () -> verifiedFacts.requireVerifiedFact(10L));
        verify(records, times(1)).selectById(10L);
        // Deliberate corruption, never rehashed; no token and no parser invocation.
    }

    @Test
    void realEncoderLiteralReplacementCharacterIsValidUtf8AndMayBeParsed() {
        String subject = realSubject("APPROVE", "合法\uFFFD节点");
        assertEquals("合法\uFFFD节点", decodePayload(subject).split("\n", -1)[5]);
        var fact = sign(subject, "APPROVE", "批准", "ordinary");
        var content = parse(fact, 1L);
        assertEquals("合法\uFFFD节点", content.subject().bpmTaskName());
        assertBusiness(content.businessContentJson(), expectedFull("APPROVE", "合法\uFFFD节点", "批准", null), false);
        assertEquals(sha(subject), fact.subjectVersion());
        assertOriginal(fact);
    }

    @Test
    void controlledIllegalUtf8IsRejectedWithCauseEvenIfOldAdapterCanSignReplacementText() {
        // Controlled byte fixture; production getBytes(UTF_8) cannot generate this malformed byte.
        byte[] bytes = FULL_PAYLOAD.replace("审批节点", "X").getBytes(StandardCharsets.UTF_8);
        int offset = FULL_PAYLOAD.substring(0, FULL_PAYLOAD.indexOf("审批节点")).getBytes(StandardCharsets.UTF_8).length;
        bytes[offset] = (byte) 0xff;
        String subject = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        assertEquals("\uFFFD", adapter.decodeFrozenSubject(subject).bpmTaskName());
        var fact = sign(subject, "APPROVE", "批准", "ordinary");
        assertEquals("\uFFFD", tree(fact.canonicalContentJson()).get("bpmTaskName").textValue());
        clearPorts();
        var error = assertThrows(IllegalArgumentException.class, () -> parse(fact, 1L));
        assertEquals("MES_SIGNATURE_SUBJECT_INVALID", error.getMessage());
        assertInstanceOf(CharacterCodingException.class, error.getCause());
        verifyNoInteractions(records, audits, users, ledgers, activations, policies, relations);
        assertOriginal(fact);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unpadded", "padded", "tailAlias"})
    void existingBase64AcceptedFormsKeepEachOriginalVersionWithoutReencoding(String form) {
        // Fixed ASCII 17-part payload with a one-byte final quantum: three '=' forms are not invented.
        String payload = FULL_PAYLOAD.replace("审批节点", "node").replace("正式来源", "source");
        while (payload.getBytes(StandardCharsets.UTF_8).length % 3 != 1) payload += "x";
        String canonical = rawSubject(payload);
        String subject = switch (form) {
            case "padded" -> Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            case "tailAlias" -> nonZeroTailAlias(canonical);
            default -> canonical;
        };
        assertArrayEquals(payload.getBytes(StandardCharsets.UTF_8), Base64.getUrlDecoder().decode(subject));
        assertEquals(payload, decodePayload(subject));
        var fact = sign(subject, "APPROVE", "批准", "ordinary");
        var content = parse(fact, 1L);
        assertEquals(subject, content.signatureFact().subjectId());
        assertEquals(sha(subject), fact.subjectVersion());
        assertEquals(payload.split("\n", -1)[16], content.subject().signatureChallengeHash());
        if (!"unpadded".equals(form)) {
            assertNotEquals(canonical, subject);
            assertNotEquals(sha(canonical), fact.subjectVersion());
        }
        assertOriginal(fact);
    }

    @ParameterizedTest
    @ValueSource(strings = {"padding", "alphabet", "length", "emptySource"})
    void oldDecoderRejectedFormsStillFailBeforeAnySignatureInsert(String form) {
        String subject = switch (form) {
            case "padding" -> "AA=";
            case "alphabet" -> "%%%";
            case "length" -> "A";
            default -> rawSubject(FULL_PAYLOAD + "\n");
        };
        assertThrows(IllegalArgumentException.class, () -> adapter.decodeFrozenSubject(subject));
        assertThrows(IllegalArgumentException.class,
                () -> writer.sign(command(subject, MODULE, TYPE, "APPROVE", sha(subject), "批准")));
        verify(records, never()).insert(any(ElectronicSignatureRecordDO.class));
        verify(audits, never()).insert(any(GxpAuditEventDO.class));
        // No token can be obtained; do not use DTO or reflection to exercise an impossible input.
    }

    @Test
    void parserUsesOnlyFrozenTokenWithExplicitTenantAfterAllContextsAndPortsChange() {
        var fact = sign(realSubject("APPROVE", "审批节点"), "APPROVE", "批准", "SYSTEM_USER");
        String raw = fact.canonicalContentJson();
        String subject = fact.subjectId();
        String hash = fact.contentHash();
        var verifiedAt = fact.verification().verifiedAt();
        assertNotSame(insertedRecord, storedRecord);
        assertNotSame(storedRecord, selectedRecord);
        assertNotSame(insertedAudit, storedAudit);
        assertNotSame(storedAudit, selectedAudit);
        insertedRecord.setReason("后写入修改");
        storedRecord.setCanonicalContentJson("{}");
        selectedRecord.setSubjectId("changed");
        storedAudit.setEventHash("changed");
        selectedAudit.setTransactionId("changed");
        TenantContextHolder.clear();
        security.close();
        security = null;
        clearPorts();
        var content = parse(fact, 1L);
        assertSame(fact, content.signatureFact());
        assertEquals(raw, fact.canonicalContentJson());
        assertEquals(subject, content.signatureFact().subjectId());
        assertEquals(hash, content.signatureFact().contentHash());
        assertEquals(verifiedAt, content.signatureFact().verification().verifiedAt());
        assertEquals("签署时姓名", content.identity().displayName());
        assertEquals("批准", tree(content.businessContentJson()).get("reason").textValue());
        assertEquals(raw, content.signatureFact().canonicalContentJson());
        verifyNoInteractions(records, audits, users, ledgers, activations, policies, relations);
    }

    @Test
    void resultAndTypedIdentityHavePrivateConstructionAndOnlyImmutableState() throws NoSuchMethodException {
        var fact = sign(realSubject("APPROVE", "审批节点"), "APPROVE", "批准", "SYSTEM_USER");
        var content = parse(fact, 1L);
        assertImmutableProjection(content.getClass());
        assertImmutableProjection(content.identity().getClass());
        assertTrue(content.subject().getClass().isRecord());
        assertEquals(String.class, content.getClass().getMethod("businessContentJson").getReturnType());
        assertSame(fact, content.signatureFact());
        assertThrows(UnsupportedOperationException.class, () -> fact.verification().reasonCodes().add("invented"));
        String raw = fact.canonicalContentJson();
        ObjectNode local = (ObjectNode) tree(content.businessContentJson());
        local.put("reason", "local-copy");
        assertEquals("批准", tree(content.businessContentJson()).get("reason").textValue());
        assertEquals(raw, fact.canonicalContentJson());
    }

    private static void assertImmutableProjection(Class<?> type) {
        assertTrue(Modifier.isFinal(type.getModifiers()));
        for (var constructor : type.getDeclaredConstructors()) assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        for (var field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            assertTrue(Modifier.isPrivate(field.getModifiers()));
            assertTrue(Modifier.isFinal(field.getModifiers()));
            assertFalse(Map.class.isAssignableFrom(field.getType()));
            assertFalse(JsonNode.class.isAssignableFrom(field.getType()));
            assertFalse(ElectronicSignatureRecordDO.class.isAssignableFrom(field.getType()));
            assertFalse(GxpAuditEventDO.class.isAssignableFrom(field.getType()));
        }
        for (var method : type.getDeclaredMethods()) {
            assertFalse(method.getName().startsWith("set"));
            if (Modifier.isPublic(method.getModifiers())) {
                assertFalse(Map.class.isAssignableFrom(method.getReturnType()));
                assertFalse(JsonNode.class.isAssignableFrom(method.getReturnType()));
                assertFalse(method.getReturnType() == type && Modifier.isStatic(method.getModifiers()));
            }
        }
    }

    private static MesSignatureContentProtocol.VerifiedMesSignatureContent parse(VerifiedSignatureFact fact, Long tenant) {
        return MesSignatureContentProtocol.parseVerifiedContent(fact, tenant);
    }

    private void assertParserFailure(String code, VerifiedSignatureFact fact, Long tenant) {
        var error = assertThrows(IllegalArgumentException.class, () -> parse(fact, tenant));
        assertEquals(code, error.getMessage());
    }

    private VerifiedSignatureFact sign(String subject, String action, String reason, String domain) {
        return signCommand(subject, MODULE, TYPE, action, sha(subject), reason, domain);
    }

    private VerifiedSignatureFact signCommand(String subject, String module, String type, String action,
                                               String version, String reason, String domain) {
        write(subject, module, type, action, version, reason, domain);
        clearPorts();
        var token = verifiedFacts.requireVerifiedFact(10L);
        assertEquals("VALID", token.verification().verificationStatus());
        assertEquals("CONSISTENT", token.verification().digestStatus());
        assertEquals("CONSISTENT", token.verification().timeConsistencyStatus());
        assertEquals("CONSISTENT", token.verification().auditBindingStatus());
        assertEquals("V1", token.verification().evidenceFormatVersion());
        assertEquals(20L, token.auditEventId());
        assertEquals(sha(canonicalOracle(storedRecord.getCanonicalContentJson())), token.contentHash());
        assertEquals(sha(evidenceOracle(storedRecord)), token.evidenceHash());
        verify(records, times(1)).selectById(10L);
        verify(audits, times(1)).selectSignatureCreationEvents(1L, "10");
        clearPorts();
        return token;
    }

    private void write(String subject, String module, String type, String action,
                       String version, String reason, String domain) {
        var command = command(subject, module, type, action, version == null ? sha(subject) : version, reason);
        if ("ordinary".equals(domain)) writer.sign(command);
        else writer.signAuthorized(command, new AuthorizedSignatureIdentity(domain, 9L, "签署时姓名"),
                "MES_EMPLOYEE_PROFILE".equals(domain) ? mock(Runnable.class) : null);
    }

    private static ElectronicSignatureCommand command(String subject, String module, String type, String action,
                                                       String version, String reason) {
        return new ElectronicSignatureCommand(module, action, type, subject, version,
                "unit-only-auth-port-input", reason, UUID.randomUUID().toString(), null, null);
    }

    private void installControlledProducer(String module, String type, String action,
                                           UnaryOperator<SignatureSubjectSnapshot> change) {
        // Explicit synthetic producer boundary; real MES snapshot always supplies the starting content.
        ElectronicSignatureSubjectAdapter controlled = new ElectronicSignatureSubjectAdapter() {
            @Override public String moduleCode() { return module; }
            @Override public Set<SignatureActionDefinition> supportedActions() {
                return Set.of(new SignatureActionDefinition(module, action, type, action, "受控producer", "controlled-v1"));
            }
            @Override public SignatureSubjectSnapshot loadAndAuthorize(SignatureSubjectCommand command) {
                return change.apply(adapter.loadAndAuthorize(command));
            }
        };
        ReflectionTestUtils.setField(writer, "subjectAdapters", List.of(controlled));
    }

    private static SignatureSubjectSnapshot replaceSnapshot(SignatureSubjectSnapshot s, String type,
                                                            String version, String body) {
        return new SignatureSubjectSnapshot(type, s.subjectId(), version, body,
                s.beforeContentJson(), s.afterContentJson(), s.fieldDiffJson(),
                s.processInstanceId(), s.taskId(), s.nodeCode(), s.nodeOrder());
    }

    private void clearPorts() { clearInvocations(records, audits, users, ledgers, activations, policies, relations); }

    private void assertOriginal(VerifiedSignatureFact fact) {
        assertEquals(insertedRecord.getSubjectId(), fact.subjectId());
        assertEquals(insertedRecord.getSubjectVersion(), fact.subjectVersion());
        assertEquals(insertedRecord.getReason(), fact.reason());
        assertEquals(insertedRecord.getCanonicalContentJson(), fact.canonicalContentJson());
        assertEquals(insertedRecord.getContentHash(), fact.contentHash());
        assertEquals(insertedRecord.getEvidenceHash(), fact.evidenceHash());
    }

    private static <T> T copy(T source, T target) {
        // These DOs carry only immutable scalar values; no mutable JSON trees/maps are shared.
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private static String realSubject(String action, String name) {
        return MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                900L, action, "pi-900", "task-900", "node-900", name, "cell-1", 0, 2,
                "SOURCE", 700L, "正式来源", "APPROVE", 3L, "head-hash", "cells-hash", "challenge-hash");
    }

    private static String rawSubject(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodePayload(String subject) {
        return new String(Base64.getUrlDecoder().decode(subject), StandardCharsets.UTF_8);
    }

    private static String nonZeroTailAlias(String canonical) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        int last = alphabet.indexOf(canonical.charAt(canonical.length() - 1));
        assertEquals(0, last & 15); // Fixed one-byte final quantum; unused four bits are zero in encoder output.
        return canonical.substring(0, canonical.length() - 1) + alphabet.charAt(last + 1);
    }

    private static ObjectNode expectedFull(String action, String name, String reason, String source) {
        String[] parts = FULL_PAYLOAD.split("\n", -1);
        parts[1] = action; parts[5] = name;
        return expectedBusiness(parts, action, reason, source);
    }

    private static ObjectNode expectedBusiness(String[] p, String action, String reason, String source) {
        // Fixed independent field oracle; does not call the adapter's canonicalBusinessContent.
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        expected.put("adapter", "MES_BATCH_RECORD");
        expected.put("executionId", p[0]); expected.put("actionType", action);
        expected.put("processInstanceId", p[2]); expected.put("bpmTaskId", p[3]);
        expected.put("bpmTaskDefinitionKey", p[4]); expected.put("bpmTaskName", p[5]);
        expected.put("signatureCellKey", p[6]); expected.put("signatureRowIndex", p[7]);
        expected.put("signatureColumnIndex", p[8]); expected.put("reviewSourceType", p[9]);
        expected.put("reviewSourceId", p[10]); expected.put("reviewSourceName", p[11]);
        expected.put("approvalResult", p[12]); expected.put("reason", reason);
        expected.put("fieldAuditRevision", p[13]); expected.put("fieldAuditHeadHash", p[14]);
        expected.put("cellValuesHash", p[15]); expected.put("signatureChallengeHash", p[16]);
        if (source != null) expected.put("opinionSource", source);
        return expected;
    }

    private static void assertBusiness(String json, ObjectNode expected, boolean source) {
        JsonNode actual = tree(json);
        assertEquals(expected, actual);
        Set<String> keys = new HashSet<>(KEYS);
        if (source) keys.add("opinionSource");
        Set<String> actualKeys = new HashSet<>();
        actual.fieldNames().forEachRemaining(actualKeys::add);
        assertEquals(keys, actualKeys);
        assertEquals(source ? 20 : 19, actualKeys.size());
        actual.forEach(value -> assertTrue(value.isTextual()));
    }

    private static JsonNode tree(String json) {
        try { return ORACLE_READER.readTree(json); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new AssertionError(ex); }
    }

    private static String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new AssertionError(ex); }
    }

    private static String canonicalOracle(String json) {
        if (json == null || json.isBlank()) return null;
        return JsonUtils.toJsonString(sortedOracle(tree(json)));
    }

    private static JsonNode sortedOracle(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            List<String> keys = new ArrayList<>();
            node.fieldNames().forEachRemaining(keys::add);
            Collections.sort(keys);
            for (String key : keys) result.set(key, sortedOracle(node.get(key)));
            return result;
        }
        if (node.isArray()) {
            var result = JsonNodeFactory.instance.arrayNode();
            node.forEach(value -> result.add(sortedOracle(value)));
            return result;
        }
        return node;
    }

    private static String evidenceOracle(ElectronicSignatureRecordDO r) {
        // Independent original 23-position V1 pipe protocol, not a V2 14/28-key envelope.
        String time = r.getTimeEvidenceId().substring("SERVER_CLOCK:".length());
        return String.join("|", String.valueOf(r.getTenantId()), String.valueOf(r.getActorId()),
                r.getModuleCode(), r.getActionCode(), r.getSubjectType(), r.getSubjectId(), r.getSubjectVersion(),
                r.getMeaningCode(), r.getMeaningLabel(), r.getReason(), time, r.getTimeEvidenceId(),
                r.getAuthenticationMethod(), r.getContentHash(), empty(r.getBeforeContentHash()),
                empty(r.getAfterContentHash()), empty(canonicalOracle(r.getBeforeContentJson())),
                empty(canonicalOracle(r.getAfterContentJson())), empty(canonicalOracle(r.getFieldDiffJson())),
                "SHA-256", r.getKeyVersion(), r.getPolicyVersion(), r.getVerificationStatus());
    }

    private static String empty(String value) { return value == null ? "" : value; }
}
