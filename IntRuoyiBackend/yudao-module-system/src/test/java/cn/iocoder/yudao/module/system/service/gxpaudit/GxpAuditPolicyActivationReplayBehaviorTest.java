package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyVersionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditLedgerSequenceDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyVersionMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_ACTIVATION_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ACT-03 bounded regression: persisted-row fixtures through mocked mapper reads.
 * This is NOT database, transaction, concurrency, approval or activation integration evidence.
 * BDD: Given an existing request and immutable version evidence, When its actor, coverage
 * or bundle facts differ, Then reject without writes; identical facts replay the same receipt.
 * RED: pending the coordinator's sole Maven executor; production implementation unchanged.
 * Remaining: full payloadHash, per-activation coverage/baseline history and lock/recheck protocol.
 */
class GxpAuditPolicyActivationReplayBehaviorTest {

    private final GxpAuditPolicyActivationServiceImpl service = new GxpAuditPolicyActivationServiceImpl();
    private final GxpAuditPolicyBundleLoader loader = mock(GxpAuditPolicyBundleLoader.class);
    private final GxpAuditPolicyActivationMapper activations = mock(GxpAuditPolicyActivationMapper.class);
    private final GxpAuditPolicyVersionMapper versions = mock(GxpAuditPolicyVersionMapper.class);
    private final GxpAuditPolicyOperationMapper operations = mock(GxpAuditPolicyOperationMapper.class);
    private final GxpAuditLedgerSequenceMapper ledger = mock(GxpAuditLedgerSequenceMapper.class);
    private final GxpAuditService audit = mock(GxpAuditService.class);
    private final TenantMapper tenants = mock(TenantMapper.class);
    private final GxpAuditEventMapper events = mock(GxpAuditEventMapper.class);
    private GxpAuditPolicyActivationDO existing;
    private GxpAuditPolicyVersionDO version;
    private GxpAuditPolicyBundle bundle;

    @BeforeEach
    void givenPersistedActivationAndVersion() {
        TenantContextHolder.setTenantId(11L);
        login(21L);
        bundle = new GxpAuditPolicyBundle("gxp-audit-policy.v2", "test-v1", "APPROVED",
                "TEST-ONLY-APPROVAL", "a".repeat(64), "b".repeat(64), "{\"test\":1}",
                "test-only", "test-only", JsonNodeFactory.instance.objectNode());
        existing = new GxpAuditPolicyActivationDO();
        existing.setId(31L);
        existing.setTenantId(11L);
        existing.setRequestId("test-request-1");
        existing.setActorId(21L);
        existing.setPolicyVersion(bundle.policyVersion());
        existing.setPolicyHash(bundle.policyHash());
        existing.setApprovalReference(bundle.approvalReference());
        existing.setCanonicalActivationJson("{\"original\":true}");
        existing.setActivationHash("c".repeat(64));
        existing.setEffectiveAfterSequence(9L);
        version = new GxpAuditPolicyVersionDO();
        version.setId(41L);
        version.setTenantId(11L);
        version.setPolicyVersion(bundle.policyVersion());
        version.setPolicyHash(bundle.policyHash());
        version.setApprovalReference(bundle.approvalReference());
        version.setApprovedBy(21L);
        version.setCoverageReportHash("d".repeat(64));
        version.setArtifactHash(bundle.artifactHash());
        version.setSchemaVersion(bundle.schemaVersion());
        version.setCanonicalPolicyJson(bundle.canonicalPolicyJson());
        when(loader.load()).thenReturn(bundle);
        when(activations.selectByRequestIdForUpdate(11L, "test-request-1")).thenReturn(existing);
        TenantDO tenant = new TenantDO();
        tenant.setId(11L);
        when(tenants.selectByIdForUpdate(11L)).thenReturn(tenant);
        GxpAuditLedgerSequenceDO watermark = new GxpAuditLedgerSequenceDO();
        watermark.setTenantId(11L);
        watermark.setNextLedgerSequence(10L);
        when(ledger.selectByTenantIdForUpdate(11L)).thenReturn(watermark);
        when(versions.selectOne(org.mockito.ArgumentMatchers.<Wrapper<GxpAuditPolicyVersionDO>>any()))
                .thenAnswer(invocation -> version);
        ReflectionTestUtils.setField(service, "bundleLoader", loader);
        ReflectionTestUtils.setField(service, "activationMapper", activations);
        ReflectionTestUtils.setField(service, "policyVersionMapper", versions);
        ReflectionTestUtils.setField(service, "policyOperationMapper", operations);
        ReflectionTestUtils.setField(service, "ledgerSequenceMapper", ledger);
        ReflectionTestUtils.setField(service, "gxpAuditService", audit);
        ReflectionTestUtils.setField(service, "tenantMapper", tenants);
        ReflectionTestUtils.setField(service, "auditEventMapper", events);
    }

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void sameRequestAndPersistedPayloadReplayWithoutWrites() {
        GxpAuditPolicyActivationResult result = service.activate(command(21L, "d".repeat(64)));
        assertEquals(new GxpAuditPolicyActivationResult(31L, "test-v1", "a".repeat(64), true), result);
        assertNoWrites();
    }

