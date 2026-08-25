package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Flow 7 Tx-C request. The request is intentionally witness-only: formal
 * source payloads are read from the Flow 6 provision audit and binding tables.
 * The credential witnesses are compared with those persisted facts and are
 * never used as capture payloads.
 */
@Data
@Accessors(chain = true)
public class MesProEdhrBatchTraceTxCCommand {

    private Long batchExecutionId;
    private String eventId;
    private String idempotencyKey;
    private String expectedSourceSnapshotHash;
    private String expectedSourceBundleHash;
    private String expectedCompletionBackfillReceiptHash;
    private String expectedSourceVersion;
    private String expectedSourceCredentialId;
    private String expectedSourceCredentialHash;
    private Long capturedBy;
}
