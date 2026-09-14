package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrBatchTraceSourceHashTest {

    @Test
    void persistedExternalWitnessHashIsAcceptedOnlyForDeclaredSourceTypes() {
        String snapshot = "{\"sourceType\":\"MATERIAL_ISSUE\",\"witnessHash\":\"upstream-hash\"}";

        assertTrue(MesProEdhrBatchTraceSourceHash.isValid(
                MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE, snapshot, "upstream-hash"));
        assertFalse(MesProEdhrBatchTraceSourceHash.isValid(
                MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE, snapshot, "tampered-hash"));
        assertFalse(MesProEdhrBatchTraceSourceHash.isValid(
                MesProEdhrBatchTraceLinkType.WORK_ORDER, snapshot, "upstream-hash"));
    }

    @Test
    void persistedCompletionOutputReceiptWitnessHashIsAcceptedForTxC() {
        String batchRecordSnapshot = "{\"sourceType\":\"BATCH_RECORD_RECEIPT\",\"witnessHash\":\"receipt-hash\"}";
        String processInspectionSnapshot =
                "{\"sourceType\":\"PROCESS_INSPECTION_RECEIPT\",\"witnessHash\":\"receipt-hash\"}";

        assertTrue(MesProEdhrBatchTraceSourceHash.isValid(
                MesProEdhrBatchTraceLinkType.BATCH_RECORD_RECEIPT, batchRecordSnapshot, "receipt-hash"));
        assertTrue(MesProEdhrBatchTraceSourceHash.isValid(
                MesProEdhrBatchTraceLinkType.PROCESS_INSPECTION_RECEIPT,
                processInspectionSnapshot, "receipt-hash"));
    }
}
