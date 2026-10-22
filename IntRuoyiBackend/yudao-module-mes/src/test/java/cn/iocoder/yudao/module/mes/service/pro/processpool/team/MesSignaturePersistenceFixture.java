package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.signature.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static org.mockito.Mockito.*;

/** Real signature SQL and real audit writer/verifier over controlled audit mapper facts.
 * Audit mapper copies are not database roundtrips; ledger mocks do not prove physical locks. */
public final class MesSignaturePersistenceFixture {
    public final ElectronicSignatureServiceImpl writer = new ElectronicSignatureServiceImpl();
    public final ElectronicSignatureQueryServiceImpl query = new ElectronicSignatureQueryServiceImpl();
    public cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService verifiedFacts;
    public final GxpAuditEventMapper auditRows = mock(GxpAuditEventMapper.class);
    public final TransactionTemplate transactions;

    public MesSignaturePersistenceFixture(ElectronicSignatureRecordMapper records, AdminUserApi users,
                                          PlatformTransactionManager transactionManager) {
        transactions = new TransactionTemplate(transactionManager);
        var adapters = List.of(new MesBatchRecordSignatureSubjectAdapter(), new BpmApprovalSignatureSubjectAdapter());
        ReflectionTestUtils.setField(writer, "signatureRecordMapper", records);
        ReflectionTestUtils.setField(writer, "adminUserApi", users);
        ReflectionTestUtils.setField(writer, "subjectAdapters", adapters);
        ReflectionTestUtils.setField(query, "signatureRecordMapper", records);
        var audit = new GxpAuditServiceImpl();
        var ledgerRows = mock(GxpAuditLedgerSequenceMapper.class);
        var policyRows = mock(GxpAuditPolicyOperationMapper.class);
        var activations = mock(GxpAuditPolicyActivationMapper.class);
        var relationRows = mock(GxpAuditEventRelationMapper.class);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var ledger = new GxpAuditLedgerSequenceDO();
        ledger.setTenantId(tenant); ledger.setNextLedgerSequence(1L);
        when(ledgerRows.selectByTenantIdForUpdate(tenant)).thenReturn(ledger);
        when(ledgerRows.updateById(any(GxpAuditLedgerSequenceDO.class))).thenAnswer(call -> {
            ledger.setNextLedgerSequence(call.getArgument(0, GxpAuditLedgerSequenceDO.class).getNextLedgerSequence());
            return 1;
        });
        var activation = new GxpAuditPolicyActivationDO();
        activation.setTenantId(tenant); activation.setPolicyVersion("signature-fixture-v1");
        when(activations.selectLatestForUpdate(tenant)).thenReturn(activation);
        var policy = new GxpAuditPolicyOperationDO();
        policy.setTenantId(tenant); policy.setPolicyVersion("signature-fixture-v1");
        policy.setOperationId("signature.record.create"); policy.setDomain("SIGNATURE");
        policy.setSubjectType("SIGNATURE_RECORD"); policy.setActionType("CREATE");
        policy.setActive(true); policy.setApplicability("GXP"); policy.setReasonPolicy("USER_REQUIRED");
        when(policyRows.selectByPolicyVersionForUpdate(tenant, "signature-fixture-v1", "signature.record.create"))
                .thenReturn(policy);
        var events = new ArrayList<GxpAuditEventDO>();
        when(auditRows.insert(any(GxpAuditEventDO.class))).thenAnswer(call -> {
            var written = call.getArgument(0, GxpAuditEventDO.class);
            written.setId(901L + events.size());
            events.add(copyAudit(written));
            return 1;
        });
        when(auditRows.selectLatestByTenantForUpdate(tenant)).thenAnswer(call ->
                events.isEmpty() ? null : copyAudit(events.get(events.size() - 1)));
        when(auditRows.selectByIdempotencyKeyForUpdate(eq(tenant), anyString())).thenAnswer(call ->
                events.stream().filter(event -> Objects.equals(event.getIdempotencyKey(), call.getArgument(1)))
                        .findFirst().map(MesSignaturePersistenceFixture::copyAudit).orElse(null));
        when(auditRows.selectSignatureCreationEvents(eq(tenant), anyString())).thenAnswer(call ->
                events.stream().filter(event -> Objects.equals(event.getSignatureRecordId(), call.getArgument(1)))
                        .map(MesSignaturePersistenceFixture::copyAudit).toList());
        ReflectionTestUtils.setField(audit, "auditEventMapper", auditRows);
        ReflectionTestUtils.setField(audit, "ledgerSequenceMapper", ledgerRows);
        ReflectionTestUtils.setField(audit, "policyOperationMapper", policyRows);
        ReflectionTestUtils.setField(audit, "policyActivationMapper", activations);
        ReflectionTestUtils.setField(audit, "eventRelationMapper", relationRows);
        var verifier = new SignatureCreationVerificationService();
        ReflectionTestUtils.setField(verifier, "auditEventMapper", auditRows);
        ReflectionTestUtils.setField(query, "signatureCreationVerificationService", verifier);
        verifiedFacts = new cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService(records, verifier);
        ReflectionTestUtils.setField(writer, "gxpAuditService", audit);
    }

    private static GxpAuditEventDO copyAudit(GxpAuditEventDO original) {
        var copy = new GxpAuditEventDO();
        BeanUtils.copyProperties(original, copy);
        return copy;
    }

    public ApprovalSignatureRecordResult bpm(long actor, long task, String opinion) {
        return bpm(actor, task, opinion, ApprovalModuleCode.EDHR, "EDHR_WORK_TASK");
    }

    public ApprovalSignatureRecordResult bpm(long actor, long task, String opinion,
                                            ApprovalModuleCode module, String sourceType) {
        var images = mock(ApprovalSignatureImageSnapshotProvider.class);
        when(images.requireActiveSnapshot(actor)).thenReturn(ApprovalSignatureImageSnapshot.builder()
                .imageId(91L).versionNo(1).fileId(92L).fileUrl("https://test.invalid/signature.png")
                .sha256("a".repeat(64)).contentType("image/png").fileSize(100L)
                .imageStatus("ACTIVE").verifiedStatus("VERIFIED").build());
        return transactions.execute(status -> new ApprovalSignatureRecordServiceImpl(writer, images).recordReviewSignature(
                ApprovalTaskReviewContext.of(actor, module, sourceType,
                        String.valueOf(task), String.valueOf(task), null, ApprovalTaskReviewResult.APPROVE,
                        opinion, "test-only", false)));
    }
}
