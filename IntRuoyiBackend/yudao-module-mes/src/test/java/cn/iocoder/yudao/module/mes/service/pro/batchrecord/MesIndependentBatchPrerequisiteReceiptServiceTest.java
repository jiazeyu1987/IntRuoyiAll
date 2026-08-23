package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptDO;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_RECEIPT_EXPIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_ALREADY_REVOKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_HASH_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_SOURCE_CHANGED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_INDEPENDENT_RECEIPT_TENANT_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesIndependentBatchPrerequisiteReceiptServiceTest {

    private final InMemoryStore store = new InMemoryStore();
    private final MesIndependentBatchPrerequisiteReceiptServiceImpl service =
            new MesIndependentBatchPrerequisiteReceiptServiceImpl(store,
                    Clock.fixed(Instant.parse("2026-08-23T00:00:00Z"), ZoneOffset.UTC), "mes-test", "test-secret");

    @Test
    void issueVerifyAndRepeatedVerifyUsePersistedCanonicalEvidence() {
        MesIndependentBatchPrerequisiteReceipt receipt = service.issue(command(), 1L, 9L);
        assertNotNull(receipt.getReceiptId());
        assertEquals(64, receipt.getPayloadHash().length());
        assertEquals(receipt.getPayloadHash(),
                MesIndependentBatchPrerequisiteReceiptCanonicalizer.sha256(store.byReceipt.get(receipt.getReceiptId()).getCanonicalPayload()));
        assertEquals(receipt.getReceiptId(), service.verify(verifyCommand(receipt), 1L).getReceiptId());
        assertEquals(receipt.getReceiptId(), service.verify(verifyCommand(receipt), 1L).getReceiptId());
    }

    @Test
    void tamperedCanonicalPayloadAndSourceChangeFailFast() {
        MesIndependentBatchPrerequisiteReceipt receipt = service.issue(command(), 1L, 9L);
        store.byReceipt.get(receipt.getReceiptId()).setCanonicalPayload("tampered");
        ServiceException hash = assertThrows(ServiceException.class, () -> service.verify(verifyCommand(receipt), 1L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_HASH_INVALID.getCode(), hash.getCode());

        MesIndependentBatchPrerequisiteReceipt second = service.issue(command().setIdempotencyKey("second"), 1L, 9L);
        ServiceException source = assertThrows(ServiceException.class,
                () -> service.verify(verifyCommand(second).setSourceSnapshotHash("changed"), 1L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_SOURCE_CHANGED.getCode(), source.getCode());
    }

    @Test
    void expiryRevokeTenantAndIdempotencyAreStable() {
        MesIndependentBatchPrerequisiteReceipt receipt = service.issue(command(), 1L, 9L);
        ServiceException tenant = assertThrows(ServiceException.class, () -> service.verify(verifyCommand(receipt), 2L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_TENANT_MISMATCH.getCode(), tenant.getCode());

        assertEquals(receipt.getReceiptId(), service.issue(command(), 1L, 9L).getReceiptId());
        ServiceException conflict = assertThrows(ServiceException.class,
                () -> service.issue(command().setSourceSnapshotHash("other"), 1L, 9L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_IDEMPOTENCY_CONFLICT.getCode(), conflict.getCode());

        service.revoke(new MesIndependentBatchPrerequisiteReceiptRevokeCommand()
                .setReceiptId(receipt.getReceiptId()).setReason("source withdrawn"), 1L, 9L);
        ServiceException revoked = assertThrows(ServiceException.class, () -> service.verify(verifyCommand(receipt), 1L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_ALREADY_REVOKED.getCode(), revoked.getCode());
        ServiceException again = assertThrows(ServiceException.class, () -> service.revoke(
                new MesIndependentBatchPrerequisiteReceiptRevokeCommand().setReceiptId(receipt.getReceiptId()).setReason("again"), 1L, 9L));
        assertEquals(PRO_EDHR_INDEPENDENT_RECEIPT_ALREADY_REVOKED.getCode(), again.getCode());
    }

    @Test
    void expiredReceiptIsRejected() {
        MesIndependentBatchPrerequisiteReceipt receipt = service.issue(command(), 1L, 9L);
        store.byReceipt.get(receipt.getReceiptId()).setExpiresAt(LocalDateTime.of(2026, 8, 22, 23, 59));
        ServiceException error = assertThrows(ServiceException.class, () -> service.verify(verifyCommand(receipt), 1L));
        assertEquals(PRO_EDHR_BATCH_ENTRY_RECEIPT_EXPIRED.getCode(), error.getCode());
    }

    private MesIndependentBatchPrerequisiteReceiptIssueCommand command() {
        return new MesIndependentBatchPrerequisiteReceiptIssueCommand()
                .setEntryType("PQC_INDEPENDENT").setWorkOrderId(22L).setWorkOrderCode("WO-22")
                .setRouteId(7L).setRouteVersionId(55L).setRouteVersion("v2").setBatchCode("B-1")
                .setSourceRelationId("relation-1").setSourceRelationVersion("v1")
                .setSourceRelationSnapshotHash("relation-snapshot").setSourceObjectType("PQC_APPLICATION")
                .setSourceObjectId("pqc-1").setMaterialSourceType("MATERIAL_ISSUE").setMaterialSourceId("material-1")
                .setSourceContextHash("context").setSourceSnapshotHash("snapshot")
                .setBusinessReason("independent test").setIdempotencyKey("idempotency-1")
                .setSourceEvidence(java.util.List.of(new MesBatchExecutionSourceEvidence()
                        .setSourceType("PQC").setSourceId("pqc-1").setSourceVersion("v1")
                        .setSourceSnapshotHash("pqc-snapshot").setPayloadHash("pqc-payload").setSignature("pqc-signature")));
    }

    private MesIndependentBatchPrerequisiteReceiptVerifyCommand verifyCommand(MesIndependentBatchPrerequisiteReceipt receipt) {
        return new MesIndependentBatchPrerequisiteReceiptVerifyCommand().setReceiptId(receipt.getReceiptId())
                .setEntryType(receipt.getEntryType()).setSourceSnapshotHash(receipt.getSourceSnapshotHash());
    }

    private static final class InMemoryStore implements MesIndependentBatchPrerequisiteReceiptStore {
        private final Map<String, MesIndependentBatchPrerequisiteReceiptDO> byReceipt = new HashMap<>();
        @Override public MesIndependentBatchPrerequisiteReceiptDO selectByReceiptId(Long tenantId, String receiptId) {
            MesIndependentBatchPrerequisiteReceiptDO value = byReceipt.get(receiptId);
            return value != null && tenantId.equals(value.getTenantId()) ? value : null;
        }
        @Override public MesIndependentBatchPrerequisiteReceiptDO selectByReceiptIdUnscoped(String receiptId) {
            return byReceipt.get(receiptId);
        }
        @Override public MesIndependentBatchPrerequisiteReceiptDO selectByIdempotencyKey(Long tenantId, String entryType, String key) {
            return byReceipt.values().stream().filter(value -> tenantId.equals(value.getTenantId())
                    && entryType.equals(value.getEntryType()) && key.equals(value.getIdempotencyKey())).findFirst().orElse(null);
        }
        @Override public void insert(MesIndependentBatchPrerequisiteReceiptDO data) { byReceipt.put(data.getReceiptId(), data); }
        @Override public void update(MesIndependentBatchPrerequisiteReceiptDO data) { byReceipt.put(data.getReceiptId(), data); }
    }
}
