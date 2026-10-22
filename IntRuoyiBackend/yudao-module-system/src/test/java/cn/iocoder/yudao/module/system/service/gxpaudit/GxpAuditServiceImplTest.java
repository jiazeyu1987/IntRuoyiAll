package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditLedgerSequenceDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import(GxpAuditServiceImpl.class)
@Transactional
class GxpAuditServiceImplTest extends BaseDbUnitTest {

    @Resource
    private GxpAuditService gxpAuditService;
    @Resource
    private GxpAuditEventMapper auditEventMapper;
    @Resource
    private GxpAuditLedgerSequenceMapper ledgerSequenceMapper;
    @Resource
    private GxpAuditPolicyActivationMapper policyActivationMapper;
    @Resource
    private GxpAuditPolicyOperationMapper policyOperationMapper;
    @Resource
    private cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper eventRelationMapper;

    @Test
    void writerProxyRequiresOuterTransactionForAppendAndLock() {
        // End (rollback) only this isolated H2 fixture transaction before invoking the real proxy.
        TestTransaction.flagForRollback();
        TestTransaction.end();
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                () -> gxpAuditService.append(command("edhr.execution.field.update", "no-transaction")));
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                () -> gxpAuditService.acquireLedgerLock());
        assertEquals(0L, auditEventMapper.selectCount());
        assertEquals(0L, eventRelationMapper.selectCount());
    }

    @Test
    void relationFailureMarksOuterTransactionRollbackOnlyAndRollsBackEventAndWatermark() {
        Object target = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(gxpAuditService);
        var failingRelations = org.mockito.Mockito.mock(
                cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("test-relation-write-failure"))
                .when(failingRelations).insert(org.mockito.ArgumentMatchers.any(
                        cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO.class));
        ReflectionTestUtils.setField(target, "eventRelationMapper", failingRelations);
        try {
            assertThrows(IllegalStateException.class,
                    () -> gxpAuditService.append(commandWithEvidence("relation-failure")));
            // Request COMMIT, not a finally rollback: the MANDATORY participant must force rollback.
            TestTransaction.flagForCommit();
            assertThrows(org.springframework.transaction.UnexpectedRollbackException.class, TestTransaction::end);
            assertEquals(0L, auditEventMapper.selectCount());
            assertEquals(0L, eventRelationMapper.selectCount());
            assertNull(ledgerSequenceMapper.selectById(1L));
        } finally {
            ReflectionTestUtils.setField(target, "eventRelationMapper", eventRelationMapper);
        }
    }

    @Test
    void missingPolicyDoesNotAdvanceWatermarkOrWriteRelations() {
        assertServiceException(() -> gxpAuditService.append(command("missing.operation", "missing-policy")),
                GXP_AUDIT_POLICY_NOT_FOUND, "missing.operation");
        assertEquals(1L, ledgerSequenceMapper.selectById(1L).getNextLedgerSequence());
        assertEquals(0L, auditEventMapper.selectCount());
        assertEquals(0L, eventRelationMapper.selectCount());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void append_shouldReplayAcrossCommittedTransactions(boolean reuseSameInstance) {
        GxpAuditCommand input = commandWithEvidence("committed-replay");
        String originalInput = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(input);
        GxpAuditAppendResult first = gxpAuditService.append(input);
        String canonical = auditEventMapper.selectById(first.eventId()).getCanonicalEventJson();
        String firstTransactionId = GxpAuditTransactionContext.requireTransactionId();
        // Commit only the isolated H2 test fixture and first append; no external database is used.
        TestTransaction.flagForCommit();
        TestTransaction.end();
        try {
            TestTransaction.start();
            assertNotEquals(firstTransactionId, GxpAuditTransactionContext.requireTransactionId());
            GxpAuditCommand replayInput = reuseSameInstance ? input : commandWithEvidence("committed-replay");
            assertReplayUnchanged(first, gxpAuditService.append(replayInput), 1L);
            assertLedgerUnchanged(first, canonical, 1L);
            assertEquals(originalInput, cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(input));
            assertEquals("{\"actorId\":\"employee-9102\"}",
                    auditEventMapper.selectById(first.eventId()).getPerformedByJson());
        } finally {
            // End the second transaction so inherited AFTER_TEST_METHOD clean.sql commits its cleanup.
            // clearSecurity() separately removes the test ledger watermark omitted by clean.sql.
            if (TestTransaction.isActive()) {
                TestTransaction.flagForRollback();
                TestTransaction.end();
            }
        }
    }

    @Test
    void append_shouldReplaySameInstanceWithoutMutatingInput() {
        GxpAuditCommand input = command("edhr.execution.field.update", "same-instance");
        String before = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(input);
        GxpAuditAppendResult first = gxpAuditService.append(input);
        GxpAuditAppendResult replay = gxpAuditService.append(input);
        assertReplayUnchanged(first, replay, 0L);
        assertEquals(before, cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(input));
    }

    @Test
    void append_shouldReplayEquivalentEmployeeAndEvidenceWithoutExtraRelations() {
        GxpAuditCommand firstInput = commandWithEvidence("equivalent-evidence");
        GxpAuditAppendResult first = gxpAuditService.append(firstInput);
        GxpAuditCommand replayInput = commandWithEvidence("equivalent-evidence");
        replayInput.setTransactionId("another-transaction-correlation");
        assertReplayUnchanged(first, gxpAuditService.append(replayInput), 1L);
        assertEquals(firstInput.getPerformedBy(), auditEventMapper.selectById(first.eventId()).getPerformedByJson());
    }

    @ParameterizedTest
    @ValueSource(strings = {"resultStatus", "errorCode", "attemptedOperationId", "reasonCode", "reasonSource", "sourceType", "sourceLocator", "performedBy", "links", "evidences"})
    void append_shouldRejectChangedAuditFactWithoutChangingLedger(String field) {
        GxpAuditAppendResult first = gxpAuditService.append(commandWithEvidence("fact-conflict"));
        GxpAuditEventDO original = auditEventMapper.selectById(first.eventId());
        GxpAuditCommand changed = commandWithEvidence("fact-conflict");
        switch (field) {
            case "resultStatus" -> changed.setResultStatus("DENIED");
            case "errorCode" -> changed.setErrorCode("OTHER_ERROR");
            case "attemptedOperationId" -> changed.setAttemptedOperationId("other.operation");
            case "reasonCode" -> changed.setReasonCode("OTHER_REASON");
            case "reasonSource" -> changed.setReasonSource("USER");
            case "sourceType" -> changed.setSourceType("CONTROLLER_METHOD");
            case "sourceLocator" -> changed.setSourceLocator("other#method");
            case "performedBy" -> changed.setPerformedBy("{\"actorId\":\"other-employee\"}");
            case "links" -> changed.setLinks(List.of(new GxpAuditRelation("SOURCE", "ORDER", "other", "1", "hash")));
            case "evidences" -> changed.setEvidences(List.of(new GxpAuditEvidence("DOCUMENT", "other", "1", "hash", "SOURCE")));
            default -> throw new AssertionError(field);
        }
        assertServiceException(() -> gxpAuditService.append(changed), GXP_AUDIT_IDEMPOTENCY_CONFLICT,
                "edhr.execution.field.update");
        assertLedgerUnchanged(first, original.getCanonicalEventJson(), 1L);
    }

    @Test
    void append_shouldValidateInvalidStatusBeforeReplay() {
        GxpAuditAppendResult first = gxpAuditService.append(commandWithEvidence("invalid-replay"));
        String original = auditEventMapper.selectById(first.eventId()).getCanonicalEventJson();
        GxpAuditCommand invalid = commandWithEvidence("invalid-replay");
        invalid.setResultStatus("INVALID");
        assertServiceException(() -> gxpAuditService.append(invalid), GXP_AUDIT_V2_CONTRACT_INVALID, "resultStatus");
        assertLedgerUnchanged(first, original, 1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"resultStatus", "errorCode", "attemptedOperationId"})
    void attemptService_shouldBindFailureFactsThroughRealAppend(String field) {
        insertPolicy("gxp.attempt.record", "GXP", "REQUEST_ATTEMPT", "CREATE", "USER_REQUIRED", "NONE");
        GxpAuditAttemptServiceImpl attempts = new GxpAuditAttemptServiceImpl();
        // Real writer and isolated persistence; enclosing test supplies the transaction.
        ReflectionTestUtils.setField(attempts, "gxpAuditService", gxpAuditService);
        GxpAuditAttemptCommand input = attempt("FAILED", "ERROR_A", "order.update");
        GxpAuditAppendResult first = attempts.record(input);
        assertReplayUnchanged(first, attempts.record(input), 0L);
        String original = auditEventMapper.selectById(first.eventId()).getCanonicalEventJson();
        GxpAuditAttemptCommand changed = attempt(field.equals("resultStatus") ? "DENIED" : "FAILED",
                field.equals("errorCode") ? "ERROR_B" : "ERROR_A",
                field.equals("attemptedOperationId") ? "order.delete" : "order.update");
        assertServiceException(() -> attempts.record(changed), GXP_AUDIT_IDEMPOTENCY_CONFLICT, "gxp.attempt.record");
        assertLedgerUnchanged(first, original, 0L);
    }

    private GxpAuditAttemptCommand attempt(String result, String error, String operation) {
        return new GxpAuditAttemptCommand(operation, "stable-server-attempt", result, error,
                "unchanged reason", "order-1", "1", "request-1", "SERVICE_METHOD", "order#update");
    }

    private GxpAuditCommand commandWithEvidence(String key) {
        GxpAuditCommand input = command("edhr.execution.field.update", key);
        input.setResultStatus("FAILED");
        input.setErrorCode("ERROR_A");
        input.setAttemptedOperationId("order.update");
        input.setReasonCode("SYSTEM_ERROR");
        input.setReasonSource("SYSTEM");
        input.setSourceType("SERVICE_METHOD");
        input.setSourceLocator("order#update");
        input.setPerformedBy("{\"actorId\":\"employee-9102\"}");
        input.setLinks(List.of(new GxpAuditRelation("SOURCE", "ORDER", "order-1", "1", "hash")));
        input.setEvidences(List.of(new GxpAuditEvidence("DOCUMENT", "document-1", "1", "hash", "SOURCE")));
        return input;
    }

    private void assertReplayUnchanged(GxpAuditAppendResult first, GxpAuditAppendResult replay, long relations) {
        assertTrue(replay.replayed());
        assertEquals(first.eventId(), replay.eventId());
        assertEquals(first.eventHash(), replay.eventHash());
        assertEquals(first.ledgerSequence(), replay.ledgerSequence());
        assertLedgerUnchanged(first, auditEventMapper.selectById(first.eventId()).getCanonicalEventJson(), relations);
    }

    private void assertLedgerUnchanged(GxpAuditAppendResult first, String canonical, long relations) {
        assertEquals(1L, auditEventMapper.selectCount());
        assertEquals(relations, eventRelationMapper.selectCount());
        assertEquals(2L, ledgerSequenceMapper.selectById(1L).getNextLedgerSequence());
        GxpAuditEventDO persisted = auditEventMapper.selectById(first.eventId());
        assertEquals(first.eventHash(), persisted.getEventHash());
        assertEquals(canonical, persisted.getCanonicalEventJson());
    }
    @MockBean
    private AdminUserApi users;

    @BeforeEach
    void setLoginUser() {
        TenantContextHolder.setTenantId(1L);
        when(users.getUser(1001L)).thenReturn(new AdminUserRespDTO().setId(1001L).setTenantId(1L)
                .setStatus(0).setUsername("qa.admin").setNickname("质量管理员"));
        LoginUser loginUser = new LoginUser();
        loginUser.setId(1001L);
        loginUser.setTenantId(1L);
        loginUser.setUserType(2);
        loginUser.setInfo(Map.of("username", "qa.admin", LoginUser.INFO_KEY_NICKNAME, "质量管理员"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null));
        insertPolicy("edhr.execution.field.update", "EDHR", "EDHR_FIELD", "UPDATE",
                "USER_REQUIRED", "NONE");
        insertPolicy("signature.record.create", "SIGNATURE", "SIGNATURE_RECORD", "CREATE",
                "USER_REQUIRED", "REQUIRED");
        GxpAuditLedgerSequenceDO sequence = new GxpAuditLedgerSequenceDO();
        sequence.setTenantId(1L);
        sequence.setNextLedgerSequence(1L);
        ledgerSequenceMapper.insert(sequence);
        GxpAuditPolicyActivationDO activation = new GxpAuditPolicyActivationDO();
        activation.setTenantId(1L);
        activation.setPolicyVersion("2026-09-approved-01");
        activation.setPolicyHash("test-policy-hash");
        activation.setRequestId("test-policy-request");
        activation.setActorId(1001L);
        activation.setApprovalReference("test-approval");
        activation.setActivatedAtUtc(java.time.LocalDateTime.now());
        activation.setEffectiveAfterSequence(0L);
        activation.setCanonicalActivationJson("{\"test\":true}");
        activation.setActivationHash("test-activation-hash");
        policyActivationMapper.insert(activation);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
        ledgerSequenceMapper.deleteById(1L);
        TenantContextHolder.clear();
    }

    @Test
    void formalNicknameDeptOnlyTokenUsesCurrentDirectoryActorInRealLedger() {
        var login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var originalInfo = Map.of(LoginUser.INFO_KEY_NICKNAME, "质量管理员", LoginUser.INFO_KEY_DEPT_ID, "9");
        login.setInfo(originalInfo);
        var appended = gxpAuditService.append(command("edhr.execution.field.update", "formal-ui-actor"));
        var event = auditEventMapper.selectById(appended.eventId());
        assertEquals(1001L, event.getActorId());
        assertEquals("qa.admin", event.getActorUsername());
        assertEquals("质量管理员", event.getActorDisplayName());
        assertTrue(event.getCanonicalEventJson().contains("qa.admin"));
        assertSame(originalInfo, login.getInfo());
        assertFalse(originalInfo.containsKey("username"));
    }

    @Test
    void missingDisabledOrForeignDirectoryAndContextRejectBeforeAnyLedgerWrite() {
        for (String invalid : new String[] { "missing", "disabled", "directoryId", "directoryTenant", "blankUsername",
                "blankNickname", "contextTenant", "ignore", "missingLoginId", "wrongType" }) {
            var account = new AdminUserRespDTO().setId(1001L).setTenantId(1L).setStatus(0)
                    .setUsername("qa.admin").setNickname("质量管理员");
            var login = new LoginUser().setId(1001L).setTenantId(1L).setUserType(2)
                    .setInfo(Map.of(LoginUser.INFO_KEY_NICKNAME, "质量管理员", LoginUser.INFO_KEY_DEPT_ID, "9"));
            TenantContextHolder.setTenantId(1L);
            TenantContextHolder.setIgnore(false);
            switch (invalid) {
                case "missing" -> account = null;
                case "disabled" -> account.setStatus(1);
                case "directoryId" -> account.setId(99L);
                case "directoryTenant" -> account.setTenantId(2L);
                case "blankUsername" -> account.setUsername(" ");
                case "blankNickname" -> account.setNickname(" ");
                case "contextTenant" -> TenantContextHolder.setTenantId(2L);
                case "ignore" -> TenantContextHolder.setIgnore(true);
                case "missingLoginId" -> login.setId(null);
                case "wrongType" -> login.setUserType(1);
            }
            when(users.getUser(1001L)).thenReturn(account);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null));
            assertServiceException(() -> gxpAuditService.append(command("edhr.execution.field.update", "bad-actor-" + invalid)),
                    GXP_AUDIT_ACTOR_MISMATCH, invalid.startsWith("directory") || invalid.startsWith("blank")
                            || invalid.equals("missing") || invalid.equals("disabled") ? "current-account" : "authenticated-actor");
            assertEquals(0L, auditEventMapper.selectCount());
            assertEquals(1L, ledgerSequenceMapper.selectCount());
            assertEquals(1L, ledgerSequenceMapper.selectById(1L).getNextLedgerSequence());
        }
    }

    @Test
    void currentDirectoryOwnsAuditNamesAndHistoricalReplayNeverRewritesIdentity() {
        var login = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var originalInfo = Map.of("username", "untrusted-cached-name", LoginUser.INFO_KEY_NICKNAME, "缓存旧昵称");
        login.setInfo(originalInfo);
        var first = gxpAuditService.append(command("edhr.execution.field.update", "directory-replay"));
        var historical = auditEventMapper.selectById(first.eventId()).getCanonicalEventJson();
        when(users.getUser(1001L)).thenReturn(new AdminUserRespDTO().setId(1001L).setTenantId(1L)
                .setStatus(0).setUsername("renamed-current").setNickname("新昵称"));
        var replay = gxpAuditService.append(command("edhr.execution.field.update", "directory-replay"));
        assertTrue(replay.replayed());
        assertEquals(historical, auditEventMapper.selectById(first.eventId()).getCanonicalEventJson());
        assertEquals("qa.admin", auditEventMapper.selectById(first.eventId()).getActorUsername());
        assertSame(originalInfo, login.getInfo());
        when(users.getUser(1001L)).thenReturn(null);
        assertServiceException(() -> gxpAuditService.append(command("edhr.execution.field.update", "directory-replay")),
                GXP_AUDIT_ACTOR_MISMATCH, "current-account");
        assertEquals(1L, auditEventMapper.selectCount());
        assertEquals(historical, auditEventMapper.selectById(first.eventId()).getCanonicalEventJson());
    }

    @Test
    void explicitSystemContextAndDeclaredSystemActorRemainSystemFacts() {
        SecurityContextHolder.clearContext();
        var automatic = gxpAuditService.append(command("edhr.execution.field.update", "system-empty-context"));
        var event = auditEventMapper.selectById(automatic.eventId());
        assertEquals(-1L, event.getActorId());
        assertEquals("SYSTEM_ACTOR", event.getActorUsername());
        var declared = new LoginUser().setId(-1L).setTenantId(1L).setUserType(2)
                .setInfo(Map.of("username", "SYSTEM_ACTOR", LoginUser.INFO_KEY_NICKNAME, "SYSTEM_ACTOR"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(declared, null));
        var explicit = gxpAuditService.append(command("edhr.execution.field.update", "system-declared"));
        assertEquals("SYSTEM_ACTOR", auditEventMapper.selectById(explicit.eventId()).getActorUsername());
        verifyNoInteractions(users);
        declared.setInfo(Map.of("username", "qa.admin", LoginUser.INFO_KEY_NICKNAME, "SYSTEM_ACTOR"));
        assertServiceException(() -> gxpAuditService.append(command("edhr.execution.field.update", "forged-system")),
                GXP_AUDIT_ACTOR_MISMATCH, "declared-system-actor");
        assertEquals(2L, auditEventMapper.selectCount());
    }

    @Test
    void append_shouldWriteCompleteLedgerEventWithHashAndActorInSameContract() {
        GxpAuditAppendResult result = gxpAuditService.append(command("edhr.execution.field.update", "idem-1"));

        GxpAuditEventDO event = auditEventMapper.selectById(result.eventId());
        assertNotNull(event);
        assertEquals(1L, event.getTenantId());
        assertEquals(1L, event.getLedgerSequence());
        assertEquals("EDHR", event.getDomain());
        assertEquals("EDHR_FIELD", event.getSubjectType());
        assertEquals("UPDATE", event.getAction());
        assertEquals("质量复核修订字段", event.getReason());
        assertEquals(1001L, event.getActorId());
        assertEquals("qa.admin", event.getActorUsername());
        assertEquals("质量管理员", event.getActorDisplayName());
        assertEquals("PRESENT", event.getBeforeState());
        assertEquals("PRESENT", event.getAfterState());
        assertNotNull(event.getServerOccurredAt());
        assertNotNull(event.getCanonicalEventJson());
        assertEquals(64, event.getIdempotencyPayloadHash().length());
        assertEquals(64, event.getEventHash().length());
        assertFalse(result.replayed());
    }

    @Test
    void append_shouldReturnExistingEventWhenIdempotencyPayloadIsSame() {
        GxpAuditAppendResult first = gxpAuditService.append(command("edhr.execution.field.update", "idem-2"));
        GxpAuditAppendResult second = gxpAuditService.append(command("edhr.execution.field.update", "idem-2"));

        assertEquals(first.eventId(), second.eventId());
        assertTrue(second.replayed());
        assertEquals(1L, auditEventMapper.selectCount());
    }

    @Test
    void append_shouldAllocateLedgerSequenceThroughWatermarkAndChainHashes() {
        GxpAuditAppendResult first = gxpAuditService.append(command("edhr.execution.field.update", "idem-seq-1"));
        GxpAuditAppendResult second = gxpAuditService.append(command("edhr.execution.field.update", "idem-seq-2"));

        GxpAuditEventDO firstEvent = auditEventMapper.selectById(first.eventId());
        GxpAuditEventDO secondEvent = auditEventMapper.selectById(second.eventId());
        assertEquals(1L, firstEvent.getLedgerSequence());
        assertEquals(2L, secondEvent.getLedgerSequence());
        assertNull(firstEvent.getPreviousEventHash());
        assertEquals(firstEvent.getEventHash(), secondEvent.getPreviousEventHash());
        assertEquals(3L, ledgerSequenceMapper.selectById(1L).getNextLedgerSequence());
    }

    @Test
    void append_shouldRejectUnregisteredOperation() {
        assertServiceException(() -> gxpAuditService.append(command("dcc.file.publish", "idem-3")),
                GXP_AUDIT_POLICY_NOT_FOUND, "dcc.file.publish");
        assertEquals(0L, auditEventMapper.selectCount());
    }

    @Test
    void append_shouldRejectMissingRequiredReason() {
        GxpAuditCommand command = command("edhr.execution.field.update", "idem-4");
        command.setReason(" ");

        assertServiceException(() -> gxpAuditService.append(command),
                GXP_AUDIT_REASON_REQUIRED, "edhr.execution.field.update");
        assertEquals(0L, auditEventMapper.selectCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER_REQUIRED", "REQUIRED", "REQUIRED_CATEGORY_AND_TEXT"})
    void userRequiredPolicyRejectsBlankReasonWithoutWritingAudit(String reasonPolicy) {
        insertPolicy("test.user-required", "TEST", "TEST", "UPDATE", reasonPolicy, "NONE");
        GxpAuditCommand input = command("test.user-required", "user-required-empty");
        input.setReason(" ");
        assertServiceException(() -> gxpAuditService.append(input),
                GXP_AUDIT_REASON_REQUIRED, "test.user-required");
        assertEquals(0L, auditEventMapper.selectCount());
    }

    @Test
    void append_shouldRejectMissingBeforeAfterSnapshot() {
        GxpAuditCommand command = command("edhr.execution.field.update", "idem-5");
        command.setAfterState(null);

        assertServiceException(() -> gxpAuditService.append(command),
                GXP_AUDIT_BEFORE_AFTER_REQUIRED, "edhr.execution.field.update");
        assertEquals(0L, auditEventMapper.selectCount());
    }

    @Test
    void append_shouldRejectRequiredSignatureWhenMissing() {
        GxpAuditCommand command = command("signature.record.create", "idem-6");

        assertServiceException(() -> gxpAuditService.append(command),
                GXP_AUDIT_SIGNATURE_REQUIRED, "signature.record.create");
        assertEquals(0L, auditEventMapper.selectCount());
    }

    @Test
    void append_shouldRejectIdempotencyPayloadConflict() {
        gxpAuditService.append(command("edhr.execution.field.update", "idem-7"));
        GxpAuditCommand conflict = command("edhr.execution.field.update", "idem-7");
        conflict.setReason("另一条不同原因");

        assertServiceException(() -> gxpAuditService.append(conflict),
                GXP_AUDIT_IDEMPOTENCY_CONFLICT, "edhr.execution.field.update");
        assertEquals(1L, auditEventMapper.selectCount());
    }

    @Test
    void appendKeepsDeviceAuthenticationSeparateFromEmployeeAndReplaysPersistedIdentity() {
        String employee = "{\"actorId\":\"9102\",\"actorType\":\"MES_EMPLOYEE_PROFILE\","
                + "\"displayName\":\"正式员工\",\"username\":\"EMP-9102\"}";
        GxpAuditCommand first = command("edhr.execution.field.update", "identity-replay");
        first.setPerformedBy(employee);
        GxpAuditAppendResult result = gxpAuditService.append(first);
        GxpAuditEventDO event = auditEventMapper.selectById(result.eventId());
        assertEquals(1001L, event.getActorId());
        Map<?, ?> authenticated = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                event.getAuthenticatedActorJson(), Map.class);
        assertEquals("1001", String.valueOf(authenticated.get("actorId")));
        assertEquals("qa.admin", authenticated.get("username"));
        assertEquals(employee, event.getPerformedByJson());
        GxpAuditCommand replay = command("edhr.execution.field.update", "identity-replay");
        replay.setPerformedBy(employee);
        assertEquals(result.eventId(), gxpAuditService.append(replay).eventId());
        assertEquals(1L, auditEventMapper.selectCount());
        assertEquals(employee, auditEventMapper.selectById(result.eventId()).getPerformedByJson());
    }

    private GxpAuditCommand command(String operationId, String idempotencyKey) {
        return GxpAuditCommand.builder()
                .operationId(operationId)
                .subjectId("execution-field-1")
                .subjectVersion("2")
                .reason("质量复核修订字段")
                .beforeState(GxpAuditStateEnvelope.builder()
                        .state("PRESENT")
                        .objectVersion("1")
                        .canonicalJson("{\"field\":\"old\"}")
                        .build())
                .afterState(GxpAuditStateEnvelope.builder()
                        .state("PRESENT")
                        .objectVersion("2")
                        .canonicalJson("{\"field\":\"new\"}")
                        .build())
                .idempotencyKey(idempotencyKey)
                .requestId("req-1")
                .traceId("trace-1")
                .source("unit-test")
                .build();
    }

    private void insertPolicy(String operationId, String domain, String subjectType, String actionType,
                              String reasonPolicy, String signaturePolicy) {
        GxpAuditPolicyOperationDO policy = new GxpAuditPolicyOperationDO();
        policy.setTenantId(1L);
        policy.setPolicyVersion("2026-09-approved-01");
        policy.setOperationId(operationId);
        policy.setSourceType("SERVICE_METHOD");
        policy.setSourceLocator("unit-test#" + operationId);
        policy.setDomain(domain);
        policy.setSubjectType(subjectType);
        policy.setActionType(actionType);
        policy.setReasonPolicy(reasonPolicy);
        policy.setSignaturePolicy(signaturePolicy);
        policy.setStatePolicy("PRESENT_TO_PRESENT");
        policy.setRetentionClass("GXP_MASTER_DATA");
        policy.setTestIds("BDD-AT-01");
        policy.setOwner("qa-owner");
        policy.setApplicability("GXP");
        policy.setActive(true);
        policyOperationMapper.insert(policy);
    }

}
