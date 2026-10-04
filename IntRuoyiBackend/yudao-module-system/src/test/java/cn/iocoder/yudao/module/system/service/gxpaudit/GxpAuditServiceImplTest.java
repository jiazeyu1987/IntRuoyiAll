package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import(GxpAuditServiceImpl.class)
class GxpAuditServiceImplTest extends BaseDbUnitTest {

    @Resource
    private GxpAuditService gxpAuditService;
    @Resource
    private GxpAuditEventMapper auditEventMapper;
    @Resource
    private GxpAuditLedgerSequenceMapper ledgerSequenceMapper;
    @Resource
    private GxpAuditPolicyOperationMapper policyOperationMapper;
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
                "REQUIRED_CATEGORY_AND_TEXT", "NOT_REQUIRED");
        insertPolicy("signature.record.create", "SIGNATURE", "SIGNATURE_RECORD", "CREATE",
                "REQUIRED_CATEGORY_AND_TEXT", "REQUIRED");
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
            assertEquals(0L, ledgerSequenceMapper.selectCount());
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

