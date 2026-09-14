package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

/** Flow 6 to Flow 7 handoff. The event contains only persisted witnesses. */
@Data
@Accessors(chain = true)
public class MesProEdhrBatchProvisionedEvent {

    private Long tenantId;
    private Long batchExecutionId;
    private Long provisioningReceiptId;
    private String eventId;
    private String idempotencyKey;
    private String expectedSourceSnapshotHash;
    private String expectedSourceBundleHash;
    private String expectedCompletionBackfillReceiptHash;
    private String expectedSourceVersion;
    private String expectedSourceCredentialId;
    private String expectedSourceCredentialHash;
    private Long capturedBy;

    MesProEdhrBatchTraceTxCCommand toCommand() {
        require(tenantId, "tenantId");
        require(batchExecutionId, "batchExecutionId");
        require(provisioningReceiptId, "provisioningReceiptId");
        requireText(eventId, "eventId");
        requireText(idempotencyKey, "idempotencyKey");
        return new MesProEdhrBatchTraceTxCCommand()
                .setBatchExecutionId(batchExecutionId)
                .setProvisioningReceiptId(provisioningReceiptId)
                .setEventId(eventId)
                .setIdempotencyKey(idempotencyKey)
                .setExpectedSourceSnapshotHash(expectedSourceSnapshotHash)
                .setExpectedSourceBundleHash(expectedSourceBundleHash)
                .setExpectedCompletionBackfillReceiptHash(expectedCompletionBackfillReceiptHash)
                .setExpectedSourceVersion(expectedSourceVersion)
                .setCapturedBy(capturedBy);
    }

    private static void require(Long value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Flow 6 provision event requires " + field);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Flow 6 provision event requires " + field);
        }
    }
}
