package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Flow 7 boundary contract: Tx-C receives only a persisted-batch witness.
 * Client receipts and raw source evidence belong to Flow 6 entry validation.
 */
class MesProEdhrBatchTraceTxCInputContractTest {

    @Test
    void txCCommandCannotCarryClientReceiptOrRawSourcePayload() {
        Set<String> fields = Arrays.stream(MesProEdhrBatchTraceTxCCommand.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "batchExecutionId", "provisioningReceiptId", "eventId", "idempotencyKey",
                "expectedSourceSnapshotHash", "expectedSourceBundleHash",
                "expectedCompletionBackfillReceiptHash", "expectedSourceVersion",
                "expectedSourceCredentialId", "expectedSourceCredentialHash", "capturedBy"), fields);
        assertFalse(fields.stream().anyMatch(name -> name.equals("completionBackfillReceipt")
                || name.equals("independentReceipt")
                || name.equals("sourceEvidence")
                || name.equals("payload")
                || name.equals("rawPayload")));
    }
}