    @Test
    void changedAuthenticatedActorCannotReplayAnotherActorsRequest() {
        login(22L); // Both command and current authentication agree; original receipt does not.
        assertConflict(command(22L, "d".repeat(64)));
    }

    @Test
    void changedCoverageReportCannotReplaySameRequest() {
        assertConflict(command(21L, "e".repeat(64)));
    }

    @Test
    void changedArtifactCannotReplaySameRequestEvenWhenPolicyHashMatches() {
        version.setArtifactHash("e".repeat(64));
        assertConflict(command(21L, "d".repeat(64)));
    }

    @Test
    void changedCanonicalPolicyCannotReplaySameRequest() {
        version.setCanonicalPolicyJson("{\"test\":2}");
        assertConflict(command(21L, "d".repeat(64)));
    }

    @Test
    void changedSchemaCannotReplaySameRequest() {
        version.setSchemaVersion("different-schema");
        assertConflict(command(21L, "d".repeat(64)));
    }

    @Test
    void missingPersistedVersionCannotBeSilentlyAccepted() {
        version = null;
        assertConflict(command(21L, "d".repeat(64)));
    }

    @Test
    void missingHistoricalCoverageCannotBeFilledFromCurrentRequest() {
        version.setCoverageReportHash(null);
        assertConflict(command(21L, "d".repeat(64)));
    }

    @Test
    void changedPolicyHashRemainsRejected() {
        existing.setPolicyHash("e".repeat(64));
        assertConflict(command(21L, "d".repeat(64)));
    }

    @Test
    void commandActorMustStillMatchAuthentication() {
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.activate(command(22L, "d".repeat(64))));
        assertEquals(GXP_AUDIT_POLICY_ACTIVATION_INVALID.getCode(), error.getCode());
        verifyNoInteractions(loader, activations, versions, operations, ledger, audit, tenants, events);
    }

    private void assertConflict(GxpAuditPolicyActivationCommand command) {
        ServiceException error = assertThrows(ServiceException.class, () -> service.activate(command));
        assertEquals(GXP_AUDIT_POLICY_ACTIVATION_INVALID.getCode(), error.getCode());
        assertTrue(error.getMessage().contains("activation-request-payload-conflict"));
        assertNoWrites();
    }

    private void assertNoWrites() {
        verifyNoInteractions(operations, audit, events);
        verify(tenants).selectByIdForUpdate(11L);
        verify(ledger).selectByTenantIdForUpdate(11L);
        verifyNoMoreInteractions(tenants, ledger);
        assertTrue(mockingDetails(activations).getInvocations().stream()
                .allMatch(call -> call.getMethod().getName().startsWith("select")));
        assertTrue(mockingDetails(versions).getInvocations().stream()
                .allMatch(call -> call.getMethod().getName().startsWith("select")));
        assertEquals("{\"original\":true}", existing.getCanonicalActivationJson());
        assertEquals("c".repeat(64), existing.getActivationHash());
        assertEquals(9L, existing.getEffectiveAfterSequence());
    }

    private GxpAuditPolicyActivationCommand command(Long actorId, String coverageHash) {
        return new GxpAuditPolicyActivationCommand(11L, actorId, "TEST-ONLY-APPROVAL",
                "test-request-1", coverageHash);
    }

    private void login(Long actorId) {
        LoginUser user = new LoginUser();
        user.setId(actorId);
        user.setTenantId(11L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null));
    }
}
